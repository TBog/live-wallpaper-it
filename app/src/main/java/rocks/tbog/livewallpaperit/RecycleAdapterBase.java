package rocks.tbog.livewallpaperit;

import android.annotation.SuppressLint;
import android.util.Log;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class RecycleAdapterBase<T, VH extends RecycleAdapterBase.Holder> extends RecyclerView.Adapter<VH> {

    private static final String TAG = RecycleAdapterBase.class.getSimpleName();

    @NonNull
    protected final List<T> mItemList;

    @Nullable
    private OnClickListener<T, RecycleAdapterBase<T, VH>> mOnClickListener = null;

    @Nullable
    private OnLongClickListener<T, RecycleAdapterBase<T, VH>> mOnLongClickListener = null;

    public RecycleAdapterBase(@NonNull List<T> list) {
        mItemList = list;
    }

    public void setOnClickListener(@Nullable OnClickListener<T, RecycleAdapterBase<T, VH>> listener) {
        mOnClickListener = listener;
    }

    public void setOnLongClickListener(@Nullable OnLongClickListener<T, RecycleAdapterBase<T, VH>> listener) {
        mOnLongClickListener = listener;
    }

    @Override
    public long getItemId(int position) {
        final T entry = getItem(position);
        if (entry == null) return -1;
        return entry.hashCode();
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        final T entry = getItem(position);
        if (entry == null) return;

        if (mOnClickListener != null) holder.setOnClickListener(v -> mOnClickListener.onClick(this, entry, v));
        else holder.setOnClickListener(null);

        if (mOnLongClickListener != null)
            holder.setOnLongClickListener(v -> mOnLongClickListener.onLongClick(this, entry, v));
        else holder.setOnLongClickListener(null);

        onBindViewHolder(holder, entry);
    }

    public abstract void onBindViewHolder(@NonNull VH holder, @NonNull T entry);

    @Override
    public int getItemCount() {
        return mItemList.size();
    }

    @Nullable
    public T getItem(int index) {
        final T entry;
        try {
            entry = mItemList.get(index);
        } catch (ArrayIndexOutOfBoundsException e) {
            Log.e(TAG, "pos=" + index + " size=" + mItemList.size(), e);
            return null;
        }
        return entry;
    }

    public void getItems(Collection<T> outItems) {
        outItems.addAll(mItemList);
    }

    public List<T> getItems() {
        return Collections.unmodifiableList(mItemList);
    }

    public void removeItem(T result) {
        int position = mItemList.indexOf(result);
        mItemList.remove(result);
        notifyItemRemoved(position);
    }

    public void addItem(T item) {
        notifyItemInserted(mItemList.size());
        mItemList.add(item);
    }

    public void addItems(Collection<T> list) {
        notifyItemRangeInserted(mItemList.size(), list.size());
        mItemList.addAll(list);
    }

    public void clear() {
        final int itemCount = mItemList.size();
        mItemList.clear();
        notifyItemRangeRemoved(0, itemCount);
    }

    public void refresh() {
        final int itemCount = mItemList.size();
        notifyItemRangeChanged(0, itemCount);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setItems(Collection<? extends T> results) {
        this.mItemList.clear();
        this.mItemList.addAll(results);
        notifyDataSetChanged();
    }

    public void notifyItemChanged(T result) {
        int position = mItemList.indexOf(result);
        // Log.d(TAG, "notifyItemChanged #" + position + " id=" + result.id);
        if (position >= 0) notifyItemChanged(position);
    }

    public static class Holder extends RecyclerView.ViewHolder {

        public Holder(@NonNull View itemView) {
            super(itemView);
            itemView.setTag(this);
        }

        public void setOnClickListener(@Nullable View.OnClickListener listener) {
            itemView.setOnClickListener(listener);
            if (listener == null) itemView.setClickable(false);
        }

        public void setOnLongClickListener(@Nullable View.OnLongClickListener listener) {
            itemView.setOnLongClickListener(listener);
            if (listener == null) itemView.setLongClickable(false);
        }
    }

    public interface OnClickListener<ItemType, Adapter> {
        void onClick(Adapter adapter, ItemType entry, View view);
    }

    public interface OnLongClickListener<ItemType, Adapter> {
        boolean onLongClick(Adapter adapter, ItemType entry, View view);
    }
}
