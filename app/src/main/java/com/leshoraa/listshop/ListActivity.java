package com.leshoraa.listshop;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.PopupMenu;
import android.widget.Toast;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.AutoTransition;
import androidx.transition.Slide;
import androidx.transition.TransitionManager;
import androidx.transition.TransitionSet;

import com.leshoraa.listshop.adapter.ItemListAdapter;
import com.leshoraa.listshop.adapter.ShopListAdapter;
import com.leshoraa.listshop.databinding.ActivityListBinding;
import com.leshoraa.listshop.model.DatabaseHelper;
import com.leshoraa.listshop.model.Item;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ListActivity extends AppCompatActivity {

    private ActivityListBinding binding;
    private DatabaseHelper dbHelper;
    private ItemListAdapter itemListAdapter;
    private ShopListAdapter shopListAdapter;
    private List<Item> itemsList;
    private List<Item> todoList;
    public static final String EXTRA_ITEM_ID = "com.leshoraa.listshop.ITEM_ID";
    public static final String EXTRA_SELECTED_ITEM_ID = "com.leshoraa.listshop.SELECTED_ITEM_ID";
    private int currentParentListId;
    private DecimalFormat decimalFormat;
    private boolean isTodoVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dbHelper = new DatabaseHelper(this);

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        decimalFormat = new DecimalFormat("#,##0", symbols);

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

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        currentParentListId = -1;
        if (getIntent() != null && getIntent().hasExtra(MainActivity.EXTRA_ITEM_ID)) {
            currentParentListId = getIntent().getIntExtra(MainActivity.EXTRA_ITEM_ID, -1);
            if (currentParentListId != -1) {
                loadMarketName(currentParentListId);
            } else {
                Toast.makeText(this, "Invalid market ID received.", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            Toast.makeText(this, "No market ID received.", Toast.LENGTH_SHORT).show();
            finish();
        }

        setupRecyclerView();
        setupTodoRecyclerView();
        loadItems();
        loadTodoItems();

        binding.marketName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (currentParentListId != -1) {
                    String newMarketName = s.toString().trim();
                    dbHelper.updateMarketName(currentParentListId, newMarketName);
                }
            }
        });

        binding.btnAddItem.setOnClickListener(v -> {
            Intent intent = new Intent(ListActivity.this, AddItemActivity.class);
            intent.putExtra(EXTRA_ITEM_ID, currentParentListId);
            startActivity(intent);
        });

        binding.back.setOnClickListener(v -> onBackPressed());

        binding.todo.setOnClickListener(v -> toggleTodoVisibility());

        binding.tvAddListBtn.setOnClickListener(v -> addTodoItemToList());

        binding.edtTodo.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addTodoItemToList();
                return true;
            }
            return false;
        });

        binding.tvDropdownMenu.setOnClickListener(this::showPopupMenu);
    }

    private void loadMarketName(int marketId) {
        Item market = dbHelper.getMarketById(marketId);
        if (market != null) {
            binding.marketName.setText(market.getName());
            binding.marketName.setSelection(binding.marketName.getText().length());
        } else {
            binding.marketName.setText("My Cart");
        }
    }

    private void setupRecyclerView() {
        itemsList = new ArrayList<>();
        itemListAdapter = new ItemListAdapter(this, itemsList);
        binding.rvList.setLayoutManager(new LinearLayoutManager(this));
        binding.rvList.setAdapter(itemListAdapter);

        itemListAdapter.setOnItemDeleteListener(itemId -> {
            dbHelper.deleteItem(itemId);
            loadItems();
        });

        itemListAdapter.setOnItemClickListener(position -> {
            Item selectedItem = itemsList.get(position);
            Intent intent = new Intent(ListActivity.this, PreviewItemActivity.class);
            intent.putExtra(EXTRA_SELECTED_ITEM_ID, selectedItem.getId());
            startActivity(intent);
        });

        itemListAdapter.setOnItemQuantityChangeListener((itemId, newQuantity) -> {
            loadItems();
        });
    }

    private void setupTodoRecyclerView() {
        todoList = new ArrayList<>();
        shopListAdapter = new ShopListAdapter(this, todoList);
        binding.rvTodo.setLayoutManager(new LinearLayoutManager(this));
        binding.rvTodo.setAdapter(shopListAdapter);

        shopListAdapter.setOnTodoItemActionListener(new ShopListAdapter.OnTodoItemActionListener() {
            @Override
            public void onTodoItemChecked(int position, boolean isChecked) {
                if (position != RecyclerView.NO_POSITION && position < todoList.size()) {
                    Item todoItem = todoList.get(position);
                    todoItem.setAddButton(isChecked);
                    dbHelper.updateTodoItem(todoItem);
                }
            }

            @Override
            public void onTodoItemNameChanged(int position, String newName) {
                if (position != RecyclerView.NO_POSITION && position < todoList.size()) {
                    Item todoItem = todoList.get(position);
                    todoItem.setName(newName);
                    dbHelper.updateTodoItem(todoItem);
                }
            }

            @Override
            public void onTodoItemDeleted(int itemId) {
                dbHelper.deleteTodoItem(itemId);
                loadTodoItems();
                Log.d("ListActivity", "Todo item with ID: " + itemId + " permanently deleted from DB. Reloading todo list.");
            }
        });
    }

    private void loadItems() {
        if (currentParentListId != -1) {
            itemsList.clear();
            itemsList.addAll(dbHelper.getItemsByParentListId(currentParentListId));
            itemListAdapter.notifyDataSetChanged();
            updateTotalPrice();
        }
    }

    private void loadTodoItems() {
        if (currentParentListId != -1) {
            todoList.clear();
            todoList.addAll(dbHelper.getTodoItemsByParentListId(currentParentListId));
            shopListAdapter.notifyDataSetChanged();
            updateShopListCount();
        }
    }

    private void updateTotalPrice() {
        double total = 0.0;
        for (Item item : itemsList) {
            total += item.getFinalPrice();
        }
        binding.tvTotal.setText(decimalFormat.format(total));
    }

    private void addTodoItemToList() {
        String todoName = binding.edtTodo.getText().toString().trim();
        if (!todoName.isEmpty()) {
            Item newTodoItem = new Item(0, todoName, 0, false, null);
            newTodoItem.setParentListId(currentParentListId);
            long newTodoId = dbHelper.addTodoItem(newTodoItem);
            if (newTodoId != -1) {
                newTodoItem.setId((int) newTodoId);
                loadTodoItems();
                binding.edtTodo.setText("");
            } else {
                Toast.makeText(ListActivity.this, "Failed to add todo item.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(ListActivity.this, "Todo item name cannot be empty.", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateShopListCount() {
        int count = todoList.size();
        binding.tvShoplistCount.setText(String.valueOf(count));
        //binding.flShoplistCount.setVisibility(count > 0 ? View.VISIBLE : View.GONE); // Hide if count is 0
    }


    private void toggleTodoVisibility() {
        TransitionManager.beginDelayedTransition(binding.getRoot(), new TransitionSet()
                .addTransition(new Slide(Gravity.TOP).addTarget(binding.cvShoplist))
                .setDuration(350)
                .addTransition(new AutoTransition().addTarget(binding.cv)));

        isTodoVisible = !isTodoVisible;
        if (isTodoVisible) {
            binding.cvShoplist.setVisibility(View.VISIBLE);
            TransitionManager.beginDelayedTransition(binding.getRoot(), new TransitionSet()
                    .addTransition(new AutoTransition().addTarget(binding.cv))
                    .setDuration(1000));
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) binding.cv.getLayoutParams();
            params.topToBottom = binding.cvShoplist.getId();
            binding.cv.setLayoutParams(params);

            ObjectAnimator animator = ObjectAnimator.ofFloat(binding.cv, "translationY", -92f);
            animator.setDuration(350);
            animator.start();

        } else {
            binding.cvShoplist.setVisibility(View.GONE);
            TransitionManager.beginDelayedTransition(binding.getRoot(), new TransitionSet()
                    .addTransition(new AutoTransition().addTarget(binding.cv))
                    .setDuration(1000));
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) binding.cv.getLayoutParams();
            params.topToBottom = binding.constraintLayout3.getId();
            binding.cv.setLayoutParams(params);

            ObjectAnimator animator = ObjectAnimator.ofFloat(binding.cv, "translationY", 0f);
            animator.setDuration(350);
            animator.start();
        }
    }

    private void showPopupMenu(View view) {
        PopupMenu popup = new PopupMenu(ListActivity.this, view);
        popup.getMenu().add("Delete All Shopping List");
        popup.setOnMenuItemClickListener(item -> {
            if (item.getTitle().equals("Delete All Shopping List")) {
                deleteAllTodoItems();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void deleteAllTodoItems() {
        if (currentParentListId != -1) {
            List<Item> itemsToDelete = dbHelper.getTodoItemsByParentListId(currentParentListId);
            for (Item todoItem : itemsToDelete) {
                dbHelper.deleteTodoItem(todoItem.getId());
            }
            todoList.clear();
            shopListAdapter.notifyDataSetChanged();
            updateShopListCount();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadItems();
        loadTodoItems();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbHelper.close();
    }
}