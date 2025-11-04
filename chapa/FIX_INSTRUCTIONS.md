# How to Fix "Email Not Found" Error

## Problem
Your Android app was using different IP addresses for send (192.168.1.2) and verify (192.168.1.3). The verification codes are stored on the server where you SEND, so when you VERIFY on a different server, it can't find them.

## Solution

### Step 1: Update Your Android Code
You have two options:

**Option A:** Use the updated file I created
- Copy `sign_in_page_updated.java` 
- Replace your `sign_in_page.java` in your Android project

**Option B:** Make the changes manually

In your `sign_in_page.java` file:

1. Add a constant at the top of the class (around line 25):
```java
private static final String BASE_URL = "http://192.168.1.2/chapa/mailer/validmailer.php";
```

2. Change line 76 in `sendUserInfoToServer`:
```java
URL url = new URL(BASE_URL);  // Was: "http://192.168.1.2/..."
```

3. Change line 153 in `checkCodeWithServer`:
```java
URL url = new URL(BASE_URL);  // Was: "http://192.168.1.3/..."
```

### Step 2: Make Sure Your Server is Running
- Ensure Apache/XAMPP is running
- Verify your server IP is 192.168.1.2 (or update BASE_URL if different)
- Test by visiting: http://192.168.1.2/chapa/mailer/validmailer.php (should show an error about invalid JSON)

### Step 3: Rebuild Your App
1. Clean your Android project
2. Rebuild
3. Install on your device
4. Try signing up again

### Step 4: Check Server Logs
If still not working, check PHP error logs:
- Location: `C:\xampp\apache\logs\error.log`
- Look for "DEBUG VERIFY" messages to see what email is being searched

## Why This Happens

The verification code workflow:
1. User submits form → Code sent to email → Code stored in `verification_codes.json` on server .2
2. User enters code → App checks server .3 → Server .3 doesn't have the code → "Email not found"

Using the same server for both operations fixes this.

## Still Having Issues?

Check:
1. **Are you still seeing different IPs in logcat?** - Make sure you rebuilt the app with the fix
2. **Is the verification dialog appearing?** - This means the send worked
3. **What error message do you see?** - "email not found" means server issue, "incorrect code" means wrong code entered

