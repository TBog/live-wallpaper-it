package rocks.tbog.livewallpaperit;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.RemoteActionCompat;
import androidx.core.graphics.drawable.IconCompat;

import com.google.android.apps.muzei.api.provider.Artwork;

import java.util.ArrayList;

public class CommandUtils {
    public static ArrayList<RemoteActionCompat> artworkCommandActions(@NonNull Context ctx, @NonNull Artwork artwork) {
        ArrayList<RemoteActionCompat> commandActions = new ArrayList<>();

        commandActions.add(obtainActionShareTitleAndLink(ctx, artwork));
        commandActions.add(obtainActionDelete(ctx, artwork));

        return commandActions;
    }

    private static RemoteActionCompat obtainActionShareTitleAndLink(@NonNull Context ctx, @NonNull Artwork artwork) {
        Intent intent = new Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, ctx.getString(R.string.share_title_and_link,
                        artwork.getTitle(),
                        artwork.getAttribution(),
                        artwork.getWebUri()));

        // we need Intent.filterEquals to differentiate between artworks
        int requestCode = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            intent.setIdentifier(artwork.getToken());
        } else {
            requestCode = (int) artwork.getId();
        }

        // create an action with a unique PendingIntent
        RemoteActionCompat action = new RemoteActionCompat(
                IconCompat.createWithResource(ctx, R.drawable.ic_share_24),
                ctx.getString(R.string.action_share),
                ctx.getString(R.string.action_share_description),
                PendingIntent.getActivity(ctx,
                        requestCode,
                        Intent.createChooser(intent, null)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE)
        );
        action.setShouldShowIcon(true);
        return action;
    }

    private static RemoteActionCompat obtainActionDelete(Context ctx, Artwork artwork) {
        Intent intent = new Intent(ctx, DeleteArtworkReceiver.class)
                .putExtra(DeleteArtworkReceiver.ARTWORK_ID, String.valueOf(artwork.getId()))
                .putExtra(DeleteArtworkReceiver.ARTWORK_TOKEN, artwork.getToken());

        // we need Intent.filterEquals to differentiate between artworks
        int requestCode = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            intent.setIdentifier(artwork.getToken());
        } else {
            requestCode = (int) artwork.getId();
        }

        // create an action with a unique PendingIntent
        RemoteActionCompat action = new RemoteActionCompat(
                IconCompat.createWithResource(ctx, R.drawable.ic_delete_24),
                ctx.getString(R.string.action_delete),
                ctx.getString(R.string.action_delete_description),
                PendingIntent.getBroadcast(ctx, requestCode, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT)
        );
        action.setShouldShowIcon(false);
        return action;
    }
}
