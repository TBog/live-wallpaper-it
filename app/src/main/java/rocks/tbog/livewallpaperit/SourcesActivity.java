package rocks.tbog.livewallpaperit;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.collection.ArraySet;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

import rocks.tbog.livewallpaperit.WorkAsync.AsyncUtils;
import rocks.tbog.livewallpaperit.dialog.DialogHelper;
import rocks.tbog.livewallpaperit.preference.SettingsActivity;
import rocks.tbog.livewallpaperit.utils.ViewUtils;

public class SourcesActivity extends AppCompatActivity {
    SourceAdapter mAdapter = new SourceAdapter();
    SharedPreferences mPreference;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sources);
        Toolbar topToolbar = (Toolbar) findViewById(R.id.top_toolbar);
        topToolbar.setTitle(R.string.sources_name);
        setSupportActionBar(topToolbar);

        mAdapter.setHasStableIds(true);
        mPreference = PreferenceManager.getDefaultSharedPreferences(this);

        RecyclerView recyclerView = findViewById(R.id.source_list);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(mAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));

        loadSources();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void loadSources() {
        final ArrayList<String> list = new ArrayList<>();
        AsyncUtils.runAsync(getLifecycle(), task -> {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
            Set<String> sources = pref.getStringSet(ArtProvider.PREF_SOURCES_SET, Collections.emptySet());
            list.addAll(sources);
            Collections.sort(list, String::compareToIgnoreCase);
        }, task -> {
            if (task.isCancelled())
                return;
            for (String subreddit : list) {
                mAdapter.addItem(new Source(subreddit));
            }
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
        mAdapter.addItem(new Source(name));
        ArraySet<String> subredditSet = new ArraySet<>();
        for (Source source : mAdapter.getItems())
            subredditSet.add(source.subreddit);

        mPreference.edit()
                .putStringSet(ArtProvider.PREF_SOURCES_SET, subredditSet)
                .apply();
    }

    public static class Source {
        public String subreddit;

        public Source(String subreddit) {
            this.subreddit = subreddit;
        }
    }

    public static class SourceAdapter extends RecycleAdapterBase<Source, RecycleAdapterBase.Holder> {
        public SourceAdapter() {
            super(new ArrayList<>());
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            final Context context = parent.getContext();

            LayoutInflater inflater = LayoutInflater.from(context);
            View itemView = inflater.inflate(R.layout.source_item, parent, false);

            return new RecycleAdapterBase.Holder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, Source source) {
            TextView textView = holder.itemView.findViewById(android.R.id.text1);
            textView.setText(source.subreddit);
            ImageButton button1 = holder.itemView.findViewById(android.R.id.button1);
            button1.setOnClickListener(v -> removeItem(source));
        }
    }
}
