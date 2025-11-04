<?php
// webhook_handler.php

// Enable error reporting for debugging
error_reporting(E_ALL);
ini_set('display_errors', 1);

// Get the raw POST data
$request_data = file_get_contents('php://input');

// Log the webhook data for debugging
error_log("Webhook received: " . $request_data);

// Decode the JSON data
$data = json_decode($request_data, true);

// Validate the webhook (replace with your actual secret key)
// Prefer environment variable CHAPA_WEBHOOK_SECRET, fallback to placeholder
$secret_key = getenv('CHAPA_WEBHOOK_SECRET') ?: 'YOUR_CHAPA_WEBHOOK_SECRET_KEY';
if (!isset($_SERVER['HTTP_X_CHAPA_SIGNATURE']) || $_SERVER['HTTP_X_CHAPA_SIGNATURE'] !== hash_hmac('sha256', $request_data, $secret_key)) {
    error_log('Invalid webhook signature!');
    http_response_code(403); // Forbidden
    exit;
}

// Check if the data was decoded successfully
if ($data === null && json_last_error() !== JSON_ERROR_NONE) {
    error_log('Error decoding JSON: ' . json_last_error_msg());
    http_response_code(400); // Bad Request
    exit;
}

// Check for the required data (adjust based on your actual Chapa webhook payload)
if (!isset($data['status']) || !isset($data['reference'])) {
    error_log('Missing required data in webhook payload!');
    http_response_code(400); // Bad Request
    exit;
}

// Check the transaction status
$transaction_status = $data['status'];
$transaction_reference = $data['reference'];

if ($transaction_status === 'success') {
    // Payment was successful!

    // TODO: Update your database with the successful transaction
    error_log("Transaction $transaction_reference successful!");

    // Send mobile API message (replace with your actual mobile API endpoint)
    $mobile_api_url = 'YOUR_MOBILE_API_ENDPOINT';
    $mobile_api_data = array(
        'transaction_reference' => $transaction_reference,
        'message' => 'Payment successful!',
        // Add any other data you want to send to your mobile app
    );

    // Use CURL to send the message to your mobile API
    $ch = curl_init($mobile_api_url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($mobile_api_data));
    curl_setopt($ch, CURLOPT_HTTPHEADER, array('Content-Type: application/json'));

    $response = curl_exec($ch);

    if (curl_errno($ch)) {
        error_log('CURL error: ' . curl_error($ch));
    } else {
        error_log('Mobile API response: ' . $response);
    }

    curl_close($ch);

} else {
    // Payment failed or is pending
    error_log("Transaction $transaction_reference status: $transaction_status");
    // TODO: Handle failed or pending payments
}

// Respond to Chapa with a 200 OK status
http_response_code(200);
exit;
?>