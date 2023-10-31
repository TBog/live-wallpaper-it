package rocks.tbog.livewallpaperit.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import rocks.tbog.livewallpaperit.Source;

public class DataUtils {
    private static final String TAG = DataUtils.class.getSimpleName();

    @NonNull
    public static String loadRedditAuth(@NonNull Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        String clientId = pref.getString("RedditAuth", null);
        if (!TextUtils.isEmpty(clientId) && pref.getBoolean("RedditAuth-verified", false)) {
            return clientId;
        }

        StringBuilder textBuilder = new StringBuilder();
        try (Reader reader =
                new BufferedReader(new InputStreamReader(context.getAssets().open("reddit_auth.txt")))) {
            int c;
            while ((c = reader.read()) != -1) {
                textBuilder.append((char) c);
            }
        } catch (IOException e) {
            Log.w(TAG, "reddit_auth");
            return "";
        }
        int pos = textBuilder.lastIndexOf("\"");
        return textBuilder.substring(textBuilder.lastIndexOf("\"", pos - 1) + 1, pos);
    }

    public static void setRedditAuth(Context context, String clientId) {
        SharedPreferences.Editor editor =
                PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putString("RedditAuth", clientId);
        editor.putBoolean("RedditAuth-verified", true);
        editor.apply();
    }

    public static boolean isRedditAuthVerified(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return pref.getBoolean("RedditAuth-verified", false);
    }
}
