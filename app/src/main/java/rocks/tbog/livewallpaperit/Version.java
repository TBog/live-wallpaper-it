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
        // if (version == null) throw new IllegalArgumentException("Version can not be null");
        // if (!version.matches("[0-9]+(\\.[0-9]+)*")) throw new IllegalArgumentException("Invalid version format");
        this.version = version;
    }

    @Override
    public int compareTo(Version that) {
        if (that == null) return 1;
        String[] thisParts = this.get().split("\\.");
        String[] thatParts = that.get().split("\\.");
        int length = Math.max(thisParts.length, thatParts.length);
        for (int i = 0; i < length; i++) {
            int thisPart;
            if (i < thisParts.length) {
                thisPart = Integer.parseInt(thisParts[i]);
            } else {
                thisPart = 0;
            }
            int thatPart;
            if (i < thatParts.length) {
                thatPart = Integer.parseInt(thatParts[i]);
            } else {
                thatPart = 0;
            }
            if (thisPart < thatPart) return -1;
            if (thisPart > thatPart) return 1;
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
