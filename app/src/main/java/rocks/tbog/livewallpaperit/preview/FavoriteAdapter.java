package rocks.tbog.livewallpaperit.preview;

import android.content.Context;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.collection.ArraySet;
import androidx.recyclerview.widget.RecyclerView;
import com.squareup.picasso.Picasso;
import java.util.ArrayList;
import java.util.List;
import rocks.tbog.livewallpaperit.R;
import rocks.tbog.livewallpaperit.RecycleAdapterBase;
import rocks.tbog.livewallpaperit.asynchronous.RunnableTask;
import rocks.tbog.livewallpaperit.data.Image;

public class FavoriteAdapter extends RecycleAdapterBase<FavoriteAdapter.Item, FavoriteAdapter.ItemHolder> {
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
            Picasso.get()
                    .load(thumbnail.url)
                    .noPlaceholder()
                    .resize(thumbnail.width, 0)
                    .centerCrop()
                    .into(holder.mImageView);
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
        var params =
                new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        var m = context.getResources().getDimensionPixelSize(R.dimen.margin) / 2;
        params.setMargins(m, m, m, m);
        itemView.setLayoutParams(params);

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
