package com.example.proyectocarpooling.presentation.main.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.view.MenuItem;
import android.widget.Toast;
import android.app.AlertDialog;
import android.widget.EditText;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;

import com.example.proyectocarpooling.R;
import com.example.proyectocarpooling.data.local.ApiBaseUrlProvider;
import com.example.proyectocarpooling.data.local.SessionManager;
import com.example.proyectocarpooling.data.local.UserAccessProvider;
import com.example.proyectocarpooling.data.model.ReservationResponse;
import com.example.proyectocarpooling.data.model.TripResponse;
import com.example.proyectocarpooling.data.remote.TripsRemoteDataSource;
import com.example.proyectocarpooling.data.remote.user.FavoritesRemoteDataSource;
import com.example.proyectocarpooling.data.repository.TripRepositoryImpl;
import com.example.proyectocarpooling.domain.repository.TripRepository;
import com.example.proyectocarpooling.domain.model.CreateTripResult;
import com.example.proyectocarpooling.domain.usecase.user.UserAccessUseCase;
import com.example.proyectocarpooling.presentation.auth.ui.LoginActivity;
import com.example.proyectocarpooling.presentation.driver.ui.DriverPassengerRequestsActivity;
import com.example.proyectocarpooling.presentation.favorites.ui.FavoritePlacesActivity;
import com.example.proyectocarpooling.presentation.history.ui.TripHistoryActivity;
import com.example.proyectocarpooling.presentation.match.ui.DriverMatchActivity;
import com.example.proyectocarpooling.presentation.main.viewmodel.MainViewModel;
import com.example.proyectocarpooling.presentation.profile.ui.ProfileActivity;

