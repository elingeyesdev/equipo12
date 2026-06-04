package com.example.proyectocarpooling.presentation.auth.ui;

import android.content.Intent;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.graphics.Bitmap;
import android.widget.ImageView;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.proyectocarpooling.presentation.BaseActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.proyectocarpooling.R;
import com.example.proyectocarpooling.data.local.SessionManager;
import com.example.proyectocarpooling.data.model.user.DriverProfileRequest;
import com.example.proyectocarpooling.data.model.user.RegisterUserRequest;
import com.example.proyectocarpooling.presentation.main.ui.MainActivity;

public class RegisterActivity extends BaseActivity {

    private EditText nameInput;
    private EditText emailInput;
    private EditText phoneInput;
    private CheckBox hasVehicleCheckbox;
    private View vehicleFieldsContainer;
    private android.widget.Spinner seatsInput;
    private EditText plateInput;
    private EditText brandInput;
    private EditText colorInput;
    private EditText passwordInput;
    private EditText confirmPasswordInput;
    private Button registerButton;
    private ProgressBar loading;

    private AuthViewModel authViewModel;
    private SessionManager sessionManager;

    private String base64Image = "";
    private boolean isRegisterPendingAfterPhoto = false;

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    if (extras != null) {
                        Bitmap bitmap = (Bitmap) extras.get("data");
                        processAndSetImage(bitmap);
                    }
                }
            }
    );

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    android.net.Uri selectedImage = result.getData().getData();
                    if (selectedImage != null) {
                        processGalleryUri(selectedImage);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        sessionManager = new SessionManager(this);
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        nameInput = findViewById(R.id.registerNameInput);
        emailInput = findViewById(R.id.registerEmailInput);
        phoneInput = findViewById(R.id.registerPhoneInput);
        hasVehicleCheckbox = findViewById(R.id.registerHasVehicleCheckbox);
        vehicleFieldsContainer = findViewById(R.id.registerVehicleFieldsContainer);
        seatsInput = findViewById(R.id.registerSeatsInput);
        android.widget.ArrayAdapter<String> seatsAdapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"1", "2", "3", "4", "5"});
        seatsInput.setAdapter(seatsAdapter);
        seatsInput.setSelection(3); // default to 4 seats
        plateInput = findViewById(R.id.registerPlateInput);
        brandInput = findViewById(R.id.registerBrandInput);
        colorInput = findViewById(R.id.registerColorInput);
        passwordInput = findViewById(R.id.registerPasswordInput);
        confirmPasswordInput = findViewById(R.id.registerConfirmPasswordInput);

        setupErrorClearer(nameInput);
        setupErrorClearer(emailInput);
        setupErrorClearer(phoneInput);
        setupErrorClearer(plateInput);
        setupErrorClearer(brandInput);
        setupErrorClearer(colorInput);
        setupErrorClearer(passwordInput);
        setupErrorClearer(confirmPasswordInput);
        registerButton = findViewById(R.id.registerButton);
        loading = findViewById(R.id.registerLoading);
        TextView backToLogin = findViewById(R.id.backToLoginText);

        View profilePictureContainer = findViewById(R.id.registerProfilePictureContainer);
        if (profilePictureContainer != null) {
            profilePictureContainer.setOnClickListener(v -> showImagePickerDialog());
        }

        hasVehicleCheckbox.setOnCheckedChangeListener((buttonView, isChecked) ->
            vehicleFieldsContainer.setVisibility(isChecked ? View.VISIBLE : View.GONE));

        registerButton.setOnClickListener(v -> performRegister());
        backToLogin.setOnClickListener(v -> finish());

        observeViewModel();
    }

    private void observeViewModel() {
        authViewModel.getLoading().observe(this, isLoading -> {
            loading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            registerButton.setEnabled(!isLoading);
        });

        authViewModel.getLoginSuccess().observe(this, user -> {
            if (user != null) {
                sessionManager.saveUser(user);
                Toast.makeText(this, R.string.register_success, Toast.LENGTH_SHORT).show();
                navigateToMain();
            }
        });

        authViewModel.getErrorEvent().observe(this, error -> {
            if (error != null) {
                if (error.toLowerCase().contains("email") || error.toLowerCase().contains("correo")) {
                    setErrorState(emailInput, true, error);
                } else {
                    new androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("Error de registro")
                            .setMessage(error)
                            .setPositiveButton("Aceptar", null)
                            .show();
                }
            }
        });
    }

    private void performRegister() {
        String fullName = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim().toLowerCase();
        String phone = phoneInput.getText().toString().trim();
        boolean hasVehicle = hasVehicleCheckbox.isChecked();
        String seatsRaw = seatsInput.getSelectedItem().toString();
        String plate = plateInput.getText().toString().trim();
        String brand = brandInput.getText().toString().trim();
        String color = colorInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        String confirmPassword = confirmPasswordInput.getText().toString();

        if (!validate(fullName, email, phone, password, confirmPassword, hasVehicle, seatsRaw, plate, brand, color)) {
            return;
        }

        int seats = hasVehicle ? Integer.parseInt(seatsRaw) : 0;
        String role = hasVehicle ? "driver" : "student";
        DriverProfileRequest driverProfile = hasVehicle
                ? new DriverProfileRequest(seats, plate.toUpperCase(), brand, color)
                : null;

        authViewModel.register(new RegisterUserRequest(fullName, email, password, phone, role, base64Image, driverProfile));
    }

    private boolean validate(
            String fullName,
            String email,
            String phone,
            String password,
            String confirmPassword,
            boolean hasVehicle,
            String seatsRaw,
            String plate,
            String brand,
            String color) {
        if (fullName.length() < 3) {
            setErrorState(nameInput, true, getString(R.string.validation_name_min));
            return false;
        }

        if (email.isEmpty()) {
            setErrorState(emailInput, true, getString(R.string.validation_required));
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            setErrorState(emailInput, true, getString(R.string.validation_invalid_email));
            return false;
        }

        if (!email.endsWith("@univalle.edu")) {
            setErrorState(emailInput, true, getString(R.string.validation_univalle_email));
            return false;
        }

        if (!phone.isEmpty() && phone.length() < 7) {
            setErrorState(phoneInput, true, getString(R.string.validation_phone_min));
            return false;
        }

        if (password.length() < 6) {
            setErrorState(passwordInput, true, getString(R.string.validation_password_min));
            return false;
        }

        if (!password.equals(confirmPassword)) {
            setErrorState(confirmPasswordInput, true, getString(R.string.validation_password_match));
            return false;
        }

        if (hasVehicle) {
            int seats = Integer.parseInt(seatsRaw);

            if (plate.length() < 5) {
                setErrorState(plateInput, true, getString(R.string.validation_plate_min));
                return false;
            }

            if (brand.length() < 2) {
                setErrorState(brandInput, true, getString(R.string.validation_brand_min));
                return false;
            }

            if (color.length() < 2) {
                setErrorState(colorInput, true, getString(R.string.validation_color_min));
                return false;
            }
        }

        if (base64Image == null || base64Image.trim().isEmpty()) {
            isRegisterPendingAfterPhoto = true;
            showRegistrationImagePickerDialog();
            return false;
        }

        return true;
    }

    private void showImagePickerDialog() {
        CharSequence[] options = {"Tomar foto", "Seleccionar de la galería", "Cancelar"};
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Foto de perfil");
        builder.setItems(options, (dialog, item) -> {
            if (options[item].equals("Tomar foto")) {
                Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                cameraLauncher.launch(intent);
            } else if (options[item].equals("Seleccionar de la galería")) {
                Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                galleryLauncher.launch(intent);
            } else {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    private void processAndSetImage(Bitmap bitmap) {
        if (bitmap == null) return;
        
        int maxDimension = 300;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width > maxDimension || height > maxDimension) {
            float ratio = (float) width / (float) height;
            if (ratio > 1) {
                width = maxDimension;
                height = (int) (maxDimension / ratio);
            } else {
                height = maxDimension;
                width = (int) (maxDimension * ratio);
            }
            bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
        }

        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        byte[] imageBytes = baos.toByteArray();
        base64Image = android.util.Base64.encodeToString(imageBytes, android.util.Base64.DEFAULT);

        ImageView registerProfilePicture = findViewById(R.id.registerProfilePicture);
        View registerProfilePicturePlaceholder = findViewById(R.id.registerProfilePicturePlaceholder);
        if (registerProfilePicture != null) {
            registerProfilePicture.setImageBitmap(bitmap);
            registerProfilePicture.setVisibility(View.VISIBLE);
        }
        if (registerProfilePicturePlaceholder != null) {
            registerProfilePicturePlaceholder.setVisibility(View.GONE);
        }

        if (isRegisterPendingAfterPhoto) {
            isRegisterPendingAfterPhoto = false;
            performRegister();
        }
    }

    private void processGalleryUri(android.net.Uri uri) {
        try {
            Bitmap bitmap = android.provider.MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            processAndSetImage(bitmap);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al cargar imagen", Toast.LENGTH_SHORT).show();
        }
    }

    private void showRegistrationImagePickerDialog() {
        CharSequence[] options = {"Tomar foto", "Seleccionar de la galería", "Cancelar"};
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Sube tu foto de perfil");
        builder.setMessage("Para completar tu registro, por favor tómate una foto o selecciona una de tu cara.");
        builder.setItems(options, (dialog, item) -> {
            if (options[item].equals("Tomar foto")) {
                Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                cameraLauncher.launch(intent);
            } else if (options[item].equals("Seleccionar de la galería")) {
                Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                galleryLauncher.launch(intent);
            } else {
                isRegisterPendingAfterPhoto = false;
                dialog.dismiss();
            }
        });
        builder.show();
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
