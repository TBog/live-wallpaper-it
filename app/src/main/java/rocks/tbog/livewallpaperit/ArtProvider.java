package rocks.tbog.livewallpaperit;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.google.android.apps.muzei.api.provider.Artwork;
import com.google.android.apps.muzei.api.provider.MuzeiArtProvider;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import rocks.tbog.livewallpaperit.utils.PrefUtils;

public class ArtProvider extends MuzeiArtProvider {
    private static final String TAG = ArtProvider.class.getSimpleName();
    UUID mRequestID = null;

    @Override
    public void onLoadRequested(boolean initial) {
        Context ctx = getContext();
        if (ctx == null)
            return;
        WorkRequest request = new OneTimeWorkRequest.Builder(ArtLoadWorker.class)
                .setInputData(new Data.Builder()
                        .putString("clientId", PrefUtils.loadRedditAuth(ctx))
                        .build())
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build())
                .build();
        WorkManager.getInstance(ctx).enqueue(request);
        mRequestID = request.getId();
    }

    @NonNull
    @Override
    public InputStream openFile(@NonNull Artwork artwork) throws IOException {
        Log.d(TAG, "openFile " + artwork.getToken());
        return super.openFile(artwork);
    }
}
