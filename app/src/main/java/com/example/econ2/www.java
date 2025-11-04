// Improved Android solution with better error handling and debugging
// Replace your existing www.java with this code

package com.example.econ2;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import android.os.AsyncTask;
import android.os.Handler;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Set;
import org.json.JSONObject;
import org.json.JSONException;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class www extends AppCompatActivity {
    private WebView webView;
    private static final String TAG = "wwwActivity";
    private String currentTxRef = null; // Store the tx_ref from payment initialization

    // Multiple server endpoints to try
    private static final String[] SERVER_ENDPOINTS = {
            ApiConfig.PRODUCT_BASE + "payment/",// Primary local IP
            "http://localhost/chapa/product/payment/",    // Localhost fallback
            "http://10.0.2.2/chapa/product/payment/",     // Android emulator fallback
            "http://127.0.0.1/chapa/product/payment/"    // Another localhost variant
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_www);

        // Apply padding for system UI (status/navigation bars)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        webView = findViewById(R.id.webView);

        // Configure WebView to intercept navigation and handle redirects
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                Log.d(TAG, "Loading URL: " + url);

                // Check if this is a redirect to any return page
                if (url.contains("return_page.php") || url.contains("/product/payment/return") || url.contains("/product/payment/callback")) {
                    // Extract tx_ref from URL
                    String txRef = extractTxRefFromUrl(url);
                    Log.d(TAG, "Payment redirect detected, URL: " + url + ", tx_ref: " + txRef);

                    if (txRef != null) {
                        // Instead of loading the page, poll the API directly
                        checkPaymentStatusWithFallback(txRef);
                        return true; // Prevent WebView from loading the redirect
                    } else if (currentTxRef != null && !currentTxRef.isEmpty()) {
                        // Use stored tx_ref if URL doesn't have one
                        Log.d(TAG, "Using stored currentTxRef: " + currentTxRef);
                        checkPaymentStatusWithFallback(currentTxRef);
                        return true; // Prevent WebView from loading the redirect
                    } else {
                        // If no tx_ref found anywhere, try to get it from the most recent payment
                        Log.e(TAG, "No tx_ref found in return URL or stored: " + url);
                        Log.e(TAG, "URL details - Path: " + Uri.parse(url).getPath() + ", Query: " + Uri.parse(url).getQuery());

                        // Try to get the latest payment status without tx_ref
                        checkLatestPaymentStatusWithFallback();
                        return true; // Prevent WebView from loading the redirect
                    }
                }

                // Also check for ngrok URLs that might be return pages
                if (url.contains("ngrok") && (url.contains("return") || url.contains("callback"))) {
                    String txRef = extractTxRefFromUrl(url);
                    Log.d(TAG, "Ngrok return page detected, URL: " + url + ", tx_ref: " + txRef);

                    if (txRef != null) {
                        checkPaymentStatusWithFallback(txRef);
                        return true; // Prevent WebView from loading the redirect
                    }
                }

                // For other URLs, load normally
                return false; // Let WebView handle the URL normally
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d(TAG, "Page finished loading: " + url);

                // Check if we're on a return page and extract tx_ref (backup method)
                if (url.contains("return_page.php") || url.contains("/product/payment/return") || url.contains("/product/payment/callback") ||
                        (url.contains("ngrok") && (url.contains("return") || url.contains("callback")))) {
                    String txRef = extractTxRefFromUrl(url);
                    if (txRef != null) {
                        Log.d(TAG, "Payment return page loaded, tx_ref: " + txRef);
                        checkPaymentStatusWithFallback(txRef);
                    }
                }

                // Also try to extract tx_ref from checkout URL when it first loads
                if (url.contains("checkout.chapa.co") && currentTxRef == null) {
                    String txRef = extractTxRefFromUrl(url);
                    if (txRef != null) {
                        currentTxRef = txRef;
                        Log.d(TAG, "Extracted tx_ref from checkout URL: " + txRef);
                    }
                }
            }
        });

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);

        // Handle intent (can come from payment start or webhook deep link)
        Log.d(TAG, "onCreate: calling handleWebhookIntent from onCreate");
        handleWebhookIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent: calling handleWebhookIntent from onNewIntent");
        setIntent(intent); // Update the intent in case it was a deep link
        handleWebhookIntent(intent);
    }

    private void handleWebhookIntent(Intent intent) {
        Log.d(TAG, "handleWebhookIntent: started");
        Uri data = intent.getData();

        if (data != null && "econ2".equalsIgnoreCase(data.getScheme())) {
            // Deep link triggered (from webhook)
            Log.d(TAG, "handleWebhookIntent: Received deep link with scheme econ2");
            String status = data.getQueryParameter("status");

            Log.d(TAG, "handleWebhookIntent: Status from deep link: " + status);

            if (status != null) {
                switch (status.toLowerCase()) {
                    case "success":
                        Log.d(TAG, "handleWebhookIntent: Payment Successful");
                        Toast.makeText(this, "✅ Payment Successful!", Toast.LENGTH_LONG).show();

                        // Wait 3 seconds then go back to MainActivity
                        new android.os.Handler().postDelayed(() -> {
                            Intent mainIntent = new Intent(www.this, MainActivity.class);
                            mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            startActivity(mainIntent);
                            finish();
                        }, 3000); // 3 seconds delay
                        break;
                    case "failed":
                        Log.d(TAG, "handleWebhookIntent: Payment Failed");
                        Toast.makeText(this, "❌ Payment Failed!", Toast.LENGTH_LONG).show();

                        // Wait 3 seconds then go back to MainActivity
                        new android.os.Handler().postDelayed(() -> {
                            Intent mainIntent = new Intent(www.this, MainActivity.class);
                            mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            startActivity(mainIntent);
                            finish();
                        }, 3000); // 3 seconds delay
                        break;
                    default:
                        Log.d(TAG, "handleWebhookIntent: Unknown Payment Status: " + status);
                        SafeToast.show(this, "ℹ️ Payment Status: " + status, Toast.LENGTH_LONG);

                        // Wait 3 seconds then go back to MainActivity
                        new android.os.Handler().postDelayed(() -> {
                            Intent mainIntent = new Intent(www.this, MainActivity.class);
                            mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            startActivity(mainIntent);
                            finish();
                        }, 3000); // 3 seconds delay
                        break;
                }
            } else {
                Log.w(TAG, "handleWebhookIntent: No status received in webhook deep link");
                SafeToast.show(this, "No status received in webhook.", Toast.LENGTH_LONG);
            }

        } else {
            // Normal start (user opened checkout page)
            Log.d(TAG, "handleWebhookIntent: Not a deep link");
            String url = intent.getStringExtra("url");
            Log.d(TAG, "handleWebhookIntent: URL from intent: " + url);
            if (url != null && !url.isEmpty()) {
                // Store tx_ref if it's in the URL
                currentTxRef = extractTxRefFromUrl(url);
                Log.d(TAG, "Stored currentTxRef: " + currentTxRef);
                webView.loadUrl(url); // Load any external URL inside WebView
            } else {
                Log.w(TAG, "handleWebhookIntent: No URL found to load.");
                SafeToast.show(this, "No URL found to load.", Toast.LENGTH_LONG);
            }
        }
    }

    private String extractTxRefFromUrl(String url) {
        try {
            Log.d(TAG, "Extracting tx_ref from URL: " + url);

            Uri uri = Uri.parse(url);

            // Method 1: Check query parameters
            String txRef = uri.getQueryParameter("tx_ref");
            if (txRef != null && !txRef.isEmpty()) {
                Log.d(TAG, "Found tx_ref in query parameter: " + txRef);
                return txRef;
            }

            // Method 2: Check all query parameters for any that might contain tx_ref
            Set<String> paramNames = uri.getQueryParameterNames();
            for (String paramName : paramNames) {
                Log.d(TAG, "Found parameter: " + paramName + " = " + uri.getQueryParameter(paramName));
                if (paramName.toLowerCase().contains("tx") || paramName.toLowerCase().contains("ref")) {
                    String value = uri.getQueryParameter(paramName);
                    if (value != null && !value.isEmpty()) {
                        Log.d(TAG, "Found potential tx_ref in parameter " + paramName + ": " + value);
                        return value;
                    }
                }
            }

            // Method 3: Manual string parsing for tx_ref=
            if (url.contains("tx_ref=")) {
                String[] parts = url.split("tx_ref=");
                if (parts.length > 1) {
                    String txRefPart = parts[1];
                    if (txRefPart.contains("&")) {
                        txRefPart = txRefPart.split("&")[0];
                    }
                    if (txRefPart.contains("#")) {
                        txRefPart = txRefPart.split("#")[0];
                    }
                    if (!txRefPart.isEmpty()) {
                        Log.d(TAG, "Found tx_ref in URL string: " + txRefPart);
                        return txRefPart;
                    }
                }
            }

            // Method 4: Check for common Chapa parameter names
            String[] commonParams = {"reference", "ref", "transaction_id", "transaction_ref", "payment_ref"};
            for (String param : commonParams) {
                String value = uri.getQueryParameter(param);
                if (value != null && !value.isEmpty()) {
                    Log.d(TAG, "Found " + param + " parameter: " + value);
                    return value;
                }
            }

            // Method 5: Check fragment
            String fragment = uri.getFragment();
            if (fragment != null && fragment.contains("tx_ref=")) {
                String[] parts = fragment.split("tx_ref=");
                if (parts.length > 1) {
                    String txRefPart = parts[1];
                    if (txRefPart.contains("&")) {
                        txRefPart = txRefPart.split("&")[0];
                    }
                    if (!txRefPart.isEmpty()) {
                        Log.d(TAG, "Found tx_ref in fragment: " + txRefPart);
                        return txRefPart;
                    }
                }
            }

            // Method 6: If no tx_ref found, try to extract from the path
            String path = uri.getPath();
            if (path != null && path.contains("return_page.php")) {
                // For return_page.php, we might need to get tx_ref from the previous page or session
                // Let's try to get it from the currentTxRef if available
                if (currentTxRef != null && !currentTxRef.isEmpty()) {
                    Log.d(TAG, "Using stored currentTxRef: " + currentTxRef);
                    return currentTxRef;
                }
            }

            Log.w(TAG, "No tx_ref found in URL: " + url);
            Log.w(TAG, "URL path: " + uri.getPath());
            Log.w(TAG, "URL query: " + uri.getQuery());
            Log.w(TAG, "URL fragment: " + uri.getFragment());
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error extracting tx_ref from URL: " + e.getMessage());
            return null;
        }
    }

    private void checkPaymentStatusWithFallback(String txRef) {
        Log.d(TAG, "Checking payment status for tx_ref: " + txRef);

        // Show loading toast
        SafeToast.show(this, "⏳ Checking payment status...", Toast.LENGTH_SHORT);

        // Try each server endpoint
        new PaymentStatusTaskWithFallback().execute(txRef);
    }

    private void checkLatestPaymentStatusWithFallback() {
        Log.d(TAG, "Checking latest payment status (no tx_ref available)");

        // Show loading toast
        SafeToast.show(this, "⏳ Checking latest payment...", Toast.LENGTH_SHORT);

        // Try each server endpoint
        new LatestPaymentStatusTaskWithFallback().execute();
    }

    private class PaymentStatusTaskWithFallback extends AsyncTask<String, Void, PaymentStatusResult> {
        @Override
        protected PaymentStatusResult doInBackground(String... txRefs) {
            String txRef = txRefs[0];

            for (String baseUrl : SERVER_ENDPOINTS) {
                try {
                    String apiUrl = baseUrl + "check_payment_status.php?tx_ref=" + txRef;
                    Log.d(TAG, "Trying API URL: " + apiUrl);

                    URL url = new URL(apiUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(10000); // 10 seconds timeout
                    connection.setReadTimeout(10000);
                    connection.setRequestProperty("User-Agent", "Econ2App/1.0");

                    int responseCode = connection.getResponseCode();
                    Log.d(TAG, "API response code: " + responseCode + " for URL: " + apiUrl);

                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        reader.close();

                        String responseStr = response.toString();
                        Log.d(TAG, "API response: " + responseStr);

                        return new PaymentStatusResult(true, responseStr, apiUrl);
                    } else {
                        Log.w(TAG, "API request failed with code: " + responseCode + " for URL: " + apiUrl);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error checking payment status with URL " + baseUrl + ": " + e.getMessage());
                }
            }

            return new PaymentStatusResult(false, null, "All endpoints failed");
        }

        @Override
        protected void onPostExecute(PaymentStatusResult result) {
            if (result.success && result.response != null) {
                Log.d(TAG, "Payment status API call successful, processing response...");
                handlePaymentResponse(result.response);
            } else {
                Log.e(TAG, "Failed to get payment status from all endpoints");
                SafeToast.show(www.this, "❌ Error: Cannot connect to payment server. Please check your network connection.", Toast.LENGTH_LONG);

                // Show debug info
                Log.e(TAG, "Debug info - Tried endpoints:");
                for (String endpoint : SERVER_ENDPOINTS) {
                    Log.e(TAG, "  - " + endpoint + "check_payment_status.php");
                }
            }
        }
    }

    private class LatestPaymentStatusTaskWithFallback extends AsyncTask<Void, Void, PaymentStatusResult> {
        @Override
        protected PaymentStatusResult doInBackground(Void... voids) {
            for (String baseUrl : SERVER_ENDPOINTS) {
                try {
                    String apiUrl = baseUrl + "check_payment_status.php?latest=1";
                    Log.d(TAG, "Trying latest payment API URL: " + apiUrl);

                    URL url = new URL(apiUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(10000);
                    connection.setReadTimeout(10000);
                    connection.setRequestProperty("User-Agent", "Econ2App/1.0");

                    int responseCode = connection.getResponseCode();
                    Log.d(TAG, "Latest payment API response code: " + responseCode + " for URL: " + apiUrl);

                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        reader.close();

                        String responseStr = response.toString();
                        Log.d(TAG, "Latest payment API response: " + responseStr);

                        return new PaymentStatusResult(true, responseStr, apiUrl);
                    } else {
                        Log.w(TAG, "Latest payment API request failed with code: " + responseCode + " for URL: " + apiUrl);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error checking latest payment status with URL " + baseUrl + ": " + e.getMessage());
                }
            }

            return new PaymentStatusResult(false, null, "All endpoints failed");
        }

        @Override
        protected void onPostExecute(PaymentStatusResult result) {
            if (result.success && result.response != null) {
                Log.d(TAG, "Latest payment status API call successful, processing response...");
                handlePaymentResponse(result.response);
            } else {
                Log.e(TAG, "Failed to get latest payment status from all endpoints");
                SafeToast.show(www.this, "❌ Error: Cannot connect to payment server. Please check your network connection.", Toast.LENGTH_LONG);
            }
        }
    }

    private static class PaymentStatusResult {
        boolean success;
        String response;
        String endpoint;

        PaymentStatusResult(boolean success, String response, String endpoint) {
            this.success = success;
            this.response = response;
            this.endpoint = endpoint;
        }
    }

    private void handlePaymentResponse(String jsonResponse) {
        try {
            JSONObject response = new JSONObject(jsonResponse);
            boolean success = response.optBoolean("success", false);
            String status = response.optString("status", "unknown");
            String message = response.optString("message", "");
            String txRef = response.optString("tx_ref", "");

            Log.d(TAG, "Payment status: " + status + ", success: " + success + ", message: " + message);

            if (success) {
                if ("success".equalsIgnoreCase(status)) {
                    SafeToast.show(this, "✅ " + message, Toast.LENGTH_LONG);
                    Log.d(TAG, "Payment successful - showing toast and returning to MainActivity in 3 seconds");

                    // Wait 3 seconds then go back to MainActivity
                    new android.os.Handler().postDelayed(() -> {
                        Intent intent = new Intent(www.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                        finish();
                    }, 3000); // 3 seconds delay

                } else if ("failed".equalsIgnoreCase(status)) {
                    SafeToast.show(this, "❌ " + message, Toast.LENGTH_LONG);
                    Log.d(TAG, "Payment failed - showing toast and returning to MainActivity in 3 seconds");

                    // Wait 3 seconds then go back to MainActivity
                    new android.os.Handler().postDelayed(() -> {
                        Intent intent = new Intent(www.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                        finish();
                    }, 3000); // 3 seconds delay

                } else if ("pending".equalsIgnoreCase(status)) {
                    SafeToast.show(this, "⏳ " + message, Toast.LENGTH_LONG);
                    Log.d(TAG, "Payment pending - showing toast and returning to MainActivity in 3 seconds");

                    // Wait 3 seconds then go back to MainActivity
                    new android.os.Handler().postDelayed(() -> {
                        Intent intent = new Intent(www.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                        finish();
                    }, 3000); // 3 seconds delay

                } else {
                    SafeToast.show(this, "⚠ " + message, Toast.LENGTH_LONG);
                    Log.d(TAG, "Payment status unknown - showing toast and returning to MainActivity in 3 seconds");

                    // Wait 3 seconds then go back to MainActivity
                    new android.os.Handler().postDelayed(() -> {
                        Intent intent = new Intent(www.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                        finish();
                    }, 3000); // 3 seconds delay
                }
            } else {
                String error = response.optString("error", "Unknown error");
                SafeToast.show(this, "❌ Error: " + error, Toast.LENGTH_LONG);
                Log.d(TAG, "Payment error - showing toast and returning to MainActivity in 3 seconds");

                // Wait 3 seconds then go back to MainActivity
                new android.os.Handler().postDelayed(() -> {
                    Intent intent = new Intent(www.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                }, 3000); // 3 seconds delay
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing payment response: " + e.getMessage());
            SafeToast.show(this, "Error processing payment response", Toast.LENGTH_LONG);
        }
    }

    @Override
    public void onBackPressed() {
        // Go back to MainActivity when user presses Back
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
