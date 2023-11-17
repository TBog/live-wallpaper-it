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
import com.kirkbushman.araw.helpers.AuthUserlessHelper;
import com.kirkbushman.araw.models.Submission;
import com.kirkbushman.araw.models.enums.SubmissionsSorting;
import com.kirkbushman.araw.models.enums.TimePeriod;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class PreviewWorker extends Worker {

    private static final String TAG = PreviewWorker.class.getSimpleName();
    public static String DATA_SUBREDDIT = "subreddit";

    private static final HashMap<String, Collection<SubComment>> s_Data = new HashMap<>();

    public PreviewWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    public static Collection<SubComment> getAndRemoveData(@NonNull String key) {
        synchronized (s_Data) {
            var data = s_Data.remove(key);
            if (data == null) return Collections.emptyList();
            return data;
        }
    }

    protected static void putData(@NonNull String key, @NonNull Collection<SubComment> data) {
        synchronized (s_Data) {
            s_Data.put(key, data);
        }
    }

    @NonNull
    @Override
    public Result doWork() {
        String clientId = getInputData().getString(WorkerUtils.DATA_CLIENT_ID);
        if (TextUtils.isEmpty(clientId)) {
            Log.d(TAG, "clientId empty");
            return Result.failure(new Data.Builder()
                    .putString(WorkerUtils.FAIL_REASON, "empty clientId")
                    .build());
        }

        String subreddit = getInputData().getString(DATA_SUBREDDIT);
        if (TextUtils.isEmpty(subreddit)) {
            return Result.failure(new Data.Builder()
                    .putString(WorkerUtils.FAIL_REASON, "empty subreddit name")
                    .build());
        }

        Context ctx = getApplicationContext();
        var helper = new AuthUserlessHelper(ctx, clientId, "DO_NOT_TRACK_THIS_DEVICE", true, false);

        var client = helper.getRedditClient();
        if (client == null) {
            Log.d(TAG, "RedditClient null");
            return Result.failure(new Data.Builder()
                    .putString(WorkerUtils.FAIL_REASON, "getRedditClient=null")
                    .build());
        }

        if (helper.shouldLogin()) {
            // you must authenticate
            Log.d(TAG, "can't authenticate");
            return Result.failure(new Data.Builder()
                    .putString(WorkerUtils.FAIL_REASON, "can't authenticate")
                    .putString(WorkerUtils.DATA_CLIENT_ID, clientId)
                    .build());
        }

        SubmissionsFetcher submissionsFetcher = client.getSubredditsClient()
                .createSubmissionsFetcher(subreddit, SubmissionsSorting.NEW, TimePeriod.ALL_TIME, Fetcher.MAX_LIMIT);

        List<Submission> submissions = submissionsFetcher.fetchNext();
        if (submissions == null || submissions.isEmpty()) {
            Log.d(TAG, "PopularSubmissions failed");
            return Result.failure();
        }

        var commentList = new ArrayList<SubComment>();
        for (var submission : submissions) {
            var comment = new SubComment();
            commentList.add(comment);

            comment.title = submission.getTitle();
            comment.score = submission.getScore();
            comment.upvoteRatio = submission.getUpvoteRatio() != null ? submission.getUpvoteRatio() : 0f;
            comment.numComments = submission.getNumComments();
            comment.over18 = submission.getOver18();
        }

        putData(subreddit, commentList);
        return Result.success(
                new Data.Builder().putString(DATA_SUBREDDIT, subreddit).build());
    }

    public static class SubComment {
        public String title;
        public int score;
        public float upvoteRatio;
        public int numComments;
        public boolean over18;
    }
}
