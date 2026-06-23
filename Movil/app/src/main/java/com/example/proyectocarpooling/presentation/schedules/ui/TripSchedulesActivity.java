package com.example.proyectocarpooling.presentation.schedules.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyectocarpooling.R;
import com.example.proyectocarpooling.data.local.SessionManager;
import com.example.proyectocarpooling.data.model.trip.RecurringReservation;
import com.example.proyectocarpooling.data.model.trip.TripSchedule;
import com.example.proyectocarpooling.presentation.BaseActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class TripSchedulesActivity extends BaseActivity implements TripSchedulesAdapter.Listener {

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
        setupSwipeToDelete();

        // Initialize default tab selection based on user role (Driver vs Passenger)
        boolean isDriver = sessionManager.isDriver();
        if (isDriver) {
            toggleGroup.setVisibility(View.VISIBLE);
            updateTabSelection(true);
        } else {
            toggleGroup.setVisibility(View.GONE);
            updateTabSelection(false);
        }
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
        int primaryColor = androidx.core.content.ContextCompat.getColor(this, R.color.carpool_primary);
        int whiteColor = androidx.core.content.ContextCompat.getColor(this, R.color.white);

        int[][] states = new int[][] {
            new int[] { android.R.attr.state_checked },
            new int[] { -android.R.attr.state_checked }
        };

        android.content.res.ColorStateList bgStates = new android.content.res.ColorStateList(states, new int[] { primaryColor, android.graphics.Color.TRANSPARENT });
        android.content.res.ColorStateList textStates = new android.content.res.ColorStateList(states, new int[] { whiteColor, primaryColor });
        android.content.res.ColorStateList strokeStates = new android.content.res.ColorStateList(states, new int[] { primaryColor, primaryColor });

        com.google.android.material.button.MaterialButton btnDriver = findViewById(R.id.btnDriverTab);
        com.google.android.material.button.MaterialButton btnPassenger = findViewById(R.id.btnPassengerTab);

        btnDriver.setBackgroundTintList(bgStates);
        btnDriver.setTextColor(textStates);
        btnDriver.setStrokeColor(strokeStates);

        btnPassenger.setBackgroundTintList(bgStates);
        btnPassenger.setTextColor(textStates);
        btnPassenger.setStrokeColor(strokeStates);

        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                updateTabSelection(checkedId == R.id.btnDriverTab);
            }
        });
    }

    private void setupSwipeToDelete() {
        androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback simpleCallback = new androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(0, androidx.recyclerview.widget.ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (adapter != null && position != RecyclerView.NO_POSITION) {
                    Object item = adapter.getItemAt(position);
                    if (item instanceof TripSchedule) {
                        onDeleteSchedule((TripSchedule) item);
                    } else if (item instanceof RecurringReservation) {
                        onCancelSubscription((RecurringReservation) item);
                    }
                }
            }

            @Override
            public void onChildDraw(@NonNull android.graphics.Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY, int actionState, boolean isCurrentlyActive) {
                if (actionState == androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_SWIPE && dX < 0) {
                    View itemView = viewHolder.itemView;
                    android.graphics.Paint paint = new android.graphics.Paint();
                    paint.setColor(android.graphics.Color.parseColor("#FFE02020"));

                    c.drawRoundRect(
                            (float) itemView.getRight() + dX, (float) itemView.getTop(),
                            (float) itemView.getRight(), (float) itemView.getBottom(),
                            18f, 18f, paint
                    );

                    android.graphics.drawable.Drawable deleteIcon = androidx.core.content.ContextCompat.getDrawable(TripSchedulesActivity.this, android.R.drawable.ic_menu_delete);
                    if (deleteIcon != null) {
                        int iconMargin = (itemView.getHeight() - deleteIcon.getIntrinsicHeight()) / 2;
                        int iconTop = itemView.getTop() + iconMargin;
                        int iconBottom = iconTop + deleteIcon.getIntrinsicHeight();
                        int iconLeft = itemView.getRight() - iconMargin - deleteIcon.getIntrinsicWidth();
                        int iconRight = itemView.getRight() - iconMargin;

                        deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                        deleteIcon.setTint(android.graphics.Color.WHITE);
                        deleteIcon.draw(c);
                    }
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };
        androidx.recyclerview.widget.ItemTouchHelper itemTouchHelper = new androidx.recyclerview.widget.ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    private void setupFabListener() {
        fabAction.setOnClickListener(v -> {
            if (isDriverView) {
                // Open creation screen
                startActivity(new Intent(this, CreateTripScheduleActivity.class));
            } else {
                // Open browse schedules activity for passengers to search and subscribe
                startActivity(new Intent(this, BrowseSchedulesActivity.class));
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
                Toast.makeText(this, sanitizeError(error), Toast.LENGTH_LONG).show();
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
                .setNegativeButton("No", (dialog, which) -> {
                    refreshCurrentTab();
                })
                .setOnCancelListener(dialog -> {
                    refreshCurrentTab();
                })
                .show();
    }

    @Override
    public void onSubscribeToSchedule(TripSchedule schedule) {
        // Not used in this list directly
    }

    @Override
    public void onScheduleClick(TripSchedule schedule) {
        Intent intent = new Intent(this, TripScheduleDetailActivity.class);
        intent.putExtra("EXTRA_SCHEDULE_ID", schedule.id);
        intent.putExtra("EXTRA_ORIGIN", schedule.originAddress);
        intent.putExtra("EXTRA_DESTINATION", schedule.destinationAddress);
        intent.putExtra("EXTRA_TIME", schedule.departureTime);
        intent.putExtra("EXTRA_DAYS", schedule.daysOfWeek);
        intent.putExtra("EXTRA_SEATS", schedule.offeredSeats);
        intent.putExtra("EXTRA_FARE", schedule.fareAmount);
        startActivity(intent);
    }

    public void onDeleteSchedule(TripSchedule schedule) {
        new AlertDialog.Builder(this)
                .setTitle("Confirmar eliminación")
                .setMessage("¿Estás seguro de que deseas eliminar este viaje programado permanentemente?")
                .setPositiveButton("Sí, Eliminar", (dialog, which) -> {
                    viewModel.deleteSchedule(schedule.id, sessionManager.getUserId());
                })
                .setNegativeButton("Cancelar", (dialog, which) -> {
                    refreshCurrentTab();
                })
                .setOnCancelListener(dialog -> {
                    refreshCurrentTab();
                })
                .show();
    }


}
