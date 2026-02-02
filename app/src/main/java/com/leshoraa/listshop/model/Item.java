package com.leshoraa.listshop.model;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.Serializable;

public class Item implements Serializable {
    private int id;
    private String itemListId;
    private String name;
    private int count;
    private boolean isAddButton;
    private String date;
    private String description;
    private String category;
    private String imageData;
    private double price;
    private String discountsJson;
    private double finalPrice;
    private int parentListId;
    private double totalDiscountPercentage;
    private int order;

    // Constructor standar
    public Item(String name, int count, boolean isAddButton, String date) {
        this.name = name;
        this.count = count;
        this.isAddButton = isAddButton;
        this.date = date;
        this.itemListId = String.valueOf(System.currentTimeMillis());
    }

    // Constructor untuk DatabaseHelper (saat load dari DB)
    public Item(int id, String name, int count, boolean isAddButton, String date) {
        this.id = id;
        this.name = name;
        this.count = count;
        this.isAddButton = isAddButton;
        this.date = date;
    }

    public void recalculateFinalPrice() {
        // baseTotalPrice is the price of ONE item
        double unitPrice = this.price;

        double totalDiscountPercent = 0.0;
        if (this.discountsJson != null && !this.discountsJson.isEmpty()) {
            try {
                JSONArray jsonArray = new JSONArray(this.discountsJson);
                for (int i = 0; i < jsonArray.length(); i++) {
                    totalDiscountPercent += jsonArray.getDouble(i);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if (totalDiscountPercent > 100) totalDiscountPercent = 100;
        this.totalDiscountPercentage = totalDiscountPercent;

        double unitDiscountAmount = unitPrice * (totalDiscountPercent / 100.0);
        double unitFinalPrice = unitPrice - unitDiscountAmount;

        // finalPrice should represent the total price for all items in this row
        this.finalPrice = unitFinalPrice * this.count;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getItemListId() { return itemListId; }
    public void setItemListId(String itemListId) { this.itemListId = itemListId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getCount() { return count; }
    public void setCount(int count) {
        this.count = count;
        recalculateFinalPrice();
    }

    public boolean isAddButton() { return isAddButton; }
    public void setAddButton(boolean addButton) { isAddButton = addButton; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getImageData() { return imageData; }
    public void setImageData(String imageData) { this.imageData = imageData; }

    public double getPrice() { return price; }
    public void setPrice(double price) {
        this.price = price;
        recalculateFinalPrice();
    }

    public String getDiscountsJson() { return discountsJson; }
    public void setDiscountsJson(String discountsJson) {
        this.discountsJson = discountsJson;
        recalculateFinalPrice();
    }

    public double getFinalPrice() {
        return finalPrice;
    }

    public void setFinalPrice(double finalPrice) {
        this.finalPrice = finalPrice;
    }

    public int getParentListId() { return parentListId; }
    public void setParentListId(int parentListId) { this.parentListId = parentListId; }

    public double getTotalDiscountPercentage() { return totalDiscountPercentage; }
    public void setTotalDiscountPercentage(double totalDiscountPercentage) {
        this.totalDiscountPercentage = totalDiscountPercentage;
    }

    public int getOrder() { return order; }
    public void setOrder(int order) { this.order = order; }
}
