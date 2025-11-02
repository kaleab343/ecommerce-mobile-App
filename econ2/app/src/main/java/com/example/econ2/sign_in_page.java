package com.example.econ2;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
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
    // Update this to match your actual server IP
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
                URL url = new URL(BASE_URL);  // Fixed: Use constant instead of hardcoded IP
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);

                JSONObject jsonParam = new JSONObject();
                jsonParam.put("action", "send");
                jsonParam.put("name", name);
                jsonParam.put("email", email);
                jsonParam.put("phone", phone);
                jsonParam.put("password", password);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonParam.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) response.append(line.trim());
                br.close();

                String responseStr = response.toString();
                System.out.println("Send Response - Full Response: " + responseStr);

                // Handle malformed response (multiple JSON objects concatenated)
                JSONObject responseJson = null;
                String finalStatus = "error";
                String finalMessage = "No message";

                // Check if response contains multiple JSON objects
                if (responseStr.contains("}{\"")) {
                    System.out.println("Multiple JSON responses detected");
                    String[] parts = responseStr.split("\\}\\{");

                    // Try to find the success response first
                    for (int i = parts.length - 1; i >= 0; i--) {
                        try {
                            String jsonStr = parts[i];
                            if (!jsonStr.startsWith("{")) jsonStr = "{" + jsonStr;
                            if (!jsonStr.endsWith("}")) jsonStr = jsonStr + "}";

                            JSONObject json = new JSONObject(jsonStr);
                            String jsonStatus = json.optString("status", "error");
                            String jsonMessage = json.optString("message", "");

                            // Prefer success status if found
                            if (jsonStatus.equals("success")) {
                                responseJson = json;
                                finalStatus = jsonStatus;
                                finalMessage = jsonMessage;
                                System.out.println("Found success response: " + jsonStr);
                                break;
                            }

                            // If no success found yet, keep the last valid JSON
                            if (i == parts.length - 1) {
                                responseJson = json;
                                finalStatus = jsonStatus;
                                finalMessage = jsonMessage;
                            }
                        } catch (Exception e) {
                            continue;
                        }
                    }
                } else {
                    // Single JSON response
                    try {
                        responseJson = new JSONObject(responseStr);
                        finalStatus = responseJson.optString("status", "error");
                        finalMessage = responseJson.optString("message", "No message");
                    } catch (Exception e) {
                        System.out.println("Failed to parse single JSON response: " + e.getMessage());
                    }
                }

                System.out.println("Send Response - Status: " + finalStatus + ", Message: " + finalMessage);

                // Create final variables for lambda
                final String status = finalStatus;
                final String message = finalMessage;

                getActivity().runOnUiThread(() -> {
                    if (status.equals("success") && message.contains("Verification code sent")) {
                        openVerificationDialog(name, email, phone, password);
                    } else if (status.equals("error") && (message.toLowerCase().contains("duplicate") ||
                            message.toLowerCase().contains("already exists") ||
                            message.toLowerCase().contains("email already"))) {
                        Toast.makeText(getContext(), "Account already exists. Redirecting to login...", Toast.LENGTH_SHORT).show();

                        // Redirect to login page instead of showing error
                        if (getActivity() != null) {
                            getActivity().getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.overlayContainer, new BlankFragment())
                                    .commit();
                        }
                    } else {
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                    }
                });

                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                getActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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
            if (enteredCode.isEmpty()) { inputCode.setError("Enter code"); return; }
            inputCode.setError(null);

            attempts[0]++;
            if (attempts[0] > 3) {
                dialog.dismiss();
                Toast.makeText(getContext(), "Suspicious activity, try later", Toast.LENGTH_LONG).show();
                return;
            }

            checkCodeWithServer(name, email, phone, password, enteredCode, dialog, attempts[0]);
        });
    }

    private void checkCodeWithServer(String name, String email, String phone, String password,
                                     String code, AlertDialog dialog, int currentAttempt) {
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL);  // Fixed: Use constant instead of hardcoded IP (was 192.168.1.3)
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setRequestProperty("Accept", "application/json");
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

                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) response.append(line.trim());
                br.close();

                String responseStr = response.toString();
                System.out.println("Verify Response - Full Response: " + responseStr);

                // Handle malformed response (multiple JSON objects concatenated)
                JSONObject responseJson = null;
                String finalStatus = "error";
                String finalMessage = "No message";

                // Check if response contains multiple JSON objects
                if (responseStr.contains("}{\"")) {
                    System.out.println("Multiple JSON responses detected in verify");
                    String[] parts = responseStr.split("\\}\\{");

                    // Try to find the success/pass response first
                    for (int i = parts.length - 1; i >= 0; i--) {
                        try {
                            String jsonStr = parts[i];
                            if (!jsonStr.startsWith("{")) jsonStr = "{" + jsonStr;
                            if (!jsonStr.endsWith("}")) jsonStr = jsonStr + "}";

                            JSONObject json = new JSONObject(jsonStr);
                            String jsonStatus = json.optString("status", "error");
                            String jsonMessage = json.optString("message", "");

                            // Prefer success/pass status if found
                            if (jsonStatus.equals("success") || jsonStatus.equals("pass")) {
                                responseJson = json;
                                finalStatus = jsonStatus;
                                finalMessage = jsonMessage;
                                System.out.println("Found success/pass response: " + jsonStr);
                                break;
                            }

                            // If no success found yet, keep the last valid JSON
                            if (i == parts.length - 1) {
                                responseJson = json;
                                finalStatus = jsonStatus;
                                finalMessage = jsonMessage;
                            }
                        } catch (Exception e) {
                            continue;
                        }
                    }
                } else {
                    // Single JSON response
                    try {
                        responseJson = new JSONObject(responseStr);
                        finalStatus = responseJson.optString("status", "error");
                        finalMessage = responseJson.optString("message", "No message");
                    } catch (Exception e) {
                        System.out.println("Failed to parse single JSON response: " + e.getMessage());
                    }
                }

                System.out.println("Verify Response - Status: " + finalStatus + ", Message: " + finalMessage);

                // Create final variables for lambda
                final String status = finalStatus;
                final String message = finalMessage;

                getActivity().runOnUiThread(() -> {
                    System.out.println("DEBUG: Processing verification response - Status: " + status + ", Message: " + message);

                    if (status.equals("pass")) {
                        // Success! User is registered
                        System.out.println("DEBUG: Verification successful, redirecting to login");
                        Toast.makeText(getContext(), "Registration successful! Redirecting to login...", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();

                        // Switch to BlankFragment after successful registration
                        if (getActivity() != null) {
                            getActivity().getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.overlayContainer, new BlankFragment())
                                    .commit();
                        }

                    } else {
                        // For ANY error (including "Email not found"), redirect to login
                        System.out.println("DEBUG: Verification failed with status: " + status + ", redirecting to login");
                        dialog.dismiss();
                        Toast.makeText(getContext(), "Redirecting to login...", Toast.LENGTH_SHORT).show();

                        // Always redirect to login page for any verification issue
                        if (getActivity() != null) {
                            getActivity().getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.overlayContainer, new BlankFragment())
                                    .commit();
                        }
                    }
                });

                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                getActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}

