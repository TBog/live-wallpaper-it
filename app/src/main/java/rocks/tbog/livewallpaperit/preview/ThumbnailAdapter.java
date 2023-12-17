package rocks.tbog.livewallpaperit.preview;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.ArrayMap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Set;
import rocks.tbog.livewallpaperit.R;
import rocks.tbog.livewallpaperit.RecycleAdapterBase;
import rocks.tbog.livewallpaperit.WorkAsync.AsyncUtils;
import rocks.tbog.livewallpaperit.WorkAsync.RunnableTask;
import rocks.tbog.livewallpaperit.data.MediaInfo;
import rocks.tbog.livewallpaperit.data.SubTopic;
import rocks.tbog.livewallpaperit.utils.ViewUtils;

public class ThumbnailAdapter extends RecycleAdapterBase<ThumbnailAdapter.Item, ThumbnailAdapter.ThumbnailHolder> {
    private static final String TAG = ThumbnailAdapter.class.getSimpleName();
    private final int mWidth;
    private final Set<String> mInvalidMediaIdSet;
    private final Set<MediaInfo> mFavoriteMediaSet;

    public ThumbnailAdapter(
            SubTopic topic,
            int width,
            boolean showObfuscated,
            @NonNull Set<String> invalidMediaIdSet,
            @NonNull Set<MediaInfo> favoriteMediaSet) {
        super(new ArrayList<>());
        mWidth = width;
        mInvalidMediaIdSet = invalidMediaIdSet;
        mFavoriteMediaSet = favoriteMediaSet;
        ArrayMap<String, Uri> links = new ArrayMap<>();
        for (var image : topic.images) {
            if (image.isObfuscated != showObfuscated) continue;
            if (image.isSource) {
                links.put(image.mediaId, Uri.parse(image.url));
            }
        }
        ArrayMap<String, Item> map = new ArrayMap<>();
        int delta = Integer.MAX_VALUE;
        for (var image : topic.images) {
            if (image.isSource) continue;
            if (image.isObfuscated != showObfuscated) continue;
            var item = map.get(image.mediaId);
            if (item == null || delta > Math.abs(item.image.width - image.width)) {
                delta = Math.abs(mWidth - image.width);
                item = new Item(image, links.get(image.mediaId));
                map.put(image.mediaId, item);
            }
        }
        mItemList.addAll(map.values());
    }

    private boolean isInvalidMedia(String mediaId) {
        return mInvalidMediaIdSet.contains(mediaId);
    }

    private boolean isFavoriteMedia(String mediaId) {
        return mFavoriteMediaSet.stream().anyMatch(info -> info.mediaId.equals(mediaId));
    }

    @Override
    public void onBindViewHolder(@NonNull ThumbnailHolder holder, @NonNull Item item) {
        holder.mImageView.setImageResource(R.drawable.ic_launcher_background);
        holder.mImageView.setScaleType(ImageView.ScaleType.FIT_XY);
        holder.mInvalidView.setVisibility(isInvalidMedia(item.image.mediaId) ? View.VISIBLE : View.GONE);
        holder.mFavoriteView.setVisibility(isFavoriteMedia(item.image.mediaId) ? View.VISIBLE : View.GONE);
        setImageViewSize(holder.mImageView, item.image);

        Activity activity = ViewUtils.getActivity(holder.itemView);
        final Bitmap[] bitmapWrapper = new Bitmap[] {null};
        if (activity instanceof ComponentActivity) {
            holder.loadImageTask = AsyncUtils.runAsync(
                    ((ComponentActivity) activity).getLifecycle(),
                    task -> {
                        InputStream inputStream = null;
                        try {
                            URL url = new URL(item.image.url);
                            inputStream = url.openConnection().getInputStream();
                        } catch (IOException e) {
                            Log.e(TAG, "image " + item.image.mediaId + " failed to open connection", e);
                        }
                        if (task.isCancelled()) return;
                        if (inputStream == null) {
                            task.cancel();
                            return;
                        }
                        bitmapWrapper[0] = BitmapFactory.decodeStream(inputStream);
                    },
                    task -> {
                        if (task.isCancelled()) return;
                        if (bitmapWrapper[0] != null) {
                            holder.mImageView.setImageBitmap(bitmapWrapper[0]);
                            holder.mImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        }
                        if (holder.loadImageTask == task) {
                            holder.loadImageTask = null;
                        }
                    });
        }
    }

    @Override
    public void onViewRecycled(@NonNull ThumbnailHolder holder) {
        if (holder.loadImageTask != null) {
            holder.loadImageTask.cancel();
            holder.loadImageTask = null;
        }
    }

    public void setImageViewSize(@NonNull ImageView view, @NonNull SubTopic.Image image) {
        float ratio = image.width / (float) image.height;
        var params = view.getLayoutParams();
        params.width = mWidth;
        params.height = Math.round(params.width / ratio);
        view.setLayoutParams(params);
    }

    @NonNull
    @Override
    public ThumbnailHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final Context context = parent.getContext();

        LayoutInflater inflater = LayoutInflater.from(context);
        View itemView = inflater.inflate(R.layout.image_carousel_item, parent, false);

        return new ThumbnailHolder(itemView);
    }

    public static class ThumbnailHolder extends RecycleAdapterBase.Holder {
        public ImageView mImageView;
        public ImageView mInvalidView;
        public ImageView mFavoriteView;
        public RunnableTask loadImageTask = null;

        public ThumbnailHolder(@NonNull View itemView) {
            super(itemView);
            mImageView = itemView.findViewById(android.R.id.icon);
            mInvalidView = itemView.findViewById(R.id.invalid);
            mFavoriteView = itemView.findViewById(R.id.favorite);
        }
    }

    public static class Item implements AdapterDiff {
        @NonNull
        final SubTopic.Image image;

        final Uri link;

        public Item(@NonNull SubTopic.Image image, Uri uri) {
            this.image = image;
            this.link = uri;
        }

        @Override
        public long getAdapterItemId() {
            return image.mediaId.hashCode();
        }
    }
}
