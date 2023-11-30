package rocks.tbog.livewallpaperit.preview;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.lifecycle.Observer;
import com.google.android.material.switchmaterial.SwitchMaterial;
import rocks.tbog.livewallpaperit.R;
import rocks.tbog.livewallpaperit.RecycleAdapterBase;
import rocks.tbog.livewallpaperit.Source;

public class SourceHolder extends RecycleAdapterBase.Holder {
    public final TextView subredditName;
    public final SwitchMaterial toggleSwitch;
    public final Button buttonRemove;
    public final Button buttonOpen;
    public final Button buttonPreview;
    public final TextView minUpvotePercentage;
    public final TextView minScore;
    public final TextView minComments;

    public Observer<Source> mSourceChangedObserver;

    public final TextChangedWatcher mUpvotePercentageWatcher = new TextChangedWatcher() {
        @Override
        public void onIntChanged(@NonNull Source source, int newValue) {
            if (newValue == source.minUpvotePercentage) return;
            source.minUpvotePercentage = newValue;
            mSourceChangedObserver.onChanged(source);
        }
    };
    public final TextChangedWatcher mScoreWatcher = new TextChangedWatcher() {
        @Override
        public void onIntChanged(@NonNull Source source, int newValue) {
            if (newValue == source.minScore) return;
            source.minScore = newValue;
            mSourceChangedObserver.onChanged(source);
        }
    };
    public final TextChangedWatcher mCommentsWatcher = new TextChangedWatcher() {
        @Override
        public void onIntChanged(@NonNull Source source, int newValue) {
            if (newValue == source.minComments) return;
            source.minComments = newValue;
            mSourceChangedObserver.onChanged(source);
        }
    };
    public final ToggleChangedListener mToggleListener = new ToggleChangedListener() {
        @Override
        void onToggleChanged(@NonNull Source source, boolean isChecked) {
            if (source.isEnabled == isChecked) return;
            source.isEnabled = isChecked;
            mSourceChangedObserver.onChanged(source);
        }
    };

    public SourceHolder(@NonNull View itemView) {
        super(itemView);

        subredditName = itemView.findViewById(R.id.subreddit_name);
        toggleSwitch = itemView.findViewById(R.id.toggle);
        buttonRemove = itemView.findViewById(R.id.button_remove);
        buttonOpen = itemView.findViewById(R.id.button_open);
        buttonPreview = itemView.findViewById(R.id.button_preview);
        minUpvotePercentage = itemView.findViewById(R.id.min_upvote_percent);
        minScore = itemView.findViewById(R.id.min_score);
        minComments = itemView.findViewById(R.id.min_comments);

        final MotionLayout parent = (MotionLayout) itemView;
        minUpvotePercentage.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                parent.transitionToState(R.id.expanded_min_upvote_percent);
            }
        });
        minScore.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                parent.transitionToState(R.id.expanded_min_score);
            }
        });
        minComments.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                parent.transitionToState(R.id.expanded_min_comments);
            }
        });
    }

    public void bind(Source source, Observer<Source> sourceChangedObserver) {
        mSourceChangedObserver = sourceChangedObserver;
        mUpvotePercentageWatcher.mSource = source;
        mScoreWatcher.mSource = source;
        mCommentsWatcher.mSource = source;
        mToggleListener.mSource = source;

        final MotionLayout parent = (MotionLayout) itemView;
        final View.OnKeyListener blurOnEnter = (view, keyCode, event) -> {
            if ((event == null || event.getAction() == KeyEvent.ACTION_DOWN) && keyCode == KeyEvent.KEYCODE_ENTER) {
                view.clearFocus();
                parent.transitionToState(R.id.base_state);
            }
            return false;
        };

        final TextView.OnEditorActionListener blurOnDone = (view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                view.clearFocus();
                parent.transitionToState(R.id.base_state);
            }
            return false;
        };

        minUpvotePercentage.setOnKeyListener(blurOnEnter);
        minUpvotePercentage.setOnEditorActionListener(blurOnDone);
        minScore.setOnKeyListener(blurOnEnter);
        minScore.setOnEditorActionListener(blurOnDone);
        minComments.setOnKeyListener(blurOnEnter);
        minComments.setOnEditorActionListener(blurOnDone);
    }

    public abstract static class TextChangedWatcher implements TextWatcher {
        @Nullable
        private Source mSource = null;

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            if (mSource == null) return;
            try {
                int newValue = Integer.parseInt(s.toString());
                onIntChanged(mSource, newValue);
            } catch (Exception ignored) {
                // ignore invalid text
            }
        }

        abstract void onIntChanged(@NonNull Source source, int newValue);
    }

    public abstract static class ToggleChangedListener implements CompoundButton.OnCheckedChangeListener {
        @Nullable
        private Source mSource = null;

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (mSource == null) return;
            onToggleChanged(mSource, isChecked);
        }

        abstract void onToggleChanged(@NonNull Source mSource, boolean isChecked);
    }
}
