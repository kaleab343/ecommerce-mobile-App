package com.example.econ2;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    // Add after the existing field declarations
    private boolean isConnectedToAPI = false;
    private boolean isCheckingConnection = false;

    private GridView gridView;
    private List<Product> productList = new ArrayList<>();
    private GridAdapter adapter;

    private FrameLayout overlayContainer;
    private ImageView overlayImage;
    private Button btnBuy;
    private Button btnAddProduct;
    private Button btnAddToCart;
    private FrameLayout progressOverlay;
    private ProgressBar progressBar;
    private TextView progressText;

    // Cart UI
    private FrameLayout cartButtonContainer;
    private ImageView cartButtonImage;
    private TextView cartBadgeText;
    private FrameLayout cartOverlay;
    private LinearLayout cartListContainer;
    private Button cartCheckoutBtn;
    private Button cartCloseBtn;
    private TextView cartTotalText; // total price shown in cart overlay

    // Cart data (simple in-memory list)
    private final List<Product> cartList = new ArrayList<>();

    private int selectedProductId = -1;
    private Product selectedProduct; // currently displayed product in overlay
    private ActivityResultLauncher<Intent> galleryLauncher;
    private Uri selectedImageUri;

    private static final String API_URL_FETCH = ApiConfig.GET_PRODUCTS;
    private static final String API_URL_ADD = ApiConfig.ADD_PRODUCT;
    private static final String API_URL_BUY = ApiConfig.BUY_PRODUCT;
    private static final String APP_URL_SCHEME = "econ2";

    private static final int MAX_IMAGE_WIDTH = 800;
    private static final int MAX_IMAGE_HEIGHT = 800;
    private static final int COMPRESSION_QUALITY = 70;
    private static final int MAX_FILE_SIZE_KB = 500;

    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "user_prefs";
    private static final String KEY_LOGGED_IN = "logged_in";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_EMAIL = "email";

    private String username = "User";
    private String userEmail = "";

    // periodic refresh
    private final Handler refreshHandler = new Handler();
    private final int REFRESH_INTERVAL_MS = 3000;
    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (isConnectedToAPI) {
                fetchProductsFromServer(false);
            } else {
                // Try to reconnect silently
                checkAPIConnectivity();
            }
            refreshHandler.postDelayed(this, REFRESH_INTERVAL_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // ensure activity_main.xml matches expected IDs

        sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        username = sharedPreferences.getString(KEY_USERNAME, "User");
        userEmail = sharedPreferences.getString(KEY_EMAIL, "");

        // find views
        gridView = findViewById(R.id.gridView);
        btnAddProduct = findViewById(R.id.btnAddProduct);
        overlayContainer = findViewById(R.id.overlayContainer);
        overlayImage = findViewById(R.id.overlayImage);
        // XML Add-to-cart button (must exist)
        btnAddToCart = findViewById(R.id.btnAddToCart);
        btnBuy = findViewById(R.id.btnBuy);

        progressOverlay = findViewById(R.id.progress_overlay);
        if (progressOverlay != null) {
            progressBar = progressOverlay.findViewById(R.id.progressBar);
            progressText = progressOverlay.findViewById(R.id.progressText);
        }

        // Cart UI
        cartButtonContainer = findViewById(R.id.cartButtonContainer);
        cartButtonImage = findViewById(R.id.cartButtonImage);
        cartBadgeText = findViewById(R.id.cartBadgeText);
        cartOverlay = findViewById(R.id.cartOverlay);
        cartListContainer = findViewById(R.id.cartListContainer);
        cartCheckoutBtn = findViewById(R.id.cartCheckoutBtn);
        cartCloseBtn = findViewById(R.id.cartCloseBtn);

        // Optional: try to find a TextView with id cartTotalText if present in XML.
        View maybeTotal = (cartOverlay != null) ? cartOverlay.findViewById(R.id.cartTotalText) : null;
        if (maybeTotal instanceof TextView) {
            cartTotalText = (TextView) maybeTotal;
        } else {
            cartTotalText = null; // we'll create programmatically when showing the cart if needed
        }

        // safety: ensure overlay is hidden initially
        if (overlayContainer != null) overlayContainer.setVisibility(View.GONE);
        if (cartOverlay != null) cartOverlay.setVisibility(View.GONE);

        updateCartBadge();

        // Grid adapter
        adapter = new GridAdapter(this, productList);
        gridView.setAdapter(adapter);
        gridView.setSmoothScrollbarEnabled(true);

        // initial load (show loader)
        checkAPIConnectivity();

        // start silent periodic refresh
        refreshHandler.postDelayed(refreshRunnable, REFRESH_INTERVAL_MS);

        // image picker
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        openAddProductDialog();
                    }
                }
        );

        btnAddProduct.setOnClickListener(v -> {
            if (!isConnectedToAPI) {
                SafeToast.show(this, "❌ Cannot add product: No server connection", Toast.LENGTH_SHORT);
                return;
            }
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryLauncher.launch(intent);
        });

        // User icon popup menu
        ImageView userIcon = findViewById(R.id.userIcon);
        if (userIcon != null) userIcon.setOnClickListener(v -> showUserMenu(v));

        // Grid item click -> show overlay
        gridView.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id) -> {
            if (!isConnectedToAPI) {
                SafeToast.show(this, "❌ Cannot view product: No server connection", Toast.LENGTH_SHORT);
                return;
            }

            // If the cart overlay is visible, close it first (user requested behavior)
            if (cartOverlay != null && cartOverlay.getVisibility() == View.VISIBLE) {
                cartOverlay.setVisibility(View.GONE);
            }
            Product clickedProduct = productList.get(position);
            selectedProductId = clickedProduct.getId();
            selectedProduct = clickedProduct;
            showProductOverlay(clickedProduct);
        });


        // overlay background closes overlay
        if (overlayContainer != null)
            overlayContainer.setOnClickListener(v -> hideProductOverlay());

        // cart button toggles overlay
        if (cartButtonContainer != null) {
            cartButtonContainer.setOnClickListener(v -> {
                if (!isConnectedToAPI) {
                    SafeToast.show(this, "❌ Cannot access cart: No server connection", Toast.LENGTH_SHORT);
                    return;
                }
                toggleCartOverlay();
            });
        }

        // cart checkout - now initiates multi-item checkout
        if (cartCheckoutBtn != null) {
            cartCheckoutBtn.setOnClickListener(v -> {
                if (!isConnectedToAPI) {
                    SafeToast.show(MainActivity.this, "❌ Cannot checkout: No server connection", Toast.LENGTH_SHORT);
                    return;
                }
                if (cartList.isEmpty()) {
                    SafeToast.show(MainActivity.this, "Cart is empty", Toast.LENGTH_SHORT);
                } else {
                    // initiate checkout for the whole cart
                    initiateCheckoutForCart();
                }
            });
        }

        if (cartCloseBtn != null) {
            cartCloseBtn.setOnClickListener(v -> toggleCartOverlay());
        }

        // btnBuy fallback behavior (keeps original single-item buy behavior) -> now uses checkout flow for single item
        if (btnBuy != null) {
            btnBuy.setOnClickListener(v -> {
                if (!isConnectedToAPI) {
                    SafeToast.show(this, "❌ Cannot purchase: No server connection", Toast.LENGTH_SHORT);
                    return;
                }
                initiateCheckoutForSingleSelectedProduct();
            });
        }

        // Add-to-cart button (XML) click handler (will be configured when overlay shown)
        if (btnAddToCart != null) {
            btnAddToCart.setVisibility(View.GONE);
        }
    }

    private void hideProductOverlay() {
        if (overlayContainer != null) {
            overlayContainer.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        refreshHandler.removeCallbacks(refreshRunnable);
    }

    // ---------------- User Menu ----------------
    private void showUserMenu(View anchor) {
        androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(this, anchor);
        popup.getMenu().add(username).setEnabled(false);
        popup.getMenu().add("Logout");

        popup.setOnMenuItemClickListener(item -> {
            if (item.getTitle().equals("Logout")) {
                logoutAndGoToLogin();
            }
            return true;
        });

        popup.show();
    }

    private void logoutAndGoToLogin() {
        sharedPreferences.edit()
                .putBoolean(KEY_LOGGED_IN, false)
                .remove(KEY_USERNAME)
                .remove(KEY_EMAIL)
                .apply();

        SafeToast.show(MainActivity.this, "Logged out!", Toast.LENGTH_SHORT);

        Intent intent = new Intent(MainActivity.this, BlankFragment.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // ---------------- Loader ----------------
    private void showLoading() {
        runOnUiThread(() -> {
            if (progressOverlay != null) progressOverlay.setVisibility(View.VISIBLE);
        });
    }

    private void hideLoading() {
        runOnUiThread(() -> {
            if (progressOverlay != null) progressOverlay.setVisibility(View.GONE);
        });
    }

    // ---------------- API Connectivity Check ----------------
    private void checkAPIConnectivity() {
        if (isCheckingConnection) return;

        isCheckingConnection = true;
        showLoading();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                URL url = new URL(API_URL_FETCH);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setDoOutput(true);

                String postData = "action=get_products";
                OutputStream os = conn.getOutputStream();
                os.write(postData.getBytes());
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                conn.disconnect();

                runOnUiThread(() -> {
                    isCheckingConnection = false;
                    hideLoading();

                    if (responseCode == 200) {
                        isConnectedToAPI = true;
                        enableAllInteractions();
                        fetchProductsFromServer(true);
                    } else {
                        isConnectedToAPI = false;
                        disableAllInteractions();
                        SafeToast.show(
                                MainActivity.this,
                                "⚠️ Cannot connect to server (Code: " + responseCode + ")\n\nApp is locked until connection is restored.",
                                Toast.LENGTH_LONG
                        );

                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    isCheckingConnection = false;
                    hideLoading();
                    isConnectedToAPI = false;
                    disableAllInteractions();
                    SafeToast.show(
                            MainActivity.this,
                            "⚠️ Cannot connect to server\n\n" + e.getMessage() + "\n\nApp is locked until connection is restored.",
                            Toast.LENGTH_LONG
                    );

                    Log.e("API_CONNECTION", "Failed to connect: " + e.getMessage());
                });
            }
        });
    }

    private void disableAllInteractions() {
        runOnUiThread(() -> {
            if (gridView != null) gridView.setEnabled(false);
            if (btnAddProduct != null) btnAddProduct.setEnabled(false);
            if (cartButtonContainer != null) cartButtonContainer.setEnabled(false);
            if (btnBuy != null) btnBuy.setEnabled(false);
            if (btnAddToCart != null) btnAddToCart.setEnabled(false);
            if (cartCheckoutBtn != null) cartCheckoutBtn.setEnabled(false);
        });
    }

    private void enableAllInteractions() {
        runOnUiThread(() -> {
            if (gridView != null) gridView.setEnabled(true);
            if (btnAddProduct != null) btnAddProduct.setEnabled(true);
            if (cartButtonContainer != null) cartButtonContainer.setEnabled(true);
            if (btnBuy != null) btnBuy.setEnabled(true);
            if (btnAddToCart != null) btnAddToCart.setEnabled(true);
            if (cartCheckoutBtn != null) cartCheckoutBtn.setEnabled(true);
        });
    }

    // ---------------- Overlay ----------------
    private void showProductOverlay(Product product) {
        if (overlayContainer == null) return;

        // load image into overlay image safely
        try {
            Picasso.get()
                    .load(product.getImageUrl())
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_delete)
                    .fit()
                    .centerInside()
                    .into(overlayImage);
        } catch (Exception e) {
            // fallback if Picasso fails
            overlayImage.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        // --- Make overlay image bigger safely without forcing wrong LayoutParams type ---
        int desiredDp = 420; // make it bigger visually
        int desiredPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, desiredDp, getResources().getDisplayMetrics());

        ViewGroup.LayoutParams currentLp = overlayImage.getLayoutParams();
        if (currentLp != null) {
            currentLp.width = desiredPx;
            currentLp.height = desiredPx;
            overlayImage.setLayoutParams(currentLp);
        }

        // Make sure image not cropped
        overlayImage.setAdjustViewBounds(true);
        overlayImage.setScaleType(ImageView.ScaleType.FIT_CENTER);

        // Show overlay
        overlayContainer.setVisibility(View.VISIBLE);

        // Ensure Add and Buy are visible side-by-side. We'll create a horizontal container programmatically
        ensureButtonsSideBySide();

        // Configure Add-to-cart button behavior depending on whether product is already in cart
        if (btnAddToCart != null) {
            btnAddToCart.setVisibility(View.VISIBLE);
            if (isProductInCart(product)) {
                btnAddToCart.setText("In cart");
                btnAddToCart.setEnabled(false);
            } else {
                btnAddToCart.setText("Add to cart");
                btnAddToCart.setEnabled(true);
                btnAddToCart.setOnClickListener(v -> {
                    addToCart(product);
                    btnAddToCart.setText("In cart");
                    btnAddToCart.setEnabled(false);
                    SafeToast.show(MainActivity.this, product.getName() + " added to cart", Toast.LENGTH_SHORT);
                    // changed: close overlay after adding, do not auto-open cart overlay
                    hideProductOverlay();
                });
            }
        }

        // Keep buy behavior for the shown product
        if (btnBuy != null) {
            btnBuy.setOnClickListener(v -> initiateCheckoutForSingleSelectedProduct());
        }
    }

    /**
     * Ensure AddToCart and Buy appear side-by-side at bottom of overlay.
     * This manipulates the view hierarchy at runtime — safe and avoids XML changes.
     */
    private void ensureButtonsSideBySide() {
        if (overlayContainer == null || btnBuy == null || btnAddToCart == null) return;

        String markerTag = "btn_horizontal_container";
        View existing = overlayContainer.findViewWithTag(markerTag);
        if (existing != null) return; // already set up

        // create horizontal LinearLayout
        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.HORIZONTAL);
        ll.setTag(markerTag);
        ll.setPadding(12, 12, 12, 12);
        ll.setGravity(Gravity.CENTER);

        // set layout params to be bottom center inside overlay (overlayContainer is FrameLayout)
        FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        flp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        flp.bottomMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, getResources().getDisplayMetrics());

        // remove buttons from their current parents safely
        removeFromParent(btnAddToCart);
        removeFromParent(btnBuy);

        // style buttons width/height to look nice side by side
        LinearLayout.LayoutParams childLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        childLp.setMargins(12, 0, 12, 0);

        // ensure minimum widths so user can tap
        btnAddToCart.setMinWidth((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 120, getResources().getDisplayMetrics()));
        btnBuy.setMinWidth((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 120, getResources().getDisplayMetrics()));

        // add to new container
        ll.addView(btnAddToCart, childLp);
        ll.addView(btnBuy, childLp);

        // finally add container to overlay
        overlayContainer.addView(ll, flp);
    }

    private void removeFromParent(View v) {
        if (v == null) return;
        ViewGroup parent = (ViewGroup) v.getParent();
        if (parent != null) parent.removeView(v);
    }

    // ---------------- Checkout flows ----------------

    /**
     * Called when user clicks "Buy" from the product overlay.
     * Uses the same server-init flow as cart checkout but with a single item.
     */
    private void initiateCheckoutForSingleSelectedProduct() {
        if (selectedProduct == null) {
            SafeToast.show(MainActivity.this, "No product selected", Toast.LENGTH_SHORT);
            return;
        }

        List<Product> items = new ArrayList<>();
        items.add(selectedProduct);
        double total = parsePriceToDouble(selectedProduct.getPrice());

        initiateCheckoutRequest(items, total);
    }

    /**
     * Called when user clicks "Checkout" from the cart overlay.
     * Sends all items + total to the server as a single checkout request.
     */
    private void initiateCheckoutForCart() {
        if (cartList.isEmpty()) {
            SafeToast.show(MainActivity.this, "Cart is empty", Toast.LENGTH_SHORT);
            return;
        }

        // compute total
        double total = 0.0;
        for (Product p : cartList) {
            total += parsePriceToDouble(p.getPrice());
        }

        // send the cartList as items
        initiateCheckoutRequest(new ArrayList<>(cartList), total);
    }

    /**
     * Sends a checkout initiation POST to the server with:
     * - items (JSON array)
     * - total_price
     * - user_email
     * - user_name
     *
     * On success expects: { "status":"success", "checkout_url":"..." }
     */
    /**
     * Sends a checkout initiation POST to the server with:
     * - items (JSON array)
     * - total_price
     * - user_email (from SharedPreferences)
     * - user_name (from SharedPreferences)
     * <p>
     * On success expects: { "status":"success", "checkout_url":"..." }
     */
    private void initiateCheckoutRequest(List<Product> items, double total) {
        showLoading();
        try {
            // ✅ GET EMAIL FROM SHAREDPREFERENCES (LOGGED IN USER'S EMAIL)
            // ✅ GET EMAIL FROM SHAREDPREFERENCES (LOGGED IN USER'S EMAIL)
            String loggedInEmail = sharedPreferences.getString(KEY_EMAIL, "");
            if (loggedInEmail.isEmpty()) {
                hideLoading();
                SafeToast.show(MainActivity.this, "User email not found. Please login again.", Toast.LENGTH_LONG);
                return;
            }

            // build items JSON array
            JSONArray itemsArray = new JSONArray();
            for (Product p : items) {
                JSONObject o = new JSONObject();
                o.put("id", p.getId());
                o.put("name", p.getName());
                o.put("price", p.getPrice());
                itemsArray.put(o);
            }

            final String itemsJson = itemsArray.toString();
            final String totalStr = String.format("%.2f", total);

            // Log what we're sending
            Log.e("CHECKOUT_DEBUG", "Sending to: " + API_URL_BUY);
            Log.e("CHECKOUT_DEBUG", "Items JSON: " + itemsJson);
            Log.e("CHECKOUT_DEBUG", "Total: " + totalStr);
            Log.e("CHECKOUT_DEBUG", "Email: " + loggedInEmail); // ✅ USING LOGGED IN EMAIL
            Log.e("CHECKOUT_DEBUG", "Username: " + username);

            RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
            StringRequest request = new StringRequest(Request.Method.POST, API_URL_BUY,
                    response -> {
                        hideLoading();
                        Log.e("CHECKOUT_RESPONSE", "Raw response: " + response);
                        try {
                            JSONObject json = new JSONObject(response);
                            String status = json.optString("status", "error");
                            if ("success".equalsIgnoreCase(status)) {
                                String checkoutUrl = json.optString("checkout_url", null);
                                if (checkoutUrl != null && !checkoutUrl.trim().isEmpty()) {
                                    Intent wwwIntent = new Intent(MainActivity.this, www.class);
                                    wwwIntent.putExtra("url", checkoutUrl);
                                    startActivity(wwwIntent);

                                    if (items.size() == cartList.size()) {
                                        cartList.clear();
                                        updateCartBadge();
                                        buildCartItemViews();
                                        updateCheckoutVisibility();
                                        updateTotalPrice();
                                    }
                                } else {
                                    SafeToast.show(MainActivity.this, "Payment URL missing from server response", Toast.LENGTH_LONG);
                                }
                            } else {
                                String msg = json.optString("message", "Server returned error");
                                SafeToast.show(MainActivity.this, "Error: " + msg, Toast.LENGTH_LONG);
                            }
                        } catch (Exception e) {
                            Log.e("CHECKOUT_ERROR", "Parse error: " + e.getMessage());
                            SafeToast.show(MainActivity.this, "Parse error: " + e.getMessage(), Toast.LENGTH_LONG);
                        }
                    },
                    error -> {
                        hideLoading();
                        isConnectedToAPI = false;  // ADD THIS LINE
                        disableAllInteractions();   // ADD THIS LINE

                        String msg = "Network error";
                        if (error.networkResponse != null) {
                            msg += " (Code: " + error.networkResponse.statusCode + ")";
                            try {
                                String body = new String(error.networkResponse.data, "UTF-8");
                                Log.e("CHECKOUT_ERROR", "Error response body: " + body);
                            } catch (Exception e) {
                                Log.e("CHECKOUT_ERROR", "Could not read error body");
                            }
                        }
                        if (error.getMessage() != null) {
                            msg += ": " + error.getMessage();
                        }
                        Log.e("CHECKOUT_ERROR", msg);
                        SafeToast.show(MainActivity.this, msg, Toast.LENGTH_LONG);
                    }
            ) {
                @Override
                protected java.util.Map<String, String> getParams() {
                    java.util.Map<String, String> params = new java.util.HashMap<>();
                    params.put("action", "checkout");
                    params.put("items", itemsJson);
                    params.put("total_price", totalStr);
                    params.put("email", loggedInEmail); // ✅ USE LOGGED IN EMAIL FROM SHAREDPREFERENCES
                    params.put("user_name", username != null && !username.isEmpty() ? username : "Guest");

                    Log.e("CHECKOUT_PARAMS", "Final params: " + params.toString()); //REMOVE THIS LOG LINE IN PRODUCTION
                    return params;
                }

                @Override
                public java.util.Map<String, String> getHeaders() {
                    java.util.Map<String, String> headers = new java.util.HashMap<>();
                    headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                    return headers;
                }
            };

            request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
                    15000,
                    0,
                    com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));

            queue.add(request);

        } catch (Exception e) {
            hideLoading();
            Log.e("CHECKOUT_ERROR", "Error preparing checkout: " + e.getMessage());
            SafeToast.show(MainActivity.this, "Error preparing checkout: " + e.getMessage(), Toast.LENGTH_LONG);
        }
    }

    // ---------------- Cart logic ----------------
    private boolean isProductInCart(Product p) {
        if (p == null) return false;
        for (Product cp : cartList) {
            if (cp.getId() == p.getId()) return true;
        }
        return false;
    }

    private void addToCart(Product p) {
        if (p == null) return;
        cartList.add(p);
        updateCartBadge();
        buildCartItemViews();
        updateCheckoutVisibility();
        updateTotalPrice();
    }

    private void removeFromCart(int productId) {
        for (int i = 0; i < cartList.size(); i++) {
            if (cartList.get(i).getId() == productId) {
                cartList.remove(i);
                break;
            }
        }
        updateCartBadge();
        buildCartItemViews();
        updateCheckoutVisibility();
        updateTotalPrice();
    }

    private void updateCartBadge() {
        runOnUiThread(() -> {
            if (cartBadgeText == null) return;
            int count = cartList.size();
            if (count <= 0) {
                cartBadgeText.setVisibility(View.GONE);
            } else {
                cartBadgeText.setVisibility(View.VISIBLE);
                cartBadgeText.setText(String.valueOf(count));
            }
        });
    }

    private void updateCheckoutVisibility() {
        if (cartCheckoutBtn == null) return;
        runOnUiThread(() -> {
            if (cartList.isEmpty()) {
                cartCheckoutBtn.setVisibility(View.GONE);
            } else {
                cartCheckoutBtn.setVisibility(View.VISIBLE);
            }
        });
    }

    private void toggleCartOverlay() {
        if (cartOverlay == null) return;
        if (cartOverlay.getVisibility() == View.VISIBLE) {
            cartOverlay.setVisibility(View.GONE);
        } else {
            buildCartItemViews();
            updateCheckoutVisibility();
            ensureCartTotalExists(); // ensure total text is present
            updateTotalPrice();
            cartOverlay.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Make sure cartTotalText exists in the cartOverlay. If the XML did not include it,
     * create it programmatically and insert it above the Checkout/Close button area.
     */
    private void ensureCartTotalExists() {
        if (cartOverlay == null) return;
        if (cartTotalText != null) return;

        // try to find by id once more
        View candidate = cartOverlay.findViewById(R.id.cartTotalText);
        if (candidate instanceof TextView) {
            cartTotalText = (TextView) candidate;
            return;
        }

        // Create programmatically and insert at bottom area (above buttons)
        cartTotalText = new TextView(this);
        cartTotalText.setId(View.generateViewId());
        cartTotalText.setTextSize(18f);
        cartTotalText.setPadding(12, 12, 12, 12);
        cartTotalText.setText("Total: Br 0.00");
        cartTotalText.setGravity(Gravity.CENTER_VERTICAL);

        // We need to add it just above the buttons container. We'll search the cartOverlay children.
        if (cartOverlay instanceof ViewGroup) {
            ViewGroup overlayGroup = (ViewGroup) cartOverlay;
            // try to find the buttons container
            ViewGroup buttonsContainer = null;
            for (int i = 0; i < overlayGroup.getChildCount(); i++) {
                View child = overlayGroup.getChildAt(i);
                if (child instanceof LinearLayout) {
                    // heuristics: contains checkout button id?
                    View found = child.findViewById(R.id.cartCheckoutBtn);
                    if (found != null) {
                        buttonsContainer = (ViewGroup) child;
                        break;
                    }
                }
            }
            if (buttonsContainer != null) {
                // insert cartTotalText just above buttonsContainer in overlayGroup
                int index = overlayGroup.indexOfChild(buttonsContainer);
                overlayGroup.addView(cartTotalText, index);
                return;
            } else {
                // fallback: append at end
                overlayGroup.addView(cartTotalText);
            }
        }
    }

    /**
     * Calculates and updates the total price text based on items currently in cartList.
     * Assumes Product.getPrice() returns a string that contains digits and possibly decimal separator.
     */
    private void updateTotalPrice() {
        double total = 0.0;
        DecimalFormat df = new DecimalFormat("#,##0.00");
        for (Product p : cartList) {
            if (p == null) continue;
            String raw = p.getPrice();
            if (raw == null || raw.trim().isEmpty()) continue;
            // attempt to extract numeric portion
            try {
                // remove currency symbols and thousands separators
                String cleaned = raw.replaceAll("[^0-9.,-]", "");
                // normalize comma to dot if needed (basic heuristic)
                if (cleaned.contains(",") && !cleaned.contains(".")) {
                    cleaned = cleaned.replace(",", ".");
                } else if (cleaned.contains(",") && cleaned.contains(".")) {
                    // remove commas (thousands separators)
                    cleaned = cleaned.replace(",", "");
                }
                double v = Double.parseDouble(cleaned);
                total += v;
            } catch (Exception e) {
                // ignore parse errors
            }
        }

        final String totalText = "Total: Br " + df.format(total);
        runOnUiThread(() -> {
            ensureCartTotalExists();
            if (cartTotalText != null) cartTotalText.setText(totalText);
        });
    }

    private void buildCartItemViews() {
        if (cartListContainer == null) return;

        runOnUiThread(() -> {
            cartListContainer.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(MainActivity.this);

            for (Product p : cartList) {
                View row;
                try {
                    // Inflate a cart row layout if you have one - prefer R.layout.cart_item_row
                    row = inflater.inflate(R.layout.cart_item_row, cartListContainer, false);
                } catch (Exception ex) {
                    // If layout cannot be inflated, create a simple fallback row programmatically
                    LinearLayout fallback = new LinearLayout(MainActivity.this);
                    fallback.setOrientation(LinearLayout.HORIZONTAL);
                    TextView t = new TextView(MainActivity.this);
                    t.setText(p.getName() + " — Br " + p.getPrice());
                    fallback.addView(t);
                    row = fallback;
                }

                ImageView thumb = null;
                TextView name = null;
                TextView price = null;
                Button removeBtn = null;

                try {
                    thumb = row.findViewById(R.id.cartItemThumb);
                    name = row.findViewById(R.id.cartItemName);
                    price = row.findViewById(R.id.cartItemPrice);
                    removeBtn = row.findViewById(R.id.cartItemRemoveBtn);
                } catch (Exception ignored) {
                }

                if (name != null) name.setText(p.getName());
                if (price != null) price.setText("Br " + p.getPrice());

                if (thumb != null) {
                    try {
                        Picasso.get()
                                .load(p.getImageUrl())
                                .placeholder(android.R.drawable.ic_menu_gallery)
                                .error(android.R.drawable.ic_delete)
                                .fit()
                                .centerInside()
                                .into(thumb);
                    } catch (Exception ignore) {
                    }
                }

                if (removeBtn != null) {
                    removeBtn.setOnClickListener(v -> removeFromCart(p.getId()));
                }

                cartListContainer.addView(row);
            }

            // After building list, ensure total is updated
            updateTotalPrice();
        });
    }

    // ---------------- Add Product dialog ----------------
    private void openAddProductDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_product, null);
        builder.setView(dialogView);

        TextView etName = dialogView.findViewById(R.id.etProductName);
        TextView etPrice = dialogView.findViewById(R.id.etProductPrice);
        TextView etAmount = dialogView.findViewById(R.id.etProductAmount);
        Button btnSubmit = dialogView.findViewById(R.id.btnSubmit);

        AlertDialog dialog = builder.create();
        dialog.show();

        btnSubmit.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String price = etPrice.getText().toString().trim();
            String amountInput = etAmount.getText().toString().trim();

            int amount;
            try {
                amount = Integer.parseInt(amountInput);
                if (amount <= 0) amount = 1;
            } catch (NumberFormatException e) {
                amount = 1;
            }

            if (name.isEmpty() || price.isEmpty() || selectedImageUri == null) {
                SafeToast.show(MainActivity.this, "Please fill all fields and select an image", Toast.LENGTH_SHORT);
            } else {
                dialog.dismiss();
                uploadProduct(name, price, String.valueOf(amount), selectedImageUri);
            }
        });
    }

    // ---------------- Upload Product ----------------
    private void uploadProduct(String name, String price, String amount, Uri imageUri) {
        showLoading();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                Bitmap originalBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                Bitmap compressedBitmap = compressAndResizeImage(originalBitmap, imageUri);

                if (compressedBitmap == null) {
                    runOnUiThread(() -> {
                        hideLoading();
                        SafeToast.show(MainActivity.this, "Failed to process image", Toast.LENGTH_SHORT);
                    });
                    return;
                }

                byte[] imageBytes = bitmapToByteArray(compressedBitmap);
                int fileSizeKB = imageBytes.length / 1024;
                if (fileSizeKB > MAX_FILE_SIZE_KB) {
                    runOnUiThread(() -> {
                        hideLoading();
                        SafeToast.show(MainActivity.this, "Image too large (" + fileSizeKB + "KB).", Toast.LENGTH_LONG);
                    });
                    return;
                }

                URL url = new URL(API_URL_ADD);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");
                String boundary = "*****" + System.currentTimeMillis() + "*****";
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                OutputStream os = conn.getOutputStream();
                String lineEnd = "\r\n";
                String twoHyphens = "--";

                // name
                os.write((twoHyphens + boundary + lineEnd).getBytes());
                os.write(("Content-Disposition: form-data; name=\"name\"" + lineEnd + lineEnd).getBytes());
                os.write((name + lineEnd).getBytes());

                // price
                os.write((twoHyphens + boundary + lineEnd).getBytes());
                os.write(("Content-Disposition: form-data; name=\"price\"" + lineEnd + lineEnd).getBytes());
                os.write((price + lineEnd).getBytes());

                // amount
                os.write((twoHyphens + boundary + lineEnd).getBytes());
                os.write(("Content-Disposition: form-data; name=\"amount\"" + lineEnd + lineEnd).getBytes());
                os.write((amount + lineEnd).getBytes());

                // image
                os.write((twoHyphens + boundary + lineEnd).getBytes());
                os.write(("Content-Disposition: form-data; name=\"image\"; filename=\"image.jpg\"" + lineEnd).getBytes());
                os.write(("Content-Type: image/jpeg" + lineEnd + lineEnd).getBytes());
                os.write(imageBytes);
                os.write(lineEnd.getBytes());
                os.write((twoHyphens + boundary + twoHyphens + lineEnd).getBytes());
                os.flush();
                os.close();

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();

                runOnUiThread(() -> {
                    hideLoading();
                    try {
                        JSONObject jsonResponse = new JSONObject(response.toString());
                        if (jsonResponse.getString("status").equals("success")) {
                            SafeToast.show(MainActivity.this, "Product added successfully!", Toast.LENGTH_SHORT);
                            fetchProductsFromServer(true);
                        } else {
                            Toast.makeText(MainActivity.this, "Error: " + jsonResponse.getString("message"), Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        SafeToast.show(MainActivity.this, "Server Response: " + response, Toast.LENGTH_LONG);
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    hideLoading();
                    SafeToast.show(MainActivity.this, "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG);
                });
            }
        });
    }

    // ---------------- Image helpers ----------------
    private Bitmap compressAndResizeImage(Bitmap originalBitmap, Uri imageUri) {
        try {
            Bitmap rotatedBitmap = fixImageRotation(originalBitmap, imageUri);
            int originalWidth = rotatedBitmap.getWidth();
            int originalHeight = rotatedBitmap.getHeight();

            float scaleWidth = (float) MAX_IMAGE_WIDTH / originalWidth;
            float scaleHeight = (float) MAX_IMAGE_HEIGHT / originalHeight;
            float scale = Math.min(scaleWidth, scaleHeight);

            if (scale < 1.0f) {
                int newWidth = Math.round(originalWidth * scale);
                int newHeight = Math.round(originalHeight * scale);
                return Bitmap.createScaledBitmap(rotatedBitmap, newWidth, newHeight, true);
            }

            return rotatedBitmap;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private byte[] bitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY, baos);
        return baos.toByteArray();
    }

    private Bitmap fixImageRotation(Bitmap bitmap, Uri imageUri) {
        try {
            InputStream input = getContentResolver().openInputStream(imageUri);
            ExifInterface exif = new ExifInterface(input);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            int rotation = 0;
            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) rotation = 90;
            else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) rotation = 180;
            else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) rotation = 270;
            if (rotation != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(rotation);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            }
            input.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bitmap;
    }

