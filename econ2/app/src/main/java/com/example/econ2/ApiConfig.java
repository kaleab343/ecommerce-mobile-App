package com.example.econ2;

public class ApiConfig {
    // 👇 Just change this one line if your server IP or domain changes
    public static final String BASE_URL = "http://192.168.1.2/chapa/product/";

    public static final String GET_PRODUCTS = BASE_URL + "get_products.php";
    public static final String ADD_PRODUCT  = BASE_URL + "add_product.php";
    public static final String BUY_PRODUCT  = BASE_URL + "buy.php";
}
