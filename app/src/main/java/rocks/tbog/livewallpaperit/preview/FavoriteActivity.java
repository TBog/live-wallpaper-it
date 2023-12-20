package rocks.tbog.livewallpaperit.preview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import rocks.tbog.livewallpaperit.R;
import rocks.tbog.livewallpaperit.WorkAsync.AsyncUtils;
import rocks.tbog.livewallpaperit.data.DBHelper;
import rocks.tbog.livewallpaperit.data.Image;
import rocks.tbog.livewallpaperit.data.SubTopic;
import rocks.tbog.livewallpaperit.utils.ViewUtils;

public class FavoriteActivity extends AppCompatActivity {
    private static final String TAG = FavoriteActivity.class.getSimpleName();
    public static final String STATE_ADAPTER_ITEMS = "adapterItems";
    TextView mText;
    FavoriteAdapter mAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_favorite);
        Toolbar topToolbar = findViewById(R.id.top_toolbar);
        topToolbar.setTitle(R.string.favorite_title);
        setSupportActionBar(topToolbar);

        mText = findViewById(R.id.favorite_list_text);
        mAdapter = new FavoriteAdapter();
        mAdapter.setOnClickListener(
                (a, i, v) -> ViewUtils.launchIntent(v, new Intent(Intent.ACTION_VIEW).setData(i.link)));

        if (savedInstanceState != null) {
            savedInstanceState.setClassLoader(SubTopic.class.getClassLoader());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                var images = savedInstanceState.getParcelableArrayList(STATE_ADAPTER_ITEMS, Image.class);
                if (images != null) {
                    var items = images.stream()
                            .map(p -> new ThumbnailAdapter.Item((Image) p, Uri.parse(((Image) p).url)))
                            .collect(Collectors.toList());
                    mAdapter.setItems(items);
                }
            } else {
                ArrayList<Parcelable> parcelables = savedInstanceState.getParcelableArrayList(STATE_ADAPTER_ITEMS);
                if (parcelables != null) {
                    var items = parcelables.stream()
                            .filter(p -> p instanceof Image)
                            .map(p -> new ThumbnailAdapter.Item((Image) p, Uri.parse(((Image) p).url)))
                            .collect(Collectors.toList());
                    mAdapter.setItems(items);
                }
            }
        }

        RecyclerView recyclerView = findViewById(R.id.favorite_list);
        ViewUtils.doOnLayout(recyclerView, (view) -> mAdapter.setWidth(view.getWidth() / 2));
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(mAdapter);
        var layout = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layout);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mAdapter.getItemCount() == 0) {
            loadFavorites();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        List<Image> adapterItems =
                mAdapter.getItems().stream().map(i -> i.image).collect(Collectors.toList());
        outState.putParcelableArrayList(STATE_ADAPTER_ITEMS, new ArrayList<>(adapterItems));
    }

    private void loadFavorites() {
        onStartLoading();
        final ArrayList<ThumbnailAdapter.Item> list = new ArrayList<>();
        AsyncUtils.runAsync(
                getLifecycle(),
                task -> {
                    var favoriteImages = DBHelper.getFavoriteImages(getApplicationContext());
                    list.addAll(favoriteImages.stream()
                            .filter(i -> i.width == 108)
                            .map(i -> new ThumbnailAdapter.Item(i, Uri.parse(i.url)))
                            .collect(Collectors.toList()));
                    for (var item : list) {
                        Log.d(TAG, item.image.width + "x" + item.image.height + " " + item.link);
                    }
                },
                task -> {
                    if (task.isCancelled()) return;
                    mAdapter.setItems(list);
                    updateLoadingText();
                });
    }

    private void updateLoadingText() {
        if (mAdapter.getItemCount() == 0) {
            onLoadingEmpty();
        } else {
            onEndLoading();
        }
    }

    private void onStartLoading() {
        mText.setText(R.string.loading_favorite);
        mText.setVisibility(View.VISIBLE);
        mText.animate().alpha(1f).start();
    }

    private void onEndLoading() {
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

    private void onLoadingEmpty() {
        mText.setText(R.string.empty_favorite);
        mText.setVisibility(View.VISIBLE);
        mText.animate().alpha(1f).start();
    }
}
