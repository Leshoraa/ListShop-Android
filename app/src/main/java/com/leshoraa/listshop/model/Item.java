package com.leshoraa.listshop.model;

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

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public boolean isAddButton() {
        return isAddButton;
    }

    public void setAddButton(boolean addButton) {
        isAddButton = addButton;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getImageData() {
        return imageData;
    }

    public void setImageData(String imageData) {
        this.imageData = imageData;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getDiscountsJson() {
        return discountsJson;
    }

    public void setDiscountsJson(String discountsJson) {
        this.discountsJson = discountsJson;
    }

    public double getFinalPrice() {
        return finalPrice;
    }

    public void setFinalPrice(double finalPrice) {
        this.finalPrice = finalPrice;
    }

    public String getItemListId() {
        return itemListId;
    }

    public double getTotalDiscountPercentage() {
        return totalDiscountPercentage;
    }

    public void setTotalDiscountPercentage(double totalDiscountPercentage) {
        this.totalDiscountPercentage = totalDiscountPercentage;
    }

    public void setItemListId(String itemListId) {
        this.itemListId = itemListId;
    }

    public int getParentListId() {
        return parentListId;
    }

    public void setParentListId(int parentListId) {
        this.parentListId = parentListId;
    }
}