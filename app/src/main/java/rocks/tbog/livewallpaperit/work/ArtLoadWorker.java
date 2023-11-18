package rocks.tbog.livewallpaperit.work;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.google.android.apps.muzei.api.provider.Artwork;
import com.google.android.apps.muzei.api.provider.ProviderClient;
import com.google.android.apps.muzei.api.provider.ProviderContract;
import com.kirkbushman.araw.RedditClient;
import com.kirkbushman.araw.fetcher.Fetcher;
import com.kirkbushman.araw.fetcher.SubmissionsFetcher;
import com.kirkbushman.araw.helpers.AuthUserlessHelper;
import com.kirkbushman.araw.models.Submission;
import com.kirkbushman.araw.models.enums.SubmissionsSorting;
import com.kirkbushman.araw.models.enums.TimePeriod;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import rocks.tbog.livewallpaperit.ArtProvider;
import rocks.tbog.livewallpaperit.Source;
import rocks.tbog.livewallpaperit.data.DBHelper;
import rocks.tbog.livewallpaperit.data.SubTopic;

public class ArtLoadWorker extends Worker {
    private static final String TAG = ArtLoadWorker.class.getSimpleName();
    private static final int FETCH_AMOUNT = 100;
    private static final int MAX_FETCHES = 5;
    private List<String> mIgnoreTokenList = Collections.emptyList();
    private int mArtworkSubmitCount = 0;
    private int mArtworkNotFoundCount = 0;

    private Filter mFilter = null;

    private static class Filter {
        public int minUpvotePercentage = 0;
        public int minScore = 0;
        public int minComments = 0;
        public boolean allowNSFW = true;
    }

    public ArtLoadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context ctx = getApplicationContext();

        String[] ignoreTokens = getInputData().getStringArray(WorkerUtils.DATA_IGNORE_TOKEN_LIST);
        if (ignoreTokens != null) {
            mIgnoreTokenList = Arrays.asList(ignoreTokens);
        }

        Source source = Source.fromByteArray(getInputData().getByteArray(WorkerUtils.DATA_SOURCE));
        if (source == null) {
            Log.e(TAG, "source=`null`");
            return Result.failure(new Data.Builder()
                    .putString(WorkerUtils.FAIL_REASON, "null source")
                    .build());
        }
        if (TextUtils.isEmpty(source.subreddit)) {
            Log.e(TAG, "subreddit=`" + source.subreddit + "`");
            return Result.failure(new Data.Builder()
                    .putString(WorkerUtils.FAIL_REASON, "empty subreddit")
                    .build());
        }
        String clientId = getInputData().getString(WorkerUtils.DATA_CLIENT_ID);
        if (TextUtils.isEmpty(clientId)) {
            return Result.failure(new Data.Builder()
                    .putString(WorkerUtils.FAIL_REASON, "empty clientId")
                    .build());
        }

        var helper = new AuthUserlessHelper(ctx, clientId, "DO_NOT_TRACK_THIS_DEVICE", false, true);
        // obtain a client
        RedditClient client = helper.getRedditClient();
        if (client == null)
            return Result.failure(new Data.Builder()
                    .putString(WorkerUtils.FAIL_REASON, "getRedditClient=null")
                    .build());

        mFilter = new Filter();
        mFilter.minUpvotePercentage = source.minUpvotePercentage;
        mFilter.minScore = source.minScore;
        mFilter.minComments = source.minComments;
        mFilter.allowNSFW = getInputData().getBoolean(WorkerUtils.DATA_ALLOW_NSFW, false);

        final int desiredArtworkCount = getInputData().getInt(WorkerUtils.DATA_DESIRED_ARTWORK_COUNT, 10);

        var cachedTopics = DBHelper.getSubTopics(ctx, source.subreddit);

        ProviderClient providerClient = ProviderContract.getProviderClient(ctx, ArtProvider.class);

