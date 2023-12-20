package rocks.tbog.livewallpaperit.preview;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import rocks.tbog.livewallpaperit.R;
import rocks.tbog.livewallpaperit.RecycleAdapterBase;
import rocks.tbog.livewallpaperit.WorkAsync.AsyncUtils;
import rocks.tbog.livewallpaperit.WorkAsync.RunnableTask;
import rocks.tbog.livewallpaperit.data.Image;
import rocks.tbog.livewallpaperit.utils.ViewUtils;

public class FavoriteAdapter extends RecycleAdapterBase<ThumbnailAdapter.Item, FavoriteAdapter.ItemHolder> {
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
    public void onBindViewHolder(@NonNull FavoriteAdapter.ItemHolder holder, @NonNull ThumbnailAdapter.Item item) {
        holder.mImageView.setImageResource(R.drawable.ic_launcher_background);
        holder.mImageView.setScaleType(ImageView.ScaleType.FIT_XY);
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
}
