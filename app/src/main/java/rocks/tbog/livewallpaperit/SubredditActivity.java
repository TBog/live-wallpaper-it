package rocks.tbog.livewallpaperit;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.divider.MaterialDividerItemDecoration;
import java.io.Serializable;
import java.util.ArrayList;
import rocks.tbog.livewallpaperit.WorkAsync.AsyncUtils;
import rocks.tbog.livewallpaperit.data.DBHelper;
import rocks.tbog.livewallpaperit.data.SubTopic;

public class SubredditActivity extends AppCompatActivity {

    public static final String EXTRA_SUBREDDIT = "subreddit.name";
    public static final String EXTRA_SOURCE = "serializable.source";
    private static final String TAG = SubredditActivity.class.getSimpleName();

    SubredditAdapter mAdapter;
    Source mSource = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent() != null) {
            var extras = getIntent().getExtras();
            if (extras != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    mSource = extras.getSerializable(EXTRA_SOURCE, Source.class);
                } else {
                    Serializable serializable = extras.getSerializable(EXTRA_SOURCE);
                    if (serializable instanceof Source) {
                        mSource = (Source) serializable;
                    }
                }

                String subreddit = extras.getString(EXTRA_SUBREDDIT);
                if (mSource == null && !TextUtils.isEmpty(subreddit)) {
                    mSource = new Source(subreddit);
                }
            }
        }
        if (mSource == null) {
            finish();
            return;
        }

        setContentView(R.layout.activity_sources);
        Toolbar topToolbar = findViewById(R.id.top_toolbar);
        topToolbar.setTitle(getString(R.string.subreddit_name, mSource.subreddit));
        setSupportActionBar(topToolbar);

        mAdapter = new SubredditAdapter();

        RecyclerView recyclerView = findViewById(R.id.source_list);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(mAdapter);
        var layout = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        recyclerView.setLayoutManager(layout);
        var decoration = new MaterialDividerItemDecoration(recyclerView.getContext(), layout.getOrientation());
        recyclerView.addItemDecoration(decoration);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mAdapter.getItemCount() == 0) {
            loadSourceData();
        }
    }

    //    private void loadSourceData() {
    //        if (mSource == null) return;
    //        var workManager = WorkManager.getInstance(this);
    //        var request = new OneTimeWorkRequest.Builder(PreviewWorker.class)
    //                .setInputData(new Data.Builder()
    //                        .putString(WorkerUtils.DATA_CLIENT_ID, DataUtils.loadRedditAuth(this))
    //                        .putString(PreviewWorker.DATA_SUBREDDIT, mSource.subreddit)
    //                        .build())
    //                .build();
    //        workManager.enqueueUniqueWork(mSource.subreddit, ExistingWorkPolicy.KEEP, request);
    //        workManager.getWorkInfosForUniqueWorkLiveData(mSource.subreddit).observe(this, workInfoList -> {
    //            if (workInfoList == null) return;
    //            Log.d(TAG, "UniqueWork " + mSource.subreddit + " size " + workInfoList.size());
    //            for (var workInfo : workInfoList) {
    //                Log.d(TAG, "work " + workInfo.getId() + " state " + workInfo.getState());
    //                if (workInfo.getState().isFinished()) {
    //                    var data = PreviewWorker.getAndRemoveData(mSource.subreddit);
    //                    if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
    //                        mAdapter.setItems(data);
    //                    }
    //                }
    //            }
    //        });
    //    }

    private void loadSourceData() {
        if (mSource == null) return;
        final ArrayList<SubTopic> topicList = new ArrayList<>();
        AsyncUtils.runAsync(
                getLifecycle(),
                t -> {
                    var list = DBHelper.getSubTopics(getApplicationContext(), mSource.subreddit);
                    topicList.addAll(list);
                },
                t -> {
                    mAdapter.setItems(topicList);
                });
    }

    public static class SubredditAdapter extends RecycleAdapterBase<SubTopic, SubmissionHolder> {

        public SubredditAdapter() {
            super(new ArrayList<>());
            setHasStableIds(true);
        }

        @Override
        public void onBindViewHolder(@NonNull SubmissionHolder holder, @NonNull SubTopic entry) {
            holder.mTitleView.setText(entry.title);
            holder.mNsfwView.setVisibility(entry.over18 ? View.VISIBLE : View.GONE);
            holder.mScoreView.setText(String.valueOf(entry.score));
            holder.mUpvoteView.setText(String.valueOf(entry.upvoteRatio));
            holder.mNumCommentView.setText(String.valueOf(entry.numComments));
        }

        @NonNull
        @Override
        public SubmissionHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            final Context context = parent.getContext();

            LayoutInflater inflater = LayoutInflater.from(context);
            View itemView = inflater.inflate(R.layout.submission_item, parent, false);

            return new SubmissionHolder(itemView);
        }
    }

    public static class SubmissionHolder extends RecycleAdapterBase.Holder {

        TextView mTitleView;
        ImageView mNsfwView;
        TextView mScoreView;
        TextView mUpvoteView;
        TextView mNumCommentView;

        public SubmissionHolder(@NonNull View itemView) {
            super(itemView);

            mTitleView = itemView.findViewById(R.id.submission_title);
            mNsfwView = itemView.findViewById(R.id.nsfw);
            mScoreView = itemView.findViewById(R.id.score);
            mUpvoteView = itemView.findViewById(R.id.upvote_ratio);
            mNumCommentView = itemView.findViewById(R.id.num_comments);
        }
    }
}