import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.MapView;
import com.mapbox.maps.Style;
import com.mapbox.maps.plugin.Plugin;
import com.mapbox.maps.plugin.annotation.AnnotationPlugin;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions;
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotation;
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManager;
import com.mapbox.maps.plugin.gestures.GesturesUtils;
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin;
import com.mapbox.maps.plugin.locationcomponent.LocationComponentUtils;
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_APPLY_FAVORITE_ID = "apply_favorite_id";
    public static final String EXTRA_APPLY_FAVORITE_KIND = "apply_favorite_kind";
    public static final String EXTRA_APPLY_ORIGIN_LAT = "apply_origin_lat";
    public static final String EXTRA_APPLY_ORIGIN_LNG = "apply_origin_lng";
    public static final String EXTRA_APPLY_DEST_LAT = "apply_dest_lat";
    public static final String EXTRA_APPLY_DEST_LNG = "apply_dest_lng";
    public static final String EXTRA_APPLY_PLACE_AS_ORIGIN = "apply_place_as_origin";

    private enum SelectionMode { NONE, ORIGIN, DESTINATION }

    private MapView mapView;
    private ProgressBar loadingIndicator;
    private TextView selectionInstruction;
    private TextView originText;
    private TextView destinationText;
    private TextView statusText;
    private TextView routeTimeText;
    private ImageButton menuToggleButton;
    private Button selectOriginButton;
    private Button selectDestinationButton;
    private Button createTripButton;
    private Button cancelTripButton;
    private Button reserveTripButton;
    private Button findDriverButton;
    private Button cancelPassengerReservationButton;
    private Button viewBoardedPassengersButton;
    private Button markBoardedButton;
    private Button startTripButton;
    private Button finishTripButton;
    private LinearLayout controlPanel;
    private LinearLayout selectionButtonsRow;
    private LinearLayout tripButtonsRow;
    private LinearLayout passengerButtonsRow;
    private Button saveFavoriteButton;

    private boolean isInitialPositionSet = false;
    private Point selectedOrigin;
    private Point selectedDestination;
    private SelectionMode selectionMode = SelectionMode.NONE;
    private String activeTripId;
    private String lastTripStatusLabel;
    private String lastRouteTimeLabel;
    private int activeTripAvailableSeats = 0;
    private Point currentDriverPosition;

    private LocationComponentPlugin locationComponentPlugin;
    private PointAnnotationManager pointAnnotationManager;
    private PolylineAnnotationManager polylineAnnotationManager;
    private PolylineAnnotation routeAnnotation;
    private PointAnnotation originAnnotation;
    private PointAnnotation destinationAnnotation;
    private Bitmap originMarkerBitmap;
    private Bitmap destinationMarkerBitmap;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private TextView drawerUserTitle;
    private TextView drawerUserSubtitle;
    private SessionManager sessionManager;
    private UserAccessUseCase userAccessUseCase;
    private MainViewModel mainViewModel;
    private BottomSheetBehavior<LinearLayout> bottomSheetBehavior;
    private boolean isDriverUser;

    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<Intent> driverMatchLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    String tripId = result.getData().getStringExtra(DriverMatchActivity.EXTRA_RESULT_TRIP_ID);
                    if (tripId != null && !tripId.isEmpty()) {
                        Toast.makeText(MainActivity.this, R.string.toast_passenger_reserved_with_driver, Toast.LENGTH_LONG).show();
                    }
                }
            });

    private final ActivityResultLauncher<String[]> locationPermissionRequest =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarseLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                if ((fineLocationGranted != null && fineLocationGranted) || (coarseLocationGranted != null && coarseLocationGranted)) {
                    setupLocationComponent();
                } else {
                    setProgressVisible(false);
                    Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
                }
            });

    private final OnIndicatorPositionChangedListener onIndicatorPositionChangedListener = point -> {
        runOnUiThread(() -> {
            currentDriverPosition = point;
            if (!isInitialPositionSet) {
                mapView.getMapboxMap().setCamera(new CameraOptions.Builder()
                        .center(point)
                        .zoom(16.0)
                        .build());
                isInitialPositionSet = true;
                setProgressVisible(false);
            }
        });
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        sessionManager = new SessionManager(this);
        userAccessUseCase = UserAccessProvider.create(this);
        if (!sessionManager.hasActiveSession()) {
            navigateToLogin();
            return;
        }
        isDriverUser = sessionManager.isDriver();

        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);
        restoreStateFromViewModel();

        bindViews();
        setProgressVisible(true);

        if (mapView != null) {
            mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS, style -> {
                initializeMapAnnotations();
                checkLocationPermissions();
            });
        }

        setupMapClickListener();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void restoreStateFromViewModel() {
        selectedOrigin = mainViewModel.getSelectedOrigin();
        selectedDestination = mainViewModel.getSelectedDestination();
        activeTripId = mainViewModel.getActiveTripId();
        lastTripStatusLabel = mainViewModel.getLastTripStatusLabel();
        activeTripAvailableSeats = mainViewModel.getActiveTripAvailableSeats();
        lastRouteTimeLabel = mainViewModel.getLastRouteTimeLabel();
    }

    private void syncSelectionStateToViewModel() {
        mainViewModel.setSelectedOrigin(selectedOrigin);
        mainViewModel.setSelectedDestination(selectedDestination);
    }

    private void syncTripStateToViewModel() {
        mainViewModel.setActiveTripId(activeTripId);
        mainViewModel.setLastTripStatusLabel(lastTripStatusLabel);
        mainViewModel.setActiveTripAvailableSeats(activeTripAvailableSeats);
        mainViewModel.setLastRouteTimeLabel(lastRouteTimeLabel);
    }

    private void bindViews() {
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        mapView = findViewById(R.id.mapView);
        controlPanel = findViewById(R.id.controlPanel);
        loadingIndicator = findViewById(R.id.loadingIndicator);
        selectionInstruction = findViewById(R.id.selectionInstruction);
        originText = findViewById(R.id.originText);
        destinationText = findViewById(R.id.destinationText);
        statusText = findViewById(R.id.statusText);
        routeTimeText = findViewById(R.id.routeTimeText);
        menuToggleButton = findViewById(R.id.menuToggleButton);
        selectOriginButton = findViewById(R.id.selectOriginButton);
        selectDestinationButton = findViewById(R.id.selectDestinationButton);
        createTripButton = findViewById(R.id.createTripButton);
        cancelTripButton = findViewById(R.id.cancelTripButton);
        reserveTripButton = findViewById(R.id.reserveTripButton);
        findDriverButton = findViewById(R.id.findDriverButton);
        cancelPassengerReservationButton = findViewById(R.id.cancelPassengerReservationButton);
        viewBoardedPassengersButton = findViewById(R.id.viewBoardedPassengersButton);
        markBoardedButton = findViewById(R.id.markBoardedButton);
        startTripButton = findViewById(R.id.startTripButton);
        finishTripButton = findViewById(R.id.finishTripButton);
        selectionButtonsRow = findViewById(R.id.selectionButtonsRow);
        tripButtonsRow = findViewById(R.id.tripButtonsRow);
        passengerButtonsRow = findViewById(R.id.passengerButtonsRow);
        saveFavoriteButton = findViewById(R.id.saveFavoriteButton);

        if (navigationView != null) {
            var header = navigationView.getHeaderView(0);
            drawerUserTitle = header.findViewById(R.id.drawerUserTitle);
            drawerUserSubtitle = header.findViewById(R.id.drawerUserSubtitle);
        }

        setupBottomSheetBehavior();
        setupDrawer();

        menuToggleButton.setOnClickListener(v -> toggleBottomSheet());
        selectOriginButton.setOnClickListener(v -> setSelectionMode(SelectionMode.ORIGIN));
        selectDestinationButton.setOnClickListener(v -> setSelectionMode(SelectionMode.DESTINATION));
        reserveTripButton.setOnClickListener(v -> reserveTrip());
        findDriverButton.setOnClickListener(v -> openDriverMatchScreen());
        cancelPassengerReservationButton.setOnClickListener(v -> cancelPassengerReservation());
        viewBoardedPassengersButton.setOnClickListener(v -> viewBoardedPassengers());
        markBoardedButton.setOnClickListener(v -> markPassengerBoardedByName());
        startTripButton.setOnClickListener(v -> startTrip());
        finishTripButton.setOnClickListener(v -> finishTrip());
        createTripButton.setOnClickListener(v -> {
            if (activeTripId != null) {
                Toast.makeText(this, R.string.toast_trip_exists, Toast.LENGTH_SHORT).show();
                return;
            }
            if (selectedOrigin == null) {
                Toast.makeText(this, R.string.toast_origin_required, Toast.LENGTH_SHORT).show();
                return;
            }
            if (selectedDestination == null) {
                Toast.makeText(this, R.string.toast_destination_required, Toast.LENGTH_SHORT).show();
                return;
            }
            createTrip();
        });
        cancelTripButton.setOnClickListener(v -> {
            if (activeTripId == null) {
                Toast.makeText(this, R.string.toast_no_trip, Toast.LENGTH_SHORT).show();
                return;
            }
            cancelTrip();
        });

        if (saveFavoriteButton != null) {
            saveFavoriteButton.setOnClickListener(v -> openSaveFavoriteDialog());
        }

        consumeApplyFavoriteIntent(getIntent());

        updateCoordinateLabels();
        applyRoleAccess();
        refreshButtons();
        updateStatusText();
        updateRouteTimeText();

        maybeRestoreDriverActiveTripAsync();
    }

    private void maybeRestoreDriverActiveTripAsync() {
        if (!isDriverUser || (activeTripId != null && !activeTripId.isEmpty())) {
            return;
        }

        final String apiBase = ApiBaseUrlProvider.get(this);
        final String mapboxToken = getString(R.string.mapbox_access_token);
        final String userId = sessionManager.getUserId();
        final String fullName = sessionManager.getFullName();

        backgroundExecutor.execute(() -> {
            try {
                TripRepository repository = new TripRepositoryImpl(new TripsRemoteDataSource(apiBase, mapboxToken));
                TripResponse restored = null;

                if (userId != null && !userId.isBlank()) {
                    restored = repository.findActiveTripForDriver(userId, fullName);
                }

                if (restored == null) {
                    String saved = sessionManager.getDriverActiveTripId();
                    if (!saved.isEmpty()) {
                        TripResponse t = repository.getTripByIdIfPresent(saved);
                        if (t != null && isTripUsableStatus(t.status)) {
                            restored = t;
                        } else {
                            sessionManager.clearDriverActiveTripId();
                        }
                    }
                }

                if (restored != null) {
                    TripResponse trip = restored;
                    runOnUiThread(() -> applyRestoredDriverTrip(trip));
                }
            } catch (IOException ignored) {
                // Sin red o sin viaje: se deja estado actual
            }
        });
    }

    private static boolean isTripUsableStatus(String status) {
        if (status == null) {
            return false;
        }
        String s = status.trim().toLowerCase(Locale.US);
        if (s.equals("cancelado") || s.equals("cancelled") || s.equals("2")) {
            return false;
        }
        if (s.equals("finalizado") || s.equals("finished") || s.equals("4")) {
            return false;
        }
        return true;
    }

    private void applyRestoredDriverTrip(TripResponse trip) {
        activeTripId = trip.id;
        lastTripStatusLabel = trip.status;
        activeTripAvailableSeats = trip.availableSeats;
        sessionManager.saveDriverActiveTripId(trip.id);
        syncTripStateToViewModel();
        updateStatusText();
        updateRouteTimeText();
        refreshButtons();
        updateDrawerDriverMenuVisibility();
    }

    private void applyRoleAccess() {
        int driverVisibility = isDriverUser ? View.VISIBLE : View.GONE;
        createTripButton.setVisibility(driverVisibility);
        cancelTripButton.setVisibility(driverVisibility);
        markBoardedButton.setVisibility(driverVisibility);
        startTripButton.setVisibility(driverVisibility);
        finishTripButton.setVisibility(driverVisibility);

        viewBoardedPassengersButton.setVisibility(driverVisibility);
        updateDrawerDriverMenuVisibility();
    }

    private void updateDrawerDriverMenuVisibility() {
        if (navigationView == null) {
            return;
        }
        MenuItem requestsItem = navigationView.getMenu().findItem(R.id.nav_driver_passenger_requests);
        if (requestsItem != null) {
            requestsItem.setVisible(isDriverUser);
        }
    }

    private void setupDrawer() {
        refreshDrawerUserInfo();

        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_edit_profile) {
                    startActivity(new Intent(this, ProfileActivity.class));
                } else if (id == R.id.nav_favorites) {
                    Intent fi = new Intent(this, FavoritePlacesActivity.class);
                    fi.putExtra(FavoritePlacesActivity.EXTRA_PICK_MODE, false);
                    startActivity(fi);
                } else if (id == R.id.nav_history) {
                    startActivity(new Intent(this, TripHistoryActivity.class));
                } else if (id == R.id.nav_driver_passenger_requests) {
                    openPassengerRequestsScreen(true);
                } else if (id == R.id.nav_logout) {
                    logout();
                }

                if (drawerLayout != null) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                }
                return true;
            });
        }
    }

    private void refreshDrawerUserInfo() {
        if (drawerUserTitle != null) {
            drawerUserTitle.setText(sessionManager.getFullName());
        }
        if (drawerUserSubtitle != null) {
            drawerUserSubtitle.setText(sessionManager.getEmail());
        }
    }

    private void logout() {
        new Thread(() -> {
            try {
                userAccessUseCase.logout();
            } catch (Exception ignored) {
            }
        }).start();

        sessionManager.clearSession();
        Toast.makeText(this, R.string.logout_done, Toast.LENGTH_SHORT).show();
        navigateToLogin();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupBottomSheetBehavior() {
        if (controlPanel != null) {
            bottomSheetBehavior = BottomSheetBehavior.from(controlPanel);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            bottomSheetBehavior.setDraggable(true);
            bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
                @Override
                public void onStateChanged(View bottomSheet, int newState) {
                    switch (newState) {
                        case BottomSheetBehavior.STATE_COLLAPSED:
                            break;
                        case BottomSheetBehavior.STATE_EXPANDED:
                            break;
                        case BottomSheetBehavior.STATE_HIDDEN:
                            break;
                    }
                }

                @Override
                public void onSlide(View bottomSheet, float slideOffset) {
                    // El slide offset va de 0 (colapsado) a 1 (expandido)
                }
            });
        }
    }

    private void toggleBottomSheet() {
        if (bottomSheetBehavior != null) {
            if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            } else {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        }
    }

    private void checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            setupLocationComponent();
        } else {
            locationPermissionRequest.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void setupMapClickListener() {
        GesturesUtils.getGestures(mapView).addOnMapClickListener(point -> {
            runOnUiThread(() -> handleMapClick(point));
            return true;
        });
    }

    private void setupLocationComponent() {
        locationComponentPlugin = LocationComponentUtils.getLocationComponent(mapView);
        locationComponentPlugin.setEnabled(true);
        locationComponentPlugin.addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener);
    }

    private void initializeMapAnnotations() {
        if (mapView == null || pointAnnotationManager != null) {
            return;
        }

        AnnotationPlugin annotationPlugin = (AnnotationPlugin) mapView.getPlugin(Plugin.MAPBOX_ANNOTATION_PLUGIN_ID);
        if (annotationPlugin == null) {
            return;
        }

        pointAnnotationManager = com.mapbox.maps.plugin.annotation.generated.PointAnnotationManagerKt.createPointAnnotationManager(annotationPlugin, null);
        polylineAnnotationManager = com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManagerKt.createPolylineAnnotationManager(annotationPlugin, null);
        originMarkerBitmap = createMarkerBitmap(Color.parseColor("#1E88E5"));
        destinationMarkerBitmap = createMarkerBitmap(Color.parseColor("#E53935"));
        updateMapMarkers();
    }

    private Bitmap createMarkerBitmap(int fillColor) {
        int size = 96;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(fillColor);

        Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(7f);
        strokePaint.setColor(Color.WHITE);

        Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowPaint.setStyle(Paint.Style.FILL);
        shadowPaint.setColor(Color.parseColor("#33000000"));

        float centerX = size / 2f;
        float circleCenterY = size * 0.36f;
        float radius = size * 0.18f;

        Path tail = new Path();
        tail.moveTo(centerX, size * 0.92f);
        tail.lineTo(size * 0.28f, size * 0.48f);
        tail.lineTo(size * 0.72f, size * 0.48f);
        tail.close();

        canvas.drawPath(tail, shadowPaint);
        canvas.drawCircle(centerX, circleCenterY, radius, fillPaint);
        canvas.drawCircle(centerX, circleCenterY, radius, strokePaint);

        Paint innerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        innerPaint.setStyle(Paint.Style.FILL);
        innerPaint.setColor(Color.WHITE);
        canvas.drawCircle(centerX, circleCenterY, radius * 0.42f, innerPaint);

        return bitmap;
    }

    private void updateMapMarkers() {
        if (pointAnnotationManager == null) {
            return;
        }

        if (selectedOrigin != null) {
            if (originAnnotation == null) {
                PointAnnotationOptions options = new PointAnnotationOptions()
                        .withPoint(selectedOrigin)
                        .withIconImage(originMarkerBitmap);
                originAnnotation = pointAnnotationManager.create(options);
            } else {
                originAnnotation.setPoint(selectedOrigin);
                pointAnnotationManager.update(originAnnotation);
            }
        } else if (originAnnotation != null) {
            pointAnnotationManager.delete(originAnnotation);
            originAnnotation = null;
        }

        if (selectedDestination != null) {
            if (destinationAnnotation == null) {
                PointAnnotationOptions options = new PointAnnotationOptions()
                        .withPoint(selectedDestination)
                        .withIconImage(destinationMarkerBitmap);
                destinationAnnotation = pointAnnotationManager.create(options);
            } else {
                destinationAnnotation.setPoint(selectedDestination);
                pointAnnotationManager.update(destinationAnnotation);
            }
        } else if (destinationAnnotation != null) {
            pointAnnotationManager.delete(destinationAnnotation);
            destinationAnnotation = null;
        }
    }

    private void handleMapClick(Point point) {
        if (selectionMode == SelectionMode.NONE) {
            Toast.makeText(this, R.string.toast_select_mode_prompt, Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectionMode == SelectionMode.ORIGIN) {
            selectedOrigin = point;
            Toast.makeText(this, R.string.button_select_origin, Toast.LENGTH_SHORT).show();
        } else if (selectionMode == SelectionMode.DESTINATION) {
            selectedDestination = point;
            Toast.makeText(this, R.string.button_select_destination, Toast.LENGTH_SHORT).show();
        }

        syncSelectionStateToViewModel();

        updateCoordinateLabels();
        updateMapMarkers();
        refreshButtons();
        setSelectionMode(SelectionMode.NONE);
    }

    private void createTrip() {
        setProgressVisible(true);
        createTripButton.setEnabled(false);

        final Point origin = selectedOrigin;
        final Point destination = selectedDestination;
        mainViewModel.createTrip(origin, destination, new MainViewModel.ResultCallback<>() {
            @Override
            public void onSuccess(CreateTripResult result) {
                TripResponse response = result.trip;
                activeTripId = response.id;
                lastTripStatusLabel = response.status;
                activeTripAvailableSeats = response.availableSeats;
                lastRouteTimeLabel = buildEstimatedTimeLabel(result.route.distanceMeters);
                sessionManager.saveDriverActiveTripId(response.id);
                syncTripStateToViewModel();

                if (result.route.points != null && !result.route.points.isEmpty()) {
                    drawRoute(result.route.points);
                }
                Toast.makeText(MainActivity.this, R.string.toast_trip_created, Toast.LENGTH_SHORT).show();
                updateStatusText();
                updateRouteTimeText();
                setProgressVisible(false);
                refreshButtons();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(MainActivity.this, R.string.toast_network_error, Toast.LENGTH_SHORT).show();
                setProgressVisible(false);
                refreshButtons();
            }
        });
    }

    private void cancelTrip() {
        setProgressVisible(true);
        cancelTripButton.setEnabled(false);
        final String tripId = activeTripId;

        mainViewModel.cancelTrip(tripId, new MainViewModel.ResultCallback<>() {
            @Override
            public void onSuccess(TripResponse response) {
                activeTripId = null;
                lastTripStatusLabel = response.status;
                activeTripAvailableSeats = 0;
                lastRouteTimeLabel = null;
                sessionManager.clearDriverActiveTripId();
                syncTripStateToViewModel();
                Toast.makeText(MainActivity.this, R.string.toast_trip_cancelled, Toast.LENGTH_SHORT).show();
                selectedOrigin = null;
                selectedDestination = null;
                syncSelectionStateToViewModel();
                clearRoute();
                setSelectionMode(SelectionMode.NONE);
                updateCoordinateLabels();
                updateMapMarkers();
                updateStatusText();
                updateRouteTimeText();
                setProgressVisible(false);
                refreshButtons();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(MainActivity.this, R.string.toast_network_error, Toast.LENGTH_SHORT).show();
                setProgressVisible(false);
                refreshButtons();
            }
        });
    }

    private String buildEstimatedTimeLabel(double distanceMeters) {
        if (distanceMeters <= 0) {
            return getString(R.string.route_time_idle);
        }

        double estimatedMinutes = distanceMeters / 1000.0;
        return getString(R.string.route_time_format, estimatedMinutes);
    }

    private void drawRoute(List<Point> routePoints) {
        if (polylineAnnotationManager == null || routePoints == null || routePoints.size() < 2) {
            return;
        }

        clearRoute();

        com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions options = new com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions()
                .withPoints(routePoints)
                .withLineColor("#1E88E5")
                .withLineWidth(7.0)
                .withLineOpacity(0.9);

        routeAnnotation = polylineAnnotationManager.create(options);
    }

    private void clearRoute() {
        if (polylineAnnotationManager != null && routeAnnotation != null) {
            polylineAnnotationManager.delete(routeAnnotation);
            routeAnnotation = null;
        }
    }

    private void setSelectionMode(SelectionMode mode) {
        selectionMode = mode;
        int messageRes;
        if (mode == SelectionMode.ORIGIN) {
            messageRes = R.string.selection_instruction_origin;
        } else if (mode == SelectionMode.DESTINATION) {
            messageRes = R.string.selection_instruction_destination;
        } else {
            messageRes = R.string.selection_instruction_idle;
        }
        selectionInstruction.setText(messageRes);
    }

    private void updateCoordinateLabels() {
        if (selectedOrigin == null) {
            originText.setText(R.string.origin_placeholder);
        } else {
            originText.setText(formatCoordinate(selectedOrigin));
        }

        if (selectedDestination == null) {
            destinationText.setText(R.string.destination_placeholder);
        } else {
            destinationText.setText(formatCoordinate(selectedDestination));
        }
    }

    private String formatCoordinate(Point point) {
        return getString(R.string.coordinate_value_format,
                point.latitude(),
                point.longitude());
    }

    private void refreshButtons() {
        boolean canCreate = isDriverUser && selectedOrigin != null && selectedDestination != null && activeTripId == null;
        createTripButton.setEnabled(canCreate);
        cancelTripButton.setEnabled(isDriverUser && activeTripId != null);
        reserveTripButton.setEnabled(activeTripId != null);
        findDriverButton.setEnabled(!isDriverUser && selectedDestination != null);
        cancelPassengerReservationButton.setEnabled(activeTripId != null);
        viewBoardedPassengersButton.setEnabled(isDriverUser && activeTripId != null);
        markBoardedButton.setEnabled(isDriverUser && activeTripId != null);
        startTripButton.setEnabled(isDriverUser && activeTripId != null && isTripReadyToStart());
        finishTripButton.setEnabled(isDriverUser && activeTripId != null && isTripInProgress());
        if (saveFavoriteButton != null) {
            boolean canSaveFavorite = selectedOrigin != null || selectedDestination != null;
            saveFavoriteButton.setEnabled(canSaveFavorite);
        }
        updateDrawerDriverMenuVisibility();
    }

    private void consumeApplyFavoriteIntent(Intent intent) {
        if (intent == null || !intent.hasExtra(EXTRA_APPLY_FAVORITE_KIND)) {
            return;
        }

        String kind = intent.getStringExtra(EXTRA_APPLY_FAVORITE_KIND);
        String favId = intent.getStringExtra(EXTRA_APPLY_FAVORITE_ID);
        double oLat = intent.getDoubleExtra(EXTRA_APPLY_ORIGIN_LAT, Double.NaN);
        double oLng = intent.getDoubleExtra(EXTRA_APPLY_ORIGIN_LNG, Double.NaN);
        if (Double.isNaN(oLat) || Double.isNaN(oLng)) {
            clearFavoriteApplyExtras(intent);
            return;
        }

        String normalizedKind = kind == null ? "" : kind.trim().toLowerCase(Locale.US);
        if ("route".equals(normalizedKind)) {
            double dLat = intent.getDoubleExtra(EXTRA_APPLY_DEST_LAT, Double.NaN);
            double dLng = intent.getDoubleExtra(EXTRA_APPLY_DEST_LNG, Double.NaN);
            if (!Double.isNaN(dLat) && !Double.isNaN(dLng)) {
                selectedOrigin = Point.fromLngLat(oLng, oLat);
                selectedDestination = Point.fromLngLat(dLng, dLat);
            }
        } else {
            boolean asOrigin = intent.getBooleanExtra(EXTRA_APPLY_PLACE_AS_ORIGIN, false);
            Point p = Point.fromLngLat(oLng, oLat);
            if (asOrigin) {
                selectedOrigin = p;
            } else {
                selectedDestination = p;
            }
        }

        clearRoute();
        syncSelectionStateToViewModel();
        updateCoordinateLabels();
        updateMapMarkers();
        refreshButtons();
        setSelectionMode(SelectionMode.NONE);
        Toast.makeText(this, R.string.favorite_applied_toast, Toast.LENGTH_SHORT).show();

        clearFavoriteApplyExtras(intent);

        if (favId != null && !favId.isBlank() && sessionManager.hasActiveSession()) {
            String uid = sessionManager.getUserId();
            backgroundExecutor.execute(() -> {
                try {
                    new FavoritesRemoteDataSource(ApiBaseUrlProvider.get(MainActivity.this)).recordUse(uid, favId);
                } catch (IOException ignored) {
                }
            });
        }
    }

    private static void clearFavoriteApplyExtras(Intent intent) {
        intent.removeExtra(EXTRA_APPLY_FAVORITE_KIND);
        intent.removeExtra(EXTRA_APPLY_FAVORITE_ID);
        intent.removeExtra(EXTRA_APPLY_ORIGIN_LAT);
        intent.removeExtra(EXTRA_APPLY_ORIGIN_LNG);
        intent.removeExtra(EXTRA_APPLY_DEST_LAT);
        intent.removeExtra(EXTRA_APPLY_DEST_LNG);
        intent.removeExtra(EXTRA_APPLY_PLACE_AS_ORIGIN);
    }

    private void openSaveFavoriteDialog() {
        boolean hasO = selectedOrigin != null;
        boolean hasD = selectedDestination != null;
        if (!hasO && !hasD) {
            Toast.makeText(this, R.string.favorite_save_need_point, Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = sessionManager.getUserId();
        if (userId == null || userId.isBlank()) {
            Toast.makeText(this, R.string.favorites_error_session, Toast.LENGTH_SHORT).show();
            return;
        }

        if (hasO && hasD) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.favorite_save_pick_kind)
                    .setItems(new CharSequence[]{
                            getString(R.string.favorite_save_as_route),
                            getString(R.string.favorite_save_place_origin),
                            getString(R.string.favorite_save_place_destination)
                    }, (d, which) -> promptFavoriteTitleAndSubmit(which))
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        } else {
            promptFavoriteTitleAndSubmit(hasO ? 1 : 2);
        }
    }

    private void promptFavoriteTitleAndSubmit(int choiceWhenBothPresent) {
        EditText input = new EditText(this);
        input.setHint(R.string.favorite_save_hint);
        new AlertDialog.Builder(this)
                .setTitle(R.string.favorite_save_dialog_title)
                .setView(input)
                .setPositiveButton(R.string.favorite_save_confirm, (dialog, which) -> {
                    String title = String.valueOf(input.getText()).trim();
                    if (title.isEmpty()) {
                        Toast.makeText(MainActivity.this, R.string.favorite_save_validation_title, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String userId = sessionManager.getUserId();
                    if (userId == null || userId.isBlank()) {
                        return;
                    }
                    if (choiceWhenBothPresent == 0) {
                        if (selectedOrigin != null && selectedDestination != null) {
                            submitFavoriteRoute(userId, title, selectedOrigin, selectedDestination);
                        }
                    } else if (choiceWhenBothPresent == 1 && selectedOrigin != null) {
                        submitFavoritePlace(userId, title, selectedOrigin);
                    } else if (choiceWhenBothPresent == 2 && selectedDestination != null) {
                        submitFavoritePlace(userId, title, selectedDestination);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void submitFavoriteRoute(String userId, String title, Point origin, Point dest) {
        setProgressVisible(true);
        backgroundExecutor.execute(() -> {
            try {
                new FavoritesRemoteDataSource(ApiBaseUrlProvider.get(MainActivity.this)).createFavorite(
                        userId,
                        "route",
                        title,
                        origin.latitude(),
                        origin.longitude(),
                        dest.latitude(),
                        dest.longitude()
                );
                runOnUiThread(() -> {
                    setProgressVisible(false);
                    Toast.makeText(MainActivity.this, R.string.favorite_save_success, Toast.LENGTH_SHORT).show();
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    setProgressVisible(false);
                    Toast.makeText(MainActivity.this, R.string.favorite_save_error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void submitFavoritePlace(String userId, String title, Point place) {
        setProgressVisible(true);
        backgroundExecutor.execute(() -> {
            try {
                new FavoritesRemoteDataSource(ApiBaseUrlProvider.get(MainActivity.this)).createFavorite(
                        userId,
                        "place",
                        title,
                        place.latitude(),
                        place.longitude(),
                        null,
                        null
                );
                runOnUiThread(() -> {
                    setProgressVisible(false);
                    Toast.makeText(MainActivity.this, R.string.favorite_save_success, Toast.LENGTH_SHORT).show();
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    setProgressVisible(false);
                    Toast.makeText(MainActivity.this, R.string.favorite_save_error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private boolean isTripReadyToStart() {
        if (lastTripStatusLabel == null) return false;
        var normalized = lastTripStatusLabel.trim().toLowerCase();
        return normalized.equals("listo") || normalized.equals("ready") || normalized.equals("1");
    }

    private boolean isTripInProgress() {
        if (lastTripStatusLabel == null) return false;
        var normalized = lastTripStatusLabel.trim().toLowerCase();
        return normalized.equals("en curso")
                || normalized.equals("en_curso")
                || normalized.equals("inprogress")
                || normalized.equals("3");
    }

    private void startTrip() {
        if (activeTripId == null) return;

        setProgressVisible(true);
        startTripButton.setEnabled(false);

        final String tripId = activeTripId;
        final Point driverPosition = currentDriverPosition;

        mainViewModel.startTrip(tripId, driverPosition, new MainViewModel.ResultCallback<>() {
            @Override
            public void onSuccess(TripResponse tripResponse) {
                Toast.makeText(MainActivity.this, R.string.toast_trip_started, Toast.LENGTH_SHORT).show();
                lastTripStatusLabel = tripResponse.status;
                activeTripAvailableSeats = tripResponse.availableSeats;
                syncTripStateToViewModel();
                updateStatusText();
                setProgressVisible(false);
                refreshButtons();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(MainActivity.this, R.string.toast_trip_start_failed, Toast.LENGTH_SHORT).show();
                setProgressVisible(false);
                refreshButtons();
            }
        });
    }

    private void finishTrip() {
        if (activeTripId == null) return;

        setProgressVisible(true);
        finishTripButton.setEnabled(false);

        final String tripId = activeTripId;

        mainViewModel.finishTrip(tripId, new MainViewModel.ResultCallback<>() {
            @Override
            public void onSuccess(TripResponse tripResponse) {
                Toast.makeText(MainActivity.this, R.string.toast_trip_finished, Toast.LENGTH_SHORT).show();
                activeTripId = null;
                lastTripStatusLabel = tripResponse.status;
                activeTripAvailableSeats = tripResponse.availableSeats;
                lastRouteTimeLabel = null;
                sessionManager.clearDriverActiveTripId();
                syncTripStateToViewModel();
                updateStatusText();
                updateRouteTimeText();
                setProgressVisible(false);
                refreshButtons();
                updateDrawerDriverMenuVisibility();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(MainActivity.this, R.string.toast_trip_finish_failed, Toast.LENGTH_SHORT).show();
                setProgressVisible(false);
                refreshButtons();
            }
        });
    }

    private void updateStatusText() {
        if (activeTripId == null) {
            statusText.setText(R.string.trip_status_idle);
        } else {
            String suffix = lastTripStatusLabel == null ? "" : " · " + lastTripStatusLabel;
            statusText.setText(getString(R.string.trip_status_with_id, activeTripId) + suffix + " · Cupos: " + activeTripAvailableSeats);
        }
    }

    private void updateRouteTimeText() {
        if (routeTimeText == null) {
            return;
        }

        if (lastRouteTimeLabel == null || activeTripId == null) {
            routeTimeText.setText(R.string.route_time_idle);
        } else {
            routeTimeText.setText(lastRouteTimeLabel);
        }
    }

    private void setProgressVisible(boolean show) {
        if (loadingIndicator != null) {
            loadingIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        consumeApplyFavoriteIntent(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mapView != null) {
            mapView.onStart();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mapView != null) {
            mapView.onStop();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) {
            mapView.onLowMemory();
        }
    }

    @Override
    protected void onDestroy() {
        clearRoute();
        if (pointAnnotationManager != null) {
            pointAnnotationManager.deleteAll();
            pointAnnotationManager = null;
        }
        if (polylineAnnotationManager != null) {
            polylineAnnotationManager.deleteAll();
            polylineAnnotationManager = null;
        }
        if (locationComponentPlugin != null) {
            locationComponentPlugin.removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener);
        }
        if (mapView != null) {
            mapView.onDestroy();
        }
        backgroundExecutor.shutdownNow();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        isDriverUser = sessionManager.isDriver();
        refreshDrawerUserInfo();
        applyRoleAccess();
        refreshButtons();
        maybeRestoreDriverActiveTripAsync();
    }

    private void reserveTrip() {
        if (activeTripId == null) return;
        if (activeTripAvailableSeats <= 0) {
            Toast.makeText(this, "No quedan cupos disponibles", Toast.LENGTH_SHORT).show();
            return;
        }

        final EditText input = new EditText(this);
        input.setHint(R.string.dialog_message_reserve);
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_title_reserve)
                .setView(input)
                .setPositiveButton(R.string.dialog_button_confirm, (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) name = "Pasajero " + (int)(Math.random() * 100);
                    submitReservation(name);
                })
                .setNegativeButton(R.string.dialog_button_cancel, null)
                .show();
    }

    private void submitReservation(String passengerName) {
        setProgressVisible(true);
        mainViewModel.createReservation(activeTripId, passengerName, new MainViewModel.SimpleCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(MainActivity.this, R.string.toast_reservation_created, Toast.LENGTH_SHORT).show();
                activeTripAvailableSeats--;
                syncTripStateToViewModel();
                updateStatusText();
                setProgressVisible(false);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(MainActivity.this, R.string.toast_reservation_failed, Toast.LENGTH_SHORT).show();
                setProgressVisible(false);
            }
        });
    }

    private void cancelPassengerReservation() {
        if (activeTripId == null) return;

        final EditText input = new EditText(this);
        input.setHint("Nombre");
        new AlertDialog.Builder(this)
                .setTitle("Cancelar Reserva")
                .setMessage("Ingresa tu nombre para buscar y cancelar tu reserva:")
                .setView(input)
                .setPositiveButton(R.string.dialog_button_confirm, (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        findAndCancelPassengerReservation(name);
                    } else {
                        Toast.makeText(this, "Nombre inválido", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.dialog_button_cancel, null)
                .show();
    }

    private void findAndCancelPassengerReservation(String passengerName) {
        setProgressVisible(true);
        mainViewModel.getReservations(activeTripId, new MainViewModel.ResultCallback<>() {
            @Override
            public void onSuccess(List<ReservationResponse> reservations) {
                String targetReservationId = null;
                for (ReservationResponse reservation : reservations) {
                    if (reservation.passengerName.equalsIgnoreCase(passengerName)) {
                        targetReservationId = reservation.id;
                        break;
                    }
                }

                if (targetReservationId != null) {
                    executeCancelReservation(targetReservationId);
                } else {
                    Toast.makeText(MainActivity.this, "No se encontró reserva para: " + passengerName, Toast.LENGTH_SHORT).show();
                    setProgressVisible(false);
                }
            }

            @Override
            public void onError(String message) {
                Toast.makeText(MainActivity.this, R.string.toast_network_error, Toast.LENGTH_SHORT).show();
                setProgressVisible(false);
            }
        });
    }

    /**
     * @param showToastIfNoTrip si es true (p. ej. desde el menú lateral) y no hay viaje activo, se muestra aviso.
     */
    private void openPassengerRequestsScreen(boolean showToastIfNoTrip) {
        if (activeTripId == null) {
            if (showToastIfNoTrip) {
                Toast.makeText(this, R.string.drawer_driver_requests_need_trip, Toast.LENGTH_SHORT).show();
            }
            return;
        }
        Intent intent = new Intent(this, DriverPassengerRequestsActivity.class);
        intent.putExtra(DriverPassengerRequestsActivity.EXTRA_TRIP_ID, activeTripId);
        startActivity(intent);
    }

    private void showManualStatusOptions(ReservationResponse reservation, boolean refreshBoardedList) {
        String[] statuses = new String[]{
                getString(R.string.manual_status_active),
                getString(R.string.manual_status_boarded),
                getString(R.string.manual_status_cancelled)
        };

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_title_manual_status) + ": " + reservation.passengerName)
                .setItems(statuses, (dialog, which) -> {
                    String targetStatus;
                    if (which == 0) {
                        targetStatus = "Active";
                    } else if (which == 1) {
                        targetStatus = "Boarded";
                    } else {
                        targetStatus = "Cancelled";
                    }
                    updatePassengerStatusManual(reservation, targetStatus, refreshBoardedList);
                })
                .setNegativeButton(R.string.dialog_button_cancel, null)
                .show();
    }

    private void markPassengerBoardedByName() {
        if (activeTripId == null) return;

        final EditText input = new EditText(this);
        input.setHint(R.string.dialog_message_mark_boarded);

        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_title_mark_boarded)
                .setMessage(R.string.dialog_message_mark_boarded)
                .setView(input)
                .setPositiveButton(R.string.dialog_button_confirm, (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, "Nombre inválido", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    submitBoardingByName(name);
                })
                .setNegativeButton(R.string.dialog_button_cancel, null)
                .show();
    }

    private void submitBoardingByName(String passengerName) {
        setProgressVisible(true);
        mainViewModel.markBoardedByName(activeTripId, passengerName, new MainViewModel.SimpleCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(MainActivity.this, R.string.toast_boarding_confirmed, Toast.LENGTH_SHORT).show();
                openPassengerRequestsScreen(false);
                viewBoardedPassengers();
                setProgressVisible(false);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(MainActivity.this, R.string.toast_boarding_failed, Toast.LENGTH_SHORT).show();
                setProgressVisible(false);
            }
        });
    }

    private void updatePassengerStatusManual(ReservationResponse reservation, String targetStatus, boolean refreshBoardedList) {
        setProgressVisible(true);
        mainViewModel.updateReservationStatus(activeTripId, reservation.id, targetStatus, new MainViewModel.SimpleCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(MainActivity.this, R.string.toast_manual_status_updated, Toast.LENGTH_SHORT).show();
                if (refreshBoardedList) {
                    viewBoardedPassengers();
                } else {
                    openPassengerRequestsScreen(false);
                }
                setProgressVisible(false);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(MainActivity.this, R.string.toast_manual_status_failed, Toast.LENGTH_SHORT).show();
                setProgressVisible(false);
            }
        });
    }

    private void viewBoardedPassengers() {
        if (activeTripId == null) return;
        setProgressVisible(true);
        mainViewModel.getBoardedPassengers(activeTripId, new MainViewModel.ResultCallback<>() {
            @Override
            public void onSuccess(List<ReservationResponse> boarded) {
                showBoardedPassengersDialog(boarded);
                setProgressVisible(false);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(MainActivity.this, R.string.toast_network_error, Toast.LENGTH_SHORT).show();
                setProgressVisible(false);
            }
        });
    }

    private void showBoardedPassengersDialog(List<ReservationResponse> boardedPassengers) {
        if (boardedPassengers.isEmpty()) {
            Toast.makeText(this, R.string.toast_no_boarded_passengers, Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayAdapter<ReservationResponse> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, boardedPassengers);
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_title_boarded_passengers)
                .setAdapter(adapter, (dialog, which) -> {
                    ReservationResponse selected = boardedPassengers.get(which);
                    showManualStatusOptions(selected, true);
                })
                .setNegativeButton(R.string.dialog_button_close, null)
                .show();
    }

    private void confirmCancelReservation(ReservationResponse reservation) {
        new AlertDialog.Builder(this)
                .setTitle("Cancelar Reserva")
                .setMessage("¿Deseas cancelar la reserva de " + reservation.passengerName + "?")
                .setPositiveButton("Sí, Cancelar", (dialog, which) -> executeCancelReservation(reservation.id))
                .setNegativeButton("No", null)
                .show();
    }

    private void executeCancelReservation(String reservationId) {
        setProgressVisible(true);
        mainViewModel.cancelReservation(reservationId, new MainViewModel.SimpleCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(MainActivity.this, R.string.toast_reservation_cancelled, Toast.LENGTH_SHORT).show();
                activeTripAvailableSeats++;
                syncTripStateToViewModel();
                updateStatusText();
                setProgressVisible(false);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(MainActivity.this, R.string.toast_reservation_failed, Toast.LENGTH_SHORT).show();
                setProgressVisible(false);
            }
        });
    }

    private void openDriverMatchScreen() {
        if (selectedDestination == null) {
            Toast.makeText(this, R.string.toast_destination_required, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, DriverMatchActivity.class);
        intent.putExtra(DriverMatchActivity.EXTRA_DESTINATION_LABEL, formatCoordinate(selectedDestination));
        intent.putExtra(DriverMatchActivity.EXTRA_REF_LATITUDE, selectedDestination.latitude());
        intent.putExtra(DriverMatchActivity.EXTRA_REF_LONGITUDE, selectedDestination.longitude());
        driverMatchLauncher.launch(intent);
    }

}