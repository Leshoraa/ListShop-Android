package com.leshoraa.listshop.adapter;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.leshoraa.listshop.R;
import com.leshoraa.listshop.databinding.ItemEdtDiscountBinding;

import java.util.List;

public class DiscountAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_DISCOUNT_ITEM = 0;
    private static final int VIEW_TYPE_ADD_BUTTON = 1;

    private List<String> discounts;
    private OnItemDeleteListener onItemDeleteListener;
    private OnEditTextChangeListener onEditTextChangeListener;
    private OnAddButtonClickListener onAddButtonClickListener;

    public interface OnItemDeleteListener {
        void onDelete(int position);
    }

    public interface OnEditTextChangeListener {
        void onTextChange(int position, String text);
    }

    public interface OnAddButtonClickListener {
        void onAddClick();
    }

    public void setOnItemDeleteListener(OnItemDeleteListener listener) {
        this.onItemDeleteListener = listener;
    }

    public void setOnEditTextChangeListener(OnEditTextChangeListener listener) {
        this.onEditTextChangeListener = listener;
    }

    public void setOnAddButtonClickListener(OnAddButtonClickListener listener) {
        this.onAddButtonClickListener = listener;
    }

    public DiscountAdapter(List<String> discounts) {
        this.discounts = discounts;
    }
    public String getDiscountAt(int position) {
        if (position < discounts.size()) {
            return discounts.get(position);
        }
        return "";
    }
    public void updateData(List<String> newDiscounts) {
        this.discounts.clear();
        this.discounts.addAll(newDiscounts);
        notifyDataSetChanged();
    }
    @Override
    public int getItemViewType(int position) {
        if (position == discounts.size()) {
            return VIEW_TYPE_ADD_BUTTON;
        } else {
            return VIEW_TYPE_DISCOUNT_ITEM;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_DISCOUNT_ITEM) {
            ItemEdtDiscountBinding itemBinding = ItemEdtDiscountBinding.inflate(
                    LayoutInflater.from(parent.getContext()),
                    parent,
                    false
            );
            return new DiscountViewHolder(itemBinding, onItemDeleteListener, onEditTextChangeListener);
        } else {
            View addButtonView = LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.item_add_edt_discount,
                    parent,
                    false
            );
            return new AddButtonViewHolder(addButtonView, onAddButtonClickListener);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == VIEW_TYPE_DISCOUNT_ITEM) {
            DiscountViewHolder discountHolder = (DiscountViewHolder) holder;
            discountHolder.edtDiscount.setText(discounts.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return discounts.size() + 1;
    }

    public static class DiscountViewHolder extends RecyclerView.ViewHolder {
        EditText edtDiscount;
        TextView tvRemove;

        public DiscountViewHolder(@NonNull ItemEdtDiscountBinding binding, OnItemDeleteListener deleteListener, OnEditTextChangeListener textChangeListener) {
            super(binding.getRoot());
            edtDiscount = binding.edtDiscount;
            tvRemove = binding.tvRemove;

            tvRemove.setOnClickListener(v -> {
                if (deleteListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        deleteListener.onDelete(position);
                    }
                }
            });

            edtDiscount.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && textChangeListener != null) {
                        textChangeListener.onTextChange(position, s.toString());
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        }
    }

    public static class AddButtonViewHolder extends RecyclerView.ViewHolder {
        public AddButtonViewHolder(@NonNull View itemView, OnAddButtonClickListener listener) {
            super(itemView);
            itemView.findViewById(R.id.add_itemDiscount).setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAddClick();
                }
            });
        }
    }
}
