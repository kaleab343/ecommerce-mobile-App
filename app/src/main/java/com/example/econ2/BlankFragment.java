package com.example.econ2;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class BlankFragment extends Fragment {

    private EditText emailInput, passwordInput;
    private Button loginButton;
    private ImageView passwordToggle;
    private boolean isPasswordVisible = false;

    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "user_prefs";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_LOGGED_IN = "logged_in";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_blank, container, false);

        emailInput = view.findViewById(R.id.emailInput);
        passwordInput = view.findViewById(R.id.passwordInput);
        loginButton = view.findViewById(R.id.loginButton);
        passwordToggle = view.findViewById(R.id.passwordToggle);

        sharedPreferences = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // Check if already logged in
        if (sharedPreferences.getBoolean(KEY_LOGGED_IN, false)) {
            startActivity(new Intent(getActivity(), MainActivity.class));
            getActivity().finish();
        }

        // Toggle password visibility
        passwordToggle.setOnClickListener(v -> {
            if (isPasswordVisible) {
                passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                passwordToggle.setImageResource(R.drawable.ic_eye_closed);
            } else {
                passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                passwordToggle.setImageResource(R.drawable.ic_eye_open);
            }
            isPasswordVisible = !isPasswordVisible;
            passwordInput.setSelection(passwordInput.getText().length());
        });

        // Login button
        loginButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                emailInput.setError("Email is required");
                return;
            }
            if (TextUtils.isEmpty(password)) {
                passwordInput.setError("Password is required");
                return;
            }

            sendLoginRequest(email, password);
        });

        return view;
    }

    private void sendLoginRequest(String email, String password) {
        new Thread(() -> {
            try {
                URL url = new URL(ApiConfig.LOGIN);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);

                JSONObject jsonParam = new JSONObject();
                jsonParam.put("email", email);
                jsonParam.put("password", password);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonParam.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line.trim());
                }
                br.close();
                conn.disconnect();

                JSONObject responseJson = new JSONObject(response.toString());
                String status = responseJson.optString("status", "error");
                String message = responseJson.optString("message", "Something went wrong");
                String username = responseJson.optString("username", "User");

                getActivity().runOnUiThread(() -> {
                    SafeToast.show(getContext(), message, Toast.LENGTH_SHORT);
                    if (status.equals("success")) {
                        sharedPreferences.edit()
                                .putBoolean(KEY_LOGGED_IN, true)
                                .putString(KEY_EMAIL, email)
                                .putString(KEY_PASSWORD, password)
                                .putString(KEY_USERNAME, username)
                                .apply();

                        startActivity(new Intent(getActivity(), MainActivity.class));
                        getActivity().finish();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                getActivity().runOnUiThread(() ->
                        SafeToast.show(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG));
            }
        }).start();
    }
}
