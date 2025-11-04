package com.example.econ2;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecitePage extends AppCompatActivity {

    private LinearLayout reciteContainer;
    private ProgressBar progressBar;
    private TextView statusText;
    private List<Recite> reciteList = new ArrayList<>();
    
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "user_prefs";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_EMAIL = "email";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recite_page);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        // Enable back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Order History");
        }

        // Initialize views
        reciteContainer = findViewById(R.id.reciteContainer);
        progressBar = findViewById(R.id.progressBar);
        statusText = findViewById(R.id.statusText);

        // Load recites from server
        fetchRecitesFromServer();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh receipts when returning to this page (e.g., after making a purchase)
        Log.d("RECITE_PAGE", "onResume: Refreshing receipts");
        fetchRecitesFromServer();
    }

    private void fetchRecitesFromServer() {
        showLoading();
        
        String userEmail = sharedPreferences.getString(KEY_EMAIL, "");
        String username = sharedPreferences.getString(KEY_USERNAME, "User");
        
        // Debug logging
        Log.d("RECITE_DEBUG", "Fetching receipts for user: " + userEmail);
        Log.d("RECITE_DEBUG", "API URL: " + ApiConfig.GET_RECITES);
        
        if (userEmail.isEmpty()) {
            showError("User email not found. Please log in again.");
            return;
        }

        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest request = new StringRequest(Request.Method.POST, ApiConfig.GET_RECITES,
                response -> {
                    hideLoading();
                    Log.d("RECITE_RESPONSE", "Response: " + response);
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        String status = jsonResponse.optString("status", "error");
                        
                        if ("success".equalsIgnoreCase(status)) {
                            JSONArray recitesArray = jsonResponse.optJSONArray("recites");
                            Log.d("RECITE_DEBUG", "Received " + (recitesArray != null ? recitesArray.length() : 0) + " orders");
                            if (recitesArray != null && recitesArray.length() > 0) {
                                reciteList.clear();
                                for (int i = 0; i < recitesArray.length(); i++) {
                                    JSONObject reciteObj = recitesArray.getJSONObject(i);
                                    Recite recite = new Recite(
                                            reciteObj.optString("id"),
                                            reciteObj.optString("title"),
                                            reciteObj.optString("content"),
                                            reciteObj.optString("date_created"),
                                            reciteObj.optString("user_id"),
                                            reciteObj.optString("category")
                                    );
                                    
                                    // Set additional order/payment fields
                                    recite.setTotalAmount(reciteObj.optString("total_amount"));
                                    recite.setCurrency(reciteObj.optString("currency"));
                                    recite.setPaymentStatus(reciteObj.optString("payment_status"));
                                    recite.setTxRef(reciteObj.optString("tx_ref"));
                                    recite.setTotalItems(reciteObj.optInt("total_items", 0));
                                    
                                    // Parse items array
                                    JSONArray itemsArray = reciteObj.optJSONArray("items");
                                    if (itemsArray != null) {
                                        for (int j = 0; j < itemsArray.length(); j++) {
                                            JSONObject itemObj = itemsArray.getJSONObject(j);
                                            OrderItem item = new OrderItem(
                                                    itemObj.optString("product_name"),
                                                    itemObj.optString("product_price"),
                                                    itemObj.optString("quantity"),
                                                    itemObj.optString("subtotal"),
                                                    itemObj.optString("image_url"),
                                                    itemObj.optString("size")
                                            );
                                            recite.addItem(item);
                                        }
                                    }
                                    
                                    reciteList.add(recite);
                                }
                                displayRecites();
                            } else {
                                showEmptyState();
                            }
                        } else {
                            String message = jsonResponse.optString("message", "Failed to load recites");
                            showError(message);
                        }
                    } catch (Exception e) {
                        Log.e("RECITE_ERROR", "Parse error: " + e.getMessage());
                        showError("Error parsing server response: " + e.getMessage());
                    }
                },
                error -> {
                    hideLoading();
                    String errorMsg = "Network error";
                    if (error.networkResponse != null) {
                        errorMsg += " (Code: " + error.networkResponse.statusCode + ")";
                    }
                    if (error.getMessage() != null) {
                        errorMsg += ": " + error.getMessage();
                    }
                    Log.e("RECITE_ERROR", errorMsg);
                    showError(errorMsg);
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("action", "get_recites");
                params.put("email", userEmail);
                params.put("username", username);
                return params;
            }

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                return headers;
            }
        };

        request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
                10000,
                1,
                com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        queue.add(request);
    }

    private void showLoading() {
        View loadingLayout = findViewById(R.id.loadingLayout);
        View emptyLayout = findViewById(R.id.emptyLayout);
        View scrollView = findViewById(R.id.scrollView);
        
        if (loadingLayout != null) loadingLayout.setVisibility(View.VISIBLE);
        if (emptyLayout != null) emptyLayout.setVisibility(View.GONE);
        if (scrollView != null) scrollView.setVisibility(View.GONE);
    }

    private void hideLoading() {
        View loadingLayout = findViewById(R.id.loadingLayout);
        View scrollView = findViewById(R.id.scrollView);
        
        if (loadingLayout != null) loadingLayout.setVisibility(View.GONE);
        if (scrollView != null) scrollView.setVisibility(View.VISIBLE);
    }

    private void showError(String message) {
        View loadingLayout = findViewById(R.id.loadingLayout);
        View emptyLayout = findViewById(R.id.emptyLayout);
        View scrollView = findViewById(R.id.scrollView);
        
        if (loadingLayout != null) loadingLayout.setVisibility(View.GONE);
        if (emptyLayout != null) emptyLayout.setVisibility(View.VISIBLE);
        if (scrollView != null) scrollView.setVisibility(View.GONE);
        
        // Update empty state to show error
        TextView emptyTitle = emptyLayout.findViewById(R.id.emptyTitle);
        TextView emptySubtitle = emptyLayout.findViewById(R.id.emptySubtitle);
        if (emptyTitle != null) emptyTitle.setText("Error Loading Orders");
        if (emptySubtitle != null) emptySubtitle.setText(message);
        
        SafeToast.show(this, message, Toast.LENGTH_LONG);
    }

    private void showEmptyState() {
        View loadingLayout = findViewById(R.id.loadingLayout);
        View emptyLayout = findViewById(R.id.emptyLayout);
        View scrollView = findViewById(R.id.scrollView);
        
        if (loadingLayout != null) loadingLayout.setVisibility(View.GONE);
        if (emptyLayout != null) emptyLayout.setVisibility(View.VISIBLE);
        if (scrollView != null) scrollView.setVisibility(View.GONE);
    }

    private void displayRecites() {
        if (reciteContainer == null) return;

        reciteContainer.removeAllViews();
        
        // Update header subtitle with order count
        TextView headerSubtitle = findViewById(R.id.headerSubtitle);
        if (headerSubtitle != null) {
            headerSubtitle.setText(reciteList.size() + " order(s) found");
        }

        for (Recite recite : reciteList) {
            LinearLayout orderCard = createProfessionalOrderCard(recite);
            reciteContainer.addView(orderCard);
        }
    }
    
    private LinearLayout createProfessionalOrderCard(Recite recite) {
        // Main card container
        LinearLayout cardContainer = new LinearLayout(this);
        cardContainer.setOrientation(LinearLayout.VERTICAL);
        cardContainer.setBackground(getDrawable(R.drawable.professional_card_background));
        cardContainer.setElevation(8f);
        
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, 24);
        cardContainer.setLayoutParams(cardParams);

        // Card header
        LinearLayout headerSection = createOrderHeader(recite);
        cardContainer.addView(headerSection);
        
        // Divider
        View divider = new View(this);
        divider.setBackgroundColor(getColor(R.color.gray_200));
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 1);
        dividerParams.setMargins(24, 0, 24, 0);
        divider.setLayoutParams(dividerParams);
        cardContainer.addView(divider);

        // Items section
        if (recite.getItems() != null && !recite.getItems().isEmpty()) {
            LinearLayout itemsSection = createItemsSection(recite);
            cardContainer.addView(itemsSection);
        }

        // Card footer
        LinearLayout footerSection = createOrderFooter(recite);
        cardContainer.addView(footerSection);

        // Card click animation and action
        cardContainer.setOnClickListener(v -> {
            // Add subtle animation
            v.animate().scaleX(0.98f).scaleY(0.98f).setDuration(100)
                .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(100));
            
            SafeToast.show(RecitePage.this, 
                "Order #" + recite.getId() + " • " + recite.getTotalItems() + " items", 
                Toast.LENGTH_SHORT);
        });

        return cardContainer;
    }
    
    private LinearLayout createOrderHeader(Recite recite) {
        LinearLayout headerLayout = new LinearLayout(this);
        headerLayout.setOrientation(LinearLayout.VERTICAL);
        headerLayout.setPadding(24, 20, 24, 16);

        // Order ID and status row
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        
        // Order ID
        TextView orderIdView = new TextView(this);
        orderIdView.setText("Order #" + recite.getId());
        orderIdView.setTextSize(20f);
        orderIdView.setTextColor(getColor(R.color.receipt_header_text));
        orderIdView.setTypeface(null, android.graphics.Typeface.BOLD);
        
        LinearLayout.LayoutParams idParams = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        orderIdView.setLayoutParams(idParams);
        topRow.addView(orderIdView);

        // Status badge
        TextView statusBadge = createStatusBadge(recite.getPaymentStatus());
        topRow.addView(statusBadge);
        
        headerLayout.addView(topRow);

        // Order summary
        TextView summaryView = new TextView(this);
        summaryView.setText(String.format("%d item%s • %s %s", 
            recite.getTotalItems(),
            recite.getTotalItems() == 1 ? "" : "s",
            recite.getTotalAmount(), 
            recite.getCurrency()));
        summaryView.setTextSize(16f);
        summaryView.setTextColor(getColor(R.color.receipt_secondary_text));
        summaryView.setPadding(0, 8, 0, 0);
        headerLayout.addView(summaryView);

        return headerLayout;
    }
    
    private TextView createStatusBadge(String status) {
        TextView statusBadge = new TextView(this);
        statusBadge.setText(status.toUpperCase());
        statusBadge.setTextSize(12f);
        statusBadge.setTypeface(null, android.graphics.Typeface.BOLD);
        statusBadge.setPadding(16, 8, 16, 8);
        statusBadge.setGravity(android.view.Gravity.CENTER);
        
        // Style based on status - professional and subtle
        if ("success".equalsIgnoreCase(status) || "completed".equalsIgnoreCase(status)) {
            statusBadge.setTextColor(getColor(R.color.button_success));
            statusBadge.setBackground(getDrawable(R.drawable.status_success_bg));
        } else if ("pending".equalsIgnoreCase(status)) {
            statusBadge.setTextColor(getColor(R.color.button_warning));
            statusBadge.setBackground(getDrawable(R.drawable.status_pending_bg));
        } else {
            statusBadge.setTextColor(getColor(R.color.status_failed));
            statusBadge.setBackground(getDrawable(R.drawable.status_failed_bg));
        }
        
        return statusBadge;
    }
    
    private LinearLayout createItemsSection(Recite recite) {
        LinearLayout itemsLayout = new LinearLayout(this);
        itemsLayout.setOrientation(LinearLayout.VERTICAL);
        itemsLayout.setPadding(24, 16, 24, 0);

        // Items header
        TextView itemsHeader = new TextView(this);
        itemsHeader.setText("Items Ordered");
        itemsHeader.setTextSize(16f);
        itemsHeader.setTextColor(getColor(R.color.receipt_header_text));
        itemsHeader.setTypeface(null, android.graphics.Typeface.BOLD);
        itemsHeader.setPadding(0, 0, 0, 12);
        itemsLayout.addView(itemsHeader);

        // Add each item
        for (OrderItem item : recite.getItems()) {
            LinearLayout itemCard = createItemCard(item, recite.getCurrency());
            itemsLayout.addView(itemCard);
        }

        return itemsLayout;
    }
    
    private LinearLayout createItemCard(OrderItem item, String currency) {
        LinearLayout itemCard = new LinearLayout(this);
        itemCard.setOrientation(LinearLayout.HORIZONTAL);
        itemCard.setBackground(getDrawable(R.drawable.item_card_background));
        itemCard.setPadding(16, 16, 16, 16);
        itemCard.setGravity(android.view.Gravity.CENTER_VERTICAL);
        
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, 12);
        itemCard.setLayoutParams(cardParams);

        // Product image
        ImageView productImage = new ImageView(this);
        int imageSize = (int) (56 * getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(imageSize, imageSize);
        imageParams.setMargins(0, 0, 16, 0);
        productImage.setLayoutParams(imageParams);
        productImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        productImage.setBackground(getDrawable(R.drawable.image_background));
        
        // Load image
        String imageUrl = item.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty() && !imageUrl.equals("null")) {
            try {
                Picasso.get()
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_placeholder)
                        .error(R.drawable.ic_placeholder)
                        .fit()
                        .centerCrop()
                        .into(productImage);
            } catch (Exception e) {
                productImage.setImageResource(R.drawable.ic_placeholder);
            }
        } else {
            productImage.setImageResource(R.drawable.ic_placeholder);
        }
        
        itemCard.addView(productImage);

        // Product details
        LinearLayout detailsLayout = new LinearLayout(this);
        detailsLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams detailsParams = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        detailsLayout.setLayoutParams(detailsParams);

        // Product name
        TextView productName = new TextView(this);
        productName.setText(item.getProductName());
        productName.setTextSize(15f);
        productName.setTextColor(getColor(R.color.receipt_header_text));
        productName.setTypeface(null, android.graphics.Typeface.BOLD);
        productName.setMaxLines(2);
        productName.setEllipsize(android.text.TextUtils.TruncateAt.END);
        detailsLayout.addView(productName);

        // Quantity and unit price
        TextView quantityPrice = new TextView(this);
        quantityPrice.setText(String.format("Qty %s × %s %s", 
            item.getQuantity(), item.getProductPrice(), currency));
        quantityPrice.setTextSize(13f);
        quantityPrice.setTextColor(getColor(R.color.receipt_secondary_text));
        quantityPrice.setPadding(0, 4, 0, 0);
        detailsLayout.addView(quantityPrice);

        // Size (if available)
        if (item.getSize() != null && !item.getSize().isEmpty() && !item.getSize().equals("null")) {
            TextView size = new TextView(this);
            size.setText("Size: " + item.getSize());
            size.setTextSize(12f);
            size.setTextColor(getColor(R.color.receipt_secondary_text));
            size.setPadding(0, 2, 0, 0);
            detailsLayout.addView(size);
        }

        itemCard.addView(detailsLayout);

        // Subtotal
        TextView subtotal = new TextView(this);
        subtotal.setText(item.getSubtotal() + " " + currency);
        subtotal.setTextSize(16f);
        subtotal.setTextColor(getColor(R.color.receipt_header_text));
        subtotal.setTypeface(null, android.graphics.Typeface.BOLD);
        subtotal.setGravity(android.view.Gravity.END);
        itemCard.addView(subtotal);

        return itemCard;
    }
    
    private LinearLayout createOrderFooter(Recite recite) {
        LinearLayout footerLayout = new LinearLayout(this);
        footerLayout.setOrientation(LinearLayout.VERTICAL);
        footerLayout.setPadding(24, 16, 24, 20);

        // Order date
        TextView dateView = new TextView(this);
        dateView.setText("Order Date: " + formatDate(recite.getDateCreated()));
        dateView.setTextSize(13f);
        dateView.setTextColor(getColor(R.color.receipt_secondary_text));
        footerLayout.addView(dateView);

        // Transaction reference
        TextView txRefView = new TextView(this);
        txRefView.setText("Transaction ID: " + recite.getTxRef());
        txRefView.setTextSize(11f);
        txRefView.setTextColor(getColor(R.color.gray_500));
        txRefView.setPadding(0, 8, 0, 0);
        txRefView.setTypeface(android.graphics.Typeface.MONOSPACE);
        footerLayout.addView(txRefView);

        return footerLayout;
    }
    
    private String formatDate(String dateString) {
        // Simple date formatting - you can enhance this with proper date parsing
        if (dateString != null && dateString.contains(" ")) {
            String[] parts = dateString.split(" ");
            if (parts.length >= 2) {
                return parts[0] + " at " + parts[1].substring(0, 5);
            }
        }
        return dateString;
    }
}