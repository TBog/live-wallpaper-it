package rocks.tbog.livewallpaperit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Objects;

public class Source implements Serializable, RecycleAdapterBase.AdapterDiff {
    private static final long serialVersionUID = 1L;

    @NonNull
    public String subreddit;

    public int minUpvotePercentage = 0;
    public int minScore = 0;
    public int minComments = 0;
    public int imageMinWidth = 0;
    public int imageMinHeight = 0;
    public Orientation imageOrientation = Orientation.ANY;
    public boolean isEnabled = true;

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
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(outputStream);
            oos.writeObject(source);
            oos.close();
        } catch (Exception ignored) {
            // ignored
        }
        return outputStream.toByteArray();
    }

    @Nullable
    public static Source fromByteArray(byte[] data) {
        if (data == null) return null;
        Source source;
        try {
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
            source = (Source) ois.readObject();
            ois.close();
        } catch (Exception ignored) {
            source = null;
        }
        return source;
    }
}
