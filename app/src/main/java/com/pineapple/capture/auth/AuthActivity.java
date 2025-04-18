package com.pineapple.capture.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.pineapple.capture.R;
import com.pineapple.capture.feed.MainFeedActivity;

public class AuthActivity extends AppCompatActivity {
    private AuthViewModel authViewModel;
    private TextInputEditText usernameInput;
    private TextInputEditText passwordInput;
    private MaterialButton loginButton;
    private MaterialButton signupButton;
    private TextView errorText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);
        
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        
        // Initialize views
        usernameInput = findViewById(R.id.username_input);
        passwordInput = findViewById(R.id.password_input);
        loginButton = findViewById(R.id.login_button);
        signupButton = findViewById(R.id.signup_button);
        errorText = findViewById(R.id.error_text);
        
        // Set up click listeners
        loginButton.setOnClickListener(v -> handleLogin());
        signupButton.setOnClickListener(v -> handleSignup());
        
        // Observe authentication state changes
        authViewModel.getAuthState().observe(this, isAuthenticated -> {
            if (isAuthenticated) {
                startActivity(new Intent(this, MainFeedActivity.class));
                finish();
            }
        });
        
        // Observe error messages
        authViewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                errorText.setText(error);
                errorText.setVisibility(View.VISIBLE);
            } else {
                errorText.setVisibility(View.GONE);
            }
        });
    }
    
    private void handleLogin() {
        String username = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        
        if (validateInput(username, password)) {
            authViewModel.signIn(username + "@pineapple.com", password);
        }
    }
    
    private void handleSignup() {
        String username = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        
        if (validateInput(username, password)) {
            authViewModel.signUp(username + "@pineapple.com", password);
        }
    }
    
    private boolean validateInput(String username, String password) {
        if (username.isEmpty()) {
            errorText.setText("Username cannot be empty");
            errorText.setVisibility(View.VISIBLE);
            return false;
        }
        
        if (password.isEmpty()) {
            errorText.setText("Password cannot be empty");
            errorText.setVisibility(View.VISIBLE);
            return false;
        }
        
        if (password.length() < 6) {
            errorText.setText("Password must be at least 6 characters");
            errorText.setVisibility(View.VISIBLE);
            return false;
        }
        
        errorText.setVisibility(View.GONE);
        return true;
    }
} 