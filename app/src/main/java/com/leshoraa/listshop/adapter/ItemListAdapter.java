package com.leshoraa.listshop.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.animation.ObjectAnimator;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;

import com.google.android.material.snackbar.Snackbar;
import com.leshoraa.listshop.R;
import com.leshoraa.listshop.databinding.ListItem1Binding;
import com.leshoraa.listshop.model.Item;
import com.leshoraa.listshop.model.DatabaseHelper;

import java.io.File;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

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

    public interface OnItemQuantityChangeListener {
        void onQuantityChanged(int itemId, int newQuantity);
    }
    private OnItemQuantityChangeListener onItemQuantityChangeListener;

    public void setOnItemDeleteListener(OnItemDeleteListener listener) {
        this.onItemDeleteListener = listener;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    public void setOnItemQuantityChangeListener(OnItemQuantityChangeListener listener) {
        this.onItemQuantityChangeListener = listener;
    }

    public ItemListAdapter(Context context, List<Item> items) {
        this.context = context;
        this.items = items;
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
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
        return new ItemViewHolder(binding, this, onItemClickListener, onItemQuantityChangeListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        Item item = items.get(position);

        holder.binding.tvListTitle.setText(item.getName());
        holder.binding.tvListCategory.setText(item.getCategory());
        holder.binding.tvListPrice.setText(decimalFormat.format(item.getFinalPrice()));
        holder.binding.edtQuantity.setText(String.valueOf(item.getCount()));

        if (holder.quantityTextWatcher != null) {
            holder.binding.edtQuantity.removeTextChangedListener(holder.quantityTextWatcher);
        }
        holder.quantityTextWatcher = new QuantityTextWatcher(holder, item, dbHelper, onItemQuantityChangeListener);
        holder.binding.edtQuantity.addTextChangedListener(holder.quantityTextWatcher);

        holder.binding.tvReducequantity.setOnClickListener(v -> {
            int currentQuantity = 0;
            try {
                currentQuantity = Integer.parseInt(holder.binding.edtQuantity.getText().toString());
            } catch (NumberFormatException e) {
            }
            int newQuantity = Math.max(1, currentQuantity - 1);
            holder.binding.edtQuantity.setText(String.valueOf(newQuantity));
        });

        holder.binding.tvAddquantity.setOnClickListener(v -> {
            int currentQuantity = 0;
            try {
                currentQuantity = Integer.parseInt(holder.binding.edtQuantity.getText().toString());
            } catch (NumberFormatException e) {
            }
            int newQuantity = currentQuantity + 1;
            holder.binding.edtQuantity.setText(String.valueOf(newQuantity));
        });

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

        holder.setDeleteItemVisibility(View.VISIBLE);
        holder.binding.deleteItem.setTranslationX(0f);
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
        public final ListItem1Binding binding;
        TextWatcher quantityTextWatcher;

        public ItemViewHolder(@NonNull ListItem1Binding binding,
                              ItemListAdapter adapter,
                              OnItemClickListener clickListener,
                              OnItemQuantityChangeListener quantityChangeListener) {
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

        public void setDeleteItemVisibility(int visibility) {
            if (binding.deleteItem.getVisibility() == visibility) {
                return;
            }

            if (visibility == View.GONE) {
                ObjectAnimator animator = ObjectAnimator.ofFloat(binding.deleteItem, "translationX", 0f, binding.deleteItem.getWidth());
                animator.setDuration(200);
                animator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        binding.deleteItem.setVisibility(View.GONE);
                        binding.deleteItem.setTranslationX(0f);
                    }
                });
                animator.start();
            } else { // View.VISIBLE
                binding.deleteItem.setVisibility(View.VISIBLE);
                ObjectAnimator animator = ObjectAnimator.ofFloat(binding.deleteItem, "translationX", binding.deleteItem.getWidth(), 0f);
                animator.setDuration(200);
                animator.start();
            }
        }
    }

    private static class QuantityTextWatcher implements TextWatcher {
        private ItemViewHolder holder;
        private Item item;
        private DatabaseHelper dbHelper;
        private OnItemQuantityChangeListener listener;

        private QuantityTextWatcher(ItemViewHolder holder, Item item, DatabaseHelper dbHelper, OnItemQuantityChangeListener listener) {
            this.holder = holder;
            this.item = item;
            this.dbHelper = dbHelper;
            this.listener = listener;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            String quantityString = s.toString();
            int newQuantity = 0;
            try {
                newQuantity = Integer.parseInt(quantityString);
            } catch (NumberFormatException e) {
                newQuantity = 0;
            }

            if (newQuantity < 1 && !quantityString.isEmpty()) {
                newQuantity = 1;
                if (!quantityString.equals("1")) {
                    holder.binding.edtQuantity.setText("1");
                    holder.binding.edtQuantity.setSelection("1".length());
                }
            } else if (quantityString.isEmpty()) {
                newQuantity = 0;
            }

            if (item.getCount() != newQuantity) {
                item.setCount(newQuantity);

                item.setFinalPrice(item.getPrice() * newQuantity);

                dbHelper.updateItemQuantity(item.getId(), newQuantity);

                if (listener != null) {
                    listener.onQuantityChanged(item.getId(), newQuantity);
                }
            }
        }
    }
}