package rocks.tbog.livewallpaperit.preference;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;

public class BroadcastPreference extends Preference implements Preference.OnPreferenceClickListener {
    public BroadcastPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceClick(@NonNull Preference preference) {
        Context ctx = getContext();
        ctx.sendBroadcast(getIntent());
        Toast.makeText(ctx, preference.getTitle(), Toast.LENGTH_SHORT).show();
        return true;
    }
}
