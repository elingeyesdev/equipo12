package com.example.proyectocarpooling.presentation.search.ui;

import com.example.proyectocarpooling.presentation.BaseActivity;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyectocarpooling.CarPoolingApplication;
import com.example.proyectocarpooling.R;
import com.example.proyectocarpooling.data.local.SessionManager;
import com.example.proyectocarpooling.data.model.DriverTripMatch;
import com.example.proyectocarpooling.data.model.TripResponse;
import com.example.proyectocarpooling.data.model.ReservationResponse;
import com.example.proyectocarpooling.data.remote.search.MapboxGeocodingRemoteDataSource;
import com.example.proyectocarpooling.data.remote.search.MapboxGeocodingRemoteDataSource.SearchSuggestion;
import com.example.proyectocarpooling.domain.repository.TripRepository;
import com.example.proyectocarpooling.presentation.main.ui.MainActivity;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.appbar.MaterialToolbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SearchTripActivity extends BaseActivity implements SearchTripAdapter.Listener {

    private enum Category {
        ALL,
        NEAR,
        FAR,
        RATED
    }

    public static final class SearchTripResultItem {
        public final String tripId;
        public final String driverName;
        public final String routeDescription;
        public final String originLabel;
        public final String destinationLabel;
        public final String statusLabel;
        public final int availableSeats;
        public final double distanceKm;
        public final int etaMinutes;
        public final Double driverRating;
        public final String vehicleInfo;
        public final double originLatitude;
        public final double originLongitude;
        public final double destinationLatitude;
        public final double destinationLongitude;
        public final String driverUserId;
        public final String myReservationId;
        public final String myReservationStatus;

        public SearchTripResultItem(
                String tripId,
                String driverName,
                String routeDescription,
                String originLabel,
                String destinationLabel,
                String statusLabel,
                int availableSeats,
                double distanceKm,
                int etaMinutes,
                Double driverRating,
                String vehicleInfo,
                double originLatitude,
                double originLongitude,
                double destinationLatitude,
                double destinationLongitude,
                String driverUserId,
                String myReservationId,
                String myReservationStatus) {
            this.tripId = tripId;
            this.driverName = driverName;
            this.routeDescription = routeDescription;
            this.originLabel = originLabel;
            this.destinationLabel = destinationLabel;
            this.statusLabel = statusLabel;
            this.availableSeats = availableSeats;
            this.distanceKm = distanceKm;
            this.etaMinutes = etaMinutes;
            this.driverRating = driverRating;
            this.vehicleInfo = vehicleInfo;
            this.originLatitude = originLatitude;
            this.originLongitude = originLongitude;
            this.destinationLatitude = destinationLatitude;
            this.destinationLongitude = destinationLongitude;
            this.driverUserId = driverUserId;
            this.myReservationId = myReservationId;
            this.myReservationStatus = myReservationStatus;
        }
    }

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private MaterialToolbar toolbar;
    private ChipGroup categoryGroup;
    private Button searchButton;
    private Button clearButton;
    private TextView resultsTitle;
    private TextView resultsCount;
    private TextView statusMessage;
    private View loadingView;
    private View emptyView;
    private TextView emptyText;
    private RecyclerView resultsRecycler;
    private SearchTripAdapter adapter;

    private final List<SearchTripResultItem> allResults = new ArrayList<>();
    private final List<SearchTripResultItem> visibleResults = new ArrayList<>();


    private TripRepository tripRepository;
    private SessionManager sessionManager;
    private com.example.proyectocarpooling.data.remote.RatingRemoteDataSource ratingRemoteDataSource;
    private ExecutorService backgroundExecutor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_trip);

        CarPoolingApplication app = (CarPoolingApplication) getApplication();
        sessionManager = app.getSessionManager();
        tripRepository = app.getTripRepository();
        ratingRemoteDataSource = app.getRatingRemoteDataSource();

        backgroundExecutor = Executors.newSingleThreadExecutor();

        bindViews();
        setupToolbar();
        setupCategoryChips();
        setupFilterListeners();
        setupActions();
        searchTrips();
    }

    private void bindViews() {
        toolbar = findViewById(R.id.searchTripToolbar);
        categoryGroup = findViewById(R.id.searchTripCategoryGroup);
        searchButton = findViewById(R.id.searchTripSearchButton);
        clearButton = findViewById(R.id.searchTripClearButton);
        resultsTitle = findViewById(R.id.searchTripResultsTitle);
        resultsCount = findViewById(R.id.searchTripResultsCount);
        statusMessage = findViewById(R.id.searchTripStatusMessage);
        loadingView = findViewById(R.id.searchTripLoading);
        emptyView = findViewById(R.id.searchTripEmptyState);
        emptyText = findViewById(R.id.searchTripEmptyText);
        resultsRecycler = findViewById(R.id.searchTripRecycler);

        adapter = new SearchTripAdapter(this, sessionManager.getUserId());
        resultsRecycler.setLayoutManager(new LinearLayoutManager(this));
        resultsRecycler.setAdapter(adapter);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setTitle(R.string.search_trip_title);
    }



    private void setupCategoryChips() {
        Chip all = categoryGroup.findViewById(R.id.searchTripChipAll);
        if (all != null) {
            all.setChecked(true);
        }
    }

    private void setupFilterListeners() {
<<<<<<< HEAD
=======
        minPriceInput.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void afterTextChanged(Editable s) {
                minPriceLayout.setError(null);
                applyFilters();
            }
        });
        maxPriceInput.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void afterTextChanged(Editable s) {
                maxPriceLayout.setError(null);
                applyFilters();
            }
        });
