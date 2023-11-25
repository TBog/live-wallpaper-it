package rocks.tbog.livewallpaperit.preference;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.NumberPicker;
import androidx.annotation.NonNull;
import androidx.annotation.PluralsRes;
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

    public static String getValueText(@NonNull Context context, @PluralsRes int plurals, int value) {
        return context.getResources().getQuantityString(plurals, value, value);
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

    interface PreferenceValueFromPicker {
        void setPreferenceValueFromPicker(@NonNull CustomDialogPreference pref, int pickerValue);
    }

    @Override
    protected void onBindDialogView(@NonNull View root) {
        super.onBindDialogView(root);
        CustomDialogPreference preference = (CustomDialogPreference) getPreference();
        final String key = preference.getKey();

        NumberPicker numberPicker = root.findViewById(R.id.number_picker);
        int number = 0;

        Context ctx = requireContext();
        final PreferenceValueFromPicker valueSetter;
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
                displayedValues[i] = getValueText(ctx, R.plurals.artwork_count, value);
            }
            numberPicker.setDisplayedValues(displayedValues);
            valueSetter = (pref, newVal) -> {
                int value;
                if (newVal == 0) {
                    value = 1;
                } else {
                    value = (newVal * 5);
                }
                pref.setValue(value);
            };
        } else if ("image-thumbnail-width".equals(key)) {
            numberPicker.setMinValue(1);
            numberPicker.setMaxValue(17);
            String[] displayedValues = new String[numberPicker.getMaxValue() - numberPicker.getMinValue() + 1];
            for (int i = 0; i < displayedValues.length; i += 1) {
                int value = (i + 1) * 54;
                if (Objects.equals(preference.getValue(), value)) {
                    number = i + numberPicker.getMinValue();
                }
                displayedValues[i] = ctx.getString(R.string.thumbnail_width, value);
            }
            numberPicker.setDisplayedValues(displayedValues);
            valueSetter = (pref, value) -> pref.setValue(value * 54);
        } else {
            valueSetter = CustomDialogPreference::setValue;
        }

        numberPicker.setValue(number);
        numberPicker.setWrapSelectorWheel(false);
        numberPicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
            CustomDialogPreference pref = ((CustomDialogPreference) NumberPickerDialog.this.getPreference());
            valueSetter.setPreferenceValueFromPicker(pref, newVal);
        });
    }
}
