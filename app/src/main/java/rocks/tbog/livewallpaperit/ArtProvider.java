package rocks.tbog.livewallpaperit;

import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.icu.util.Calendar;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.RemoteActionCompat;
import androidx.lifecycle.Observer;
import androidx.preference.PreferenceManager;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
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
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import rocks.tbog.livewallpaperit.data.DBHelper;
import rocks.tbog.livewallpaperit.utils.DataUtils;
import rocks.tbog.livewallpaperit.work.ArtLoadWorker;
import rocks.tbog.livewallpaperit.work.CleanupWorker;
import rocks.tbog.livewallpaperit.work.SetupWorker;
import rocks.tbog.livewallpaperit.work.WorkerUtils;

public class ArtProvider extends MuzeiArtProvider {
    private static final String TAG = ArtProvider.class.getSimpleName();
    public static final String PREF_SOURCES_SET = "subreddit_sources";

    final Observer<WorkInfo> debugLogWork = workInfo -> {
        if (workInfo == null) return;
        Log.d(TAG, "work " + workInfo.getId() + " state " + workInfo.getState());
        if (workInfo.getState() == WorkInfo.State.FAILED) {
            String reason = workInfo.getOutputData().getString(WorkerUtils.FAIL_REASON);
            if (!TextUtils.isEmpty(reason)) {
                Toast.makeText(ArtProvider.this.getContext(), reason, Toast.LENGTH_SHORT)
                        .show();
            }
        }
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

        long nowInMillis;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            nowInMillis = Calendar.getInstance().getTime().getTime();
        } else {
            nowInMillis = new Date().getTime();
        }
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
        long lastRefresh = pref.getLong("lastRefresh", Long.MIN_VALUE);
        if ((lastRefresh + DateUtils.HOUR_IN_MILLIS) > nowInMillis) {
            var displayAgo = DateUtils.getRelativeDateTimeString(
                    ctx,
                    lastRefresh,
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.DAY_IN_MILLIS,
                    DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE);
            Log.i(TAG, "refreshing too soon; lastRefresh = " + displayAgo);
            return;
        }

        final OneTimeWorkRequest setupWork = buildSetupWorkRequest(ctx);
        final OneTimeWorkRequest removeOldArtwork = buildCleanupWorkRequest();
        final ArrayList<OneTimeWorkRequest> subredditWorkList = new ArrayList<>();
        var sources = DBHelper.getSources(ctx);
        for (Source source : sources) {
            if (!source.isEnabled) continue;
            subredditWorkList.add(buildSourceWorkRequest(source));
        }
        final var workManager = WorkManager.getInstance(ctx);
        workManager
                .beginUniqueWork(WorkerUtils.UNIQUE_WORK_REFRESH_ENABLED, ExistingWorkPolicy.KEEP, setupWork)
                .then(subredditWorkList)
                .then(removeOldArtwork)
                .enqueue();
        if (BuildConfig.DEBUG) {
            new Handler(Looper.getMainLooper()).post(() -> {
                workManager.getWorkInfoByIdLiveData(setupWork.getId()).observeForever(debugLogWork);
                for (OneTimeWorkRequest work : subredditWorkList) {
                    workManager.getWorkInfoByIdLiveData(work.getId()).observeForever(debugLogWork);
                }
            });
        }
        if (pref.edit().putLong("lastRefresh", nowInMillis).commit()) {
            DateFormat dateFormat = DateFormat.getDateTimeInstance();
            Log.i(TAG, "last refresh set to " + dateFormat.format(new Date(nowInMillis)));
        }
    }

    @NonNull
    public static OneTimeWorkRequest buildSetupWorkRequest(@NonNull Context ctx) {
        return new OneTimeWorkRequest.Builder(SetupWorker.class)
                .setInputMerger(OverwritingInputMerger.class)
                .setInputData(new Data.Builder()
                        .putString(WorkerUtils.DATA_CLIENT_ID, DataUtils.loadRedditAuth(ctx))
                        .build())
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build())
                .build();
    }

    @NonNull
    public static OneTimeWorkRequest buildCleanupWorkRequest() {
        return new OneTimeWorkRequest.Builder(CleanupWorker.class)
                .setInputMerger(OverwritingInputMerger.class)
                .build();
    }

    @NonNull
    public static OneTimeWorkRequest buildSourceWorkRequest(@NonNull Source source) {
        return new OneTimeWorkRequest.Builder(ArtLoadWorker.class)
                .setInputData(new Data.Builder()
                        .putByteArray(WorkerUtils.DATA_SOURCE, Source.toByteArray(source))
                        .build())
                .build();
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
            var mediaInfo = DBHelper.getMediaByToken(ctx, token);
            if (mediaInfo != null) {
                if (DBHelper.insertIgnoreMedia(ctx, mediaInfo)) {
                    Log.d(TAG, "ignored " + mediaInfo.mediaId + " from " + mediaInfo.subreddit);
                }
            }
        }

        super.onInvalidArtwork(artwork);
    }
}
