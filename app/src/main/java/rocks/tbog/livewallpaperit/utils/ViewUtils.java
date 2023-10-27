package rocks.tbog.livewallpaperit.utils;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ViewUtils {
    private static final int[] ON_SCREEN_POS = new int[2];
    private static final Rect ON_SCREEN_RECT = new Rect();
    private static final String TAG = ViewUtils.class.getSimpleName();

    /**
     * Return a valid activity or null given a view
     *
     * @param view any view of an activity
     * @return an activity or null
     */
    @Nullable
    public static Activity getActivity(@Nullable View view) {
        if (view != null) {
            return getActivity(view.getContext());
        } else {
            return null;
        }
    }

    /**
     * Return a valid activity or null given a context
     *
     * @param ctx context
     * @return an activity or null
     */
    @Nullable
    public static Activity getActivity(@Nullable Context ctx) {
        while (ctx instanceof ContextWrapper) {
            if (ctx instanceof Activity) {
                Activity act = (Activity) ctx;
                if (act.isFinishing() || act.isDestroyed()) return null;
                return act;
            }
            ctx = ((ContextWrapper) ctx).getBaseContext();
        }
        return null;
    }

    public static void launchIntent(@NonNull Activity activity, @Nullable View view, @NonNull Intent intent) {
        setIntentSourceBounds(intent, view);
        Bundle startActivityOptions = makeStartActivityOptions(view);
        try {
            activity.startActivity(intent, startActivityOptions);
        } catch (ActivityNotFoundException e) {
            Log.w(TAG, "startActivity failed", e);
        }
    }

    public static void launchIntent(@NonNull View view, @NonNull Intent intent) {
        Activity activity = getActivity(view);
        if (activity == null) return;
        launchIntent(activity, view, intent);
    }

    public static void setIntentSourceBounds(@NonNull Intent intent, @Nullable View v) {
        if (v == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            v.getLocationOnScreen(ON_SCREEN_POS);
            ON_SCREEN_RECT.set(
                    ON_SCREEN_POS[0],
                    ON_SCREEN_POS[1],
                    ON_SCREEN_POS[0] + v.getWidth(),
                    ON_SCREEN_POS[1] + v.getHeight());
            intent.setSourceBounds(ON_SCREEN_RECT);
        }
    }

    @Nullable
    public static Bundle makeStartActivityOptions(@Nullable View source) {
        if (source == null) return null;
        Bundle opts = null;
        // If we got an icon, we create options to get a nice animation
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            opts = ActivityOptions.makeClipRevealAnimation(
                            source, 0, 0, source.getMeasuredWidth(), source.getMeasuredHeight())
                    .toBundle();
        }
        if (opts == null) {
            opts = ActivityOptions.makeScaleUpAnimation(
                            source, 0, 0, source.getMeasuredWidth(), source.getMeasuredHeight())
                    .toBundle();
        }
        return opts;
    }
}
