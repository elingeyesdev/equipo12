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

        TextView nameValue = findViewById(R.id.accountNameValue);
        TextView emailValue = findViewById(R.id.accountEmailValue);
        Button closeButton = findViewById(R.id.accountCloseButton);

        String fullName = sessionManager.getFullName();
        String email = sessionManager.getEmail();

        if (fullName == null || fullName.trim().isEmpty()) {
            fullName = "Usuario Demo";
        }

        if (email == null || email.trim().isEmpty()) {
            email = "cuenta.demo@univalle.edu";
        }

        nameValue.setText(fullName);
        emailValue.setText(email);

        closeButton.setOnClickListener(v -> finish());
    }
}
