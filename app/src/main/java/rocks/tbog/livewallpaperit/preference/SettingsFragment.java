package rocks.tbog.livewallpaperit.preference;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import rocks.tbog.livewallpaperit.BuildConfig;
import rocks.tbog.livewallpaperit.R;

public class SettingsFragment extends PreferenceFragmentCompat {

    private static final String TAG = SettingsFragment.class.getSimpleName();
    public static final String FRAGMENT_TAG = SettingsFragment.class.getName();
    private static final String DIALOG_FRAGMENT_TAG = "androidx.preference.PreferenceFragment.DIALOG";

    @Override
    public void onDisplayPreferenceDialog(@NonNull Preference preference) {
        String key = preference.getKey();
        DialogFragment dialogFragment;
        if (preference instanceof CustomDialogPreference) {
            // Create a new instance of CustomDialog with the key of the related Preference
            Log.d(TAG, "onDisplayPreferenceDialog " + key);
            dialogFragment = NumberPickerDialog.newInstance(key);
        } else {
            Log.i(TAG, "Preference \"" + key + "\" has no custom dialog defined");
            dialogFragment = null;
        }

        // If it was one of our custom Preferences, show its dialog
        if (dialogFragment != null) {
            final FragmentManager fm = this.getParentFragmentManager();
            // check if dialog is already showing
            if (fm.findFragmentByTag(DIALOG_FRAGMENT_TAG) != null) {
                Log.e(TAG, "dialog is already showing");
                return;
            }
            dialogFragment.setTargetFragment(this, 0);
            dialogFragment.show(fm, DIALOG_FRAGMENT_TAG);
        }
        // Could not be handled here. Try with the super method.
        else {
            super.onDisplayPreferenceDialog(preference);
        }
    }

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
