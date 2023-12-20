package rocks.tbog.livewallpaperit.preview;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.collection.ArraySet;
import androidx.recyclerview.widget.RecyclerView;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import rocks.tbog.livewallpaperit.R;
import rocks.tbog.livewallpaperit.RecycleAdapterBase;
import rocks.tbog.livewallpaperit.WorkAsync.AsyncUtils;
import rocks.tbog.livewallpaperit.WorkAsync.RunnableTask;
import rocks.tbog.livewallpaperit.data.Image;
import rocks.tbog.livewallpaperit.utils.ViewUtils;

public class FavoriteAdapter extends RecycleAdapterBase<FavoriteAdapter.Item, FavoriteAdapter.ItemHolder> {
    private static final String TAG = FavoriteAdapter.class.getSimpleName();
    private int mWidth = 108;

    public FavoriteAdapter() {
        super(new ArrayList<>());
    }

    public void setWidth(int width) {
        mWidth = width;
        notifyItemRangeChanged(0, getItemCount());
    }

    @Override
    public void onBindViewHolder(@NonNull FavoriteAdapter.ItemHolder holder, @NonNull FavoriteAdapter.Item item) {
        holder.mImageView.setImageResource(R.drawable.ic_launcher_background);
        holder.mImageView.setScaleType(ImageView.ScaleType.FIT_XY);

        Image thumbnail = null;
        int delta = Integer.MAX_VALUE;
        for (var image : item.images) {
            if (image.isSource) continue;
            if (image.isObfuscated) continue;
            if (thumbnail == null || delta > Math.abs(thumbnail.width - image.width)) {
                delta = Math.abs(mWidth - image.width);
                thumbnail = image;
            }
        }
        if (thumbnail != null) {
            setImageViewSize(holder.mImageView, thumbnail);
            asyncLoadImage(holder, thumbnail);
        }
    }

    private void asyncLoadImage(@NonNull FavoriteAdapter.ItemHolder holder, @NonNull Image image) {
        Activity activity = ViewUtils.getActivity(holder.itemView);
        final Bitmap[] bitmapWrapper = new Bitmap[] {null};
        if (activity instanceof ComponentActivity) {
            holder.loadImageTask = AsyncUtils.runAsync(
                    ((ComponentActivity) activity).getLifecycle(),
                    task -> {
                        InputStream inputStream = null;
                        try {
                            URL url = new URL(image.url);
                            inputStream = url.openConnection().getInputStream();
                        } catch (IOException e) {
                            Log.e(TAG, "image " + image.mediaId + " failed to open connection to " + image.url, e);
                        }
                        if (task.isCancelled()) return;
                        if (inputStream == null) {
                            task.cancel();
                            return;
                        }
                        bitmapWrapper[0] = BitmapFactory.decodeStream(inputStream);
                    },
                    task -> {
                        if (holder.loadImageTask == task) {
                            holder.loadImageTask = null;
                        }
                        if (task.isCancelled()) return;
                        if (bitmapWrapper[0] != null) {
                            holder.mImageView.setImageBitmap(bitmapWrapper[0]);
                            holder.mImageView.setScaleType(ImageView.ScaleType.CENTER);
                        }
                    });
        }
    }

    @Override
    public void onViewRecycled(@NonNull FavoriteAdapter.ItemHolder holder) {
        if (holder.loadImageTask != null) {
            holder.loadImageTask.cancel(true);
            holder.loadImageTask = null;
        }
    }

    @NonNull
    @Override
    public FavoriteAdapter.ItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final Context context = parent.getContext();

        View itemView = new ImageView(context);
        itemView.setId(android.R.id.icon);
        itemView.setLayoutParams(new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        return new FavoriteAdapter.ItemHolder(itemView);
    }

    public void setImageViewSize(@NonNull ImageView view, @NonNull Image image) {
        float ratio = image.width / (float) image.height;
        var params = view.getLayoutParams();
        params.width = mWidth;
        params.height = Math.round(params.width / ratio);
        view.setLayoutParams(params);
    }

    public static class ItemHolder extends RecycleAdapterBase.Holder {
        public ImageView mImageView;
        public RunnableTask loadImageTask = null;

        public ItemHolder(@NonNull View itemView) {
            super(itemView);
            mImageView = itemView.findViewById(android.R.id.icon);
        }
    }

    public static class Item implements AdapterDiff {
        @NonNull
        public final ArraySet<Image> images = new ArraySet<>();

        public final Uri link;

        public Item(@NonNull List<Image> imageList, Uri uri) {
            images.addAll(imageList);
            link = uri;
        }

        @Override
        public long getAdapterItemId() {
            return images.valueAt(0).mediaId.hashCode();
        }
    }
}
