package com.example.proyectocarpooling.presentation.schedules.ui;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;

import com.example.proyectocarpooling.R;
import com.example.proyectocarpooling.data.local.SessionManager;
import com.example.proyectocarpooling.data.model.user.VehicleResponse;
import com.example.proyectocarpooling.presentation.BaseActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CreateTripScheduleActivity extends BaseActivity {

    private SessionManager sessionManager;
    private CreateTripScheduleViewModel viewModel;

    private MaterialToolbar toolbar;
    private TextInputLayout tilOriginAddress;
    private TextInputLayout tilDestinationAddress;
    private TextInputEditText etOriginAddress;
    private TextInputEditText etDestinationAddress;
    private CheckBox cbMon, cbTue, cbWed, cbThu, cbFri, cbSat, cbSun;
    private MaterialButton btnSelectTime;
    private TextView tvSelectedTime;
    private TextInputEditText etOfferedSeats;
    private TextInputEditText etFareAmount;
    private MaterialButton btnSelectVehicle;
    private TextView tvSelectedVehicle;
    private MaterialButton btnSaveSchedule;

    private String selectedTime = "";
    private String selectedVehicleId = "";
    private List<VehicleResponse> userVehicles = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_trip_schedule);

        sessionManager = new SessionManager(this);
        viewModel = new ViewModelProvider(this).get(CreateTripScheduleViewModel.class);

        bindViews();
        setupToolbar();
        setupMapPickers();
        setupTimePicker();
        setupVehiclePicker();
        setupSaveButton();
        observeViewModel();

        // Retrieve and pre-fill origin/destination from intent if present
        if (getIntent() != null) {
            String extraOrigin = getIntent().getStringExtra("EXTRA_ORIGIN");
            String extraDestination = getIntent().getStringExtra("EXTRA_DESTINATION");
            if (extraOrigin != null && !extraOrigin.isEmpty()) {
                etOriginAddress.setText(extraOrigin);
            }
            if (extraDestination != null && !extraDestination.isEmpty()) {
                etDestinationAddress.setText(extraDestination);
            }
        }

        // Load vehicles
        viewModel.loadVehicles(sessionManager.getUserId());
    }

    private void bindViews() {
        toolbar = findViewById(R.id.createScheduleToolbar);
        tilOriginAddress = findViewById(R.id.tilOriginAddress);
        tilDestinationAddress = findViewById(R.id.tilDestinationAddress);
        etOriginAddress = findViewById(R.id.etOriginAddress);
        etDestinationAddress = findViewById(R.id.etDestinationAddress);
        cbMon = findViewById(R.id.cbMon);
        cbTue = findViewById(R.id.cbTue);
        cbWed = findViewById(R.id.cbWed);
        cbThu = findViewById(R.id.cbThu);
        cbFri = findViewById(R.id.cbFri);
        cbSat = findViewById(R.id.cbSat);
        cbSun = findViewById(R.id.cbSun);
        btnSelectTime = findViewById(R.id.btnSelectTime);
        tvSelectedTime = findViewById(R.id.tvSelectedTime);
        etOfferedSeats = findViewById(R.id.etOfferedSeats);
        etFareAmount = findViewById(R.id.etFareAmount);
        btnSelectVehicle = findViewById(R.id.btnSelectVehicle);
        tvSelectedVehicle = findViewById(R.id.tvSelectedVehicle);
        btnSaveSchedule = findViewById(R.id.btnSaveSchedule);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupTimePicker() {
        btnSelectTime.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);

            TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                    (view, hourOfDay, selectedMinute) -> {
                        selectedTime = String.format(Locale.US, "%02d:%02d:00", hourOfDay, selectedMinute);
                        tvSelectedTime.setText(String.format(Locale.US, "%02d:%02d", hourOfDay, selectedMinute));
                    }, hour, minute, true);
            timePickerDialog.show();
        });
    }

    private void setupVehiclePicker() {
        btnSelectVehicle.setOnClickListener(v -> {
            if (userVehicles.isEmpty()) {
                Toast.makeText(this, "Cargando vehículos...", Toast.LENGTH_SHORT).show();
                viewModel.loadVehicles(sessionManager.getUserId());
                return;
            }

            String[] items = new String[userVehicles.size()];
            for (int i = 0; i < userVehicles.size(); i++) {
                VehicleResponse vehicle = userVehicles.get(i);
                items[i] = vehicle.brand + " " + vehicle.model + " (" + vehicle.color + ")  ·  " + vehicle.licensePlate.toUpperCase();
            }

            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setTitle("Seleccionar vehículo")
                    .setItems(items, (dialog, which) -> {
                        VehicleResponse selectedVehicle = userVehicles.get(which);
                        selectedVehicleId = selectedVehicle.id;
                        tvSelectedVehicle.setText(selectedVehicle.brand + " (" + selectedVehicle.licensePlate + ")");
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });
    }

    private void setupSaveButton() {
        btnSaveSchedule.setOnClickListener(v -> {
            String origin = etOriginAddress.getText() != null ? etOriginAddress.getText().toString() : "";
            String destination = etDestinationAddress.getText() != null ? etDestinationAddress.getText().toString() : "";
            String seatsStr = etOfferedSeats.getText() != null ? etOfferedSeats.getText().toString() : "0";
            String fareStr = etFareAmount.getText() != null ? etFareAmount.getText().toString() : "0";

            if (!validateScheduleInput(origin, destination, seatsStr, fareStr)) {
                return;
            }

            int seats = Integer.parseInt(seatsStr.trim());
            double fare = Double.parseDouble(fareStr.trim());

            // Build daysOfWeek bitmask/string
            List<String> selectedDays = new ArrayList<>();
            if (cbMon.isChecked()) selectedDays.add("1");
            if (cbTue.isChecked()) selectedDays.add("2");
            if (cbWed.isChecked()) selectedDays.add("3");
            if (cbThu.isChecked()) selectedDays.add("4");
            if (cbFri.isChecked()) selectedDays.add("5");
            if (cbSat.isChecked()) selectedDays.add("6");
            if (cbSun.isChecked()) selectedDays.add("0");

            StringBuilder daysOfWeek = new StringBuilder();
            for (int i = 0; i < selectedDays.size(); i++) {
                if (i > 0) daysOfWeek.append(",");
                daysOfWeek.append(selectedDays.get(i));
            }

            // Start date (today) and end date (1 year from now)
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            String startDate = sdf.format(new Date());

            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.YEAR, 1);
            String endDate = sdf.format(cal.getTime());

            String mapboxToken = getString(R.string.mapbox_access_token);

            viewModel.createSchedule(
                    sessionManager.getUserId(), origin, destination,
                    selectedTime, daysOfWeek.toString(), startDate, endDate,
                    selectedVehicleId, seats, fare, mapboxToken
            );
        });
    }

    private boolean validateScheduleInput(String origin, String destination, String seatsStr, String fareStr) {
        tilOriginAddress.setError(null);
        tilDestinationAddress.setError(null);

        if (origin == null || origin.trim().length() < 5) {
            tilOriginAddress.setError("Selecciona un origen valido.");
            return false;
        }
        if (destination == null || destination.trim().length() < 5) {
            tilDestinationAddress.setError("Selecciona un destino valido.");
            return false;
        }
        if (selectedTime == null || selectedTime.isEmpty()) {
            Toast.makeText(this, "Selecciona la hora de salida.", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!cbMon.isChecked() && !cbTue.isChecked() && !cbWed.isChecked() && !cbThu.isChecked()
                && !cbFri.isChecked() && !cbSat.isChecked() && !cbSun.isChecked()) {
            Toast.makeText(this, "Selecciona al menos un dia de la semana.", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (seatsStr == null || !seatsStr.trim().matches("\\d+")) {
            etOfferedSeats.setError(getString(R.string.validation_seats_number));
            return false;
        }
        int seats = Integer.parseInt(seatsStr.trim());
        if (seats < 1 || seats > 12) {
            etOfferedSeats.setError(getString(R.string.validation_seats_range));
            return false;
        }
        if (fareStr == null || !fareStr.trim().matches("\\d+(\\.\\d{1,2})?")) {
            etFareAmount.setError("Ingresa un precio valido en Bs.");
            return false;
        }
        double fare = Double.parseDouble(fareStr.trim());
        if (fare < 0.0 || fare > 1000.0) {
            etFareAmount.setError("El precio debe estar entre Bs 0.00 y Bs 1000.00.");
            return false;
        }
        if (selectedVehicleId == null || selectedVehicleId.trim().isEmpty()) {
            Toast.makeText(this, "Selecciona un vehiculo para el horario.", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void observeViewModel() {
        viewModel.getLoading().observe(this, loading -> {
            btnSaveSchedule.setEnabled(!loading);
            if (loading) {
                btnSaveSchedule.setText("Procesando y Geocodificando...");
            } else {
                btnSaveSchedule.setText("Guardar Horario Programado");
            }
        });

        viewModel.getVehicles().observe(this, vehicles -> {
            userVehicles.clear();
            userVehicles.addAll(vehicles);
            if (!vehicles.isEmpty()) {
                // Automatically select first vehicle
                selectedVehicleId = vehicles.get(0).id;
                tvSelectedVehicle.setText(vehicles.get(0).brand + " (" + vehicles.get(0).licensePlate + ")");
            }
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, sanitizeError(error), Toast.LENGTH_LONG).show();
            }
        });

        viewModel.getSuccessEvent().observe(this, success -> {
            if (success) {
                Toast.makeText(this, "Horario guardado correctamente", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void setupMapPickers() {
        tilOriginAddress.setEndIconOnClickListener(v -> {
            Intent intent = new Intent(this, MapPickerActivity.class);
            startActivityForResult(intent, 1001);
        });
        tilDestinationAddress.setEndIconOnClickListener(v -> {
            Intent intent = new Intent(this, MapPickerActivity.class);
            startActivityForResult(intent, 1002);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            String address = data.getStringExtra(MapPickerActivity.EXTRA_RESULT_ADDRESS);
            if (requestCode == 1001) {
                etOriginAddress.setText(address);
            } else if (requestCode == 1002) {
                etDestinationAddress.setText(address);
            }
        }
    }
}
