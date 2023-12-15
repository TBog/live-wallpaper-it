package rocks.tbog.livewallpaperit.preview;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.collection.ArraySet;
import androidx.fragment.app.FragmentManager;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import rocks.tbog.livewallpaperit.DeleteArtworkReceiver;
import rocks.tbog.livewallpaperit.R;
import rocks.tbog.livewallpaperit.RecycleAdapterBase;
import rocks.tbog.livewallpaperit.Source;
import rocks.tbog.livewallpaperit.data.DBHelper;
import rocks.tbog.livewallpaperit.data.MediaInfo;
import rocks.tbog.livewallpaperit.data.SubTopic;
import rocks.tbog.livewallpaperit.dialog.DialogHelper;
import rocks.tbog.livewallpaperit.utils.ViewUtils;
import rocks.tbog.livewallpaperit.work.ArtLoadWorker;

public class SubredditAdapter extends RecycleAdapterBase<SubTopic, SubmissionHolder> {
    private boolean mAllowNSFW = true;
    private int mWidth = 108;
    private String mSubreddit = null;
    private ArtLoadWorker.Filter mFilter = null;
    private final ArraySet<MediaInfo> mIgnoreMediaSet = new ArraySet<>();
    private final ArraySet<MediaInfo> mFavoriteMediaSet = new ArraySet<>();

    public SubredditAdapter() {
        super(new ArrayList<>());
        setHasStableIds(true);
    }

    public void setAllowNSFW(boolean allowNSFW) {
        if (mAllowNSFW == allowNSFW) return;
        mAllowNSFW = allowNSFW;
        if (mFilter != null) {
            mFilter.allowNSFW = mAllowNSFW;
        }
        notifyItemRangeChanged(0, getItemCount());
    }

    public void setPreviewWidth(int width) {
        if (mWidth == width) return;
        mWidth = width;
        notifyItemRangeChanged(0, getItemCount());
    }

    public void setSource(@Nullable Source source) {
        ArtLoadWorker.Filter filter = ArtLoadWorker.Filter.fromSource(source);
        if (filter != null) {
            filter.allowNSFW = mAllowNSFW;
        }
        if (Objects.equals(mFilter, filter)) return;
        mFilter = filter;
        mSubreddit = source != null ? source.subreddit : null;
        notifyItemRangeChanged(0, getItemCount());
    }

    public void setIgnoreList(@NonNull List<MediaInfo> ignoreList) {
        mIgnoreMediaSet.clear();
        mIgnoreMediaSet.addAll(ignoreList);
        notifyItemRangeChanged(0, getItemCount());
    }

    public void setFavoriteList(@NonNull List<MediaInfo> favoriteList) {
        mFavoriteMediaSet.clear();
        mFavoriteMediaSet.addAll(favoriteList);
        notifyItemRangeChanged(0, getItemCount());
    }

    @Override
    public void onBindViewHolder(@NonNull SubmissionHolder holder, @NonNull SubTopic topic) {
        final Context ctx = holder.itemView.getContext();
        long createdMilli = Instant.ofEpochSecond(topic.createdUTC).toEpochMilli();
        var displayAgo = DateUtils.getRelativeDateTimeString(
                ctx,
                createdMilli,
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.DAY_IN_MILLIS,
                DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE);

        boolean showObfuscatedPreview = topic.over18 && !mAllowNSFW;
        var invalidMediaIdSet = mIgnoreMediaSet.stream()
                .filter(info -> topic.id.equals(info.topicId))
                .map(info -> info.mediaId)
                .collect(Collectors.toSet());
        ThumbnailAdapter thumbnailAdapter =
                new ThumbnailAdapter(topic, mWidth, showObfuscatedPreview, invalidMediaIdSet, mFavoriteMediaSet);
        thumbnailAdapter.setOnClickListener(
                (a, t, v) -> ViewUtils.launchIntent(v, new Intent(Intent.ACTION_VIEW).setData(t.link)));
        thumbnailAdapter.setOnLongClickListener(
                (adapter, thumbnail, v) -> onLongClickThumbnail(adapter, thumbnail, v, topic));

        holder.mInfoView.setText(displayAgo);
        holder.mTitleView.setText(topic.title);
        holder.mButtonOpen.setOnClickListener(
                v -> ViewUtils.launchIntent(v, new Intent(Intent.ACTION_VIEW).setData(topic.getPermalinkUri())));
        holder.mButtonRemove.setOnClickListener(btnRemove -> onClickRemove(btnRemove, topic));
        holder.mImageCarouselView.setAdapter(thumbnailAdapter);
        holder.mNsfwView.setVisibility(topic.over18 ? View.VISIBLE : View.GONE);
        holder.mScoreView.setText(String.valueOf(topic.score));
        holder.mUpvoteView.setText(String.valueOf(topic.upvoteRatio));
        holder.mNumCommentView.setText(String.valueOf(topic.numComments));
        if (ArtLoadWorker.shouldSkipTopic(topic, mFilter)) {
            holder.mInvalidView.setVisibility(View.VISIBLE);
        } else {
            holder.mInvalidView.setVisibility(View.GONE);
        }
    }

