package com.leshoraa.listshop;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
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
import android.util.Base64;
import android.util.DisplayMetrics;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
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
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.leshoraa.listshop.adapter.DiscountAdapter;
import com.leshoraa.listshop.databinding.ActivityAddItemBinding;
import com.leshoraa.listshop.model.DatabaseHelper;
import com.leshoraa.listshop.model.Item;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AddItemActivity extends AppCompatActivity {

    private ActivityAddItemBinding binding;
    private DatabaseHelper dbHelper;
    private DiscountAdapter discountAdapter;
    private List<String> discountItems;
    private DecimalFormat decimalFormat;
    private int numberOfDiscountColumns;
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA};
    private ActivityResultLauncher<Intent> takePictureLauncher;
    private android.hardware.Camera camera;
    private SurfaceHolder surfaceHolder;
    private int displayRotation;
    private Bitmap capturedImageBitmap;
    private int parentListIdFromIntent;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable enforceMaxDiscountRunnable = () -> {
        enforceMaxDiscount();
    };

    private final String GEMINI_API_KEY = "AIzaSyBh2atjZlS_3CBjzsrLNCTvXKitRbvDiLI";

    private final OkHttpClient client = new OkHttpClient();
    private final List<String> GEMINI_MODELS = Arrays.asList(
            "gemini-1.5-flash-latest",
            "gemini-1.5-pro-latest"
    );
    private int currentModelIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddItemBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dbHelper = new DatabaseHelper(this);

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        displayRotation = getWindowManager().getDefaultDisplay().getRotation();

        parentListIdFromIntent = getIntent().getIntExtra(ListActivity.EXTRA_ITEM_ID, -1);
        if (parentListIdFromIntent == -1) {
            Toast.makeText(this, "Error: Parent list ID not provided.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

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

        setupDiscountRecyclerView();
        setupTakePictureLauncher();
        setupDropdownMenu();
        setupAiSwitch();

        SurfaceView surfaceView = binding.cameraPreview;
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                if (allPermissionsGranted()) {
                    openCameraPreview(holder);
                } else {
                    ActivityCompat.requestPermissions(AddItemActivity.this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
                }
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
                if (camera != null) {
                    camera.stopPreview();
                    try {
                        camera.setPreviewDisplay(holder);
                        camera.startPreview();
                    } catch (Exception e) {
                        Toast.makeText(AddItemActivity.this, "Preview restart failed", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                releaseCamera();
            }
        });

        binding.cameraPreview.setOnClickListener(v -> {
            if (allPermissionsGranted()) {
                dispatchTakePictureIntent();
            } else {
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
            }
        });

        binding.imageViewCaptured.setOnClickListener(v -> {
            if (allPermissionsGranted()) {
                dispatchTakePictureIntent();
            } else {
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
            }
        });

        binding.back.setOnClickListener(v -> onBackPressed());

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
        binding.btnSaveItem.setOnClickListener(v -> addItemToDatabase());
    }

    private void setupAiSwitch() {
        String savedState = dbHelper.getSetting(DatabaseHelper.SETTING_AI_SWITCH_STATE, "0");
        boolean isAiSwitchOn = "1".equals(savedState);
        binding.switchAi.setChecked(isAiSwitchOn);
        binding.switchAi.setOnCheckedChangeListener((buttonView, isChecked) -> {
            dbHelper.saveSetting(DatabaseHelper.SETTING_AI_SWITCH_STATE, isChecked ? "1" : "0");
            if (isChecked && capturedImageBitmap != null) {
                analyzeImageWithGemini(capturedImageBitmap);
            }
        });
    }

    private void setupDropdownMenu() {
        binding.dropdownMenu.setOnClickListener(v -> {
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
        });
    }

    private void setupDiscountRecyclerView() {
        discountItems = new ArrayList<>();
        discountItems.add("");
        discountAdapter = new DiscountAdapter(discountItems);
        numberOfDiscountColumns = calculateDiscountColumns(150);
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
                discountItems.remove(position);
                discountAdapter.notifyItemRemoved(position);
                if (discountItems.isEmpty()) {
                    discountItems.add("");
                    discountAdapter.notifyItemInserted(0);
                }
                binding.rvEdtDiscount.post(() -> enforceMaxDiscount());
            }
        });
        discountAdapter.setOnAddButtonClickListener(() -> {
            if (!discountItems.isEmpty()) {
                String lastItemValue = discountItems.get(discountItems.size() - 1);
                if (lastItemValue.trim().isEmpty()) {
                    Toast.makeText(AddItemActivity.this, "Please fill the current discount field first.", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            discountItems.add("");
            discountAdapter.notifyItemInserted(discountItems.size() - 1);
            binding.rvEdtDiscount.scrollToPosition(discountItems.size() - 1);
            binding.rvEdtDiscount.post(() -> enforceMaxDiscount());
        });
    }

    private void setupTakePictureLauncher() {
        takePictureLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Bundle extras = result.getData().getExtras();
                if (extras != null) {
                    capturedImageBitmap = (Bitmap) extras.get("data");
                    binding.imageViewCaptured.setImageBitmap(capturedImageBitmap);
                    binding.imageViewCaptured.setVisibility(View.VISIBLE);
                    binding.cameraPreview.setVisibility(View.GONE);
                    binding.edtQuantity.setText("1");
                    if (binding.switchAi.isChecked()) {
                        currentModelIndex = 0;
                        analyzeImageWithGemini(capturedImageBitmap);
                    }
                }
            }
        });
    }

    private void analyzeImageWithGemini(Bitmap image) {
        if (currentModelIndex >= GEMINI_MODELS.size()) {
            runOnUiThread(() -> {
                Toast.makeText(this, "All Gemini models failed. Please try again later.", Toast.LENGTH_LONG).show();
                binding.edtTitle.setText("API Error");
                binding.edtDesc.setText("All available AI models failed to process the image.");
                binding.edtCategory.setText("Error");
            });
            return;
        }
        String currentModel = GEMINI_MODELS.get(currentModelIndex);
        binding.edtTitle.setText("Analyzing image...");
        binding.edtDesc.setText("Please wait, identifying object...");
        binding.edtCategory.setText("Identifying category...");
        Bitmap resizedImage = resizeBitmap(image, 1200, 1200);
        if (resizedImage == null) {
            Toast.makeText(this, "Failed to process image.", Toast.LENGTH_LONG).show();
            binding.edtTitle.setText("Image Error");
            binding.edtDesc.setText("Failed to prepare image.");
            binding.edtCategory.setText("Error");
            return;
        }
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        resizedImage.compress(Bitmap.CompressFormat.JPEG, 70, stream);
        String base64Image = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP);
        JSONObject json = new JSONObject();
        try {
            json.put("model", currentModel);
            JSONArray contents = new JSONArray();
            JSONObject part1 = new JSONObject();
            part1.put("text", "As a shopping list assistant, identify the main object in this image. Provide a detailed description, a concise title (max 4 words), and a single, most appropriate category for the item, all in English. If the object is not a common shopping item (e.g., a person, an animal, a landscape, abstract art, a vehicle that cannot be purchased in a typical grocery or department store), respond only with the exact phrase 'Not a shopping item detected.' and nothing else. Ensure titles are descriptive, descriptions are informative and cover general uses/characteristics in paragraphs, and categories are precise (e.g., 'Fresh Produce', 'Dairy & Refrigerated', 'Packaged Snacks', 'Beverages', 'Home Cleaning', 'Personal Care', 'Health & Medicine', 'Pet Supplies', 'Electronics', 'Apparel', 'Kitchenware', 'Office Supplies', 'Toys & Games', 'Automotive', 'Tools & Hardware', 'Home Decor', 'Garden & Outdoor', 'Books & Media', 'Other').\n\nFormat your response for shopping items as follows:\n\nTitle: [object title]\nDescription: [detailed, paragraph-based description in English]\nCategory: [single English category word/phrase]");
            JSONObject part2 = new JSONObject();
            JSONObject inlineData = new JSONObject();
            inlineData.put("mime_type", "image/jpeg");
            inlineData.put("data", base64Image);
            part2.put("inlineData", inlineData);
            JSONObject content = new JSONObject();
            JSONArray parts = new JSONArray();
            parts.put(part1);
            parts.put(part2);
            content.put("parts", parts);
            contents.put(content);
            json.put("contents", contents);
            json.put("generationConfig", new JSONObject().put("temperature", 0.4));
            JSONArray safetySettings = new JSONArray();
            JSONObject harmCategory = new JSONObject();
            harmCategory.put("category", "HARM_CATEGORY_HARASSMENT");
            harmCategory.put("threshold", "BLOCK_NONE");
            safetySettings.put(harmCategory);
            json.put("safetySettings", safetySettings);
        } catch (Exception e) {
            runOnUiThread(() -> {
                Toast.makeText(this, "JSON creation error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                binding.edtTitle.setText("Error");
                binding.edtDesc.setText("Failed to prepare request.");
                binding.edtCategory.setText("Error");
            });
            return;
        }
        RequestBody body = RequestBody.create(json.toString(), MediaType.get("application/json"));
        Request request = new Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/" + currentModel + ":generateContent?key=" + GEMINI_API_KEY)
                .post(body)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(AddItemActivity.this, "Failed to connect to Gemini API: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    binding.edtTitle.setText("Connection Error");
                    binding.edtDesc.setText("Failed to retrieve description. Check internet connection.");
                    binding.edtCategory.setText("Error");
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try {
                    String resBody = response.body() != null ? response.body().string() : "Empty Response Body";
                    if (!response.isSuccessful()) {
                        if (response.code() == 404 || response.code() == 503 || response.code() == 429) {
                            currentModelIndex++;
                            runOnUiThread(() -> analyzeImageWithGemini(capturedImageBitmap));
                        } else {
                            runOnUiThread(() -> {
                                Toast.makeText(AddItemActivity.this, "API response unsuccessful: " + response.code() + " - " + resBody, Toast.LENGTH_LONG).show();
                                binding.edtTitle.setText("Error " + response.code());
                                binding.edtDesc.setText("Failed to get description. Status: " + response.code() + ". Detail: " + (resBody.length() > 100 ? resBody.substring(0, 100) + "..." : resBody));
                                binding.edtCategory.setText("Error");
                            });
                        }
                        return;
                    }
                    JSONObject resJson = new JSONObject(resBody);
                    JSONArray candidates = resJson.optJSONArray("candidates");
                    if (candidates != null && candidates.length() > 0) {
                        JSONObject firstCandidate = candidates.getJSONObject(0);
                        JSONObject content = firstCandidate.optJSONObject("content");
                        if (content != null) {
                            JSONArray parts = content.optJSONArray("parts");
                            if (parts != null && parts.length() > 0) {
                                String generatedText = parts.getJSONObject(0).optString("text", "");
                                if (!generatedText.isEmpty()) {
                                    runOnUiThread(() -> processGeminiResponse(generatedText));
                                } else {
                                    runOnUiThread(() -> {
                                        binding.edtTitle.setText("Not Found");
                                        binding.edtDesc.setText("No description generated.");
                                        binding.edtCategory.setText("Not Found");
                                    });
                                }
                            } else {
                                runOnUiThread(() -> {
                                    binding.edtTitle.setText("Not Found");
                                    binding.edtDesc.setText("No description generated.");
                                    binding.edtCategory.setText("Not Found");
                                });
                            }
                        } else {
                            runOnUiThread(() -> {
                                binding.edtTitle.setText("Not Found");
                                binding.edtDesc.setText("No description generated.");
                                binding.edtCategory.setText("Not Found");
                            });
                        }
                    } else {
                        JSONObject promptFeedback = resJson.optJSONObject("promptFeedback");
                        if (promptFeedback != null) {
                            JSONArray safetyRatings = promptFeedback.optJSONArray("safetyRatings");
                            if (safetyRatings != null) {
                                StringBuilder safetyMsg = new StringBuilder("Content blocked due to: ");
                                for (int i = 0; i < safetyRatings.length(); i++) {
                                    JSONObject rating = safetyRatings.getJSONObject(i);
                                    String category = rating.optString("category", "UNKNOWN");
                                    String probability = rating.optString("probability", "UNSPECIFIED");
                                    if (probability.startsWith("BLOCK") || probability.equals("HIGH")) {
                                        safetyMsg.append(category).append(" (").append(probability).append(") ");
                                    }
                                }
                                runOnUiThread(() -> {
                                    binding.edtTitle.setText("AI Blocked");
                                    binding.edtDesc.setText(safetyMsg.toString().trim());
                                    binding.edtCategory.setText("Blocked");
                                });
                            } else {
                                runOnUiThread(() -> {
                                    binding.edtTitle.setText("Unknown Error");
                                    binding.edtDesc.setText("No description generated.");
                                    binding.edtCategory.setText("Error");
                                });
                            }
                        } else {
                            runOnUiThread(() -> {
                                binding.edtTitle.setText("Not Found");
                                binding.edtDesc.setText("No description generated.");
                                binding.edtCategory.setText("Not Found");
                            });
                        }
                    }
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        Toast.makeText(AddItemActivity.this, "Response parsing error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        binding.edtTitle.setText("Parsing Error");
                        binding.edtDesc.setText("An error occurred while processing the response.");
                        binding.edtCategory.setText("Error");
                    });
                }
            }
        });
    }

    private Bitmap resizeBitmap(Bitmap image, int maxWidth, int maxHeight) {
        if (image == null) return null;
        int originalWidth = image.getWidth();
        int originalHeight = image.getHeight();
        if (originalWidth <= maxWidth && originalHeight <= maxHeight) {
            return image;
        }
        float ratio = Math.min((float) maxWidth / originalWidth, (float) maxHeight / originalHeight);
        int newWidth = Math.round(originalWidth * ratio);
        int newHeight = Math.round(originalHeight * ratio);
        if (newWidth <= 0 || newHeight <= 0) {
            return image;
        }
        return Bitmap.createScaledBitmap(image, newWidth, newHeight, true);
    }

    private void processGeminiResponse(String response) {
        try {
            String title = "";
            String description = "";
            String category = "";
            if (response.trim().equalsIgnoreCase("Not a shopping item detected.")) {
                title = "Not a Shopping Item";
                description = "The detected object is not recognized as a typical shopping item for your list. Please ensure you are photographing groceries, household goods, or similar items.";
                category = "N/A";
            } else {
                int titleStart = response.indexOf("Title:");
                int descStart = response.indexOf("Description:");
                int categoryStart = response.indexOf("Category:");
                if (titleStart != -1) {
                    titleStart += "Title:".length();
                    int titleEnd = (descStart != -1) ? descStart : (categoryStart != -1) ? categoryStart : response.length();
                    title = response.substring(titleStart, titleEnd).replace("Description:", "").replace("Category:", "").trim();
                }
                if (descStart != -1) {
                    descStart += "Description:".length();
                    int descEnd = (categoryStart != -1) ? categoryStart : response.length();
                    description = response.substring(descStart, descEnd).replace("Category:", "").trim();
                }
                if (categoryStart != -1) {
                    categoryStart += "Category:".length();
                    category = response.substring(categoryStart).trim();
                }
                String[] descLines = description.split("\r?\n");
                StringBuilder finalDescription = new StringBuilder();
                for (String line : descLines) {
                    finalDescription.append(line.trim()).append("\n");
                }
                description = finalDescription.toString().trim();
            }
            binding.edtTitle.setText(title);
            binding.edtDesc.setText(description);
            binding.edtCategory.setText(category);
        } catch (Exception e) {
            binding.edtTitle.setText("Parse Error");
            binding.edtDesc.setText("An error occurred while processing AI response.");
            binding.edtCategory.setText("Error");
            Toast.makeText(this, "Error parsing Gemini response: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            takePictureLauncher.launch(takePictureIntent);
        } catch (Exception e) {
            Toast.makeText(this, "Cannot open camera app", Toast.LENGTH_SHORT).show();
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

    private int calculateDiscountColumns(int itemWidthDp) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        float screenWidthDp = displayMetrics.widthPixels / displayMetrics.density;
        int calculatedColumns = (int) (screenWidthDp / itemWidthDp);
        return Math.max(calculatedColumns, 1);
    }

    private void openCameraPreview(SurfaceHolder holder) {
        try {
            if (camera != null) {
                releaseCamera();
            }
            camera = android.hardware.Camera.open();
            android.hardware.Camera.Parameters parameters = camera.getParameters();
            android.hardware.Camera.Size optimalSize = getOptimalPreviewSize(parameters.getSupportedPreviewSizes(), 4.0 / 4.0);
            if (optimalSize != null) {
                parameters.setPreviewSize(optimalSize.width, optimalSize.height);
            }
            camera.setParameters(parameters);
            setCameraDisplayOrientation(android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK, camera);
            camera.setPreviewDisplay(holder);
            camera.startPreview();
            float targetRatio = 4f / 3f;
            int parentWidth = binding.cameraPreview.getWidth();
            int parentHeight = binding.cameraPreview.getHeight();
            int newWidth, newHeight;
            if ((float) parentWidth / parentHeight > targetRatio) {
                newHeight = parentHeight;
                newWidth = (int) (parentHeight * targetRatio);
            } else {
                newWidth = parentWidth;
                newHeight = (int) (parentWidth / targetRatio);
            }
            ViewGroup.LayoutParams params = binding.cameraPreview.getLayoutParams();
            params.width = newWidth;
            params.height = newHeight;
            binding.cameraPreview.setLayoutParams(params);
            binding.cameraPreview.requestLayout();
        } catch (Exception e) {
            Toast.makeText(this, "Camera error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void releaseCamera() {
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    private void setCameraDisplayOrientation(int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = displayRotation;
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }
        int result;
        if (info.facing == android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;
        } else {
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    private android.hardware.Camera.Size getOptimalPreviewSize(List<android.hardware.Camera.Size> sizes, double targetRatio) {
        final double ASPECT_TOLERANCE = 0.1;
        android.hardware.Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;
        for (android.hardware.Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) < ASPECT_TOLERANCE) {
                if (Math.abs(size.height - 720) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - 720);
                }
            }
        }
        if (optimalSize == null && !sizes.isEmpty()) {
            optimalSize = sizes.get(0);
        }
        return optimalSize;
    }

    private String addDiscountData() {
        JSONArray discountsJsonArray = new JSONArray();
        double sumOfDiscounts = 0.0;
        for (int i = 0; i < discountAdapter.getItemCount(); i++) {
            String discountStr = discountAdapter.getDiscountAt(i);
            if (discountStr != null && !discountStr.trim().isEmpty()) {
                try {
                    double discountValue = Double.parseDouble(discountStr);
                    if (discountValue < 0 || discountValue > 100) {
                        Toast.makeText(this, "Discount must be between 0 and 100.", Toast.LENGTH_SHORT).show();
                        return null;
                    }
                    discountsJsonArray.put(discountValue);
                    sumOfDiscounts += discountValue;
                } catch (NumberFormatException | JSONException e) {
                    Toast.makeText(this, "Invalid discount value: " + discountStr, Toast.LENGTH_SHORT).show();
                    return null;
                }
            }
        }
        return discountsJsonArray.toString();
    }

    private void addItemToDatabase() {
        String name = binding.edtTitle.getText().toString();
        String description = binding.edtDesc.getText().toString();
        String category = binding.edtCategory.getText().toString();
        int count;
        try {
            count = Integer.parseInt(binding.edtQuantity.getText().toString());
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid quantity amount.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (count < 1) {
            Toast.makeText(this, "Minimum quantity must be 1 to save the item.", Toast.LENGTH_SHORT).show();
            return;
        }
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
        String discountsJson = addDiscountData();
        if (discountsJson == null) {
            return;
        }
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String imageData = null;
        String item_list_id = String.valueOf(System.currentTimeMillis());
        if (capturedImageBitmap != null) {
            imageData = saveImageToInternalStorage(capturedImageBitmap, item_list_id);
        }
        Item newItem = new Item(name, count, false, date);
        newItem.setDescription(description);
        newItem.setCategory(category);
        newItem.setImageData(imageData);
        newItem.setPrice(price);
        newItem.setDiscountsJson(discountsJson);
        newItem.recalculateFinalPrice();
        newItem.setItemListId(item_list_id);
        newItem.setParentListId(parentListIdFromIntent);
        long newRowId = dbHelper.addItem(newItem);
        if (newRowId != -1) {
            Toast.makeText(this, "Item added successfully!", Toast.LENGTH_SHORT).show();
            setResult(Activity.RESULT_OK);
            finish();
        } else {
            Toast.makeText(this, "Failed to add item.", Toast.LENGTH_SHORT).show();
        }
    }

    private String saveImageToInternalStorage(Bitmap bitmap, String filenameId) {
        String filename = filenameId + ".jpg";
        FileOutputStream fos = null;
        try {
            fos = openFileOutput(filename, Context.MODE_PRIVATE);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            return filename;
        } catch (Exception e) {
            return null;
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
            }
        }
    }

    private void enforceMaxDiscount() {
        double totalCurrentDiscount = 0.0;
        for (int i = 0; i < discountItems.size(); i++) {
            try {
                if (!discountItems.get(i).isEmpty()) {
                    double discountValue = Double.parseDouble(discountItems.get(i));
                    totalCurrentDiscount += discountValue;
                }
            } catch (NumberFormatException e) {
            }
        }
        if (totalCurrentDiscount > 100.0) {
            Toast.makeText(this, "Total discount cannot exceed 100%. Adjusting discounts.", Toast.LENGTH_LONG).show();
            double excess = totalCurrentDiscount - 100.0;
            if (!discountItems.isEmpty()) {
                try {
                    if (!discountItems.get(0).isEmpty()) {
                        double firstDiscount = Double.parseDouble(discountItems.get(0));
                        double newFirstDiscount = Math.max(0, firstDiscount - excess);
                        discountItems.set(0, String.valueOf(newFirstDiscount));
                    }
                } catch (NumberFormatException e) {
                    discountItems.set(0, "100");
                }
            } else {
                discountItems.add("100");
            }
            discountAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseCamera();
        dbHelper.close();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                openCameraPreview(surfaceHolder);
            } else {
                Toast.makeText(this, "Camera permissions not granted.", Toast.LENGTH_LONG).show();
            }
        }
    }
}