package rocks.tbog.livewallpaperit.preview;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import rocks.tbog.livewallpaperit.DeleteArtworkReceiver;
import rocks.tbog.livewallpaperit.R;
import rocks.tbog.livewallpaperit.RecycleAdapterBase;
import rocks.tbog.livewallpaperit.Source;
import rocks.tbog.livewallpaperit.data.SubTopic;
import rocks.tbog.livewallpaperit.dialog.DialogHelper;
import rocks.tbog.livewallpaperit.utils.ViewUtils;
import rocks.tbog.livewallpaperit.work.ArtLoadWorker;

public class SubredditAdapter extends RecycleAdapterBase<SubTopic, SubmissionHolder> {
    private boolean mAllowNSFW = true;
    private int mWidth = 108;
    private ArtLoadWorker.Filter mFilter = null;
    private final ArraySet<String> mIgnoreMediaIdSet = new ArraySet<>();

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

    public void setFilterFromSource(@Nullable Source source) {
        ArtLoadWorker.Filter filter = ArtLoadWorker.Filter.fromSource(source);
        if (filter != null) {
            filter.allowNSFW = mAllowNSFW;
        }
        if (Objects.equals(mFilter, filter)) return;
        mFilter = filter;
        notifyItemRangeChanged(0, getItemCount());
    }

    public void setIgnoreList(@NonNull List<String> ignoreList) {
        mIgnoreMediaIdSet.clear();
        mIgnoreMediaIdSet.addAll(ignoreList);
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
        holder.mInfoView.setText(displayAgo);
        holder.mTitleView.setText(topic.title);
        holder.mButtonOpen.setOnClickListener(v -> {
            Uri urlToOpen = Uri.parse("https://www.reddit.com" + topic.permalink);
            ViewUtils.launchIntent(v, new Intent(Intent.ACTION_VIEW).setData(urlToOpen));
        });
        holder.mButtonRemove.setOnClickListener(v -> {
            Activity activity = ViewUtils.getActivity(v);
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
                                    mIgnoreMediaIdSet.add(image.mediaId);
                                }
                                Intent intent = new Intent(activity, DeleteArtworkReceiver.class)
                                        .putExtra(DeleteArtworkReceiver.ACTION, DeleteArtworkReceiver.ACTION_DELETE)
                                        .putExtra(
                                                DeleteArtworkReceiver.MEDIA_ID_ARRAY,
                                                topic.images.stream()
                                                        .map(image -> image.mediaId)
                                                        .distinct()
                                                        .toArray(String[]::new));
                                activity.sendBroadcast(intent);
                                Toast.makeText(
                                                activity,
                                                holder.mButtonRemove.getContentDescription(),
                                                Toast.LENGTH_SHORT)
                                        .show();
                                notifyItemChanged(holder.getAdapterPosition());
                            })
                    .show(fragmentManager);
        });
        boolean showObfuscatedPreview = topic.over18 && !mAllowNSFW;
        holder.mImageCarouselView.setAdapter(
                new ThumbnailAdapter(topic, mWidth, showObfuscatedPreview, mIgnoreMediaIdSet));
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

    @NonNull
    @Override
    public SubmissionHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final Context context = parent.getContext();

        LayoutInflater inflater = LayoutInflater.from(context);
        View itemView = inflater.inflate(R.layout.submission_item, parent, false);

        return new SubmissionHolder(itemView);
    }
}
