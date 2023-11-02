package rocks.tbog.livewallpaperit.preference;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.NumberPicker;
import androidx.annotation.NonNull;
import androidx.preference.DialogPreference;
import java.util.Objects;
import rocks.tbog.livewallpaperit.R;

public class NumberPickerDialog extends BasePreferenceDialog {

    public static NumberPickerDialog newInstance(String key) {
        NumberPickerDialog fragment = new NumberPickerDialog();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);

        return fragment;
    }

    public static String getValueText(@NonNull Context context, int value) {
        return context.getResources().getQuantityString(R.plurals.artwork_count, value, value);
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        DialogPreference dialogPreference = getPreference();
        if (!(dialogPreference instanceof CustomDialogPreference)) return;
        CustomDialogPreference preference = (CustomDialogPreference) dialogPreference;
        if (positiveResult) {
            // save data when user clicked OK
            preference.persistValueIfAllowed();
        } else {
            preference.resetValue();
        }
    }

    @Override
    protected void onBindDialogView(@NonNull View root) {
        super.onBindDialogView(root);
        CustomDialogPreference preference = (CustomDialogPreference) getPreference();
        final String key = preference.getKey();

        NumberPicker numberPicker = root.findViewById(R.id.number_picker);
        int number = 0;

        Context ctx = requireContext();
        if ("desired-artwork-count".equals(key)) {
            numberPicker.setMinValue(0);
            numberPicker.setMaxValue(10);
            String[] displayedValues = new String[numberPicker.getMaxValue() - numberPicker.getMinValue() + 1];
            for (int i = 0; i < displayedValues.length; i += 1) {
                int value;
                if (i == 0) {
                    value = 1;
                } else {
                    value = (i * 5);
                }
                if (Objects.equals(preference.getValue(), value)) {
                    number = i;
                }
                displayedValues[i] = getValueText(ctx, value);
            }
            numberPicker.setDisplayedValues(displayedValues);
        }

        numberPicker.setValue(number);
        numberPicker.setWrapSelectorWheel(false);
        numberPicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
            CustomDialogPreference pref = ((CustomDialogPreference) NumberPickerDialog.this.getPreference());
            int value;
            if (newVal == 0) {
                value = 1;
            } else {
                value = (newVal * 5);
            }
            pref.setValue(value);
        });
    }
}
