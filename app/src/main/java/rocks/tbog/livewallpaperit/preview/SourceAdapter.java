package rocks.tbog.livewallpaperit.preview;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;
import java.util.ArrayList;
import rocks.tbog.livewallpaperit.R;
import rocks.tbog.livewallpaperit.RecycleAdapterBase;
import rocks.tbog.livewallpaperit.Source;
import rocks.tbog.livewallpaperit.dialog.DialogHelper;
import rocks.tbog.livewallpaperit.utils.ViewUtils;

public class SourceAdapter extends RecycleAdapterBase<Source, SourceHolder> {
    private final Observer<Source> mSourceChangedObserver;
    private final Observer<Source> mSourceRemovedObserver;

    public SourceAdapter(Observer<Source> changeObserver, Observer<Source> removeObserver) {
        super(new ArrayList<>());
        mSourceChangedObserver = changeObserver;
        mSourceRemovedObserver = removeObserver;
    }

    @NonNull
    @Override
    public SourceHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final Context context = parent.getContext();

        LayoutInflater inflater = LayoutInflater.from(context);
        View itemView = inflater.inflate(R.layout.source_item, parent, false);

        return new SourceHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull SourceHolder holder, @SuppressLint("RecyclerView") @NonNull Source source) {
        holder.bind(source, mSourceChangedObserver);

        holder.subredditName.setText(source.subreddit);
        holder.toggleSwitch.setOnCheckedChangeListener(null);
        holder.toggleSwitch.setChecked(source.isEnabled);
        holder.toggleSwitch.setOnCheckedChangeListener(holder.mToggleListener);

        // minUpvotePercentage
        holder.minUpvotePercentage.removeTextChangedListener(holder.mUpvotePercentageWatcher);
        holder.minUpvotePercentage.setText(intToString(source.minUpvotePercentage));
        holder.minUpvotePercentage.addTextChangedListener(holder.mUpvotePercentageWatcher);

        // minScore
        holder.minScore.removeTextChangedListener(holder.mScoreWatcher);
        holder.minScore.setText(intToString(source.minScore));
        holder.minScore.addTextChangedListener(holder.mScoreWatcher);

        // minComments
        holder.minComments.removeTextChangedListener(holder.mCommentsWatcher);
        holder.minComments.setText(intToString(source.minComments));
        holder.minComments.addTextChangedListener(holder.mCommentsWatcher);

        // preview button
        holder.buttonPreview.setOnClickListener(v -> {
            Activity activity = ViewUtils.getActivity(v);
            if (activity == null) return;
            Intent intent = new Intent(activity, SubredditActivity.class)
                    .putExtra(SubredditActivity.EXTRA_SOURCE, source)
                    .putExtra(SubredditActivity.EXTRA_SUBREDDIT, source.subreddit);
            ViewUtils.launchIntent(activity, v, intent);
        });

        // open button
        holder.buttonOpen.setOnClickListener(v -> {
            Uri urlToOpen = Uri.parse("https://www.reddit.com/r/")
                    .buildUpon()
                    .appendPath(source.subreddit)
                    .build();
            ViewUtils.launchIntent(v, new Intent(Intent.ACTION_VIEW).setData(urlToOpen));
        });

        // remove button
        holder.buttonRemove.setOnClickListener(v -> {
            Activity activity = ViewUtils.getActivity(v);
            FragmentManager fragmentManager = null;
            if (activity instanceof AppCompatActivity) {
                fragmentManager = ((AppCompatActivity) activity).getSupportFragmentManager();
            }
            if (fragmentManager == null) return;
            DialogHelper.makeConfirmDialog(
                            activity.getString(R.string.confirm_remove_subreddit, source.subreddit),
                            activity.getString(R.string.confirm_remove_subreddit_description, source.subreddit),
                            (dialog, button) -> {
                                mSourceRemovedObserver.onChanged(source);
                                removeItem(source);
                            })
                    .show(fragmentManager);
        });
    }

    private static String intToString(int value) {
        if (value <= 0) return "";
        return Integer.toString(value);
    }
}
