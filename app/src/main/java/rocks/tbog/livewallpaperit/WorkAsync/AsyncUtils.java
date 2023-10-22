package rocks.tbog.livewallpaperit.WorkAsync;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class AsyncUtils {
    public static final ExecutorService EXECUTOR_RUN_ASYNC;
    private static final int CORE_POOL_SIZE = 1;
    private static final int MAXIMUM_POOL_SIZE = 10;
    private static final int KEEP_ALIVE_SECONDS = 3;
    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        public Thread newThread(Runnable r) {
            return new Thread(r, "UtilAsync #" + mCount.getAndIncrement());
        }
    };
    private static final String TAG = AsyncUtils.class.getSimpleName();

    static {
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE_SECONDS, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(), sThreadFactory);
        threadPoolExecutor.setRejectedExecutionHandler((runnable, executor) -> {
            Log.w(TAG, "task rejected");
            if (!executor.isShutdown()) {
                runnable.run();
            }
        });
        EXECUTOR_RUN_ASYNC = threadPoolExecutor;
    }
    public static RunnableTask runAsync(@NonNull Lifecycle lifecycle, @NonNull TaskRunner.AsyncRunnable background, @NonNull TaskRunner.AsyncRunnable after) {
        RunnableTask task = TaskRunner.newTask(lifecycle, background, after);
        TaskRunner.runOnUiThread(() -> EXECUTOR_RUN_ASYNC.execute(task));
        return task;
    }

    public static RunnableTask runAsync(@NonNull TaskRunner.AsyncRunnable background, @Nullable TaskRunner.AsyncRunnable after) {
        RunnableTask task = TaskRunner.newTask(background, after);
        TaskRunner.runOnUiThread(() -> EXECUTOR_RUN_ASYNC.execute(task));
        return task;
    }

    public static void runAsync(@NonNull Runnable background) {
        EXECUTOR_RUN_ASYNC.execute(background);
    }

    public static <I, O> void executeAsync(@NonNull AsyncTask<I, O> task) {
        TaskRunner.executeOnExecutor(EXECUTOR_RUN_ASYNC, task);
    }
}
