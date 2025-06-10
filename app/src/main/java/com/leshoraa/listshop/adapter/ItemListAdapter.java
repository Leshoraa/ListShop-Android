package com.leshoraa.listshop.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.leshoraa.listshop.R;
import com.leshoraa.listshop.databinding.ListItem1Binding;
import com.leshoraa.listshop.model.Item;
import com.leshoraa.listshop.model.DatabaseHelper;

import java.io.File;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;

public class ItemListAdapter extends RecyclerView.Adapter<ItemListAdapter.ItemViewHolder> {

    private List<Item> items;
    private Context context;
    private OnItemDeleteListener onItemDeleteListener;
    private OnItemClickListener onItemClickListener;
    private DecimalFormat decimalFormat;

    private DatabaseHelper dbHelper;
    private Item recentlyDeletedItem;
    private int recentlyDeletedItemPosition;
    private RecyclerView recyclerView;

    public interface OnItemDeleteListener {
        void onDelete(int itemId);
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public void setOnItemDeleteListener(OnItemDeleteListener listener) {
        this.onItemDeleteListener = listener;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    public ItemListAdapter(Context context, List<Item> items) {
        this.context = context;
        this.items = items;
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        decimalFormat = new DecimalFormat("#,##0", symbols);
        this.dbHelper = new DatabaseHelper(context);
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ListItem1Binding binding = ListItem1Binding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ItemViewHolder(binding, this, onItemClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        Item item = items.get(position);

        holder.binding.tvListTitle.setText(item.getName());
        holder.binding.tvListCategory.setText(item.getCategory());
        holder.binding.tvListPrice.setText(decimalFormat.format(item.getFinalPrice()));

        if (item.getImageData() != null && !item.getImageData().isEmpty()) {
            File imgFile = new File(context.getFilesDir(), item.getImageData());
            if (imgFile.exists()) {
                Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                holder.binding.imgItemPreview.setImageBitmap(myBitmap);
            } else {
                holder.binding.imgItemPreview.setImageResource(R.drawable.ic_launcher_background);
            }
        } else {
            holder.binding.imgItemPreview.setImageResource(R.drawable.ic_launcher_background);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void removeItem(int position) {
        if (position >= 0 && position < items.size()) {
            recentlyDeletedItem = items.get(position);
            recentlyDeletedItemPosition = position;
            items.remove(position);
            notifyItemRemoved(position);
            showUndoSnackbar();
        }
    }

    private void showUndoSnackbar() {
        if (recyclerView == null) return;
        View rootView = recyclerView;
        Snackbar snackbar = Snackbar.make(rootView, "Item deleted", Snackbar.LENGTH_LONG);
        snackbar.setAction("Undo", v -> undoDelete());
        snackbar.addCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar transientBottomBar, int event) {
                if (event != DISMISS_EVENT_ACTION) {
                    confirmDelete();
                }
            }
        });
        snackbar.show();
    }

    private void undoDelete() {
        if (recentlyDeletedItem != null) {
            items.add(recentlyDeletedItemPosition, recentlyDeletedItem);
            notifyItemInserted(recentlyDeletedItemPosition);
            if (recyclerView != null) {
                recyclerView.scrollToPosition(recentlyDeletedItemPosition);
            }
            recentlyDeletedItem = null;
            recentlyDeletedItemPosition = -1;
        }
    }

    private void confirmDelete() {
        if (recentlyDeletedItem != null) {
            if (onItemDeleteListener != null) {
                onItemDeleteListener.onDelete(recentlyDeletedItem.getId());
            }
            recentlyDeletedItem = null;
            recentlyDeletedItemPosition = -1;
        }
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        final ListItem1Binding binding;

        public ItemViewHolder(@NonNull ListItem1Binding binding, ItemListAdapter adapter, OnItemClickListener clickListener) {
            super(binding.getRoot());
            this.binding = binding;

            binding.deleteItem.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    adapter.removeItem(position);
                }
            });

            binding.getRoot().setOnClickListener(v -> {
                if (clickListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        clickListener.onItemClick(position);
                    }
                }
            });
        }
    }
}