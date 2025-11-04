package com.example.econ2;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

/**
 * Wishlist Activity - Shows saved favorite products
 * Uses same ProductAdapter and flows as MainActivity for consistency
 */
public class WishlistActivity extends AppCompatActivity {
    
    private RecyclerView recyclerView;
    private ProductAdapter adapter;
    private List<Product> wishlistProducts = new ArrayList<>();
    private TextView emptyStateText;
    private View emptyStateLayout;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wishlist);
        
        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("My Wishlist");
        }
        
        // Initialize views
        recyclerView = findViewById(R.id.wishlistRecyclerView);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        emptyStateText = findViewById(R.id.emptyStateText);
        
        // Setup RecyclerView
        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(layoutManager);
        
        // Setup adapter with same behavior as MainActivity
        adapter = new ProductAdapter(this, wishlistProducts);
        recyclerView.setAdapter(adapter);
        
        // Product click -> same overlay behavior as MainActivity
        adapter.setOnProductClickListener((product, position) -> {
            // Return to MainActivity with selected product
            Intent intent = new Intent();
            intent.putExtra("selected_product_id", product.getId());
            setResult(RESULT_OK, intent);
            finish();
        });
        
        // Wishlist click -> remove from wishlist
        adapter.setOnWishlistClickListener((product, isAdding) -> {
            if (!isAdding) { // removing from wishlist
                WishlistManager.removeFromWishlist(this, product.getId());
                loadWishlistProducts();
                SafeToast.show(this, product.getName() + " removed from wishlist", android.widget.Toast.LENGTH_SHORT);
            }
        });
        
        loadWishlistProducts();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        loadWishlistProducts(); // Refresh in case wishlist changed
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
    
    private void loadWishlistProducts() {
        // Get wishlist product IDs
        List<Integer> wishlistIds = WishlistManager.getWishlistProductIds(this);
        
        if (wishlistIds.isEmpty()) {
            showEmptyState();
            return;
        }
        
        // In a real app, you would fetch these products from the server
        // For now, we'll get them from the intent extras or use mock data
        List<Product> allProducts = getAllProductsFromCache();
        
        wishlistProducts.clear();
        for (Product product : allProducts) {
            if (wishlistIds.contains(product.getId())) {
                wishlistProducts.add(product);
            }
        }
        
        if (wishlistProducts.isEmpty()) {
            showEmptyState();
        } else {
            showWishlistContent();
            adapter.updateProducts(wishlistProducts);
        }
    }
    
    private void showEmptyState() {
        recyclerView.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.VISIBLE);
        if (emptyStateText != null) {
            emptyStateText.setText("Your wishlist is empty\n\nTap the ❤️ on any product to add it here");
        }
    }
    
    private void showWishlistContent() {
        recyclerView.setVisibility(View.VISIBLE);
        emptyStateLayout.setVisibility(View.GONE);
    }
    
    /**
     * Get cached products - in a real implementation, this would fetch from your API
     * For now, return empty list and products will be loaded when user returns to MainActivity
     */
    private List<Product> getAllProductsFromCache() {
        // This would ideally be populated from a cache or API call
        // For now, return empty list - the wishlist will work when products are available
        return new ArrayList<>();
    }
}