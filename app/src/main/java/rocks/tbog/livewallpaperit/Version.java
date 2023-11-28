package rocks.tbog.livewallpaperit;

import androidx.annotation.NonNull;

/**
 * How do you compare two version Strings in Java?
 * https://stackoverflow.com/questions/198431/how-do-you-compare-two-version-strings-in-java
 */
public class Version implements Comparable<Version> {

    private final String version;

    public final String get() {
        return this.version;
    }

    public Version(@NonNull String version) {
        this.version = version;
    }

    @Override
    public int compareTo(Version that) {
        if (that == null) return 1;
        String[] thisParts = this.version.split("\\.");
        String[] thatParts = that.version.split("\\.");
        int length = Math.max(thisParts.length, thatParts.length);
        for (int i = 0; i < length; i++) {
            int thisPart = getIntegerFromPart(thisParts, i);
            int thatPart = getIntegerFromPart(thatParts, i);
            if (thisPart < thatPart) return -1;
            if (thisPart > thatPart) return 1;
        }
        return 0;
    }

    private static int getIntegerFromPart(@NonNull String[] parts, int idx) {
        if (idx < parts.length) {
            try {
                return Integer.parseInt(parts[idx]);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) return true;
        if (that == null) return false;
        if (this.getClass() != that.getClass()) return false;
        return this.compareTo((Version) that) == 0;
    }
}
