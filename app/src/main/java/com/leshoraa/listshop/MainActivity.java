package com.leshoraa.listshop;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.snackbar.Snackbar;
import com.leshoraa.listshop.adapter.GridSpacingItemDecoration;
import com.leshoraa.listshop.adapter.ItemAdapter;
import com.leshoraa.listshop.databinding.ActivityMainBinding;
import com.leshoraa.listshop.model.DatabaseHelper;
import com.leshoraa.listshop.model.Item;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements ItemAdapter.ItemAdapterListener {

    private static final String TAG = "MainActivity";

    private ActivityMainBinding binding;
    private ItemAdapter adapter;
    private DatabaseHelper dbHelper;
    public static final SimpleDateFormat DB_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    public static final SimpleDateFormat UI_DATE_FORMAT = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
    public static final SimpleDateFormat UI_DATE_FORMAT_DISPLAY = new SimpleDateFormat("dd/MM", Locale.getDefault());

    public static final String EXTRA_ITEM_ID = "extra_item_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(Color.WHITE);
        window.setNavigationBarColor(Color.WHITE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.getDecorView().setSystemUiVisibility(
                    window.getDecorView().getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (window.getDecorView().getWindowInsetsController() != null) {
                window.getDecorView().getWindowInsetsController().setSystemBarsAppearance(
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
                window.getDecorView().getWindowInsetsController().setSystemBarsAppearance(
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dbHelper = new DatabaseHelper(this);

        int numberOfColumns = calculateNumberOfColumns(120);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, numberOfColumns);
        binding.rv.setLayoutManager(gridLayoutManager);
        binding.rv.setHasFixedSize(true);
        binding.rv.addItemDecoration(new GridSpacingItemDecoration(numberOfColumns, 16));

        adapter = new ItemAdapter(new ArrayList<>(), this::addNewItem, this);
        adapter.setItemAdapterListener(this);
        binding.rv.setAdapter(adapter);

        adapter.setOnItemClickListener(itemId -> {
            Intent intent = new Intent(MainActivity.this, ListActivity.class);
            intent.putExtra(EXTRA_ITEM_ID, itemId);
            startActivity(intent);
        });

        setupFabClickListeners();

        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (position >= adapter.getItemCount()) return 1;
                int viewType = adapter.getItemViewType(position);
                if (viewType == ItemAdapter.VIEW_TYPE_HEADER) {
                    return numberOfColumns;
                }
                return 1;
            }
        });

        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT,
                0) {
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                int dragFlags = 0;
                if (viewHolder.getItemViewType() == ItemAdapter.VIEW_TYPE_ITEM && !isSelectionModeActive()) {
                    int position = viewHolder.getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        Object itemObject = adapter.getCombinedList().get(position);
                        if (itemObject instanceof Item) {
                            Item item = (Item) itemObject;
                            if (!item.isAddButton()) {
                                dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
                            }
                        }
                    }
                }
                return makeMovementFlags(dragFlags, 0);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                if (isSelectionModeActive()) return false;

                int fromPosition = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();

                if (viewHolder.getItemViewType() != ItemAdapter.VIEW_TYPE_ITEM ||
                        target.getItemViewType() != ItemAdapter.VIEW_TYPE_ITEM) {
                    return false;
                }

                Object fromObject = adapter.getCombinedList().get(fromPosition);
                Object toObject = adapter.getCombinedList().get(toPosition);

                if (((Item) fromObject).isAddButton() ||
                        ((Item) toObject).isAddButton()) {
                    return false;
                }

                adapter.onItemMove(fromPosition, toPosition);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {}

            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                super.onSelectedChanged(viewHolder, actionState);
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && viewHolder instanceof ItemAdapter.ItemViewHolder) {
                    ((ItemAdapter.ItemViewHolder) viewHolder).startJiggleAnimation();
                }
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                if (viewHolder instanceof ItemAdapter.ItemViewHolder) {
                    ((ItemAdapter.ItemViewHolder) viewHolder).stopJiggleAnimation();
                }
                adapter.saveItemOrderToDatabase();
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(binding.rv);

        loadItems();
        updateDateRangeDisplay();
    }

    private boolean isSelectionModeActive() {
        return binding.fabCancelSelection.getVisibility() == View.VISIBLE;
    }

    private void addNewItem() {
        String currentDbDate = DB_DATE_FORMAT.format(new Date());
        String defaultListName = "New List";
        Item newMainList = new Item(defaultListName, 0, false, currentDbDate);
        dbHelper.addMarket(newMainList);
        loadItems();
    }

    private void setupFabClickListeners() {
        binding.fabCancelSelection.setOnClickListener(v -> adapter.cancelSelectionMode());
        binding.fabDeleteSelection.setOnClickListener(v -> adapter.deleteSelectedItems());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null && isSelectionModeActive()) {
            adapter.cancelSelectionMode();
        }
        loadItems();
        updateDateRangeDisplay();
    }

    @Override
    public void onSelectionModeChanged(boolean isActive, int selectionCount) {
        if (isActive) {
            binding.fabDeleteSelection.setVisibility(View.VISIBLE);
            binding.fabCancelSelection.setVisibility(View.VISIBLE);
            binding.tvDeleteItem.setText("Delete (" + selectionCount + ")");
            binding.fabDeleteSelection.setEnabled(selectionCount > 0);
        } else {
            binding.fabDeleteSelection.setVisibility(View.GONE);
            binding.fabCancelSelection.setVisibility(View.GONE);
        }
    }

    @Override
    public void onItemDeleted(Item item, int position) {
        String message = "Item \"" + item.getName() + "\" deleted";
        Snackbar snackbar = Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG);
        snackbar.setAction("Undo", v -> {
            adapter.undoDelete(item, position);
            updateDateRangeDisplay();
        });
        snackbar.addCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar transientBottomBar, int event) {
                if (event != DISMISS_EVENT_ACTION) {
                    dbHelper.deleteMarket(item.getId());
                    loadItems();
                    updateDateRangeDisplay();
                }
            }
        });
        snackbar.show();
        updateDateRangeDisplay();
    }

    @Override
    public void onMultipleItemsDeleted(List<Item> items) {
        String message = items.size() + " items deleted";
        Snackbar snackbar = Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG);
        snackbar.setAction("Undo", v -> {
            loadItems();
            updateDateRangeDisplay();
        });
        snackbar.addCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar transientBottomBar, int event) {
                if (event != DISMISS_EVENT_ACTION) {
                    for (Item item : items) {
                        dbHelper.deleteMarket(item.getId());
                    }
                    loadItems();
                    updateDateRangeDisplay();
                }
            }
        });
        snackbar.show();
        updateDateRangeDisplay();
    }

    private void loadItems() {
        List<Item> marketLists = dbHelper.getMarkets();
        Log.d(TAG, "loadItems: Found " + marketLists.size() + " items after refresh.");
        adapter.setItems(marketLists);
    }

    private void updateDateRangeDisplay() {
        List<Item> marketLists = dbHelper.getMarkets();
        if (marketLists.isEmpty()) {
            binding.date.setText("");
            return;
        }

        Date minDate = null;
        Date maxDate = null;

        for (Item market : marketLists) {
            try {
                Date marketDate = DB_DATE_FORMAT.parse(market.getDate());
                if (minDate == null || marketDate.before(minDate)) {
                    minDate = marketDate;
                }
                if (maxDate == null || marketDate.after(maxDate)) {
                    maxDate = marketDate;
                }
            } catch (java.text.ParseException e) {
                e.printStackTrace();
            }
        }

        if (minDate != null) {
            String minDateStr = UI_DATE_FORMAT_DISPLAY.format(minDate);
            String maxDateStr = UI_DATE_FORMAT_DISPLAY.format(maxDate);
            String dateRange = minDateStr.equals(maxDateStr) ? minDateStr : minDateStr + " - " + maxDateStr;
            binding.date.setText(dateRange);
        } else {
            binding.date.setText("");
        }
    }

    private int calculateNumberOfColumns(int itemWidthDp) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        float screenWidthDp = displayMetrics.widthPixels / displayMetrics.density;
        return Math.max((int) (screenWidthDp / itemWidthDp), 3);
    }

    @Override
    public void onBackPressed() {
        if (isSelectionModeActive()) {
            adapter.cancelSelectionMode();
        } else {
            super.onBackPressed();
        }
    }
}