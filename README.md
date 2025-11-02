# Econ2 - Android E-commerce App

## Description

An Android e-commerce app for buying and selling products. Implements user authentication, product browsing, cart management, and secure payment checkout.

## Features

*   **User Authentication:** Registration and login functionality using email and password.
*   **Product Catalog:** Browse a grid-based display of available products.
*   **Shopping Cart:** Add products to a cart and manage quantities.
*   **Payment Integration:** Integrated with [Name of Payment Gateway, e.g., Chapa] for secure online payments.
*   **REST API Integration:** Fetches product data and processes transactions via REST APIs.
*   **Image Handling:** Displays product images fetched from remote URLs.
*   **Foreground Toast Notifications:** In-app notifications for payment status and other events.

## Technologies

*   **Android SDK:** Core Android development framework.
*   **Java:** Primary programming language.
*   **REST APIs:** Communication with the backend server.
*   **chapa:** (e.g., Chapa) Payment gateway integration for processing transactions.
*   **Volley:** HTTP library for making network requests.
*   **Glide/Picasso:** Image loading and caching libraries.
*   **Shared Preferences:** For storing user login state.

## Setup Instructions

1.  **Open the project in Android Studio.**
2.  **Configure the `ApiConfig.java` file:**

    *   Replace `YOUR_BASE_URL_HERE` with the actual base URL of your backend server.  **Important:** Ensure the backend server is running and accessible.
3.  **[If applicable, describe any specific backend setup required, e.g., database configuration].**
4.  **Build and run the app on an Android emulator or physical device.**

## API Endpoints

The app communicates with the following API endpoints:

*   `[BASE_URL]/mailer/login.php`: User login.
*   `[BASE_URL]/mailer/validmailer.php`: User registration and verification.
*   `[BASE_URL]/product/get_products.php`: Retrieve product catalog.
*   `[BASE_URL]/product/add_product.php`: Add a new product (admin functionality).
*   `[BASE_URL]/product/buy.php`: Initiate payment checkout.
*   `[BASE_URL]/product/payment/check_payment_status.php`: Check the status of a payment transaction.
*   `[BASE_URL]/product/payment/return_page.php`: Redirection after the payment process (success or failure).

   **Note:** Replace `[BASE_URL]` with your actual base URL.

## Security Considerations

*   **Important:** This app stores user passwords in `SharedPreferences`, which is **not a secure practice**.  For a production environment, use a more robust method like encrypting the password before storing it or utilizing Android's AccountManager.
*   The `MainActivity.java` file retrieves the user's email from `SharedPreferences` for use in the checkout process. Ensure that access to `SharedPreferences` is properly protected.
*   The code includes fallback URLs for payment processing. Ensure that these URLs are secure and properly configured.



## Disclaimer

This is a basic e-commerce application intended for demonstration and educational purposes. It may not be suitable for production use without further security enhancements and testing.
