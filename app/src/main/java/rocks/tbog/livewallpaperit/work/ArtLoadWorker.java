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
import com.kirkbushman.araw.fetcher.SubmissionsFetcher;
import com.kirkbushman.araw.helpers.AuthUserlessHelper;
import com.kirkbushman.araw.models.GalleryData;
import com.kirkbushman.araw.models.GalleryImageData;
import com.kirkbushman.araw.models.GalleryMedia;
import com.kirkbushman.araw.models.GalleryMediaItem;
import com.kirkbushman.araw.models.Submission;
import com.kirkbushman.araw.models.commons.ImageDetail;
import com.kirkbushman.araw.models.commons.Images;
import com.kirkbushman.araw.models.commons.SubmissionPreview;
import com.kirkbushman.araw.models.enums.SubmissionsSorting;
import com.kirkbushman.araw.models.enums.TimePeriod;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import rocks.tbog.livewallpaperit.ArtProvider;
import rocks.tbog.livewallpaperit.Source;

public class ArtLoadWorker extends Worker {
    private static final String TAG = ArtLoadWorker.class.getSimpleName();
    private static final int LOAD_COUNT = 10;
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
        ProviderClient providerClient = ProviderContract.getProviderClient(ctx, ArtProvider.class);

        SubmissionsFetcher submissionsFetcher = client.getSubredditsClient()
                .createSubmissionsFetcher(
                        source.subreddit,
                        SubmissionsSorting.NEW,
                        TimePeriod.ALL_TIME,
                        LOAD_COUNT); // Fetcher.MAX_LIMIT);
        Log.d(TAG, "fetch " + submissionsFetcher.getSubreddit());
        List<Submission> submissions = submissionsFetcher.fetchNext();
        while (submissions != null) {
            for (Submission submission : submissions) {
                processSubmission(submission, providerClient);
            }

            if (mArtworkSubmitCount < LOAD_COUNT && submissionsFetcher.hasNext()) {
                Log.d(
                        TAG,
                        "fetchNext " + submissionsFetcher.getSubreddit() + "; artworkSubmitCount="
                                + mArtworkSubmitCount);
                submissions = submissionsFetcher.fetchNext();
            } else {
                submissions = null;
            }
        }

        Log.i(TAG, "artworkSubmitCount=" + mArtworkSubmitCount + " artworkNotFoundCount=" + mArtworkNotFoundCount);
        return Result.success(new Data.Builder()
                .putInt(WorkerUtils.DATA_ARTWORK_SUBMIT_COUNT, mArtworkSubmitCount)
                .putInt(WorkerUtils.DATA_ARTWORK_NOT_FOUND_COUNT, mArtworkNotFoundCount)
                .build());
    }

    private void processSubmission(Submission submission, ProviderClient providerClient) {
        if (mFilter != null) {
            if (mFilter.minUpvotePercentage > 0) {
                Float upvoteRatio = submission.getUpvoteRatio();
                if (upvoteRatio == null) upvoteRatio = 0f;
                int upvotePercent = (int) (upvoteRatio * 100f);
                if (upvotePercent < mFilter.minUpvotePercentage) {
                    Log.v(
                            TAG,
                            "upvote " + upvotePercent + "%<" + mFilter.minUpvotePercentage + "% skipping "
                                    + submission.getPermalink());
                    return;
                }
            }
            if (mFilter.minScore > 0) {
                int score = submission.getScore();
                if (score < mFilter.minScore) {
                    Log.v(TAG, "score " + score + "<" + mFilter.minScore + " skipping " + submission.getPermalink());
                    return;
                }
            }
            if (mFilter.minComments > 0) {
                int numComments = submission.getNumComments();
                if (numComments < mFilter.minComments) {
                    Log.v(
                            TAG,
                            "numComments " + numComments + "<" + mFilter.minComments + " skipping "
                                    + submission.getPermalink());
                    return;
                }
            }
            if (!mFilter.allowNSFW) {
                if (submission.getOver18()) {
                    Log.v(TAG, "NSFW not allowed skipping " + submission.getPermalink());
                    return;
                }
            }
        }
        boolean artworkFound = false;
        SubmissionPreview preview = submission.getPreview();
        Images[] imagesArray;
        if (preview != null) {
            imagesArray = preview.getImages();
        } else {
            imagesArray = new Images[0];
        }
        for (Images images : imagesArray) {
            ImageDetail imageDetail = images.getSource();
            String byline = submission.getLinkFlairText();
            if (TextUtils.isEmpty(byline)) byline = submission.getSubredditNamePrefixed();
            Artwork artwork = new Artwork.Builder()
                    .persistentUri(Uri.parse(imageDetail.getUrl()))
                    .webUri(Uri.parse("https://www.reddit.com" + submission.getPermalink()))
                    .token(submission.getId())
                    .attribution(submission.getAuthor())
                    .byline(byline)
                    .title(submission.getTitle())
                    .build();

            Log.v(TAG, "(image)addArtwork " + artwork.getToken() + " `" + artwork.getTitle() + "`");
            artworkFound = true;
            addArtwork(artwork, providerClient);
        }

        if (!artworkFound && submission.isRedditMediaDomain()) {
            String byline = submission.getLinkFlairText();
            if (TextUtils.isEmpty(byline)) byline = submission.getSubredditNamePrefixed();
            Artwork artwork = new Artwork.Builder()
                    .persistentUri(Uri.parse(submission.getUrl()))
                    .webUri(Uri.parse("https://www.reddit.com" + submission.getPermalink()))
                    .token(submission.getId())
                    .attribution(submission.getAuthor())
                    .byline(byline)
                    .title(submission.getTitle())
                    .build();

            Log.v(TAG, "(media)addArtwork " + artwork.getToken() + " `" + artwork.getTitle() + "`");
            artworkFound = true;
            addArtwork(artwork, providerClient);
        }

        if (artworkFound) return;

        GalleryData galleryData = submission.getGalleryData();
        if (galleryData == null) {
            Log.d(TAG, "galleryData == null");
            return;
        }
        Map<String, GalleryMedia> mediaMetadata = submission.getMediaMetadata();
        if (mediaMetadata == null) {
            Log.d(TAG, "mediaMetadata == null");
            return;
        }

        List<GalleryMediaItem> mediaItems = galleryData.getItems();
        for (GalleryMediaItem item : mediaItems) {
            GalleryMedia media = mediaMetadata.get(item.getMediaId());
            if (media == null || !"Image".equalsIgnoreCase(media.getE())) continue;
            GalleryImageData imageData = media.getS();
            if (imageData == null || TextUtils.isEmpty(imageData.getU())) continue;

            Artwork artwork = new Artwork.Builder()
                    .persistentUri(Uri.parse(imageData.getU()))
                    .webUri(Uri.parse("https://www.reddit.com" + submission.getPermalink()))
                    .token(media.getId())
                    .attribution(submission.getAuthor())
                    .byline(submission.getLinkFlairText())
                    .title(submission.getTitle())
                    .build();

            Log.v(TAG, "(gallery)addArtwork " + artwork.getToken() + " `" + artwork.getTitle() + "`");
            artworkFound = true;
            addArtwork(artwork, providerClient);
        }
        if (!artworkFound) {
            mArtworkNotFoundCount += 1;
        }
    }

    private void addArtwork(Artwork artwork, ProviderClient providerClient) {
        if (!mIgnoreTokenList.contains(artwork.getToken())) {
            mArtworkSubmitCount += 1;
            providerClient.addArtwork(artwork);
        }
    }
}
