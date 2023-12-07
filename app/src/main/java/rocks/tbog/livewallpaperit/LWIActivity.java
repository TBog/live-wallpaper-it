package rocks.tbog.livewallpaperit;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import com.google.android.apps.muzei.api.MuzeiContract;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputLayout;
import java.util.UUID;
import rocks.tbog.livewallpaperit.preference.SettingsActivity;
import rocks.tbog.livewallpaperit.preview.SourcesActivity;
import rocks.tbog.livewallpaperit.utils.DataUtils;
import rocks.tbog.livewallpaperit.utils.ViewUtils;
import rocks.tbog.livewallpaperit.work.VerifyClientIdWorker;
import rocks.tbog.livewallpaperit.work.WorkerUtils;

public class LWIActivity extends AppCompatActivity {
    private static final String TAG = LWIActivity.class.getSimpleName();
    private LWIViewModel mModel;
    private UUID mVerifyRequestID;
    TextInputLayout mInputLayout;
    TextView mInput;
    MaterialButton mButtonVerify;
    MaterialButton mButtonShowApi;
    MaterialButton mButtonHideApi;
    MaterialButton mButtonActivate;
    MaterialButton mButtonSources;
    MaterialButton mButtonSettings;
    CircularProgressIndicator mProgressVerify;

    private final ActivityResultLauncher<Intent> redirectLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), activityResult -> finish());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mModel = new ViewModelProvider(this).get(LWIViewModel.class);
        mInputLayout = findViewById(R.id.input_client_id);
        mInput = findViewById(R.id.client_id);
        mButtonVerify = findViewById(R.id.btn_verify);
        mButtonShowApi = findViewById(R.id.btn_show_api);
        mButtonHideApi = findViewById(R.id.btn_hide_api);
        mButtonSources = findViewById(R.id.btn_edit_source);
        mProgressVerify = findViewById(R.id.verify_progress);
        mButtonActivate = findViewById(R.id.btn_ok);
        mButtonSettings = findViewById(R.id.btn_settings);

        if (savedInstanceState == null) {
            boolean isVerified = DataUtils.isRedditAuthVerified(getApplicationContext());
            String verifiedClientId = DataUtils.loadRedditAuth(getApplicationContext());
            mInput.setText(verifiedClientId);
            mModel.setRedditAuthNow(verifiedClientId, isVerified);
        }

        mInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // nothing to do
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // nothing to do
            }

            @Override
            public void afterTextChanged(Editable s) {
                String text = s.toString();
                if (text.isEmpty()) {
                    mModel.setRedditAuth("", false);
                    ((MotionLayout) findViewById(R.id.activity_main)).transitionToState(R.id.state_api_key_empty);
                } else {
                    String verifiedAuth = DataUtils.loadRedditAuth(getApplicationContext());
                    mModel.setRedditAuth(text, verifiedAuth.equals(s.toString()));
                }
            }
        });
        mInput.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if (mButtonVerify.isEnabled()) {
                    return mButtonVerify.performClick();
                }
            }
            return false;
        });

        mButtonSources.setOnClickListener(this::onClickSources);
        mButtonVerify.setOnClickListener(this::onClickVerify);
        mButtonShowApi.setOnClickListener(this::onClickEnableApi);
        mButtonHideApi.setOnClickListener(this::onClickDisableApi);
        mButtonActivate.setOnClickListener(this::onClickActivate);
        mButtonSettings.setOnClickListener(this::onClickSettings);

        mModel.getRedditAuth().observe(this, auth -> {
            if (mVerifyRequestID != null) {
                WorkManager workManager = WorkManager.getInstance(this);
                workManager.cancelWorkById(mVerifyRequestID);
            }
            if (auth != null) {
                if (auth.mIsVerified) {
                    DataUtils.setRedditAuth(getApplicationContext(), auth.mClientId);
                    mModel.setRedditAuthState(LWIViewModel.RedditAuthState.AUTH_VALID);
                }
                if (auth.mClientId.isBlank()) {
                    ((MotionLayout) findViewById(R.id.activity_main)).transitionToState(R.id.state_api_key_hidden);
                    mButtonActivate.setEnabled(true);
                    mModel.setRedditAuthState(LWIViewModel.RedditAuthState.AUTH_NOT_NEEDED);
                    return;
                }
                return;
            }
            mModel.setRedditAuthState(LWIViewModel.RedditAuthState.AUTH_NOT_DONE);
        });
        mModel.getRedditAuthState().observe(this, state -> {
            if (LWIViewModel.RedditAuthState.AUTH_IN_PROGRESS.equals(state)) {
                mButtonVerify.setEnabled(false);
                mProgressVerify.setVisibility(View.VISIBLE);
                mInputLayout.setError(null);
            } else {
                mProgressVerify.setVisibility(View.INVISIBLE);
            }

            if (LWIViewModel.RedditAuthState.AUTH_FAILED.equals(state)) {
                mInputLayout.setError(getText(R.string.error_clientId_verify));
                mButtonVerify.setEnabled(true);
                mButtonActivate.setEnabled(false);
            } else if (LWIViewModel.RedditAuthState.AUTH_VALID.equals(state)) {
                mButtonVerify.setEnabled(false);
                mButtonActivate.setEnabled(true);
            } else if (LWIViewModel.RedditAuthState.AUTH_NOT_DONE.equals(state)) {
                mInputLayout.setError(null);
                mButtonVerify.setEnabled(true);
                mButtonActivate.setEnabled(false);
            } else if (LWIViewModel.RedditAuthState.AUTH_NOT_NEEDED.equals(state)) {
                mInputLayout.setError(null);
                mButtonVerify.setEnabled(false);
                mButtonActivate.setEnabled(true);
            }
        });
    }

    private void onClickActivate(View view) {
        var launchIntent = getPackageManager().getLaunchIntentForPackage(BuildConfig.MUZEI_PACKAGE_NAME);
        if (MuzeiContract.Sources.isProviderSelected(this, BuildConfig.LWI_AUTHORITY) && launchIntent != null) {
            // Already selected so just open Muzei
            redirectLauncher.launch(launchIntent);
            return;
        }
        // LWIt isn't selected, so try to deep link into Muzei's Sources screen
        var deepLinkIntent = MuzeiContract.Sources.createChooseProviderIntent(BuildConfig.LWI_AUTHORITY);
        if (tryStartIntent(deepLinkIntent, R.string.toast_enable)) {
            return;
        }
        // createChooseProviderIntent didn't work, so try to just launch Muzei
        if (launchIntent != null && tryStartIntent(launchIntent, R.string.toast_enable_source)) {
            return;
        }
        // Muzei isn't installed, so try to open the Play Store so that users can install Muzei
        var playStoreIntent = new Intent(Intent.ACTION_VIEW)
                .setData(Uri.parse("https://play.google.com/store/apps/details?id=" + BuildConfig.MUZEI_PACKAGE_NAME));
        if (tryStartIntent(playStoreIntent, R.string.toast_muzei_missing_error)) {
            return;
        }
        // Only if all Intents failed show a 'everything failed' Toast
        Toast.makeText(this, R.string.toast_play_store_missing_error, Toast.LENGTH_LONG)
                .show();
        finish();
    }

    private void onClickSettings(View view) {
        Intent intent = new Intent(this, SettingsActivity.class);
        ViewUtils.launchIntent(view, intent);
    }

    private void onClickSources(View view) {
        Intent intent = new Intent(this, SourcesActivity.class);
        ViewUtils.launchIntent(view, intent);
    }

    private boolean tryStartIntent(Intent intent, @StringRes int toastResId) {
        try {
            // Use startActivityForResult() so that we get a callback to
            // onActivityResult() if the user hits the system back button
            redirectLauncher.launch(intent);
            Toast.makeText(this, toastResId, Toast.LENGTH_LONG).show();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void onClickVerify(View v) {
        v.setEnabled(false);
        var clientId = mInput.getText();
        if (TextUtils.isEmpty(clientId)) {
            onClickDisableApi(v);
            return;
        }

        WorkRequest request = new OneTimeWorkRequest.Builder(VerifyClientIdWorker.class)
                .setInputData(new Data.Builder()
                        .putString(WorkerUtils.DATA_CLIENT_ID, mInput.getText().toString())
                        .build())
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build())
                .build();
        mVerifyRequestID = request.getId();
        WorkManager workManager = WorkManager.getInstance(this);
        workManager.enqueue(request);
        workManager.getWorkInfoByIdLiveData(request.getId()).observe(this, workInfo -> {
            Log.d(TAG, "work " + workInfo.getId() + " state " + workInfo.getState());
            switch (workInfo.getState()) {
                case SUCCEEDED:
                    if (workInfo.getId() == mVerifyRequestID) {
                        mVerifyRequestID = null;
                    }
                    String workerClientId = workInfo.getOutputData().getString(WorkerUtils.DATA_CLIENT_ID);
                    if (workerClientId == null) workerClientId = "";
                    String modelClientId = mInput.getText().toString();
                    if (TextUtils.equals(workerClientId, modelClientId)) {
                        mModel.setRedditAuth(workerClientId, true);
                    } else {
                        mModel.setRedditAuthState(LWIViewModel.RedditAuthState.AUTH_FAILED);
                    }
                    return;
                case ENQUEUED:
                case RUNNING:
                    mModel.setRedditAuthState(LWIViewModel.RedditAuthState.AUTH_IN_PROGRESS);
                    break;
                case FAILED:
                    mModel.setRedditAuthState(LWIViewModel.RedditAuthState.AUTH_FAILED);
                    // fallthrough
                case CANCELLED:
                    if (workInfo.getId() == mVerifyRequestID) {
                        mVerifyRequestID = null;
                    }
                    break;
            }
        });
    }

    private void onClickEnableApi(View unused) {
        ((MotionLayout) findViewById(R.id.activity_main)).transitionToState(R.id.state_api_key_visible);
        DataUtils.resetRedditAuth(getApplicationContext());
        mModel.setRedditAuthState(LWIViewModel.RedditAuthState.AUTH_NOT_DONE);
    }

    private void onClickDisableApi(View unused) {
        ((MotionLayout) findViewById(R.id.activity_main)).transitionToState(R.id.state_api_key_hidden);
        DataUtils.setRedditAuth(getApplicationContext(), "");
        mModel.setRedditAuthState(LWIViewModel.RedditAuthState.AUTH_NOT_NEEDED);
    }
}
