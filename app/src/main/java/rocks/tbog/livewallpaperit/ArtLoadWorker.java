package rocks.tbog.livewallpaperit;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ArtLoadWorker extends Worker {
    private static final String TAG = ArtLoadWorker.class.getSimpleName();

    public ArtLoadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context ctx = getApplicationContext();

        String clientId = getInputData().getString("clientId");
        if (TextUtils.isEmpty(clientId))
            return Result.failure();

        var helper = new AuthUserlessHelper(ctx, clientId, "DO_NOT_TRACK_THIS_DEVICE", true, true);
        if (!helper.shouldLogin()) {
            // use saved one
            Log.i(TAG, String.valueOf(helper));
        } else {
            // you must authenticate
            Log.i(TAG, "you must authenticate");
        }

        // obtain a client
        RedditClient client = helper.getRedditClient();
        if (client == null)
            return Result.failure();

        ProviderClient providerClient = ProviderContract.getProviderClient(ctx, ArtProvider.class);

        SubmissionsFetcher submissionsFetcher = client.getSubredditsClient().createSubmissionsFetcher("AnimeWallpapers", SubmissionsSorting.NEW, TimePeriod.ALL_TIME, 10);//Fetcher.MAX_LIMIT);
        List<Submission> submissions = submissionsFetcher.fetchNext();
        if (submissions == null)
            submissions = Collections.emptyList();
        for (Submission submission : submissions) {
            boolean artworkSubmitted = false;
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

                Log.d(TAG, "addArtwork " + artwork.getToken() + " " + artwork.getTitle());
                providerClient.addArtwork(artwork);
                artworkSubmitted = true;
            }

            if (!artworkSubmitted && submission.isRedditMediaDomain()) {
                Artwork artwork = new Artwork.Builder()
                        .persistentUri(Uri.parse(submission.getUrl()))
                        .webUri(Uri.parse("https://www.reddit.com" + submission.getPermalink()))
                        .token(submission.getId())
                        .attribution(submission.getAuthor())
                        .byline(submission.getLinkFlairText())
                        .title(submission.getTitle())
                        .build();

                Log.d(TAG, "addArtwork " + artwork.getToken() + " " + artwork.getTitle());
                providerClient.addArtwork(artwork);
                artworkSubmitted = true;
            }

            if (artworkSubmitted)
                continue;

            GalleryData galleryData = submission.getGalleryData();
            if (galleryData == null) {
                Log.d(TAG, "galleryData == null");
                continue;
            }
            Map<String, GalleryMedia> mediaMetadata = submission.getMediaMetadata();
            if (mediaMetadata == null) {
                Log.d(TAG, "mediaMetadata == null");
                continue;
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

                Log.d(TAG, "addArtwork " + artwork.getToken() + " " + artwork.getTitle());
                providerClient.addArtwork(artwork);
                artworkSubmitted = true;
            }
        }

        return Result.success();
    }
}
