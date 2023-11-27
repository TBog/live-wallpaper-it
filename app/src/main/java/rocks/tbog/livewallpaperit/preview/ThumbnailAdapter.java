package rocks.tbog.livewallpaperit.preview;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import java.util.HashMap;
import java.util.Set;
import rocks.tbog.livewallpaperit.R;
import rocks.tbog.livewallpaperit.RecycleAdapterBase;
import rocks.tbog.livewallpaperit.WorkAsync.AsyncUtils;
import rocks.tbog.livewallpaperit.data.SubTopic;
import rocks.tbog.livewallpaperit.utils.ViewUtils;

public class ThumbnailAdapter extends RecycleAdapterBase<SubTopic.Image, ThumbnailAdapter.ThumbnailHolder> {
    private static final String TAG = ThumbnailAdapter.class.getSimpleName();
    private final int mWidth;
    private final Set<String> mInvalidMediaIdSet;

    public ThumbnailAdapter(SubTopic topic, int width, boolean showObfuscated, @NonNull Set<String> invalidMediaIdSet) {
        super(new ArrayList<>());
        mWidth = width;
        mInvalidMediaIdSet = invalidMediaIdSet;
        HashMap<String, SubTopic.Image> map = new HashMap<>();
        int delta = Integer.MAX_VALUE;
        for (var image : topic.images) {
            if (image.isSource) continue;
            if (image.isObfuscated != showObfuscated) continue;
            var mapImage = map.get(image.mediaId);
            if (mapImage == null || delta > Math.abs(mapImage.width - image.width)) {
                delta = Math.abs(mWidth - image.width);
                map.put(image.mediaId, image);
            }
        }
        mItemList.addAll(map.values());
    }

    @Override
    public void onBindViewHolder(@NonNull ThumbnailHolder holder, @NonNull SubTopic.Image image) {
        holder.mImageView.setImageResource(R.drawable.ic_launcher_background);
        holder.mInvalidView.setVisibility(mInvalidMediaIdSet.contains(image.mediaId) ? View.VISIBLE : View.GONE);

        setImageViewSize(holder.mImageView, image);

        Activity activity = ViewUtils.getActivity(holder.itemView);
        final Bitmap[] bitmapWrapper = new Bitmap[] {null};
        if (activity instanceof ComponentActivity)
            AsyncUtils.runAsync(
                    ((ComponentActivity) activity).getLifecycle(),
                    task -> {
                        InputStream inputStream = null;
                        try {
                            URL url = new URL(image.url);
                            inputStream = url.openConnection().getInputStream();
                        } catch (IOException e) {
                            Log.e(TAG, "image " + image.mediaId + " failed to open connection", e);
                        }
                        if (inputStream == null) task.cancel();
                        bitmapWrapper[0] = BitmapFactory.decodeStream(inputStream);
                    },
                    task -> {
                        if (bitmapWrapper[0] != null) {
                            holder.mImageView.setImageBitmap(bitmapWrapper[0]);
                        }
                    });
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
        ImageView mImageView;
        ImageView mInvalidView;

        public ThumbnailHolder(@NonNull View itemView) {
            super(itemView);
            mImageView = itemView.findViewById(android.R.id.icon);
            mInvalidView = itemView.findViewById(R.id.invalid);
        }
    }
}
