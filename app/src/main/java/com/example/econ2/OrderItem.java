package com.example.econ2;

public class OrderItem {
    private String productName;
    private String productPrice;
    private String quantity;
    private String subtotal;
    private String imageUrl;
    private String size;

    // Default constructor
    public OrderItem() {
    }

    // Constructor with all parameters
    public OrderItem(String productName, String productPrice, String quantity, 
                     String subtotal, String imageUrl, String size) {
        this.productName = productName;
        this.productPrice = productPrice;
        this.quantity = quantity;
        this.subtotal = subtotal;
        this.imageUrl = imageUrl;
        this.size = size;
    }

    // Getters
    public String getProductName() {
        return productName;
    }

    public String getProductPrice() {
        return productPrice;
    }

    public String getQuantity() {
        return quantity;
    }

    public String getSubtotal() {
        return subtotal;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getSize() {
        return size;
    }

    // Setters
    public void setProductName(String productName) {
        this.productName = productName;
    }

    public void setProductPrice(String productPrice) {
        this.productPrice = productPrice;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public void setSubtotal(String subtotal) {
        this.subtotal = subtotal;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setSize(String size) {
        this.size = size;
    }
}