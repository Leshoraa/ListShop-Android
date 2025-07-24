package com.leshoraa.listshop;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PreviewItemActivity extends AppCompatActivity {

    private ActivityPreviewItemBinding binding;
    private DatabaseHelper dbHelper;
    private int selectedItemId = -1;
    private List<Double> discounts;
    private Item currentItem;
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
                    enforceMaxDiscount();
                }
            } catch (ParseException e) {
                if (position < discounts.size()) {
                    discounts.set(position, 0.0);
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
                currentItem = dbHelper.getItemById(selectedItemId);
                if (currentItem != null) {
                    loadItemData();
                } else {
                    Toast.makeText(this, "Item not found.", Toast.LENGTH_SHORT).show();
                    finish();
                }
            } else {
                Toast.makeText(this, "Invalid Item ID.", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            Toast.makeText(this, "No Item ID passed.", Toast.LENGTH_SHORT).show();
            finish();
        }

        binding.back.setOnClickListener(v -> onBackPressed());
        binding.btnSaveItem.setOnClickListener(v -> saveItemChanges());
        binding.copy.setOnClickListener(v -> copyItemDetailsToClipboard());
        binding.tvReducequantity.setOnClickListener(v -> updateQuantity(-1));
        binding.tvAddquantity.setOnClickListener(v -> updateQuantity(1));
        binding.edtPrice.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().equals(currentPriceString)) return;
                binding.edtPrice.removeTextChangedListener(this);
                String cleanString = s.toString().replaceAll("[^\\d]", "");
                double parsed = 0;
                try {
                    if (!cleanString.isEmpty()) {
                        parsed = Double.parseDouble(cleanString);
                    }
                } catch (NumberFormatException e) {
                    parsed = 0;
                }
                String formatted = decimalFormat.format(parsed);
                currentPriceString = formatted;
                binding.edtPrice.setText(formatted);
                binding.edtPrice.setSelection(formatted.length());
                binding.edtPrice.addTextChangedListener(this);
            }
        });
    }

    private void copyItemDetailsToClipboard() {
        String itemName = binding.edtTitle.getText().toString();
        String description = binding.edtDesc.getText().toString();
        String category = binding.edtCategory.getText().toString();
        String priceString = binding.edtPrice.getText().toString();
        String quantityStr = binding.edtQuantity.getText().toString();
        String date = binding.dateItem.getText().toString();
        double originalPrice = 0.0;
        try {
            originalPrice = decimalFormat.parse(priceString).doubleValue();
        } catch (ParseException e) {
            Toast.makeText(this, "Invalid price format.", Toast.LENGTH_SHORT).show();
            return;
        }
        int quantity = Integer.parseInt(quantityStr);
        double totalDiscountPercentage = 0.0;
        for (Double discount : discounts) {
            if (discount != null) {
                totalDiscountPercentage += discount;
            }
        }
        if (totalDiscountPercentage > 100.0) totalDiscountPercentage = 100.0;
        double finalPrice = (originalPrice * quantity) * (1 - (totalDiscountPercentage / 100));
        String formattedDiscount = new DecimalFormat("#.##").format(totalDiscountPercentage) + "%";
        String formattedFinalPrice = decimalFormat.format(finalPrice);
        String details = "Item name: " + itemName + "\n\n" +
                "Description: " + (description.isEmpty() ? "-" : description) + "\n\n" +
                "Category: " + (category.isEmpty() ? "-" : category) + "\n\n" +
                "Price: " + priceString + " (Disc: " + formattedDiscount + ") = " + formattedFinalPrice + "\n\n" +
                "Quantity: " + quantityStr + "\n\n" +
                "Date: " + date;
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Item Details", details);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Copied: " + itemName, Toast.LENGTH_SHORT).show();
    }

    private int calculateDiscountColumns(int itemWidthDp) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        float screenWidthDp = displayMetrics.widthPixels / displayMetrics.density;
        int calculatedColumns = (int) (screenWidthDp / itemWidthDp);
        return Math.max(calculatedColumns, 1);
    }

    private String formatDiscountForDisplay(Double discount) {
        if (discount == null) return "";
        if (discount == discount.intValue()) {
            return String.valueOf(discount.intValue());
        }
        return String.valueOf(discount);
    }

    private void updateDiscountAdapterData() {
        List<String> discountStrings = new ArrayList<>();
        for (Double d : discounts) {
            discountStrings.add(formatDiscountForDisplay(d));
        }
        discountAdapter.updateData(discountStrings);
    }

    @SuppressLint("SetTextI18n")
    private void loadItemData() {
        binding.edtTitle.setText(currentItem.getName());
        binding.edtDesc.setText(currentItem.getDescription());
        binding.edtCategory.setText(currentItem.getCategory());
        binding.edtQuantity.setText(String.valueOf(currentItem.getCount()));
        loadDate(currentItem.getDate());
        discounts.clear();
        if (currentItem.getDiscountsJson() != null && !currentItem.getDiscountsJson().isEmpty()) {
            Gson gson = new Gson();
            Type type = new TypeToken<List<Double>>() {}.getType();
            List<Double> loadedDiscounts = gson.fromJson(currentItem.getDiscountsJson(), type);
            if (loadedDiscounts != null) {
                discounts.addAll(loadedDiscounts);
            }
        }
        if (discounts.isEmpty()) {
            discounts.add(null);
        }
        updateDiscountAdapterData();
        binding.edtPrice.setText(decimalFormat.format(currentItem.getPrice()));
        if (currentItem.getImageData() != null && !currentItem.getImageData().isEmpty()) {
            File imgFile = new File(getFilesDir(), currentItem.getImageData());
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
    }

    private void updateQuantity(int change) {
        try {
            int currentQuantity = Integer.parseInt(binding.edtQuantity.getText().toString());
            int newQuantity = Math.max(1, currentQuantity + change);
            binding.edtQuantity.setText(String.valueOf(newQuantity));
        } catch (NumberFormatException e) {
            binding.edtQuantity.setText("1");
        }
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
                    excess -= (discount - newDiscount);
                    if (excess <= 0) break;
                }
            }
            updateDiscountAdapterData();
        }
    }

    private void loadDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            binding.dateItem.setText("No Date");
            return;
        }
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        SimpleDateFormat outputFormat = new SimpleDateFormat("d MMM yyyy", Locale.getDefault());
        try {
            Date date = inputFormat.parse(dateString);
            if (date != null) {
                binding.dateItem.setText(outputFormat.format(date));
            }
        } catch (ParseException e) {
            binding.dateItem.setText(dateString);
        }
    }

    private void saveItemChanges() {
        if (currentItem == null) return;
        currentItem.setName(binding.edtTitle.getText().toString());
        currentItem.setDescription(binding.edtDesc.getText().toString());
        currentItem.setCategory(binding.edtCategory.getText().toString());
        try {
            int count = Integer.parseInt(binding.edtQuantity.getText().toString());
            if (count < 1) {
                Toast.makeText(this, "Quantity cannot be less than 1.", Toast.LENGTH_SHORT).show();
                return;
            }
            currentItem.setCount(count);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid quantity amount.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            currentItem.setPrice(decimalFormat.parse(binding.edtPrice.getText().toString()).doubleValue());
        } catch (ParseException e) {
            Toast.makeText(this, "Please enter a valid price.", Toast.LENGTH_SHORT).show();
            return;
        }
        List<Double> discountsToSave = new ArrayList<>();
        for (Double d : discounts) {
            if (d != null && d > 0) {
                discountsToSave.add(d);
            }
        }
        currentItem.setDiscountsJson(new Gson().toJson(discountsToSave));
        currentItem.recalculateFinalPrice();
        int rowsAffected = dbHelper.updateItem(currentItem);
        if (rowsAffected > 0) {
            Toast.makeText(this, "Item updated successfully!", Toast.LENGTH_SHORT).show();
            setResult(Activity.RESULT_OK);
            finish();
        } else {
            Toast.makeText(this, "Failed to update item.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbHelper.close();
    }
}