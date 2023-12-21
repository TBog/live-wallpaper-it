package rocks.tbog.livewallpaperit.preview;

import android.content.Context;
import android.net.Uri;
import android.util.ArrayMap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import com.squareup.picasso.Picasso;
import java.util.ArrayList;
import java.util.Set;
import rocks.tbog.livewallpaperit.R;
import rocks.tbog.livewallpaperit.RecycleAdapterBase;
import rocks.tbog.livewallpaperit.WorkAsync.RunnableTask;
import rocks.tbog.livewallpaperit.data.Image;
import rocks.tbog.livewallpaperit.data.MediaInfo;
import rocks.tbog.livewallpaperit.data.SubTopic;

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

    public void addInvalidMedia(ThumbnailAdapter.Item thumbnail) {
        mInvalidMediaIdSet.add(thumbnail.image.mediaId);
        notifyItemChanged(thumbnail);
    }

    public void removeInvalidMedia(ThumbnailAdapter.Item thumbnail) {
        mInvalidMediaIdSet.remove(thumbnail.image.mediaId);
        notifyItemChanged(thumbnail);
    }

    private boolean isInvalidMedia(String mediaId) {
        return mInvalidMediaIdSet.contains(mediaId);
    }

    private boolean isFavoriteMedia(String mediaId) {
        return mFavoriteMediaSet.stream().anyMatch(info -> info.mediaId.equals(mediaId));
    }

    @Override
    public void onBindViewHolder(@NonNull ThumbnailHolder holder, @NonNull Item item) {
        holder.mInvalidView.setVisibility(isInvalidMedia(item.image.mediaId) ? View.VISIBLE : View.GONE);
        holder.mFavoriteView.setVisibility(isFavoriteMedia(item.image.mediaId) ? View.VISIBLE : View.GONE);

        holder.mImageView.setImageResource(R.drawable.ic_launcher_background);
        holder.mImageView.setScaleType(ImageView.ScaleType.FIT_XY);
        setImageViewSize(holder.mImageView, item.image);
        Picasso.get()
                .load(item.image.url)
                .noPlaceholder()
                .resize(item.image.width, 0)
                .centerCrop()
                .into(holder.mImageView);
    }

    @Override
    public void onViewRecycled(@NonNull ThumbnailHolder holder) {
        if (holder.loadImageTask != null) {
            holder.loadImageTask.cancel(true);
            holder.loadImageTask = null;
        }
    }

    public void setImageViewSize(@NonNull ImageView view, @NonNull Image image) {
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
        final Image image;

        final Uri link;

        public Item(@NonNull Image image, Uri uri) {
            this.image = image;
            this.link = uri;
        }

        @Override
        public long getAdapterItemId() {
            return image.mediaId.hashCode();
        }
    }
}
