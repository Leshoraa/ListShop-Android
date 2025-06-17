package com.leshoraa.listshop.adapter;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
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
import java.util.Comparator; // Import Comparator here

public class ItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "ItemAdapter";

    public static final int VIEW_TYPE_ITEM = 0;
    public static final int VIEW_TYPE_HEADER = 1;
    public static final int VIEW_TYPE_ADD_BUTTON = 2;

    private List<Object> combinedList;
    public OnAddItemClickListener addItemClickListener;
    public OnDeleteItemClickListener deleteItemClickListener;
    private OnItemClickListener onItemClickListener;

    private DatabaseHelper dbHelper;
    private Object recentlyDeletedObject;
    private int recentlyDeletedPosition;
    private RecyclerView recyclerView;
    private ItemTouchHelper itemTouchHelper;

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
                       OnDeleteItemClickListener deleteListener, Context context, ItemTouchHelper itemTouchHelper) {
        this.addItemClickListener = addListener;
        this.deleteItemClickListener = deleteListener;
        this.combinedList = new ArrayList<>();
        this.dbHelper = new DatabaseHelper(context);
        this.itemTouchHelper = itemTouchHelper;
        setItems(initialItemList);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    public List<Object> getCombinedList() {
        return combinedList;
    }

    public void setItems(List<Item> rawItemList) {
        Item addButtonInList = null;
        List<Item> actualItemsFromDb = new ArrayList<>();
        for (Item item : rawItemList) {
            if (item.isAddButton()) {
                addButtonInList = item;
            } else {
                actualItemsFromDb.add(item);
            }
        }

        // Urutkan item berdasarkan properti 'order' (urutan global)
        // Ini akan mengurutkan *pasar* berdasarkan urutan yang tersimpan.
        Collections.sort(actualItemsFromDb, Comparator.comparingInt(Item::getOrder));

        List<Object> tempCombinedList = new ArrayList<>();
        String currentDbDate = MainActivity.DB_DATE_FORMAT.format(new Date());
        String currentUiDate = MainActivity.UI_DATE_FORMAT.format(new Date());

        // Tambahkan Header untuk tanggal hari ini (Jika ini berlaku untuk pasar, jika tidak, hapus)
        // Perhatikan bahwa header tanggal mungkin tidak relevan jika Anda hanya menampilkan daftar pasar.
        // Jika Anda ingin mengelompokkan pasar berdasarkan tanggal pembuatannya, maka biarkan ini.
        tempCombinedList.add(new DateHeader(currentUiDate));

        // Tambahkan tombol "Add Item"
        if (addButtonInList == null) {
            addButtonInList = new Item("Add Item", 0, true, currentDbDate);
        }
        tempCombinedList.add(addButtonInList);

        String lastProcessedDate = currentDbDate;

        for (Item item : actualItemsFromDb) {
            try {
                String itemDate = item.getDate();
                if (!itemDate.equals(lastProcessedDate)) {
                    if (!itemDate.equals(currentDbDate)) {
                        String uiDate = MainActivity.UI_DATE_FORMAT.format(MainActivity.DB_DATE_FORMAT.parse(itemDate));
                        tempCombinedList.add(new DateHeader(uiDate));
                    }
                    lastProcessedDate = itemDate;
                }
                tempCombinedList.add(item);
            } catch (ParseException e) {
                e.printStackTrace();
                tempCombinedList.add(item);
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

    public void setItemTouchHelper(ItemTouchHelper itemTouchHelper) {
        this.itemTouchHelper = itemTouchHelper;
    }

    public void onItemMove(int fromPosition, int toPosition) {
        if (fromPosition < combinedList.size() && toPosition < combinedList.size()) {
            Object movedObject = combinedList.get(fromPosition);
            Object targetObject = combinedList.get(toPosition);

            // Only allow moves for actual items (not headers or the add button)
            if (!(movedObject instanceof Item && !((Item) movedObject).isAddButton()) ||
                    !(targetObject instanceof Item && !((Item) targetObject).isAddButton())) {
                Log.d(TAG, "onItemMove: Invalid move target (header or add button)");
                return;
            }

            Collections.swap(combinedList, fromPosition, toPosition); // Use Collections.swap for direct swap
            notifyItemMoved(fromPosition, toPosition);

            // Save the new order immediately after a visual move
            saveItemOrderToDatabase();
        }
    }

    public void saveItemOrderToDatabase() {
        Log.d(TAG, "Saving new market order to database...");
        int currentOrder = 0; // Initialize order counter
        for (int i = 0; i < combinedList.size(); i++) {
            Object obj = combinedList.get(i);
            if (obj instanceof Item && !((Item) obj).isAddButton()) {
                Item market = (Item) obj;
                // Assign the new order based on the current position in the combinedList
                market.setOrder(currentOrder);
                // Now, call updateMarketOrder with id and the new order
                dbHelper.updateMarketOrder(market.getId(), market.getOrder());
                Log.d(TAG, "Updating market ID: " + market.getId() + " to order: " + market.getOrder());
                currentOrder++; // Increment for the next item
            }
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
            // After undo, the order in the database might be stale for this item.
            // It's good practice to re-save the order of all visible items.
            saveItemOrderToDatabase();
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
            return new ItemViewHolder(binding, this, itemTouchHelper);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == VIEW_TYPE_HEADER) {
            DateHeader header = (DateHeader) combinedList.get(position);
            ((DateHeaderViewHolder) holder).bind(header);
        } else {
            Item item = (Item) combinedList.get(position);
            ((ItemViewHolder) holder).bind(item, position);
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
        private final ItemTouchHelper itemTouchHelper;

        public ItemViewHolder(MainItem1Binding binding, ItemAdapter adapter, ItemTouchHelper itemTouchHelper) {
            super(binding.getRoot());
            this.binding = binding;
            this.adapter = adapter;
            this.itemTouchHelper = itemTouchHelper;

            binding.getRoot().setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Object currentItem = combinedList.get(position);
                    if (currentItem instanceof Item) {
                        Item item = (Item) currentItem;
                        if (item.isAddButton()) {
                            addItemClickListener.onAddItemClick();
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
                    if (currentItem instanceof Item && !((Item) currentItem).isAddButton()) {
                        showItemPopupMenu(v, position, (Item) currentItem);
                        return true;
                    }
                }
                return false;
            });
        }

        private void showItemPopupMenu(View view, int position, Item item) {
            PopupMenu popup = new PopupMenu(view.getContext(), view);
            popup.getMenu().add(0, 0, 0, "Delete Item");

            popup.setOnMenuItemClickListener(menuItem -> {
                if (menuItem.getTitle().equals("Delete Item")) {
                    adapter.removeItem(position);
                    return true;
                }
                return false;
            });

            popup.setOnDismissListener(menu -> {
                stopJiggleAnimation();
            });

            startJiggleAnimation();
            popup.show();
        }

        public void bind(Item item, int position) {
            binding.getRoot().setVisibility(View.VISIBLE);

            if (item.isAddButton()) {
                binding.tvMainItemsCount.setText("+");
                binding.tvTitleItems.setText("Add Item");
            } else {
                binding.tvMainItemsCount.setText(String.valueOf(item.getCount()));
                binding.tvTitleItems.setText(item.getName());
            }
        }

        public void startJiggleAnimation() {
            if (jiggleAnimator != null) {
                jiggleAnimator.cancel();
            }
            jiggleAnimator = ObjectAnimator.ofFloat(binding.getRoot(), "rotation", -5f, 5f);
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
            binding.tvDateHeader.setText(header.getDate());
        }
    }
}