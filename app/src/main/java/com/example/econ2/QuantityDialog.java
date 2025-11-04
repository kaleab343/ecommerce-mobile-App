package com.example.econ2;

import android.app.Dialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import com.squareup.picasso.Picasso;

/**
 * Quantity selection dialog for Buy/Add to Cart actions
 * Allows users to select quantity before proceeding with purchase or cart addition
 */
public class QuantityDialog {
    
    public interface OnQuantitySelectedListener {
        void onAddToCart(Product product, int quantity);
        void onBuyNow(Product product, int quantity);
    }
    
    private Context context;
    private Product product;
    private OnQuantitySelectedListener listener;
    private Dialog dialog;
    private EditText quantityInput;
    private TextView totalPriceDisplay;
    private ImageView btnDecrease, btnIncrease;
    private int currentQuantity = 1;
    private double unitPrice = 0.0;
    
    public QuantityDialog(Context context, Product product, OnQuantitySelectedListener listener) {
        this.context = context;
        this.product = product;
        this.listener = listener;
        this.unitPrice = parsePrice(product.getPrice());
    }
    
    public void show() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_quantity_selector, null);
        builder.setView(dialogView);
        
        // Initialize views
        ImageView productImagePreview = dialogView.findViewById(R.id.productImagePreview);
        TextView productNamePreview = dialogView.findViewById(R.id.productNamePreview);
        TextView productPricePreview = dialogView.findViewById(R.id.productPricePreview);
        quantityInput = dialogView.findViewById(R.id.quantityInput);
        totalPriceDisplay = dialogView.findViewById(R.id.totalPriceDisplay);
        btnDecrease = dialogView.findViewById(R.id.btnDecrease);
        btnIncrease = dialogView.findViewById(R.id.btnIncrease);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnAddToCart = dialogView.findViewById(R.id.btnAddToCart);
        Button btnBuyNow = dialogView.findViewById(R.id.btnBuyNow);
        
        // Setup product preview
        setupProductPreview(productImagePreview, productNamePreview, productPricePreview);
        
        // Setup quantity controls
        setupQuantityControls();
        
        // Setup action buttons
        setupActionButtons(btnCancel, btnAddToCart, btnBuyNow);
        
        // Create and show dialog
        dialog = builder.create();
        dialog.show();
        
        // Initial total calculation
        updateTotalPrice();
    }
    
    private void setupProductPreview(ImageView image, TextView name, TextView price) {
        // Set product name
        if (name != null) {
            name.setText(product.getName());
        }
        
        // Set product price
        if (price != null) {
            price.setText("Br " + product.getPrice());
        }
        
        // Load product image
        if (image != null) {
            try {
                String imageUrl = product.getImageUrl();
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    Picasso.get()
                            .load(imageUrl)
                            .placeholder(R.drawable.ic_placeholder)
                            .error(R.drawable.image_error)
                            .fit()
                            .centerCrop()
                            .into(image);
                } else {
                    image.setImageResource(R.drawable.ic_placeholder);
                }
            } catch (Exception e) {
                image.setImageResource(R.drawable.ic_placeholder);
            }
        }
    }
    
    private void setupQuantityControls() {
        // Initialize quantity input
        quantityInput.setText(String.valueOf(currentQuantity));
        
        // Decrease button
        btnDecrease.setOnClickListener(v -> {
            if (currentQuantity > 1) {
                currentQuantity--;
                quantityInput.setText(String.valueOf(currentQuantity));
                updateTotalPrice();
            }
        });
        
        // Increase button
        btnIncrease.setOnClickListener(v -> {
            if (currentQuantity < 999) { // Set reasonable max limit
                currentQuantity++;
                quantityInput.setText(String.valueOf(currentQuantity));
                updateTotalPrice();
            }
        });
        
        // Quantity input text watcher
        quantityInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    String input = s.toString().trim();
                    if (!input.isEmpty()) {
                        int quantity = Integer.parseInt(input);
                        if (quantity >= 1 && quantity <= 999) {
                            currentQuantity = quantity;
                            updateTotalPrice();
                        } else if (quantity < 1) {
                            currentQuantity = 1;
                            quantityInput.setText("1");
                            quantityInput.setSelection(1);
                        } else if (quantity > 999) {
                            currentQuantity = 999;
                            quantityInput.setText("999");
                            quantityInput.setSelection(3);
                        }
                    } else {
                        // If empty, default to 1
                        currentQuantity = 1;
                    }
                } catch (NumberFormatException e) {
                    // Invalid input, reset to current quantity
                    quantityInput.setText(String.valueOf(currentQuantity));
                    quantityInput.setSelection(String.valueOf(currentQuantity).length());
                }
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
    
    private void setupActionButtons(Button btnCancel, Button btnAddToCart, Button btnBuyNow) {
        // Cancel button
        btnCancel.setOnClickListener(v -> {
            if (dialog != null) {
                dialog.dismiss();
            }
        });
        
        // Add to Cart button
        btnAddToCart.setOnClickListener(v -> {
            if (listener != null) {
                // Ensure we have valid quantity
                if (currentQuantity < 1) currentQuantity = 1;
                
                // Create product copy with selected quantity
                Product productWithQuantity = createProductWithQuantity(product, currentQuantity);
                listener.onAddToCart(productWithQuantity, currentQuantity);
            }
            if (dialog != null) {
                dialog.dismiss();
            }
        });
        
        // Buy Now button
        btnBuyNow.setOnClickListener(v -> {
            if (listener != null) {
                // Ensure we have valid quantity
                if (currentQuantity < 1) currentQuantity = 1;
                
                // Create product copy with selected quantity
                Product productWithQuantity = createProductWithQuantity(product, currentQuantity);
                listener.onBuyNow(productWithQuantity, currentQuantity);
            }
            if (dialog != null) {
                dialog.dismiss();
            }
        });
    }
    
    private void updateTotalPrice() {
        if (totalPriceDisplay != null) {
            double total = unitPrice * currentQuantity;
            totalPriceDisplay.setText(String.format("Br %.2f", total));
        }
    }
    
    private double parsePrice(String priceString) {
        if (priceString == null || priceString.trim().isEmpty()) {
            return 0.0;
        }
        
        try {
            // Remove currency symbols and clean the string
            String cleaned = priceString.replaceAll("[^0-9.,-]", "");
            
            // Handle comma as decimal separator
            if (cleaned.contains(",") && !cleaned.contains(".")) {
                cleaned = cleaned.replace(",", ".");
            } else if (cleaned.contains(",") && cleaned.contains(".")) {
                // Remove commas (thousands separators)
                cleaned = cleaned.replace(",", "");
            }
            
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
    
    /**
     * Create a product copy with quantity information for cart/checkout
     * This preserves the original product while adding quantity data
     */
    private Product createProductWithQuantity(Product original, int quantity) {
        // Create a copy of the product using the correct constructor
        Product productCopy = new Product(
            original.getId(),
            original.getName(), 
            original.getPrice(),
            original.getImageUrl()
        );
        
        // Note: Quantity will be tracked separately in the cart system
        // The calling code will handle the quantity parameter
        
        return productCopy;
    }
    
    public void dismiss() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }
}