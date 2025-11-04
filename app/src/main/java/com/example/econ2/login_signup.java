package com.example.econ2;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class login_signup extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_signup); // Host layout

        // References to the top TextViews
        TextView signUpLink = findViewById(R.id.signUpLink);
        TextView dontHaveAccount = findViewById(R.id.tst);

        // Load the initial fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.overlayContainer, new BlankFragment())
                .commit();

        // Click listener for Sign Up link
        signUpLink.setOnClickListener(v -> {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.overlayContainer, new sign_in_page())
                    .addToBackStack(null) // allows back navigation
                    .commit();

            // Hide the top TextViews while fragment is visible
            dontHaveAccount.setVisibility(TextView.GONE);
            signUpLink.setVisibility(TextView.GONE);
        });

        // Listener to restore TextViews when back button is pressed
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                // Back to initial state
                dontHaveAccount.setVisibility(TextView.VISIBLE);
                signUpLink.setVisibility(TextView.VISIBLE);
            }
        });
    }
}
