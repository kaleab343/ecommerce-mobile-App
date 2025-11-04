package com.example.econ2;

import java.util.ArrayList;
import java.util.List;

public class Recite {
    private String id;
    private String title;
    private String content;
    private String dateCreated;
    private String userId;
    private String category;
    private String imageUrl;
    private String productName;
    private String totalAmount;
    private String currency;
    private String paymentStatus;
    private String txRef;
    private List<OrderItem> items;
    private int totalItems;

    // Default constructor
    public Recite() {
        this.items = new ArrayList<>();
        this.totalItems = 0;
    }

    // Constructor with all parameters
    public Recite(String id, String title, String content, String dateCreated, String userId, String category) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.dateCreated = dateCreated;
        this.userId = userId;
        this.category = category;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getDateCreated() {
        return dateCreated;
    }

    public String getUserId() {
        return userId;
    }

    public String getCategory() {
        return category;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setDateCreated(String dateCreated) {
        this.dateCreated = dateCreated;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    // Additional getters for order/payment fields
    public String getImageUrl() {
        return imageUrl;
    }

    public String getProductName() {
        return productName;
    }

    public String getTotalAmount() {
        return totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public String getTxRef() {
        return txRef;
    }

    // Additional setters for order/payment fields
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public void setTotalAmount(String totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public void setTxRef(String txRef) {
        this.txRef = txRef;
    }

    // Getters and setters for new fields
    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
        this.totalItems = items != null ? items.size() : 0;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(int totalItems) {
        this.totalItems = totalItems;
    }

    // Helper method to add an item
    public void addItem(OrderItem item) {
        if (this.items == null) {
            this.items = new ArrayList<>();
        }
        this.items.add(item);
        this.totalItems = this.items.size();
    }
}