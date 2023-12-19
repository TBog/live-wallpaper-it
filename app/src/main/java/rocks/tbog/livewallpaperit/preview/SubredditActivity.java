package rocks.tbog.livewallpaperit.preview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.ExistingWorkPolicy;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import com.google.android.material.divider.MaterialDividerItemDecoration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import rocks.tbog.livewallpaperit.ArtProvider;
import rocks.tbog.livewallpaperit.BuildConfig;
import rocks.tbog.livewallpaperit.R;
import rocks.tbog.livewallpaperit.Source;
import rocks.tbog.livewallpaperit.WorkAsync.AsyncUtils;
import rocks.tbog.livewallpaperit.data.DBHelper;
import rocks.tbog.livewallpaperit.data.MediaInfo;
import rocks.tbog.livewallpaperit.data.SubTopic;
import rocks.tbog.livewallpaperit.preference.SettingsActivity;
import rocks.tbog.livewallpaperit.utils.ViewUtils;

public class SubredditActivity extends AppCompatActivity {

    public static final String EXTRA_SUBREDDIT = "subreddit.name";
    public static final String EXTRA_SOURCE = "parcelable.source";
    public static final String STATE_ADAPTER_ITEMS = "adapterItems";
    private static final String TAG = SubredditActivity.class.getSimpleName();

    SubredditAdapter mAdapter;
    TextView mText;
    Source mSource = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAdapter = new SubredditAdapter();

