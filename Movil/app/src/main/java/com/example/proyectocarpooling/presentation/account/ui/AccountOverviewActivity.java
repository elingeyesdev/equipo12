package com.example.proyectocarpooling.presentation.account.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proyectocarpooling.R;
import com.example.proyectocarpooling.data.local.SessionManager;

public class AccountOverviewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_overview);

        SessionManager sessionManager = new SessionManager(this);

        // Get all views
        TextView nameValue = findViewById(R.id.accountNameValue);
        TextView emailValue = findViewById(R.id.accountEmailValue);
        TextView userInitials = findViewById(R.id.accountUserInitials);
        TextView userRole = findViewById(R.id.accountRoleValue);
        Button backButton = findViewById(R.id.accountBackButton);
        Button closeButton = findViewById(R.id.accountCloseButton);

        // Get user data
        String fullName = sessionManager.getFullName();
        String email = sessionManager.getEmail();
        boolean isDriver = sessionManager.isDriver();

        // Set defaults if empty
        if (fullName == null || fullName.trim().isEmpty()) {
            fullName = "Usuario Demo";
        }

        if (email == null || email.trim().isEmpty()) {
            email = "usuario@univalle.edu";
        }

        // Load user information
        nameValue.setText(fullName);
        emailValue.setText(email);
        
        // Generate and display initials
        String initials = generateInitials(fullName);
        userInitials.setText(initials);
        
        // Display user role
        String roleText = isDriver ? getString(R.string.user_role_driver) : getString(R.string.user_role_passenger);
        userRole.setText(roleText);

        // Set up button listeners
        backButton.setOnClickListener(v -> finish());
        closeButton.setOnClickListener(v -> finish());
    }

    private String generateInitials(String fullName) {
        if (fullName == null || fullName.isEmpty()) {
            return "UI";
        }
        String[] parts = fullName.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();
        for (int i = 0; i < Math.min(2, parts.length); i++) {
            if (parts[i].length() > 0) {
                initials.append(parts[i].charAt(0));
            }
        }
        return initials.length() > 0 ? initials.toString().toUpperCase() : "UI";
    }
}
