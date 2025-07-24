package com.leshoraa.listshop.adapter;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.leshoraa.listshop.MainActivity;
import com.leshoraa.listshop.R;
import com.leshoraa.listshop.databinding.ItemDateHeaderBinding;
import com.leshoraa.listshop.databinding.MainItem1Binding;
import com.leshoraa.listshop.model.DatabaseHelper;
import com.leshoraa.listshop.model.DateHeader;
import com.leshoraa.listshop.model.Item;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "ItemAdapter";

    public static final int VIEW_TYPE_ITEM = 0;
    public static final int VIEW_TYPE_HEADER = 1;
    public static final int VIEW_TYPE_ADD_BUTTON = 2;

    private List<Object> combinedList;
    private final List<Item> selectedItems = new ArrayList<>();
    private boolean isSelectionModeActive = false;

    public OnAddItemClickListener addItemClickListener;
    private OnItemClickListener onItemClickListener;
    private ItemAdapterListener itemAdapterListener;

    private final DatabaseHelper dbHelper;

    public interface ItemAdapterListener {
        void onSelectionModeChanged(boolean isActive, int selectionCount);
        void onItemDeleted(Item item, int position);
        void onMultipleItemsDeleted(List<Item> items);
    }

    public void setItemAdapterListener(ItemAdapterListener listener) {
        this.itemAdapterListener = listener;
    }

    public interface OnAddItemClickListener {
        void onAddItemClick();
    }

    public interface OnItemClickListener {
        void onItemClick(int itemId);
    }

    public ItemAdapter(List<Item> initialItemList, OnAddItemClickListener addListener, Context context) {
        this.addItemClickListener = addListener;
        this.combinedList = new ArrayList<>();
        this.dbHelper = new DatabaseHelper(context);
        setItems(initialItemList);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    public List<Object> getCombinedList() {
        return combinedList;
    }

    public void setItems(List<Item> rawItemList) {
        this.combinedList.clear();
        Map<String, List<Item>> itemsGroupedByDate = new LinkedHashMap<>();

        rawItemList.sort((o1, o2) -> Integer.compare(o1.getOrder(), o2.getOrder()));

        for (Item item : rawItemList) {
            itemsGroupedByDate.computeIfAbsent(item.getDate(), k -> new ArrayList<>()).add(item);
        }

        String currentDbDate = MainActivity.DB_DATE_FORMAT.format(new Date());

        try {
            Date today = MainActivity.DB_DATE_FORMAT.parse(currentDbDate);
            String currentUiDate = MainActivity.UI_DATE_FORMAT.format(Objects.requireNonNull(today));
            this.combinedList.add(new DateHeader(currentUiDate, currentDbDate));
        } catch (ParseException e) {
            this.combinedList.add(new DateHeader("Today", currentDbDate));
        }

        this.combinedList.add(new Item("Add Item", 0, true, currentDbDate));

        if (itemsGroupedByDate.containsKey(currentDbDate)) {
            this.combinedList.addAll(Objects.requireNonNull(itemsGroupedByDate.get(currentDbDate)));
            itemsGroupedByDate.remove(currentDbDate);
        }

        for (Map.Entry<String, List<Item>> entry : itemsGroupedByDate.entrySet()) {
            String dbDate = entry.getKey();
            List<Item> itemsForDate = entry.getValue();
            try {
                Date date = MainActivity.DB_DATE_FORMAT.parse(dbDate);
                String uiDate = MainActivity.UI_DATE_FORMAT.format(Objects.requireNonNull(date));
                this.combinedList.add(new DateHeader(uiDate, dbDate));
                this.combinedList.addAll(itemsForDate);
            } catch (ParseException e) {
                Log.e(TAG, "Error parsing date: " + dbDate, e);
            }
        }
        notifyDataSetChanged();
    }

    public void enterSelectionMode(int position) {
        if (isSelectionModeActive) return;
        isSelectionModeActive = true;
        toggleSelection(position);
        if (itemAdapterListener != null) {
            itemAdapterListener.onSelectionModeChanged(true, selectedItems.size());
        }
    }

    public void cancelSelectionMode() {
        isSelectionModeActive = false;
        selectedItems.clear();
        if (itemAdapterListener != null) {
            itemAdapterListener.onSelectionModeChanged(false, 0);
        }
        notifyDataSetChanged();
    }

    public void deleteSelectedItems() {
        if (itemAdapterListener != null) {
            itemAdapterListener.onMultipleItemsDeleted(new ArrayList<>(selectedItems));
        }
        combinedList.removeAll(selectedItems);
        cancelSelectionMode();
    }

    public void undoDelete(Item item, int position) {
        if (item != null && position >= 0 && position <= combinedList.size()) {
            combinedList.add(position, item);
            notifyItemInserted(position);
        }
    }

    private void toggleSelection(int position) {
        if (position < 0 || position >= combinedList.size()) return;
        Object obj = combinedList.get(position);
        if (!(obj instanceof Item) || ((Item) obj).isAddButton()) return;

        Item item = (Item) obj;
        if (selectedItems.contains(item)) {
            selectedItems.remove(item);
        } else {
            selectedItems.add(item);
        }
        notifyItemChanged(position);

        if (itemAdapterListener != null) {
            itemAdapterListener.onSelectionModeChanged(true, selectedItems.size());
        }

        if (selectedItems.isEmpty()) {
            cancelSelectionMode();
        }
    }

    public void onItemMove(int fromPosition, int toPosition) {
        if (fromPosition < combinedList.size() && toPosition < combinedList.size()) {
            Object fromObject = combinedList.get(fromPosition);
            Object toObject = combinedList.get(toPosition);

            if (!(fromObject instanceof Item && !((Item) fromObject).isAddButton()) ||
                    !(toObject instanceof Item && !((Item) toObject).isAddButton())) {
                return;
            }

            final Item movedItem = (Item) combinedList.remove(fromPosition);
            combinedList.add(toPosition, movedItem);

            String newDbDate = movedItem.getDate();
            for (int i = toPosition; i >= 0; i--) {
                if (combinedList.get(i) instanceof DateHeader) {
                    newDbDate = ((DateHeader) combinedList.get(i)).getDbDate();
                    break;
                }
            }
            movedItem.setDate(newDbDate);
            notifyItemMoved(fromPosition, toPosition);
        }
    }

    public void saveItemOrderToDatabase() {
        Log.d(TAG, "Saving new item order and dates to database...");
        int currentOrder = 0;
        for (Object obj : combinedList) {
            if (obj instanceof Item && !((Item) obj).isAddButton()) {
                Item item = (Item) obj;
                item.setOrder(currentOrder);
                dbHelper.updateMarketOrderAndDate(item.getId(), item.getOrder(), item.getDate());
                Log.d(TAG, "Updating item ID: " + item.getId() + " to order: " + item.getOrder() + " and date: " + item.getDate());
                currentOrder++;
            }
        }
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
        int viewType = getItemViewType(position);
        if (viewType == VIEW_TYPE_HEADER) {
            DateHeader header = (DateHeader) combinedList.get(position);
            ((DateHeaderViewHolder) holder).bind(header);
        } else {
            Item item = (Item) combinedList.get(position);
            ((ItemViewHolder) holder).bind(item);
        }
    }

    @Override
    public int getItemCount() {
        return combinedList.size();
    }

    public class ItemViewHolder extends RecyclerView.ViewHolder {
        private final MainItem1Binding binding;
        private ObjectAnimator jiggleAnimator;
        private final ItemAdapter adapter;

        public ItemViewHolder(MainItem1Binding binding, ItemAdapter adapter) {
            super(binding.getRoot());
            this.binding = binding;
            this.adapter = adapter;

            binding.getRoot().setOnClickListener(v -> {
                int clickedPosition = getAdapterPosition();
                if (clickedPosition == RecyclerView.NO_POSITION) return;

                if (adapter.isSelectionModeActive) {
                    adapter.toggleSelection(clickedPosition);
                } else {
                    Object currentItem = combinedList.get(clickedPosition);
                    if (currentItem instanceof Item) {
                        Item item = (Item) currentItem;
                        if (item.isAddButton()) {
                            if (addItemClickListener != null) addItemClickListener.onAddItemClick();
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
                if (position != RecyclerView.NO_POSITION && !adapter.isSelectionModeActive) {
                    Object currentItem = combinedList.get(position);
                    if (currentItem instanceof Item && !((Item) currentItem).isAddButton()) {
                        showItemPopupMenu(v, position);
                        return true;
                    }
                }
                return false;
            });
        }

        private void showItemPopupMenu(View view, int position) {
            PopupMenu popup = new PopupMenu(view.getContext(), view);
            popup.getMenu().add(0, 1, 0, "Select");
            popup.getMenu().add(0, 2, 1, "Delete");

            popup.setOnMenuItemClickListener(menuItem -> {
                int itemId = menuItem.getItemId();
                if (itemId == 1) {
                    adapter.enterSelectionMode(position);
                    return true;
                } else if (itemId == 2) {
                    int currentPosition = getAdapterPosition();
                    if (currentPosition != RecyclerView.NO_POSITION) {
                        Item itemToDelete = (Item) combinedList.get(currentPosition);
                        if (itemAdapterListener != null) {
                            itemAdapterListener.onItemDeleted(itemToDelete, currentPosition);
                        }
                        combinedList.remove(currentPosition);
                        notifyItemRemoved(currentPosition);
                    }
                    return true;
                }
                return false;
            });

            popup.setOnDismissListener(menu -> stopJiggleAnimation());
            startJiggleAnimation();
            popup.show();
        }

        public void bind(Item item) {
            binding.getRoot().setVisibility(View.VISIBLE);

            if (item.isAddButton()) {
                binding.tvMainItemsCount.setText("+");
                binding.tvTitleItems.setText("Add Item");
            } else {
                binding.tvMainItemsCount.setText(String.valueOf(item.getCount()));
                binding.tvTitleItems.setText(item.getName());

                if (isSelectionModeActive && selectedItems.contains(item)) {
                    binding.cvMainList.setCardBackgroundColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.colorPrimaryVariant));
                } else {
                    binding.cvMainList.setCardBackgroundColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.colorOnCard));
                }
            }
        }

        public void startJiggleAnimation() {
            stopJiggleAnimation();
            jiggleAnimator = ObjectAnimator.ofFloat(binding.getRoot(), "rotation", -2f, 2f);
            jiggleAnimator.setDuration(150);
            jiggleAnimator.setRepeatCount(ObjectAnimator.INFINITE);
            jiggleAnimator.setRepeatMode(ObjectAnimator.REVERSE);
            jiggleAnimator.start();
        }

        public void stopJiggleAnimation() {
            if (jiggleAnimator != null) {
                jiggleAnimator.cancel();
                binding.getRoot().setRotation(0f);
            }
        }
    }

    class DateHeaderViewHolder extends RecyclerView.ViewHolder {
        private final ItemDateHeaderBinding binding;

        public DateHeaderViewHolder(@NonNull ItemDateHeaderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(DateHeader header) {
            binding.tvDateHeader.setText(header.getUiDate());
        }
    }
}