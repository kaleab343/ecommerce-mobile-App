package com.example.econ2;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Local wishlist management using SharedPreferences
 * No server changes required - fully local implementation
 */
public class WishlistManager {
    
    private static final String PREF_NAME = "wishlist_prefs";
    private static final String KEY_WISHLIST_IDS = "wishlist_product_ids";
    
    public static boolean isInWishlist(Context context, int productId) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Set<String> wishlistIds = prefs.getStringSet(KEY_WISHLIST_IDS, new HashSet<>());
        return wishlistIds.contains(String.valueOf(productId));
    }
    
    public static void addToWishlist(Context context, int productId) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Set<String> wishlistIds = new HashSet<>(prefs.getStringSet(KEY_WISHLIST_IDS, new HashSet<>()));
        wishlistIds.add(String.valueOf(productId));
        prefs.edit().putStringSet(KEY_WISHLIST_IDS, wishlistIds).apply();
    }
    
    public static void removeFromWishlist(Context context, int productId) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Set<String> wishlistIds = new HashSet<>(prefs.getStringSet(KEY_WISHLIST_IDS, new HashSet<>()));
        wishlistIds.remove(String.valueOf(productId));
        prefs.edit().putStringSet(KEY_WISHLIST_IDS, wishlistIds).apply();
    }
    
    public static void toggleWishlist(Context context, int productId) {
        if (isInWishlist(context, productId)) {
            removeFromWishlist(context, productId);
        } else {
            addToWishlist(context, productId);
        }
    }
    
    public static List<Integer> getWishlistProductIds(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Set<String> wishlistIds = prefs.getStringSet(KEY_WISHLIST_IDS, new HashSet<>());
        List<Integer> ids = new ArrayList<>();
        for (String id : wishlistIds) {
            try {
                ids.add(Integer.parseInt(id));
            } catch (NumberFormatException e) {
                // Skip invalid IDs
            }
        }
        return ids;
    }
    
    public static List<Product> getWishlistProducts(Context context, List<Product> allProducts) {
        List<Integer> wishlistIds = getWishlistProductIds(context);
        List<Product> wishlistProducts = new ArrayList<>();
        
        for (Product product : allProducts) {
            if (wishlistIds.contains(product.getId())) {
                wishlistProducts.add(product);
            }
        }
        
        return wishlistProducts;
    }
    
    public static void clearWishlist(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_WISHLIST_IDS).apply();
    }
    
    public static int getWishlistCount(Context context) {
        return getWishlistProductIds(context).size();
    }
}