// ---------------- Fetch Products ----------------

    /**
     * Fetch products.
     *
     * @param showLoader true to show the loading overlay, false to fetch silently (used by auto-refresh).
     */
    private void fetchProductsFromServer(boolean showLoader) {
        if (showLoader) showLoading();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                URL url = new URL(API_URL_FETCH);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                String postData = "action=get_products";
                OutputStream os = conn.getOutputStream();
                os.write(postData.getBytes());
                os.flush();
                os.close();

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();

                JSONObject jsonResponse = new JSONObject(response.toString());
                JSONArray productsArray = jsonResponse.getJSONArray("products");

                List<Product> newList = new ArrayList<>();
                for (int i = 0; i < productsArray.length(); i++) {
                    JSONObject obj = productsArray.getJSONObject(i);

                    // Skip if amount < 1 according to client's request
                    int amount = obj.optInt("amount", 0);
                    if (amount < 1) continue;

                    int id = obj.getInt("id");
                    String name = obj.getString("name");
                    String price = obj.getString("price");
                    String imageUrl = obj.getString("image_url");

                    // Convert relative image paths to absolute using ApiConfig.BASE_URL if necessary
                    if (!imageUrl.startsWith("http")) {
                        String base = ApiConfig.BASE_URL != null ? ApiConfig.BASE_URL : "";
                        if (!base.isEmpty()) {
                            if (!imageUrl.startsWith("/")) imageUrl = base + "/" + imageUrl;
                            else imageUrl = base + imageUrl;
                        }
                    }

                    newList.add(new Product(id, name, price, imageUrl));
                }

                runOnUiThread(() -> {
                    productList.clear();
                    productList.addAll(newList);
                    adapter.notifyDataSetChanged();
                    if (showLoader) hideLoading();
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (showLoader) hideLoading();
                    SafeToast.show(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG);
                });
            }
        });
    }

    // ---------------- Helpers ----------------
    private double parsePriceToDouble(String raw) {
        if (raw == null) return 0.0;
        try {
            String cleaned = raw.replaceAll("[^0-9.,-]", "");
            if (cleaned.contains(",") && !cleaned.contains("."))
                cleaned = cleaned.replace(",", ".");
            else if (cleaned.contains(",") && cleaned.contains("."))
                cleaned = cleaned.replace(",", "");
            return Double.parseDouble(cleaned);
        } catch (Exception e) {
            return 0.0;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Uri data = intent.getData();
        if (data != null && data.getScheme() != null && data.getScheme().equals(APP_URL_SCHEME)) {
            String status = data.getQueryParameter("status");
            if ("successful".equals(status)) {
                SafeToast.show(this, "Payment Successful!", Toast.LENGTH_SHORT);

            } else if ("failed".equals(status)) {
                SafeToast.show(this, "Payment Failed!", Toast.LENGTH_SHORT);
            }
        }
    }
}