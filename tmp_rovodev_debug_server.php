<?php
// Debug script to test what's happening on the server
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Origin: *");

echo "=== SERVER DEBUG INFO ===\n\n";

// Check if get_recites.php exists
$get_recites_path = __DIR__ . '/get_recites.php';
echo "Looking for get_recites.php at: " . $get_recites_path . "\n";
echo "File exists: " . (file_exists($get_recites_path) ? "YES" : "NO") . "\n\n";

// Check if db.php exists
$db_path = __DIR__ . '/db.php';
echo "Looking for db.php at: " . $db_path . "\n";
echo "Database file exists: " . (file_exists($db_path) ? "YES" : "NO") . "\n\n";

// List all PHP files in current directory
echo "PHP files in current directory:\n";
$files = glob(__DIR__ . '/*.php');
foreach ($files as $file) {
    echo "- " . basename($file) . "\n";
}

echo "\n=== REQUEST INFO ===\n";
echo "Request Method: " . $_SERVER['REQUEST_METHOD'] . "\n";
echo "Request URI: " . $_SERVER['REQUEST_URI'] . "\n";
echo "Server Name: " . $_SERVER['SERVER_NAME'] . "\n";
echo "Document Root: " . $_SERVER['DOCUMENT_ROOT'] . "\n";

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    echo "\nPOST Data received:\n";
    foreach ($_POST as $key => $value) {
        echo "- $key: $value\n";
    }
}
?>