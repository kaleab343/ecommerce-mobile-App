package com.example.econ2;

import android.graphics.Bitmap;

public class Product {
    private int id;          // <-- Add this
    private String name;
    private String price;
    private Bitmap image;

    // Constructor including id
    public Product(int id, String name, String price, Bitmap image) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.image = image;
    }

    // Old constructor without id (if needed)
    public Product(String name, String price, Bitmap image) {
        this.name = name;
        this.price = price;
        this.image = image;
    }

    // Getter for id
    public int getId() {
        return id;
    }

    // Getters for other fields
    public String getName() {
        return name;
    }

    public String getPrice() {
        return price;
    }

    public Bitmap getImage() {
        return image;
    }
}
