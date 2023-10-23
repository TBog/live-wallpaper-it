package rocks.tbog.livewallpaperit;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.WorkManager;

import com.google.android.apps.muzei.api.provider.Artwork;
import com.google.android.apps.muzei.api.provider.MuzeiArtProvider;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

import rocks.tbog.livewallpaperit.utils.PrefUtils;
import rocks.tbog.livewallpaperit.work.ArtLoadWorker;
import rocks.tbog.livewallpaperit.work.LoginWorker;

public class ArtProvider extends MuzeiArtProvider {
    private static final String TAG = ArtProvider.class.getSimpleName();
    public static final String PREF_SOURCES_SET = "subreddit_sources";
    UUID mRequestID = null;

    @Override
    public void onLoadRequested(boolean initial) {
        Context ctx = getContext();
        if (ctx == null)
            return;
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(LoginWorker.class)
                .setInputData(new Data.Builder()
                        .putString("clientId", PrefUtils.loadRedditAuth(ctx))
                        .build())
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build())
                .build();
        var workQueue = WorkManager.getInstance(ctx).beginWith(request);

        ArrayList<OneTimeWorkRequest> subredditWorkList = new ArrayList<>();
        var sourcesSet = PreferenceManager.getDefaultSharedPreferences(ctx).getStringSet(PREF_SOURCES_SET, Collections.emptySet());
        for (String subreddit : sourcesSet) {
            subredditWorkList.add(new OneTimeWorkRequest
                    .Builder(ArtLoadWorker.class)
                    .setInputData(new Data.Builder()
                            .putString("subreddit", subreddit)
                            .build())
                    .build());
        }
        workQueue.then(subredditWorkList).enqueue();
        mRequestID = request.getId();
    }

    @NonNull
    @Override
    public InputStream openFile(@NonNull Artwork artwork) throws IOException {
        Log.d(TAG, "openFile " + artwork.getToken());
        return super.openFile(artwork);
    }
}