    private void onClickRemove(View btnRemove, @NonNull SubTopic topic) {
        Activity activity = ViewUtils.getActivity(btnRemove);
        FragmentManager fragmentManager = null;
        if (activity instanceof AppCompatActivity) {
            fragmentManager = ((AppCompatActivity) activity).getSupportFragmentManager();
        }
        if (fragmentManager == null) return;
        DialogHelper.makeConfirmDialog(
                        activity.getString(R.string.confirm_remove_submission),
                        activity.getString(R.string.confirm_remove_submission_description, topic.title),
                        (dialog, button) -> {
                            // add ignore list changes locally
                            for (var image : topic.images) {
                                mIgnoreMediaSet.add(new MediaInfo(image.mediaId, topic.id, mSubreddit));
                            }
                            Intent intent = new Intent(activity, DeleteArtworkReceiver.class)
                                    .putExtra(DeleteArtworkReceiver.ACTION, DeleteArtworkReceiver.ACTION_DELETE)
                                    .putExtra(
                                            DeleteArtworkReceiver.MEDIA_ID_ARRAY,
                                            topic.images.stream()
                                                    .map(image -> image.mediaId)
                                                    .distinct()
                                                    .toArray(String[]::new))
                                    .putExtra(DeleteArtworkReceiver.MEDIA_TOPIC_ID, topic.id)
                                    .putExtra(DeleteArtworkReceiver.MEDIA_SUBREDDIT, mSubreddit);
                            activity.sendBroadcast(intent);
                            Toast.makeText(activity, btnRemove.getContentDescription(), Toast.LENGTH_SHORT)
                                    .show();
                            notifyItemChanged(topic);
                        })
                .show(fragmentManager);
    }

    private boolean onLongClickThumbnail(
            RecycleAdapterBase<ThumbnailAdapter.Item, ThumbnailAdapter.ThumbnailHolder> adapter,
            ThumbnailAdapter.Item thumbnail,
            View v,
            SubTopic topic) {
        final Context ctx = v.getContext();
        final MediaInfo media = new MediaInfo(thumbnail.image.mediaId, topic.id, mSubreddit);
        PopupMenu popupMenu = new PopupMenu(ctx, v, Gravity.START | Gravity.TOP);
        var menu = popupMenu.getMenu();
        if (mFavoriteMediaSet.contains(media)) {
            menu.add(R.string.unset_favorite).setOnMenuItemClickListener(item -> {
                DBHelper.removeFavorite(ctx, media);
                mFavoriteMediaSet.remove(media);
                adapter.notifyItemChanged(thumbnail);
                return true;
            });
        } else {
            menu.add(R.string.set_favorite).setOnMenuItemClickListener(item -> {
                DBHelper.insertFavorite(ctx, media);
                DBHelper.removeIgnoreMedia(ctx, media);
                mIgnoreMediaSet.remove(media);
                mFavoriteMediaSet.add(media);
                adapter.notifyItemChanged(thumbnail);
                return true;
            });
        }
        if (mIgnoreMediaSet.contains(media)) {
            menu.add(R.string.clear_ignore).setOnMenuItemClickListener(item -> {
                DBHelper.removeIgnoreMedia(ctx, media);
                mIgnoreMediaSet.remove(media);
                adapter.notifyItemChanged(thumbnail);
                return true;
            });
        } else {
            menu.add(R.string.add_ignore).setOnMenuItemClickListener(item -> {
                DBHelper.insertIgnoreMedia(ctx, media);
                DBHelper.removeFavorite(ctx, media);
                mFavoriteMediaSet.remove(media);
                mIgnoreMediaSet.add(media);
                adapter.notifyItemChanged(thumbnail);
                return true;
            });
        }
        menu.add(R.string.action_share_link).setOnMenuItemClickListener(item -> {
            Intent intent = new Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(
                            Intent.EXTRA_TEXT,
                            ctx.getString(
                                    R.string.title_and_link, topic.title, topic.author, topic.getPermalinkString()));
            var chooser = Intent.createChooser(intent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ViewUtils.launchIntent(v, chooser);
            return true;
        });
        popupMenu.show();
        return true;
    }

    @NonNull
    @Override
    public SubmissionHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final Context context = parent.getContext();

        LayoutInflater inflater = LayoutInflater.from(context);
        View itemView = inflater.inflate(R.layout.submission_item, parent, false);

        return new SubmissionHolder(itemView);
    }
}
