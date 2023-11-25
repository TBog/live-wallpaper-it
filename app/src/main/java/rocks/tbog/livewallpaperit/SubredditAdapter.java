package rocks.tbog.livewallpaperit;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.Objects;
import rocks.tbog.livewallpaperit.data.SubTopic;
import rocks.tbog.livewallpaperit.work.ArtLoadWorker;

public class SubredditAdapter extends RecycleAdapterBase<SubTopic, SubredditActivity.SubmissionHolder> {
    private boolean mAllowNSFW = true;
    private int mWidth = 108;
    private ArtLoadWorker.Filter mFilter = null;

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

    @Override
    public void onBindViewHolder(@NonNull SubredditActivity.SubmissionHolder holder, @NonNull SubTopic topic) {
        holder.mTitleView.setText(topic.title);
        boolean showObfuscatedPreview = topic.over18 && !mAllowNSFW;
        holder.mImageCarouselView.setAdapter(new ThumbnailAdapter(topic, mWidth, showObfuscatedPreview));
        holder.mNsfwView.setVisibility(topic.over18 ? View.VISIBLE : View.GONE);
        holder.mScoreView.setText(String.valueOf(topic.score));
        holder.mUpvoteView.setText(String.valueOf(topic.upvoteRatio));
        holder.mNumCommentView.setText(String.valueOf(topic.numComments));
        if (mFilter == null) {
            holder.mValidView.setVisibility(View.GONE);
        } else {
            holder.mValidView.setVisibility(View.VISIBLE);
            if (ArtLoadWorker.shouldSkipTopic(topic, mFilter)) {
                holder.mValidView.setImageResource(R.drawable.ic_invalid_24);
            } else {
                holder.mValidView.setImageResource(R.drawable.ic_validated_24);
            }
        }
    }

    @NonNull
    @Override
    public SubredditActivity.SubmissionHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final Context context = parent.getContext();

        LayoutInflater inflater = LayoutInflater.from(context);
        View itemView = inflater.inflate(R.layout.submission_item, parent, false);

        return new SubredditActivity.SubmissionHolder(itemView);
    }
}
