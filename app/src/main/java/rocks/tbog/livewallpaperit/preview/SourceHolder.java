package rocks.tbog.livewallpaperit.preview;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import com.google.android.material.switchmaterial.SwitchMaterial;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import rocks.tbog.livewallpaperit.R;
import rocks.tbog.livewallpaperit.RecycleAdapterBase;
import rocks.tbog.livewallpaperit.Source;
import rocks.tbog.livewallpaperit.dialog.EditTextDialog;
import rocks.tbog.livewallpaperit.utils.ViewUtils;

public class SourceHolder extends RecycleAdapterBase.Holder {
    @Nullable
    private Source mSource = null;

    public final TextView subredditName;
    public final SwitchMaterial toggleSwitch;
    public final Button buttonRemove;
    public final Button buttonOpen;
    public final Button buttonPreview;
    public final Button buttonMinUpvotes;
    public final Button buttonMinScore;
    public final Button buttonMinComments;
    public final AutoCompleteTextView imageMinWidth;
    public final AutoCompleteTextView imageMinHeight;
    public final AutoCompleteTextView imageOrientation;

    public Observer<Source> mSourceChangedObserver;

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
    public final View.OnLongClickListener mShowTooltipFromContentDescription = v -> {
        Toast.makeText(v.getContext(), v.getContentDescription(), Toast.LENGTH_SHORT)
                .show();
        return true;
    };
    public final GetIntDialog mDialogSetMinUpvotes = new GetIntDialog() {
        @Override
        public int getInitialValue(@NonNull Source source) {
            return source.minUpvotePercentage;
        }

        @Override
        public void onIntChanged(@NonNull Source source, int value) {
            source.minUpvotePercentage = value;
        }
    };
    public final GetIntDialog mDialogSetMinScore = new GetIntDialog() {
        @Override
        public int getInitialValue(@NonNull Source source) {
            return source.minScore;
        }

        @Override
        public void onIntChanged(@NonNull Source source, int value) {
            source.minScore = value;
        }
    };
    public final GetIntDialog mDialogSetMinComments = new GetIntDialog() {
        @Override
        public int getInitialValue(@NonNull Source source) {
            return source.minComments;
        }

        @Override
        public void onIntChanged(@NonNull Source source, int value) {
            source.minComments = value;
        }
    };

    public SourceHolder(@NonNull View itemView) {
        super(itemView);

        subredditName = itemView.findViewById(R.id.subreddit_name);
        toggleSwitch = itemView.findViewById(R.id.toggle);
        buttonRemove = itemView.findViewById(R.id.button_remove);
        buttonOpen = itemView.findViewById(R.id.button_open);
        buttonPreview = itemView.findViewById(R.id.button_preview);
        buttonMinUpvotes = itemView.findViewById(R.id.button_min_upvote_percent);
        buttonMinScore = itemView.findViewById(R.id.button_min_score);
        buttonMinComments = itemView.findViewById(R.id.button_min_comments);
        imageMinWidth = itemView.findViewById(R.id.img_min_width);
        imageMinHeight = itemView.findViewById(R.id.img_min_height);
        imageOrientation = itemView.findViewById(R.id.img_orientation);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            buttonMinUpvotes.setOnLongClickListener(mShowTooltipFromContentDescription);
            buttonMinScore.setOnLongClickListener(mShowTooltipFromContentDescription);
            buttonMinComments.setOnLongClickListener(mShowTooltipFromContentDescription);
        }

        buttonMinUpvotes.setOnClickListener(mDialogSetMinUpvotes::showDialogOnClick);
        buttonMinScore.setOnClickListener(mDialogSetMinScore::showDialogOnClick);
        buttonMinComments.setOnClickListener(mDialogSetMinComments::showDialogOnClick);

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
    }

    public void bind(Source source, Observer<Source> sourceChangedObserver) {
        mSource = source;
        mSourceChangedObserver = sourceChangedObserver;
    }

    public void unbind() {
        mSource = null;
        mSourceChangedObserver = null;
    }

    public abstract class GetIntDialog {
        public void showDialogOnClick(View v) {
            if (mSource == null) return;
            EditTextDialog.Builder builder = new EditTextDialog.Builder(v.getContext())
                    .setTitle(v.getContentDescription())
                    .setInitialText(String.valueOf(getInitialValue(mSource)))
                    .setPositiveButton(android.R.string.ok, (dialog, button) -> {
                        EditText input = dialog.findViewById(R.id.rename);
                        if (input != null) {
                            input.setInputType(EditorInfo.TYPE_CLASS_NUMBER);
                            dialog.onConfirm(input.getText());
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null);

            EditTextDialog dialog = builder.getDialog();
            dialog.setOnConfirmListener(newValue -> {
                if (mSource == null) return;
                int value = 0;
                if (newValue != null) {
                    try {
                        value = Integer.parseInt(newValue.toString().trim());
                    } catch (NumberFormatException ignored) {
                        // keep value = 0
                    }
                }
                if (getInitialValue(mSource) != value) {
                    onIntChanged(mSource, value);
                    if (mSourceChangedObserver != null) {
                        mSourceChangedObserver.onChanged(mSource);
                    }
                }
            });
            Activity activity = ViewUtils.getActivity(v);
            if (activity instanceof AppCompatActivity) {
                dialog.show(((AppCompatActivity) activity).getSupportFragmentManager(), "int_dialog");
            }
        }

        public abstract int getInitialValue(@NonNull Source source);

        public abstract void onIntChanged(@NonNull Source source, int newValue);
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
