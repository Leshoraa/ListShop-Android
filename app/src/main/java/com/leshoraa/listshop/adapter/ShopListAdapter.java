package com.leshoraa.listshop.adapter;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.leshoraa.listshop.databinding.ShoplistItemBinding;
import com.leshoraa.listshop.model.DatabaseHelper;
import com.leshoraa.listshop.model.Item;

import java.util.List;

public class ShopListAdapter extends RecyclerView.Adapter<ShopListAdapter.TodoViewHolder> {

    private final Context context;
    private final List<Item> todoList;
    private OnTodoItemActionListener listener;
    private RecyclerView recyclerView;
    private DatabaseHelper dbHelper;

    private static class DeletedState {
        Item item;
        int position;
        Snackbar snackbar;

        DeletedState(Item item, int position, Snackbar snackbar) {
            this.item = item;
            this.position = position;
            this.snackbar = snackbar;
        }
    }

    public interface OnTodoItemActionListener {
        void onTodoItemChecked(int position, boolean isChecked);
        void onTodoItemNameChanged(int position, String newName);
        void onTodoItemDeleted(int itemId);
    }

    public void setOnTodoItemActionListener(OnTodoItemActionListener listener) {
        this.listener = listener;
    }

    public ShopListAdapter(Context context, List<Item> todoList) {
        this.context = context;
        this.todoList = todoList;
        this.dbHelper = new DatabaseHelper(context);
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
    }

    @NonNull
    @Override
    public TodoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ShoplistItemBinding binding = ShoplistItemBinding.inflate(LayoutInflater.from(context), parent, false);
        return new TodoViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TodoViewHolder holder, int position) {
        Item todoItem = todoList.get(position);
        holder.binding.itemNumberTextview.setText(String.valueOf(position + 1) + ".");
        holder.binding.todoItemNameEdittext.setText(todoItem.getName());

        holder.binding.todoItemCheckbox.setOnCheckedChangeListener(null);
        holder.binding.todoItemCheckbox.setChecked(todoItem.isAddButton());
        applyStrikeThrough(holder.binding.todoItemNameEdittext, todoItem.isAddButton(), false);

        holder.binding.todoItemCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int currentPosition = holder.getAdapterPosition();
            if (currentPosition != RecyclerView.NO_POSITION) {
                applyStrikeThrough(holder.binding.todoItemNameEdittext, isChecked, true);
                todoList.get(currentPosition).setAddButton(isChecked);
                if (listener != null) {
                    listener.onTodoItemChecked(currentPosition, isChecked);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return todoList.size();
    }

    private void applyStrikeThrough(EditText editText, boolean isChecked, boolean animate) {
        if (isChecked) {
            editText.setPaintFlags(editText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            ObjectAnimator colorAnim = ObjectAnimator.ofArgb(editText, "textColor", Color.BLACK, Color.GRAY);
            ObjectAnimator alphaAnim = ObjectAnimator.ofFloat(editText, "alpha", 1.0f, 0.6f);
            if (animate) {
                colorAnim.setDuration(300);
                alphaAnim.setDuration(300);
                colorAnim.start();
                alphaAnim.start();
            } else {
                editText.setTextColor(Color.GRAY);
                editText.setAlpha(0.6f);
            }
        } else {
            editText.setPaintFlags(editText.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            ObjectAnimator colorAnim = ObjectAnimator.ofArgb(editText, "textColor", Color.GRAY, Color.BLACK);
            ObjectAnimator alphaAnim = ObjectAnimator.ofFloat(editText, "alpha", 0.6f, 1.0f);
            if (animate) {
                colorAnim.setDuration(300);
                alphaAnim.setDuration(300);
                colorAnim.start();
                alphaAnim.start();
            } else {
                editText.setTextColor(Color.BLACK);
                editText.setAlpha(1.0f);
            }
        }
    }

    public void removeItem(int position) {
        if (position >= 0 && position < todoList.size()) {
            Item deletedItem = todoList.get(position);
            int deletedPosition = position;
            todoList.remove(position);
            notifyItemRemoved(position);
            showUndoSnackbar(new DeletedState(deletedItem, deletedPosition, null));
        }
    }

    private void showUndoSnackbar(DeletedState deletedState) {
        if (recyclerView == null) return;
        View rootView = recyclerView.getRootView();
        Snackbar snackbar = Snackbar.make(rootView, "Item deleted", Snackbar.LENGTH_LONG);
        deletedState.snackbar = snackbar;
        snackbar.setAction("Undo", v -> undoDelete(deletedState));
        snackbar.addCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(@NonNull Snackbar transientBottomBar, int event) {
                if (event != DISMISS_EVENT_ACTION) {
                    confirmDelete(deletedState);
                }
            }
        });
        snackbar.show();
    }

    private void undoDelete(DeletedState deletedState) {
        if (deletedState.item != null && deletedState.position != RecyclerView.NO_POSITION) {
            if (deletedState.position > todoList.size()) {
                todoList.add(deletedState.item);
                notifyItemInserted(todoList.size() - 1);
            } else {
                todoList.add(deletedState.position, deletedState.item);
                notifyItemInserted(deletedState.position);
            }
            if (recyclerView != null) {
                recyclerView.scrollToPosition(deletedState.position);
            }
        }
    }

    private void confirmDelete(DeletedState deletedState) {
        if (deletedState.item != null && listener != null) {
            listener.onTodoItemDeleted(deletedState.item.getId());
        }
    }

    public class TodoViewHolder extends RecyclerView.ViewHolder {
        final ShoplistItemBinding binding;

        public TodoViewHolder(@NonNull ShoplistItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            binding.todoItemNameEdittext.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (getAdapterPosition() != RecyclerView.NO_POSITION && listener != null) {
                        listener.onTodoItemNameChanged(getAdapterPosition(), s.toString());
                    }
                }
                @Override
                public void afterTextChanged(Editable s) {}
            });

            binding.todoItemNameEdittext.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    binding.todoItemDeleteButton.setVisibility(View.VISIBLE);
                } else {
                    binding.todoItemDeleteButton.postDelayed(() -> {
                        if (!v.isFocused()) {
                            binding.todoItemDeleteButton.setVisibility(View.GONE);
                        }
                    }, 100);
                }
            });

            binding.todoItemDeleteButton.setOnClickListener(v -> {
                int adapterPosition = getAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    removeItem(adapterPosition);
                }
            });
        }
    }
}