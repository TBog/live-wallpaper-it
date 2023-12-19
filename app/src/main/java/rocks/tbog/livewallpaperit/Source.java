package rocks.tbog.livewallpaperit;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.Objects;

public class Source implements RecycleAdapterBase.AdapterDiff, Parcelable {
    @NonNull
    public String subreddit;

    public int minUpvotePercentage = 0;
    public int minScore = 0;
    public int minComments = 0;
    public int imageMinWidth = 0;
    public int imageMinHeight = 0;
    public Orientation imageOrientation = Orientation.ANY;
    public boolean isEnabled = true;

    protected Source(Parcel in) {
        subreddit = Objects.requireNonNull(in.readString());
        minUpvotePercentage = in.readInt();
        minScore = in.readInt();
        minComments = in.readInt();
        imageMinWidth = in.readInt();
        imageMinHeight = in.readInt();
        imageOrientation = Orientation.fromInt(in.readByte());
        isEnabled = in.readByte() != 0;
    }

    public static final Creator<Source> CREATOR = new Creator<>() {
        @Override
        public Source createFromParcel(Parcel in) {
            return new Source(in);
        }

        @Override
        public Source[] newArray(int size) {
            return new Source[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(subreddit);
        dest.writeInt(minUpvotePercentage);
        dest.writeInt(minScore);
        dest.writeInt(minComments);
        dest.writeInt(imageMinWidth);
        dest.writeInt(imageMinHeight);
        dest.writeByte((byte) imageOrientation.toInt());
        dest.writeByte((byte) (isEnabled ? 1 : 0));
    }

    @Override
    public long getAdapterItemId() {
        return subreddit.hashCode();
    }

    public enum Orientation {
        ANY(0),
        PORTRAIT(1),
        LANDSCAPE(2),
        SQUARE(3);

        private final int mValue;

        Orientation(int value) {
            mValue = value;
        }

        public static Orientation fromInt(int value) {
            for (var e : values()) {
                if (e.mValue == value) {
                    return e;
                }
            }
            return ANY;
        }

        public int toInt() {
            return mValue;
        }
    }

    public Source(@NonNull String subreddit) {
        this.subreddit = subreddit;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Source source = (Source) o;
        return minUpvotePercentage == source.minUpvotePercentage
                && minScore == source.minScore
                && minComments == source.minComments
                && imageMinWidth == source.imageMinWidth
                && imageMinHeight == source.imageMinHeight
                && isEnabled == source.isEnabled
                && Objects.equals(subreddit, source.subreddit)
                && imageOrientation == source.imageOrientation;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                subreddit,
                minUpvotePercentage,
                minScore,
                minComments,
                imageMinWidth,
                imageMinHeight,
                imageOrientation,
                isEnabled);
    }

    @NonNull
    public static byte[] toByteArray(@NonNull Source source) {
        Parcel p = Parcel.obtain();
        source.writeToParcel(p, 0);
        byte[] data = p.marshall();
        p.recycle();
        return data;
    }

    @Nullable
    public static Source fromByteArray(byte[] data) {
        if (data == null) return null;
        Parcel p = Parcel.obtain();
        p.unmarshall(data, 0, data.length);
        p.setDataPosition(0);
        Source source = Source.CREATOR.createFromParcel(p);
        p.recycle();
        return source;
    }
}
