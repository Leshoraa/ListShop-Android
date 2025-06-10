package com.leshoraa.listshop;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;

import com.leshoraa.listshop.adapter.GridSpacingItemDecoration;
import com.leshoraa.listshop.adapter.ItemAdapter;
import com.leshoraa.listshop.model.DatabaseHelper;
import com.leshoraa.listshop.model.Item;
import com.leshoraa.listshop.databinding.ActivityMainBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Collections;
import java.util.Comparator;

public class MainActivity extends AppCompatActivity {

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
                    window.getDecorView().getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            );
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (window.getDecorView().getWindowInsetsController() != null) {
                window.getDecorView().getWindowInsetsController().setSystemBarsAppearance(
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                );
                window.getDecorView().getWindowInsetsController().setSystemBarsAppearance(
                        WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                );
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

        adapter = new ItemAdapter(new ArrayList<>(), null, null, this);
        binding.rv.setAdapter(adapter);

        adapter.addItemClickListener = () -> {
            String currentDbDate = DB_DATE_FORMAT.format(new Date());
            String defaultListName = "New List";

            Item newMainList = new Item(defaultListName, 0, false, currentDbDate);
            dbHelper.addMarket(newMainList);

            loadItems();
        };

        adapter.deleteItemClickListener = marketId -> {
            Log.d(TAG, "deleteItemClickListener (for Market): Deleting market from DB with ID: " + marketId);
            dbHelper.deleteMarket(marketId);
            Log.d(TAG, "deleteItemClickListener (for Market): Database delete attempt finished. Loading items...");
            loadItems();
            Log.d(TAG, "deleteItemClickListener (for Market): Items reloaded. Setting focused position to -1.");
            adapter.setFocusedPosition(-1);
        };

        adapter.setOnDeleteListItemClickListener(itemId -> {
            Log.d(TAG, "onDeleteListItemClick (for Item): Deleting item from DB with ID: " + itemId);
            dbHelper.deleteItem(itemId);
        });


        adapter.setOnItemClickListener(itemId -> {
            Intent intent = new Intent(MainActivity.this, ListActivity.class);
            intent.putExtra(EXTRA_ITEM_ID, itemId);
            startActivity(intent);
        });

        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (adapter.getItemViewType(position) == ItemAdapter.VIEW_TYPE_HEADER) {
                    return numberOfColumns;
                }
                return 1;
            }
        });

        loadItems();
        updateDateRangeDisplay();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadItems();
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

        if (minDate != null && maxDate != null) {
            String minDateStr = UI_DATE_FORMAT_DISPLAY.format(minDate);
            String maxDateStr = UI_DATE_FORMAT_DISPLAY.format(maxDate);
            binding.date.setText(minDateStr + " - " + maxDateStr);
        } else {
            binding.date.setText("");
        }
    }

    private int calculateNumberOfColumns(int itemWidthDp) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        float screenWidthDp = displayMetrics.widthPixels / displayMetrics.density;
        int numberOfColumns = (int) (screenWidthDp / itemWidthDp);
        return Math.max(numberOfColumns, 3);
    }
}