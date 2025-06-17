package com.leshoraa.listshop;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
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
import androidx.recyclerview.widget.GridLayoutManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.leshoraa.listshop.adapter.DiscountAdapter;
import com.leshoraa.listshop.databinding.ActivityPreviewItemBinding;
import com.leshoraa.listshop.model.DatabaseHelper;
import com.leshoraa.listshop.model.Item;
import java.io.File;
import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PreviewItemActivity extends AppCompatActivity {

    private ActivityPreviewItemBinding binding;
    private DatabaseHelper dbHelper;
    private int selectedItemId = -1;
    private List<Double> discounts;
    private DiscountAdapter discountAdapter;
    private DecimalFormat decimalFormat;
    private String currentPriceString = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityPreviewItemBinding.inflate(getLayoutInflater());
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

        discounts = new ArrayList<>();
        discountAdapter = new DiscountAdapter(new ArrayList<>());
        binding.rvEdtDiscount.setAdapter(discountAdapter);
        int numberOfDiscountColumns = calculateDiscountColumns(150);
        GridLayoutManager layoutManager = new GridLayoutManager(this, numberOfDiscountColumns);
        binding.rvEdtDiscount.setLayoutManager(layoutManager);

        discountAdapter.setOnItemDeleteListener(position -> {
            if (position < discounts.size()) {
                discounts.remove(position);
                updateDiscountAdapterData();
                enforceMaxDiscount();
            }
        });

        discountAdapter.setOnEditTextChangeListener((position, text) -> {
            try {
                String normalized = text.replace('.', ',');
                DecimalFormatSymbols symbols1 = new DecimalFormatSymbols(Locale.getDefault());
                symbols1.setDecimalSeparator(',');
                DecimalFormat inputDecimalFormat = new DecimalFormat("#,##0.##", symbols1);
                Number parsedNumber = inputDecimalFormat.parse(normalized.isEmpty() ? "0" : normalized);
                double newDiscount = parsedNumber.doubleValue();

                if (position < discounts.size()) {
                    discounts.set(position, newDiscount);
                    calculateAndDisplayTotalDiscount();
                    enforceMaxDiscount();
                }
            } catch (ParseException e) {
                if (position < discounts.size()) {
                    discounts.set(position, 0.0);
                    calculateAndDisplayTotalDiscount();
                    enforceMaxDiscount();
                }
            }
        });

        discountAdapter.setOnAddButtonClickListener(() -> {
            if (!discounts.isEmpty()) {
                Double lastDiscountValue = discounts.get(discounts.size() - 1);
                if (lastDiscountValue == null || lastDiscountValue == 0.0) {
                    Toast.makeText(this, "Please fill the last discount field before adding a new one.", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            discounts.add(null);
            updateDiscountAdapterData();
            enforceMaxDiscount();
            binding.rvEdtDiscount.scrollToPosition(discounts.size() - 1);
        });

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

        binding.tvReducequantity.setOnClickListener(v -> updateQuantity(-1));
        binding.tvAddquantity.setOnClickListener(v -> updateQuantity(1));

        binding.edtPrice.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().equals(currentPriceString)) {
                    return;
                }

                binding.edtPrice.removeTextChangedListener(this);

                String cleanString = s.toString().replaceAll("[^\\d,]", "");
                double parsed = 0;
                try {
                    if (!cleanString.isEmpty()) {
                        DecimalFormat inputDecimalFormat = new DecimalFormat("#,##0.##", new DecimalFormatSymbols(Locale.getDefault()));
                        Number parsedNumber = inputDecimalFormat.parse(cleanString);
                        parsed = parsedNumber.doubleValue();
                    }
                } catch (ParseException e) {
                    parsed = 0;
                }

                String formatted = decimalFormat.format(parsed);
                currentPriceString = formatted;
                binding.edtPrice.setText(formatted);
                binding.edtPrice.setSelection(formatted.length());

                binding.edtPrice.addTextChangedListener(this);
                calculateAndDisplayTotalDiscount();
            }
        });
    }

    private int calculateDiscountColumns(int itemWidthDp) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        float screenWidthDp = displayMetrics.widthPixels / displayMetrics.density;
        int calculatedColumns = (int) (screenWidthDp / itemWidthDp);
        return Math.max(calculatedColumns, 1);
    }

    private String formatDiscountForDisplay(Double discount) {
        if (discount == null) {
            return "";
        }
        if (Math.abs(discount - Math.round(discount)) < 0.0001) {
            return String.format(Locale.getDefault(), "%.0f", discount);
        } else if (Math.round(discount * 10) == discount * 10) {
            return String.format(Locale.getDefault(), "%.1f", discount);
        } else {
            return String.format(Locale.getDefault(), "%.2f", discount).replaceAll("0+$", "").replaceAll("[.,]$", "");
        }
    }

    private void updateDiscountAdapterData() {
        List<String> discountStrings = new ArrayList<>();
        for (Double d : discounts) {
            discountStrings.add(formatDiscountForDisplay(d));
        }
        discountAdapter.updateData(discountStrings);
        calculateAndDisplayTotalDiscount();
    }


    @SuppressLint("SetTextI18n")
    private void loadItemData(int itemId) {
        Item item = dbHelper.getItemById(itemId);
        if (item != null) {
            binding.edtTitle.setText(item.getName());
            binding.edtDesc.setText(item.getDescription());
            binding.edtCategory.setText(item.getCategory());
            binding.edtQuantity.setText(String.valueOf(item.getCount()));

            discounts.clear();
            if (item.getDiscountsJson() != null && !item.getDiscountsJson().isEmpty()) {
                Gson gson = new Gson();
                Type type = new TypeToken<List<Double>>() {}.getType();
                List<Double> loadedDiscounts = gson.fromJson(item.getDiscountsJson(), type);
                if (loadedDiscounts != null) {
                    discounts.addAll(loadedDiscounts);
                }
            }

            if (discounts.isEmpty()) {
                discounts.add(null);
            }
            updateDiscountAdapterData();

            binding.edtPrice.setText(decimalFormat.format(item.getPrice()));

            if (item.getImageData() != null && !item.getImageData().isEmpty()) {
                File imgFile = new File(getFilesDir(), item.getImageData());
                if (imgFile.exists()) {
                    Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                    binding.imageViewCaptured.setImageBitmap(myBitmap);
                    binding.imageViewCaptured.setVisibility(View.VISIBLE);
                } else {
                    binding.imageViewCaptured.setVisibility(View.GONE);
                }
            } else {
                binding.imageViewCaptured.setVisibility(View.GONE);
            }
            calculateAndDisplayTotalDiscount();
        } else {
            Toast.makeText(this, "Failed to load item data.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void updateQuantity(int change) {
        String currentQuantityString = binding.edtQuantity.getText().toString();
        int currentQuantity = 0;
        try {
            currentQuantity = Integer.parseInt(currentQuantityString);
        } catch (NumberFormatException e) {
        }

        int newQuantity = currentQuantity + change;
        if (newQuantity < 1) {
            newQuantity = 1;
        }
        binding.edtQuantity.setText(String.valueOf(newQuantity));
        calculateAndDisplayTotalDiscount();
    }


    @SuppressLint("SetTextI18n")
    private void calculateAndDisplayTotalDiscount() {
        double originalPrice = 0.0;
        try {
            String priceString = binding.edtPrice.getText().toString();
            Number parsedNumber = decimalFormat.parse(priceString.isEmpty() ? "0" : priceString);
            originalPrice = parsedNumber.doubleValue();
        } catch (ParseException | NumberFormatException e) {
            originalPrice = 0.0;
        }

        int quantity = 0;
        try {
            quantity = Integer.parseInt(binding.edtQuantity.getText().toString());
        } catch (NumberFormatException e) {
            quantity = 0;
        }

        double totalBasePrice = originalPrice * quantity;
        double totalCombinedDiscountPercentage = 0.0;

        for (Double discount : discounts) {
            if (discount != null) {
                totalCombinedDiscountPercentage += discount;
            }
        }

        if (totalCombinedDiscountPercentage > 100.0) {
            totalCombinedDiscountPercentage = 100.0;
        }

        double finalPrice = totalBasePrice * (1 - (totalCombinedDiscountPercentage / 100));

        if (finalPrice < 0.0) {
            finalPrice = 0.0;
        }

        //binding.tvTotalPrice.setText(decimalFormat.format(finalPrice));
    }

    private void enforceMaxDiscount() {
        double totalCurrentDiscount = 0.0;

        for (Double discount : discounts) {
            if (discount != null) {
                totalCurrentDiscount += discount;
            }
        }

        if (totalCurrentDiscount > 100.0) {
            Toast.makeText(this, "Total discount cannot exceed 100%. Adjusting discounts.", Toast.LENGTH_LONG).show();

            double excess = totalCurrentDiscount - 100.0;

            for (int i = discounts.size() - 1; i >= 0; i--) {
                Double discount = discounts.get(i);
                if (discount != null && discount > 0) {
                    double newDiscount = Math.max(0, discount - excess);
                    discounts.set(i, newDiscount);
                    double newTotal = 0.0;
                    for (Double d : discounts) {
                        if (d != null) {
                            newTotal += d;
                        }
                    }
                    if (newTotal <= 100.0) {
                        break;
                    } else {
                        excess = newTotal - 100.0;
                    }
                }
            }
            updateDiscountAdapterData();
        }
    }

    private void saveItemChanges() {
        if (!discounts.isEmpty()) {
            Double lastDiscountValue = discounts.get(discounts.size() - 1);
        }

        Item item = dbHelper.getItemById(selectedItemId);
        if (item != null) {
            item.setName(binding.edtTitle.getText().toString());
            item.setDescription(binding.edtDesc.getText().toString());
            item.setCategory(binding.edtCategory.getText().toString());

            int count;
            try {
                count = Integer.parseInt(binding.edtQuantity.getText().toString());
                if (count < 1) {
                    Toast.makeText(this, "Quantity cannot be less than 1.", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid quantity amount.", Toast.LENGTH_SHORT).show();
                return;
            }
            item.setCount(count);

            double originalPrice = 0.0;
            try {
                String priceString = binding.edtPrice.getText().toString();
                Number parsedNumber = decimalFormat.parse(priceString);
                originalPrice = parsedNumber.doubleValue();

            } catch (ParseException e) {
                Toast.makeText(this, "Please enter a valid price format (e.g., 10.000 or 10.000,50).", Toast.LENGTH_LONG).show();
                return;
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter a valid price.", Toast.LENGTH_SHORT).show();
                return;
            }
            item.setPrice(originalPrice);

            List<Double> discountsToSave = new ArrayList<>();
            for (Double d : discounts) {
                if (d != null && d != 0.0) {
                    discountsToSave.add(d);
                }
            }
            Gson gson = new Gson();
            if (discountsToSave.isEmpty()) {
                item.setDiscountsJson(null);
            } else {
                item.setDiscountsJson(gson.toJson(discountsToSave));
            }

            double totalBasePrice = item.getPrice() * item.getCount();
            double totalCombinedDiscountPercentage = 0.0;
            for (Double discount : discountsToSave) {
                if (discount != null) {
                    totalCombinedDiscountPercentage += discount;
                }
            }

            if (totalCombinedDiscountPercentage > 100.0) {
                totalCombinedDiscountPercentage = 100.0;
            }
            item.setTotalDiscountPercentage(totalCombinedDiscountPercentage);

            double finalPrice = totalBasePrice * (1 - (totalCombinedDiscountPercentage / 100));
            if (finalPrice < 0.0) {
                finalPrice = 0.0;
            }
            item.setFinalPrice(finalPrice);

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