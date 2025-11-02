package com.example.econ2;

public class ApiConfig {

    // ✅ Base URL — change this once if your server IP or domain changes
    public static final String BASE_URL = "YOUR_BASE_URL_HERE";

    // ✅ Auth / Mailer endpoints
    public static final String LOGIN = BASE_URL + "mailer/login.php";
    public static final String VALID_MAILER = BASE_URL + "mailer/validmailer.php";

    // ✅ Product-related endpoints
    public static final String PRODUCT_BASE = BASE_URL + "product/";
    public static final String ADD_PRODUCT = PRODUCT_BASE + "add_product.php";
    public static final String GET_PRODUCTS = PRODUCT_BASE + "get_products.php";
    public static final String BUY_PRODUCT = PRODUCT_BASE + "buy.php";
    public static final String PAYMENT_CHECK = PRODUCT_BASE + "payment/check_payment_status.php";
    public static final String RETURN_PAGE = PRODUCT_BASE + "payment/return_page.php";

    private ApiConfig() {
        // Prevent instantiation
    }
}
