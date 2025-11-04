package com.example.econ2;

public class Product {
    private int id;
    private String name;
    private String price;
    private String imageUrl; // store image URL instead of Bitmap

    public Product(int id, String name, String price, String imageUrl) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.imageUrl = imageUrl;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getPrice() { return price; }
    public String getImageUrl() { return imageUrl; }
}
