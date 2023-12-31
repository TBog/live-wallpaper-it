package rocks.tbog.livewallpaperit.work;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.google.android.apps.muzei.api.provider.ProviderContract;
import com.kirkbushman.araw.RedditClient;
import com.kirkbushman.araw.helpers.AuthHelper;
import com.kirkbushman.araw.helpers.AuthUserlessHelper;
import com.kirkbushman.araw.helpers.NoAuthHelper;
import rocks.tbog.livewallpaperit.ArtProvider;
import rocks.tbog.livewallpaperit.data.DBHelper;

public class SetupWorker extends Worker {
    private static final String TAG = SetupWorker.class.getSimpleName();

    public SetupWorker(@NonNull Context appContext, @NonNull WorkerParameters workerParams) {
        super(appContext, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context ctx = getApplicationContext();

        final AuthHelper helper;
        String clientId = getInputData().getString(WorkerUtils.DATA_CLIENT_ID);
        if (TextUtils.isEmpty(clientId)) {
            helper = new NoAuthHelper(false, true);
        } else {
            helper = new AuthUserlessHelper(ctx, clientId, "DO_NOT_TRACK_THIS_DEVICE", false, true);
        }

        // obtain a client
        RedditClient client = helper.getRedditClient();
        if (client == null)
            return Result.failure(new Data.Builder()
                    .putString(WorkerUtils.FAIL_REASON, "getRedditClient=null")
                    .build());

        if (helper.shouldLogin()) {
            // you must authenticate
            Log.e(TAG, "you must authenticate. Probably wrong clientId.");
            return Result.failure(new Data.Builder()
                    .putString(WorkerUtils.FAIL_REASON, "auth required, probably wrong clientId")
                    .build());
        } else {
            // use saved one
            Log.v(TAG, "hasSavedBearer=" + helper.hasSavedBearer());
        }

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
        int desiredArtworkCount = pref.getInt("desired-artwork-count", 1);
        boolean allowNSFW = pref.getBoolean("allow-nsfw", false);

        addAllFavorites(ctx);

        return Result.success(new Data.Builder()
                .putAll(getInputData())
                .putBoolean(WorkerUtils.DATA_ALLOW_NSFW, allowNSFW)
                .putInt(WorkerUtils.DATA_DESIRED_ARTWORK_COUNT, desiredArtworkCount)
                .build());
    }

    private static void addAllFavorites(Context ctx) {
        var provider = ProviderContract.getProviderClient(ctx, ArtProvider.class);
        var topics = DBHelper.getSubTopicsWithFavorites(ctx);
        DBHelper.loadSubTopicImages(ctx, topics);
        for (var topic : topics) {
            for (var image : topic.images) {
                if (!image.isSource) continue;
                if (image.isObfuscated) continue;
                var artwork = ArtLoadWorker.buildArtwork(topic, image.mediaId, Uri.parse(image.url))
                        .byline(new String(Character.toChars(129293))) // 🤍
                        .build();
                provider.addArtwork(artwork);
            }
        }
    }
}
