package rocks.tbog.livewallpaperit;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

public class Utils {
    private static final String TAG = Utils.class.getSimpleName();

    @NonNull
    public static String loadRedditAuth(@NonNull Context context) {
        StringBuilder textBuilder = new StringBuilder();
        try (Reader reader = new BufferedReader(new InputStreamReader(context.getAssets().open("reddit_auth.txt")))) {
            int c;
            while ((c = reader.read()) != -1) {
                textBuilder.append((char) c);
            }
        } catch (IOException e) {
            Log.e(TAG, "reddit_auth");
            return "";
        }
        int pos = textBuilder.lastIndexOf("\"");
        return textBuilder.substring(textBuilder.lastIndexOf("\"", pos - 1) + 1, pos);
    }
}
