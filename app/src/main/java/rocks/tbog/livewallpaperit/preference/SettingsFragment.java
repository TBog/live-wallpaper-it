package rocks.tbog.livewallpaperit.preference;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import rocks.tbog.livewallpaperit.BuildConfig;
import rocks.tbog.livewallpaperit.R;

public class SettingsFragment extends PreferenceFragmentCompat {

    private static final String TAG = SettingsFragment.class.getSimpleName();
    public static final String FRAGMENT_TAG = SettingsFragment.class.getName();

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preference_settings, rootKey);

        if (!BuildConfig.SHOW_RATE_APP) {
            removePreference("rate-app");
        }
        updateAppNameAndVersion();

        final Activity activity = requireActivity();

        // set activity title as the preference screen title
        activity.setTitle(getPreferenceScreen().getTitle());
    }

    private void removePreference(@NonNull String key) {
        Preference pref = findPreference(key);
        if (pref == null || pref.getParent() == null) {
            Log.w(TAG, "can't remove preference `" + key + "`");
        } else {
            pref.getParent().removePreference(pref);
        }
    }

    private void updateAppNameAndVersion() {
        Preference appVer = findPreference("app-version");
        if (appVer != null) {
            var version = appVer.getContext().getString(R.string.app_version, BuildConfig.VERSION_NAME);
            var appName = appVer.getContext().getText(R.string.app_name);
            String appStore;
            switch (BuildConfig.FLAVOR) {
                case "playstore":
                    appStore = "Google Play";
                    break;
                case "fdroid":
                    appStore = "F-Droid";
                    break;
                case "github":
                    appStore = "GitHub";
                    break;
                default:
                    throw new IllegalStateException("Undefined flavor");
            }
            var summary = appVer.getContext().getString(R.string.app_version_summary, appName, appStore);
            appVer.setTitle(version);
            appVer.setSummary(summary);

            // add link to the launcher webpage if app not installed from a store
            if (!BuildConfig.SHOW_RATE_APP) {
                appVer.setEnabled(true);
            }
        }
    }
}