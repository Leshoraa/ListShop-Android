package com.leshoraa.listshop;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.PopupMenu;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.leshoraa.listshop.utils.CameraPreviewManager;
import com.leshoraa.listshop.utils.GeminiHelper;
import com.leshoraa.listshop.utils.ImageUtils;
import com.leshoraa.listshop.adapter.DiscountAdapter;
import com.leshoraa.listshop.databinding.ActivityAddItemBinding;
import com.leshoraa.listshop.model.DatabaseHelper;
import com.leshoraa.listshop.model.Item;

import org.json.JSONArray;
import org.json.JSONException;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AddItemActivity extends AppCompatActivity {

    private ActivityAddItemBinding binding;
    private DatabaseHelper dbHelper;
    private DiscountAdapter discountAdapter;
    private List<String> discountItems;
    private DecimalFormat decimalFormat;

    private CameraPreviewManager cameraPreviewManager;
    private GeminiHelper geminiHelper;

    private Bitmap capturedImageBitmap;
    private int parentListIdFromIntent;

    private final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA};
    private static final int REQUEST_CODE_PERMISSIONS = 10;

    private ActivityResultLauncher<Intent> takePictureLauncher;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable enforceMaxDiscountRunnable = this::enforceMaxDiscount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddItemBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dbHelper = new DatabaseHelper(this);
        geminiHelper = new GeminiHelper(BuildConfig.GEMINI_API_KEY);

        parentListIdFromIntent = getIntent().getIntExtra(ListActivity.EXTRA_ITEM_ID, -1);
        if (parentListIdFromIntent == -1) {
            Toast.makeText(this, "Error: Parent list ID not provided.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setupUI();
        setupWindow();
        setupDiscountRecyclerView();
        setupTakePictureLauncher();
        setupDropdownMenu();
        setupCategoryPicker();

        cameraPreviewManager = new CameraPreviewManager(this, binding.cameraPreview);

        setupListeners();
        checkPermissionAndStartCamera();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (allPermissionsGranted()) {
            cameraPreviewManager.startCamera();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cameraPreviewManager != null) {
            cameraPreviewManager.releaseCamera();
        }
    }

    private void setupUI() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());

            v.setPadding(systemBars.left, systemBars.top, systemBars.right,
                    Math.max(systemBars.bottom, imeInsets.bottom));

            return WindowInsetsCompat.CONSUMED;
        });

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
        symbols.setGroupingSeparator('.');
        decimalFormat = new DecimalFormat("#,##0", symbols);
        decimalFormat.setGroupingUsed(true);

        binding.edtPrice.addTextChangedListener(new TextWatcher() {
            private String current = "";
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().equals(current)) {
                    binding.edtPrice.removeTextChangedListener(this);
                    String cleanString = s.toString().replaceAll("[.]", "");
                    if (cleanString.isEmpty()) {
                        current = "";
                        binding.edtPrice.setText("");
                        binding.edtPrice.setSelection(0);
                        binding.edtPrice.addTextChangedListener(this);
                        return;
                    }
                    long parsed;
                    try {
                        parsed = Long.parseLong(cleanString);
                    } catch (NumberFormatException e) {
                        parsed = 0;
                    }
                    String formatted = decimalFormat.format(parsed);
                    current = formatted;
                    binding.edtPrice.setText(formatted);
                    binding.edtPrice.setSelection(formatted.length());
                    binding.edtPrice.addTextChangedListener(this);
                }
            }
        });
    }

    private void setupWindow() {
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(Color.WHITE);
        window.setNavigationBarColor(Color.WHITE);

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
        } else {
            window.getDecorView().setSystemUiVisibility(
                    window.getDecorView().getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            );
        }
    }

    private void setupListeners() {
        binding.cameraPreview.setOnClickListener(v -> checkPermissionAndTakePhoto());
        binding.imageViewCaptured.setOnClickListener(v -> checkPermissionAndTakePhoto());
        binding.back.setOnClickListener(v -> finish());
        binding.btnSaveItem.setOnClickListener(v -> addItemToDatabase());

        binding.tvReducequantity.setOnClickListener(v -> {
            try {
                int currentQuantity = Integer.parseInt(binding.edtQuantity.getText().toString());
                if (currentQuantity > 0) {
                    currentQuantity--;
                    binding.edtQuantity.setText(String.valueOf(currentQuantity));
                }
            } catch (NumberFormatException e) {
                binding.edtQuantity.setText("0");
            }
        });

        binding.tvAddquantity.setOnClickListener(v -> {
            try {
                int currentQuantity = Integer.parseInt(binding.edtQuantity.getText().toString());
                currentQuantity++;
                binding.edtQuantity.setText(String.valueOf(currentQuantity));
            } catch (NumberFormatException e) {
                binding.edtQuantity.setText("1");
            }
        });

        setupAiSwitch();
    }

    private void checkPermissionAndStartCamera() {
        if (allPermissionsGranted()) {
            cameraPreviewManager.startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    private void checkPermissionAndTakePhoto() {
        if (allPermissionsGranted()) {
            dispatchTakePictureIntent();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                cameraPreviewManager.startCamera();
            } else {
                Toast.makeText(this, "Camera permission required.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void setupAiSwitch() {
        String savedState = dbHelper.getSetting(DatabaseHelper.SETTING_AI_SWITCH_STATE, "0");
        binding.switchAi.setChecked("1".equals(savedState));
        binding.switchAi.setOnCheckedChangeListener((buttonView, isChecked) -> {
            dbHelper.saveSetting(DatabaseHelper.SETTING_AI_SWITCH_STATE, isChecked ? "1" : "0");
            if (isChecked && capturedImageBitmap != null) {
                analyzeImage(capturedImageBitmap);
            }
        });
    }

    private void analyzeImage(Bitmap image) {
        geminiHelper.analyzeImage(image, new GeminiHelper.GeminiCallback() {
            @Override
            public void onLoading(String status) {
                runOnUiThread(() -> {
                    binding.edtTitle.setText(status);
                    binding.edtDesc.setText("Please wait...");
                    binding.edtCategory.setText("Identifying...");
                });
            }

            @Override
            public void onSuccess(String title, String description, String category) {
                runOnUiThread(() -> {
                    binding.edtTitle.setText(title);
                    binding.edtDesc.setText(description);
                    binding.edtCategory.setText(category);
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(AddItemActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    binding.edtTitle.setText("Error");
                });
            }
        });
    }

    private void addItemToDatabase() {
        String name = binding.edtTitle.getText().toString();
        String description = binding.edtDesc.getText().toString();
        String category = binding.edtCategory.getText().toString();

        // Validasi Quantity
        int count;
        try {
            String qtyString = binding.edtQuantity.getText().toString();
            if (qtyString.isEmpty()) count = 1;
            else count = Integer.parseInt(qtyString);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid quantity amount.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (count < 1) {
            Toast.makeText(this, "Minimum quantity must be 1.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validasi Price
        double price = 0.0;
        try {
            String priceString = binding.edtPrice.getText().toString().replaceAll("[.]", "");
            if (!priceString.isEmpty()) {
                price = Double.parseDouble(priceString);
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid price.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validasi Discount (Optional)
        String discountsJson = addDiscountData();
        if ("INVALID".equals(discountsJson)) return;

        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String imageData = null;
        String item_list_id = String.valueOf(System.currentTimeMillis());

        if (capturedImageBitmap != null) {
            imageData = ImageUtils.saveImageToInternalStorage(this, capturedImageBitmap, item_list_id);
        }

        Item newItem = new Item(name, count, false, date);
        newItem.setItemListId(item_list_id);
        newItem.setParentListId(parentListIdFromIntent);

        newItem.setDescription(description);
        newItem.setCategory(category);
        newItem.setImageData(imageData);

        newItem.setPrice(price);
        newItem.setDiscountsJson(discountsJson.isEmpty() ? null : discountsJson);

        newItem.recalculateFinalPrice();

        long newRowId = dbHelper.addItem(newItem);
        if (newRowId != -1) {
            Toast.makeText(this, "Item added successfully!", Toast.LENGTH_SHORT).show();
            setResult(Activity.RESULT_OK);
            finish();
        } else {
            Toast.makeText(this, "Failed to add item.", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupDiscountRecyclerView() {
        discountItems = new ArrayList<>();
        discountItems.add("");
        discountAdapter = new DiscountAdapter(discountItems);
        int numberOfDiscountColumns = calculateDiscountColumns(150);
        GridLayoutManager layoutManager = new GridLayoutManager(this, numberOfDiscountColumns);
        binding.rvEdtDiscount.setLayoutManager(layoutManager);
        binding.rvEdtDiscount.setAdapter(discountAdapter);

        discountAdapter.setOnEditTextChangeListener((position, text) -> {
            if (position >= 0 && position < discountItems.size()) {
                discountItems.set(position, text);
                handler.removeCallbacks(enforceMaxDiscountRunnable);
                handler.postDelayed(enforceMaxDiscountRunnable, 500);
            }
        });

        discountAdapter.setOnItemDeleteListener(position -> {
            if (position != RecyclerView.NO_POSITION && position < discountItems.size()) {
                if (discountItems.size() == 1) {
                    discountItems.set(position, "");
                    discountAdapter.notifyItemChanged(position);
                } else {
                    discountItems.remove(position);
                    discountAdapter.notifyDataSetChanged();
                }
                binding.rvEdtDiscount.post(this::enforceMaxDiscount);
            }
        });

        discountAdapter.setOnAddButtonClickListener(() -> {
            if (!discountItems.isEmpty()) {
                String lastItemVal = discountItems.get(discountItems.size() - 1);
                if (lastItemVal.isEmpty()) {
                    Toast.makeText(this, "Please fill existing discount field.", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            discountItems.add("");
            discountAdapter.notifyItemInserted(discountItems.size() - 1);
            binding.rvEdtDiscount.scrollToPosition(discountItems.size() - 1);
        });
    }

    private int calculateDiscountColumns(int columnWidthDp) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        float screenWidthDp = displayMetrics.widthPixels / displayMetrics.density;
        int noOfColumns = (int) (screenWidthDp / columnWidthDp);
        return Math.max(noOfColumns, 2);
    }

    private String addDiscountData() {
        JSONArray jsonArray = new JSONArray();
        for (String discount : discountItems) {
            String trimmed = discount.trim();
            if (!trimmed.isEmpty()) {
                try {
                    double val = Double.parseDouble(trimmed);
                    if (val > 100) {
                        Toast.makeText(this, "Discount cannot exceed 100%.", Toast.LENGTH_SHORT).show();
                        return "INVALID";
                    }
                    try {
                        jsonArray.put(val);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        return "INVALID";
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid discount value: " + trimmed, Toast.LENGTH_SHORT).show();
                    return "INVALID";
                }
            }
        }
        return jsonArray.length() > 0 ? jsonArray.toString() : "";
    }

    private void enforceMaxDiscount() {
        boolean changed = false;
        for (int i = 0; i < discountItems.size(); i++) {
            String val = discountItems.get(i);
            if (!val.isEmpty()) {
                try {
                    double dVal = Double.parseDouble(val);
                    if (dVal > 100) {
                        discountItems.set(i, "100");
                        changed = true;
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        if (changed) {
            discountAdapter.notifyDataSetChanged();
            Toast.makeText(this, "Max discount is 100%", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupTakePictureLauncher() {
        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Bundle extras = result.getData().getExtras();
                        if (extras != null) {
                            capturedImageBitmap = (Bitmap) extras.get("data");
                            if (capturedImageBitmap != null) {
                                binding.imageViewCaptured.setImageBitmap(capturedImageBitmap);
                                binding.imageViewCaptured.setVisibility(View.VISIBLE);
                                binding.cameraPreview.setVisibility(View.GONE);
                                if (binding.switchAi.isChecked()) {
                                    analyzeImage(capturedImageBitmap);
                                }
                            }
                        }
                    }
                }
        );
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (getPackageManager().resolveActivity(takePictureIntent, 0) != null) {
            takePictureLauncher.launch(takePictureIntent);
        }
    }

    private void setupDropdownMenu() {
        View.OnClickListener listener = v -> {
            PopupMenu popup = new PopupMenu(AddItemActivity.this, v);
            popup.getMenu().add("Remove title");
            popup.getMenu().add("Remove Description");
            popup.getMenu().add("Remove Category");
            popup.getMenu().add("Remove Price");
            popup.getMenu().add("----------");
            popup.getMenu().add("Delete All");
            popup.setOnMenuItemClickListener(item -> {
                switch (item.getTitle().toString()) {
                    case "Remove title":
                        binding.edtTitle.setText("");
                        return true;
                    case "Remove Description":
                        binding.edtDesc.setText("");
                        return true;
                    case "Remove Category":
                        binding.edtCategory.setText("");
                        return true;
                    case "Remove Price":
                        binding.edtPrice.setText("");
                        return true;
                    case "Delete All":
                        binding.edtTitle.setText("");
                        binding.edtDesc.setText("");
                        binding.edtCategory.setText("");
                        binding.edtPrice.setText("");
                        return true;
                    default:
                        return false;
                }
            });
            popup.show();
        };
        binding.dropdownMenu.setOnClickListener(listener);
        binding.tvDropdownMenu.setOnClickListener(listener);
    }

    private void setupCategoryPicker() {
        binding.edtCategory.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(AddItemActivity.this, v);
            popupMenu.getMenu().add("Food & Drinks");
            popupMenu.getMenu().add("Health & Beauty");
            popupMenu.getMenu().add("Household");
            popupMenu.getMenu().add("Electronics");
            popupMenu.getMenu().add("Others");

            popupMenu.setOnMenuItemClickListener(item -> {
                binding.edtCategory.setText(item.getTitle());
                return true;
            });
            popupMenu.show();
        });
    }
}
