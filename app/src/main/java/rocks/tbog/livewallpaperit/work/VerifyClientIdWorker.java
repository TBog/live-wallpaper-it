package rocks.tbog.livewallpaperit.work;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.kirkbushman.araw.fetcher.Fetcher;
import com.kirkbushman.araw.fetcher.SubmissionsFetcher;
import com.kirkbushman.araw.helpers.AuthAppHelper;
import com.kirkbushman.araw.helpers.AuthUserlessHelper;
import com.kirkbushman.araw.models.Submission;
import com.kirkbushman.araw.models.enums.SubmissionsSorting;
import com.kirkbushman.araw.models.enums.TimePeriod;

import java.util.List;

public class VerifyClientIdWorker extends Worker {
    private static final String TAG = VerifyClientIdWorker.class.getSimpleName();

    public VerifyClientIdWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context ctx = getApplicationContext();

        String clientId = getInputData().getString(WorkerUtils.DATA_CLIENT_ID);
        if (TextUtils.isEmpty(clientId)) {
            Log.d(TAG, "clientId empty");
            return Result.failure(new Data.Builder().putString(WorkerUtils.FAIL_REASON, "empty clientId").build());
        }

        var helper = new AuthUserlessHelper(ctx, clientId, "DO_NOT_TRACK_THIS_DEVICE", true, false);
        helper.forceRenew();

        var client = helper.getRedditClient();
        if (client == null) {
            Log.d(TAG, "RedditClient null");
            return Result.failure(new Data.Builder().putString(WorkerUtils.FAIL_REASON, "getRedditClient=null").build());
        }

        if (helper.shouldLogin()) {
            // you must authenticate
            Log.d(TAG, "can't authenticate");
            return Result.failure(new Data.Builder()
                    .putString(WorkerUtils.FAIL_REASON, "can't authenticate")
                    .putString(WorkerUtils.DATA_CLIENT_ID, clientId)
                    .build());
        }

        SubmissionsFetcher submissionsFetcher = client.getSubredditsClient().createPopularSubmissionsFetcher(SubmissionsSorting.HOT, TimePeriod.ALL_TIME, Fetcher.MIN_LIMIT);
        List<Submission> submissions = submissionsFetcher.fetchNext();
        if (submissions == null || submissions.isEmpty())
        {
            Log.d(TAG, "PopularSubmissions failed");
            return Result.failure();
        }

        //helper.forceRevoke();
        return Result.success(new Data.Builder().putString(WorkerUtils.DATA_CLIENT_ID, clientId).build());
    }
}
