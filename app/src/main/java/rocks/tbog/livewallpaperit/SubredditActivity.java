package rocks.tbog.livewallpaperit;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Operation;
import androidx.work.WorkManager;
import com.google.android.material.divider.MaterialDividerItemDecoration;
import java.io.Serializable;
import java.util.ArrayList;
import rocks.tbog.livewallpaperit.WorkAsync.AsyncUtils;
import rocks.tbog.livewallpaperit.data.DBHelper;
import rocks.tbog.livewallpaperit.data.SubTopic;
import rocks.tbog.livewallpaperit.preference.SettingsActivity;
import rocks.tbog.livewallpaperit.utils.ViewUtils;

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
        decoration.setLastItemDecorated(false);
        recyclerView.addItemDecoration(decoration);

        mAdapter.setOnClickListener((subTopic, v) -> {
            Uri urlToOpen = Uri.parse("https://www.reddit.com" + subTopic.permalink);
            ViewUtils.launchIntent(v, new Intent(Intent.ACTION_VIEW).setData(urlToOpen));
        });
        mAdapter.setOnLongClickListener((subTopic, view) -> {
            Uri urlToOpen = Uri.parse("https://www.reddit.com" + subTopic.permalink);
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(mSource.subreddit, urlToOpen.toString());
            clipboard.setPrimaryClip(clip);
            // Only show a toast for Android 12 and lower.
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2)
                Toast.makeText(this, "Copied link", Toast.LENGTH_SHORT).show();
            return true;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mAdapter.getItemCount() == 0) {
            loadSourceData();
        }
    }

    private void loadSourceData() {
        if (mSource == null) return;
        final ArrayList<SubTopic> topicList = new ArrayList<>();
        AsyncUtils.runAsync(
                getLifecycle(),
                t -> {
                    Context ctx = getApplicationContext();
                    var list = DBHelper.getSubTopics(ctx, mSource.subreddit);
                    DBHelper.loadSubTopicImages(ctx, list);
                    topicList.addAll(list);
                },
                t -> {
                    mAdapter.setItems(topicList);
                });
    }

    private void refreshSource() {
        WorkManager.getInstance(this)
                .beginWith(ArtProvider.buildSetupWorkRequest(this))
                .then(ArtProvider.buildSourceWorkRequest(mSource))
                .enqueue()
                .getState()
                .observe(SubredditActivity.this, state -> {
                    if (state instanceof Operation.State.SUCCESS) {
                        loadSourceData();
                    } else if (state instanceof Operation.State.FAILURE) {
                        Toast.makeText(this, "Failed to get " + mSource.subreddit, Toast.LENGTH_SHORT)
                                .show();
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.subreddit_toolbar, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            ViewUtils.launchIntent(this, null, intent);
            return true;
        } else if (itemId == R.id.action_refresh) {
            refreshSource();
        }
        // The user's action isn't recognized. Invoke the superclass to handle it.
        return super.onOptionsItemSelected(item);
    }

    public static class SubredditAdapter extends RecycleAdapterBase<SubTopic, SubmissionHolder> {

        public SubredditAdapter() {
            super(new ArrayList<>());
            setHasStableIds(true);
        }

        @Override
        public void onBindViewHolder(@NonNull SubmissionHolder holder, @NonNull SubTopic topic) {
            holder.mTitleView.setText(topic.title);
            holder.mImageCarouselView.setAdapter(new ThumbnailAdapter(topic));
            holder.mNsfwView.setVisibility(topic.over18 ? View.VISIBLE : View.GONE);
            holder.mScoreView.setText(String.valueOf(topic.score));
            holder.mUpvoteView.setText(String.valueOf(topic.upvoteRatio));
            holder.mNumCommentView.setText(String.valueOf(topic.numComments));
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
        RecyclerView mImageCarouselView;
        ImageView mNsfwView;
        TextView mScoreView;
        TextView mUpvoteView;
        TextView mNumCommentView;

        public SubmissionHolder(@NonNull View itemView) {
            super(itemView);

            mTitleView = itemView.findViewById(R.id.submission_title);
            mImageCarouselView = itemView.findViewById(R.id.image_carousel);
            mNsfwView = itemView.findViewById(R.id.nsfw);
            mScoreView = itemView.findViewById(R.id.score);
            mUpvoteView = itemView.findViewById(R.id.upvote_ratio);
            mNumCommentView = itemView.findViewById(R.id.num_comments);

            var layout = new LinearLayoutManager(mImageCarouselView.getContext(), RecyclerView.HORIZONTAL, false);
            var decoration =
                    new MaterialDividerItemDecoration(mImageCarouselView.getContext(), layout.getOrientation());
            decoration.setLastItemDecorated(false);
            mImageCarouselView.addItemDecoration(decoration);
        }
    }
}
