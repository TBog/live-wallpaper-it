package rocks.tbog.livewallpaperit.preview;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.lifecycle.Observer;
import com.google.android.material.switchmaterial.SwitchMaterial;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import rocks.tbog.livewallpaperit.R;
import rocks.tbog.livewallpaperit.RecycleAdapterBase;
import rocks.tbog.livewallpaperit.Source;

public class SourceHolder extends RecycleAdapterBase.Holder {
    @Nullable
    private Source mSource = null;

    public final TextView subredditName;
    public final SwitchMaterial toggleSwitch;
    public final Button buttonRemove;
    public final Button buttonOpen;
    public final Button buttonPreview;
    public final TextView minUpvotePercentage;
    public final TextView minScore;
    public final TextView minComments;
    public final AutoCompleteTextView imageMinWidth;
    public final AutoCompleteTextView imageMinHeight;
    public final AutoCompleteTextView imageOrientation;

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
    public final AdapterView.OnItemClickListener mImageWidthListener = new IntItemSelectedListener() {
        @Override
        void onSelectionChanged(@NonNull Source source, @Nullable Integer newValue) {
            int value;
            if (newValue != null) {
                value = newValue;
            } else {
                value = 0;
            }
            if (source.imageMinWidth == value) return;
            source.imageMinWidth = value;
            mSourceChangedObserver.onChanged(source);
        }
    };
    public final AdapterView.OnItemClickListener mImageHeightListener = new IntItemSelectedListener() {
        @Override
        void onSelectionChanged(@NonNull Source source, @Nullable Integer newValue) {
            final int value;
            if (newValue != null) {
                value = newValue;
            } else {
                value = 0;
            }
            if (source.imageMinHeight == value) return;
            source.imageMinHeight = value;
            mSourceChangedObserver.onChanged(source);
        }
    };
    public final AdapterView.OnItemClickListener mImageOrientationListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (mSource == null) return;
            var values = parent.getResources().getIntArray(R.array.image_orientation_values);
            Source.Orientation orientation = Source.Orientation.fromInt(values[position]);
            if (mSource.imageOrientation == orientation) return;
            mSource.imageOrientation = orientation;
            mSourceChangedObserver.onChanged(mSource);
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
        imageMinWidth = itemView.findViewById(R.id.img_min_width);
        imageMinHeight = itemView.findViewById(R.id.img_min_height);
        imageOrientation = itemView.findViewById(R.id.img_orientation);

        Context ctx = itemView.getContext();
        var widthList = IntStream.range(108, 2160).boxed().collect(Collectors.toList());
        var widthAdapter = new ArrayAdapter<>(ctx, android.R.layout.simple_dropdown_item_1line, widthList);
        imageMinWidth.setAdapter(widthAdapter);
        imageMinWidth.setOnFocusChangeListener((v, hasFocus) -> {
            if (mSource == null) return;
            if (hasFocus) return;
            final String value;
            if (mSource.imageMinWidth > 0) {
                value = String.valueOf(mSource.imageMinWidth);
            } else {
                value = "";
            }
            imageMinWidth.setText(value);
        });

        var heightList = IntStream.range(108, 3840).boxed().collect(Collectors.toList());
        var heightAdapter = new ArrayAdapter<>(ctx, android.R.layout.simple_dropdown_item_1line, heightList);
        imageMinHeight.setAdapter(heightAdapter);
        imageMinHeight.setOnFocusChangeListener((v, hasFocus) -> {
            if (mSource == null) return;
            if (hasFocus) return;
            final String value;
            if (mSource.imageMinHeight > 0) {
                value = String.valueOf(mSource.imageMinHeight);
            } else {
                value = "";
            }
            imageMinHeight.setText(value);
        });

        var dropdownOptions = ctx.getResources().getTextArray(R.array.image_orientation_display);
        imageOrientation.setAdapter(
                new ArrayAdapter<>(ctx, android.R.layout.simple_dropdown_item_1line, dropdownOptions));

        setTransitionFocusChangedListeners((MotionLayout) itemView);
    }

    private void setTransitionFocusChangedListeners(@NonNull MotionLayout parent) {
        minUpvotePercentage.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) return;
            parent.transitionToState(R.id.expanded_min_upvote_percent);
        });
        minScore.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) return;
            parent.transitionToState(R.id.expanded_min_score);
        });
        minComments.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) return;
            parent.transitionToState(R.id.expanded_min_comments);
        });
    }

    public void bind(Source source, Observer<Source> sourceChangedObserver) {
        mSource = source;
        mSourceChangedObserver = sourceChangedObserver;

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

    public abstract class TextChangedWatcher implements TextWatcher {
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

    public abstract class ToggleChangedListener implements CompoundButton.OnCheckedChangeListener {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (mSource == null) return;
            onToggleChanged(mSource, isChecked);
        }

        abstract void onToggleChanged(@NonNull Source mSource, boolean isChecked);
    }

    public abstract class IntItemSelectedListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (mSource == null) return;
            var item = parent.getItemAtPosition(position);
            if (item instanceof Integer) {
                onSelectionChanged(mSource, (Integer) item);
            } else if (item instanceof String) {
                int value;
                try {
                    value = Integer.parseInt((String) item);
                } catch (NumberFormatException ignored) {
                    return;
                }
                onSelectionChanged(mSource, value);
            }
        }

        abstract void onSelectionChanged(@NonNull Source source, @Nullable Integer value);
    }
}
