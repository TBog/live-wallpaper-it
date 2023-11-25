package rocks.tbog.livewallpaperit.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import java.util.Collections;
import rocks.tbog.livewallpaperit.R;

public class CustomDialogPreference extends androidx.preference.DialogPreference {

    private Object mValue = null;

    public CustomDialogPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setSummaryProvider();
    }

    public CustomDialogPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setSummaryProvider();
    }

    public CustomDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setSummaryProvider();
    }

    public CustomDialogPreference(Context context) {
        super(context);
        setSummaryProvider();
    }

    private void setSummaryProvider() {
        setSummaryProvider(new CustomDialogSummaryProvider());
    }

    public Object getValue() {
        return mValue;
    }

    public void setValue(Object value) {
        mValue = value;
    }

    public boolean persistValue() {
        Object value = getValue();
        if (value instanceof String) return persistString((String) value);
        else if (value instanceof Integer) return persistInt((Integer) value);
        else if (value instanceof Float) return persistFloat((Float) value);
        return false;
    }

    public void resetValue() {
        final var pref = getSharedPreferences();
        final var prefMap = pref != null ? pref.getAll() : Collections.emptyMap();
        Object value = prefMap.get(getKey());
        setValue(value);
    }

    public boolean persistValueIfAllowed() {
        if (callChangeListener(getValue())) {
            notifyChanged();
            return persistValue();
        }
        return false;
    }

    public boolean persistValueIfAllowed(Object value) {
        if (callChangeListener(value)) {
            setValue(value);
            notifyChanged();
            return persistValue();
        }
        return false;
    }

    @Override
    protected void onSetInitialValue(@Nullable Object defaultValue) {
        if (defaultValue == null) {
            resetValue();
        } else if (getValue() == null) {
            setValue(defaultValue);
        }
        persistValue();
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        try {
            return a.getInteger(index, 0);
        } catch (UnsupportedOperationException e) {
            try {
                return a.getFloat(index, 0f);
            } catch (UnsupportedOperationException ignored) {
                return a.getString(index);
            }
        }
    }

    private static class CustomDialogSummaryProvider implements Preference.SummaryProvider<CustomDialogPreference> {

        @Nullable
        @Override
        public CharSequence provideSummary(@NonNull CustomDialogPreference preference) {
            final String key = preference.getKey();
            final Object value = preference.getValue();
            String summary = null;
            if ("desired-artwork-count".equals(key)) {
                if (value instanceof Integer) {
                    summary = NumberPickerDialog.getValueText(
                            preference.getContext(), R.plurals.artwork_count, ((Integer) value).intValue());
                }
            } else if ("image-thumbnail-width".equals(key)) {
                if (value instanceof Integer) {
                    summary = preference.getContext().getString(R.string.thumbnail_width, ((Integer) value).intValue());
                }
            }
            return summary;
        }
    }
}
