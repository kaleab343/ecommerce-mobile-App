<?php
// Test script for add product functionality
require_once 'product/db.php';

try {
    $pdo = getDB();
    echo "Database connection successful\n";
    
    // Create a test image (small 100x100 red square)
    $testImage = imagecreate(100, 100);
    $red = imagecolorallocate($testImage, 255, 0, 0);
    imagefill($testImage, 0, 0, $red);
    
    // Convert to binary data
    ob_start();
    imagejpeg($testImage, null, 80);
    $imageData = ob_get_contents();
    ob_end_clean();
    imagedestroy($testImage);
    
    echo "Test image created: " . strlen($imageData) . " bytes\n";
    
    // Test the add product functionality
    $name = "Test Product " . time();
    $price = 99.99;
    $size = 1.0;
    $amount = 5;
    
    $stmt = $pdo->prepare("INSERT INTO product (name, price, size, amount, time, image) VALUES (?, ?, ?, ?, NOW(), ?)");
    $stmt->bindParam(1, $name);
    $stmt->bindParam(2, $price);
    $stmt->bindParam(3, $size);
    $stmt->bindParam(4, $amount);
    $stmt->bindParam(5, $imageData, PDO::PARAM_LOB);
    
    if ($stmt->execute()) {
        echo "✓ Test product added successfully\n";
        echo "Product ID: " . $pdo->lastInsertId() . "\n";
        echo "Name: $name\n";
        echo "Price: $price\n";
        echo "Amount: $amount\n";
    } else {
        echo "✗ Failed to add test product\n";
    }
    
} catch (Exception $e) {
    echo "Error: " . $e->getMessage() . "\n";
}
?>
