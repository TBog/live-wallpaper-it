package rocks.tbog.livewallpaperit.work;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import java.util.Objects;
import rocks.tbog.livewallpaperit.ArtProvider;
import rocks.tbog.livewallpaperit.Source;
import rocks.tbog.livewallpaperit.data.DBHelper;
import rocks.tbog.livewallpaperit.data.SubTopic;

public class ArtLoadWorker extends Worker {
    private static final String TAG = ArtLoadWorker.class.getSimpleName();
    private static final int FETCH_AMOUNT = 100;
    private static final int MAX_FETCHES = 3;
    private List<String> mIgnoreTokenList = Collections.emptyList();
    private int mArtworkSubmitCount = 0;
    private int mArtworkNotFoundCount = 0;

    private Filter mFilter = null;

    public static final class Filter {
        public int minUpvotePercentage = 0;
        public int minScore = 0;
        public int minComments = 0;
        public boolean allowNSFW = true;

        @Nullable
        public static Filter fromSource(@Nullable Source source) {
            if (source == null) return null;
            Filter filter = new Filter();
            filter.minUpvotePercentage = source.minUpvotePercentage;
            filter.minScore = source.minScore;
            filter.minComments = source.minComments;
            return filter;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Filter filter = (Filter) o;
            return minUpvotePercentage == filter.minUpvotePercentage
                    && minScore == filter.minScore
                    && minComments == filter.minComments
                    && allowNSFW == filter.allowNSFW;
        }

        @Override
        public int hashCode() {
            return Objects.hash(minUpvotePercentage, minScore, minComments, allowNSFW);
        }
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

        mFilter = Filter.fromSource(source);
        if (mFilter != null) {
            mFilter.allowNSFW = getInputData().getBoolean(WorkerUtils.DATA_ALLOW_NSFW, false);
        }

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
                if (topic.images.isEmpty() || isRemovedOrDeleted(submission)) {
                    mArtworkNotFoundCount += 1;
                    if (DBHelper.removeSubTopic(getApplicationContext(), topic)) {
                        Log.v(TAG, "removed `" + topic.permalink + "`");
                    }
                    continue;
                }
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

    private static boolean isRemovedOrDeleted(@NonNull Submission submission) {
        /*
        According to a Reddit developer post https://www.reddit.com/r/redditdev/comments/kypjmk/check_if_submission_has_been_removed_by_a_mod/
        The following are the possible values for the removed_by_category field:

        moderator: The post was removed by a moderator.
        deleted: The post was deleted by the author.
        author: The post was removed by the author of the post.
        spam: The post was removed by Reddit’s spam filters.
        admin: The post was removed by an administrator.
        anti_evil_ops: The post was removed by Anti-Evil Operations due to violations of Reddit’s content policy.
        community_ops: The post was removed by a member of the community team while acting as an admin.
        legal_operations: The post was removed by the legal team for non-copyright related reasons.
        copyright_takedown: The post was removed for copyright infringement. The content is not removed in the traditional sense, but rather is censored with the text “[ Removed by reddit in response to a copyright notice. ]”.
        banned_by: The post was removed by a moderator who is not the author of the post.
        */
        return submission.getRemovedByCategory() != null;
    }

    public static boolean shouldSkipTopic(@NonNull SubTopic topic, @Nullable Filter filter) {
        if (filter == null) return false;

        if (filter.minUpvotePercentage > 0) {
            if (topic.upvoteRatio < filter.minUpvotePercentage) {
                Log.v(
                        TAG,
                        "upvote " + topic.upvoteRatio + "%<" + filter.minUpvotePercentage + "% skipping "
                                + topic.permalink);
                return true;
            }
        }
        if (filter.minScore > 0) {
            if (topic.score < filter.minScore) {
                Log.v(TAG, "score " + topic.score + "<" + filter.minScore + " skipping " + topic.permalink);
                return true;
            }
        }
        if (filter.minComments > 0) {
            if (topic.numComments < filter.minComments) {
                Log.v(
                        TAG,
                        "numComments " + topic.numComments + "<" + filter.minComments + " skipping " + topic.permalink);
                return true;
            }
        }
        if (!filter.allowNSFW) {
            if (topic.over18) {
                Log.v(TAG, "NSFW not allowed skipping " + topic.permalink);
                return true;
            }
        }
        return false;
    }

    private void getArtworks(String subredditNamePrefixed, @NonNull SubTopic topic, ProviderClient providerClient) {
        if (shouldSkipTopic(topic, mFilter)) return;

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
