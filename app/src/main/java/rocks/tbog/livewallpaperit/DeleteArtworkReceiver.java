package rocks.tbog.livewallpaperit;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import com.google.android.apps.muzei.api.provider.ProviderContract;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;
import rocks.tbog.livewallpaperit.data.DBHelper;
import rocks.tbog.livewallpaperit.data.MediaInfo;

public class DeleteArtworkReceiver extends BroadcastReceiver {

    private static final String TAG = DeleteArtworkReceiver.class.getSimpleName();
    public static final String ACTION = "delete.artwork.action";
    public static final String ACTION_DELETE = "delete";
    public static final String ACTION_CLEAR_CACHE = "clear_cache";
    public static final String ARTWORK_ID = "delete.artwork.ID";
    public static final String ARTWORK_TOKEN = "delete.artwork.token";
    public static final String MEDIA_ID = "delete.media_id";
    public static final String MEDIA_ID_ARRAY = "delete.media_id.array";
    public static final String MEDIA_TOPIC_ID = "delete.topic_id";
    public static final String MEDIA_SUBREDDIT = "delete.subreddit";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getStringExtra(ACTION);
        if (ACTION_DELETE.equals(action)) {
            String[] mediaIdArray = null;
            if (intent.hasExtra(MEDIA_ID)) {
                mediaIdArray = new String[] {intent.getStringExtra(MEDIA_ID)};
            } else if (intent.hasExtra(MEDIA_ID_ARRAY)) {
                mediaIdArray = intent.getStringArrayExtra(MEDIA_ID_ARRAY);
            }
            if (mediaIdArray != null && intent.hasExtra(MEDIA_TOPIC_ID) && intent.hasExtra(MEDIA_SUBREDDIT)) {
                deleteMedia(
                        context,
                        mediaIdArray,
                        intent.getStringExtra(MEDIA_TOPIC_ID),
                        intent.getStringExtra(MEDIA_SUBREDDIT));
            } else {
                deleteArtwork(context, intent);
            }
        } else if (ACTION_CLEAR_CACHE.equals(action)) {
            deleteAll(context);
        } else {
            Log.e(TAG, "invalid action `" + action + "`");
        }
    }

    private void deleteMedia(Context context, String[] mediaArray, String topicId, String subreddit) {
        if (mediaArray == null) {
            Log.e(TAG, "mediaId array is null");
            return;
        }

        final ContentResolver content = context.getContentResolver();
        final Uri contentUri =
                ProviderContract.getProviderClient(context, ArtProvider.class).getContentUri();
        final String whereFilter;
        if (mediaArray.length > 1) {
            whereFilter = ProviderContract.Artwork.TOKEN + " IN (?" + ",?".repeat(mediaArray.length - 1) + ")";
        } else {
            whereFilter = ProviderContract.Artwork.TOKEN + " = ?";
        }
        int count = content.delete(contentUri, whereFilter, mediaArray);
        Log.d(TAG, "deleted " + count + "/" + mediaArray.length);

        var collection = Arrays.stream(mediaArray)
                .map(mediaId -> new MediaInfo(mediaId, topicId, subreddit))
                .collect(Collectors.toSet());
        if ((count = DBHelper.insertIgnoreTokens(context, collection)) != -1) {
            Log.d(TAG, "ignored " + count + "/" + mediaArray.length);
        }
    }

    private void deleteArtwork(Context context, Intent intent) {
        final String artworkId = intent.getStringExtra(ARTWORK_ID);
        final String artworkToken = intent.getStringExtra(ARTWORK_TOKEN);
        if (artworkId == null) {
            Log.e(TAG, "artworkId is null");
            return;
        }
        if (artworkToken == null) {
            Log.e(TAG, "artworkToken is null");
            return;
        }

        Log.d(TAG, "delete " + artworkToken);

        final ContentResolver content = context.getContentResolver();
        final Uri contentUri =
                ProviderContract.getProviderClient(context, ArtProvider.class).getContentUri();
        final String whereFilter = ProviderContract.Artwork._ID + " = ? AND " + ProviderContract.Artwork.TOKEN + " = ?";
        final String[] whereArgs = new String[] {artworkId, artworkToken};

        int count = content.delete(contentUri, whereFilter, whereArgs);
        Log.d(TAG, "delete count=" + count);

        var mediaInfo = DBHelper.getMediaByToken(context, artworkToken);
        if (mediaInfo != null) {
            if (DBHelper.insertIgnoreMedia(context, mediaInfo)) {
                Log.d(TAG, "ignored " + mediaInfo.mediaId + " from " + mediaInfo.subreddit);
            }
        }
    }

    private void deleteAll(Context context) {
        ProviderContract.getProviderClient(context, ArtProvider.class).setArtwork(Collections.emptyList());
    }
}