        if (getIntent() != null) {
            var extras = getIntent().getExtras();
            if (extras != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    mSource = extras.getParcelable(EXTRA_SOURCE, Source.class);
                } else {
                    Parcelable parcelable = extras.getParcelable(EXTRA_SOURCE);
                    if (parcelable instanceof Source) {
                        mSource = (Source) parcelable;
                    }
                }

                String subreddit = extras.getString(EXTRA_SUBREDDIT);
                if (mSource == null && !TextUtils.isEmpty(subreddit)) {
                    mSource = new Source(subreddit);
                }
            }
        }

        if (savedInstanceState != null) {
            savedInstanceState.setClassLoader(SubTopic.class.getClassLoader());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                var topics = savedInstanceState.getParcelableArrayList(STATE_ADAPTER_ITEMS, SubTopic.class);
                if (topics != null) {
                    mAdapter.setItems(topics);
                }
            } else {
                ArrayList<Parcelable> parcelables = savedInstanceState.getParcelableArrayList(STATE_ADAPTER_ITEMS);
                if (parcelables != null) {
                    var topics = parcelables.stream()
                            .filter(p -> p instanceof SubTopic)
                            .map(p -> (SubTopic) p)
                            .collect(Collectors.toList());
                    mAdapter.setItems(topics);
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

        mText = findViewById(R.id.source_list_text);

        RecyclerView recyclerView = findViewById(R.id.source_list);
        recyclerView.setHasFixedSize(true);
        recyclerView.setNestedScrollingEnabled(true);
        recyclerView.setAdapter(mAdapter);
        var layout = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        recyclerView.setLayoutManager(layout);
        var decoration = new MaterialDividerItemDecoration(recyclerView.getContext(), layout.getOrientation());
        decoration.setLastItemDecorated(false);
        recyclerView.addItemDecoration(decoration);

        mAdapter.setOnLongClickListener((a, subTopic, view) -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(mSource.subreddit, subTopic.getPermalinkString());
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

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean allowNSFW = pref.getBoolean("allow-nsfw", false);
        mAdapter.setAllowNSFW(allowNSFW);
        int previewWidth = pref.getInt("image-thumbnail-width", 108);
        mAdapter.setPreviewWidth(previewWidth);
        mAdapter.setSource(mSource);

        if (mAdapter.getItemCount() == 0) {
            loadSourceData();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        List<SubTopic> adapterItems = mAdapter.getItems();
        outState.putParcelableArrayList(STATE_ADAPTER_ITEMS, new ArrayList<>(adapterItems));
    }

    private void loadSourceData() {
        if (mSource == null) return;
        onStartLoadData();
        final ArrayList<SubTopic> topicList = new ArrayList<>();
        final ArrayList<MediaInfo> ignoreList = new ArrayList<>();
        final ArrayList<MediaInfo> favoriteList = new ArrayList<>();
        AsyncUtils.runAsync(
                getLifecycle(),
                t -> {
                    Context ctx = getApplicationContext();
                    var list = DBHelper.getSubTopics(ctx, mSource.subreddit);
                    DBHelper.loadSubTopicImages(ctx, list);
                    topicList.addAll(list);

                    ignoreList.addAll(DBHelper.getIgnoreMediaList(ctx, mSource.subreddit));
                    favoriteList.addAll(DBHelper.getFavoriteMediaList(ctx, mSource.subreddit));
                },
                t -> {
                    mAdapter.setItems(topicList);
                    mAdapter.setIgnoreList(ignoreList);
                    mAdapter.setFavoriteList(favoriteList);
                    onEndLoadData();
                    if (mAdapter.getItemCount() == 0) {
                        refreshSource();
                    }
                });
    }

    private void onStartLoadData() {
        mText.animate().cancel();
        mText.setText(R.string.loading_subreddit);
        mText.setVisibility(View.VISIBLE);
        mText.animate().alpha(1f).start();
    }

    private void onStartRefresh() {
        mText.animate().cancel();
        mText.setText(R.string.refreshing_subreddit);
        mText.setVisibility(View.VISIBLE);
        mText.animate().alpha(1f).start();
    }

    private void onEndLoadData() {
        mText.animate()
                .alpha(0f)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mText.setVisibility(View.GONE);
                    }
                })
                .start();
    }

    private void refreshSource() {
        onStartRefresh();
        var workMgr = WorkManager.getInstance(this);
        workMgr.beginUniqueWork(mSource.subreddit, ExistingWorkPolicy.KEEP, ArtProvider.buildSetupWorkRequest(this))
                .then(ArtProvider.buildSourceWorkRequest(mSource))
                .then(ArtProvider.buildCleanupWorkRequest())
                .enqueue();
        workMgr.getWorkInfosForUniqueWorkLiveData(mSource.subreddit).observe(SubredditActivity.this, workInfos -> {
            if (workInfos == null || workInfos.isEmpty()) return;

            StringBuilder logWork = new StringBuilder("work.size=").append(workInfos.size());
            for (var workInfo : workInfos) {
                logWork.append("\n\tid=")
                        .append(workInfo.getId())
                        .append(" state=")
                        .append(workInfo.getState());
                if (BuildConfig.DEBUG) {
                    if (workInfo.getState().isFinished()) {
                        Map<String, Object> map = workInfo.getOutputData().getKeyValueMap();
                        for (Map.Entry<String, Object> entry : map.entrySet()) {
                            String key = entry.getKey();
                            Object value = entry.getValue();
                            logWork.append("\n\t\t[").append(key).append("]=").append(value);
                        }
                    }
                }
            }
            Log.v(TAG, logWork.toString());

            boolean allFinished = true;
            boolean allSucceeded = true;
            for (var workInfo : workInfos) {
                if (!workInfo.getState().isFinished()) {
                    allFinished = false;
                    allSucceeded = false;
                    break;
                }
                if (!WorkInfo.State.SUCCEEDED.equals(workInfo.getState())) {
                    allSucceeded = false;
                }
            }
            if (allFinished) {
                if (allSucceeded) {
                    Log.i(TAG, "refresh succeeded");
                    loadSourceData();
                } else {
                    Log.i(TAG, "refresh failed");
                    onEndLoadData();
                    Toast.makeText(this, "Failed to get " + mSource.subreddit, Toast.LENGTH_SHORT)
                            .show();
                }
            }
        });
    }

    private void reloadSource() {
        mAdapter.clear();
        AsyncUtils.runAsync(
                getLifecycle(),
                t -> DBHelper.removeSourceSubTopics(getApplicationContext(), mSource),
                t -> refreshSource());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.subreddit_toolbar, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            ViewUtils.launchIntent(this, null, intent);
            return true;
        } else if (itemId == R.id.action_refresh) {
            refreshSource();
        } else if (itemId == R.id.action_reload) {
            reloadSource();
        }
        // The user's action isn't recognized. Invoke the superclass to handle it.
        return super.onOptionsItemSelected(item);
    }
}
