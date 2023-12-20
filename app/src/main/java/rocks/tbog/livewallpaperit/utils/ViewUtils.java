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
import android.view.ViewTreeObserver;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;

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
            opts = ActivityOptions.makeClipRevealAnimation(source, 0, 0, source.getWidth(), source.getHeight())
                    .toBundle();
        }
        if (opts == null) {
            opts = ActivityOptions.makeScaleUpAnimation(source, 0, 0, source.getWidth(), source.getHeight())
                    .toBundle();
        }
        return opts;
    }

    public interface ViewAction<T extends View> {
        void run(T view);
    }

    public static <T extends View> void doOnNextLayout(@NonNull T view, @NonNull ViewAction<T> action) {
        view.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(
                    View v,
                    int left,
                    int top,
                    int right,
                    int bottom,
                    int oldLeft,
                    int oldTop,
                    int oldRight,
                    int oldBottom) {
                view.removeOnLayoutChangeListener(this);
                action.run(view);
            }
        });
    }

    public static <T extends View> void doOnLayout(@NonNull T view, @NonNull ViewAction<T> action) {
        if (ViewCompat.isLaidOut(view) && !view.isLayoutRequested()) {
            action.run(view);
        } else {
            doOnNextLayout(view, action);
        }
    }

    public static <T extends View> void doOnPreDraw(@NonNull T view, @NonNull ViewAction<T> action) {
        final ViewTreeObserver vto = view.getViewTreeObserver();
        vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                action.run(view);
                if (vto.isAlive()) {
                    vto.removeOnPreDrawListener(this);
                } else {
                    view.getViewTreeObserver().removeOnPreDrawListener(this);
                }
                return true;
            }
        });
    }
}
