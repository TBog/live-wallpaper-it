package rocks.tbog.livewallpaperit;

import android.app.Application;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Configuration;

public class LWIApplication extends Application implements Configuration.Provider {
    @NonNull
    @Override
    public Configuration getWorkManagerConfiguration() {
        final Configuration cfg;
        if (BuildConfig.DEBUG) {
            cfg = new Configuration.Builder().setMinimumLoggingLevel(Log.DEBUG).build();
        } else {
            cfg = new Configuration.Builder().setMinimumLoggingLevel(Log.ERROR).build();
        }
        return cfg;
    }
}
