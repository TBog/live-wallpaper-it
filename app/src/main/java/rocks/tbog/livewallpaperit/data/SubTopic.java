package rocks.tbog.livewallpaperit.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.kirkbushman.araw.models.GalleryData;
import com.kirkbushman.araw.models.GalleryImageData;
import com.kirkbushman.araw.models.GalleryMedia;
import com.kirkbushman.araw.models.GalleryMediaItem;
import com.kirkbushman.araw.models.Submission;
import com.kirkbushman.araw.models.commons.ImageDetail;
import com.kirkbushman.araw.models.commons.SubmissionPreview;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class SubTopic {
    private static final String TAG = SubTopic.class.getSimpleName();

    @NonNull
    public final String id;

    @NonNull
    public final String title;

    @NonNull
    public final String author;

    public final String linkFlairText;
    public final String permalink;
    public final String thumbnail;
    public final long createdUTC;
    public final int score;
    public final int upvoteRatio;
    public final int numComments;
    public final boolean over18;
    public final ArrayList<Image> images = new ArrayList<>();

    private SubTopic(
            @NonNull String id,
            @NonNull String title,
            @NonNull String author,
            String linkFlairText,
            String permalink,
            String thumbnail,
            long createdUTC,
            int score,
            int upvoteRatio,
            int numComments,
            boolean over18) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.linkFlairText = linkFlairText;
        this.permalink = permalink;
        this.thumbnail = thumbnail;
        this.createdUTC = createdUTC;
        this.score = score;
        this.upvoteRatio = upvoteRatio;
        this.numComments = numComments;
        this.over18 = over18;
    }

    public static SubTopic fromSubmission(Submission submission) {
        String id = submission.getId();
        String title = submission.getTitle();
        String author = submission.getAuthor();
        String linkFlairText = submission.getLinkFlairText();
        String permalink = submission.getPermalink();
        String thumbnail = submission.getThumbnailUrl();
        long createdUTC = submission.getCreatedUtc();
        int score = submission.getScore();
        int upvoteRatio;
        if (submission.getUpvoteRatio() != null) {
            upvoteRatio = Math.round(submission.getUpvoteRatio() * 100f);
        } else {
            upvoteRatio = 0;
        }
        int numComments = submission.getNumComments();
        boolean over18 = submission.getOver18();
        return new SubTopic(
                        id,
                        title,
                        author,
                        linkFlairText,
                        permalink,
                        thumbnail,
                        createdUTC,
                        score,
                        upvoteRatio,
                        numComments,
                        over18)
                .addImages(submission.getPreview(), submission.getId())
                .addImages(submission.getGalleryData(), submission.getMediaMetadata());
    }

    public static SubTopic fromCursor(Cursor cursor) {
        final String[] columnNames = cursor.getColumnNames();
        String id = getStringFromCursor(cursor, columnNames, RedditDatabase.TOPIC_ID);
        String title = getStringFromCursor(cursor, columnNames, RedditDatabase.TOPIC_TITLE);
        String author = getStringFromCursor(cursor, columnNames, RedditDatabase.TOPIC_AUTHOR);
        String linkFlairText = getStringFromCursor(cursor, columnNames, RedditDatabase.TOPIC_LINK_FLAIR_TEXT);
        String permalink = getStringFromCursor(cursor, columnNames, RedditDatabase.TOPIC_PERMALINK);
        String thumbnail = getStringFromCursor(cursor, columnNames, RedditDatabase.TOPIC_THUMBNAIL);
        long createdUTC = getLongFromCursor(cursor, columnNames, RedditDatabase.TOPIC_CREATED_UTC);
        int score = getIntFromCursor(cursor, columnNames, RedditDatabase.TOPIC_SCORE);
        int upvoteRatio = getIntFromCursor(cursor, columnNames, RedditDatabase.TOPIC_UPVOTE_RATIO);
        int numComments = getIntFromCursor(cursor, columnNames, RedditDatabase.TOPIC_NUM_COMMENTS);
        boolean over18 = 0 != getIntFromCursor(cursor, columnNames, RedditDatabase.TOPIC_OVER_18);
        return new SubTopic(
                id,
                title,
                author,
                linkFlairText,
                permalink,
                thumbnail,
                createdUTC,
                score,
                upvoteRatio,
                numComments,
                over18);
    }

    @NonNull
    private static String getStringFromCursor(Cursor cursor, String[] columnNames, @NonNull String column) {
        for (int idx = 0; idx < columnNames.length; idx += 1) {
            if (column.equals(columnNames[idx])) {
                return cursor.getString(idx);
            }
        }
        Log.e(TAG, "cursor missing `" + column + "` (string) in " + Arrays.toString(columnNames));
        return "";
    }

    private static int getIntFromCursor(Cursor cursor, String[] columnNames, @NonNull String column) {
        for (int idx = 0; idx < columnNames.length; idx += 1) {
            if (column.equals(columnNames[idx])) {
                return cursor.getInt(idx);
            }
        }
        Log.e(TAG, "cursor missing `" + column + "` (int) in " + Arrays.toString(columnNames));
        return 0;
    }

    private static long getLongFromCursor(Cursor cursor, String[] columnNames, @NonNull String column) {
        for (int idx = 0; idx < columnNames.length; idx += 1) {
            if (column.equals(columnNames[idx])) {
                return cursor.getLong(idx);
            }
        }
        Log.e(TAG, "cursor missing `" + column + "` (int) in " + Arrays.toString(columnNames));
        return 0;
    }

    private SubTopic addImages(GalleryData galleryData, Map<String, GalleryMedia> mediaMetadata) {
        if (galleryData == null || mediaMetadata == null) return this;
        List<GalleryMediaItem> mediaItems = galleryData.getItems();
        for (GalleryMediaItem item : mediaItems) {
            GalleryMedia media = mediaMetadata.get(item.getMediaId());
            if (media == null || !"Image".equalsIgnoreCase(media.getE())) continue;

            Image source = makeImage(media.getS(), item.getMediaId(), false, true);
            if (source != null) images.add(source);
            addResolutions(media.getP(), item.getMediaId(), false);
            addResolutions(media.getO(), item.getMediaId(), true);
        }
        return this;
    }

    private SubTopic addImages(@Nullable SubmissionPreview preview, @NonNull String submissionId) {
        if (preview == null) return this;
        for (var img : preview.getImages()) {
            String mediaId;
            if (TextUtils.isEmpty(img.getId())) {
                mediaId = submissionId;
            } else {
                mediaId = img.getId();
            }
            if (images.stream().anyMatch(image -> image.mediaId.equals(mediaId))) continue;
            images.add(makeImage(img.getSource(), mediaId, false, true));
            addResolutions(img.getResolutions(), mediaId, false);
            var variants = img.getVariants();
            if (variants != null) {
                var obfuscated = variants.getObfuscated();
                if (obfuscated != null) {
                    images.add(makeImage(obfuscated.getSource(), mediaId, true, true));
                    addResolutions(obfuscated.getResolutions(), mediaId, true);
                }
            }
        }
        return this;
    }

    @Nullable
    private Image makeImage(
            @Nullable GalleryImageData source, @NonNull String mediaId, boolean obfuscated, boolean isSource) {
        if (source == null) return null;
        var width = source.getX();
        var height = source.getY();
        if (width == null || height == null) return null;
        var url = source.getU();
        if (TextUtils.isEmpty(url)) return null;
        return new Image(url, mediaId, width, height, obfuscated, isSource);
    }

    @NonNull
    private Image makeImage(@NonNull ImageDetail image, @NonNull String mediaId, boolean obfuscated, boolean isSource) {
        return new Image(image.getUrl(), mediaId, image.getWidth(), image.getHeight(), obfuscated, isSource);
    }

    private void addResolutions(@Nullable List<GalleryImageData> list, @NonNull String mediaId, boolean obfuscated) {
        if (list == null || list.isEmpty()) return;
        for (var resolution : list) {
            var image = makeImage(resolution, mediaId, obfuscated, false);
            addResolutionUnique(image);
        }
    }

    private void addResolutions(@NonNull ImageDetail[] resolutions, @NonNull String mediaId, boolean obfuscated) {
        for (var resolution : resolutions) {
            var image = makeImage(resolution, mediaId, obfuscated, false);
            addResolutionUnique(image);
        }
    }

    private void addResolutionUnique(@Nullable final Image resolution) {
        if (resolution == null) return;
        if (images.stream()
                .anyMatch(image -> resolution.width == image.width
                        && resolution.height == image.height
                        && resolution.isObfuscated == image.isObfuscated
                        && resolution.isSource == image.isSource
                        && image.mediaId.equals(resolution.mediaId))) {
            return;
        }
        images.add(resolution);
    }

    public void fillTopicValues(ContentValues value) {
        value.put(RedditDatabase.TOPIC_ID, id);
        value.put(RedditDatabase.TOPIC_TITLE, title);
        value.put(RedditDatabase.TOPIC_AUTHOR, author);
        value.put(RedditDatabase.TOPIC_LINK_FLAIR_TEXT, linkFlairText);
        value.put(RedditDatabase.TOPIC_PERMALINK, permalink);
        value.put(RedditDatabase.TOPIC_THUMBNAIL, thumbnail);
        value.put(RedditDatabase.TOPIC_CREATED_UTC, createdUTC);
        value.put(RedditDatabase.TOPIC_SCORE, score);
        value.put(RedditDatabase.TOPIC_UPVOTE_RATIO, upvoteRatio);
        value.put(RedditDatabase.TOPIC_NUM_COMMENTS, numComments);
        value.put(RedditDatabase.TOPIC_OVER_18, over18);
    }

    public void fillImageValues(Image image, ContentValues value) {
        value.put(RedditDatabase.IMAGE_TOPIC_ID, id);
        value.put(RedditDatabase.IMAGE_URL, image.url);
        value.put(RedditDatabase.IMAGE_MEDIA_ID, image.mediaId);
        value.put(RedditDatabase.IMAGE_WIDTH, image.width);
        value.put(RedditDatabase.IMAGE_HEIGHT, image.height);
        value.put(RedditDatabase.IMAGE_IS_NSFW, image.isObfuscated);
        value.put(RedditDatabase.IMAGE_IS_SOURCE, image.isSource);
    }

    public static class Image {
        @NonNull
        public final String url;

        @NonNull
        public final String mediaId;

        public final int width;
        public final int height;
        public final boolean isObfuscated;
        public final boolean isSource;

        public Image(
                @NonNull String url,
                @NonNull String mediaId,
                int width,
                int height,
                boolean obfuscated,
                boolean isSource) {
            this.url = url;
            this.mediaId = mediaId;
            this.width = width;
            this.height = height;
            this.isObfuscated = obfuscated;
            this.isSource = isSource;
        }

        public static Image fromCursor(Cursor cursor) {
            final String[] columnNames = cursor.getColumnNames();
            String url = getStringFromCursor(cursor, columnNames, RedditDatabase.IMAGE_URL);
            String mediaId = getStringFromCursor(cursor, columnNames, RedditDatabase.IMAGE_MEDIA_ID);
            int width = getIntFromCursor(cursor, columnNames, RedditDatabase.IMAGE_WIDTH);
            int height = getIntFromCursor(cursor, columnNames, RedditDatabase.IMAGE_HEIGHT);
            boolean isObfuscated = 0 != getIntFromCursor(cursor, columnNames, RedditDatabase.IMAGE_IS_NSFW);
            boolean isSource = 0 != getIntFromCursor(cursor, columnNames, RedditDatabase.IMAGE_IS_SOURCE);
            return new Image(url, mediaId, width, height, isObfuscated, isSource);
        }
    }
}
