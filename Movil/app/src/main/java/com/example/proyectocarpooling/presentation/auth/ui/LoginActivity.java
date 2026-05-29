package com.example.proyectocarpooling.presentation.auth.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.proyectocarpooling.presentation.BaseActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.proyectocarpooling.R;
import com.example.proyectocarpooling.data.local.SessionManager;
import com.example.proyectocarpooling.presentation.main.ui.MainActivity;

public class LoginActivity extends BaseActivity {

    private EditText emailInput;
    private EditText passwordInput;
    private ProgressBar loading;
    private Button loginButton;
    private AuthViewModel authViewModel;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sessionManager = new SessionManager(this);

        if (sessionManager.hasActiveSession()) {
            navigateToMain();
            return;
        }

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        emailInput = findViewById(R.id.loginEmailInput);
        passwordInput = findViewById(R.id.loginPasswordInput);
        loading = findViewById(R.id.loginLoading);
        loginButton = findViewById(R.id.loginButton);
        TextView openRegisterText = findViewById(R.id.openRegisterText);

        loginButton.setOnClickListener(v -> performLogin());
        openRegisterText.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));

        observeViewModel();
    }

    private void observeViewModel() {
        authViewModel.getLoading().observe(this, isLoading -> {
            loading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            loginButton.setEnabled(!isLoading);
        });

        authViewModel.getLoginSuccess().observe(this, user -> {
            if (user != null) {
                sessionManager.saveUser(user);
                Toast.makeText(this, R.string.login_success, Toast.LENGTH_SHORT).show();
                navigateToMain();
            }
        });

        authViewModel.getErrorEvent().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void performLogin() {
        String email = emailInput.getText().toString().trim().toLowerCase();
        String password = passwordInput.getText().toString();

        if (!validate(email, password)) {
            return;
        }

        authViewModel.login(email, password);
    }

    private boolean validate(String email, String password) {
        if (email.isEmpty()) {
            emailInput.setError(getString(R.string.validation_required));
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError(getString(R.string.validation_invalid_email));
            return false;
        }

        if (!email.endsWith("@univalle.edu")) {
            emailInput.setError(getString(R.string.validation_univalle_email));
            return false;
        }

        if (password.length() < 6) {
            passwordInput.setError(getString(R.string.validation_password_min));
            return false;
        }

        return true;
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
