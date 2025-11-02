package com.example.econ2;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private GridView gridView;
    private List<Product> productList = new ArrayList<>();
    private GridAdapter adapter;

    private FrameLayout overlayContainer;
    private ImageView overlayImage;
    private Button btnBuy;
    private Button btnAddProduct;
    private FrameLayout progressOverlay;
    private ProgressBar progressBar;
    private TextView progressText;

    private int selectedProductId = -1;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        username = sharedPreferences.getString(KEY_USERNAME, "User");
        userEmail = sharedPreferences.getString(KEY_EMAIL, "");

        gridView = findViewById(R.id.gridView);
        btnAddProduct = findViewById(R.id.btnAddProduct);
        overlayContainer = findViewById(R.id.overlayContainer);
        overlayImage = findViewById(R.id.overlayImage);
        btnBuy = findViewById(R.id.btnBuy);

        progressOverlay = findViewById(R.id.progress_overlay);
        progressBar = progressOverlay.findViewById(R.id.progressBar);
        progressText = progressOverlay.findViewById(R.id.progressText);

        adapter = new GridAdapter(this, productList);
        gridView.setAdapter(adapter);
        gridView.setSmoothScrollbarEnabled(true);

        fetchProductsFromServer();

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
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryLauncher.launch(intent);
        });

        ImageView userIcon = findViewById(R.id.userIcon);
        userIcon.setOnClickListener(v -> showUserMenu(v));

        gridView.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id) -> {
            Product clickedProduct = productList.get(position);
            selectedProductId = clickedProduct.getId();
            showProductOverlay(clickedProduct);
        });

        overlayContainer.setOnClickListener(v -> hideProductOverlay());
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

        Toast.makeText(MainActivity.this, "Logged out!", Toast.LENGTH_SHORT).show();

        // Start BlankFragment instantly
        Intent intent = new Intent(MainActivity.this, BlankFragment.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // ---------------- Loader ----------------
    private void showLoading() {
        runOnUiThread(() -> progressOverlay.setVisibility(View.VISIBLE));
    }

    private void hideLoading() {
        runOnUiThread(() -> progressOverlay.setVisibility(View.GONE));
    }

    // ---------------- Overlay ----------------
    private void showProductOverlay(Product product) {
        overlayImage.setImageBitmap(product.getImage());

        int newSize = 200 * 5;
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(newSize, newSize);
        params.gravity = Gravity.CENTER;
        overlayImage.setLayoutParams(params);

        overlayContainer.setVisibility(View.VISIBLE);
        btnBuy.setVisibility(View.VISIBLE);

        btnBuy.setOnClickListener(v -> {
            if (selectedProductId != -1) {
                showLoading();
                try {
                    StringRequest request = new StringRequest(
                            Request.Method.POST,
                            API_URL_BUY,
                            response -> {
                                hideLoading();
                                try {
                                    JSONObject jsonResponse = new JSONObject(response);
                                    if (jsonResponse.getString("status").equals("success")) {
                                        String checkoutUrl = jsonResponse.getString("checkout_url");
                                        Intent wwwIntent = new Intent(MainActivity.this, www.class);
                                        wwwIntent.putExtra("url", checkoutUrl);
                                        startActivity(wwwIntent);
                                    } else {
                                        Toast.makeText(MainActivity.this, jsonResponse.getString("message"), Toast.LENGTH_SHORT).show();
                                    }
                                    overlayContainer.setVisibility(View.GONE);
                                } catch (Exception e) {
                                    Toast.makeText(MainActivity.this, "Error parsing response: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            },
                            error -> {
                                hideLoading();
                                Toast.makeText(MainActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                    ) {
                        @Override
                        protected Map<String, String> getParams() {
                            Map<String, String> params = new HashMap<>();
                            params.put("confirm", "yes");
                            params.put("product_id", String.valueOf(selectedProductId));
                            params.put("email", userEmail); // Send saved email
                            return params;
                        }
                    };

                    RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
                    queue.add(request);

                } catch (Exception e) {
                    hideLoading();
                    Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void hideProductOverlay() {
        overlayContainer.setVisibility(View.GONE);
    }

    // ---------------- Add Product ----------------
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
                Toast.makeText(MainActivity.this, "Please fill all fields and select an image", Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(MainActivity.this, "Failed to process image", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                byte[] imageBytes = bitmapToByteArray(compressedBitmap);
                int fileSizeKB = imageBytes.length / 1024;
                if (fileSizeKB > MAX_FILE_SIZE_KB) {
                    runOnUiThread(() -> {
                        hideLoading();
                        Toast.makeText(MainActivity.this, "Image too large (" + fileSizeKB + "KB).", Toast.LENGTH_LONG).show();
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
                            Toast.makeText(MainActivity.this, "Product added successfully!", Toast.LENGTH_SHORT).show();
                            fetchProductsFromServer();
                        } else {
                            Toast.makeText(MainActivity.this, "Error: " + jsonResponse.getString("message"), Toast.LENGTH_LONG).show();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(MainActivity.this, "Server Response: " + response, Toast.LENGTH_LONG).show();
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    hideLoading();
                    Toast.makeText(MainActivity.this, "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    // ---------------- Image Compression ----------------
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
    private void fetchProductsFromServer() {
        showLoading();
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

                productList.clear();
                for (int i = 0; i < productsArray.length(); i++) {
                    JSONObject obj = productsArray.getJSONObject(i);
                    int id = obj.getInt("id");
                    String name = obj.getString("name");
                    String price = obj.getString("price");
                    String imageBase64 = obj.getString("image");

                    byte[] imageBytes = Base64.getDecoder().decode(imageBase64);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                    productList.add(new Product(id, name, price, bitmap));
                }

                runOnUiThread(() -> {
                    hideLoading();
                    adapter.notifyDataSetChanged();
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    hideLoading();
                    Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Uri data = intent.getData();
        if (data != null && data.getScheme().equals(APP_URL_SCHEME)) {
            String status = data.getQueryParameter("status");
            if ("successful".equals(status)) {
                Toast.makeText(this, "Payment Successful!", Toast.LENGTH_SHORT).show();
            } else if ("failed".equals(status)) {
                Toast.makeText(this, "Payment Failed!", Toast.LENGTH_SHORT).show();
            }
        }
    } // <-- Close onNewIntent

} // <-- Close MainActivity class
