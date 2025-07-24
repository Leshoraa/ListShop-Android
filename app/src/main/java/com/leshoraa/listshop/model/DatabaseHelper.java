package com.leshoraa.listshop.model;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";
    private Context context;

    private static final String DATABASE_NAME = "shopping_list.db";
    private static final int DATABASE_VERSION = 1;
    public static final String TABLE_MARKETS = "markets";
    public static final String COLUMN_MARKET_ID = "_id";
    public static final String COLUMN_MARKET_NAME = "name";
    public static final String COLUMN_MARKET_DATE = "date";
    public static final String COLUMN_MARKET_COUNT = "count";
    public static final String COLUMN_MARKET_ORDER = "market_order";

    public static final String TABLE_ITEM_LIST = "item_list";
    public static final String COLUMN_ITEM_LIST_INTERNAL_ID = "_id";
    public static final String COLUMN_ITEM_LIST_ITEM_ID = "item_list_item_id";
    public static final String COLUMN_ITEM_LIST_NAME = "name";
    public static final String COLUMN_ITEM_LIST_COUNT = "count";
    public static final String COLUMN_ITEM_LIST_IS_ADD_BUTTON = "is_add_button";
    public static final String COLUMN_ITEM_LIST_DATE = "date";
    public static final String COLUMN_ITEM_LIST_DESCRIPTION = "description";
    public static final String COLUMN_ITEM_LIST_CATEGORY = "category";
    public static final String COLUMN_ITEM_LIST_IMAGE_DATA = "image_data";
    public static final String COLUMN_ITEM_LIST_PRICE = "price";
    public static final String COLUMN_ITEM_LIST_DISCOUNTS_JSON = "discounts_json";
    public static final String COLUMN_ITEM_LIST_FINAL_PRICE = "final_price";
    public static final String COLUMN_ITEM_LIST_PARENT_LIST_ID = "parent_list_id";
    public static final String COLUMN_ITEM_LIST_TOTAL_DISCOUNT_PERCENTAGE = "total_discount_percentage";
    public static final String COLUMN_ITEM_LIST_ORDER = "item_order";

    public static final String TABLE_TODO_LIST = "todo_list";
    public static final String COLUMN_TODO_ID = "_id";
    public static final String COLUMN_TODO_NAME = "name";
    public static final String COLUMN_TODO_IS_CHECKED = "is_checked";
    public static final String COLUMN_TODO_PARENT_LIST_ID = "parent_list_id";

    public static final String TABLE_SETTINGS = "settings";
    public static final String COLUMN_SETTING_KEY = "setting_key";
    public static final String COLUMN_SETTING_VALUE = "setting_value";
    public static final String SETTING_AI_SWITCH_STATE = "ai_switch_state";


    private static final String CREATE_TABLE_MARKETS = "CREATE TABLE " + TABLE_MARKETS + "("
            + COLUMN_MARKET_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_MARKET_NAME + " TEXT,"
            + COLUMN_MARKET_COUNT + " INTEGER,"
            + COLUMN_MARKET_DATE + " TEXT,"
            + COLUMN_MARKET_ORDER + " INTEGER DEFAULT 0" + ")";

    private static final String CREATE_TABLE_ITEM_LIST = "CREATE TABLE " + TABLE_ITEM_LIST + "("
            + COLUMN_ITEM_LIST_INTERNAL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_ITEM_LIST_ITEM_ID + " TEXT UNIQUE,"
            + COLUMN_ITEM_LIST_NAME + " TEXT,"
            + COLUMN_ITEM_LIST_COUNT + " INTEGER,"
            + COLUMN_ITEM_LIST_IS_ADD_BUTTON + " INTEGER,"
            + COLUMN_ITEM_LIST_DATE + " TEXT,"
            + COLUMN_ITEM_LIST_DESCRIPTION + " TEXT,"
            + COLUMN_ITEM_LIST_CATEGORY + " TEXT,"
            + COLUMN_ITEM_LIST_IMAGE_DATA + " TEXT,"
            + COLUMN_ITEM_LIST_PRICE + " REAL,"
            + COLUMN_ITEM_LIST_DISCOUNTS_JSON + " TEXT,"
            + COLUMN_ITEM_LIST_FINAL_PRICE + " REAL,"
            + COLUMN_ITEM_LIST_PARENT_LIST_ID + " INTEGER NOT NULL,"
            + COLUMN_ITEM_LIST_TOTAL_DISCOUNT_PERCENTAGE + " REAL,"
            + COLUMN_ITEM_LIST_ORDER + " INTEGER DEFAULT 0,"
            + " FOREIGN KEY (" + COLUMN_ITEM_LIST_PARENT_LIST_ID + ") REFERENCES " + TABLE_MARKETS + "(" + COLUMN_MARKET_ID + ") ON DELETE CASCADE" + ")";

    private static final String CREATE_TABLE_TODO_LIST = "CREATE TABLE " + TABLE_TODO_LIST + "("
            + COLUMN_TODO_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_TODO_NAME + " TEXT,"
            + COLUMN_TODO_IS_CHECKED + " INTEGER,"
            + COLUMN_TODO_PARENT_LIST_ID + " INTEGER NOT NULL,"
            + " FOREIGN KEY (" + COLUMN_TODO_PARENT_LIST_ID + ") REFERENCES " + TABLE_MARKETS + "(" + COLUMN_MARKET_ID + ") ON DELETE CASCADE" + ")";

    private static final String CREATE_TABLE_SETTINGS = "CREATE TABLE " + TABLE_SETTINGS + "("
            + COLUMN_SETTING_KEY + " TEXT PRIMARY KEY,"
            + COLUMN_SETTING_VALUE + " TEXT" + ")";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_MARKETS);
        db.execSQL(CREATE_TABLE_ITEM_LIST);
        db.execSQL(CREATE_TABLE_TODO_LIST);
        db.execSQL(CREATE_TABLE_SETTINGS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("PRAGMA foreign_keys = OFF;");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SETTINGS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TODO_LIST);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ITEM_LIST);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MARKETS);
        onCreate(db);
        db.execSQL("PRAGMA foreign_keys = ON;");
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (!db.isReadOnly()) {
            db.execSQL("PRAGMA foreign_keys = ON;");
        }
    }

    public void saveSetting(String key, String value) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SETTING_KEY, key);
        values.put(COLUMN_SETTING_VALUE, value);
        db.replace(TABLE_SETTINGS, null, values);
        db.close();
        Log.d(TAG, "Setting saved: " + key + " = " + value);
    }

    public String getSetting(String key, String defaultValue) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        String value = defaultValue;
        try {
            cursor = db.query(TABLE_SETTINGS, new String[]{COLUMN_SETTING_VALUE},
                    COLUMN_SETTING_KEY + " = ?", new String[]{key},
                    null, null, null);
            if (cursor.moveToFirst()) {
                value = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SETTING_VALUE));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting setting: " + key, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        Log.d(TAG, "Setting retrieved: " + key + " = " + value);
        return value;
    }

    public void addMarket(Item market) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_MARKET_NAME, market.getName());
        values.put(COLUMN_MARKET_COUNT, market.getCount());
        values.put(COLUMN_MARKET_DATE, market.getDate());
        int lastOrder = getLastMarketOrder();
        values.put(COLUMN_MARKET_ORDER, lastOrder + 1);
        market.setOrder(lastOrder + 1);

        long id = db.insert(TABLE_MARKETS, null, values);
        if (id == -1) {
            Log.e(TAG, "Failed to insert market: " + market.getName());
        } else {
            Log.d(TAG, "Market added with ID: " + id + ", Name: " + market.getName());
        }
        db.close();
    }

    private int getLastMarketOrder() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        int lastOrder = -1;
        try {
            cursor = db.rawQuery("SELECT MAX(" + COLUMN_MARKET_ORDER + ") FROM " + TABLE_MARKETS, null);
            if (cursor.moveToFirst()) {
                lastOrder = cursor.getInt(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting last market order", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return lastOrder;
    }

    public void deleteMarket(int marketId) {
        SQLiteDatabase db = this.getWritableDatabase();

        List<String> imagePathsToDelete = new ArrayList<>();
        Cursor cursor = db.query(TABLE_ITEM_LIST,
                new String[]{COLUMN_ITEM_LIST_IMAGE_DATA},
                COLUMN_ITEM_LIST_PARENT_LIST_ID + " = ?",
                new String[]{String.valueOf(marketId)},
                null, null, null);

        if (cursor.moveToFirst()) {
            do {
                String imageData = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ITEM_LIST_IMAGE_DATA));
                if (imageData != null && !imageData.isEmpty()) {
                    imagePathsToDelete.add(imageData);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        Log.d(TAG, "deleteMarket: Found " + imagePathsToDelete.size() + " image paths to delete for Market ID: " + marketId);

        int rowsAffected = db.delete(TABLE_MARKETS, COLUMN_MARKET_ID + " = ?", new String[]{String.valueOf(marketId)});
        db.close();
        Log.d(TAG, "deleteMarket: Market ID " + marketId + " deleted from DB. Rows affected: " + rowsAffected);

        if (context != null) {
            for (String imageFileName : imagePathsToDelete) {
                File file = new File(context.getFilesDir(), imageFileName);
                if (file.exists()) {
                    if (file.delete()) {
                        Log.d(TAG, "deleteMarket: Deleted image file: " + file.getAbsolutePath());
                    } else {
                        Log.e(TAG, "deleteMarket: Failed to delete image file: " + file.getAbsolutePath());
                    }
                } else {
                    Log.d(TAG, "deleteMarket: Image file not found: " + file.getAbsolutePath());
                }
            }
        } else {
            Log.e(TAG, "deleteMarket: Context is null, cannot delete image files.");
        }
    }

    public List<Item> getMarkets() {
        List<Item> marketList = new ArrayList<>();
        // ## PERUBAHAN DI SINI ##
        String selectQuery = "SELECT * FROM " + TABLE_MARKETS + " ORDER BY " + COLUMN_MARKET_DATE + " DESC, " + COLUMN_MARKET_ORDER + " ASC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                @SuppressLint("Range") int id = cursor.getInt(cursor.getColumnIndex(COLUMN_MARKET_ID));
                @SuppressLint("Range") String name = cursor.getString(cursor.getColumnIndex(COLUMN_MARKET_NAME));
                @SuppressLint("Range") String date = cursor.getString(cursor.getColumnIndex(COLUMN_MARKET_DATE));
                @SuppressLint("Range") int order = cursor.getInt(cursor.getColumnIndex(COLUMN_MARKET_ORDER));

                int listItemCount = getListItemCountForMarket(id);

                Item marketItem = new Item(id, name, listItemCount, false, date);
                marketItem.setOrder(order);
                marketList.add(marketItem);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        Log.d(TAG, "getMarkets: Retrieved " + marketList.size() + " markets.");
        return marketList;
    }

    // ## METODE BARU DITAMBAHKAN DI SINI ##
    public int updateMarketOrderAndDate(int marketId, int newOrder, String newDate) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_MARKET_ORDER, newOrder);
        values.put(COLUMN_MARKET_DATE, newDate);

        int rowsAffected = db.update(TABLE_MARKETS, values, COLUMN_MARKET_ID + " = ?",
                new String[]{String.valueOf(marketId)});
        db.close();
        if (rowsAffected > 0) {
            Log.d(TAG, "Market ID: " + marketId + " order updated to " + newOrder + " and date to " + newDate);
        } else {
            Log.e(TAG, "Failed to update market ID: " + marketId);
        }
        return rowsAffected;
    }

    public int getListItemCountForMarket(int marketId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String countQuery = "SELECT COUNT(*) FROM " + TABLE_ITEM_LIST + " WHERE " + COLUMN_ITEM_LIST_PARENT_LIST_ID + " = ?";
        Cursor cursor = db.rawQuery(countQuery, new String[]{String.valueOf(marketId)});
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        Log.d(TAG, "getListItemCountForMarket: Market ID " + marketId + " has " + count + " items.");
        return count;
    }

    public long addItem(Item item) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ITEM_LIST_ITEM_ID, item.getItemListId());
        values.put(COLUMN_ITEM_LIST_NAME, item.getName());
        values.put(COLUMN_ITEM_LIST_COUNT, item.getCount());
        values.put(COLUMN_ITEM_LIST_IS_ADD_BUTTON, item.isAddButton() ? 1 : 0);
        values.put(COLUMN_ITEM_LIST_DATE, item.getDate());
        values.put(COLUMN_ITEM_LIST_DESCRIPTION, item.getDescription());
        values.put(COLUMN_ITEM_LIST_CATEGORY, item.getCategory());
        values.put(COLUMN_ITEM_LIST_IMAGE_DATA, item.getImageData());
        values.put(COLUMN_ITEM_LIST_PRICE, item.getPrice());
        values.put(COLUMN_ITEM_LIST_DISCOUNTS_JSON, item.getDiscountsJson());
        values.put(COLUMN_ITEM_LIST_FINAL_PRICE, item.getFinalPrice());
        values.put(COLUMN_ITEM_LIST_PARENT_LIST_ID, item.getParentListId());
        values.put(COLUMN_ITEM_LIST_TOTAL_DISCOUNT_PERCENTAGE, item.getTotalDiscountPercentage());
        values.put(COLUMN_ITEM_LIST_ORDER, item.getOrder());

        long id = db.insert(TABLE_ITEM_LIST, null, values);
        if (id == -1) {
            Log.e(TAG, "Failed to insert item: " + item.getName());
        } else {
            Log.d(TAG, "Item added with ID: " + id + ", Name: " + item.getName());
        }
        db.close();
        return id;
    }

    public int updateItemQuantity(int itemId, int newQuantity) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ITEM_LIST_COUNT, newQuantity);

        Item existingItem = getItemByIdInternal(db, itemId);
        if (existingItem != null) {
            double originalPrice = existingItem.getPrice();
            String discountsJson = existingItem.getDiscountsJson();

            List<Double> discounts = new ArrayList<>();
            if (discountsJson != null && !discountsJson.isEmpty()) {
                Gson gson = new Gson();
                Type type = new TypeToken<List<Double>>() {}.getType();
                List<Double> loadedDiscounts = gson.fromJson(discountsJson, type);
                if (loadedDiscounts != null) {
                    discounts.addAll(loadedDiscounts);
                }
            }

            double totalCombinedDiscountPercentage = 0.0;
            for (Double discount : discounts) {
                if (discount != null) {
                    totalCombinedDiscountPercentage += discount;
                }
            }

            if (totalCombinedDiscountPercentage > 100.0) {
                totalCombinedDiscountPercentage = 100.0;
            }

            double totalBasePrice = originalPrice * newQuantity;
            double finalPrice = totalBasePrice * (1 - (totalCombinedDiscountPercentage / 100));
            if (finalPrice < 0.0) {
                finalPrice = 0.0;
            }

            values.put(COLUMN_ITEM_LIST_FINAL_PRICE, finalPrice);
            values.put(COLUMN_ITEM_LIST_TOTAL_DISCOUNT_PERCENTAGE, totalCombinedDiscountPercentage);
        } else {
            Log.w(TAG, "Attempted to update quantity for non-existent item ID: " + itemId);
            db.close();
            return 0;
        }

        int rowsAffected = db.update(TABLE_ITEM_LIST, values, COLUMN_ITEM_LIST_INTERNAL_ID + " = ?",
                new String[]{String.valueOf(itemId)});
        db.close();
        if (rowsAffected > 0) {
            Log.d(TAG, "Item ID: " + itemId + " quantity updated to " + newQuantity);
        } else {
            Log.e(TAG, "Failed to update item ID: " + itemId + " quantity.");
        }
        return rowsAffected;
    }

    public Item getItemById(int itemId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Item item = getItemByIdInternal(db, itemId);
        db.close();
        return item;
    }

    private Item getItemByIdInternal(SQLiteDatabase db, int itemId) {
        Cursor cursor = null;
        Item item = null;
        try {
            cursor = db.query(TABLE_ITEM_LIST, null,
                    COLUMN_ITEM_LIST_INTERNAL_ID + " = ?",
                    new String[]{String.valueOf(itemId)},
                    null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                @SuppressLint("Range") int id = cursor.getInt(cursor.getColumnIndex(COLUMN_ITEM_LIST_INTERNAL_ID));
                @SuppressLint("Range") String name = cursor.getString(cursor.getColumnIndex(COLUMN_ITEM_LIST_NAME));
                @SuppressLint("Range") int count = cursor.getInt(cursor.getColumnIndex(COLUMN_ITEM_LIST_COUNT));
                @SuppressLint("Range") boolean isAddButton = cursor.getInt(cursor.getColumnIndex(COLUMN_ITEM_LIST_IS_ADD_BUTTON)) == 1;
                @SuppressLint("Range") String date = cursor.getString(cursor.getColumnIndex(COLUMN_ITEM_LIST_DATE));
                @SuppressLint("Range") String description = cursor.getString(cursor.getColumnIndex(COLUMN_ITEM_LIST_DESCRIPTION));
                @SuppressLint("Range") String category = cursor.getString(cursor.getColumnIndex(COLUMN_ITEM_LIST_CATEGORY));
                @SuppressLint("Range") String imageData = cursor.getString(cursor.getColumnIndex(COLUMN_ITEM_LIST_IMAGE_DATA));
                @SuppressLint("Range") double price = cursor.getDouble(cursor.getColumnIndex(COLUMN_ITEM_LIST_PRICE));
                @SuppressLint("Range") String discountsJson = cursor.getString(cursor.getColumnIndex(COLUMN_ITEM_LIST_DISCOUNTS_JSON));
                @SuppressLint("Range") double finalPrice = cursor.getDouble(cursor.getColumnIndex(COLUMN_ITEM_LIST_FINAL_PRICE));
                @SuppressLint("Range") int parentListId = cursor.getInt(cursor.getColumnIndex(COLUMN_ITEM_LIST_PARENT_LIST_ID));
                @SuppressLint("Range") double totalDiscountPercentage = cursor.getDouble(cursor.getColumnIndex(COLUMN_ITEM_LIST_TOTAL_DISCOUNT_PERCENTAGE));
                @SuppressLint("Range") int order = cursor.getInt(cursor.getColumnIndex(COLUMN_ITEM_LIST_ORDER));

                item = new Item(id, name, count, isAddButton, date);
                item.setDescription(description);
                item.setCategory(category);
                item.setImageData(imageData);
                item.setPrice(price);
                item.setDiscountsJson(discountsJson);
                item.setFinalPrice(finalPrice);
                item.setParentListId(parentListId);
                item.setTotalDiscountPercentage(totalDiscountPercentage);
                item.setOrder(order);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return item;
    }


    public List<Item> getItemsByParentListId(int parentListId) {
        List<Item> items = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_ITEM_LIST, null,
                    COLUMN_ITEM_LIST_PARENT_LIST_ID + " = ?",
                    new String[]{String.valueOf(parentListId)},
                    null, null, COLUMN_ITEM_LIST_ORDER + " ASC", null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    @SuppressLint("Range") int id = cursor.getInt(cursor.getColumnIndex(COLUMN_ITEM_LIST_INTERNAL_ID));
                    @SuppressLint("Range") String name = cursor.getString(cursor.getColumnIndex(COLUMN_ITEM_LIST_NAME));
                    @SuppressLint("Range") int count = cursor.getInt(cursor.getColumnIndex(COLUMN_ITEM_LIST_COUNT));
                    @SuppressLint("Range") boolean isAddButton = cursor.getInt(cursor.getColumnIndex(COLUMN_ITEM_LIST_IS_ADD_BUTTON)) == 1;
                    @SuppressLint("Range") String date = cursor.getString(cursor.getColumnIndex(COLUMN_ITEM_LIST_DATE));
                    @SuppressLint("Range") String description = cursor.getString(cursor.getColumnIndex(COLUMN_ITEM_LIST_DESCRIPTION));
                    @SuppressLint("Range") String category = cursor.getString(cursor.getColumnIndex(COLUMN_ITEM_LIST_CATEGORY));
                    @SuppressLint("Range") String imageData = cursor.getString(cursor.getColumnIndex(COLUMN_ITEM_LIST_IMAGE_DATA));
                    @SuppressLint("Range") double price = cursor.getDouble(cursor.getColumnIndex(COLUMN_ITEM_LIST_PRICE));
                    @SuppressLint("Range") String discountsJson = cursor.getString(cursor.getColumnIndex(COLUMN_ITEM_LIST_DISCOUNTS_JSON));
                    @SuppressLint("Range") double finalPrice = cursor.getDouble(cursor.getColumnIndex(COLUMN_ITEM_LIST_FINAL_PRICE));
                    @SuppressLint("Range") int pListId = cursor.getInt(cursor.getColumnIndex(COLUMN_ITEM_LIST_PARENT_LIST_ID));
                    @SuppressLint("Range") double totalDiscountPercentage = cursor.getDouble(cursor.getColumnIndex(COLUMN_ITEM_LIST_TOTAL_DISCOUNT_PERCENTAGE));
                    @SuppressLint("Range") int order = cursor.getInt(cursor.getColumnIndex(COLUMN_ITEM_LIST_ORDER));

                    Item item = new Item(id, name, count, isAddButton, date);
                    item.setDescription(description);
                    item.setCategory(category);
                    item.setImageData(imageData);
                    item.setPrice(price);
                    item.setDiscountsJson(discountsJson);
                    item.setFinalPrice(finalPrice);
                    item.setParentListId(pListId);
                    item.setTotalDiscountPercentage(totalDiscountPercentage);
                    item.setOrder(order);
                    items.add(item);
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return items;
    }

    public int deleteItem(int itemId) {
        SQLiteDatabase db = this.getWritableDatabase();
        Item itemToDelete = getItemByIdInternal(db, itemId);
        if (itemToDelete != null && itemToDelete.getImageData() != null) {
            File imageFile = new File(context.getFilesDir(), itemToDelete.getImageData());
            if (imageFile.exists()) {
                if (imageFile.delete()) {
                    Log.d(TAG, "Deleted image file: " + imageFile.getAbsolutePath());
                } else {
                    Log.e(TAG, "Failed to delete image file: " + imageFile.getAbsolutePath());
                }
            }
        }

        int rowsAffected = db.delete(TABLE_ITEM_LIST, COLUMN_ITEM_LIST_INTERNAL_ID + " = ?",
                new String[]{String.valueOf(itemId)});
        db.close();
        if (rowsAffected > 0) {
            Log.d(TAG, "Item deleted with ID: " + itemId);
        } else {
            Log.e(TAG, "Failed to delete item with ID: " + itemId);
        }
        return rowsAffected;
    }

    public int updateItem(Item item) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ITEM_LIST_NAME, item.getName());
        values.put(COLUMN_ITEM_LIST_COUNT, item.getCount());
        values.put(COLUMN_ITEM_LIST_IS_ADD_BUTTON, item.isAddButton() ? 1 : 0);
        values.put(COLUMN_ITEM_LIST_DATE, item.getDate());
        values.put(COLUMN_ITEM_LIST_DESCRIPTION, item.getDescription());
        values.put(COLUMN_ITEM_LIST_CATEGORY, item.getCategory());
        values.put(COLUMN_ITEM_LIST_IMAGE_DATA, item.getImageData());
        values.put(COLUMN_ITEM_LIST_PRICE, item.getPrice());
        values.put(COLUMN_ITEM_LIST_DISCOUNTS_JSON, item.getDiscountsJson());
        values.put(COLUMN_ITEM_LIST_FINAL_PRICE, item.getFinalPrice());
        values.put(COLUMN_ITEM_LIST_TOTAL_DISCOUNT_PERCENTAGE, item.getTotalDiscountPercentage());
        values.put(COLUMN_ITEM_LIST_ORDER, item.getOrder());

        int rowsAffected = db.update(TABLE_ITEM_LIST, values, COLUMN_ITEM_LIST_INTERNAL_ID + " = ?",
                new String[]{String.valueOf(item.getId())});
        db.close();
        if (rowsAffected > 0) {
            Log.d(TAG, "Item updated with ID: " + item.getId());
        } else {
            Log.e(TAG, "Failed to update item with ID: " + item.getId());
        }
        return rowsAffected;
    }

    public int updateItemOrder(int itemId, int newOrder) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ITEM_LIST_ORDER, newOrder);

        int rowsAffected = db.update(TABLE_ITEM_LIST, values, COLUMN_ITEM_LIST_INTERNAL_ID + " = ?",
                new String[]{String.valueOf(itemId)});
        db.close();
        if (rowsAffected > 0) {
            Log.d(TAG, "Item ID: " + itemId + " order updated to " + newOrder);
        } else {
            Log.e(TAG, "Failed to update item ID: " + itemId + " order.");
        }
        return rowsAffected;
    }

    public long addTodoItem(Item todoItem) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TODO_NAME, todoItem.getName());
        values.put(COLUMN_TODO_IS_CHECKED, todoItem.isAddButton() ? 1 : 0);
        values.put(COLUMN_TODO_PARENT_LIST_ID, todoItem.getParentListId());

        long id = db.insert(TABLE_TODO_LIST, null, values);
        if (id == -1) {
            Log.e(TAG, "Failed to insert todo item: " + todoItem.getName());
        } else {
            Log.d(TAG, "Todo item added with ID: " + id + ", Name: " + todoItem.getName());
        }
        db.close();
        return id;
    }

    public List<Item> getTodoItemsByParentListId(int parentListId) {
        List<Item> todoItems = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_TODO_LIST, null,
                    COLUMN_TODO_PARENT_LIST_ID + " = ?",
                    new String[]{String.valueOf(parentListId)},
                    null, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    @SuppressLint("Range") int id = cursor.getInt(cursor.getColumnIndex(COLUMN_TODO_ID));
                    @SuppressLint("Range") String name = cursor.getString(cursor.getColumnIndex(COLUMN_TODO_NAME));
                    @SuppressLint("Range") boolean isChecked = cursor.getInt(cursor.getColumnIndex(COLUMN_TODO_IS_CHECKED)) == 1;
                    @SuppressLint("Range") int pListId = cursor.getInt(cursor.getColumnIndex(COLUMN_TODO_PARENT_LIST_ID));

                    Item todoItem = new Item(id, name, 0, isChecked, null);
                    todoItem.setParentListId(pListId);
                    todoItems.add(todoItem);
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return todoItems;
    }

    public int updateTodoItem(Item todoItem) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TODO_NAME, todoItem.getName());
        values.put(COLUMN_TODO_IS_CHECKED, todoItem.isAddButton() ? 1 : 0);

        int rowsAffected = db.update(TABLE_TODO_LIST, values, COLUMN_TODO_ID + " = ?",
                new String[]{String.valueOf(todoItem.getId())});
        db.close();
        if (rowsAffected > 0) {
            Log.d(TAG, "Todo item updated with ID: " + todoItem.getId());
        } else {
            Log.e(TAG, "Failed to update todo item with ID: " + todoItem.getId());
        }
        return rowsAffected;
    }

    public int deleteTodoItem(int todoItemId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsAffected = db.delete(TABLE_TODO_LIST, COLUMN_TODO_ID + " = ?",
                new String[]{String.valueOf(todoItemId)});
        db.close();
        if (rowsAffected > 0) {
            Log.d(TAG, "Todo item deleted with ID: " + todoItemId);
        } else {
            Log.e(TAG, "Failed to delete todo item with ID: " + todoItemId);
        }
        return rowsAffected;
    }

    public void updateMarketName(int marketId, String newName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_MARKET_NAME, newName);

        int rowsAffected = db.update(TABLE_MARKETS, values, COLUMN_MARKET_ID + " = ?",
                new String[]{String.valueOf(marketId)});
        db.close();
        if (rowsAffected > 0) {
            Log.d(TAG, "Market name updated for ID: " + marketId + " to " + newName);
        } else {
            Log.e(TAG, "Failed to update market name for ID: " + marketId);
        }
    }

    public Item getMarketById(int marketId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Item market = getMarketByIdInternal(db, marketId);
        db.close();
        return market;
    }

    private Item getMarketByIdInternal(SQLiteDatabase db, int marketId) {
        Cursor cursor = null;
        Item market = null;
        try {
            cursor = db.query(TABLE_MARKETS, null,
                    COLUMN_MARKET_ID + " = ?",
                    new String[]{String.valueOf(marketId)},
                    null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                @SuppressLint("Range") int id = cursor.getInt(cursor.getColumnIndex(COLUMN_MARKET_ID));
                @SuppressLint("Range") String name = cursor.getString(cursor.getColumnIndex(COLUMN_MARKET_NAME));
                @SuppressLint("Range") int count = cursor.getInt(cursor.getColumnIndex(COLUMN_MARKET_COUNT));
                @SuppressLint("Range") String date = cursor.getString(cursor.getColumnIndex(COLUMN_MARKET_DATE));
                @SuppressLint("Range") int order = cursor.getInt(cursor.getColumnIndex(COLUMN_MARKET_ORDER));

                market = new Item(id, name, count, false, date);
                market.setOrder(order);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return market;
    }
}