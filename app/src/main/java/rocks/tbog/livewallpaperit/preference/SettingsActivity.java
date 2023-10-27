package rocks.tbog.livewallpaperit.preference;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import java.util.ArrayList;
import rocks.tbog.livewallpaperit.R;
import rocks.tbog.livewallpaperit.TitleActivity;

public class SettingsActivity extends TitleActivity
        implements PreferenceFragmentCompat.OnPreferenceStartScreenCallback {

    private static final String INTENT_EXTRA_BACK_STACK_TAGS = "backStackTagList";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if (savedInstanceState == null) {
            // Create the fragment only when the activity is created for the first time.
            // ie. not after orientation changes
            Fragment fragment = getSupportFragmentManager().findFragmentByTag(SettingsFragment.FRAGMENT_TAG);
            if (fragment == null) {
                fragment = new SettingsFragment();
            }

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_container, fragment, SettingsFragment.FRAGMENT_TAG)
                    .commit();

            restoreBackStack();
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void restoreBackStack() {
        Intent intent = getIntent();
        if (intent == null) return;
        ArrayList<String> backStackEntryList = intent.getStringArrayListExtra(INTENT_EXTRA_BACK_STACK_TAGS);
        if (backStackEntryList != null) for (String key : backStackEntryList) if (key != null) addToBackStack(key);
    }

    private void addToBackStack(@NonNull String key) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        SettingsFragment fragment = new SettingsFragment();
        Bundle args = new Bundle();
        args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, key);
        fragment.setArguments(args);
        ft.replace(R.id.settings_container, fragment, key);
        ft.addToBackStack(key);
        ft.commit();
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (getSupportFragmentManager().popBackStackImmediate()) {
            final int count = getSupportFragmentManager().getBackStackEntryCount();
            CharSequence title = null;
            if (count > 0) {
                String tag = getSupportFragmentManager()
                        .getBackStackEntryAt(count - 1)
                        .getName();
                if (tag != null) {
                    Fragment fragment = getSupportFragmentManager().findFragmentByTag(SettingsFragment.FRAGMENT_TAG);
                    if (fragment instanceof SettingsFragment) {
                        Preference preference = ((SettingsFragment) fragment).findPreference(tag);
                        if (preference != null) title = preference.getTitle();
                    }
                }
            }
            if (title != null) setTitle(title);
            else setTitle(R.string.settings_name);
            return true;
        }
        return super.onSupportNavigateUp();
    }

    @Override
    public boolean onPreferenceStartScreen(
            @NonNull PreferenceFragmentCompat caller, PreferenceScreen preferenceScreen) {
        final String key = preferenceScreen.getKey();
        addToBackStack(key);
        return true;
    }
}
