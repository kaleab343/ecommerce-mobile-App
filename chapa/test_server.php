<?php
// Test script to verify server responses
echo "Testing server responses...\n\n";

// Test 1: Send verification code
echo "=== TEST 1: Send Verification Code ===\n";
$sendData = [
    'action' => 'send',
    'name' => 'Test User',
    'email' => 'test@example.com',
    'phone' => '1234567890',
    'password' => 'password123'
];

$sendResponse = testServer('send', $sendData);
echo "Send Response: " . $sendResponse . "\n\n";

// Test 2: Verify with wrong code
echo "=== TEST 2: Verify with Wrong Code ===\n";
$verifyData = [
    'action' => 'verify',
    'name' => 'Test User',
    'email' => 'test@example.com',
    'phone' => '1234567890',
    'password' => 'password123',
    'code' => '12345678'
];

$verifyResponse = testServer('verify', $verifyData);
echo "Verify Response: " . $verifyResponse . "\n\n";

function testServer($action, $data) {
    $url = 'http://192.168.1.2/chapa/mailer/validmailer.php';
    
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $url);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($data));
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        'Content-Type: application/json',
        'Accept: application/json'
    ]);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_TIMEOUT, 30);
    
    $response = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);
    
    return "HTTP $httpCode: $response";
}
?>
