<?php
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Origin: *");

require_once "db.php";

$response = array();

try {
    // Get user email from POST data
    $user_email = isset($_POST['email']) ? $_POST['email'] : '';
    
    if (empty($user_email)) {
        $response["status"] = "error";
        $response["message"] = "Email is required";
        echo json_encode($response, JSON_PRETTY_PRINT | JSON_UNESCAPED_SLASHES);
        exit;
    }

    // Query to get user's order history with product details
    $query = "
        SELECT 
            o.id as order_id,
            o.total_amount,
            o.currency,
            o.tx_ref,
            o.status,
            o.payment_method,
            o.created_at as order_date,
            oi.product_name,
            oi.product_price,
            oi.quantity,
            oi.subtotal,
            p.image_url,
            p.size
        FROM orders o
        LEFT JOIN order_items oi ON o.id = oi.order_id
        LEFT JOIN product p ON oi.product_id = p.id
        WHERE o.user_email = ?
        ORDER BY o.created_at DESC
    ";
    
    $stmt = $conn->prepare($query);
    $stmt->execute([$user_email]);

    $orders = array();
    $base_url = "http://" . $_SERVER['HTTP_HOST'];

    while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
        $order_id = $row["order_id"];
        
        // Handle image URL
        $image_url = $row["image_url"];
        if ($image_url && strpos($image_url, "http") !== 0) {
            $image_url = $base_url . $image_url;
        }

        // Create order item
        $item = array(
            "product_name" => $row["product_name"],
            "product_price" => $row["product_price"],
            "quantity" => $row["quantity"],
            "subtotal" => $row["subtotal"],
            "image_url" => $image_url,
            "size" => $row["size"]
        );

        // If this order doesn't exist yet, create it
        if (!isset($orders[$order_id])) {
            $orders[$order_id] = array(
                "id" => $order_id,
                "total_amount" => $row["total_amount"],
                "currency" => $row["currency"],
                "tx_ref" => $row["tx_ref"],
                "status" => $row["status"],
                "payment_method" => $row["payment_method"],
                "date_created" => $row["order_date"],
                "user_id" => $user_email,
                "category" => $row["status"],
                "items" => array(),
                "total_items" => 0
            );
        }

        // Add item to order
        $orders[$order_id]["items"][] = $item;
        $orders[$order_id]["total_items"] = count($orders[$order_id]["items"]);
    }

    // Convert to indexed array and create titles with all product names
    $recites = array();
    foreach ($orders as $order) {
        $product_names = array();
        foreach ($order["items"] as $item) {
            $product_names[] = $item["product_name"];
        }
        
        $order["title"] = "Order #" . $order["id"] . " - " . implode(", ", $product_names);
        $order["content"] = "Payment Status: " . ucfirst($order["status"]) . "\n" .
                           "Total Items: " . $order["total_items"] . "\n" .
                           "Total Amount: " . $order["total_amount"] . " " . $order["currency"] . "\n" .
                           "Payment Method: " . ($order["payment_method"] ? $order["payment_method"] : "N/A") . "\n" .
                           "Transaction Ref: " . $order["tx_ref"];
        
        $recites[] = $order;
    }

    if (count($recites) > 0) {
        $response["status"] = "success";
        $response["recites"] = $recites;
        $response["count"] = count($recites);
    } else {
        $response["status"] = "success";
        $response["recites"] = array();
        $response["count"] = 0;
        $response["message"] = "No order history found.";
    }

} catch (PDOException $e) {
    $response["status"] = "error";
    $response["message"] = "Database error: " . $e->getMessage();
}

echo json_encode($response, JSON_PRETTY_PRINT | JSON_UNESCAPED_SLASHES);
?>