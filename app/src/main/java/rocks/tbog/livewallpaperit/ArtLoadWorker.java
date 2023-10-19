package rocks.tbog.livewallpaperit;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.apps.muzei.api.provider.Artwork;
import com.google.android.apps.muzei.api.provider.ProviderClient;
import com.google.android.apps.muzei.api.provider.ProviderContract;

public class ArtLoadWorker extends Worker {
    private static final String TAG = ArtLoadWorker.class.getSimpleName();

    public ArtLoadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context ctx = getApplicationContext();
        String url = "https://raw.githubusercontent.com/muzei/muzei/main/art/screenshots/gallery_photos/20140313_095649_230.jpg?raw";

        Artwork artwork = new Artwork.Builder()
                .persistentUri(Uri.parse(url))
                .token(url)
                .build();

        ProviderClient providerClient = ProviderContract.getProviderClient(ctx, ArtProvider.class);
        Log.d(TAG, "addArtwork " + artwork.getToken());
        providerClient.addArtwork(artwork);

        return Result.success();
    }
}
