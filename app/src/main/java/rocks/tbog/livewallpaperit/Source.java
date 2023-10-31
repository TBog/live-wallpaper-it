package rocks.tbog.livewallpaperit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Objects;

public class Source implements Serializable {
    private static final long serialVersionUID = 1L;

    @NonNull
    public String subreddit;

    public int minUpvotePercentage = 0;
    public int minScore = 0;
    public int minComments = 0;

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
                && subreddit.equals(source.subreddit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subreddit, minUpvotePercentage, minScore, minComments);
    }

    @NonNull
    public static byte[] toByteArray(@NonNull Source source) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(outputStream);
            oos.writeObject(source);
            oos.close();
        } catch (Exception ignored) {
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