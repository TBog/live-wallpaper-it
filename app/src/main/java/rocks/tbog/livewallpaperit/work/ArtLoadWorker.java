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

public class ArtLoadWorker extends Worker {
    private static final String TAG = ArtLoadWorker.class.getSimpleName();
    private static final int LOAD_COUNT = 10;
    private List<String> mIgnoreTokenList = Collections.emptyList();
    private int mArtworkSubmitCount = 0;
    private int mArtworkNotFoundCount = 0;

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

        String subreddit = getInputData().getString(WorkerUtils.DATA_SUBREDDIT);
        if (TextUtils.isEmpty(subreddit)) {
            Log.e(TAG, "subreddit=`" + subreddit + "`");
            return Result.failure(new Data.Builder().putString(WorkerUtils.FAIL_REASON, "empty subreddit").build());
        }
        String clientId = getInputData().getString(WorkerUtils.DATA_CLIENT_ID);
        if (TextUtils.isEmpty(clientId)) {
            return Result.failure(new Data.Builder().putString(WorkerUtils.FAIL_REASON, "empty clientId").build());
        }

        var helper = new AuthUserlessHelper(ctx, clientId, "DO_NOT_TRACK_THIS_DEVICE", false, true);
        // obtain a client
        RedditClient client = helper.getRedditClient();
        if (client == null)
            return Result.failure(new Data.Builder().putString(WorkerUtils.FAIL_REASON, "getRedditClient=null").build());

        ProviderClient providerClient = ProviderContract.getProviderClient(ctx, ArtProvider.class);

        SubmissionsFetcher submissionsFetcher = client.getSubredditsClient().createSubmissionsFetcher(subreddit, SubmissionsSorting.NEW, TimePeriod.ALL_TIME, LOAD_COUNT);//Fetcher.MAX_LIMIT);
        List<Submission> submissions = submissionsFetcher.fetchNext();
        while (submissions != null) {
            for (Submission submission : submissions) {
                processSubmission(submission, providerClient);
            }

            if (mArtworkSubmitCount < LOAD_COUNT && submissionsFetcher.hasNext()) {
                Log.v(TAG, "fetchNext; artworkSubmitCount=" + mArtworkSubmitCount);
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
        boolean artworkFound = false;
        SubmissionPreview preview = submission.getPreview();
        Images[] imagesArray = preview != null ? preview.getImages() : new Images[0];
        for (Images images : imagesArray) {
            ImageDetail imageDetail = images.getSource();
            Artwork artwork = new Artwork.Builder()
                    .persistentUri(Uri.parse(imageDetail.getUrl()))
                    .webUri(Uri.parse("https://www.reddit.com" + submission.getPermalink()))
                    .token(submission.getId())
                    .attribution(submission.getAuthor())
                    .byline(submission.getLinkFlairText())
                    .title(submission.getTitle())
                    .build();

            Log.v(TAG, "addArtwork " + artwork.getToken() + " " + artwork.getTitle());
            artworkFound = true;
            if (!mIgnoreTokenList.contains(artwork.getToken())) {
                mArtworkSubmitCount += 1;
                providerClient.addArtwork(artwork);
            }
        }

        if (!artworkFound && submission.isRedditMediaDomain()) {
            Artwork artwork = new Artwork.Builder()
                    .persistentUri(Uri.parse(submission.getUrl()))
                    .webUri(Uri.parse("https://www.reddit.com" + submission.getPermalink()))
                    .token(submission.getId())
                    .attribution(submission.getAuthor())
                    .byline(submission.getLinkFlairText())
                    .title(submission.getTitle())
                    .build();

            Log.v(TAG, "addArtwork " + artwork.getToken() + " " + artwork.getTitle());
            artworkFound = true;
            if (!mIgnoreTokenList.contains(artwork.getToken())) {
                mArtworkSubmitCount += 1;
                providerClient.addArtwork(artwork);
            }
        }

        if (artworkFound)
            return;

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
            Log.d(TAG, "mediaId: " + item.getMediaId());

            GalleryMedia media = mediaMetadata.get(item.getMediaId());
            if (media == null || !"Image".equalsIgnoreCase(media.getE()))
                continue;
            GalleryImageData imageData = media.getS();
            if (imageData == null || TextUtils.isEmpty(imageData.getU()))
                continue;

            Artwork artwork = new Artwork.Builder()
                    .persistentUri(Uri.parse(imageData.getU()))
                    .webUri(Uri.parse("https://www.reddit.com" + submission.getPermalink()))
                    .token(media.getId())
                    .attribution(submission.getAuthor())
                    .byline(submission.getLinkFlairText())
                    .title(submission.getTitle())
                    .build();

            Log.v(TAG, "addArtwork " + artwork.getToken() + " " + artwork.getTitle());
            artworkFound = true;
            if (!mIgnoreTokenList.contains(artwork.getToken())) {
                mArtworkSubmitCount += 1;
                providerClient.addArtwork(artwork);
            }
        }
        if (!artworkFound) {
            mArtworkNotFoundCount += 1;
        }
    }
}
