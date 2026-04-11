package com.example.proyectocarpooling.presentation.profile.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proyectocarpooling.R;
import com.example.proyectocarpooling.data.local.SessionManager;
import com.example.proyectocarpooling.data.local.UserAccessProvider;
import com.example.proyectocarpooling.data.model.user.UpdateUserRequest;
import com.example.proyectocarpooling.domain.usecase.user.UserAccessUseCase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProfileActivity extends AppCompatActivity {

    private EditText nameInput;
    private EditText emailInput;
    private EditText phoneInput;
    private EditText newPasswordInput;
    private Button saveButton;
    private ProgressBar loading;

    private SessionManager sessionManager;
    private UserAccessUseCase userAccessUseCase;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        sessionManager = new SessionManager(this);
        userAccessUseCase = UserAccessProvider.create(this);

        nameInput = findViewById(R.id.profileNameInput);
        emailInput = findViewById(R.id.profileEmailInput);
        phoneInput = findViewById(R.id.profilePhoneInput);
        newPasswordInput = findViewById(R.id.profileNewPasswordInput);
        saveButton = findViewById(R.id.profileSaveButton);
        loading = findViewById(R.id.profileLoading);

        nameInput.setText(sessionManager.getFullName());
        emailInput.setText(sessionManager.getEmail());
        phoneInput.setText(sessionManager.getPhone());

        saveButton.setOnClickListener(v -> saveProfile());
    }

    private void saveProfile() {
        String fullName = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim().toLowerCase();
        String phone = phoneInput.getText().toString().trim();
        String newPassword = newPasswordInput.getText().toString();

        if (!validate(fullName, email, phone, newPassword)) {
            return;
        }

        String userId = sessionManager.getUserId();
        if (userId.isEmpty()) {
            Toast.makeText(this, R.string.profile_missing_session, Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        executor.execute(() -> {
            try {
                var user = userAccessUseCase.update(userId, new UpdateUserRequest(fullName, email, phone, newPassword));
                runOnUiThread(() -> {
                    sessionManager.saveUser(user);
                    Toast.makeText(this, R.string.profile_updated, Toast.LENGTH_SHORT).show();
                    setLoading(false);
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private boolean validate(String fullName, String email, String phone, String newPassword) {
        if (fullName.length() < 3) {
            nameInput.setError(getString(R.string.validation_name_min));
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

        if (!newPassword.isEmpty() && newPassword.length() < 6) {
            newPasswordInput.setError(getString(R.string.validation_password_min));
            return false;
        }

        return true;
    }

    private void setLoading(boolean isLoading) {
        loading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        saveButton.setEnabled(!isLoading);
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
