package rocks.tbog.livewallpaperit;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import com.google.android.apps.muzei.api.provider.ProviderContract;
import rocks.tbog.livewallpaperit.data.DBHelper;

public class DeleteArtworkReceiver extends BroadcastReceiver {

    private static final String TAG = DeleteArtworkReceiver.class.getSimpleName();
    public static final String ARTWORK_ID = "artworkId";
    public static final String ARTWORK_TOKEN = "artworkToken";

    @Override
    public void onReceive(Context context, Intent intent) {
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

        if (DBHelper.insertIgnoreToken(context, artworkToken)) {
            Log.d(TAG, "ignored " + artworkToken);
        }
    }
}
