package com.example.econ2;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.squareup.picasso.Picasso;
import java.util.ArrayList;
import java.util.List;

/**
 * Modern RecyclerView adapter for product grid with enhanced cards
 * Backward compatible with existing Product model and click behavior
 */
public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {
    
    private Context context;
    private List<Product> productList;
    private List<Product> originalProductList; // For search/filter functionality
    private OnProductClickListener clickListener;
    private OnWishlistClickListener wishlistListener;
    
    public interface OnProductClickListener {
        void onProductClick(Product product, int position);
    }
    
    public interface OnWishlistClickListener {
        void onWishlistClick(Product product, boolean isAdding);
    }
    
    public ProductAdapter(Context context, List<Product> productList) {
        this.context = context;
        this.productList = productList != null ? productList : new ArrayList<>();
        this.originalProductList = new ArrayList<>(this.productList);
    }
    
    public void setOnProductClickListener(OnProductClickListener listener) {
        this.clickListener = listener;
    }
    
    public void setOnWishlistClickListener(OnWishlistClickListener listener) {
        this.wishlistListener = listener;
    }
    
    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product_enhanced, parent, false);
        return new ProductViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);
        holder.bind(product);
    }
    
    @Override
    public int getItemCount() {
        return productList.size();
    }
    
    // Search and filter functionality
    public void filter(String query) {
        productList.clear();
        if (query.isEmpty()) {
            productList.addAll(originalProductList);
        } else {
            String lowercaseQuery = query.toLowerCase().trim();
            for (Product product : originalProductList) {
                if (product.getName().toLowerCase().contains(lowercaseQuery) ||
                    (product.getPrice() != null && product.getPrice().toLowerCase().contains(lowercaseQuery))) {
                    productList.add(product);
                }
            }
        }
        notifyDataSetChanged();
    }
    
    public void filterByPriceRange(double minPrice, double maxPrice) {
        productList.clear();
        for (Product product : originalProductList) {
            try {
                double price = Double.parseDouble(product.getPrice().replaceAll("[^0-9.]", ""));
                if (price >= minPrice && price <= maxPrice) {
                    productList.add(product);
                }
            } catch (NumberFormatException e) {
                // Include products with unparseable prices
                productList.add(product);
            }
        }
        notifyDataSetChanged();
    }
    
    public void updateProducts(List<Product> newProducts) {
        this.originalProductList.clear();
        this.originalProductList.addAll(newProducts);
        this.productList.clear();
        this.productList.addAll(newProducts);
        notifyDataSetChanged();
    }
    
    public void clearFilter() {
        productList.clear();
        productList.addAll(originalProductList);
        notifyDataSetChanged();
    }
    
    class ProductViewHolder extends RecyclerView.ViewHolder {
        private ImageView productImage;
        private TextView productName;
        private TextView productPrice;
        private ImageView wishlistButton;
        private TextView ratingText;
        private View saleBadge;
        private TextView saleText;
        
        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.productImage);
            productName = itemView.findViewById(R.id.productName);
            productPrice = itemView.findViewById(R.id.productPrice);
            wishlistButton = itemView.findViewById(R.id.wishlistButton);
            ratingText = itemView.findViewById(R.id.ratingText);
            saleBadge = itemView.findViewById(R.id.saleBadge);
            saleText = itemView.findViewById(R.id.saleText);
            
            // Set click listeners
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && clickListener != null) {
                    clickListener.onProductClick(productList.get(position), position);
                }
            });
            
            if (wishlistButton != null) {
                wishlistButton.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && wishlistListener != null) {
                        Product product = productList.get(position);
                        boolean isCurrentlyWishlisted = WishlistManager.isInWishlist(context, product.getId());
                        wishlistListener.onWishlistClick(product, !isCurrentlyWishlisted);
                        updateWishlistButton(product);
                    }
                });
            }
        }
        
        public void bind(Product product) {
            // Set product name
            if (productName != null) {
                productName.setText(product.getName());
            }
            
            // Set product price
            if (productPrice != null) {
                productPrice.setText("Br " + product.getPrice());
            }
            
            // Load product image with enhanced error handling
            if (productImage != null) {
                try {
                    String imageUrl = product.getImageUrl();
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        Picasso.get()
                                .load(imageUrl)
                                .placeholder(R.drawable.ic_placeholder)
                                .error(R.drawable.image_error)
                                .fit()
                                .centerCrop()
                                .into(productImage);
                    } else {
                        productImage.setImageResource(R.drawable.ic_placeholder);
                    }
                } catch (Exception e) {
                    productImage.setImageResource(R.drawable.ic_placeholder);
                }
            }
            
            // Update wishlist button
            updateWishlistButton(product);
            
            // Set rating (mock data for now - can be enhanced later)
            if (ratingText != null) {
                // Generate a realistic rating based on product ID for consistency
                double rating = 3.5 + (product.getId() % 15) / 10.0; // Range: 3.5-4.9
                ratingText.setText(String.format("⭐%.1f", rating));
            }
            
            // Show sale badge for products with specific conditions (mock logic)
            if (saleBadge != null && saleText != null) {
                // Example: Show sale for products with ID ending in specific digits
                boolean isOnSale = (product.getId() % 4 == 0);
                if (isOnSale) {
                    saleBadge.setVisibility(View.VISIBLE);
                    saleText.setText("SALE");
                } else {
                    saleBadge.setVisibility(View.GONE);
                }
            }
        }
        
        private void updateWishlistButton(Product product) {
            if (wishlistButton != null) {
                boolean isWishlisted = WishlistManager.isInWishlist(context, product.getId());
                wishlistButton.setImageResource(isWishlisted ? 
                    R.drawable.ic_favorite_filled : R.drawable.ic_favorite_outline);
                wishlistButton.setAlpha(isWishlisted ? 1.0f : 0.7f);
            }
        }
    }
}