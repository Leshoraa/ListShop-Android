package com.leshoraa.listshop.adapter;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.snackbar.Snackbar;
import com.leshoraa.listshop.MainActivity;
import com.leshoraa.listshop.databinding.MainItem1Binding;
import com.leshoraa.listshop.databinding.ItemDateHeaderBinding;
import com.leshoraa.listshop.model.DateHeader;
import com.leshoraa.listshop.model.Item;
import com.leshoraa.listshop.model.DatabaseHelper;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "ItemAdapter";

    public static final int VIEW_TYPE_ITEM = 0;
    public static final int VIEW_TYPE_HEADER = 1;
    public static final int VIEW_TYPE_ADD_BUTTON = 2;

    private List<Object> combinedList;
    public OnAddItemClickListener addItemClickListener;
    public OnDeleteItemClickListener deleteItemClickListener;
    private OnItemClickListener onItemClickListener;
    private int focusedPosition = -1;

    private DatabaseHelper dbHelper;
    private Object recentlyDeletedObject;
    private int recentlyDeletedPosition;
    private RecyclerView recyclerView;

    public interface OnAddItemClickListener {
        void onAddItemClick();
    }

    public interface OnDeleteItemClickListener {
        void onDeleteItemClick(int id);
    }

    public interface OnItemClickListener {
        void onItemClick(int itemId);
    }


    public interface OnDeleteListItemClickListener {
        void onDeleteListItemClick(int itemId);
    }

    private OnDeleteListItemClickListener deleteListItemClickListener;

    public void setOnDeleteListItemClickListener(OnDeleteListItemClickListener listener) {
        this.deleteListItemClickListener = listener;
    }


    public ItemAdapter(List<Item> initialItemList, OnAddItemClickListener addListener,
                       OnDeleteItemClickListener deleteListener, Context context) {
        this.addItemClickListener = addListener;
        this.deleteItemClickListener = deleteListener;
        this.combinedList = new ArrayList<>();
        this.dbHelper = new DatabaseHelper(context);
        setItems(initialItemList);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    public void setItems(List<Item> rawItemList) {
        Collections.sort(rawItemList, (item1, item2) -> {
            int dateCompare = item2.getDate().compareTo(item1.getDate());
            if (dateCompare == 0) {
                return Integer.compare(item2.getId(), item1.getId());
            }
            return dateCompare;
        });

        LinkedHashMap<String, List<Item>> groupedItems = new LinkedHashMap<>();
        for (Item item : rawItemList) {
            groupedItems.computeIfAbsent(item.getDate(), k -> new ArrayList<>()).add(item);
        }

        List<Object> tempCombinedList = new ArrayList<>();
        String currentDbDate = MainActivity.DB_DATE_FORMAT.format(new Date());

        List<String> sortedDbDates = new ArrayList<>(groupedItems.keySet());
        Collections.sort(sortedDbDates, (d1, d2) -> d2.compareTo(d1));

        Item addButton = new Item("Add Item", 0, true, currentDbDate);

        tempCombinedList.add(new DateHeader(MainActivity.UI_DATE_FORMAT.format(new Date())));
        tempCombinedList.add(addButton);

        if (groupedItems.containsKey(currentDbDate)) {
            tempCombinedList.addAll(groupedItems.get(currentDbDate));
            sortedDbDates.remove(currentDbDate);
        }

        for (String dbDate : sortedDbDates) {
            if (!groupedItems.get(dbDate).isEmpty()) {
                try {
                    String uiDate = MainActivity.UI_DATE_FORMAT.format(MainActivity.DB_DATE_FORMAT.parse(dbDate));
                    tempCombinedList.add(new DateHeader(uiDate));
                    tempCombinedList.addAll(groupedItems.get(dbDate));
                } catch (ParseException e) {
                    e.printStackTrace();
                    tempCombinedList.add(new DateHeader("Invalid Date: " + dbDate));
                    tempCombinedList.addAll(groupedItems.get(dbDate));
                }
            }
        }

        this.combinedList.clear();
        this.combinedList.addAll(tempCombinedList);
        notifyDataSetChanged();
    }

    private int compareItemNamesNumerically(String name1, String name2) {
        Pattern p = Pattern.compile("Item (\\d+)");
        Matcher m1 = p.matcher(name1);
        Matcher m2 = p.matcher(name2);

        int num1 = -1;
        int num2 = -1;

        if (m1.matches()) {
            num1 = Integer.parseInt(m1.group(1));
        }
        if (m2.matches()) {
            num2 = Integer.parseInt(m2.group(1));
        }

        if (num1 != -1 && num2 != -1) {
            return Integer.compare(num2, num1);
        }
        return name2.compareTo(name1);
    }

    public void setFocusedPosition(int position) {
        int oldFocusedPosition = this.focusedPosition;
        this.focusedPosition = position;

        if (oldFocusedPosition != RecyclerView.NO_POSITION) {
            notifyItemChanged(oldFocusedPosition);
        }
        if (position != RecyclerView.NO_POSITION) {
            notifyItemChanged(position);
        } else {
            notifyDataSetChanged();
        }
    }

    public void removeItem(int position) {
        if (position >= 0 && position < combinedList.size()) {
            Object objectToRemove = combinedList.get(position);
            if (objectToRemove instanceof Item) {
                Item itemToRemove = (Item) objectToRemove;
                if (!itemToRemove.isAddButton()) {
                    recentlyDeletedObject = itemToRemove;
                    recentlyDeletedPosition = position;
                    combinedList.remove(position);
                    notifyItemRemoved(position);
                    showUndoSnackbar();
                    Log.d(TAG, "removeItem: Item at position " + position + " removed visually. ID: " + itemToRemove.getId());
                }
            }
        }
    }

    private void showUndoSnackbar() {
        if (recyclerView == null) return;
        View rootView = recyclerView;
        Snackbar snackbar = Snackbar.make(rootView, "Item deleted", Snackbar.LENGTH_LONG);
        snackbar.setAction("Undo", v -> undoDelete());
        snackbar.addCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(@NonNull Snackbar transientBottomBar, int event) {
                if (event != DISMISS_EVENT_ACTION) {
                    Log.d(TAG, "Snackbar dismissed without Undo. Confirming delete.");
                    confirmDelete();
                } else {
                    Log.d(TAG, "Snackbar dismissed with Undo.");
                }
            }
        });
        snackbar.show();
    }

    private void undoDelete() {
        if (recentlyDeletedObject != null) {
            combinedList.add(recentlyDeletedPosition, recentlyDeletedObject);
            notifyItemInserted(recentlyDeletedPosition);
            if (recyclerView != null) {
                recyclerView.scrollToPosition(recentlyDeletedPosition);
            }
            recentlyDeletedObject = null;
            recentlyDeletedPosition = -1;
            Log.d(TAG, "undoDelete: Item restored visually.");
        }
    }

    private void confirmDelete() {
        if (recentlyDeletedObject instanceof Item) {
            Item itemToDelete = (Item) recentlyDeletedObject;
            if (deleteItemClickListener != null) {
                Log.d(TAG, "confirmDelete: Deleting MARKET with ID: " + itemToDelete.getId() + " from database.");
                deleteItemClickListener.onDeleteItemClick(itemToDelete.getId());
            } else if (deleteListItemClickListener != null) {
                Log.d(TAG, "confirmDelete: Deleting LIST ITEM with ID: " + itemToDelete.getId() + " from database (and image).");
                deleteListItemClickListener.onDeleteListItemClick(itemToDelete.getId());
            } else {
                Log.e(TAG, "confirmDelete: No delete listener available for item ID: " + itemToDelete.getId());
            }
        }
        recentlyDeletedObject = null;
        recentlyDeletedPosition = -1;
        Log.d(TAG, "confirmDelete: recentlyDeletedObject cleared.");
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
    }

    @Override
    public int getItemViewType(int position) {
        Object item = combinedList.get(position);
        if (item instanceof Item && ((Item) item).isAddButton()) {
            return VIEW_TYPE_ADD_BUTTON;
        } else if (item instanceof DateHeader) {
            return VIEW_TYPE_HEADER;
        } else {
            return VIEW_TYPE_ITEM;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_HEADER) {
            ItemDateHeaderBinding headerBinding = ItemDateHeaderBinding.inflate(inflater, parent, false);
            return new DateHeaderViewHolder(headerBinding);
        } else {
            MainItem1Binding binding = MainItem1Binding.inflate(inflater, parent, false);
            return new ItemViewHolder(binding, this);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == VIEW_TYPE_HEADER) {
            DateHeader header = (DateHeader) combinedList.get(position);
            ((DateHeaderViewHolder) holder).bind(header);
        } else {
            Item item = (Item) combinedList.get(position);
            ((ItemViewHolder) holder).bind(item, position, position == focusedPosition);
        }
    }

    @Override
    public int getItemCount() {
        return combinedList.size();
    }

    class ItemViewHolder extends RecyclerView.ViewHolder {
        private final MainItem1Binding binding;
        private AnimatorSet pulseAnimator;
        private ObjectAnimator jiggleAnimator;
        private boolean isLongPressing = false;
        private final ItemAdapter adapter;

        public ItemViewHolder(MainItem1Binding binding, ItemAdapter adapter) {
            super(binding.getRoot());
            this.binding = binding;
            this.adapter = adapter;

            binding.getRoot().setOnTouchListener((v, event) -> {
                int adapterPos = getAdapterPosition();
                if (adapterPos == RecyclerView.NO_POSITION) return false;
                Object currentItem = combinedList.get(adapterPos);
                if (!(currentItem instanceof Item)) return false;
                Item item = (Item) currentItem;

                if (!item.isAddButton()) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            if (!isLongPressing && focusedPosition == -1) {
                                startPulseAnimation();
                            }
                            break;
                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            if (!isLongPressing) {
                                stopPulseAnimation();
                            }
                            break;
                    }
                }
                return false;
            });

            binding.getRoot().setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Object currentItem = combinedList.get(position);
                    if (currentItem instanceof Item) {
                        Item item = (Item) currentItem;
                        if (item.isAddButton()) {
                            addItemClickListener.onAddItemClick();
                        } else if (focusedPosition != -1) {
                            setFocusedPosition(-1);
                        } else {
                            if (onItemClickListener != null) {
                                onItemClickListener.onItemClick(item.getId());
                            }
                        }
                    }
                }
            });

            binding.getRoot().setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Object currentItem = combinedList.get(position);
                    if (currentItem instanceof Item) {
                        Item item = (Item) currentItem;
                        if (!item.isAddButton()) {
                            isLongPressing = true;
                            setFocusedPosition(position);
                            showPopupMenu(v, item.getId(), position);
                            return true;
                        }
                    }
                }
                return false;
            });
        }

        public void bind(Item item, int position, boolean isFocused) {
            binding.getRoot().setVisibility(View.VISIBLE);

            if (item.isAddButton()) {
                binding.tvMainItemsCount.setText("+");
                binding.tvTitleItems.setText("Add Item");
            } else {
                binding.tvMainItemsCount.setText(String.valueOf(item.getCount()));
                binding.tvTitleItems.setText(item.getName());
            }

            if (!isFocused && !item.isAddButton()) {
                stopJiggleAnimation();
            }
            if (focusedPosition == -1 && pulseAnimator != null) {
                stopPulseAnimation();
            }

            if (isFocused) {
                startJiggleAnimation();
            } else {
                stopJiggleAnimation();
            }
        }

        private void startPulseAnimation() {
            if (pulseAnimator != null) {
                pulseAnimator.cancel();
            }
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(binding.getRoot(), "scaleX", 1f, 1.1f, 0.9f, 1f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(binding.getRoot(), "scaleY", 1f, 1.1f, 0.9f, 1f);
            pulseAnimator = new AnimatorSet();
            pulseAnimator.playTogether(scaleX, scaleY);
            pulseAnimator.setDuration(300);
            pulseAnimator.start();
        }

        private void stopPulseAnimation() {
            if (pulseAnimator != null) {
                pulseAnimator.cancel();
                binding.getRoot().setScaleX(1f);
                binding.getRoot().setScaleY(1f);
            }
        }

        private void startJiggleAnimation() {
            if (jiggleAnimator != null) {
                jiggleAnimator.cancel();
            }
            jiggleAnimator = ObjectAnimator.ofFloat(binding.getRoot(), "rotation", -5f, 5f);
            jiggleAnimator.setDuration(150);
            jiggleAnimator.setRepeatCount(ObjectAnimator.INFINITE);
            jiggleAnimator.setRepeatMode(ObjectAnimator.REVERSE);
            jiggleAnimator.start();
        }

        private void stopJiggleAnimation() {
            if (jiggleAnimator != null) {
                jiggleAnimator.cancel();
                binding.getRoot().setRotation(0f);
            }
        }

        private void showPopupMenu(View view, int itemIdToDelete, int itemPosition) {
            PopupMenu popup = new PopupMenu(view.getContext(), view);
            popup.getMenu().add("Delete");
            popup.setOnMenuItemClickListener(menuItem -> {
                if (menuItem.getTitle().equals("Delete")) {
                    Log.d(TAG, "PopupMenu: Delete clicked for item ID: " + itemIdToDelete + " at position " + itemPosition);
                    adapter.removeItem(itemPosition);
                    setFocusedPosition(-1);
                }
                return true;
            });
            popup.setOnDismissListener(menu -> {
                isLongPressing = false;
                setFocusedPosition(-1);
                Log.d(TAG, "PopupMenu dismissed.");
            });
            popup.show();
        }
    }

    class DateHeaderViewHolder extends RecyclerView.ViewHolder {
        private final ItemDateHeaderBinding binding;

        public DateHeaderViewHolder(@NonNull ItemDateHeaderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(DateHeader header) {
            binding.tvDateHeader.setText(header.getDate());
        }
    }
}