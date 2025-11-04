package com.example.econ2;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class sign_in_page extends Fragment {

    // Base URL for the API - USE THE SAME IP ADDRESS FOR BOTH SEND AND VERIFY
    private static final String BASE_URL = "http://192.168.1.2/chapa/mailer/validmailer.php";

    private EditText nameInput, emailInput, phoneInput, passwordInput, confirmPasswordInput;
    private ImageView passwordToggle, confirmPasswordToggle;
    private Button signupButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sign_in_page, container, false);

        // Inputs
        nameInput = view.findViewById(R.id.nameInput);
        emailInput = view.findViewById(R.id.emailInput);
        phoneInput = view.findViewById(R.id.phoneInput);
        passwordInput = view.findViewById(R.id.passwordInput);
        confirmPasswordInput = view.findViewById(R.id.confirmPasswordInput);
        passwordToggle = view.findViewById(R.id.passwordToggle);
        confirmPasswordToggle = view.findViewById(R.id.confirmPasswordToggle);
        signupButton = view.findViewById(R.id.signupButton);

        // Toggle password visibility
        passwordToggle.setOnClickListener(v -> togglePasswordVisibility(passwordInput, passwordToggle));
        confirmPasswordToggle.setOnClickListener(v -> togglePasswordVisibility(confirmPasswordInput, confirmPasswordToggle));

        // Sign up button click
        signupButton.setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            String email = emailInput.getText().toString().trim();
            String phone = phoneInput.getText().toString().trim();
            String password = passwordInput.getText().toString();
            String confirmPassword = confirmPasswordInput.getText().toString();

            boolean hasError = false;

            // Inline validation
            if (name.isEmpty()) { nameInput.setError("Name is required"); hasError = true; } else nameInput.setError(null);
            if (email.isEmpty()) { emailInput.setError("Email is required"); hasError = true; } else emailInput.setError(null);
            if (phone.isEmpty()) { phoneInput.setError("Phone number is required"); hasError = true; } else phoneInput.setError(null);
            if (password.isEmpty()) { passwordInput.setError("Password is required"); hasError = true; }
            else if (password.length() < 6) { passwordInput.setError("Password must be at least 6 characters"); hasError = true; }
            else passwordInput.setError(null);
            if (confirmPassword.isEmpty()) { confirmPasswordInput.setError("Please confirm password"); hasError = true; }
            else if (!password.equals(confirmPassword)) { confirmPasswordInput.setError("Passwords do not match"); hasError = true; }
            else confirmPasswordInput.setError(null);

            if (hasError) return;

            sendUserInfoToServer(name, email, phone, password);
        });

        return view;
    }

    private void togglePasswordVisibility(EditText editText, ImageView toggle) {
        if (editText.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            toggle.setImageResource(R.drawable.ic_eye_open);
        } else {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            toggle.setImageResource(R.drawable.ic_eye_closed);
        }
        editText.setSelection(editText.getText().length());
    }

    private void sendUserInfoToServer(String name, String email, String phone, String password) {
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                conn.setDoOutput(true);

                JSONObject jsonParam = new JSONObject();
                jsonParam.put("action", "send");
                jsonParam.put("name", name);
                jsonParam.put("email", email);
                jsonParam.put("phone", phone);
                jsonParam.put("password", password);
                
                Log.d("SignUp", "Sending request to: " + BASE_URL);
                Log.d("SignUp", "Request JSON: " + jsonParam.toString());

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonParam.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                Log.d("SignUp", "HTTP Response Code: " + responseCode);
                
                BufferedReader br;
                if (responseCode >= 200 && responseCode < 300) {
                    br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
                } else {
                    br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "utf-8"));
                }

                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) response.append(line.trim());
                br.close();
                
                String responseStr = response.toString();
                Log.d("SignUp", "Raw Response: " + responseStr);

                // Parse response with robust error handling
                ResponseData responseData = parseServerResponse(responseStr);
                Log.d("SignUp", "Parsed - Status: " + responseData.status + ", Message: " + responseData.message);

                final String status = responseData.status;
                final String message = responseData.message;

                getActivity().runOnUiThread(() -> {
                    if (status.equals("success") && message.contains("Verification code sent")) {
                        openVerificationDialog(name, email, phone, password);
                    } else if (status.equals("error") && message.toLowerCase().contains("duplicate")) {
                        emailInput.setError("This email is already registered");
                        emailInput.requestFocus();
                        Toast.makeText(getContext(), "This email is already registered. Please use a different email or try logging in.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getContext(), "Registration failed: " + message, Toast.LENGTH_LONG).show();
                    }
                });

                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("SignUp", "Error in sendUserInfoToServer: " + e.getMessage(), e);
                getActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Network error: Please check your connection and try again", Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void openVerificationDialog(String name, String email, String phone, String password) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Enter Verification Code");

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText inputCode = new EditText(getContext());
        inputCode.setHint("8-digit code");
        inputCode.setInputType(InputType.TYPE_CLASS_NUMBER);
        layout.addView(inputCode);

        Button confirmButton = new Button(getContext());
        confirmButton.setText("Confirm");
        layout.addView(confirmButton);

        builder.setView(layout);
        AlertDialog dialog = builder.create();
        dialog.show();

        final int[] attempts = {0};

        confirmButton.setOnClickListener(v -> {
            String enteredCode = inputCode.getText().toString().trim();
            if (enteredCode.isEmpty()) { 
                inputCode.setError("Enter code"); 
                return; 
            }
            inputCode.setError(null);

            attempts[0]++;
            if (attempts[0] > 3) {
                dialog.dismiss();
                Toast.makeText(getContext(), "Too many attempts. Please try signing up again.", Toast.LENGTH_LONG).show();
                return;
            }

            checkCodeWithServer(name, email, phone, password, enteredCode, dialog, attempts[0]);
        });
    }

    private void checkCodeWithServer(String name, String email, String phone, String password,
                                     String code, AlertDialog dialog, int currentAttempt) {
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                conn.setDoOutput(true);

                JSONObject jsonParam = new JSONObject();
                jsonParam.put("action", "verify");
                jsonParam.put("email", email);
                jsonParam.put("code", code);
                jsonParam.put("name", name);
                jsonParam.put("phone", phone);
                jsonParam.put("password", password);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonParam.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                BufferedReader br;
                if (responseCode >= 200 && responseCode < 300) {
                    br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
                } else {
                    br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "utf-8"));
                }
                
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) response.append(line.trim());
                br.close();
                
                String responseStr = response.toString();
                Log.d("SignUp", "Verify Response: " + responseStr);

                // Parse response with robust error handling
                ResponseData responseData = parseServerResponse(responseStr);
                Log.d("SignUp", "Verify Parsed - Status: " + responseData.status + ", Message: " + responseData.message);

                final String status = responseData.status;
                final String message = responseData.message;

                getActivity().runOnUiThread(() -> {
                    if (status.equals("pass") || status.equals("success")) {
                        Toast.makeText(getContext(), "Registration successful! Redirecting to login...", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();

                        // Switch to BlankFragment after successful registration
                        if (getActivity() != null) {
                            getActivity().getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.overlayContainer, new BlankFragment())
                                    .commit();
                        }
                    } else {
                        // Handle verification errors
                        String errorMessage = getErrorMessage(message);
                        boolean shouldDismiss = shouldDismissDialog(message);
                        
                        if (shouldDismiss || currentAttempt >= 3) {
                            dialog.dismiss();
                            if (shouldDismiss) {
                                // Clear form for retry
                                clearForm();
                            }
                            Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("SignUp", "Error in checkCodeWithServer: " + e.getMessage(), e);
                getActivity().runOnUiThread(() -> {
                    dialog.dismiss();
                    Toast.makeText(getContext(), "Network error: Please check your connection and try again", Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    // Helper class for response data
    private static class ResponseData {
        String status;
        String message;
        
        ResponseData(String status, String message) {
            this.status = status;
            this.message = message;
        }
    }

    // Robust JSON parsing that handles malformed responses
    private ResponseData parseServerResponse(String responseStr) {
        try {
            // First try to parse as single JSON
            JSONObject responseJson = new JSONObject(responseStr);
            return new ResponseData(
                responseJson.optString("status", "error"),
                responseJson.optString("message", "No message")
            );
        } catch (Exception e) {
            Log.w("SignUp", "Failed to parse as single JSON, trying to extract valid JSON");
            
            // Handle malformed responses with multiple JSON objects
            if (responseStr.contains("}{")) {
                String[] parts = responseStr.split("}\\{");
                for (int i = parts.length - 1; i >= 0; i--) {
                    try {
                        String jsonStr = parts[i];
                        if (!jsonStr.startsWith("{")) jsonStr = "{" + jsonStr;
                        if (!jsonStr.endsWith("}")) jsonStr = jsonStr + "}";
                        
                        JSONObject json = new JSONObject(jsonStr);
                        String status = json.optString("status", "error");
                        String message = json.optString("message", "No message");
                        
                        // Prefer success/pass status if available
                        if (status.equals("success") || status.equals("pass")) {
                            return new ResponseData(status, message);
                        }
                        
                        // Return the last valid JSON if no success found
                        if (i == parts.length - 1) {
                            return new ResponseData(status, message);
                        }
                    } catch (Exception ex) {
                        continue;
                    }
                }
            }
            
            // If all parsing fails, return error
            return new ResponseData("error", "Unable to parse server response");
        }
    }

    // Get user-friendly error message
    private String getErrorMessage(String message) {
        if (message.toLowerCase().contains("verification code not found") || 
            message.toLowerCase().contains("not found")) {
            return "Verification code not found. The code may have expired or you need to request a new one.\n\nPlease try signing up again.";
        } else if (message.toLowerCase().contains("expired")) {
            return "❌ Verification code expired (30 minutes)\n\nYour code has expired. Please sign up again to receive a new code.";
        } else if (message.toLowerCase().contains("incorrect verification code") || 
                   message.toLowerCase().contains("incorrect")) {
            return "❌ Invalid verification code\n\nPlease enter the 8-digit code from your email.";
        } else if (message.isEmpty()) {
            return "Verification failed. Please try again.";
        } else {
            return message;
        }
    }

    // Determine if dialog should be dismissed
    private boolean shouldDismissDialog(String message) {
        return message.toLowerCase().contains("verification code not found") ||
               message.toLowerCase().contains("not found") ||
               message.toLowerCase().contains("expired");
    }

    // Clear form fields
    private void clearForm() {
        nameInput.setText("");
        emailInput.setText("");
        phoneInput.setText("");
        passwordInput.setText("");
        confirmPasswordInput.setText("");
    }
}
