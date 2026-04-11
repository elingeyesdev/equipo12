package com.example.proyectocarpooling.presentation.main.ui;

import android.Manifest;
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
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.proyectocarpooling.R;
import com.example.proyectocarpooling.data.model.ReservationResponse;
import com.example.proyectocarpooling.data.model.TripResponse;
import com.example.proyectocarpooling.domain.model.CreateTripResult;
import com.example.proyectocarpooling.presentation.main.viewmodel.MainViewModel;

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

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

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
    private Button cancelPassengerReservationButton;
    private Button viewReservationsButton;
    private Button viewBoardedPassengersButton;
    private Button markBoardedButton;
    private Button startTripButton;
    private Button finishTripButton;
    private LinearLayout controlPanel;
    private LinearLayout selectionButtonsRow;
    private LinearLayout tripButtonsRow;
    private LinearLayout passengerButtonsRow;

    private boolean isInitialPositionSet = false;
    private Point selectedOrigin;
    private Point selectedDestination;
    private SelectionMode selectionMode = SelectionMode.NONE;
    private String activeTripId;
    private String lastTripStatusLabel;
    private String lastRouteTimeLabel;
    private int activeTripAvailableSeats = 0;
    private Point currentDriverPosition;
    private boolean isPanelCollapsed = true;

    private LocationComponentPlugin locationComponentPlugin;
    private PointAnnotationManager pointAnnotationManager;
    private PolylineAnnotationManager polylineAnnotationManager;
    private PolylineAnnotation routeAnnotation;
    private PointAnnotation originAnnotation;
    private PointAnnotation destinationAnnotation;
    private Bitmap originMarkerBitmap;
    private Bitmap destinationMarkerBitmap;
    private MainViewModel mainViewModel;

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
        cancelPassengerReservationButton = findViewById(R.id.cancelPassengerReservationButton);
        viewReservationsButton = findViewById(R.id.viewReservationsButton);
        viewBoardedPassengersButton = findViewById(R.id.viewBoardedPassengersButton);
        markBoardedButton = findViewById(R.id.markBoardedButton);
        startTripButton = findViewById(R.id.startTripButton);
        finishTripButton = findViewById(R.id.finishTripButton);
        selectionButtonsRow = findViewById(R.id.selectionButtonsRow);
        tripButtonsRow = findViewById(R.id.tripButtonsRow);
        passengerButtonsRow = findViewById(R.id.passengerButtonsRow);

        menuToggleButton.setOnClickListener(v -> toggleControlPanel());
        selectOriginButton.setOnClickListener(v -> setSelectionMode(SelectionMode.ORIGIN));
        selectDestinationButton.setOnClickListener(v -> setSelectionMode(SelectionMode.DESTINATION));
        reserveTripButton.setOnClickListener(v -> reserveTrip());
        cancelPassengerReservationButton.setOnClickListener(v -> cancelPassengerReservation());
        viewReservationsButton.setOnClickListener(v -> viewReservations());
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

        updateCoordinateLabels();
        refreshButtons();
        updateStatusText();
        updateRouteTimeText();
    }

    private void toggleControlPanel() {
        isPanelCollapsed = !isPanelCollapsed;
        controlPanel.setVisibility(isPanelCollapsed ? View.GONE : View.VISIBLE);

        menuToggleButton.setContentDescription(getString(
                isPanelCollapsed ? R.string.button_expand_panel : R.string.button_collapse_panel
        ));
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
        boolean canCreate = selectedOrigin != null && selectedDestination != null && activeTripId == null;
        createTripButton.setEnabled(canCreate);
        cancelTripButton.setEnabled(activeTripId != null);
        reserveTripButton.setEnabled(activeTripId != null);
        cancelPassengerReservationButton.setEnabled(activeTripId != null);
        viewReservationsButton.setEnabled(activeTripId != null);
        viewBoardedPassengersButton.setEnabled(activeTripId != null);
        markBoardedButton.setEnabled(activeTripId != null);
        startTripButton.setEnabled(activeTripId != null && isTripReadyToStart());
        finishTripButton.setEnabled(activeTripId != null && isTripInProgress());
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
                lastTripStatusLabel = tripResponse.status;
                activeTripAvailableSeats = tripResponse.availableSeats;
                syncTripStateToViewModel();
                updateStatusText();
                setProgressVisible(false);
                refreshButtons();
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
        super.onDestroy();
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

    private void viewReservations() {
        if (activeTripId == null) return;
        setProgressVisible(true);
        mainViewModel.getReservations(activeTripId, new MainViewModel.ResultCallback<>() {
            @Override
            public void onSuccess(List<ReservationResponse> reservations) {
                showReservationsDialog(reservations);
                setProgressVisible(false);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(MainActivity.this, R.string.toast_network_error, Toast.LENGTH_SHORT).show();
                setProgressVisible(false);
            }
        });
    }

    private void showReservationsDialog(List<ReservationResponse> reservations) {
        if (reservations.isEmpty()) {
            Toast.makeText(this, "No hay reservas para este viaje", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayAdapter<ReservationResponse> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, reservations);
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_title_reservations)
                .setAdapter(adapter, null)
                .setNegativeButton(R.string.dialog_button_close, null)
                .show();
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
                viewReservations();
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
                    viewReservations();
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

}