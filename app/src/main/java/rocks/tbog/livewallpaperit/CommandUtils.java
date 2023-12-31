package rocks.tbog.livewallpaperit;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.RemoteActionCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.drawable.IconCompat;
import com.google.android.apps.muzei.api.provider.Artwork;
import java.io.File;
import java.util.ArrayList;
import rocks.tbog.livewallpaperit.data.DBHelper;
import rocks.tbog.livewallpaperit.data.MediaInfo;

public class CommandUtils {
    private static final String TAG = CommandUtils.class.getSimpleName();

    public static ArrayList<RemoteActionCompat> artworkCommandActions(@NonNull Context ctx, @NonNull Artwork artwork) {
        ArrayList<RemoteActionCompat> commandActions = new ArrayList<>();

        final MediaInfo mediaInfo = DBHelper.getMediaByToken(ctx, artwork.getToken());
        if (mediaInfo == null) {
            Log.i(TAG, "token " + artwork.getToken() + " not found");
        } else {
            String title = artwork.getTitle();
            if (TextUtils.isEmpty(title)) title = mediaInfo.mediaId;

            Uri linkComment = Uri.parse(
                    "https://www.reddit.com/r/" + mediaInfo.subreddit + "/comments/" + mediaInfo.topicId + "/");
            Intent openComment = new Intent(Intent.ACTION_VIEW).setData(linkComment);
            RemoteActionCompat titleCommand = new RemoteActionCompat(
                    IconCompat.createWithResource(
                            ctx, com.google.android.apps.muzei.api.R.drawable.muzei_launch_command),
                    title,
                    title,
                    PendingIntent.getActivity(
                            ctx, 0, openComment, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
            titleCommand.setEnabled(false);
            titleCommand.setShouldShowIcon(false);

            Uri linkSubreddit = Uri.parse("https://www.reddit.com/r/" + mediaInfo.subreddit + "/");
            // Intent openLWI = ctx.getPackageManager().getLaunchIntentForPackage(ctx.getPackageName());
            Intent openSubreddit = new Intent(Intent.ACTION_VIEW).setData(linkSubreddit);
            RemoteActionCompat subredditCommand = new RemoteActionCompat(
                    IconCompat.createWithResource(
                            ctx, com.google.android.apps.muzei.api.R.drawable.muzei_launch_command),
                    "r/" + mediaInfo.subreddit,
                    mediaInfo.subreddit,
                    PendingIntent.getActivity(
                            ctx, 0, openSubreddit, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
            subredditCommand.setShouldShowIcon(false);

            commandActions.add(titleCommand);
            commandActions.add(subredditCommand);
        }

        commandActions.add(obtainActionShareTitleAndLink(ctx, artwork));
        commandActions.add(obtainActionShareImage(ctx, artwork));
        commandActions.add(obtainActionDelete(ctx, artwork));

        return commandActions;
    }

    private static RemoteActionCompat obtainActionShareTitleAndLink(@NonNull Context ctx, @NonNull Artwork artwork) {
        Intent intent = new Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(
                        Intent.EXTRA_TEXT,
                        ctx.getString(
                                R.string.share_title_and_link,
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
                ctx.getString(R.string.action_share_link),
                ctx.getString(R.string.action_share_description),
                PendingIntent.getActivity(
                        ctx,
                        requestCode,
                        Intent.createChooser(intent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        action.setShouldShowIcon(true);
        return action;
    }

    private static RemoteActionCompat obtainActionShareImage(Context context, Artwork artwork) {
        //        String authority = BuildConfig.LWI_AUTHORITY;
        //        try {
        //            authority = context.getPackageManager()
        //                    .getProviderInfo(new ComponentName(context, ArtProvider.class), 0)
        //                    .authority;
        //        } catch (PackageManager.NameNotFoundException e) {
        //            Log.w(TAG, "can't get authority", e);
        //        }
        File cacheDir = new File(context.getCacheDir(), "muzei_" + BuildConfig.LWI_AUTHORITY);
        File cacheFile = new File(cacheDir, Long.toString(artwork.getId()));
        Uri uri = FileProvider.getUriForFile(context, BuildConfig.LWI_AUTHORITY + ".fileprovider", cacheFile);

        Intent intent = new Intent(Intent.ACTION_SEND).setType("image/*").putExtra(Intent.EXTRA_STREAM, uri);
        Intent chooseIntent = Intent.createChooser(intent, artwork.getTitle())
                .addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // for PendingIntents we need Intent.filterEquals to differentiate between artworks
        int requestCode = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            intent.setIdentifier(artwork.getToken());
        } else {
            requestCode = (int) artwork.getId();
        }

        RemoteActionCompat action = new RemoteActionCompat(
                IconCompat.createWithResource(context, R.drawable.ic_share_image_24),
                context.getString(R.string.action_share_image),
                context.getString(R.string.action_share_image_description),
                PendingIntent.getActivity(
                        context,
                        requestCode,
                        chooseIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        action.setShouldShowIcon(true);
        return action;
    }

    private static RemoteActionCompat obtainActionDelete(Context ctx, Artwork artwork) {
        Intent intent = new Intent(ctx, DeleteArtworkReceiver.class)
                .putExtra(DeleteArtworkReceiver.ACTION, DeleteArtworkReceiver.ACTION_DELETE)
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
                PendingIntent.getBroadcast(
                        ctx, requestCode, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));
        action.setShouldShowIcon(false);
        return action;
    }
}
