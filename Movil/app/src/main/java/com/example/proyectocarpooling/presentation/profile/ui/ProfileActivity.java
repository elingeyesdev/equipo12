package com.example.proyectocarpooling.presentation.profile.ui;

import com.example.proyectocarpooling.presentation.BaseActivity;
import android.os.Bundle;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import android.widget.ImageView;
import android.graphics.Bitmap;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.proyectocarpooling.CarPoolingApplication;
import com.example.proyectocarpooling.R;
import com.example.proyectocarpooling.data.local.SessionManager;
import com.example.proyectocarpooling.data.model.user.DriverProfileRequest;
import com.example.proyectocarpooling.data.model.user.UpdateUserRequest;
import com.example.proyectocarpooling.data.model.user.VehicleResponse;

import java.util.ArrayList;
import java.util.List;

public class ProfileActivity extends BaseActivity {

    private EditText nameInput, emailInput, phoneInput, seatsInput, plateInput, brandInput, colorInput, newPasswordInput;
    private CheckBox hasVehicleCheckbox;
    private LinearLayout vehicleFieldsContainer;
    private Button saveButton, manageVehiclesButton, saveVehicleButton;
    private ProgressBar loading;

    private SessionManager sessionManager;
    private ProfileViewModel viewModel;
    private String originalRole = "student";
    private String editingVehicleId = null;
    private ViewGroup vehicleFieldsParent;
    private View profilePictureContainer;
    private ImageView profilePicture;
    private View profilePicturePlaceholder;
    private TextView profilePictureInitials;
    private String newBase64Image = "";

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
        setContentView(R.layout.activity_profile);

        sessionManager = ((CarPoolingApplication) getApplication()).getSessionManager();
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        bindViews();
        populateInitialFields();
        setupVehicleButtons();
        createVehicleFieldButtons();

        hasVehicleCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            vehicleFieldsContainer.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            manageVehiclesButton.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        loadCurrentProfile();
        saveButton.setOnClickListener(v -> saveProfile());
        if (profilePictureContainer != null) {
            profilePictureContainer.setOnClickListener(v -> showImagePickerDialog());
        }
        observeViewModel();
    }

    private void bindViews() {
        nameInput = findViewById(R.id.profileNameInput);
        emailInput = findViewById(R.id.profileEmailInput);
        phoneInput = findViewById(R.id.profilePhoneInput);
        hasVehicleCheckbox = findViewById(R.id.profileHasVehicleCheckbox);
        vehicleFieldsContainer = findViewById(R.id.profileVehicleFieldsContainer);
        seatsInput = findViewById(R.id.profileSeatsInput);
        plateInput = findViewById(R.id.profilePlateInput);
        brandInput = findViewById(R.id.profileBrandInput);
        colorInput = findViewById(R.id.profileColorInput);
        newPasswordInput = findViewById(R.id.profileNewPasswordInput);
        saveButton = findViewById(R.id.profileSaveButton);
        loading = findViewById(R.id.profileLoading);

        profilePictureContainer = findViewById(R.id.profilePictureContainer);
        profilePicture = findViewById(R.id.profilePicture);
        profilePicturePlaceholder = findViewById(R.id.profilePicturePlaceholder);
        profilePictureInitials = findViewById(R.id.profilePictureInitials);
    }

    private void populateInitialFields() {
        nameInput.setText(sessionManager.getFullName());
        emailInput.setText(sessionManager.getEmail());
        phoneInput.setText(sessionManager.getPhone());
        hasVehicleCheckbox.setChecked(sessionManager.isDriver());
        originalRole = sessionManager.getRole();
        vehicleFieldsContainer.setVisibility(hasVehicleCheckbox.isChecked() ? View.VISIBLE : View.GONE);

        newBase64Image = sessionManager.getProfilePicture();
        loadBase64Image(newBase64Image, profilePicture, profilePicturePlaceholder);
        if (profilePictureInitials != null) {
            profilePictureInitials.setText(generateInitials(sessionManager.getFullName()));
        }
    }

    private void setupVehicleButtons() {
        vehicleFieldsParent = (ViewGroup) vehicleFieldsContainer.getParent();

        saveVehicleButton = new Button(this);
        saveVehicleButton.setText("Guardar vehículo");
        saveVehicleButton.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        int topMargin = (int) (12 * getResources().getDisplayMetrics().density);
        ((LinearLayout.LayoutParams) saveVehicleButton.getLayoutParams()).topMargin = topMargin;
        saveVehicleButton.setBackgroundResource(R.drawable.bg_primary_action);
        saveVehicleButton.setBackgroundTintList(null);
        saveVehicleButton.setTextColor(getResources().getColor(R.color.uber_text_inverse, null));
        saveVehicleButton.setVisibility(View.GONE);
        saveVehicleButton.setOnClickListener(v -> saveVehicle());
        vehicleFieldsContainer.addView(saveVehicleButton);

        manageVehiclesButton = new Button(this);
        manageVehiclesButton.setText("Mis vehículos");
        manageVehiclesButton.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        ((LinearLayout.LayoutParams) manageVehiclesButton.getLayoutParams()).topMargin = topMargin;
        manageVehiclesButton.setBackgroundResource(R.drawable.bg_info_field);
        manageVehiclesButton.setTextColor(getResources().getColor(R.color.carpool_text_primary, null));
        manageVehiclesButton.setVisibility(hasVehicleCheckbox.isChecked() ? View.VISIBLE : View.GONE);
        manageVehiclesButton.setOnClickListener(v -> viewModel.loadVehicles(sessionManager.getUserId()));

        int containerIndex = vehicleFieldsParent.indexOfChild(vehicleFieldsContainer);
        vehicleFieldsParent.addView(manageVehiclesButton, containerIndex + 1);
    }

    private void createVehicleFieldButtons() {
        // Buttons already created in setupVehicleButtons
    }

    private void observeViewModel() {
        viewModel.getLoading().observe(this, isLoading -> {
            loading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            saveButton.setEnabled(!isLoading);
        });

        viewModel.getUserProfile().observe(this, user -> {
            if (user != null) {
                hasVehicleCheckbox.setChecked("driver".equalsIgnoreCase(user.role));
                originalRole = user.role;
                if (user.driverProfile != null && user.vehicles != null && !user.vehicles.isEmpty()) {
                    VehicleResponse v = user.vehicles.get(0);
                    seatsInput.setText(String.valueOf(v.totalSeats));
                    plateInput.setText(v.licensePlate);
                    brandInput.setText(v.brand);
                    colorInput.setText(v.color);
                }
            }
        });

        viewModel.getVehicleList().observe(this, vehicles -> {
            if (vehicles != null) {
                buildVehicleDialog(vehicles);
            }
        });

        viewModel.getVehicleSaved().observe(this, v -> {
            editingVehicleId = null;
            saveVehicleButton.setVisibility(View.GONE);
            Toast.makeText(this, "Vehículo guardado", Toast.LENGTH_SHORT).show();
        });

        viewModel.getProfileUpdated().observe(this, user -> {
            if (user != null) {
                sessionManager.saveUser(user);
                Toast.makeText(this, R.string.profile_updated, Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        viewModel.getErrorEvent().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.getSuccessEvent().observe(this, msg -> {
            if (msg != null) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                viewModel.loadVehicles(sessionManager.getUserId());
            }
        });
    }

    private void loadCurrentProfile() {
        String userId = sessionManager.getUserId();
        if (!userId.isEmpty()) {
            viewModel.loadProfile(userId);
        }
    }

    private void buildVehicleDialog(List<VehicleResponse> vehicles) {
        List<VehicleResponse> vehicleList = new ArrayList<>(vehicles);
        String userId = sessionManager.getUserId();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Mis vehículos");

        if (vehicleList.isEmpty()) {
            builder.setMessage("No tienes vehículos registrados.");
        } else {
            LinearLayout container = new LinearLayout(this);
            container.setOrientation(LinearLayout.VERTICAL);
            int pad = (int) (12 * getResources().getDisplayMetrics().density);

            for (VehicleResponse v : vehicleList) {
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(0, pad / 2, 0, pad / 2);
                row.setGravity(android.view.Gravity.CENTER_VERTICAL);

                TextView info = new TextView(this);
                info.setText(v.licensePlate + " – " + v.brand + " – " + v.color + " (" + v.totalSeats + " cupos)");
                info.setTextSize(13);
                info.setTextColor(getResources().getColor(R.color.carpool_text_primary, null));
                LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                info.setLayoutParams(ip);
                row.addView(info);

                Button editBtn = new Button(this);
                editBtn.setText("Editar");
                editBtn.setTextSize(12);
                editBtn.setTextColor(getResources().getColor(R.color.carpool_text_primary, null));
                editBtn.setBackgroundResource(R.drawable.bg_info_field);
                int smallPad = (int) (6 * getResources().getDisplayMetrics().density);
                editBtn.setPadding(smallPad * 2, smallPad, smallPad * 2, smallPad);
                editBtn.setOnClickListener(ev -> {
                    seatsInput.setText(String.valueOf(v.totalSeats));
                    plateInput.setText(v.licensePlate);
                    brandInput.setText(v.brand);
                    colorInput.setText(v.color);
                    editingVehicleId = v.id;
                    saveVehicleButton.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "Vehículo cargado. Edita y presiona \"Guardar vehículo\".", Toast.LENGTH_SHORT).show();
                });
                row.addView(editBtn);

                Button delBtn = new Button(this);
                delBtn.setText("Eliminar");
                delBtn.setTextSize(12);
                delBtn.setTextColor(getResources().getColor(R.color.uber_error, null));
                delBtn.setBackgroundResource(R.drawable.bg_info_field);
                delBtn.setPadding(smallPad * 2, smallPad, smallPad * 2, smallPad);
                delBtn.setOnClickListener(ev -> {
                    new AlertDialog.Builder(this)
                            .setTitle("Eliminar vehículo")
                            .setMessage("¿Eliminar " + v.licensePlate + "?")
                            .setPositiveButton("Eliminar", (d, w) -> viewModel.deleteVehicle(userId, v.id))
                            .setNegativeButton("Cancelar", null)
                            .show();
                });
                row.addView(delBtn);
                container.addView(row);
            }
            builder.setView(container);
        }

        builder.setPositiveButton("Agregar nuevo", (d, w) -> {
            seatsInput.setText("");
            plateInput.setText("");
            brandInput.setText("");
            colorInput.setText("");
            editingVehicleId = "";
            saveVehicleButton.setVisibility(View.VISIBLE);
            Toast.makeText(this, "Llena los campos y presiona \"Guardar vehículo\".", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Cerrar", null);
        builder.show();
    }

    private void saveVehicle() {
        String userId = sessionManager.getUserId();
        if (userId.isEmpty()) {
            Toast.makeText(this, R.string.profile_missing_session, Toast.LENGTH_SHORT).show();
            return;
        }

        String plate = plateInput.getText().toString().trim().toUpperCase();
        String brand = brandInput.getText().toString().trim();
        String color = colorInput.getText().toString().trim();
        String seatsRaw = seatsInput.getText().toString().trim();

        if (plate.length() < 5) { plateInput.setError(getString(R.string.validation_plate_min)); return; }
        if (brand.length() < 2) { brandInput.setError(getString(R.string.validation_brand_min)); return; }
        if (color.length() < 2) { colorInput.setError(getString(R.string.validation_color_min)); return; }
        int seats;
        try { seats = Integer.parseInt(seatsRaw); } catch (NumberFormatException e) {
            seatsInput.setError(getString(R.string.validation_seats_number)); return;
        }
        if (seats < 1 || seats > 12) { seatsInput.setError(getString(R.string.validation_seats_range)); return; }

        viewModel.saveVehicle(userId, editingVehicleId, plate, brand, color, seats);
    }

    private void saveProfile() {
        String fullName = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim().toLowerCase();
        String phone = phoneInput.getText().toString().trim();
        boolean hasVehicle = hasVehicleCheckbox.isChecked();
        String seatsRaw = seatsInput.getText().toString().trim();
        String plate = plateInput.getText().toString().trim();
        String brand = brandInput.getText().toString().trim();
        String color = colorInput.getText().toString().trim();
        String newPassword = newPasswordInput.getText().toString();

        if (!validate(fullName, email, phone, newPassword, hasVehicle, seatsRaw, plate, brand, color)) {
            return;
        }

        String role = hasVehicle ? "driver" : "student";
        boolean roleChangeRequested = !role.equalsIgnoreCase(originalRole);
        DriverProfileRequest driverProfile = hasVehicle
                ? new DriverProfileRequest(Integer.parseInt(seatsRaw), plate.toUpperCase(), brand, color)
                : null;

        String userId = sessionManager.getUserId();
        if (userId.isEmpty()) {
            Toast.makeText(this, R.string.profile_missing_session, Toast.LENGTH_SHORT).show();
            return;
        }

        viewModel.updateProfile(userId, new UpdateUserRequest(fullName, email, phone, newPassword, role, roleChangeRequested, newBase64Image, driverProfile));
    }

    private boolean validate(String fullName, String email, String phone, String newPassword,
                              boolean hasVehicle, String seatsRaw, String plate, String brand, String color) {
        if (fullName.length() < 3) { nameInput.setError(getString(R.string.validation_name_min)); return false; }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError(getString(R.string.validation_invalid_email)); return false;
        }
        if (!email.endsWith("@univalle.edu")) {
            emailInput.setError(getString(R.string.validation_univalle_email)); return false;
        }
        if (!phone.isEmpty() && phone.length() < 7) {
            phoneInput.setError(getString(R.string.validation_phone_min)); return false;
        }
        if (!newPassword.isEmpty() && newPassword.length() < 6) {
            newPasswordInput.setError(getString(R.string.validation_password_min)); return false;
        }
        if (hasVehicle) {
            if (seatsRaw.isEmpty()) { seatsInput.setError(getString(R.string.validation_required)); return false; }
            int seats;
            try { seats = Integer.parseInt(seatsRaw); } catch (NumberFormatException e) {
                seatsInput.setError(getString(R.string.validation_seats_number)); return false;
            }
            if (seats < 1 || seats > 12) { seatsInput.setError(getString(R.string.validation_seats_range)); return false; }
            if (plate.length() < 5) { plateInput.setError(getString(R.string.validation_plate_min)); return false; }
            if (brand.length() < 2) { brandInput.setError(getString(R.string.validation_brand_min)); return false; }
            if (color.length() < 2) { colorInput.setError(getString(R.string.validation_color_min)); return false; }
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
        newBase64Image = android.util.Base64.encodeToString(imageBytes, android.util.Base64.DEFAULT);

        if (profilePicture != null) {
            profilePicture.setImageBitmap(bitmap);
            profilePicture.setVisibility(View.VISIBLE);
        }
        if (profilePicturePlaceholder != null) {
            profilePicturePlaceholder.setVisibility(View.GONE);
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

    private static String generateInitials(String fullName) {
        if (fullName == null || fullName.isEmpty()) return "U";
        String[] parts = fullName.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();
        for (int i = 0; i < Math.min(2, parts.length); i++) {
            if (parts[i].length() > 0) initials.append(parts[i].charAt(0));
        }
        return initials.length() > 0 ? initials.toString().toUpperCase() : "U";
    }
}
