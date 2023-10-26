package rocks.tbog.livewallpaperit.work;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.kirkbushman.araw.RedditClient;
import com.kirkbushman.araw.helpers.AuthUserlessHelper;

import java.util.List;

import rocks.tbog.livewallpaperit.data.DBHelper;

public class LoginWorker extends Worker {
    private static final String TAG = LoginWorker.class.getSimpleName();

    public LoginWorker(@NonNull Context appContext, @NonNull WorkerParameters workerParams) {
        super(appContext, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context ctx = getApplicationContext();
        String clientId = getInputData().getString("clientId");
        if (TextUtils.isEmpty(clientId))
            return Result.failure();

        var helper = new AuthUserlessHelper(ctx, clientId, "DO_NOT_TRACK_THIS_DEVICE", false, true);
        if (!helper.shouldLogin()) {
            // use saved one
            Log.i(TAG, String.valueOf(helper));
        } else {
            // you must authenticate
            Log.i(TAG, "you must authenticate");
        }

        // obtain a client
        RedditClient client = helper.getRedditClient();
        if (client == null)
            return Result.failure();

        List<String> list = DBHelper.getIgnoreTokenList(ctx);

        return Result.success(new Data.Builder()
                .putString("clientId", clientId)
                .putStringArray("ignoreTokenList", list.toArray(new String[0]))
                .build());
    }

}
