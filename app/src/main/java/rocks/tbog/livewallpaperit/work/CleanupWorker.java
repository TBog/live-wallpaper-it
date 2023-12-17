package rocks.tbog.livewallpaperit.work;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.collection.ArraySet;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.google.android.apps.muzei.api.provider.ProviderContract;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import rocks.tbog.livewallpaperit.ArtProvider;
import rocks.tbog.livewallpaperit.Source;
import rocks.tbog.livewallpaperit.data.DBHelper;
import rocks.tbog.livewallpaperit.data.MediaInfo;
import rocks.tbog.livewallpaperit.data.SubTopic;

public class CleanupWorker extends Worker {
    private static final String TAG = CleanupWorker.class.getSimpleName();

    public CleanupWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context ctx = getApplicationContext();

        Set<String> cachedImages = new HashSet<>();
        final List<Source> sources = DBHelper.getSources(ctx);
        for (var source : sources) {
            final List<SubTopic> topics = DBHelper.getSubTopics(ctx, source.subreddit);
            for (var topic : topics) {
                DBHelper.loadSubTopicImages(ctx, topic);
                for (var image : topic.images) {
                    cachedImages.add(image.mediaId);
                }
            }
        }
        Log.v(TAG, "found " + cachedImages.size() + " image(s) in cache");

        removeFromIgnoreAllExcept(ctx, sources, cachedImages);
        removeFromMuzeiAllExcept(ctx, cachedImages);

        return Result.success();
    }

    private void removeFromIgnoreAllExcept(Context ctx, Collection<Source> sources, Set<String> keepImages) {
        Set<MediaInfo> mediaToRemove = new ArraySet<>();
        for (var source : sources) {
            final var ignoreList = DBHelper.getIgnoreMediaList(ctx, source.subreddit);
            mediaToRemove.addAll(ignoreList);
        }
        Log.v(TAG, "found " + mediaToRemove.size() + " image(s) in ignore list");

        mediaToRemove.removeIf(info -> !keepImages.contains(info.mediaId));
        DBHelper.removeIgnoreMedia(ctx, mediaToRemove);
    }

    private void removeFromMuzeiAllExcept(Context ctx, Set<String> keepImages) {
        Set<String> tokensToRemove = new ArraySet<>();

        final ContentResolver content = ctx.getContentResolver();
        final Uri contentUri =
                ProviderContract.getProviderClient(ctx, ArtProvider.class).getContentUri();
        try (Cursor cursor =
                content.query(contentUri, new String[] {ProviderContract.Artwork.TOKEN}, null, null, null)) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String token = cursor.getString(0);
                    if (!keepImages.contains(token)) {
                        tokensToRemove.add(token);
                    }
                }
            }
        }

        final String whereFilter;
        if (tokensToRemove.size() > 1) {
            whereFilter = ProviderContract.Artwork.TOKEN + " IN (?" + ",?".repeat(tokensToRemove.size() - 1) + ")";
        } else {
            whereFilter = ProviderContract.Artwork.TOKEN + " = ?";
        }
        int count = content.delete(contentUri, whereFilter, tokensToRemove.toArray(new String[0]));
        Log.d(TAG, "deleted " + count + "/" + tokensToRemove.size() + " from Muzei");
    }
}