>>>>>>> f2994777d8fb6d95afab56b84dcd87c7046aa833
        categoryGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            applyFilters();
        });
    }

    private void setupActions() {
        searchButton.setOnClickListener(v -> searchTrips());
        clearButton.setOnClickListener(v -> resetAllFilters());
    }

    private void searchTrips() {
        setLoading(true);
        setStatusMessage(getString(R.string.search_trip_results_loading), false);

        backgroundExecutor.execute(() -> {
            try {
                List<DriverTripMatch> candidates = tripRepository.searchTripMatchCandidates(Double.NaN, Double.NaN);
                List<SearchTripResultItem> enriched = enrichCandidates(candidates);

                final List<SearchTripResultItem> finalResults = enriched;
                runOnUiThread(() -> {
                    allResults.clear();
                    allResults.addAll(finalResults);
                    applyFilters();
                    setLoading(false);
                    setStatusMessage(getString(R.string.search_trip_results_count, allResults.size()), false);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    renderEmptyState(getString(R.string.search_trip_error_state), true);
                    setStatusMessage(e.getMessage() != null ? e.getMessage() : getString(R.string.search_trip_error_state), true);
                });
            }
        });
    }



    private List<SearchTripResultItem> enrichCandidates(List<DriverTripMatch> candidates) throws IOException, JSONException {
        List<SearchTripResultItem> items = new ArrayList<>();
        String accessorUserId = sessionManager.getUserId();

        for (DriverTripMatch candidate : candidates) {
            TripResponse trip = tripRepository.getTripByIdIfPresent(candidate.tripId);
            String driverUserId = trip != null ? trip.driverUserId : null;
            Double driverRating = null;

            if (driverUserId != null && !driverUserId.isBlank()) {
                try {
                    JSONObject summary = ratingRemoteDataSource.getUserRatingSummary(driverUserId, accessorUserId);
                    double avg = summary.optDouble("averageScore", Double.NaN);
                    if (!Double.isNaN(avg) && avg > 0.0) {
                        driverRating = avg;
                    }
                } catch (Exception ignored) {
                    driverRating = null;
                }
            }

            String myReservationId = null;
            String myReservationStatus = null;
            if (accessorUserId != null && !accessorUserId.isBlank()) {
                try {
                    List<ReservationResponse> pendingRes = tripRepository.getReservations(candidate.tripId);
                    for (ReservationResponse res : pendingRes) {
                        if (accessorUserId.equals(res.passengerUserId)) {
                            myReservationId = res.id;
                            myReservationStatus = "Pending";
                            break;
                        }
                    }
                    if (myReservationId == null) {
                        List<ReservationResponse> confirmedRes = tripRepository.getConfirmedReservations(candidate.tripId);
                        for (ReservationResponse res : confirmedRes) {
                            if (accessorUserId.equals(res.passengerUserId)) {
                                myReservationId = res.id;
                                myReservationStatus = "Confirmed";
                                break;
                            }
                        }
                    }
                    if (myReservationId == null) {
                        List<ReservationResponse> boardedRes = tripRepository.getBoardedPassengers(candidate.tripId);
                        for (ReservationResponse res : boardedRes) {
                            if (accessorUserId.equals(res.passengerUserId)) {
                                myReservationId = res.id;
                                myReservationStatus = "Boarded";
                                break;
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }

            String routeDescription = String.format(Locale.US, "%s -> %s",
                    candidate.originAddress == null || candidate.originAddress.isEmpty() ? "Origen" : candidate.originAddress,
                    candidate.destinationAddress == null || candidate.destinationAddress.isEmpty() ? "Destino" : candidate.destinationAddress);
            String vehicleInfo = buildVehicleInfo(candidate.vehicleBrand, candidate.vehicleColor, candidate.vehiclePlate);

            items.add(new SearchTripResultItem(
                    candidate.tripId,
                    candidate.driverName,
                    routeDescription,
                    candidate.originAddress,
                    candidate.destinationAddress,
                    candidate.statusLabel,
                    candidate.availableSeats,
                    candidate.distanceKm,
                    candidate.etaMinutes,
                    driverRating,
                    vehicleInfo,
                    candidate.originLatitude,
                    candidate.originLongitude,
                    candidate.destinationLatitude,
                    candidate.destinationLongitude,
                    driverUserId,
                    myReservationId,
                    myReservationStatus));
        }

        return items;
    }

    private void applyFilters() {
        if (allResults.isEmpty()) {
            adapter.setItems(new ArrayList<>());
            resultsCount.setText(getString(R.string.search_trip_results_count, 0));
            renderEmptyState(getString(R.string.search_trip_empty_state), true);
            return;
        }

        Category category = selectedCategory();
        List<SearchTripResultItem> filtered = new ArrayList<>();

        for (SearchTripResultItem item : allResults) {
            if (!matchesCategory(item, category)) {
                continue;
            }

            filtered.add(item);
        }

        // Sort descending by rating when RATED (Calificados) category is checked
        if (category == Category.RATED) {
            filtered.sort((o1, o2) -> {
                double r1 = o1.driverRating != null ? o1.driverRating : 0.0;
                double r2 = o2.driverRating != null ? o2.driverRating : 0.0;
                return Double.compare(r2, r1); // Best to worst
            });
        }

        adapter.setItems(filtered);
        resultsCount.setText(getString(R.string.search_trip_results_count, filtered.size()));
        if (filtered.isEmpty()) {
            renderEmptyState(getString(R.string.search_trip_no_results), false);
        } else {
            hideEmptyState();
        }
    }



    private boolean matchesCategory(SearchTripResultItem item, Category category) {
        switch (category) {
            case NEAR:
                return item.distanceKm <= 5.0;
            case FAR:
                return item.distanceKm >= 12.0;
            case RATED:
            case ALL:
            default:
                return true;
        }
    }

    private Category selectedCategory() {
        int checkedId = categoryGroup.getCheckedChipId();
        if (checkedId == R.id.searchTripChipNear) return Category.NEAR;
        if (checkedId == R.id.searchTripChipFar) return Category.FAR;
        if (checkedId == R.id.searchTripChipRated) return Category.RATED;
        return Category.ALL;
    }

    private void resetAllFilters() {
        categoryGroup.check(R.id.searchTripChipAll);
        searchTrips();
    }

    private void setLoading(boolean visible) {
        loadingView.setVisibility(visible ? View.VISIBLE : View.GONE);
        resultsRecycler.setVisibility(visible ? View.GONE : View.VISIBLE);
        searchButton.setEnabled(!visible);
        clearButton.setEnabled(!visible);
    }

    private void renderEmptyState(String message, boolean keepListHidden) {
        emptyText.setText(message);
        emptyView.setVisibility(View.VISIBLE);
        if (keepListHidden) {
            resultsRecycler.setVisibility(View.GONE);
        }
        loadingView.setVisibility(View.GONE);
    }

    private void hideEmptyState() {
        emptyView.setVisibility(View.GONE);
        resultsRecycler.setVisibility(View.VISIBLE);
    }

    private void setStatusMessage(String message, boolean error) {
        statusMessage.setText(message);
        statusMessage.setTextColor(ContextCompat.getColor(this, error ? R.color.uber_error : R.color.carpool_text_secondary));
        statusMessage.setVisibility(View.VISIBLE);
    }



    private static String buildVehicleInfo(String brand, String color, String plate) {
        StringBuilder builder = new StringBuilder();
        if (brand != null && !brand.isEmpty()) builder.append(brand);
        if (color != null && !color.isEmpty()) {
            if (builder.length() > 0) builder.append(" • ");
            builder.append(color);
        }
        if (plate != null && !plate.isEmpty()) {
            if (builder.length() > 0) builder.append(" • ");
            builder.append(plate);
        }
        return builder.toString();
    }



    private static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        final double earthRadius = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.getDefault());
    }

    @Override
    public void onViewRoute(SearchTripResultItem item) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(MainActivity.EXTRA_APPLY_FAVORITE_KIND, "route");
        intent.putExtra(MainActivity.EXTRA_APPLY_ORIGIN_LAT, item.originLatitude);
        intent.putExtra(MainActivity.EXTRA_APPLY_ORIGIN_LNG, item.originLongitude);
        intent.putExtra(MainActivity.EXTRA_APPLY_DEST_LAT, item.destinationLatitude);
        intent.putExtra(MainActivity.EXTRA_APPLY_DEST_LNG, item.destinationLongitude);
        intent.putExtra(MainActivity.EXTRA_HISTORY_ROUTE_PREVIEW, true);
        intent.putExtra(MainActivity.EXTRA_ROUTE_PREVIEW_CONTEXT, MainActivity.ROUTE_PREVIEW_CONTEXT_SEARCH_TRIP);
        intent.putExtra(MainActivity.EXTRA_ROUTE_PREVIEW_TRIP_ID, item.tripId);
        intent.putExtra(MainActivity.EXTRA_ROUTE_PREVIEW_DRIVER_NAME, item.driverName);
        startActivity(intent);
    }

    @Override
    public void onReserveTrip(SearchTripResultItem item) {
        String userId = sessionManager.getUserId();
        if (userId == null || userId.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("Acceso requerido")
                    .setMessage("Inicia sesión para reservar plaza")
                    .setPositiveButton("Aceptar", null)
                    .show();
            return;
        }

        if (sessionManager.hasPassengerBookedTrip()) {
            new AlertDialog.Builder(this)
                    .setTitle("Reserva activa")
                    .setMessage(R.string.passenger_already_booked)
                    .setPositiveButton("Aceptar", null)
                    .show();
            return;
        }

        setLoading(true);
        backgroundExecutor.execute(() -> {
            try {
                tripRepository.createReservation(item.tripId, userId, 1);
                
                JSONObject activeRes = ((CarPoolingApplication) getApplication()).getUserRepository().getActiveReservation(userId);
                if (activeRes != null) {
                    String resId = activeRes.optString("reservationId", "");
                    String code = activeRes.optString("boardingCode", "");
                    String driverName = activeRes.optString("driverName", "");
                    sessionManager.savePassengerBookedTrip(item.tripId, resId, code, driverName);
                }

                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.toast_reservation_created, Toast.LENGTH_SHORT).show();
                    searchTrips();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    new AlertDialog.Builder(this)
                            .setTitle("Error de reserva")
                            .setMessage(sanitizeError(e.getMessage() != null ? e.getMessage() : getString(R.string.toast_reservation_failed)))
                            .setPositiveButton("Aceptar", null)
                            .show();
                });
            }
        });
    }

    @Override
    public void onCancelReservation(SearchTripResultItem item) {
        if (item.myReservationId == null) return;
        setLoading(true);
        backgroundExecutor.execute(() -> {
            try {
                tripRepository.cancelReservation(item.myReservationId);
                if (item.tripId.equals(sessionManager.getPassengerBookedTripId())) {
                    sessionManager.clearPassengerBookedTrip();
                }
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.cancel_reservation_success, Toast.LENGTH_SHORT).show();
                    searchTrips();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    new AlertDialog.Builder(this)
                            .setTitle("Error al cancelar")
                            .setMessage(sanitizeError(e.getMessage() != null ? e.getMessage() : getString(R.string.cancel_reservation_error)))
                            .setPositiveButton("Aceptar", null)
                            .show();
                });
            }
        });
    }

    @Override
    public void onBoardTrip(SearchTripResultItem item) {
        if (item.myReservationId == null) return;
        setLoading(true);
        backgroundExecutor.execute(() -> {
            try {
                tripRepository.boardPassenger(item.tripId, item.myReservationId);
                
                String userId = sessionManager.getUserId();
                JSONObject activeRes = ((CarPoolingApplication) getApplication()).getUserRepository().getActiveReservation(userId);
                if (activeRes != null) {
                    String resId = activeRes.optString("reservationId", "");
                    String code = activeRes.optString("boardingCode", "");
                    String driverName = activeRes.optString("driverName", "");
                    sessionManager.savePassengerBookedTrip(item.tripId, resId, code, driverName);
                }

                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.toast_boarding_confirmed, Toast.LENGTH_SHORT).show();
                    searchTrips();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    new AlertDialog.Builder(this)
                            .setTitle("Error de abordaje")
                            .setMessage(sanitizeError(e.getMessage() != null ? e.getMessage() : getString(R.string.toast_boarding_failed)))
                            .setPositiveButton("Aceptar", null)
                            .show();
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        backgroundExecutor.shutdownNow();
    }
}