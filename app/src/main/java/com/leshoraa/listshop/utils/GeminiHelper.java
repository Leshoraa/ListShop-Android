package com.leshoraa.listshop.utils;

import android.graphics.Bitmap;
import androidx.annotation.NonNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GeminiHelper {

    private final String apiKey;
    private final OkHttpClient client;
    private final List<String> GEMINI_MODELS = Arrays.asList("gemini-2.0-flash", "gemini-1.5-flash");
    private int currentModelIndex = 0;

    public interface GeminiCallback {
        void onSuccess(String title, String description, String category);
        void onError(String errorMessage);
        void onLoading(String status);
    }

    public GeminiHelper(String apiKey) {
        this.apiKey = apiKey;
        this.client = new OkHttpClient();
    }

    public void analyzeImage(Bitmap image, GeminiCallback callback) {
        currentModelIndex = 0;
        tryRequest(image, callback);
    }

    private void tryRequest(Bitmap image, GeminiCallback callback) {
        if (currentModelIndex >= GEMINI_MODELS.size()) {
            callback.onError("All Gemini models failed. Please try again later.");
            return;
        }

        String currentModel = GEMINI_MODELS.get(currentModelIndex);
        callback.onLoading("Identifying item...");

        Bitmap resizedImage = ImageUtils.resizeBitmap(image, 1024, 1024);
        String base64Image = ImageUtils.bitmapToBase64(resizedImage);

        JSONObject json = createJsonRequest(currentModel, base64Image);
        if (json == null) {
            callback.onError("Failed to create JSON request");
            return;
        }

        RequestBody body = RequestBody.create(json.toString(), MediaType.get("application/json"));
        Request request = new Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/" + currentModel + ":generateContent?key=" + apiKey)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onError("Connection Failed: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String resBody = response.body() != null ? response.body().string() : "";

                if (!response.isSuccessful()) {
                    // Retry logic untuk error server
                    if (response.code() == 404 || response.code() == 503 || response.code() == 429) {
                        currentModelIndex++;
                        tryRequest(image, callback);
                    } else {
                        callback.onError("API Error " + response.code());
                    }
                    return;
                }

                parseGeminiResponse(resBody, callback);
            }
        });
    }

    private JSONObject createJsonRequest(String model, String base64Image) {
        try {
            JSONObject json = new JSONObject();
            json.put("model", model);

            JSONArray contents = new JSONArray();
            JSONObject part1 = new JSONObject();

            String prompt = "Act as a smart inventory assistant. Identify the main physical object in this image as a product.\n" +
                    "Even if the object is a toy, figurine, tool, or spare part, treat it as a shopping item.\n\n" +
                    "Provide:\n" +
                    "1. A concise product title (max 5 words).\n" +
                    "2. A short description (1 sentence).\n" +
                    "3. A single category chosen STRICTLY from this list:\n" +
                    "[Fresh Produce, Meat & Seafood, Dairy & Eggs, Bakery, Pantry & Groceries, Frozen Foods, Snacks & Sweets, Beverages, Household & Cleaning, Personal Care & Health, Baby & Kids, Pet Supplies, Electronics & Office, Clothing & Accessories, Home & Garden, Toys & Hobbies, Automotive & Tools, Sports & Outdoors, General].\n\n" +
                    "Rules:\n" +
                    "- If it's an Action Figure/Doll, categorize as 'Toys & Hobbies'.\n" +
                    "- Only respond with 'Not a shopping item detected' if the image is a selfie of a real human face, a blurred floor, or a vast landscape without objects.\n\n" +
                    "Format output:\n" +
                    "Title: [title]\n" +
                    "Description: [description]\n" +
                    "Category: [category from list]";

            part1.put("text", prompt);

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

            json.put("generationConfig", new JSONObject().put("temperature", 0.5));

            return json;
        } catch (JSONException e) {
            return null;
        }
    }

    private void parseGeminiResponse(String responseBody, GeminiCallback callback) {
        try {
            JSONObject resJson = new JSONObject(responseBody);
            JSONArray candidates = resJson.optJSONArray("candidates");
            if (candidates != null && candidates.length() > 0) {
                JSONObject firstCandidate = candidates.getJSONObject(0);
                JSONObject content = firstCandidate.optJSONObject("content");
                if (content != null) {
                    JSONArray parts = content.optJSONArray("parts");
                    if (parts != null && parts.length() > 0) {
                        String text = parts.getJSONObject(0).optString("text", "");

                        if (text.toLowerCase().contains("not a shopping item")) {
                            callback.onSuccess("Unknown Item", "Could not identify the object clearly.", "General");
                            return;
                        }

                        String title = extractValue(text, "Title:");
                        String desc = extractValue(text, "Description:");
                        String cat = extractValue(text, "Category:");

                        desc = desc.replace("Category:", "").trim();

                        if (title.isEmpty()) title = "Item Detected";
                        if (cat.isEmpty()) cat = "General";

                        callback.onSuccess(title, desc, cat);
                        return;
                    }
                }
            }
            callback.onError("Could not identify image.");
        } catch (Exception e) {
            callback.onError("Parsing Error");
        }
    }

    private String extractValue(String text, String key) {
        int start = text.indexOf(key);
        if (start == -1) return "";
        start += key.length();
        int end = text.indexOf("\n", start);
        if (end == -1) end = text.indexOf("Category:", start);
        if (end == -1) end = text.length();
        return text.substring(start, end).trim();
    }
}