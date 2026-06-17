package com.example.proyectocarpooling.presentation.schedules.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyectocarpooling.R;
import com.example.proyectocarpooling.data.local.SessionManager;
import com.example.proyectocarpooling.data.model.trip.RecurringReservation;
import com.example.proyectocarpooling.data.model.trip.TripSchedule;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class TripSchedulesActivity extends AppCompatActivity implements TripSchedulesAdapter.Listener {

    private SessionManager sessionManager;
    private TripSchedulesViewModel viewModel;

    private MaterialToolbar toolbar;
    private MaterialButtonToggleGroup toggleGroup;
    private RecyclerView recyclerView;
    private TextView emptyStateText;
    private ProgressBar loadingProgress;
    private ExtendedFloatingActionButton fabAction;

    private TripSchedulesAdapter adapter;
    private boolean isDriverView = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_schedules);

        sessionManager = new SessionManager(this);
        viewModel = new ViewModelProvider(this).get(TripSchedulesViewModel.class);

        bindViews();
        setupToolbar();
        setupToggleListener();
        setupFabListener();
        observeViewModel();

        // Load default tab
        updateTabSelection(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshCurrentTab();
    }

    private void bindViews() {
        toolbar = findViewById(R.id.schedulesToolbar);
        toggleGroup = findViewById(R.id.toggleGroup);
        recyclerView = findViewById(R.id.schedulesRecyclerView);
        emptyStateText = findViewById(R.id.emptyStateText);
        loadingProgress = findViewById(R.id.schedulesLoading);
        fabAction = findViewById(R.id.fabAction);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupToggleListener() {
        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                updateTabSelection(checkedId == R.id.btnDriverTab);
            }
        });
    }

    private void setupFabListener() {
        fabAction.setOnClickListener(v -> {
            if (isDriverView) {
                // Open creation screen
                startActivity(new Intent(this, CreateTripScheduleActivity.class));
            } else {
                // Show available schedules dialog for passengers to subscribe
                showAvailableSchedulesDialog();
            }
        });
    }

    private void updateTabSelection(boolean driverTab) {
        isDriverView = driverTab;
        if (isDriverView) {
            adapter = new TripSchedulesAdapter(TripSchedulesAdapter.TYPE_DRIVER_SCHEDULE, this);
            recyclerView.setAdapter(adapter);
            fabAction.setText("Crear Horario");
            fabAction.setIconResource(android.R.drawable.ic_input_add);
            viewModel.loadDriverSchedules(sessionManager.getUserId());
        } else {
            adapter = new TripSchedulesAdapter(TripSchedulesAdapter.TYPE_PASSENGER_SUBSCRIPTION, this);
            recyclerView.setAdapter(adapter);
            fabAction.setText("Buscar Rutas");
            fabAction.setIconResource(android.R.drawable.ic_menu_search);
            viewModel.loadPassengerSubscriptions(sessionManager.getUserId());
        }
    }

    private void refreshCurrentTab() {
        if (isDriverView) {
            viewModel.loadDriverSchedules(sessionManager.getUserId());
        } else {
            viewModel.loadPassengerSubscriptions(sessionManager.getUserId());
        }
    }

    private void observeViewModel() {
        viewModel.getLoading().observe(this, loading -> {
            loadingProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        });

        viewModel.getDriverSchedules().observe(this, schedules -> {
            if (isDriverView) {
                adapter.setItems(schedules);
                emptyStateText.setVisibility(schedules.isEmpty() ? View.VISIBLE : View.GONE);
                emptyStateText.setText("No tienes horarios programados como conductor");
            }
        });

        viewModel.getPassengerSubscriptions().observe(this, subs -> {
            if (!isDriverView) {
                adapter.setItems(subs);
                emptyStateText.setVisibility(subs.isEmpty() ? View.VISIBLE : View.GONE);
                emptyStateText.setText("No estás suscrito a ningún viaje programado");
            }
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.getSuccessMessage().observe(this, msg -> {
            if (msg != null) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // TripSchedulesAdapter.Listener Implementations
    @Override
    public void onToggleSchedule(TripSchedule schedule, boolean active) {
        viewModel.toggleSchedule(schedule, active, sessionManager.getUserId());
    }

    @Override
    public void onCancelSubscription(RecurringReservation subscription) {
        new AlertDialog.Builder(this)
                .setTitle("Confirmar cancelación")
                .setMessage("¿Estás seguro de que deseas cancelar tu suscripción diaria a este viaje?")
                .setPositiveButton("Sí, Cancelar", (dialog, which) -> {
                    viewModel.cancelSubscription(subscription, sessionManager.getUserId());
                })
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    public void onSubscribeToSchedule(TripSchedule schedule) {
        // Not used in this list directly
    }

    private void showAvailableSchedulesDialog() {
        viewModel.loadActiveSchedules();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Rutas Recurrentes Disponibles");

        View view = LayoutInflater.from(this).inflate(R.layout.activity_trip_schedules, null);
        ProgressBar dialogProgress = view.findViewById(R.id.schedulesLoading);
        RecyclerView dialogRecycler = view.findViewById(R.id.schedulesRecyclerView);
        TextView dialogEmpty = view.findViewById(R.id.emptyStateText);
        View dialogToggle = view.findViewById(R.id.toggleGroup);
        View dialogFab = view.findViewById(R.id.fabAction);

        // Hide toggle and FAB in dialog
        dialogToggle.setVisibility(View.GONE);
        dialogFab.setVisibility(View.GONE);

        dialogProgress.setVisibility(View.VISIBLE);
        dialogRecycler.setLayoutManager(new LinearLayoutManager(this));

        AlertDialog dialog = builder.setView(view)
                .setNegativeButton("Cerrar", null)
                .create();

        TripSchedulesAdapter dialogAdapter = new TripSchedulesAdapter(TripSchedulesAdapter.TYPE_BROWSE_SCHEDULE, new TripSchedulesAdapter.Listener() {
            @Override
            public void onToggleSchedule(TripSchedule schedule, boolean active) {}

            @Override
            public void onCancelSubscription(RecurringReservation subscription) {}

            @Override
            public void onSubscribeToSchedule(TripSchedule schedule) {
                dialog.dismiss();
                confirmSubscriptionSeatsDialog(schedule);
            }
        });

        dialogRecycler.setAdapter(dialogAdapter);

        viewModel.getActiveSchedules().observe(this, activeList -> {
            dialogProgress.setVisibility(View.GONE);
            List<TripSchedule> othersList = new ArrayList<>();
            String currentUid = sessionManager.getUserId();
            for (TripSchedule s : activeList) {
                if (!s.driverUserId.equals(currentUid)) {
                    othersList.add(s);
                }
            }
            dialogAdapter.setItems(othersList);
            if (othersList.isEmpty()) {
                dialogEmpty.setVisibility(View.VISIBLE);
                dialogEmpty.setText("No hay rutas programadas de otros conductores disponibles.");
            } else {
                dialogEmpty.setVisibility(View.GONE);
            }
        });

        dialog.show();
    }

    private void confirmSubscriptionSeatsDialog(TripSchedule schedule) {
        String[] options = {"1 asiento", "2 asientos", "3 asientos", "4 asientos"};
        new AlertDialog.Builder(this)
                .setTitle("Seleccionar asientos para suscripción")
                .setItems(options, (dialog, which) -> {
                    int seats = which + 1;
                    viewModel.subscribeToSchedule(schedule, sessionManager.getUserId(), seats);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
