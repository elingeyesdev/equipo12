package com.example.proyectocarpooling.presentation.auth.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proyectocarpooling.R;
import com.example.proyectocarpooling.data.local.SessionManager;
import com.example.proyectocarpooling.data.local.UserAccessProvider;
import com.example.proyectocarpooling.data.model.user.RegisterUserRequest;
import com.example.proyectocarpooling.domain.usecase.user.UserAccessUseCase;
import com.example.proyectocarpooling.presentation.main.ui.MainActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RegisterActivity extends AppCompatActivity {

    private EditText nameInput;
    private EditText emailInput;
    private EditText phoneInput;
    private EditText passwordInput;
    private EditText confirmPasswordInput;
    private Button registerButton;
    private ProgressBar loading;

    private UserAccessUseCase userAccessUseCase;
    private SessionManager sessionManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        userAccessUseCase = UserAccessProvider.create(this);
        sessionManager = new SessionManager(this);

        nameInput = findViewById(R.id.registerNameInput);
        emailInput = findViewById(R.id.registerEmailInput);
        phoneInput = findViewById(R.id.registerPhoneInput);
        passwordInput = findViewById(R.id.registerPasswordInput);
        confirmPasswordInput = findViewById(R.id.registerConfirmPasswordInput);
        registerButton = findViewById(R.id.registerButton);
        loading = findViewById(R.id.registerLoading);
        TextView backToLogin = findViewById(R.id.backToLoginText);

        registerButton.setOnClickListener(v -> performRegister());
        backToLogin.setOnClickListener(v -> finish());
    }

    private void performRegister() {
        String fullName = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim().toLowerCase();
        String phone = phoneInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        String confirmPassword = confirmPasswordInput.getText().toString();

        if (!validate(fullName, email, phone, password, confirmPassword)) {
            return;
        }

        setLoading(true);
        executor.execute(() -> {
            try {
                var user = userAccessUseCase.register(new RegisterUserRequest(fullName, email, password, phone));
                runOnUiThread(() -> {
                    sessionManager.saveUser(user);
                    Toast.makeText(this, R.string.register_success, Toast.LENGTH_SHORT).show();
                    navigateToMain();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private boolean validate(String fullName, String email, String phone, String password, String confirmPassword) {
        if (fullName.length() < 3) {
            nameInput.setError(getString(R.string.validation_name_min));
            return false;
        }

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

        if (!phone.isEmpty() && phone.length() < 7) {
            phoneInput.setError(getString(R.string.validation_phone_min));
            return false;
        }

        if (password.length() < 6) {
            passwordInput.setError(getString(R.string.validation_password_min));
            return false;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordInput.setError(getString(R.string.validation_password_match));
            return false;
        }

        return true;
    }

    private void setLoading(boolean isLoading) {
        loading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        registerButton.setEnabled(!isLoading);
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
