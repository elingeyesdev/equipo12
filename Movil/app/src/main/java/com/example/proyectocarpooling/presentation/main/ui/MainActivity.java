package com.example.proyectocarpooling.presentation.main.ui;

import com.example.proyectocarpooling.presentation.BaseActivity;
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
import android.util.Log;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.view.MenuItem;
import android.widget.Toast;
import android.app.AlertDialog;
import android.widget.EditText;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import org.json.JSONObject;

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

import com.example.proyectocarpooling.CarPoolingApplication;
import com.example.proyectocarpooling.R;
import com.example.proyectocarpooling.data.local.ApiBaseUrlProvider;
import com.example.proyectocarpooling.data.local.SessionManager;
import com.example.proyectocarpooling.data.model.ReservationResponse;
import com.example.proyectocarpooling.data.model.SafeZoneItem;
import com.example.proyectocarpooling.data.model.TripResponse;
import com.example.proyectocarpooling.domain.model.CreateTripResult;
import com.example.proyectocarpooling.domain.repository.TripRepository;
import com.example.proyectocarpooling.domain.usecase.user.UserAccessUseCase;
import com.example.proyectocarpooling.presentation.auth.ui.LoginActivity;
import com.example.proyectocarpooling.presentation.account.ui.AccountOverviewActivity;
import com.example.proyectocarpooling.presentation.driver.ui.DriverPassengerRequestsActivity;
import com.example.proyectocarpooling.presentation.favorites.ui.FavoritePlacesActivity;
import com.example.proyectocarpooling.presentation.help.ui.HelpActivity;
import com.example.proyectocarpooling.presentation.support.ui.SupportActivity;
import com.example.proyectocarpooling.presentation.history.ui.TripHistoryActivity;
import com.example.proyectocarpooling.presentation.search.ui.SearchTripActivity;
import com.example.proyectocarpooling.presentation.match.ui.DriverMatchActivity;
// MainViewModel now in same package
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends BaseActivity {

    public static boolean sReservationCompletedFromBanner = false;
    public static Intent sReservationResultData = null;

    public static final String EXTRA_APPLY_FAVORITE_ID = "apply_favorite_id";
    public static final String EXTRA_APPLY_FAVORITE_KIND = "apply_favorite_kind";
    public static final String EXTRA_APPLY_ORIGIN_LAT = "apply_origin_lat";
    public static final String EXTRA_APPLY_ORIGIN_LNG = "apply_origin_lng";
    public static final String EXTRA_APPLY_DEST_LAT = "apply_dest_lat";
    public static final String EXTRA_APPLY_DEST_LNG = "apply_dest_lng";
    public static final String EXTRA_APPLY_PLACE_AS_ORIGIN = "apply_place_as_origin";
    public static final String EXTRA_HISTORY_ROUTE_PREVIEW = "history_route_preview";
    /** Contexto de la vista previa de ruta: {@link #ROUTE_PREVIEW_CONTEXT_DRIVER_MATCH}. */
    public static final String EXTRA_ROUTE_PREVIEW_CONTEXT = "route_preview_context";
    public static final String EXTRA_ROUTE_PREVIEW_DRIVER_NAME = "route_preview_driver_name";
    public static final String ROUTE_PREVIEW_CONTEXT_DRIVER_MATCH = "driver_match";
    public static final String EXTRA_ROUTE_PREVIEW_TRIP_ID = "route_preview_trip_id";
    public static final String EXTRA_ROUTE_PREVIEW_DRIVER_TRIP_NAME = "route_preview_driver_trip_name";

    private enum SelectionMode { NONE, ORIGIN, DESTINATION }

    private MapView mapView;
    private ProgressBar loadingIndicator;
    private TextView selectionInstruction;
    private TextView originText;
    private TextView destinationText;
    private TextView statusText;
    private TextView routeTimeText;
    private View menuToggleButton;
    private Button createTripButton;
    private Button cancelTripButton;
    private Button findDriverButton;
    private Button viewBoardedPassengersButton;
    private Button markBoardedButton;
    private Button startTripButton;
    private Button finishTripButton;
    private Button boardPassengerCodeButton;
    private LinearLayout controlPanel;
    private LinearLayout tripButtonsRow;
    private LinearLayout driverActionsRow;
    private Button saveFavoriteButton;
    private Button bannerAcceptButton;
    private Button bannerRejectButton;
    private TextView bottomSheetTitle;
    private com.google.android.material.floatingactionbutton.FloatingActionButton chatFloatingButton;

    private boolean isInitialPositionSet = false;
    private Point selectedOrigin;
    private Point selectedDestination;
    private SelectionMode selectionMode = SelectionMode.NONE;
    private String activeTripId;
    private String lastTripStatusLabel;
    private int lastTripStatusId;
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
    private Bitmap safeZoneMarkerBitmap;
    private final List<PointAnnotation> safeZoneAnnotations = new ArrayList<>();
    private final Map<Long, SafeZoneItem> safeZoneByAnnotationId = new HashMap<>();
    private boolean safeZonesLoaded;
    private boolean safeZoneClickListenerRegistered;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private TextView drawerUserTitle;
    private TextView drawerUserEmail;
    private TextView drawerUserInitials;
    private TextView drawerUserRole;
    private TextView drawerUserRating;
    private LinearLayout drawerReservationInfo;
    private TextView drawerReservationDriver;
    private TextView drawerReservationCode;
    private SessionManager sessionManager;
    private UserAccessUseCase userAccessUseCase;
    private MainViewModel mainViewModel;
    private BottomSheetBehavior<LinearLayout> bottomSheetBehavior;
    private boolean isDriverUser;

    private View driverRoutePreviewBanner;
    private TextView driverRoutePreviewBannerBody;
    private String routePreviewTripId;
    private String routePreviewDriverName;

    private boolean hasActivePassengerReservation;
    private String passengerReservedTripId;
    private String passengerBoardingCode;
    private String passengerReservedDriverName;
    private int activeTripPendingCount;

    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();

    private final android.os.Handler pollingHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable pendingReservationPollingRunnable = new Runnable() {
        @Override
        public void run() {
            checkPendingReservations();
            pollingHandler.postDelayed(this, 30_000);
        }
    };
    private Runnable passengerPollingRunnable;

    private final ActivityResultLauncher<Intent> driverMatchLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Intent matchData = result.getData();
                    String tripId = matchData.getStringExtra(DriverMatchActivity.EXTRA_RESULT_TRIP_ID);
                    if (tripId != null && !tripId.isEmpty()) {
                        String driverName = matchData.getStringExtra(DriverMatchActivity.EXTRA_RESULT_DRIVER_NAME);
                        if (driverName == null) driverName = "Conductor";
                        Toast.makeText(MainActivity.this, R.string.toast_passenger_reserved_with_driver, Toast.LENGTH_LONG).show();
                        String code = String.format("%04d", (int)(Math.random() * 10000));
                        sessionManager.savePassengerBookedTrip(tripId, code, driverName);
                        hasActivePassengerReservation = true;
                        passengerReservedTripId = tripId;
                        passengerBoardingCode = code;
                        passengerReservedDriverName = driverName;
                        refreshForPassengerReservation();
                    }
                    double oLat = matchData.getDoubleExtra(DriverMatchActivity.EXTRA_RESULT_ORIGIN_LAT, Double.NaN);
                    double oLng = matchData.getDoubleExtra(DriverMatchActivity.EXTRA_RESULT_ORIGIN_LNG, Double.NaN);
                    double destLat = matchData.getDoubleExtra(DriverMatchActivity.EXTRA_RESULT_DEST_LAT, Double.NaN);
                    double destLng = matchData.getDoubleExtra(DriverMatchActivity.EXTRA_RESULT_DEST_LNG, Double.NaN);
                    if (!Double.isNaN(oLat) && !Double.isNaN(oLng) && !Double.isNaN(destLat) && !Double.isNaN(destLng)) {
                        Intent route = new Intent();
                        route.putExtra(EXTRA_APPLY_FAVORITE_KIND, "route");
                        route.putExtra(EXTRA_APPLY_ORIGIN_LAT, oLat);
                        route.putExtra(EXTRA_APPLY_ORIGIN_LNG, oLng);
                        route.putExtra(EXTRA_APPLY_DEST_LAT, destLat);
                        route.putExtra(EXTRA_APPLY_DEST_LNG, destLng);
                        route.putExtra(EXTRA_HISTORY_ROUTE_PREVIEW, true);
                        consumeApplyFavoriteIntent(route);
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

        CarPoolingApplication app = (CarPoolingApplication) getApplication();
        sessionManager = app.getSessionManager();
        userAccessUseCase = new UserAccessUseCase(app.getUserRepository());
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
        createTripButton = findViewById(R.id.createTripButton);
        cancelTripButton = findViewById(R.id.cancelTripButton);
        findDriverButton = findViewById(R.id.findDriverButton);
        viewBoardedPassengersButton = findViewById(R.id.viewBoardedPassengersButton);
        markBoardedButton = findViewById(R.id.markBoardedButton);
        startTripButton = findViewById(R.id.startTripButton);
        finishTripButton = findViewById(R.id.finishTripButton);
        boardPassengerCodeButton = findViewById(R.id.boardPassengerCodeButton);
        tripButtonsRow = findViewById(R.id.tripButtonsRow);
        driverActionsRow = findViewById(R.id.driverActionsRow);
        saveFavoriteButton = findViewById(R.id.saveFavoriteButton);

        bottomSheetTitle = findViewById(R.id.bottomSheetTitle);

        chatFloatingButton = findViewById(R.id.chatFloatingButton);
        if (chatFloatingButton != null) {
            chatFloatingButton.setOnClickListener(v -> {
                String activeTripIdVal = "";
                String chatTitleVal = "Chat de viaje";

                if (isDriverUser) {
                    activeTripIdVal = activeTripId;
                    chatTitleVal = "Mis Pasajeros";
                } else if (hasActivePassengerReservation) {
                    activeTripIdVal = passengerReservedTripId;
                    chatTitleVal = "Conductor: " + (passengerReservedDriverName != null ? passengerReservedDriverName : "Tu Conductor");
                }

                if (activeTripIdVal != null && !activeTripIdVal.isEmpty()) {
                    Intent intent = new Intent(MainActivity.this, com.example.proyectocarpooling.presentation.chat.ui.ChatActivity.class);
                    intent.putExtra("trip_id", activeTripIdVal);
                    intent.putExtra("chat_title", chatTitleVal);
                    startActivity(intent);
                } else {
                    Toast.makeText(MainActivity.this, "No hay chat activo para este viaje", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (navigationView != null) {
            var header = navigationView.getHeaderView(0);
            drawerUserTitle = header.findViewById(R.id.drawerUserTitle);
            drawerUserEmail = header.findViewById(R.id.drawerUserEmail);
            drawerUserInitials = header.findViewById(R.id.drawerUserInitials);
            drawerUserRole = header.findViewById(R.id.drawerUserRole);
            drawerUserRating = header.findViewById(R.id.drawerUserRating);
            drawerReservationInfo = header.findViewById(R.id.drawerReservationInfo);
            drawerReservationDriver = header.findViewById(R.id.drawerReservationDriver);
            drawerReservationCode = header.findViewById(R.id.drawerReservationCode);

            header.setOnClickListener(v -> {
                startActivity(new Intent(this, AccountOverviewActivity.class));
                if (drawerLayout != null) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                }
            });
        }

        setupBottomSheetBehavior();
        setupDrawer();

        driverRoutePreviewBanner = findViewById(R.id.driverRoutePreviewBanner);
        driverRoutePreviewBannerBody = findViewById(R.id.driverRoutePreviewBannerBody);
        bannerAcceptButton = findViewById(R.id.driverRoutePreviewBannerAccept);
        bannerRejectButton = findViewById(R.id.driverRoutePreviewBannerReject);
        View dismissBanner = findViewById(R.id.driverRoutePreviewBannerDismiss);
        View closeBanner = findViewById(R.id.driverRoutePreviewBannerClose);
        if (dismissBanner != null) {
            dismissBanner.setOnClickListener(v -> hideDriverMatchRouteExplainer());
        }
        if (closeBanner != null) {
            closeBanner.setOnClickListener(v -> hideDriverMatchRouteExplainer());
        }
        if (bannerAcceptButton != null) {
            bannerAcceptButton.setOnClickListener(v -> acceptDriverRouteFromBanner());
        }
        if (bannerRejectButton != null) {
            bannerRejectButton.setOnClickListener(v -> {
                hideDriverMatchRouteExplainer();
                Toast.makeText(this, "Viaje rechazado", Toast.LENGTH_SHORT).show();
                finish();
            });
        }

        menuToggleButton.setOnClickListener(v -> {
            if (drawerLayout != null) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
        originText.setOnClickListener(v -> {
            if (hasActivePassengerReservation && !isDriverUser) {
                Toast.makeText(this, "No puedes cambiar el origen de un viaje reservado", Toast.LENGTH_SHORT).show();
                return;
            }
            if (isDriverUser || activeTripId == null) {
                setSelectionMode(SelectionMode.ORIGIN);
            }
        });
        destinationText.setOnClickListener(v -> {
            if (hasActivePassengerReservation && !isDriverUser) {
                Toast.makeText(this, "No puedes cambiar el destino de un viaje reservado", Toast.LENGTH_SHORT).show();
                return;
            }
            if (isDriverUser || activeTripId == null) {
                setSelectionMode(SelectionMode.DESTINATION);
            }
        });
        findDriverButton.setOnClickListener(v -> {
            if (hasActivePassengerReservation) {
                Toast.makeText(this, "Ya tienes un viaje reservado", Toast.LENGTH_SHORT).show();
                return;
            }
            openDriverMatchScreen();
        });
        viewBoardedPassengersButton.setOnClickListener(v -> viewBoardedPassengers());
        markBoardedButton.setOnClickListener(v -> markPassengerBoardedByName());
        startTripButton.setOnClickListener(v -> startTrip());
        finishTripButton.setOnClickListener(v -> finishTrip());
        boardPassengerCodeButton.setOnClickListener(v -> showBoardPassengerByCodeDialog());
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
            setProgressVisible(true);
            cancelTripButton.setEnabled(false);
            final String tripId = activeTripId;
            backgroundExecutor.execute(() -> {
                try {
                    String apiBase = ApiBaseUrlProvider.get(MainActivity.this);
                    String mapboxToken = getString(R.string.mapbox_access_token);
                    TripRepository repository = ((CarPoolingApplication) getApplication()).getTripRepository();
                    List<ReservationResponse> confirmed = repository.getConfirmedReservations(tripId);
                    int count = confirmed.size();
                    runOnUiThread(() -> {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        setProgressVisible(false);
                        cancelTripButton.setEnabled(true);
                        if (count > 0) {
                            new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("Cancelar viaje")
                                    .setMessage("Hay " + count + " pasajero(s) confirmado(s). ¿Cancelar el viaje de todos modos?")
                                    .setPositiveButton("Si, cancelar", (d, w) -> cancelTrip())
                                    .setNegativeButton("No", null)
                                    .show();
                        } else {
                            cancelTrip();
                        }
                    });
                } catch (IOException e) {
                    runOnUiThread(() -> {
                        setProgressVisible(false);
                        cancelTripButton.setEnabled(true);
                        cancelTrip();
                    });
                }
            });
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
        checkPassengerReservationOnStart();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        consumeApplyFavoriteIntent(intent);
    }

    private void checkPassengerReservationOnStart() {
        if (isDriverUser) {
            return;
        }
        final String userId = sessionManager.getUserId();
        if (userId == null || userId.isEmpty()) {
            return;
        }

        setProgressVisible(true);
        backgroundExecutor.execute(() -> {
            try {
                CarPoolingApplication app = (CarPoolingApplication) getApplication();
                org.json.JSONObject reservation = app.getUserRepository().getActiveReservation(userId);

                if (reservation != null) {
                    final String tripId = reservation.optString("tripId", "");
                    final String reservationId = reservation.optString("reservationId", "");
                    final String boardingCode = reservation.optString("boardingCode", "");
                    final String driverName = reservation.optString("driverName", "");

                    if (!tripId.isEmpty()) {
                        sessionManager.savePassengerBookedTrip(tripId, reservationId, boardingCode, driverName);

                        TripRepository repository = app.getTripRepository();
                        TripResponse trip = repository.getTripByIdIfPresent(tripId);

                        runOnUiThread(() -> {
                            setProgressVisible(false);
                            if (isFinishing() || isDestroyed()) {
                                return;
                            }
                            if (trip != null && isTripUsableStatus(trip.statusLabel)) {
                                hasActivePassengerReservation = true;
                                passengerReservedTripId = trip.id;
                                passengerBoardingCode = boardingCode;
                                passengerReservedDriverName = trip.driverName;
                                if (passengerReservedDriverName == null || passengerReservedDriverName.isEmpty()) {
                                    passengerReservedDriverName = driverName;
                                }
                                selectedOrigin = Point.fromLngLat(trip.originLongitude, trip.originLatitude);
                                selectedDestination = Point.fromLngLat(trip.destinationLongitude, trip.destinationLatitude);
                                syncSelectionStateToViewModel();
                                updateCoordinateLabels();
                                updateMapMarkers();
                                if (selectedOrigin != null && selectedDestination != null) {
                                    fetchAndDrawRoutePreviewAsync(selectedOrigin, selectedDestination);
                                }
                                refreshForPassengerReservation();
                                refreshButtons();
                            } else {
                                final String previouslyBookedTripId = sessionManager.getPassengerBookedTripId();
                                sessionManager.clearPassengerBookedTrip();
                                hasActivePassengerReservation = false;
                                passengerReservedTripId = null;
                                refreshForPassengerReservation();
                                refreshButtons();
                                if (previouslyBookedTripId != null && !previouslyBookedTripId.isEmpty()) {
                                    checkFinishedTripAndPromptRating(previouslyBookedTripId);
                                }
                            }
                        });
                        return;
                    }
                }

                final String previouslyBookedTripId = sessionManager.getPassengerBookedTripId();
                sessionManager.clearPassengerBookedTrip();
                runOnUiThread(() -> {
                    setProgressVisible(false);
                    hasActivePassengerReservation = false;
                    passengerReservedTripId = null;
                    refreshForPassengerReservation();
                    refreshButtons();
                    if (previouslyBookedTripId != null && !previouslyBookedTripId.isEmpty()) {
                        checkFinishedTripAndPromptRating(previouslyBookedTripId);
                    }
                });

            } catch (IOException e) {
                runOnUiThread(() -> {
                    setProgressVisible(false);
                    if (sessionManager.hasPassengerBookedTrip()) {
                        final String bookedTripId = sessionManager.getPassengerBookedTripId();
                        backgroundExecutor.execute(() -> {
                            try {
                                TripRepository repository = ((CarPoolingApplication) getApplication()).getTripRepository();
                                TripResponse trip = repository.getTripByIdIfPresent(bookedTripId);
                                runOnUiThread(() -> {
                                    if (isFinishing() || isDestroyed()) {
                                        return;
                                    }
                                    if (trip != null && isTripUsableStatus(trip.statusLabel)) {
                                        hasActivePassengerReservation = true;
                                        passengerReservedTripId = trip.id;
                                        passengerBoardingCode = sessionManager.getPassengerBoardingCode();
                                        passengerReservedDriverName = trip.driverName;
                                        if (passengerReservedDriverName == null || passengerReservedDriverName.isEmpty()) {
                                            passengerReservedDriverName = sessionManager.getPassengerDriverName();
                                        }
                                        selectedOrigin = Point.fromLngLat(trip.originLongitude, trip.originLatitude);
                                        selectedDestination = Point.fromLngLat(trip.destinationLongitude, trip.destinationLatitude);
                                        syncSelectionStateToViewModel();
                                        updateCoordinateLabels();
                                        updateMapMarkers();
                                        if (selectedOrigin != null && selectedDestination != null) {
                                            fetchAndDrawRoutePreviewAsync(selectedOrigin, selectedDestination);
                                        }
                                        refreshForPassengerReservation();
                                        refreshButtons();
                                    } else {
                                        final String previouslyBookedTripIdCatch = sessionManager.getPassengerBookedTripId();
                                        sessionManager.clearPassengerBookedTrip();
                                        hasActivePassengerReservation = false;
                                        passengerReservedTripId = null;
                                        refreshForPassengerReservation();
                                        refreshButtons();
                                        if (previouslyBookedTripIdCatch != null && !previouslyBookedTripIdCatch.isEmpty()) {
                                            checkFinishedTripAndPromptRating(previouslyBookedTripIdCatch);
                                        }
                                    }
                                });
                            } catch (IOException ignored) {}
                        });
                    } else {
                        refreshForPassengerReservation();
                        refreshButtons();
                    }
                });
            }
        });
    }

    private void refreshForPassengerReservation() {
        if (hasActivePassengerReservation && passengerReservedTripId != null) {
            if (bottomSheetTitle != null) bottomSheetTitle.setText("Viaje reservado");
            findDriverButton.setVisibility(View.GONE);
            if (saveFavoriteButton != null) saveFavoriteButton.setVisibility(View.GONE);
            selectionInstruction.setText("Viaje reservado - Codigo: " + (passengerBoardingCode != null ? passengerBoardingCode : "----"));
            statusText.setText("Conductor: " + (passengerReservedDriverName != null ? passengerReservedDriverName : "Asignado"));
            routeTimeText.setText("Destino confirmado");
            updateDrawerReservationMenu();
            refreshDrawerUserInfo();
            startPassengerPolling();
        } else {
            if (bottomSheetTitle != null) bottomSheetTitle.setText(R.string.bottom_sheet_title);
            findDriverButton.setVisibility(isDriverUser ? View.GONE : View.VISIBLE);
            if (saveFavoriteButton != null) saveFavoriteButton.setVisibility(View.VISIBLE);
            selectionInstruction.setText(R.string.selection_instruction_idle);
            selectionInstruction.setTextColor(getResources().getColor(R.color.carpool_text_secondary, getTheme()));
            updateDrawerReservationMenu();
            refreshDrawerUserInfo();
            stopPassengerPolling();
        }
    }

    private void cancelPassengerReservationAction() {
        if (passengerReservedTripId == null || passengerReservedTripId.isEmpty()) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Cancelar reserva")
                .setMessage("Estas seguro de que deseas cancelar tu reserva para este viaje?")
                .setPositiveButton("Si, cancelar", (d, w) -> {
                    setProgressVisible(true);
                    final String tripId = passengerReservedTripId;
                    final String userId = sessionManager.getUserId();
                    backgroundExecutor.execute(() -> {
                        try {
                            String apiBase = ApiBaseUrlProvider.get(MainActivity.this);
                            String mapboxToken = getString(R.string.mapbox_access_token);
                            TripRepository repository = ((CarPoolingApplication) getApplication()).getTripRepository();

                            // Buscar en pendientes y confirmadas
                            List<ReservationResponse> all = new ArrayList<>();
                            all.addAll(repository.getReservations(tripId));
                            all.addAll(repository.getConfirmedReservations(tripId));

                            String reservationId = null;
                            for (ReservationResponse r : all) {
                                if (r.passengerUserId != null && r.passengerUserId.equals(userId)) {
                                    reservationId = r.id;
                                    break;
                                }
                            }
                            if (reservationId != null) {
                                repository.cancelReservation(reservationId);
                            }
                            runOnUiThread(() -> {
                                setProgressVisible(false);
                                sessionManager.clearPassengerBookedTrip();
                                hasActivePassengerReservation = false;
                                passengerReservedTripId = null;
                                passengerBoardingCode = null;
                                passengerReservedDriverName = null;
                                stopPassengerPolling();
                                selectedOrigin = null;
                                selectedDestination = null;
                                syncSelectionStateToViewModel();
                                clearRoute();
                                updateCoordinateLabels();
                                updateMapMarkers();
                                updateStatusText();
                                updateRouteTimeText();
                                refreshButtons();
                                refreshForPassengerReservation();
                                refreshDrawerUserInfo();
                                Toast.makeText(MainActivity.this, "Reserva cancelada", Toast.LENGTH_SHORT).show();
                            });
                        } catch (IOException e) {
                            runOnUiThread(() -> {
                                setProgressVisible(false);
                                Toast.makeText(MainActivity.this, "Error al cancelar reserva", Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void updateDrawerReservationMenu() {
        if (navigationView == null) {
            return;
        }
        MenuItem reservationItem = navigationView.getMenu().findItem(R.id.nav_my_reservation);
        if (reservationItem != null) {
            reservationItem.setVisible(hasActivePassengerReservation && !isDriverUser);
        }
        MenuItem findDriverItem = navigationView.getMenu().findItem(R.id.nav_find_driver);
        if (findDriverItem != null) {
            findDriverItem.setVisible(!isDriverUser && !hasActivePassengerReservation);
        }
        MenuItem cancelResItem = navigationView.getMenu().findItem(R.id.nav_cancel_reservation);
        if (cancelResItem != null) {
            cancelResItem.setVisible(hasActivePassengerReservation && !isDriverUser);
        }
    }

    private void maybeRestoreDriverActiveTripAsync() {
        if (!isDriverUser) {
            return;
        }
        if (activeTripId != null && !activeTripId.isEmpty() && selectedOrigin != null && selectedDestination != null) {
            // Misma sesión: el viaje ya está en memoria, solo asegurar ruta al mapa si hace falta
            requestDriverRoutePreviewIfOnMap();
            return;
        }
        if (activeTripId != null && !activeTripId.isEmpty()) {
            // ID sin puntos: rehidratar coordenadas desde el servidor
            rehydrateDriverTripFromIdAsync(activeTripId);
            return;
        }

        final String apiBase = ApiBaseUrlProvider.get(this);
        final String mapboxToken = getString(R.string.mapbox_access_token);
        final String userId = sessionManager.getUserId();
        final String fullName = sessionManager.getFullName();

        backgroundExecutor.execute(() -> {
            try {
                TripRepository repository = ((CarPoolingApplication) getApplication()).getTripRepository();
                TripResponse restored = null;

                if (userId != null && !userId.isBlank()) {
                    restored = repository.findActiveTripForDriver(userId, fullName);
                }

                if (restored == null) {
                    String saved = sessionManager.getDriverActiveTripId();
                    if (!saved.isEmpty()) {
                        TripResponse t = repository.getTripByIdIfPresent(saved);
                        if (t != null && isTripUsableStatus(t.statusLabel)) {
                            restored = t;
                        } else {
                            sessionManager.clearDriverActiveTripId();
                        }
                    }
                }

                if (restored != null) {
                    TripResponse trip = restored;
                    runOnUiThread(() -> {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        applyRestoredDriverTrip(trip);
                    });
                }
            } catch (IOException ignored) {
                // Sin red o sin viaje: se deja estado actual
            }
        });
    }

    /** Si hay {@link #activeTripId} en memoria pero aún no hay origen/destino, los pide al API. */
    private void rehydrateDriverTripFromIdAsync(String tripId) {
        if (tripId == null || tripId.isEmpty()) {
            return;
        }
        final String apiBase = ApiBaseUrlProvider.get(this);
        final String mapboxToken = getString(R.string.mapbox_access_token);
        backgroundExecutor.execute(() -> {
            try {
                TripRepository repository = ((CarPoolingApplication) getApplication()).getTripRepository();
                TripResponse t = repository.getTripByIdIfPresent(tripId);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    if (t != null && isTripUsableStatus(t.statusLabel)) {
                        applyRestoredDriverTrip(t);
                    } else {
                        sessionManager.clearDriverActiveTripId();
                        activeTripId = null;
                        lastTripStatusLabel = null;
                        lastTripStatusId = 0;
                        activeTripAvailableSeats = 0;
                        lastRouteTimeLabel = null;
                        syncTripStateToViewModel();
                        selectedOrigin = null;
                        selectedDestination = null;
                        syncSelectionStateToViewModel();
                        clearRoute();
                        setSelectionMode(SelectionMode.NONE);
                        updateCoordinateLabels();
                        updateMapMarkers();
                        updateStatusText();
                        updateRouteTimeText();
                        refreshButtons();
                    }
                });
            } catch (IOException ignored) {
            }
        });
    }

    private static boolean isTripUsableStatus(String status) {
        if (status == null) {
            return false;
        }
        String s = status.trim().toLowerCase(Locale.US);
        if (s.equals("cancelado") || s.equals("cancelled") || s.equals("5")) {
            return false;
        }
        if (s.equals("finalizado") || s.equals("finished") || s.equals("4")) {
            return false;
        }
        return true;
    }

    private void applyRestoredDriverTrip(TripResponse trip) {
        activeTripId = trip.id;
        lastTripStatusLabel = trip.statusLabel;
        lastTripStatusId = trip.statusId;
        activeTripAvailableSeats = trip.availableSeats;
        sessionManager.saveDriverActiveTripId(trip.id);
        syncTripStateToViewModel();

        selectedOrigin = Point.fromLngLat(trip.originLongitude, trip.originLatitude);
        selectedDestination = Point.fromLngLat(trip.destinationLongitude, trip.destinationLatitude);
        syncSelectionStateToViewModel();

        setSelectionMode(SelectionMode.NONE);
        updateCoordinateLabels();
        updateMapMarkers();
        requestDriverRoutePreviewIfOnMap();

        updateStatusText();
        updateRouteTimeText();
        refreshButtons();
        updateDrawerDriverMenuVisibility();
        checkPendingReservations();
    }

    /**
     * Trazar origen→destino del viaje activo del conductor (p. ej. al volver de otra sesión) cuando el mapa ya tiene capas.
     */
    private void requestDriverRoutePreviewIfOnMap() {
        if (!isDriverUser || activeTripId == null || activeTripId.isEmpty()) {
            return;
        }
        if (polylineAnnotationManager == null || selectedOrigin == null || selectedDestination == null) {
            return;
        }
        fetchAndDrawRoutePreviewAsync(selectedOrigin, selectedDestination);
    }

    private void applyRoleAccess() {
        int driverVisibility = isDriverUser ? View.VISIBLE : View.GONE;
        int passengerVisibility = isDriverUser ? View.GONE : View.VISIBLE;
        createTripButton.setVisibility(driverVisibility);
        cancelTripButton.setVisibility(driverVisibility);
        tripButtonsRow.setVisibility(driverVisibility);
        markBoardedButton.setVisibility(driverVisibility);
        startTripButton.setVisibility(driverVisibility);
        finishTripButton.setVisibility(driverVisibility);
        viewBoardedPassengersButton.setVisibility(driverVisibility);
        driverActionsRow.setVisibility(driverVisibility);
        findDriverButton.setVisibility(passengerVisibility);
        if (hasActivePassengerReservation && !isDriverUser) {
            findDriverButton.setVisibility(View.GONE);
        }
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
                } else if (id == R.id.nav_search_trip) {
                    startActivity(new Intent(this, SearchTripActivity.class));
                } else if (id == R.id.nav_help) {
                    startActivity(new Intent(this, HelpActivity.class));
                } else if (id == R.id.nav_support) {
                    startActivity(new Intent(this, SupportActivity.class));
                } else if (id == R.id.nav_my_addresses
                        || id == R.id.nav_notifications
                        || id == R.id.nav_security) {
                    Toast.makeText(this, R.string.drawer_option_coming_soon, Toast.LENGTH_SHORT).show();
                } else if (id == R.id.nav_driver_passenger_requests) {
                    openPassengerRequestsScreen(true);
                } else if (id == R.id.nav_find_driver) {
                    openDriverMatchScreen();
                } else if (id == R.id.nav_my_reservation) {
                    if (hasActivePassengerReservation) {
                        showBoardingCodeDialog(passengerReservedDriverName, passengerBoardingCode);
                    }
                } else if (id == R.id.nav_cancel_reservation) {
                    cancelPassengerReservationAction();
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
        // Cargar información del usuario desde SessionManager
        String userId = sessionManager.getUserId();
        String fullName = sessionManager.getFullName();
        String email = sessionManager.getEmail();
        
        if (drawerUserTitle != null && fullName != null) {
            drawerUserTitle.setText(fullName);
        }
        
        if (drawerUserEmail != null && email != null) {
            drawerUserEmail.setText(email);
        }
        
        // Generar y mostrar iniciales
        if (drawerUserInitials != null && fullName != null) {
            String initials = generateInitials(fullName);
            drawerUserInitials.setText(initials);
        }
        
        // Mostrar rol del usuario
        if (drawerUserRole != null) {
            String rolText = isDriverUser ? 
                getString(R.string.user_role_driver) : 
                getString(R.string.user_role_passenger);
            drawerUserRole.setText(rolText);
        }
        
        // Mostrar rating del usuario
        if (drawerUserRating != null) {
            if (userId != null && !userId.isEmpty()) {
                final String currentUid = userId;
                backgroundExecutor.execute(() -> {
                    try {
                        CarPoolingApplication app = (CarPoolingApplication) getApplication();
                        org.json.JSONObject summary = app.getRatingRemoteDataSource().getUserRatingSummary(currentUid, currentUid);
                        if (summary != null) {
                            double avg = summary.optDouble("averageScore", 0.0);
                            int count = summary.optInt("totalRatingsCount", 0);
                            runOnUiThread(() -> {
                                if (isFinishing() || isDestroyed()) return;
                                if (count > 0) {
                                    drawerUserRating.setText(String.format(Locale.US, "⭐ %.2f (%d)", avg, count));
                                } else {
                                    drawerUserRating.setText("Calificación: --");
                                }
                            });
                        }
                    } catch (Exception ignored) {}
                });
            } else {
                drawerUserRating.setText("Calificación: --");
            }
        }

        if (drawerReservationInfo != null) {
            if (hasActivePassengerReservation && !isDriverUser) {
                drawerReservationInfo.setVisibility(View.VISIBLE);
                if (drawerReservationDriver != null && passengerReservedDriverName != null) {
                    drawerReservationDriver.setText("Conductor: " + passengerReservedDriverName);
                }
                if (drawerReservationCode != null && passengerBoardingCode != null) {
                    drawerReservationCode.setText("Codigo: " + passengerBoardingCode);
                }
            } else {
                drawerReservationInfo.setVisibility(View.GONE);
            }
        }
    }
    
    private String generateInitials(String fullName) {
        if (fullName == null || fullName.isEmpty()) {
            return "UI";
        }
        String[] parts = fullName.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();
        for (int i = 0; i < Math.min(2, parts.length); i++) {
            if (parts[i].length() > 0) {
                initials.append(parts[i].charAt(0));
            }
        }
        return initials.length() > 0 ? initials.toString().toUpperCase() : "UI";
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
                            if (isDriverUser && activeTripId != null && !activeTripId.isEmpty()) {
                                checkPendingReservations();
                            }
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
        if (mapView == null) {
            return;
        }
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
        safeZoneMarkerBitmap = createSafeZoneMarkerBitmap();
        registerSafeZoneClickListenerIfNeeded();
        loadSafeZonesOnMap();
        updateMapMarkers();
        if (selectedOrigin != null && selectedDestination != null) {
            fetchAndDrawRoutePreviewAsync(selectedOrigin, selectedDestination);
        } else {
            requestDriverRoutePreviewIfOnMap();
        }
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

    private Bitmap createSafeZoneMarkerBitmap() {
        int size = 96;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(Color.parseColor("#43A047"));

        Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(6f);
        strokePaint.setColor(Color.WHITE);

        float centerX = size / 2f;
        float centerY = size * 0.42f;
        float radius = size * 0.22f;

        canvas.drawCircle(centerX, centerY, radius + 4f, strokePaint);
        canvas.drawCircle(centerX, centerY, radius, fillPaint);

        Paint shieldPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shieldPaint.setStyle(Paint.Style.FILL);
        shieldPaint.setColor(Color.WHITE);

        Path shield = new Path();
        shield.moveTo(centerX, size * 0.24f);
        shield.lineTo(centerX + radius * 0.72f, size * 0.34f);
        shield.lineTo(centerX + radius * 0.55f, size * 0.56f);
        shield.quadTo(centerX, size * 0.64f, centerX - radius * 0.55f, size * 0.56f);
        shield.lineTo(centerX - radius * 0.72f, size * 0.34f);
        shield.close();
        canvas.drawPath(shield, shieldPaint);

        Paint checkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        checkPaint.setStyle(Paint.Style.STROKE);
        checkPaint.setStrokeWidth(5f);
        checkPaint.setColor(Color.parseColor("#2E7D32"));
        checkPaint.setStrokeCap(Paint.Cap.ROUND);

        Path check = new Path();
        check.moveTo(centerX - radius * 0.22f, centerY);
        check.lineTo(centerX - radius * 0.02f, centerY + radius * 0.2f);
        check.lineTo(centerX + radius * 0.28f, centerY - radius * 0.18f);
        canvas.drawPath(check, checkPaint);

        return bitmap;
    }

    private void registerSafeZoneClickListenerIfNeeded() {
        if (safeZoneClickListenerRegistered || pointAnnotationManager == null) {
            return;
        }

        pointAnnotationManager.addClickListener(clicked -> {
            SafeZoneItem zone = safeZoneByAnnotationId.get(clicked.getId());
            if (zone != null) {
                runOnUiThread(() -> Toast.makeText(
                        MainActivity.this,
                        getString(R.string.safe_zone_marker_toast, zone.name),
                        Toast.LENGTH_SHORT
                ).show());
                return true;
            }
            return false;
        });
        safeZoneClickListenerRegistered = true;
    }

    private void loadSafeZonesOnMap() {
        if (pointAnnotationManager == null || safeZoneMarkerBitmap == null) {
            return;
        }

        backgroundExecutor.execute(() -> {
            try {
                List<SafeZoneItem> zones = ((CarPoolingApplication) getApplication())
                        .getSafeZonesRemoteDataSource()
                        .fetchActiveSafeZones();
                runOnUiThread(() -> renderSafeZoneMarkers(zones));
            } catch (IOException exception) {
                Log.w("MainActivity", "No se pudieron cargar zonas seguras", exception);
            }
        });
    }

    private void renderSafeZoneMarkers(List<SafeZoneItem> zones) {
        if (pointAnnotationManager == null || safeZoneMarkerBitmap == null) {
            return;
        }

        clearSafeZoneMarkers();
        for (SafeZoneItem zone : zones) {
            if (zone.latitude == 0d && zone.longitude == 0d) {
                continue;
            }
            PointAnnotationOptions options = new PointAnnotationOptions()
                    .withPoint(Point.fromLngLat(zone.longitude, zone.latitude))
                    .withIconImage(safeZoneMarkerBitmap)
                    .withIconSize(1.2);
            PointAnnotation annotation = pointAnnotationManager.create(options);
            safeZoneAnnotations.add(annotation);
            safeZoneByAnnotationId.put(annotation.getId(), zone);
        }
        safeZonesLoaded = true;
    }

    private void clearSafeZoneMarkers() {
        if (pointAnnotationManager == null) {
            return;
        }

        for (PointAnnotation annotation : safeZoneAnnotations) {
            pointAnnotationManager.delete(annotation);
        }
        safeZoneAnnotations.clear();
        safeZoneByAnnotationId.clear();
        safeZonesLoaded = false;
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
        if (isDriverUser) {
            showVehiclePickerDialog();
        } else {
            createTripWithVehicle(null);
        }
    }

    private void showVehiclePickerDialog() {
        setProgressVisible(true);
        final String userId = sessionManager.getUserId();
        mainViewModel.getVehiclesForUser(userId, new MainViewModel.ResultCallback<>() {
            @Override
            public void onSuccess(List<com.example.proyectocarpooling.data.model.user.VehicleResponse> vehicles) {
                setProgressVisible(false);
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (vehicles.isEmpty()) {
                    Toast.makeText(MainActivity.this, "No tienes vehiculos registrados", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (vehicles.size() == 1) {
                    createTripWithVehicle(vehicles.get(0).id);
                    return;
                }
                String[] items = new String[vehicles.size()];
                for (int i = 0; i < vehicles.size(); i++) {
                    com.example.proyectocarpooling.data.model.user.VehicleResponse v = vehicles.get(i);
                    items[i] = v.brand + " " + v.model + " (" + v.licensePlate + ")";
                }
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Seleccionar vehiculo")
                        .setItems(items, (dialog, which) -> createTripWithVehicle(vehicles.get(which).id))
                        .setNegativeButton("Cancelar", null)
                        .show();
            }

            @Override
            public void onError(String message) {
                setProgressVisible(false);
                Toast.makeText(MainActivity.this, "Error al cargar vehiculos", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createTripWithVehicle(String vehicleId) {
        setProgressVisible(true);
        createTripButton.setEnabled(false);

        final Point origin = selectedOrigin;
        final Point destination = selectedDestination;
        mainViewModel.createTrip(origin, destination, vehicleId, new MainViewModel.ResultCallback<>() {
            @Override
            public void onSuccess(CreateTripResult result) {
                TripResponse response = result.trip;
                activeTripId = response.id;
                lastTripStatusLabel = response.statusLabel;
                lastTripStatusId = response.statusId;
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
                checkPendingReservations();
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
                lastTripStatusLabel = response.statusLabel;
                lastTripStatusId = response.statusId;
                activeTripAvailableSeats = 0;
                activeTripPendingCount = 0;
                lastRouteTimeLabel = null;
                sessionManager.clearDriverActiveTripId();
                syncTripStateToViewModel();
                pollingHandler.removeCallbacks(pendingReservationPollingRunnable);
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

        // Center and zoom camera on the route points
        double sumLat = 0;
        double sumLng = 0;
        for (Point p : routePoints) {
            sumLat += p.latitude();
            sumLng += p.longitude();
        }
        double avgLat = sumLat / routePoints.size();
        double avgLng = sumLng / routePoints.size();
        Point center = Point.fromLngLat(avgLng, avgLat);

        Point first = routePoints.get(0);
        Point last = routePoints.get(routePoints.size() - 1);
        double distKm = distanceKm(first.latitude(), first.longitude(), last.latitude(), last.longitude());
        double zoom = 13.0;
        if (distKm < 0.5) {
            zoom = 15.5;
        } else if (distKm < 1.5) {
            zoom = 14.5;
        } else if (distKm < 3.0) {
            zoom = 13.8;
        } else if (distKm < 6.0) {
            zoom = 12.8;
        } else if (distKm < 12.0) {
            zoom = 11.8;
        } else if (distKm < 24.0) {
            zoom = 10.8;
        } else {
            zoom = 9.8;
        }

        mapView.getMapboxMap().setCamera(new CameraOptions.Builder()
                .center(center)
                .zoom(zoom)
                .build());
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

    private void updateStatusText() {
        if (statusText == null) {
            return;
        }

        if (hasActivePassengerReservation && !isDriverUser) {
            String driverName = passengerReservedDriverName != null && !passengerReservedDriverName.isEmpty()
                    ? passengerReservedDriverName
                    : "Asignado";
            statusText.setText("Conductor: " + driverName);
            return;
        }

        if (activeTripId == null || activeTripId.isEmpty()) {
            statusText.setText(R.string.trip_status_idle);
            return;
        }

        String baseStatus;
        if (lastTripStatusLabel != null && !lastTripStatusLabel.isEmpty()) {
            baseStatus = getString(R.string.trip_status_with_id, activeTripId) + " · " + lastTripStatusLabel;
        } else {
            baseStatus = getString(R.string.trip_status_with_id, activeTripId);
        }
        if (isDriverUser && activeTripPendingCount > 0) {
            baseStatus = baseStatus + "\nSolicitudes pendientes: " + activeTripPendingCount;
        }
        statusText.setText(baseStatus);
    }

    private void updateRouteTimeText() {
        if (routeTimeText == null) {
            return;
        }

        if (hasActivePassengerReservation && !isDriverUser) {
            routeTimeText.setText("Destino confirmado");
            return;
        }

        if (lastRouteTimeLabel == null || lastRouteTimeLabel.isEmpty()) {
            routeTimeText.setText(R.string.route_time_idle);
        } else {
            routeTimeText.setText(lastRouteTimeLabel);
        }
    }

    private void setProgressVisible(boolean visible) {
        if (loadingIndicator != null) {
            loadingIndicator.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private void hideDriverMatchRouteExplainer() {
        if (driverRoutePreviewBanner != null) {
            driverRoutePreviewBanner.setVisibility(View.GONE);
        }
        routePreviewTripId = null;
        routePreviewDriverName = null;
    }

    private void acceptDriverRouteFromBanner() {
        if (routePreviewTripId != null && !routePreviewTripId.isEmpty()) {
            String userId = sessionManager.getUserId();
            if (userId.isEmpty()) {
                Toast.makeText(this, "Sesión inválida", Toast.LENGTH_SHORT).show();
                return;
            }
            if (sessionManager.hasPassengerBookedTrip()) {
                Toast.makeText(this, R.string.passenger_already_booked, Toast.LENGTH_LONG).show();
                return;
            }
            submitReservationForTripId(routePreviewTripId, userId, routePreviewDriverName);
        } else {
            hideDriverMatchRouteExplainer();
            Toast.makeText(this, R.string.favorite_applied_toast, Toast.LENGTH_SHORT).show();
        }
    }

    private void submitReservationForTripId(String tripId, String passengerUserId, String driverName) {
        setProgressVisible(true);
        mainViewModel.createReservation(tripId, passengerUserId, 1, new MainViewModel.SimpleCallback() {
            @Override
            public void onSuccess() {
                backgroundExecutor.execute(() -> {
                    String resId = null;
                    String code = String.format(Locale.US, "%04d", (int)(Math.random() * 10000));
                    String finalDriverName = driverName != null ? driverName : "Conductor";
                    try {
                        CarPoolingApplication app = (CarPoolingApplication) getApplication();
                        JSONObject activeRes = app.getUserRepository().getActiveReservation(passengerUserId);
                        if (activeRes != null) {
                            resId = activeRes.optString("reservationId", null);
                            code = activeRes.optString("boardingCode", code);
                            finalDriverName = activeRes.optString("driverName", finalDriverName);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    final String finalResId = resId;
                    final String finalCode = code;
                    final String finalDriver = finalDriverName;

                    runOnUiThread(() -> {
                        sessionManager.savePassengerBookedTrip(tripId, finalResId, finalCode, finalDriver);
                        hasActivePassengerReservation = true;
                        setProgressVisible(false);
                        hideDriverMatchRouteExplainer();
                        Toast.makeText(MainActivity.this, R.string.toast_reservation_created, Toast.LENGTH_SHORT).show();
                        refreshForPassengerReservation();
                        refreshButtons();
                    });
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, R.string.toast_reservation_failed, Toast.LENGTH_SHORT).show();
                    setProgressVisible(false);
                });
            }
        });
    }

    private void openSaveFavoriteDialog() {
        boolean hasOrigin = selectedOrigin != null;
        boolean hasDestination = selectedDestination != null;

        if (!hasOrigin && !hasDestination) {
            Toast.makeText(this, R.string.favorite_save_need_point, Toast.LENGTH_SHORT).show();
            return;
        }

        final EditText input = new EditText(this);
        input.setHint(R.string.favorite_save_hint);

        String defaultTitle;
        if (hasOrigin && hasDestination) {
            defaultTitle = getString(R.string.favorite_type_route);
        } else {
            defaultTitle = getString(R.string.favorite_type_place);
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.favorite_save_dialog_title)
                .setMessage(defaultTitle)
                .setView(input)
                .setPositiveButton(R.string.favorite_save_confirm, (dialog, which) -> {
                    String title = input.getText().toString().trim();
                    if (title.isEmpty()) {
                        Toast.makeText(this, R.string.favorite_save_validation_title, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    saveFavorite(title);
                })
                .setNegativeButton(R.string.dialog_button_cancel, null)
                .show();
    }

    private void saveFavorite(String title) {
        String userId = sessionManager.getUserId();
        if (userId == null || userId.isBlank()) {
            Toast.makeText(this, R.string.favorites_error_session, Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedOrigin == null && selectedDestination == null) {
            Toast.makeText(this, R.string.favorite_save_need_point, Toast.LENGTH_SHORT).show();
            return;
        }

        final String kind = selectedOrigin != null && selectedDestination != null ? "route" : "place";
        final Point origin = selectedOrigin != null ? selectedOrigin : selectedDestination;
        final Double destLat = selectedOrigin != null && selectedDestination != null ? selectedDestination.latitude() : null;
        final Double destLng = selectedOrigin != null && selectedDestination != null ? selectedDestination.longitude() : null;

        setProgressVisible(true);
        backgroundExecutor.execute(() -> {
            try {
                CarPoolingApplication app = (CarPoolingApplication) getApplication();
                app.getFavoritesRepository().createFavorite(userId, kind, title, origin.latitude(), origin.longitude(), destLat, destLng);
                runOnUiThread(() -> {
                    setProgressVisible(false);
                    Toast.makeText(this, R.string.favorite_save_success, Toast.LENGTH_SHORT).show();
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    setProgressVisible(false);
                    Toast.makeText(this, R.string.favorite_save_error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void fetchAndDrawRoutePreviewAsync(Point origin, Point destination) {
        if (origin == null || destination == null) {
            return;
        }

        setProgressVisible(true);
        final boolean showPreviewBanner = routePreviewTripId != null || routePreviewDriverName != null;
        final String previewDriverName = routePreviewDriverName;

        backgroundExecutor.execute(() -> {
            try {
                CarPoolingApplication app = (CarPoolingApplication) getApplication();
                com.example.proyectocarpooling.data.model.RouteData route = app.getTripRepository().fetchRoute(origin, destination);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    drawRoute(route.points);
                    lastRouteTimeLabel = buildEstimatedTimeLabel(route.distanceMeters);
                    updateRouteTimeText();
                    if (showPreviewBanner && driverRoutePreviewBanner != null) {
                        String driverName = previewDriverName != null && !previewDriverName.isEmpty()
                                ? previewDriverName
                                : getString(R.string.driver_match_default_driver_name);
                        if (driverRoutePreviewBannerBody != null) {
                            driverRoutePreviewBannerBody.setText(getString(R.string.main_driver_route_banner_body, driverName));
                        }
                        if (bannerAcceptButton != null) {
                            bannerAcceptButton.setText("Reservar cupo");
                        }
                        driverRoutePreviewBanner.setVisibility(View.VISIBLE);
                    }
                    setProgressVisible(false);
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    setProgressVisible(false);
                    Toast.makeText(this, R.string.driver_match_fetch_error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showBoardingCodeDialog(String driverName, String boardingCode) {
        if (boardingCode == null || boardingCode.isEmpty()) {
            Toast.makeText(this, R.string.toast_network_error, Toast.LENGTH_SHORT).show();
            return;
        }

        String safeDriverName = driverName == null || driverName.isEmpty()
                ? getString(R.string.driver_match_default_driver_name)
                : driverName;

        new AlertDialog.Builder(this)
                .setTitle("Código de abordaje")
                .setMessage("Conductor: " + safeDriverName + "\nCódigo: " + boardingCode)
                .setPositiveButton(R.string.dialog_button_close, null)
                .show();
    }

    private boolean isTripReadyToStart() {
        return activeTripId != null && !activeTripId.isEmpty() && lastTripStatusId == 2;
    }

    private boolean isTripInProgress() {
        return activeTripId != null && !activeTripId.isEmpty() && lastTripStatusId == 3;
    }

    private void startTrip() {
        if (!isTripReadyToStart()) {
            Toast.makeText(this, R.string.toast_trip_start_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        setProgressVisible(true);
        mainViewModel.startTrip(activeTripId, currentDriverPosition, new MainViewModel.ResultCallback<>() {
            @Override
            public void onSuccess(TripResponse response) {
                lastTripStatusLabel = response.statusLabel;
                lastTripStatusId = response.statusId;
                activeTripAvailableSeats = response.availableSeats;
                syncTripStateToViewModel();
                updateStatusText();
                updateRouteTimeText();
                setProgressVisible(false);
                refreshButtons();
                pollingHandler.removeCallbacks(pendingReservationPollingRunnable);
                pollingHandler.post(pendingReservationPollingRunnable);
                Toast.makeText(MainActivity.this, R.string.toast_trip_started, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message) {
                setProgressVisible(false);
                Toast.makeText(MainActivity.this, R.string.toast_trip_start_failed, Toast.LENGTH_SHORT).show();
                refreshButtons();
            }
        });
    }

    private void finishTrip() {
        if (!isTripInProgress()) {
            Toast.makeText(this, R.string.toast_trip_finish_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        final String finishedTripId = activeTripId; // Keep in memory!
        setProgressVisible(true);
        mainViewModel.finishTrip(activeTripId, new MainViewModel.ResultCallback<>() {
            @Override
            public void onSuccess(TripResponse response) {
                activeTripId = null;
                lastTripStatusLabel = response.statusLabel;
                lastTripStatusId = response.statusId;
                activeTripAvailableSeats = response.availableSeats;
                activeTripPendingCount = 0;
                lastRouteTimeLabel = null;
                sessionManager.clearDriverActiveTripId();
                syncTripStateToViewModel();
                pollingHandler.removeCallbacks(pendingReservationPollingRunnable);
                clearRoute();
                selectedOrigin = null;
                selectedDestination = null;
                syncSelectionStateToViewModel();
                updateCoordinateLabels();
                updateMapMarkers();
                updateStatusText();
                updateRouteTimeText();
                setProgressVisible(false);
                refreshButtons();
                Toast.makeText(MainActivity.this, R.string.toast_trip_finished, Toast.LENGTH_SHORT).show();
                promptDriverToRatePassengers(finishedTripId);
            }

            @Override
            public void onError(String message) {
                setProgressVisible(false);
                Toast.makeText(MainActivity.this, R.string.toast_trip_finish_failed, Toast.LENGTH_SHORT).show();
                refreshButtons();
            }
        });
    }

    private void clearFavoriteApplyExtras(Intent intent) {
        if (intent == null) {
            return;
        }

        intent.removeExtra(EXTRA_APPLY_FAVORITE_ID);
        intent.removeExtra(EXTRA_APPLY_FAVORITE_KIND);
        intent.removeExtra(EXTRA_APPLY_ORIGIN_LAT);
        intent.removeExtra(EXTRA_APPLY_ORIGIN_LNG);
        intent.removeExtra(EXTRA_APPLY_DEST_LAT);
        intent.removeExtra(EXTRA_APPLY_DEST_LNG);
        intent.removeExtra(EXTRA_APPLY_PLACE_AS_ORIGIN);
        intent.removeExtra(EXTRA_HISTORY_ROUTE_PREVIEW);
        intent.removeExtra(EXTRA_ROUTE_PREVIEW_CONTEXT);
        intent.removeExtra(EXTRA_ROUTE_PREVIEW_DRIVER_NAME);
        intent.removeExtra(EXTRA_ROUTE_PREVIEW_TRIP_ID);
        intent.removeExtra(EXTRA_ROUTE_PREVIEW_DRIVER_TRIP_NAME);
    }

    private String normalizeTripStatus(String status) {
        if (status == null) {
            return "";
        }
        return status.trim().toLowerCase(Locale.US)
                .replace("í", "i")
                .replace("á", "a")
                .replace("é", "e")
                .replace("ó", "o")
                .replace("ú", "u")
                .replace(" ", "");
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
        findDriverButton.setEnabled(!isDriverUser && selectedDestination != null && !hasActivePassengerReservation);
        viewBoardedPassengersButton.setEnabled(isDriverUser && activeTripId != null);
        markBoardedButton.setEnabled(isDriverUser && activeTripId != null);
        startTripButton.setEnabled(isDriverUser && activeTripId != null && isTripReadyToStart());
        finishTripButton.setEnabled(isDriverUser && activeTripId != null && isTripInProgress());
        if (boardPassengerCodeButton != null) {
            boolean tripInProgress = isDriverUser && activeTripId != null && isTripInProgress();
            boardPassengerCodeButton.setEnabled(tripInProgress);
            boardPassengerCodeButton.setVisibility(tripInProgress ? View.VISIBLE : View.GONE);
        }
        if (saveFavoriteButton != null) {
            boolean canSaveFavorite = selectedOrigin != null || selectedDestination != null;
            saveFavoriteButton.setEnabled(canSaveFavorite);
        }
        if (chatFloatingButton != null) {
            boolean hasActiveTrip = (isDriverUser && activeTripId != null && !activeTripId.isEmpty()) || (!isDriverUser && hasActivePassengerReservation);
            chatFloatingButton.setVisibility(hasActiveTrip ? View.VISIBLE : View.GONE);
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
        String prevContext = intent.getStringExtra(EXTRA_ROUTE_PREVIEW_CONTEXT);
        String prevDriverName = intent.getStringExtra(EXTRA_ROUTE_PREVIEW_DRIVER_NAME);
        String prevTripId = intent.getStringExtra(EXTRA_ROUTE_PREVIEW_TRIP_ID);
        boolean shouldPreviewRoute = intent.getBooleanExtra(EXTRA_HISTORY_ROUTE_PREVIEW, false);
        
        // Asignar los valores de preview si vienen del intent
        if (prevTripId != null && !prevTripId.isEmpty()) {
            routePreviewTripId = prevTripId;
        }
        if (prevDriverName != null && !prevDriverName.isEmpty()) {
            routePreviewDriverName = prevDriverName;
        }
        
        if ("route".equals(normalizedKind)) {
            double dLat = intent.getDoubleExtra(EXTRA_APPLY_DEST_LAT, Double.NaN);
            double dLng = intent.getDoubleExtra(EXTRA_APPLY_DEST_LNG, Double.NaN);
            if (!Double.isNaN(dLat) && !Double.isNaN(dLng)) {
                selectedOrigin = Point.fromLngLat(oLng, oLat);
                selectedDestination = Point.fromLngLat(dLng, dLat);
                shouldPreviewRoute = true;
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
        if (selectedOrigin != null && selectedDestination != null) {
            fetchAndDrawRoutePreviewAsync(selectedOrigin, selectedDestination);
        }
        refreshForPassengerReservation();
    }

    private void reserveTrip() {
        if (activeTripId == null) return;
        if (activeTripAvailableSeats <= 0) {
            Toast.makeText(this, "No quedan cupos disponibles", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = sessionManager.getUserId();
        if (userId.isEmpty()) {
            Toast.makeText(this, "Sesión inválida", Toast.LENGTH_SHORT).show();
            return;
        }
        submitReservation(userId);
    }

    private void submitReservation(String passengerUserId) {
        setProgressVisible(true);
        mainViewModel.createReservation(activeTripId, passengerUserId, 1, new MainViewModel.SimpleCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(MainActivity.this, R.string.toast_reservation_created, Toast.LENGTH_SHORT).show();
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

        String userId = sessionManager.getUserId();
        if (userId.isEmpty()) {
            Toast.makeText(this, "Sesión inválida", Toast.LENGTH_SHORT).show();
            return;
        }
        findAndCancelPassengerReservation(userId);
    }

    private void findAndCancelPassengerReservation(String passengerUserId) {
        setProgressVisible(true);
        mainViewModel.getReservations(activeTripId, new MainViewModel.ResultCallback<>() {
            @Override
            public void onSuccess(List<ReservationResponse> reservations) {
                String targetReservationId = null;
                for (ReservationResponse reservation : reservations) {
                    if (reservation.passengerUserId != null && reservation.passengerUserId.equals(passengerUserId)) {
                        targetReservationId = reservation.id;
                        break;
                    }
                }

                if (targetReservationId != null) {
                    executeCancelReservation(targetReservationId);
                } else {
                    Toast.makeText(MainActivity.this, "No se encontró reserva", Toast.LENGTH_SHORT).show();
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
        String[] statuses;
        if (reservation.statusId == 1) { // pending
            statuses = new String[]{
                    "Aceptar (Confirmado)",
                    "Rechazar (Cancelado)"
            };
        } else {
            statuses = new String[]{
                    getString(R.string.manual_status_active),
                    getString(R.string.manual_status_boarded),
                    getString(R.string.manual_status_cancelled)
            };
        }

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_title_manual_status) + ": " + reservation.getPassengerName())
                .setItems(statuses, (dialog, which) -> {
                    if (reservation.statusId == 1) { // pending
                        if (which == 0) {
                            acceptPendingReservation(reservation.id);
                        } else {
                            rejectPendingReservation(reservation.id);
                        }
                    } else {
                        String targetStatus;
                        if (which == 0) targetStatus = "Active";
                        else if (which == 1) targetStatus = "Boarded";
                        else targetStatus = "Cancelled";
                        updatePassengerStatusManual(reservation, targetStatus, refreshBoardedList);
                    }
                })
                .setNegativeButton(R.string.dialog_button_cancel, null)
                .show();
    }

    private void acceptPendingReservation(String reservationId) {
        setProgressVisible(true);
        String apiBase = ApiBaseUrlProvider.get(this);
        String mapboxToken = getString(R.string.mapbox_access_token);
        TripRepository repository = ((CarPoolingApplication) getApplication()).getTripRepository();
        backgroundExecutor.execute(() -> {
            try {
                repository.acceptReservation(activeTripId, reservationId);
                runOnUiThread(() -> {
                    setProgressVisible(false);
                    activeTripAvailableSeats--;
                    syncTripStateToViewModel();
                    updateStatusText();
                    Toast.makeText(MainActivity.this, "Reserva aceptada", Toast.LENGTH_SHORT).show();
                    openPassengerRequestsScreen(false);
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    setProgressVisible(false);
                    Toast.makeText(MainActivity.this, "Error al aceptar", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void rejectPendingReservation(String reservationId) {
        setProgressVisible(true);
        String apiBase = ApiBaseUrlProvider.get(this);
        String mapboxToken = getString(R.string.mapbox_access_token);
        TripRepository repository = ((CarPoolingApplication) getApplication()).getTripRepository();
        backgroundExecutor.execute(() -> {
            try {
                repository.rejectReservation(activeTripId, reservationId);
                runOnUiThread(() -> {
                    setProgressVisible(false);
                    Toast.makeText(MainActivity.this, "Reserva rechazada", Toast.LENGTH_SHORT).show();
                    openPassengerRequestsScreen(false);
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    setProgressVisible(false);
                    Toast.makeText(MainActivity.this, "Error al rechazar", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void markPassengerBoardedByName() {
        if (activeTripId == null) return;
        setProgressVisible(true);
        mainViewModel.getConfirmedReservations(activeTripId, new MainViewModel.ResultCallback<>() {
            @Override
            public void onSuccess(List<ReservationResponse> confirmed) {
                setProgressVisible(false);
                if (confirmed.isEmpty()) {
                    Toast.makeText(MainActivity.this, "No hay reservas confirmadas para abordar", Toast.LENGTH_SHORT).show();
                    return;
                }
                ArrayAdapter<ReservationResponse> adapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_list_item_1, confirmed);
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Seleccionar pasajero a abordar")
                        .setAdapter(adapter, (dialog, which) -> {
                            ReservationResponse selected = confirmed.get(which);
                            promptForBoardingCode(selected);
                        })
                        .setNegativeButton(R.string.dialog_button_cancel, null)
                        .show();
            }

            @Override
            public void onError(String message) {
                setProgressVisible(false);
                Toast.makeText(MainActivity.this, R.string.toast_network_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void promptForBoardingCode(final ReservationResponse selected) {
        final EditText input = new EditText(this);
        input.setHint("Código de 4 dígitos");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        new AlertDialog.Builder(this)
                .setTitle("Abordar a " + selected.getPassengerName())
                .setMessage("Pídele al pasajero su código de abordaje de 4 dígitos:")
                .setView(input)
                .setPositiveButton("Confirmar", (dialog, which) -> {
                    String code = input.getText().toString().trim();
                    if (code.isEmpty()) {
                        Toast.makeText(this, "Debe ingresar el código", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    verifyAndBoardPassengerByIdAndCode(selected.id, code);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void verifyAndBoardPassengerByIdAndCode(final String reservationId, final String code) {
        setProgressVisible(true);
        mainViewModel.verifyBoardingCode(activeTripId, reservationId, code, new MainViewModel.SimpleCallback() {
            @Override
            public void onSuccess() {
                // Código correcto, proceder a abordar
                boardPassenger(reservationId);
            }

            @Override
            public void onError(String message) {
                setProgressVisible(false);
                Toast.makeText(MainActivity.this, "Código de abordaje incorrecto para este pasajero.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void boardPassenger(final String reservationId) {
        setProgressVisible(true);
        String apiBase = ApiBaseUrlProvider.get(this);
        String mapboxToken = getString(R.string.mapbox_access_token);
        TripRepository repository = ((CarPoolingApplication) getApplication()).getTripRepository();
        backgroundExecutor.execute(() -> {
            try {
                repository.boardPassenger(activeTripId, reservationId);
                runOnUiThread(() -> {
                    setProgressVisible(false);
                    Toast.makeText(MainActivity.this, R.string.toast_boarding_confirmed, Toast.LENGTH_SHORT).show();
                    viewBoardedPassengers();
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    setProgressVisible(false);
                    Toast.makeText(MainActivity.this, R.string.toast_boarding_failed, Toast.LENGTH_SHORT).show();
                });
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
                .setMessage("¿Deseas cancelar la reserva de " + reservation.getPassengerName() + "?")
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
        if (hasActivePassengerReservation) {
            Toast.makeText(this, "Ya tienes un viaje reservado. Cancelalo antes de buscar otro.", Toast.LENGTH_LONG).show();
            return;
        }
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

    private void checkPendingReservations() {
        if (!isDriverUser || activeTripId == null || activeTripId.isEmpty()) {
            return;
        }
        final String tripId = activeTripId;
        backgroundExecutor.execute(() -> {
            try {
                String apiBase = ApiBaseUrlProvider.get(MainActivity.this);
                String mapboxToken = getString(R.string.mapbox_access_token);
                TripRepository repository = ((CarPoolingApplication) getApplication()).getTripRepository();
                List<ReservationResponse> pending = repository.getReservations(tripId);
                final int count = pending.size();
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    activeTripPendingCount = count;
                    updateStatusText();
                });
            } catch (IOException ignored) {
            }
        });
    }

    private void showBoardPassengerByCodeDialog() {
        if (activeTripId == null || activeTripId.isEmpty()) {
            return;
        }
        final EditText input = new EditText(this);
        input.setHint("Código de abordaje del pasajero");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        new AlertDialog.Builder(this)
                .setTitle("Subir pasajero")
                .setView(input)
                .setPositiveButton("Confirmar", (dialog, which) -> {
                    String code = input.getText().toString().trim();
                    if (code.isEmpty()) {
                        Toast.makeText(this, "Ingrese el codigo", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    verifyAndBoardPassengerByCode(code);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void verifyAndBoardPassengerByCode(String code) {
        final String tripId = activeTripId;
        setProgressVisible(true);
        mainViewModel.getConfirmedReservations(tripId, new MainViewModel.ResultCallback<>() {
            @Override
            public void onSuccess(List<ReservationResponse> confirmed) {
                if (confirmed.isEmpty()) {
                    setProgressVisible(false);
                    Toast.makeText(MainActivity.this, "No hay pasajeros confirmados", Toast.LENGTH_SHORT).show();
                    return;
                }
                tryMatchCode(tripId, confirmed, code, 0);
            }

            @Override
            public void onError(String message) {
                setProgressVisible(false);
                Toast.makeText(MainActivity.this, R.string.toast_network_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void tryMatchCode(String tripId, List<ReservationResponse> confirmed, String code, int index) {
        if (index >= confirmed.size()) {
            setProgressVisible(false);
            Toast.makeText(MainActivity.this, "Codigo no coincide con ningun pasajero confirmado", Toast.LENGTH_SHORT).show();
            return;
        }
        ReservationResponse reservation = confirmed.get(index);
        mainViewModel.verifyBoardingCode(tripId, reservation.id, code, new MainViewModel.SimpleCallback() {
            @Override
            public void onSuccess() {
                mainViewModel.boardPassenger(tripId, reservation.id, new MainViewModel.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        setProgressVisible(false);
                        Toast.makeText(MainActivity.this, "Pasajero abordado", Toast.LENGTH_SHORT).show();
                        viewBoardedPassengers();
                    }

                    @Override
                    public void onError(String message) {
                        setProgressVisible(false);
                        Toast.makeText(MainActivity.this, "Error al abordar", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String message) {
                tryMatchCode(tripId, confirmed, code, index + 1);
            }
        });
    }

    private void pollPassengerReservationStatus() {
        if (!hasActivePassengerReservation || isDriverUser) {
            return;
        }
        final String userId = sessionManager.getUserId();
        if (userId == null || userId.isEmpty()) {
            return;
        }
        backgroundExecutor.execute(() -> {
            try {
                CarPoolingApplication app = (CarPoolingApplication) getApplication();
                org.json.JSONObject reservation = app.getUserRepository().getActiveReservation(userId);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    if (reservation != null) {
                        int statusId = reservation.optInt("statusId", 0);
                        if (selectionInstruction != null) {
                            if (statusId == 1) {
                                selectionInstruction.setText("Pendiente de confirmación");
                            } else if (statusId == 2) {
                                String bc = reservation.optString("boardingCode", "----");
                                selectionInstruction.setText("Confirmado - Código: " + bc);
                            } else if (statusId == 3) {
                                selectionInstruction.setText("Abordado - Buen viaje!");
                            }
                        }
                        if (statusText != null) {
                            String dName = reservation.optString("driverName", "");
                            String vBrand = reservation.optString("vehicleBrand", "");
                            String vPlate = reservation.optString("vehiclePlate", "");
                            StringBuilder sb = new StringBuilder();
                            if (!dName.isEmpty()) {
                                sb.append("Conductor: ").append(dName);
                            }
                            if (!vBrand.isEmpty() || !vPlate.isEmpty()) {
                                if (sb.length() > 0) sb.append(" · ");
                                sb.append(vBrand);
                                if (!vPlate.isEmpty()) {
                                    if (!vBrand.isEmpty()) sb.append(" ");
                                    sb.append(vPlate);
                                }
                            }
                            if (sb.length() > 0) {
                                statusText.setText(sb.toString());
                            }
                        }
                    } else {
                        // El servidor retornó 404/null (la reserva ya no está activa, ej: el viaje finalizó)
                        final String finishedTripId = passengerReservedTripId;
                        sessionManager.clearPassengerBookedTrip();
                        hasActivePassengerReservation = false;
                        passengerReservedTripId = null;
                        refreshForPassengerReservation();
                        refreshButtons();
                        if (finishedTripId != null && !finishedTripId.isEmpty()) {
                            checkFinishedTripAndPromptRating(finishedTripId);
                        }
                    }
                });
            } catch (IOException ignored) {
            }
        });
    }

    private void startPassengerPolling() {
        if (passengerPollingRunnable != null) {
            pollingHandler.removeCallbacks(passengerPollingRunnable);
        }
        passengerPollingRunnable = new Runnable() {
            @Override
            public void run() {
                pollPassengerReservationStatus();
                if (hasActivePassengerReservation && !isDriverUser) {
                    pollingHandler.postDelayed(this, 15_000);
                }
            }
        };
        pollingHandler.post(passengerPollingRunnable);
    }

    private void stopPassengerPolling() {
        if (passengerPollingRunnable != null) {
            pollingHandler.removeCallbacks(passengerPollingRunnable);
            passengerPollingRunnable = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (hasActivePassengerReservation && !isDriverUser) {
            startPassengerPolling();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mapView != null) {
            mapView.onStart();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopPassengerPolling();
    }

    @Override
    protected void onStop() {
        if (mapView != null) {
            mapView.onStop();
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (mapView != null) {
            mapView.onDestroy();
        }
        super.onDestroy();
        pollingHandler.removeCallbacks(pendingReservationPollingRunnable);
        stopPassengerPolling();
        backgroundExecutor.shutdownNow();
    }

    private void promptDriverToRatePassengers(String finishedTripId) {
        if (finishedTripId == null || finishedTripId.isEmpty()) {
            return;
        }
        mainViewModel.getBoardedPassengers(finishedTripId, new MainViewModel.ResultCallback<List<ReservationResponse>>() {
            @Override
            public void onSuccess(List<ReservationResponse> boarded) {
                if (boarded == null || boarded.isEmpty()) {
                    return;
                }
                showDriverRatingQueueDialog(finishedTripId, boarded, 0);
            }

            @Override
            public void onError(String message) {
                // Falla silenciosa
            }
        });
    }

    private void showDriverRatingQueueDialog(String finishedTripId, List<ReservationResponse> passengers, int index) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        if (index >= passengers.size()) {
            Toast.makeText(this, "¡Todas las calificaciones han sido enviadas!", Toast.LENGTH_SHORT).show();
            return;
        }

        ReservationResponse passenger = passengers.get(index);
        final String passengerId = passenger.passengerUserId;
        final String passengerName = passenger.getPassengerName();

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_rating, null);
        if (dialogView == null) {
            Log.w("MainActivity", "Failed to inflate dialog_rating layout");
            return;
        }
        TextView titleView = dialogView.findViewById(R.id.ratingPassengerTitle);
        RatingBar ratingBar = dialogView.findViewById(R.id.ratingStars);
        EditText commentInput = dialogView.findViewById(R.id.ratingCommentInput);
        android.widget.LinearLayout tagsContainer = dialogView.findViewById(R.id.ratingTagsContainer);
        if (ratingBar == null) Log.w("MainActivity", "ratingStars view is null in dialog_rating");
        if (commentInput == null) Log.w("MainActivity", "ratingCommentInput view is null in dialog_rating");
        if (tagsContainer == null) Log.w("MainActivity", "ratingTagsContainer view is null in dialog_rating");

        if (titleView != null) {
            titleView.setText("Calificar a " + passengerName);
        }

        final java.util.List<String> selectedTags = new java.util.ArrayList<>();
        String[] passengerTags = {"Respetuoso", "Puntual", "Buen pasajero", "Amigable", "Tranquilo"};
        setupRatingTags(tagsContainer, passengerTags, selectedTags);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .setPositiveButton("Enviar Calificación", null)
                .setNegativeButton("Saltar", (d, w) -> {
                    showDriverRatingQueueDialog(finishedTripId, passengers, index + 1);
                })
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button submitButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            submitButton.setOnClickListener(v -> {
                int score = ratingBar != null ? (int) ratingBar.getRating() : 5;
                if (score < 1 || score > 5) {
                    Toast.makeText(MainActivity.this, "Por favor selecciona un puntaje entre 1 y 5 estrellas", Toast.LENGTH_SHORT).show();
                    return;
                }
                String comment = commentInput != null ? commentInput.getText().toString().trim() : "";

                StringBuilder tagsBuilder = new StringBuilder();
                for (int i = 0; i < selectedTags.size(); i++) {
                    tagsBuilder.append(selectedTags.get(i));
                    if (i < selectedTags.size() - 1) {
                        tagsBuilder.append(",");
                    }
                }
                String tagsString = tagsBuilder.toString();

                submitDriverRatingAsync(finishedTripId, passengerId, score, comment, tagsString, new MainViewModel.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        dialog.dismiss();
                        showDriverRatingQueueDialog(finishedTripId, passengers, index + 1);
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(MainActivity.this, "Error al calificar: " + message, Toast.LENGTH_LONG).show();
                    }
                });
            });
        });

        dialog.show();
    }

    private void submitDriverRatingAsync(String tripId, String passengerUserId, int score, String comment, String tags, MainViewModel.SimpleCallback callback) {
        final String evaluatorUserId = sessionManager.getUserId();
        if (evaluatorUserId.isEmpty()) {
            callback.onError("Sesión inválida");
            return;
        }

        backgroundExecutor.execute(() -> {
            try {
                CarPoolingApplication app = (CarPoolingApplication) getApplication();
                app.getRatingRemoteDataSource().createRating(tripId, evaluatorUserId, passengerUserId, score, comment, tags);
                runOnUiThread(callback::onSuccess);
            } catch (Exception e) {
                runOnUiThread(() -> callback.onError(e.getMessage() != null ? e.getMessage() : "Error de red"));
            }
        });
    }

    private void checkFinishedTripAndPromptRating(String tripId) {
        backgroundExecutor.execute(() -> {
            try {
                CarPoolingApplication app = (CarPoolingApplication) getApplication();
                TripResponse trip = app.getTripRepository().getTripByIdIfPresent(tripId);
                if (trip != null) {
                    String status = trip.statusLabel != null ? trip.statusLabel.trim().toLowerCase(Locale.US) : "";
                    if (status.contains("finalizado") || status.contains("finished") || trip.statusId == 4) {
                        runOnUiThread(() -> {
                            promptPassengerToRateDriver(tripId, trip.driverUserId != null ? trip.driverUserId : "", trip.driverName);
                        });
                    }
                }
            } catch (IOException ignored) {}
        });
    }

    private void promptPassengerToRateDriver(String tripId, String driverUserId, String driverName) {
        if (isFinishing() || isDestroyed() || driverUserId == null || driverUserId.isEmpty()) {
            return;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_rating, null);
        if (dialogView == null) {
            Log.w("MainActivity", "Failed to inflate dialog_rating layout (passenger)-> promptPassengerToRateDriver");
            return;
        }
        TextView titleView = dialogView.findViewById(R.id.ratingPassengerTitle);
        RatingBar ratingBar = dialogView.findViewById(R.id.ratingStars);
        EditText commentInput = dialogView.findViewById(R.id.ratingCommentInput);
        android.widget.LinearLayout tagsContainer = dialogView.findViewById(R.id.ratingTagsContainer);
        if (ratingBar == null) Log.w("MainActivity", "ratingStars view is null in dialog_rating (passenger)");
        if (commentInput == null) Log.w("MainActivity", "ratingCommentInput view is null in dialog_rating (passenger)");
        if (tagsContainer == null) Log.w("MainActivity", "ratingTagsContainer view is null in dialog_rating (passenger)");

        if (titleView != null) {
            titleView.setText("Calificar a " + (driverName != null && !driverName.isEmpty() ? driverName : "Conductor"));
        }
        if (commentInput != null) {
            commentInput.setHint("Escribe una opinión sobre el conductor (opcional)...");
        }

        final java.util.List<String> selectedTags = new java.util.ArrayList<>();
        String[] driverTags = {"Vehículo limpio", "Conducción segura", "Puntual", "Buena música", "Amigable"};
        setupRatingTags(tagsContainer, driverTags, selectedTags);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .setPositiveButton("Enviar Calificación", null)
                .setNegativeButton("Saltar", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button submitButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            submitButton.setOnClickListener(v -> {
                int score = ratingBar != null ? (int) ratingBar.getRating() : 5;
                if (score < 1 || score > 5) {
                    Toast.makeText(MainActivity.this, "Por favor selecciona un puntaje entre 1 y 5 estrellas", Toast.LENGTH_SHORT).show();
                    return;
                }
                String comment = commentInput != null ? commentInput.getText().toString().trim() : "";

                StringBuilder tagsBuilder = new StringBuilder();
                for (int i = 0; i < selectedTags.size(); i++) {
                    tagsBuilder.append(selectedTags.get(i));
                    if (i < selectedTags.size() - 1) {
                        tagsBuilder.append(",");
                    }
                }
                String tagsString = tagsBuilder.toString();

                submitPassengerRatingAsync(tripId, driverUserId, score, comment, tagsString, new MainViewModel.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        dialog.dismiss();
                        Toast.makeText(MainActivity.this, "¡Calificación enviada con éxito!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(MainActivity.this, "Error al calificar: " + message, Toast.LENGTH_LONG).show();
                    }
                });
            });
        });

        dialog.show();
    }

    private void submitPassengerRatingAsync(String tripId, String driverUserId, int score, String comment, String tags, MainViewModel.SimpleCallback callback) {
        final String evaluatorUserId = sessionManager.getUserId();
        if (evaluatorUserId.isEmpty()) {
            callback.onError("Sesión inválida");
            return;
        }

        backgroundExecutor.execute(() -> {
            try {
                CarPoolingApplication app = (CarPoolingApplication) getApplication();
                app.getRatingRemoteDataSource().createRating(tripId, evaluatorUserId, driverUserId, score, comment, tags);
                runOnUiThread(callback::onSuccess);
            } catch (Exception e) {
                runOnUiThread(() -> callback.onError(e.getMessage() != null ? e.getMessage() : "Error de red"));
            }
        });
    }

    private void setupRatingTags(android.widget.LinearLayout container, String[] tagsList, final java.util.List<String> selectedTagsList) {
        if (container == null || tagsList == null) return;
        container.removeAllViews();
        selectedTagsList.clear();

        float density = getResources().getDisplayMetrics().density;
        int horizontalMargin = (int) (6 * density);
        int verticalPadding = (int) (6 * density);
        int horizontalPadding = (int) (12 * density);

        for (final String tag : tagsList) {
            final TextView chip = new TextView(this);
            chip.setText(tag);
            chip.setTextSize(12);
            chip.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);

            android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, horizontalMargin, 0);
            chip.setLayoutParams(lp);

            chip.setBackgroundResource(R.drawable.bg_chip_unselected);
            chip.setTextColor(getResources().getColor(R.color.carpool_text_secondary, null));

            chip.setOnClickListener(v -> {
                if (selectedTagsList.contains(tag)) {
                    selectedTagsList.remove(tag);
                    chip.setBackgroundResource(R.drawable.bg_chip_unselected);
                    chip.setTextColor(getResources().getColor(R.color.carpool_text_secondary, null));
                } else {
                    selectedTagsList.add(tag);
                    chip.setBackgroundResource(R.drawable.bg_chip_selected);
                    chip.setTextColor(getResources().getColor(R.color.white, null));
                }
            });

            container.addView(chip);
        }
    }
}