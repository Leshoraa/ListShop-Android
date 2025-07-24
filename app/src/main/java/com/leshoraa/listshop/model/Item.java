package com.leshoraa.listshop.model;

import org.json.JSONArray;
import org.json.JSONException;

public class Item {
    private int id;
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
    private String itemListId;
    private int parentListId;
    private int order;
    private double totalDiscountPercentage;

    public Item(int id, String name, int count, boolean isAddButton, String date) {
        this.id = id;
        this.name = name;
        this.count = count;
        this.isAddButton = isAddButton;
        this.date = date;
    }

    public Item(String name, int count, boolean isAddButton, String date) {
        this.name = name;
        this.count = count;
        this.isAddButton = isAddButton;
        this.date = date;
    }

    public Item(int id, String name, int count, boolean isAddButton, String date, int order) {
        this.id = id;
        this.name = name;
        this.count = count;
        this.isAddButton = isAddButton;
        this.date = date;
        this.order = order;
    }

    public void recalculateFinalPrice() {
        double basePrice = this.price * this.count;
        double currentPrice = basePrice;

        if (this.discountsJson != null && !this.discountsJson.isEmpty()) {
            try {
                JSONArray discounts = new JSONArray(this.discountsJson);
                for (int i = 0; i < discounts.length(); i++) {
                    double discountPercentage = discounts.getDouble(i);
                    if (discountPercentage > 0) {
                        currentPrice -= (currentPrice * (discountPercentage / 100.0));
                    }
                }
            } catch (JSONException e) {
                currentPrice = basePrice;
            }
        }
        this.finalPrice = Math.max(0, currentPrice);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
    public boolean isAddButton() { return isAddButton; }
    public void setAddButton(boolean addButton) { isAddButton = addButton; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public void setOrder(int order) { this.order = order; }
    public int getOrder() { return order; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getImageData() { return imageData; }
    public void setImageData(String imageData) { this.imageData = imageData; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public String getDiscountsJson() { return discountsJson; }
    public void setDiscountsJson(String discountsJson) { this.discountsJson = discountsJson; }
    public double getFinalPrice() { return finalPrice; }
    public void setFinalPrice(double finalPrice) { this.finalPrice = finalPrice; }
    public String getItemListId() { return itemListId; }
    public double getTotalDiscountPercentage() { return totalDiscountPercentage; }
    public void setTotalDiscountPercentage(double totalDiscountPercentage) { this.totalDiscountPercentage = totalDiscountPercentage; }
    public void setItemListId(String itemListId) { this.itemListId = itemListId; }
    public int getParentListId() { return parentListId; }
    public void setParentListId(int parentListId) { this.parentListId = parentListId; }
}