package rocks.tbog.livewallpaperit.preview;

import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.divider.MaterialDividerItemDecoration;
import rocks.tbog.livewallpaperit.R;
import rocks.tbog.livewallpaperit.RecycleAdapterBase;

public class SubmissionHolder extends RecycleAdapterBase.Holder {
    public final TextView mInfoView;
    public final TextView mTitleView;
    public final Button mButtonOpen;
    public final Button mButtonRemove;
    public final RecyclerView mImageCarouselView;
    public final ImageView mNsfwView;
    public final TextView mScoreView;
    public final TextView mUpvoteView;
    public final TextView mNumCommentView;
    public final ImageView mInvalidView;

    public SubmissionHolder(@NonNull View itemView) {
        super(itemView);

        mInfoView = itemView.findViewById(R.id.submission_info);
        mTitleView = itemView.findViewById(R.id.submission_title);
        mButtonOpen = itemView.findViewById(R.id.button_open);
        mButtonRemove = itemView.findViewById(R.id.button_remove);
        mImageCarouselView = itemView.findViewById(R.id.image_carousel);
        mNsfwView = itemView.findViewById(R.id.nsfw);
        mScoreView = itemView.findViewById(R.id.score);
        mUpvoteView = itemView.findViewById(R.id.upvote_ratio);
        mNumCommentView = itemView.findViewById(R.id.num_comments);
        mInvalidView = itemView.findViewById(R.id.invalid);

        var layout = new LinearLayoutManager(mImageCarouselView.getContext(), RecyclerView.HORIZONTAL, false);
        var decoration = new MaterialDividerItemDecoration(mImageCarouselView.getContext(), layout.getOrientation());
        decoration.setLastItemDecorated(false);
        mImageCarouselView.addItemDecoration(decoration);
    }
}
