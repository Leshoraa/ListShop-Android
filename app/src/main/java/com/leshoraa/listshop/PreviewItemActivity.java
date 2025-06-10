package com.leshoraa.listshop;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.leshoraa.listshop.databinding.ActivityPreviewItemBinding;
import com.leshoraa.listshop.model.DatabaseHelper;
import com.leshoraa.listshop.model.Item;
import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PreviewItemActivity extends AppCompatActivity {

    private ActivityPreviewItemBinding binding;
    private DatabaseHelper dbHelper;
    private int selectedItemId = -1;
    private List<Double> discounts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityPreviewItemBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dbHelper = new DatabaseHelper(this);

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

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

        if (getIntent() != null && getIntent().hasExtra(ListActivity.EXTRA_SELECTED_ITEM_ID)) {
            selectedItemId = getIntent().getIntExtra(ListActivity.EXTRA_SELECTED_ITEM_ID, -1);
            if (selectedItemId != -1) {
                loadItemData(selectedItemId);
            } else {
                Toast.makeText(this, "Item ID not found.", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            Toast.makeText(this, "No Item ID passed.", Toast.LENGTH_SHORT).show();
            finish();
        }

        binding.back.setOnClickListener(v -> onBackPressed());
        binding.btnSaveItem.setOnClickListener(v -> saveItemChanges());
    }

    @SuppressLint("SetTextI18n")
    private void loadItemData(int itemId) {
        Item item = dbHelper.getItemById(itemId);
        if (item != null) {
            binding.edtTitle.setText(item.getName());
            binding.edtDesc.setText(item.getDescription());
            binding.edtCategory.setText(item.getCategory());

            discounts = new ArrayList<>();
            if (item.getDiscountsJson() != null && !item.getDiscountsJson().isEmpty()) {
                Gson gson = new Gson();
                Type type = new TypeToken<List<Double>>() {}.getType();
                discounts.addAll(gson.fromJson(item.getDiscountsJson(), type));
            }

            double totalDiscountPercentage = 0.0;
            for (Double discount : discounts) {
                totalDiscountPercentage += discount;
            }
            if (totalDiscountPercentage > 100.0) {
                totalDiscountPercentage = 100.0;
            }
            binding.tvDiscount.setText(String.format(Locale.getDefault(), "%.2f%%", totalDiscountPercentage));

            binding.edtPrice.setText(String.valueOf(item.getPrice()));

            if (item.getImageData() != null && !item.getImageData().isEmpty()) {
                File imgFile = new File(getFilesDir(), item.getImageData());
                if (imgFile.exists()) {
                    Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                    binding.imageView.setImageBitmap(myBitmap);
                    binding.imageView.setVisibility(android.view.View.VISIBLE);
                } else {
                    binding.imageView.setVisibility(android.view.View.GONE);
                }
            } else {
                binding.imageView.setVisibility(android.view.View.GONE);
            }
        } else {
            Toast.makeText(this, "Failed to load item data.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void saveItemChanges() {
        Item item = dbHelper.getItemById(selectedItemId);
        if (item != null) {
            item.setName(binding.edtTitle.getText().toString());
            item.setDescription(binding.edtDesc.getText().toString());
            item.setCategory(binding.edtCategory.getText().toString());
            try {
                item.setPrice(Double.parseDouble(binding.edtPrice.getText().toString()));
            } catch (NumberFormatException e) {
                item.setPrice(0.0);
            }

            Gson gson = new Gson();
            item.setDiscountsJson(gson.toJson(discounts));

            double finalPrice = item.getPrice();
            for (Double discount : discounts) {
                finalPrice -= (finalPrice * discount / 100);
            }
            item.setFinalPrice(finalPrice);

            double totalDiscountSum = 0.0;
            for (Double discount : discounts) {
                totalDiscountSum += discount;
            }
            if (totalDiscountSum > 100.0) {
                totalDiscountSum = 100.0;
            }
            item.setTotalDiscountPercentage(totalDiscountSum);

            int rowsAffected = dbHelper.updateItem(item);
            if (rowsAffected > 0) {
                Toast.makeText(this, "Item updated successfully!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Failed to update item.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbHelper.close();
    }
}