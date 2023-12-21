package rocks.tbog.livewallpaperit;

import android.app.Application;
import android.os.StatFs;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Configuration;
import com.squareup.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;
import java.io.File;
import java.util.concurrent.TimeUnit;
import kotlin.random.Random;
import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import rocks.tbog.livewallpaperit.WorkAsync.AsyncUtils;

public class LWIApplication extends Application implements Configuration.Provider {
    private static final String PICASSO_CACHE = "picasso-cache";
    private static final int MIN_DISK_CACHE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final int MAX_DISK_CACHE_SIZE = 50 * 1024 * 1024; // 50MB
    private static final int MAX_RETRY_COUNT = 5; // Default is 3 in Picasso

    //    @Override
    public void onCreate() {
        super.onCreate();
        File cacheDir = createDefaultCacheDir();
        var httpClient = new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .callTimeout(10, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)
                .addInterceptor(chain -> {
                    Request request = chain.request();
                    // try the request
                    Response response = chain.proceed(request);
                    int tryCount = 0;
                    while (!response.isSuccessful() && tryCount < MAX_RETRY_COUNT) {
                        Log.d("okHttp", "request code=" + response.code() + " - tryCount=" + tryCount);
                        tryCount++;
                        try {
                            //noinspection BusyWait
                            Thread.sleep(Math.round(Math.pow(tryCount, 1.666) * Random.Default.nextDouble(150, 200)));
                        } catch (InterruptedException ignored) {
                            // ignore exception, we can try again even if sleep failed
                        }
                        // retry the request
                        response = chain.proceed(request);
                        if (response.code() == 404) {
                            Log.v("okHttp", "404 on " + request.url());
                            break;
                        }
                    }
                    // otherwise just pass the original response on
                    return response;
                })
                .cache(new Cache(cacheDir, calculateDiskCacheSize(cacheDir)))
                .build();
        var picassoBuilder = new Picasso.Builder(getApplicationContext())
                .executor(AsyncUtils.EXECUTOR_RUN_ASYNC)
                // .indicatorsEnabled(BuildConfig.DEBUG)
                .downloader(new OkHttp3Downloader(httpClient));
        Picasso.setSingletonInstance(picassoBuilder.build());
    }

    private File createDefaultCacheDir() {
        File cache = new File(getApplicationContext().getCacheDir(), PICASSO_CACHE);
        if (!cache.exists()) {
            //noinspection ResultOfMethodCallIgnored
            cache.mkdirs();
        }
        return cache;
    }

    private static long calculateDiskCacheSize(File dir) {
        long size = MIN_DISK_CACHE_SIZE;

        try {
            StatFs statFs = new StatFs(dir.getAbsolutePath());
            long blockCount = statFs.getBlockCountLong();
            long blockSize = statFs.getBlockSizeLong();
            long available = blockCount * blockSize;
            // Target 2% of the total space.
            size = available * 2 / 100;
        } catch (IllegalArgumentException ignored) {
        }

        // Bound inside min/max size for disk cache.
        return Math.max(Math.min(size, MAX_DISK_CACHE_SIZE), MIN_DISK_CACHE_SIZE);
    }

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
