package rocks.tbog.livewallpaperit.data;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import java.util.Objects;

public class Image implements Parcelable {
    @NonNull
    public final String url;

    @NonNull
    public final String mediaId;

    public final int width;
    public final int height;
    public final boolean isObfuscated;
    public final boolean isSource;

    public Image(
            @NonNull String url, @NonNull String mediaId, int width, int height, boolean obfuscated, boolean isSource) {
        this.url = url;
        this.mediaId = mediaId;
        this.width = width;
        this.height = height;
        this.isObfuscated = obfuscated;
        this.isSource = isSource;
    }

    protected Image(Parcel in) {
        url = Objects.requireNonNull(in.readString());
        mediaId = Objects.requireNonNull(in.readString());
        width = in.readInt();
        height = in.readInt();
        byte bits = in.readByte();
        isObfuscated = (bits & 2) == 2;
        isSource = (bits & 1) == 1;
    }

    public static final Creator<Image> CREATOR = new Creator<>() {
        @Override
        public Image createFromParcel(Parcel in) {
            return new Image(in);
        }

        @Override
        public Image[] newArray(int size) {
            return new Image[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(url);
        dest.writeString(mediaId);
        dest.writeInt(width);
        dest.writeInt(height);
        byte bits = (byte) ((isObfuscated ? 1 : 0) << 1 | (isSource ? 1 : 0));
        dest.writeByte((byte) bits);
    }

    public static Image fromCursor(Cursor cursor) {
        String url = SubTopic.getStringFromCursor(cursor, RedditDatabase.IMAGE_URL);
        String mediaId = SubTopic.getStringFromCursor(cursor, RedditDatabase.IMAGE_MEDIA_ID);
        int width = SubTopic.getIntFromCursor(cursor, RedditDatabase.IMAGE_WIDTH);
        int height = SubTopic.getIntFromCursor(cursor, RedditDatabase.IMAGE_HEIGHT);
        boolean isObfuscated = 0 != SubTopic.getIntFromCursor(cursor, RedditDatabase.IMAGE_IS_BLUR);
        boolean isSource = 0 != SubTopic.getIntFromCursor(cursor, RedditDatabase.IMAGE_IS_SOURCE);
        return new Image(url, mediaId, width, height, isObfuscated, isSource);
    }
}
