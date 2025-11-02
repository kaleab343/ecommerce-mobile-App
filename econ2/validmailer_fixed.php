<?php
// validmailer.php
// Handles sending verification codes and inserting verified users into DB

header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");

use PHPMailer\PHPMailer\PHPMailer;
use PHPMailer\PHPMailer\Exception;

require __DIR__ . '/../vendor/autoload.php';
require __DIR__ . '/db.php'; // make sure this path is correct

// -------- CONFIG --------
$MAIL_FROM_EMAIL = 'kaleabzelalem82@gmail.com';
$MAIL_FROM_NAME  = 'Kaleab PHP Mailer';
$SMTP_USERNAME  = 'kaleabzelalem82@gmail.com';
$SMTP_PASSWORD  = 'siey ozvj lxsp nhrp';
$SMTP_HOST      = 'smtp.gmail.com';
$SMTP_PORT      = 587;
$SMTP_SECURE    = 'tls';

$CODES_FILE = __DIR__ . '/verification_codes.json';
$CODE_TTL_SECONDS = 3 * 60; // 3 minutes

// -------- helpers --------
function respond($arr) {
    echo json_encode($arr);
    exit;
}

function normalize_email($email) {
    // Convert to lowercase and trim - this ensures consistent matching
    return strtolower(trim($email));
}

function load_codes($file) {
    if (!file_exists($file)) return [];
    $json = file_get_contents($file);
    if ($json === false) return [];
    $data = json_decode($json, true);
    return is_array($data) ? $data : [];
}

function save_codes($file, $data) {
    $tmp = $file . '.tmp';
    file_put_contents($tmp, json_encode($data, JSON_PRETTY_PRINT));
    rename($tmp, $file);
}

function generate_8_digit_code() {
    $n = random_int(0, 99999999);
    return str_pad((string)$n, 8, '0', STR_PAD_LEFT);
}

function set_code_record(&$codes, $email, $code, $ttl) {
    // Normalize email to ensure consistent key
    $normalized_email = normalize_email($email);
    $codes[$normalized_email] = [
        'code' => $code,
        'expires_at' => time() + $ttl,
        'original_email' => $email // Store original for email sending
    ];
}

function get_code_record($codes, $email) {
    // Normalize email to match how it was stored
    $normalized_email = normalize_email($email);
    return isset($codes[$normalized_email]) ? $codes[$normalized_email] : null;
}

// -------- read input --------
$raw = file_get_contents("php://input");
$data = json_decode($raw, true);

// Log all requests to a file  
$log_file = __DIR__ . '/request_debug.log';
$timestamp = date('Y-m-d H:i:s');
$ip = $_SERVER['REMOTE_ADDR'] ?? 'unknown';
$log_entry = "[" . $timestamp . "] Action: " . ($data['action'] ?? 'none') . " | IP: " . $ip . " | Data: " . $raw . "\n";
file_put_contents($log_file, $log_entry, FILE_APPEND);

if (!is_array($data)) respond(['status'=>'error','message'=>'Invalid JSON']);

$action = strtolower(trim($data['action'] ?? ''));

// ========== SEND CODE ==========
if ($action === 'send') {
    $name = trim($data['name'] ?? '');
    $email = trim($data['email'] ?? '');
    $phone = trim($data['phone'] ?? '');
    $password = trim($data['password'] ?? '');

    if ($name === '' || $email === '' || $phone === '' || $password === '') {
        respond(['status'=>'error','message'=>'All fields (name,email,phone,password) are required']);
    }

    $code = generate_8_digit_code();
    $codes = load_codes($CODES_FILE);
    
    // Log before saving
    file_put_contents($log_file, "SAVING CODE for email: " . $email . " (normalized: " . normalize_email($email) . ") | Code: " . $code . "\n", FILE_APPEND);
    
    set_code_record($codes, $email, $code, $CODE_TTL_SECONDS);
    save_codes($CODES_FILE, $codes);
    
    // Log after saving
    file_put_contents($log_file, "SAVED. Total codes: " . count($codes) . " | Keys: " . implode(", ", array_keys($codes)) . "\n", FILE_APPEND);

    try {
        $mail = new PHPMailer(true);
        $mail->isSMTP();
        $mail->Host = $SMTP_HOST;
        $mail->SMTPAuth = true;
        $mail->Username = $SMTP_USERNAME;
        $mail->Password = $SMTP_PASSWORD;
        $mail->SMTPSecure = $SMTP_SECURE;
        $mail->Port = $SMTP_PORT;

        $mail->setFrom($MAIL_FROM_EMAIL, $MAIL_FROM_NAME);
        $mail->addAddress($email);

        $mail->isHTML(true);
        $mail->Subject = 'Your verification code';
        $mail->Body = "<p>Hi " . htmlspecialchars($name) . ",</p>
                       <p>Your verification code is: <strong>" . htmlspecialchars($code) . "</strong></p>
                       <p>This code is valid for 3 minutes.</p>";

        $mail->send();
        respond([
            'status' => 'success',
            'message' => 'Verification code sent to email. Open the dialog to enter it.',
            'expires_in' => $CODE_TTL_SECONDS
        ]);
    } catch (Exception $e) {
        respond(['status'=>'error','message'=>'Mail error: '.$mail->ErrorInfo]);
    }
}

