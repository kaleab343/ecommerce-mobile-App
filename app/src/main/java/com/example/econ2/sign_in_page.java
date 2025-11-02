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

    // Base URL for the API
    private static final String BASE_URL = ApiConfig.VALID_MAILER;

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
            // Remove all spaces from the name
            String name = nameInput.getText().toString().replaceAll("\\s+", "");
            String email = emailInput.getText().toString().trim();
            String phone = phoneInput.getText().toString().trim();
            String password = passwordInput.getText().toString();
            String confirmPassword = confirmPasswordInput.getText().toString();

            boolean hasError = false;

            // Inline validation
            if (name.isEmpty()) {
                nameInput.setError("Name is required");
                hasError = true;
            } else nameInput.setError(null);

            if (email.isEmpty()) {
                emailInput.setError("Email is required");
                hasError = true;
            } else emailInput.setError(null);

            if (phone.isEmpty()) {
                phoneInput.setError("Phone number is required");
                hasError = true;
            } else phoneInput.setError(null);

            if (password.isEmpty()) {
                passwordInput.setError("Password is required");
                hasError = true;
            } else if (password.length() < 6) {
                passwordInput.setError("Password must be at least 6 characters");
                hasError = true;
            } else passwordInput.setError(null);

            if (confirmPassword.isEmpty()) {
                confirmPasswordInput.setError("Please confirm password");
                hasError = true;
            } else if (!password.equals(confirmPassword)) {
                confirmPasswordInput.setError("Passwords do not match");
                hasError = true;
            } else confirmPasswordInput.setError(null);

            if (hasError) return;

            // Send user data to server
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
                JSONObject responseJson = null;
                String finalStatus = "error";
                String finalMessage = "No message";

                if (responseStr.contains("}{\"")) {
                    String[] parts = responseStr.split("\\}\\{");
                    for (int i = parts.length - 1; i >= 0; i--) {
                        try {
                            String jsonStr = parts[i];
                            if (!jsonStr.startsWith("{")) jsonStr = "{" + jsonStr;
                            if (!jsonStr.endsWith("}")) jsonStr = jsonStr + "}";
                            JSONObject json = new JSONObject(jsonStr);
                            String jsonStatus = json.optString("status", "error");
                            String jsonMessage = json.optString("message", "");
                            if (jsonStatus.equals("success")) {
                                responseJson = json;
                                finalStatus = jsonStatus;
                                finalMessage = jsonMessage;
                                break;
                            }
                        } catch (Exception ignored) {}
                    }
                } else {
                    try {
                        responseJson = new JSONObject(responseStr);
                        finalStatus = responseJson.optString("status", "error");
                        finalMessage = responseJson.optString("message", "No message");
                    } catch (Exception ignored) {}
                }

                String status = finalStatus;
                String message = finalMessage;

                getActivity().runOnUiThread(() -> {
                    if (status.equals("success") && message.contains("Verification code sent")) {
                        openVerificationDialog(name, email, phone, password);
                    } else if (status.equals("error") && (message.toLowerCase().contains("duplicate") ||
                            message.toLowerCase().contains("already exists") ||
                            message.toLowerCase().contains("email already"))) {
                        SafeToast.show(getContext(), "Account already exists. Redirecting to login...", Toast.LENGTH_SHORT);

                        if (getActivity() != null) {
                            getActivity().getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.overlayContainer, new BlankFragment())
                                    .commit();
                        }
                    } else {
                        SafeToast.show(getContext(), message, Toast.LENGTH_SHORT);
                    }
                });

                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                getActivity().runOnUiThread(() ->
                        SafeToast.show(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT));
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
                SafeToast.show(getContext(), "Suspicious activity, try later", Toast.LENGTH_LONG);
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
                JSONObject responseJson = null;
                String finalStatus = "error";
                String finalMessage = "No message";

                if (responseStr.contains("}{\"")) {
                    String[] parts = responseStr.split("\\}\\{");
                    for (int i = parts.length - 1; i >= 0; i--) {
                        try {
                            String jsonStr = parts[i];
                            if (!jsonStr.startsWith("{")) jsonStr = "{" + jsonStr;
                            if (!jsonStr.endsWith("}")) jsonStr = jsonStr + "}";
                            JSONObject json = new JSONObject(jsonStr);
                            String jsonStatus = json.optString("status", "error");
                            String jsonMessage = json.optString("message", "");
                            if (jsonStatus.equals("success") || jsonStatus.equals("pass")) {
                                responseJson = json;
                                finalStatus = jsonStatus;
                                finalMessage = jsonMessage;
                                break;
                            }
                        } catch (Exception ignored) {}
                    }
                } else {
                    try {
                        responseJson = new JSONObject(responseStr);
                        finalStatus = responseJson.optString("status", "error");
                        finalMessage = responseJson.optString("message", "No message");
                    } catch (Exception ignored) {}
                }

                String status = finalStatus;
                String message = finalMessage;

                getActivity().runOnUiThread(() -> {
                    if (status.equals("pass")) {
                        SafeToast.show(getContext(), "Registration successful! Redirecting to login...", Toast.LENGTH_SHORT);
                        dialog.dismiss();

                        if (getActivity() != null) {
                            getActivity().getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.overlayContainer, new BlankFragment())
                                    .commit();
                        }
                    } else {
                        dialog.dismiss();
                        SafeToast.show(getContext(), "Redirecting to login...", Toast.LENGTH_SHORT);
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
                        SafeToast.show(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT));

            }
        }).start();
    }
}