        SubmissionsFetcher submissionsFetcher = client.getSubredditsClient()
                .createSubmissionsFetcher(
                        source.subreddit,
                        SubmissionsSorting.NEW,
                        TimePeriod.ALL_TIME,
                        Math.min(FETCH_AMOUNT, Fetcher.MAX_LIMIT));
        Log.d(TAG, "fetch " + submissionsFetcher.getSubreddit());
        List<Submission> submissions = submissionsFetcher.fetchNext();
        for (int fetchIndex = 1; fetchIndex < MAX_FETCHES; fetchIndex += 1) {
            if (submissions == null) break;

            for (Submission submission : submissions) {
                final SubTopic topic = SubTopic.fromSubmission(submission);
                if (topic.images.isEmpty()) continue;
                if (cachedTopics.stream().noneMatch(subTopic -> subTopic.id.equals(topic.id))) {
                    DBHelper.addSubTopic(getApplicationContext(), source.subreddit, topic);
                }
                getArtworks(submission.getSubredditNamePrefixed(), topic, providerClient);
                if (mArtworkSubmitCount >= desiredArtworkCount) {
                    Log.v(
                            TAG,
                            "stop; desiredArtworkCount=" + desiredArtworkCount + " artworkSubmitCount="
                                    + mArtworkSubmitCount);
                    break;
                }
            }

            if (mArtworkSubmitCount < desiredArtworkCount && submissionsFetcher.hasNext()) {
                Log.d(
                        TAG,
                        "#" + fetchIndex + " fetchNext " + submissionsFetcher.getSubreddit() + "; artworkSubmitCount="
                                + mArtworkSubmitCount);
                submissions = submissionsFetcher.fetchNext();
            } else {
                break;
            }
        }

        Log.i(TAG, "artworkSubmitCount=" + mArtworkSubmitCount + " artworkNotFoundCount=" + mArtworkNotFoundCount);
        return Result.success(new Data.Builder()
                .putInt(WorkerUtils.DATA_ARTWORK_SUBMIT_COUNT, mArtworkSubmitCount)
                .putInt(WorkerUtils.DATA_ARTWORK_NOT_FOUND_COUNT, mArtworkNotFoundCount)
                .build());
    }

    private void getArtworks(String subredditNamePrefixed, SubTopic topic, ProviderClient providerClient) {
        if (mFilter != null) {
            if (mFilter.minUpvotePercentage > 0) {
                if (topic.upvoteRatio < mFilter.minUpvotePercentage) {
                    Log.v(
                            TAG,
                            "upvote " + topic.upvoteRatio + "%<" + mFilter.minUpvotePercentage + "% skipping "
                                    + topic.permalink);
                    return;
                }
            }
            if (mFilter.minScore > 0) {
                if (topic.score < mFilter.minScore) {
                    Log.v(TAG, "score " + topic.score + "<" + mFilter.minScore + " skipping " + topic.permalink);
                    return;
                }
            }
            if (mFilter.minComments > 0) {
                if (topic.numComments < mFilter.minComments) {
                    Log.v(
                            TAG,
                            "numComments " + topic.numComments + "<" + mFilter.minComments + " skipping "
                                    + topic.permalink);
                    return;
                }
            }
            if (!mFilter.allowNSFW) {
                if (topic.over18) {
                    Log.v(TAG, "NSFW not allowed skipping " + topic.permalink);
                    return;
                }
            }
        }
        for (var image : topic.images) {
            if (!image.isSource || image.isObfuscated) continue;
            String byline = topic.linkFlairText;
            if (TextUtils.isEmpty(byline)) byline = subredditNamePrefixed;
            Artwork artwork = new Artwork.Builder()
                    .persistentUri(Uri.parse(image.url))
                    .webUri(Uri.parse("https://www.reddit.com" + topic.permalink))
                    .token(image.mediaId)
                    .attribution(topic.author)
                    .byline(byline)
                    .title(topic.title)
                    .build();

            Log.v(TAG, "addArtwork " + artwork.getToken() + " `" + artwork.getTitle() + "`");
            addArtwork(artwork, providerClient);
        }
    }

    private void addArtwork(Artwork artwork, ProviderClient providerClient) {
        if (!mIgnoreTokenList.contains(artwork.getToken())) {
            mArtworkSubmitCount += 1;
            providerClient.addArtwork(artwork);
        }
    }
}
