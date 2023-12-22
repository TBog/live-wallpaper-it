package rocks.tbog.livewallpaperit.preview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
import java.util.HashMap;
import java.util.stream.Collectors;
import rocks.tbog.livewallpaperit.R;
import rocks.tbog.livewallpaperit.asynchronous.AsyncUtils;
import rocks.tbog.livewallpaperit.data.DBHelper;
import rocks.tbog.livewallpaperit.data.Image;
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
            Bundle bundle = savedInstanceState.getBundle(STATE_ADAPTER_ITEMS);
            if (bundle != null) {
                bundle.setClassLoader(Image.class.getClassLoader());
                for (var link : bundle.keySet()) {
                    var parcelableArrayList = bundle.getParcelableArrayList(link);
                    if (parcelableArrayList == null || parcelableArrayList.isEmpty()) continue;
                    var images = parcelableArrayList.stream()
                            .filter(p -> p instanceof Image)
                            .map(p -> (Image) p)
                            .collect(Collectors.toList());
                    var item = new FavoriteAdapter.Item(images, Uri.parse(link));
                    mAdapter.addItem(item);
                }
            }
        }

        final int columnCount = 3;
        RecyclerView recyclerView = findViewById(R.id.favorite_list);
        ViewUtils.doOnLayout(recyclerView, (view) -> mAdapter.setWidth(view.getWidth() / columnCount));
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(mAdapter);
        var layout = new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);
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
        Bundle bundle = new Bundle();
        for (var item : mAdapter.getItems()) {
            bundle.putParcelableArrayList(item.link.toString(), new ArrayList<>(item.images));
        }
        outState.putBundle(STATE_ADAPTER_ITEMS, bundle);
    }

    private void loadFavorites() {
        onStartLoading();
        final ArrayList<FavoriteAdapter.Item> list = new ArrayList<>();
        AsyncUtils.runAsync(
                getLifecycle(),
                task -> {
                    var favoriteImages = DBHelper.getFavoriteImages(getApplicationContext());
                    Log.d(TAG, "found " + favoriteImages.size() + " favorite images(s)");
                    HashMap<String, ArrayList<Image>> imageById = new HashMap<>(favoriteImages.size());
                    for (var image : favoriteImages) {
                        var mediaList = imageById.get(image.mediaId);
                        if (mediaList == null) {
                            imageById.put(image.mediaId, mediaList = new ArrayList<>());
                        }
                        mediaList.add(image);
                    }
                    Log.d(TAG, "found " + imageById.size() + " unique ID(s)");
                    for (var mediaList : imageById.values()) {
                        if (mediaList == null || mediaList.isEmpty()) continue;
                        var sourceImage = mediaList.stream()
                                .filter(i -> i.isSource && !i.isObfuscated)
                                .findAny()
                                .orElse(mediaList.get(0));
                        var item = new FavoriteAdapter.Item(mediaList, Uri.parse(sourceImage.url));
                        list.add(item);
                    }
                },
                task -> {
                    if (task.isCancelled()) return;
                    mAdapter.setItems(list);
                    Log.d(TAG, "adapter has " + list.size() + " item(s)");
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
