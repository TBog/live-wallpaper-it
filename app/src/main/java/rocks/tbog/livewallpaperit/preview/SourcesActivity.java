package rocks.tbog.livewallpaperit.preview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import com.google.android.material.divider.MaterialDividerItemDecoration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import rocks.tbog.livewallpaperit.ArtProvider;
import rocks.tbog.livewallpaperit.BuildConfig;
import rocks.tbog.livewallpaperit.DeleteArtworkReceiver;
import rocks.tbog.livewallpaperit.R;
import rocks.tbog.livewallpaperit.Source;
import rocks.tbog.livewallpaperit.WorkAsync.AsyncUtils;
import rocks.tbog.livewallpaperit.data.DBHelper;
import rocks.tbog.livewallpaperit.dialog.DialogHelper;
import rocks.tbog.livewallpaperit.preference.SettingsActivity;
import rocks.tbog.livewallpaperit.utils.ViewUtils;
import rocks.tbog.livewallpaperit.work.WorkerUtils;

public class SourcesActivity extends AppCompatActivity {

    private static final String TAG = SourcesActivity.class.getSimpleName();
    SourceAdapter mAdapter;
    TextView mText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sources);
        Toolbar topToolbar = findViewById(R.id.top_toolbar);
        topToolbar.setTitle(R.string.sources_name);
        setSupportActionBar(topToolbar);

        mText = findViewById(R.id.source_list_text);

        mAdapter = new SourceAdapter(
                source -> DBHelper.updateSource(getApplicationContext(), source),
                source -> DBHelper.removeSource(getApplicationContext(), source));

        loadSourcesFromPreferences();

        RecyclerView recyclerView = findViewById(R.id.source_list);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(mAdapter);
        var layout = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        recyclerView.setLayoutManager(layout);
        var decoration = new MaterialDividerItemDecoration(recyclerView.getContext(), layout.getOrientation());
        decoration.setLastItemDecorated(false);
        recyclerView.addItemDecoration(decoration);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSources();
    }

    /**
     * First version stored subreddits in the preferences, load from there then delete
     */
    protected void loadSourcesFromPreferences() {
        final var sourcesSet = PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                .getStringSet(ArtProvider.PREF_SOURCES_SET, Collections.emptySet());
        if (sourcesSet.isEmpty()) return;
        final ArrayList<Source> list = new ArrayList<>();
        AsyncUtils.runAsync(
                getLifecycle(),
                task -> {
                    list.addAll(DBHelper.loadSources(getApplicationContext()));
                    for (String subreddit : sourcesSet) {
                        Source source = new Source(subreddit);
                        if (DBHelper.insertSource(getApplicationContext(), source)) {
                            list.add(source);
                        }
                    }
                    Collections.sort(list, (o1, o2) -> o1.subreddit.compareToIgnoreCase(o2.subreddit));
                },
                task -> {
                    if (task.isCancelled()) return;
                    mAdapter.setItems(list);
                    PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                            .edit()
                            .remove(ArtProvider.PREF_SOURCES_SET)
                            .apply();
                });
    }

    private void loadSources() {
        onStartLoadSources();
        final ArrayList<Source> list = new ArrayList<>();
        AsyncUtils.runAsync(
                getLifecycle(),
                task -> {
                    list.addAll(DBHelper.loadSources(getApplicationContext()));
                    Collections.sort(list, (o1, o2) -> o1.subreddit.compareToIgnoreCase(o2.subreddit));
                },
                task -> {
                    if (task.isCancelled()) return;
                    mAdapter.setItems(list);
                    updateSourcesText();
                });
    }

    private void updateSourcesText() {
        if (mAdapter.getItemCount() == 0) {
            onEmptySources();
        } else {
            onEndLoadSources();
        }
    }

    private void onStartLoadSources() {
        mText.setText(R.string.loading_sources);
        mText.setVisibility(View.VISIBLE);
        mText.animate().alpha(1f).start();
    }

    private void onStartRefreshSources() {
        mText.setText(R.string.refresh_sources);
        mText.setVisibility(View.VISIBLE);
        mText.animate().alpha(1f).start();
    }

    private void onEndLoadSources() {
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

    private void onEmptySources() {
        mText.setText(R.string.empty_sources);
        mText.setVisibility(View.VISIBLE);
        mText.animate().alpha(1f).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.sources_toolbar, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            ViewUtils.launchIntent(this, null, intent);
            return true;
        } else if (itemId == R.id.action_clear_cache) {
            Intent intent = new Intent(this, DeleteArtworkReceiver.class)
                    .putExtra(DeleteArtworkReceiver.ACTION, DeleteArtworkReceiver.ACTION_CLEAR_CACHE);
            sendBroadcast(intent);
            Toast.makeText(this, item.getTitle(), Toast.LENGTH_SHORT).show();
            return true;
        } else if (itemId == R.id.action_refresh) {
            refreshSources();
            return true;
        } else if (itemId == R.id.action_add) {
            return openAddSourceDialog();
        }
        // The user's action isn't recognized. Invoke the superclass to handle it.
        return super.onOptionsItemSelected(item);
    }

    public boolean openAddSourceDialog() {
        DialogHelper.makeRenameDialog(this, "", (dialog, name) -> addSource(name))
                .setTitle(R.string.title_add_subreddit)
                .setHint(R.string.hint_add_subreddit)
                .show(getSupportFragmentManager());
        return true;
    }

    public void addSource(String name) {
        String subreddit = name.trim();
        if (TextUtils.isEmpty(subreddit)) return;
        final Source source = new Source(subreddit);
        source.minUpvotePercentage = 50;
        source.minScore = 1;
        AsyncUtils.runAsync(
                getLifecycle(),
                task -> {
                    if (!DBHelper.insertSource(getApplicationContext(), source)) {
                        task.cancel();
                    }
                },
                task -> {
                    if (task.isCancelled()) return;
                    mAdapter.addItem(source);
                    updateSourcesText();
                });
    }

    private void refreshSources() {
        onStartRefreshSources();
        final var sources = mAdapter.getItems();

        final var setupWork = ArtProvider.buildSetupWorkRequest(this);
        final var workMgr = WorkManager.getInstance(this);
        final var workQueue =
                workMgr.beginUniqueWork(WorkerUtils.UNIQUE_WORK_REFRESH_ENABLED, ExistingWorkPolicy.KEEP, setupWork);
        final ArrayList<OneTimeWorkRequest> sourceWorkList = new ArrayList<>();
        for (Source source : sources) {
            if (!source.isEnabled) continue;
            sourceWorkList.add(ArtProvider.buildSourceWorkRequest(source));
        }
        workQueue.then(sourceWorkList).enqueue();

        workMgr.getWorkInfosForUniqueWorkLiveData(WorkerUtils.UNIQUE_WORK_REFRESH_ENABLED)
                .observe(SourcesActivity.this, workInfos -> {
                    if (workInfos == null || workInfos.isEmpty()) return;

                    StringBuilder logWork = new StringBuilder("work.size=").append(workInfos.size());
                    for (var workInfo : workInfos) {
                        logWork.append("\n\tid=")
                                .append(workInfo.getId())
                                .append(" state=")
                                .append(workInfo.getState());
                        if (BuildConfig.DEBUG) {
                            if (workInfo.getState().isFinished()) {
                                Map<String, Object> map =
                                        workInfo.getOutputData().getKeyValueMap();
                                for (Map.Entry<String, Object> entry : map.entrySet()) {
                                    String key = entry.getKey();
                                    Object value = entry.getValue();
                                    logWork.append("\n\t\t[")
                                            .append(key)
                                            .append("]=")
                                            .append(value);
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
                        } else {
                            Log.i(TAG, "refresh failed");
                            Toast.makeText(this, "Failed to refresh enabled sources", Toast.LENGTH_SHORT)
                                    .show();
                        }
                        onEndLoadSources();
                    }
                });
    }
}
