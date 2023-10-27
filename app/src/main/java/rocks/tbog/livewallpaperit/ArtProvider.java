package rocks.tbog.livewallpaperit;

import android.app.PendingIntent;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.RemoteActionCompat;
import androidx.lifecycle.Observer;
import androidx.preference.PreferenceManager;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.OverwritingInputMerger;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import com.google.android.apps.muzei.api.provider.Artwork;
import com.google.android.apps.muzei.api.provider.MuzeiArtProvider;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import rocks.tbog.livewallpaperit.data.DBHelper;
import rocks.tbog.livewallpaperit.utils.PrefUtils;
import rocks.tbog.livewallpaperit.work.ArtLoadWorker;
import rocks.tbog.livewallpaperit.work.LoginWorker;
import rocks.tbog.livewallpaperit.work.WorkerUtils;

public class ArtProvider extends MuzeiArtProvider {

    private static final String TAG = ArtProvider.class.getSimpleName();
    public static final String PREF_SOURCES_SET = "subreddit_sources";

    final Observer<WorkInfo> debugLogWork = workInfo -> {
        Log.d(TAG, "work " + workInfo.getId() + " state " + workInfo.getState());
        if (workInfo.getState() != WorkInfo.State.BLOCKED) {
            Map<String, Object> map = workInfo.getOutputData().getKeyValueMap();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                Log.d(TAG, "[" + key + "]=" + value);
            }
        }
    };

    @Override
    public void onLoadRequested(boolean initial) {
        Context ctx = getContext();
        if (ctx == null) return;
        final OneTimeWorkRequest setupWork = new OneTimeWorkRequest.Builder(LoginWorker.class)
                .setInputMerger(OverwritingInputMerger.class)
                .setInputData(new Data.Builder()
                        .putString(WorkerUtils.DATA_CLIENT_ID, PrefUtils.loadRedditAuth(ctx))
                        .build())
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build())
                .build();
        var workManager = WorkManager.getInstance(ctx);
        var workQueue = workManager.beginWith(setupWork);
        final ArrayList<OneTimeWorkRequest> subredditWorkList = new ArrayList<>();
        var sourcesSet = PreferenceManager.getDefaultSharedPreferences(ctx)
                .getStringSet(PREF_SOURCES_SET, Collections.emptySet());
        for (String subreddit : sourcesSet) {
            subredditWorkList.add(new OneTimeWorkRequest.Builder(ArtLoadWorker.class)
                    .setInputData(new Data.Builder()
                            .putString(WorkerUtils.DATA_SUBREDDIT, subreddit)
                            .build())
                    .build());
        }
        workQueue.then(subredditWorkList).enqueue();
        if (BuildConfig.DEBUG) {
            new Handler(Looper.getMainLooper()).post(() -> {
                workManager.getWorkInfoByIdLiveData(setupWork.getId()).observeForever(debugLogWork);
                for (OneTimeWorkRequest work : subredditWorkList) {
                    workManager.getWorkInfoByIdLiveData(work.getId()).observeForever(debugLogWork);
                }
            });
        }
    }

    @NonNull
    @Override
    public List<RemoteActionCompat> getCommandActions(@NonNull Artwork artwork) {
        Log.d(TAG, "getCommandActions " + artwork.getToken());
        List<RemoteActionCompat> commands = super.getCommandActions(artwork);
        Context ctx = getContext();
        if (ctx != null) {
            commands.addAll(CommandUtils.artworkCommandActions(ctx.getApplicationContext(), artwork));
        }
        return commands;
    }

    @NonNull
    @Override
    public InputStream openFile(@NonNull Artwork artwork) throws IOException {
        Log.d(TAG, "openFile " + artwork.getToken());
        return super.openFile(artwork);
    }

    @Nullable
    @Override
    public PendingIntent getArtworkInfo(@NonNull Artwork artwork) {
        Log.d(TAG, "getArtworkInfo " + artwork.getToken());
        return super.getArtworkInfo(artwork);
    }

    @Override
    public void onInvalidArtwork(@NonNull Artwork artwork) {
        Log.d(TAG, "onInvalidArtwork " + artwork.getToken());
        // add to ignore list
        Context ctx = getContext();
        String token = artwork.getToken();
        if (ctx != null && token != null) {
            DBHelper.insertIgnoreToken(ctx, token);
        }

        super.onInvalidArtwork(artwork);
    }
}
