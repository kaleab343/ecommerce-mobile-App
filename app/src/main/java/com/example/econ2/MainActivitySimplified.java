package com.example.econ2;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

/**
 * Simplified MainActivity with quantity selection
 * Removes add product functionality and adds quantity dialog
 */
public class MainActivitySimplified extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProductAdapter productAdapter;
    private List<Product> productList = new ArrayList<>();
    
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "user_prefs";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_USERNAME = "username";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Use your existing layout
        
        sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        
        // Initialize RecyclerView (if available)
        recyclerView = findViewById(R.id.recyclerView);
        if (recyclerView != null) {
            setupRecyclerView();
        } else {
            // Fallback to existing MainActivity behavior
            startMainActivity();
        }
    }
    
    private void setupRecyclerView() {
        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(layoutManager);
        
        productAdapter = new ProductAdapter(this, productList);
        recyclerView.setAdapter(productAdapter);
        
        // Product click → Quantity Dialog
        productAdapter.setOnProductClickListener((product, position) -> {
            showQuantityDialog(product);
        });
        
        // Wishlist functionality
        productAdapter.setOnWishlistClickListener((product, isAdding) -> {
            if (isAdding) {
                WishlistManager.addToWishlist(this, product.getId());
                SafeToast.show(this, product.getName() + " added to wishlist", Toast.LENGTH_SHORT);
            } else {
                WishlistManager.removeFromWishlist(this, product.getId());
                SafeToast.show(this, product.getName() + " removed from wishlist", Toast.LENGTH_SHORT);
            }
        });
        
        // Load products (placeholder - implement actual loading)
        loadSampleProducts();
    }
    
    private void showQuantityDialog(Product product) {
        QuantityDialog quantityDialog = new QuantityDialog(this, product, new QuantityDialog.OnQuantitySelectedListener() {
            @Override
            public void onAddToCart(Product product, int quantity) {
                // For now, just show toast
                SafeToast.show(MainActivitySimplified.this, 
                    quantity + "x " + product.getName() + " added to cart", 
                    Toast.LENGTH_SHORT);
                
                // TODO: Implement actual cart functionality
            }

            @Override
            public void onBuyNow(Product product, int quantity) {
                // For now, just show toast
                SafeToast.show(MainActivitySimplified.this, 
                    "Buying " + quantity + "x " + product.getName(), 
                    Toast.LENGTH_SHORT);
                
                // TODO: Implement actual checkout functionality
            }
        });
        
        quantityDialog.show();
    }
    
    private void loadSampleProducts() {
        // Add some sample products for testing
        productList.clear();
        productList.add(new Product(1, "Sample Product 1", "100.00", ""));
        productList.add(new Product(2, "Sample Product 2", "200.00", ""));
        productList.add(new Product(3, "Sample Product 3", "300.00", ""));
        productList.add(new Product(4, "Sample Product 4", "400.00", ""));
        
        productAdapter.updateProducts(productList);
    }
    
    private void startMainActivity() {
        // Fallback to original MainActivity
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}