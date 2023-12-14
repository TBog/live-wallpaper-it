package rocks.tbog.livewallpaperit.data;

import android.content.ContentValues;
import androidx.annotation.NonNull;
import java.util.Objects;

public final class MediaInfo {
    @NonNull
    public final String mediaId;

    @NonNull
    public final String topicId;

    @NonNull
    public final String subreddit;

    public MediaInfo(@NonNull String mediaId, @NonNull String topicId, @NonNull String subreddit) {
        this.mediaId = mediaId;
        this.topicId = topicId;
        this.subreddit = subreddit;
    }

    public void fillValues(ContentValues values) {
        values.put(RedditDatabase.FAVORITE_MEDIA_ID, mediaId);
        values.put(RedditDatabase.FAVORITE_TOPIC_ID, topicId);
        values.put(RedditDatabase.FAVORITE_SUBREDDIT, subreddit);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MediaInfo info = (MediaInfo) o;
        return Objects.equals(mediaId, info.mediaId)
                && Objects.equals(topicId, info.topicId)
                && Objects.equals(subreddit, info.subreddit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mediaId, topicId, subreddit);
    }
}