// ========== VERIFY CODE ==========
elseif ($action === 'verify') {
    $name = trim($data['name'] ?? '');
    $email = trim($data['email'] ?? '');
    $phone = trim($data['phone'] ?? '');
    $password = trim($data['password'] ?? '');
    $input_code = trim($data['code'] ?? '');

    if ($email === '' || $input_code === '') {
        respond(['status'=>'error','message'=>'Email and code are required']);
    }

    $codes = load_codes($CODES_FILE);
    
    // Write to log file for debugging
    $log_file = __DIR__ . '/verify_debug.log';
    $normalized_email = normalize_email($email);
    
    $log_data = "\n========================================\n";
    $log_data .= date('Y-m-d H:i:s') . " VERIFY ATTEMPT\n";
    $log_data .= "Original email: " . $email . "\n";
    $log_data .= "Normalized email: " . $normalized_email . "\n";
    $log_data .= "Code input: " . $input_code . "\n";
    $log_data .= "Codes file exists: " . (file_exists($CODES_FILE) ? "YES" : "NO") . "\n";
    $log_data .= "Total codes in memory: " . count($codes) . "\n";
    $log_data .= "Available email keys: " . implode(", ", array_keys($codes)) . "\n";
    $log_data .= "Full codes data:\n" . json_encode($codes, JSON_PRETTY_PRINT) . "\n";
    file_put_contents($log_file, $log_data, FILE_APPEND);
    
    // Use the new get_code_record function which normalizes the email
    $record = get_code_record($codes, $email);
    
    if ($record === null) {
        $debug_msg = "Email '" . $email . "' (normalized: " . $normalized_email . ") not found in verification codes. Available keys: " . implode(", ", array_keys($codes));
        file_put_contents($log_file, "FAILED: " . $debug_msg . "\n========================================\n", FILE_APPEND);
        respond(['status'=>'error','message'=>'Email not found in verification records. Please try signing up again.']);
    }
    
    file_put_contents($log_file, "FOUND RECORD for normalized email: " . $normalized_email . "\n", FILE_APPEND);

    if (time() > $record['expires_at']) {
        unset($codes[$normalized_email]);
        save_codes($CODES_FILE, $codes);
        file_put_contents($log_file, "CODE EXPIRED\n========================================\n", FILE_APPEND);
        respond(['status'=>'error','message'=>'Code expired']);
    }

    if ($record['code'] !== $input_code) {
        file_put_contents($log_file, "INCORRECT CODE. Expected: " . $record['code'] . ", Got: " . $input_code . "\n========================================\n", FILE_APPEND);
        respond(['status'=>'error','message'=>'Incorrect code']);
    }

    // Success! Remove code
    unset($codes[$normalized_email]);
    save_codes($CODES_FILE, $codes);
    file_put_contents($log_file, "VERIFICATION SUCCESS!\n========================================\n", FILE_APPEND);

    // Insert into database
    try {
        global $conn;
        
        // Check if email already exists
        $check = $conn->prepare("SELECT email FROM customer_detail WHERE email = :email");
        $check->bindParam(':email', $email);
        $check->execute();
        
        if ($check->rowCount() > 0) {
            respond(['status'=>'error','message'=>'This email is already registered']);
        }
        
        $stmt = $conn->prepare("INSERT INTO customer_detail (name, email, password, phone_number) VALUES (:name, :email, :password, :phone_number)");
        $hashedPassword = password_hash($password, PASSWORD_DEFAULT);
        $stmt->bindParam(':name', $name);
        $stmt->bindParam(':email', $email);
        $stmt->bindParam(':password', $hashedPassword);
        $stmt->bindParam(':phone_number', $phone);
        $stmt->execute();

        respond(['status'=>'pass','message'=>'Verification successful and user saved']);
    } catch (PDOException $e) {
        file_put_contents($log_file, "DB ERROR: " . $e->getMessage() . "\n========================================\n", FILE_APPEND);
        respond(['status'=>'error','message'=>'Database error. Please try again.']);
    }
}

else {
    respond(['status'=>'error','message'=>'Invalid action. Use "send" or "verify"']);
}
?>
