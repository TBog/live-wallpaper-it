package rocks.tbog.livewallpaperit.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

public class DataUtils {
    private static final String TAG = DataUtils.class.getSimpleName();

    @NonNull
    public static String loadRedditAuth(@NonNull Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        String clientId = pref.getString("RedditAuth", null);
        if (!TextUtils.isEmpty(clientId) && pref.getBoolean("RedditAuth-verified", false)) {
            return clientId;
        }
        return "";
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
