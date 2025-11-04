<?php
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Origin: *");

require_once "db.php";

$response = array();

try {
    // ✅ Select only products where amount >= 1
    $query = "SELECT id, name, price, size, time, amount, image_url FROM product WHERE amount >= 1";
    $stmt = $conn->prepare($query);
    $stmt->execute();

    $products = array();

    $base_url = "http://" . $_SERVER['HTTP_HOST'];

    while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
        $image_url = $row["image_url"];
        if (strpos($image_url, "http") !== 0) {
            $image_url = $base_url . $image_url;
        }

        $products[] = array(
            "id" => $row["id"],
            "name" => $row["name"],
            "price" => $row["price"],
            "size" => $row["size"],
            "time" => $row["time"],
            "amount" => $row["amount"],
            "image_url" => $image_url
        );
    }

    if (count($products) > 0) {
        $response["status"] = "success";
        $response["products"] = $products;
    } else {
        $response["status"] = "error";
        $response["message"] = "No available products (all out of stock).";
    }

} catch (PDOException $e) {
    $response["error"] = "Database error: " . $e->getMessage();
}

echo json_encode($response, JSON_PRETTY_PRINT | JSON_UNESCAPED_SLASHES);
?>
