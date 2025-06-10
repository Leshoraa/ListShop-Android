package com.leshoraa.listshop.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
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

    public static final String TABLE_TODO_LIST = "todo_list";
    public static final String COLUMN_TODO_ID = "_id";
    public static final String COLUMN_TODO_NAME = "name";
    public static final String COLUMN_TODO_IS_CHECKED = "is_checked";
    public static final String COLUMN_TODO_PARENT_LIST_ID = "parent_list_id";

    private static final String CREATE_TABLE_MARKETS = "CREATE TABLE " + TABLE_MARKETS + "("
            + COLUMN_MARKET_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_MARKET_NAME + " TEXT,"
            + COLUMN_MARKET_COUNT + " INTEGER,"
            + COLUMN_MARKET_DATE + " TEXT" + ")";

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
            + " FOREIGN KEY (" + COLUMN_ITEM_LIST_PARENT_LIST_ID + ") REFERENCES " + TABLE_MARKETS + "(" + COLUMN_MARKET_ID + ") ON DELETE CASCADE" + ")";

    private static final String CREATE_TABLE_TODO_LIST = "CREATE TABLE " + TABLE_TODO_LIST + "("
            + COLUMN_TODO_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_TODO_NAME + " TEXT,"
            + COLUMN_TODO_IS_CHECKED + " INTEGER,"
            + COLUMN_TODO_PARENT_LIST_ID + " INTEGER NOT NULL,"
            + " FOREIGN KEY (" + COLUMN_TODO_PARENT_LIST_ID + ") REFERENCES " + TABLE_MARKETS + "(" + COLUMN_MARKET_ID + ") ON DELETE CASCADE" + ")";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_MARKETS);
        db.execSQL(CREATE_TABLE_ITEM_LIST);
        db.execSQL(CREATE_TABLE_TODO_LIST);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("PRAGMA foreign_keys = OFF;");
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

    public void addMarket(Item market) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_MARKET_NAME, market.getName());
        values.put(COLUMN_MARKET_COUNT, market.getCount());
        values.put(COLUMN_MARKET_DATE, market.getDate());

        long id = db.insert(TABLE_MARKETS, null, values);
        if (id == -1) {
            Log.e(TAG, "Failed to insert market: " + market.getName());
        } else {
            Log.d(TAG, "Market added with ID: " + id + ", Name: " + market.getName());
        }
        db.close();
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

        long id = db.insert(TABLE_ITEM_LIST, null, values);
        if (id == -1) {
            Log.e(TAG, "Failed to insert item: " + item.getName());
        } else {
            Log.d(TAG, "Item added with ID: " + id + ", Name: " + item.getName());
        }
        db.close();
        return id;
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

    public List<Item> getMarkets() {
        List<Item> marketList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_MARKETS;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MARKET_ID));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MARKET_NAME));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MARKET_DATE));

                int listItemCount = getListItemCountForMarket(id);

                Item marketItem = new Item(id, name, listItemCount, false, date);
                marketList.add(marketItem);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        Log.d(TAG, "getMarkets: Retrieved " + marketList.size() + " markets.");
        return marketList;
    }

    public Item getMarketById(int marketId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_MARKETS,
                new String[]{COLUMN_MARKET_ID, COLUMN_MARKET_NAME, COLUMN_MARKET_COUNT, COLUMN_MARKET_DATE},
                COLUMN_MARKET_ID + " = ?",
                new String[]{String.valueOf(marketId)},
                null, null, null);

        Item market = null;
        if (cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MARKET_ID));
            String name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MARKET_NAME));
            int count = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MARKET_COUNT));
            String date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MARKET_DATE));
            market = new Item(id, name, count, false, date);
            Log.d(TAG, "getMarketById: Found market with ID: " + marketId + ", Name: " + name);
        } else {
            Log.d(TAG, "getMarketById: No market found with ID: " + marketId);
        }
        cursor.close();
        db.close();
        return market;
    }
    public void updateMarketName(int marketId, String newName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_MARKET_NAME, newName);

        int rowsAffected = db.update(TABLE_MARKETS, values, COLUMN_MARKET_ID + " = ?",
                new String[]{String.valueOf(marketId)});
        db.close();
        Log.d(TAG, "updateMarketName: Market ID " + marketId + " updated, rows affected: " + rowsAffected);
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

    public List<Item> getItemsByParentListId(int parentListId) {
        List<Item> itemList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_ITEM_LIST + " WHERE " + COLUMN_ITEM_LIST_PARENT_LIST_ID + " = ?";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[]{String.valueOf(parentListId)});

        if (cursor.moveToFirst()) {
            do {
                Item item = createItemFromCursor(cursor);
                itemList.add(item);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        Log.d(TAG, "getItemsByParentListId: Parent ID " + parentListId + " has " + itemList.size() + " items.");
        return itemList;
    }

    public List<Item> getTodoItemsByParentListId(int parentListId) {
        List<Item> todoItemList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_TODO_LIST + " WHERE " + COLUMN_TODO_PARENT_LIST_ID + " = ?";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[]{String.valueOf(parentListId)});

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TODO_ID));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TODO_NAME));
                boolean isChecked = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TODO_IS_CHECKED)) == 1;
                Item todoItem = new Item(id, name, 0, isChecked, null);
                todoItem.setParentListId(parentListId);
                todoItemList.add(todoItem);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        Log.d(TAG, "getTodoItemsByParentListId: Parent ID " + parentListId + " has " + todoItemList.size() + " todo items.");
        return todoItemList;
    }

    public Item getItemById(int itemId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_ITEM_LIST, null, COLUMN_ITEM_LIST_INTERNAL_ID + " = ?", new String[]{String.valueOf(itemId)}, null, null, null);
        Item item = null;
        if (cursor.moveToFirst()) {
            item = createItemFromCursor(cursor);
            Log.d(TAG, "getItemById: Found item with ID: " + itemId + ", Name: " + item.getName());
        } else {
            Log.d(TAG, "getItemById: No item found with ID: " + itemId);
        }
        cursor.close();
        db.close();
        return item;
    }

    public int updateTodoItem(Item todoItem) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TODO_NAME, todoItem.getName());
        values.put(COLUMN_TODO_IS_CHECKED, todoItem.isAddButton() ? 1 : 0);

        int rowsAffected = db.update(TABLE_TODO_LIST, values, COLUMN_TODO_ID + " = ?",
                new String[]{String.valueOf(todoItem.getId())});
        db.close();
        Log.d(TAG, "updateTodoItem: Todo item ID " + todoItem.getId() + " updated, rows affected: " + rowsAffected);
        return rowsAffected;
    }

    public int updateItem(Item item) {
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

        int rowsAffected = db.update(TABLE_ITEM_LIST, values, COLUMN_ITEM_LIST_INTERNAL_ID + " = ?",
                new String[]{String.valueOf(item.getId())});
        db.close();
        Log.d(TAG, "updateItem: Item ID " + item.getId() + " updated, rows affected: " + rowsAffected);
        return rowsAffected;
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

    public void deleteItem(int itemId) {
        SQLiteDatabase db = this.getWritableDatabase();
        String imagePathToDelete = null;
        Cursor cursor = db.query(TABLE_ITEM_LIST,
                new String[]{COLUMN_ITEM_LIST_IMAGE_DATA},
                COLUMN_ITEM_LIST_INTERNAL_ID + " = ?",
                new String[]{String.valueOf(itemId)},
                null, null, null);

        if (cursor.moveToFirst()) {
            imagePathToDelete = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ITEM_LIST_IMAGE_DATA));
        }
        cursor.close();

        int rowsAffected = db.delete(TABLE_ITEM_LIST, COLUMN_ITEM_LIST_INTERNAL_ID + " = ?", new String[]{String.valueOf(itemId)});
        db.close();
        Log.d(TAG, "deleteItem: Item ID " + itemId + " deleted from DB. Rows affected: " + rowsAffected);

        if (context != null && imagePathToDelete != null && !imagePathToDelete.isEmpty()) {
            File file = new File(context.getFilesDir(), imagePathToDelete);
            if (file.exists()) {
                if (file.delete()) {
                    Log.d(TAG, "deleteItem: Deleted image file: " + file.getAbsolutePath());
                } else {
                    Log.e(TAG, "deleteItem: Failed to delete image file: " + file.getAbsolutePath());
                }
            } else {
                Log.d(TAG, "deleteItem: Image file not found for item ID " + itemId + ": " + file.getAbsolutePath());
            }
        } else if (context == null) {
            Log.e(TAG, "deleteItem: Context is null, cannot delete image file for item ID: " + itemId);
        }
    }

    public void deleteTodoItem(int todoId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsAffected = db.delete(TABLE_TODO_LIST, COLUMN_TODO_ID + " = ?", new String[]{String.valueOf(todoId)});
        db.close();
        Log.d(TAG, "deleteTodoItem: Todo item ID " + todoId + " deleted from DB. Rows affected: " + rowsAffected);
    }

    private Item createItemFromCursor(Cursor cursor) {
        int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ITEM_LIST_INTERNAL_ID));
        String itemListId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ITEM_LIST_ITEM_ID));
        String name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ITEM_LIST_NAME));
        int count = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ITEM_LIST_COUNT));
        boolean isAddButton = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ITEM_LIST_IS_ADD_BUTTON)) == 1;
        String date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ITEM_LIST_DATE));
        String description = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ITEM_LIST_DESCRIPTION));
        String category = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ITEM_LIST_CATEGORY));
        String imageData = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ITEM_LIST_IMAGE_DATA));
        double price = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_ITEM_LIST_PRICE));
        String discountsJson = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ITEM_LIST_DISCOUNTS_JSON));
        double finalPrice = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_ITEM_LIST_FINAL_PRICE));
        int retrievedParentListId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ITEM_LIST_PARENT_LIST_ID));
        double totalDiscountPercentage = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_ITEM_LIST_TOTAL_DISCOUNT_PERCENTAGE));

        Item item = new Item(id, name, count, isAddButton, date);
        item.setDescription(description);
        item.setCategory(category);
        item.setImageData(imageData);
        item.setPrice(price);
        item.setDiscountsJson(discountsJson);
        item.setFinalPrice(finalPrice);
        item.setItemListId(itemListId);
        item.setParentListId(retrievedParentListId);
        item.setTotalDiscountPercentage(totalDiscountPercentage);
        return item;
    }
}