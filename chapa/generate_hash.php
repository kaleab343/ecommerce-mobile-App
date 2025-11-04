<?php
$secretHash = bin2hex(random_bytes(32)); // Generate a 64-character hexadecimal string
echo $secretHash;
?>