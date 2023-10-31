package rocks.tbog.livewallpaperit;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.lifecycle.Observer;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.divider.MaterialDividerItemDecoration;
import java.util.ArrayList;
import java.util.Collections;
import rocks.tbog.livewallpaperit.WorkAsync.AsyncUtils;
import rocks.tbog.livewallpaperit.data.DBHelper;
import rocks.tbog.livewallpaperit.dialog.DialogHelper;
import rocks.tbog.livewallpaperit.preference.SettingsActivity;
import rocks.tbog.livewallpaperit.utils.ViewUtils;

public class SourcesActivity extends AppCompatActivity {

    SourceAdapter mAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sources);
        Toolbar topToolbar = findViewById(R.id.top_toolbar);
        topToolbar.setTitle(R.string.sources_name);
        setSupportActionBar(topToolbar);

        mAdapter = new SourceAdapter(
                source -> DBHelper.updateSource(getApplicationContext(), source),
                source -> DBHelper.removeSource(getApplicationContext(), source));
        mAdapter.setHasStableIds(true);

        loadSourcesFromPreferences();

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
                });
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
                });
    }

    public static class SourceAdapter extends RecycleAdapterBase<Source, SourceHolder> {
        private final Observer<Source> mSourceChangedObserver;
        private final Observer<Source> mSourceRemovedObserver;

        public SourceAdapter(Observer<Source> changeObserver, Observer<Source> removeObserver) {
            super(new ArrayList<>());
            mSourceChangedObserver = changeObserver;
            mSourceRemovedObserver = removeObserver;
        }

        @NonNull
        @Override
        public SourceHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            final Context context = parent.getContext();

            LayoutInflater inflater = LayoutInflater.from(context);
            View itemView = inflater.inflate(R.layout.source_item, parent, false);

            return new SourceHolder(itemView);
        }

        @Override
        public void onBindViewHolder(
                @NonNull SourceHolder holder, @SuppressLint("RecyclerView") @NonNull Source source) {
            holder.bind(source, mSourceChangedObserver);

            holder.subredditName.setText(source.subreddit);

            // minUpvotePercentage
            holder.minUpvotePercentage.removeTextChangedListener(holder.mUpvotePercentageWatcher);
            holder.minUpvotePercentage.setText(intToString(source.minUpvotePercentage));
            holder.minUpvotePercentage.addTextChangedListener(holder.mUpvotePercentageWatcher);

            // minScore
            holder.minScore.removeTextChangedListener(holder.mScoreWatcher);
            holder.minScore.setText(intToString(source.minScore));
            holder.minScore.addTextChangedListener(holder.mScoreWatcher);

            // minComments
            holder.minComments.removeTextChangedListener(holder.mCommentsWatcher);
            holder.minComments.setText(intToString(source.minComments));
            holder.minComments.addTextChangedListener(holder.mCommentsWatcher);

            // remove button
            holder.buttonRemove.setOnClickListener(v -> {
                mSourceRemovedObserver.onChanged(source);
                removeItem(source);
            });
        }

        private static String intToString(int value) {
            if (value <= 0) return "";
            return Integer.toString(value);
        }
    }

    public abstract static class TextChangedWatcher implements TextWatcher {
        @Nullable
        public Source mSource = null;

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            if (mSource == null) return;
            try {
                int newValue = Integer.parseInt(s.toString());
                onIntChanged(mSource, newValue);
            } catch (Exception ignored) {
                // ignore invalid text
            }
        }

        abstract void onIntChanged(@NonNull Source source, int newValue);
    }

    public static class SourceHolder extends RecycleAdapterBase.Holder {
        private final TextView subredditName;
        private final Button buttonRemove;
        private final TextView minUpvotePercentage;
        private final TextView minScore;
        private final TextView minComments;

        public Observer<Source> mSourceChangedObserver;

        private final TextChangedWatcher mUpvotePercentageWatcher = new TextChangedWatcher() {
            @Override
            public void onIntChanged(@NonNull Source source, int newValue) {
                if (newValue != source.minUpvotePercentage) {
                    source.minUpvotePercentage = newValue;
                    mSourceChangedObserver.onChanged(source);
                }
            }
        };
        private final TextChangedWatcher mScoreWatcher = new TextChangedWatcher() {
            @Override
            public void onIntChanged(@NonNull Source source, int newValue) {
                if (newValue != source.minScore) {
                    source.minScore = newValue;
                    mSourceChangedObserver.onChanged(source);
                }
            }
        };
        private final TextChangedWatcher mCommentsWatcher = new TextChangedWatcher() {
            @Override
            public void onIntChanged(@NonNull Source source, int newValue) {
                if (newValue != source.minComments) {
                    source.minComments = newValue;
                    mSourceChangedObserver.onChanged(source);
                }
            }
        };

        public SourceHolder(@NonNull View itemView) {
            super(itemView);

            subredditName = itemView.findViewById(R.id.subreddit_name);
            buttonRemove = itemView.findViewById(R.id.button_remove);
            minUpvotePercentage = itemView.findViewById(R.id.min_upvote_percent);
            minScore = itemView.findViewById(R.id.min_score);
            minComments = itemView.findViewById(R.id.min_comments);

            final MotionLayout parent = (MotionLayout) itemView;
            minUpvotePercentage.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    parent.transitionToState(R.id.expanded_min_upvote_percent);
                }
            });
            minScore.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    parent.transitionToState(R.id.expanded_min_score);
                }
            });
            minComments.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    parent.transitionToState(R.id.expanded_min_comments);
                }
            });
        }

        public void bind(Source source, Observer<Source> sourceChangedObserver) {
            mSourceChangedObserver = sourceChangedObserver;
            mUpvotePercentageWatcher.mSource = source;
            mScoreWatcher.mSource = source;
            mCommentsWatcher.mSource = source;
        }
    }
}
