package com.example.proyectocarpooling.presentation.schedules.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyectocarpooling.R;
import com.example.proyectocarpooling.data.local.SessionManager;
import com.example.proyectocarpooling.data.model.trip.RecurringReservation;
import com.example.proyectocarpooling.data.model.trip.TripSchedule;
import com.example.proyectocarpooling.presentation.BaseActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

public class BrowseSchedulesActivity extends BaseActivity {

    private SessionManager sessionManager;
    private TripSchedulesViewModel viewModel;

    private MaterialToolbar toolbar;
    private TextInputLayout tilSearch;
    private TextInputEditText etSearch;
    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private ProgressBar pbLoading;

    private TripSchedulesAdapter adapter;
    private final List<TripSchedule> allSchedules = new ArrayList<>();
    private final List<TripSchedule> filteredSchedules = new ArrayList<>();

    // Map filter state
    private boolean hasSelectedFilter = false;
    private double filterLat = 0.0;
    private double filterLng = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse_schedules);

        sessionManager = new SessionManager(this);
        viewModel = new ViewModelProvider(this).get(TripSchedulesViewModel.class);

        bindViews();
        setupToolbar();
        setupRecyclerView();
        setupSearchFilter();
        observeViewModel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.loadActiveSchedules();
    }

    private void bindViews() {
        toolbar = findViewById(R.id.browseToolbar);
        tilSearch = findViewById(R.id.tilSearch);
        etSearch = findViewById(R.id.etSearch);
        recyclerView = findViewById(R.id.rvBrowseSchedules);
        tvEmpty = findViewById(R.id.tvBrowseEmpty);
        pbLoading = findViewById(R.id.pbBrowseLoading);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TripSchedulesAdapter(TripSchedulesAdapter.TYPE_BROWSE_SCHEDULE, new TripSchedulesAdapter.Listener() {
            @Override
            public void onToggleSchedule(TripSchedule schedule, boolean active) {}

            @Override
            public void onCancelSubscription(RecurringReservation subscription) {}

            @Override
            public void onSubscribeToSchedule(TripSchedule schedule) {
                openPreview(schedule);
            }

            @Override
            public void onScheduleClick(TripSchedule schedule) {
                openPreview(schedule);
            }

            @Override
            public void onSubscriptionClick(RecurringReservation subscription) {}
        });
        recyclerView.setAdapter(adapter);
    }

    private void setupSearchFilter() {
        etSearch.setOnClickListener(v -> launchMapPicker());
        tilSearch.setEndIconOnClickListener(v -> {
            if (hasSelectedFilter) {
                clearFilter();
            } else {
                launchMapPicker();
            }
        });
    }

    private void launchMapPicker() {
        Intent intent = new Intent(this, MapPickerActivity.class);
        startActivityForResult(intent, 2001);
    }

    private void clearFilter() {
        hasSelectedFilter = false;
        filterLat = 0.0;
        filterLng = 0.0;
        etSearch.setText("");
        tilSearch.setEndIconDrawable(androidx.core.content.ContextCompat.getDrawable(this, android.R.drawable.ic_dialog_map));
        filterSchedules();
    }

    private void filterSchedules() {
        filteredSchedules.clear();
        if (!hasSelectedFilter) {
            filteredSchedules.addAll(allSchedules);
        } else {
            for (TripSchedule s : allSchedules) {
                double dist = distanceKm(s.destinationLatitude, s.destinationLongitude, filterLat, filterLng);
                if (dist <= 2.0) { // within 2 km radius
                    filteredSchedules.add(s);
                }
            }
        }
        adapter.setItems(filteredSchedules);
        if (filteredSchedules.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            if (hasSelectedFilter) {
                tvEmpty.setText("No hay rutas programadas de otros conductores cerca del destino seleccionado.");
            } else {
                tvEmpty.setText("No hay rutas programadas de otros conductores disponibles.");
            }
        } else {
            tvEmpty.setVisibility(View.GONE);
        }
    }

    private double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double theta = lon1 - lon2;
        double dist = Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2))
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(theta));
        dist = Math.acos(dist);
        dist = Math.toDegrees(dist);
        dist = dist * 60 * 1.1515 * 1.609344;
        return dist;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == 2001 && data != null) {
            String address = data.getStringExtra(MapPickerActivity.EXTRA_RESULT_ADDRESS);
            double lat = data.getDoubleExtra(MapPickerActivity.EXTRA_RESULT_LATITUDE, 0.0);
            double lng = data.getDoubleExtra(MapPickerActivity.EXTRA_RESULT_LONGITUDE, 0.0);

            hasSelectedFilter = true;
            filterLat = lat;
            filterLng = lng;
            etSearch.setText(address != null && !address.trim().isEmpty() ? address : String.format(java.util.Locale.US, "%.5f, %.5f", lat, lng));
            tilSearch.setEndIconDrawable(androidx.core.content.ContextCompat.getDrawable(this, android.R.drawable.ic_menu_close_clear_cancel));

            filterSchedules();
        }
    }

    private void openPreview(TripSchedule schedule) {
        Intent intent = new Intent(this, TripSchedulePreviewActivity.class);
        intent.putExtra("EXTRA_SCHEDULE_ID", schedule.id);
        intent.putExtra("EXTRA_DRIVER_ID", schedule.driverUserId);
        intent.putExtra("EXTRA_DRIVER_NAME", schedule.driverName);
        intent.putExtra("EXTRA_ORIGIN_LAT", schedule.originLatitude);
        intent.putExtra("EXTRA_ORIGIN_LNG", schedule.originLongitude);
        intent.putExtra("EXTRA_ORIGIN_ADDRESS", schedule.originAddress);
        intent.putExtra("EXTRA_DEST_LAT", schedule.destinationLatitude);
        intent.putExtra("EXTRA_DEST_LNG", schedule.destinationLongitude);
        intent.putExtra("EXTRA_DEST_ADDRESS", schedule.destinationAddress);
        intent.putExtra("EXTRA_TIME", schedule.departureTime);
        intent.putExtra("EXTRA_DAYS", schedule.daysOfWeek);
        intent.putExtra("EXTRA_SEATS", schedule.offeredSeats);
        intent.putExtra("EXTRA_FARE", schedule.fareAmount);
        startActivity(intent);
    }

    private void observeViewModel() {
        viewModel.getLoading().observe(this, loading -> {
            pbLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        });

        viewModel.getActiveSchedules().observe(this, activeList -> {
            allSchedules.clear();
            String currentUid = sessionManager.getUserId();
            for (TripSchedule s : activeList) {
                if (s.isActive && !s.driverUserId.equals(currentUid)) {
                    allSchedules.add(s);
                }
            }
            filterSchedules();
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, sanitizeError(error), Toast.LENGTH_LONG).show();
            }
        });
    }
}
