package com.example.proyectocarpooling.presentation.schedules.ui;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.proyectocarpooling.R;
import com.example.proyectocarpooling.data.local.SessionManager;
import com.example.proyectocarpooling.data.model.user.VehicleResponse;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CreateTripScheduleActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private CreateTripScheduleViewModel viewModel;

    private MaterialToolbar toolbar;
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
        setupTimePicker();
        setupVehiclePicker();
        setupSaveButton();
        observeViewModel();

        // Load vehicles
        viewModel.loadVehicles(sessionManager.getUserId());
    }

    private void bindViews() {
        toolbar = findViewById(R.id.createScheduleToolbar);
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
                items[i] = vehicle.brand + " " + vehicle.model + " (" + vehicle.licensePlate + ")";
            }

            new AlertDialog.Builder(this)
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

            int seats = 4;
            try { seats = Integer.parseInt(seatsStr); } catch (NumberFormatException ignored) {}

            double fare = 15.0;
            try { fare = Double.parseDouble(fareStr); } catch (NumberFormatException ignored) {}

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
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.getSuccessEvent().observe(this, success -> {
            if (success) {
                Toast.makeText(this, "Horario guardado correctamente", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}
