package com.example.proyectocarpooling;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Bundle;
import android.util.Log;
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
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorBearingChangedListener;
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

public class MainActivity extends AppCompatActivity {

    private enum SelectionMode { NONE, ORIGIN, DESTINATION }

    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");
    private static final String TAG = "TripsApi";

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
    private OkHttpClient httpClient;
    private ExecutorService networkExecutor;
    private String apiBaseUrl;
    private String mapboxAccessToken;

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

        bindViews();
        initNetworking();
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

    private void initNetworking() {
        networkExecutor = Executors.newSingleThreadExecutor();
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(message -> Log.d(TAG, message));
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .build();

        apiBaseUrl = getString(R.string.api_base_url);
        if (apiBaseUrl.endsWith("/")) {
            apiBaseUrl = apiBaseUrl.substring(0, apiBaseUrl.length() - 1);
        }

        mapboxAccessToken = getString(R.string.mapbox_access_token);
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

        networkExecutor.execute(() -> {
            try {
                TripResponse response = createTripOnServer(origin, destination);
                RouteData routeData = fetchRouteData(origin, destination);
                activeTripId = response.id;
                lastTripStatusLabel = response.status;
                activeTripAvailableSeats = response.availableSeats;
                lastRouteTimeLabel = buildEstimatedTimeLabel(routeData.distanceMeters);
                runOnUiThread(() -> {
                    if (routeData.points != null && !routeData.points.isEmpty()) {
                        drawRoute(routeData.points);
                    }
                    Toast.makeText(this, R.string.toast_trip_created, Toast.LENGTH_SHORT).show();
                    updateStatusText();
                    updateRouteTimeText();
                });
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Error creating trip", e);
                runOnUiThread(() -> Toast.makeText(this, R.string.toast_network_error, Toast.LENGTH_SHORT).show());
            } finally {
                runOnUiThread(() -> {
                    setProgressVisible(false);
                    refreshButtons();
                });
            }
        });
    }

    private void cancelTrip() {
        setProgressVisible(true);
        cancelTripButton.setEnabled(false);
        final String tripId = activeTripId;

        networkExecutor.execute(() -> {
            try {
                TripResponse response = cancelTripOnServer(tripId);
                activeTripId = null;
                lastTripStatusLabel = response.status;
                activeTripAvailableSeats = 0;
                lastRouteTimeLabel = null;
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.toast_trip_cancelled, Toast.LENGTH_SHORT).show();
                    selectedOrigin = null;
                    selectedDestination = null;
                    clearRoute();
                    setSelectionMode(SelectionMode.NONE);
                    updateCoordinateLabels();
                    updateMapMarkers();
                    updateStatusText();
                    updateRouteTimeText();
                });
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Error cancelling trip", e);
                runOnUiThread(() -> Toast.makeText(this, R.string.toast_network_error, Toast.LENGTH_SHORT).show());
            } finally {
                runOnUiThread(() -> {
                    setProgressVisible(false);
                    refreshButtons();
                });
            }
        });
    }

    private TripResponse createTripOnServer(Point origin, Point destination) throws IOException, JSONException {
        TripResponse createdTrip = sendCoordinateRequest(apiBaseUrl + "/api/Trips/origin", origin);
        if (destination != null) {
            createdTrip = sendCoordinateRequest(apiBaseUrl + "/api/Trips/" + createdTrip.id + "/destination", destination);
        }
        return createdTrip;
    }

    private TripResponse cancelTripOnServer(String tripId) throws IOException, JSONException {
        Request request = new Request.Builder()
                .url(apiBaseUrl + "/api/Trips/" + tripId + "/cancel")
                .post(RequestBody.create("{}", JSON_MEDIA_TYPE))
                .build();
        return executeRequest(request);
    }

    private TripResponse sendCoordinateRequest(String url, Point point) throws IOException, JSONException {
        JSONObject body = new JSONObject();
        body.put("latitude", point.latitude());
        body.put("longitude", point.longitude());

        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(body.toString(), JSON_MEDIA_TYPE))
                .build();

        return executeRequest(request);
    }

    private RouteData fetchRouteData(Point origin, Point destination) throws IOException, JSONException {
        String routeUrl = String.format(
                "https://api.mapbox.com/directions/v5/mapbox/driving/%f,%f;%f,%f?geometries=geojson&overview=full&access_token=%s",
                origin.longitude(),
                origin.latitude(),
                destination.longitude(),
                destination.latitude(),
                mapboxAccessToken);

        Request request = new Request.Builder()
                .url(routeUrl)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected route response: " + response);
            }
            if (response.body() == null) {
                throw new IOException("Respuesta vacía de la ruta");
            }

            JSONObject json = new JSONObject(response.body().string());
            JSONArray routes = json.getJSONArray("routes");
            if (routes.length() == 0) {
                throw new IOException("No se encontró ruta");
            }

            JSONObject route = routes.getJSONObject(0);
            JSONObject geometry = route.getJSONObject("geometry");
            double distanceMeters = route.optDouble("distance", 0.0);
            JSONArray coordinates = geometry.getJSONArray("coordinates");

            List<Point> points = new ArrayList<>();
            for (int i = 0; i < coordinates.length(); i++) {
                JSONArray coordinate = coordinates.getJSONArray(i);
                double longitude = coordinate.getDouble(0);
                double latitude = coordinate.getDouble(1);
                points.add(Point.fromLngLat(longitude, latitude));
            }

            return new RouteData(points, distanceMeters);
        }
    }

    private String buildEstimatedTimeLabel(double distanceMeters) {
        if (distanceMeters <= 0) {
            return getString(R.string.route_time_idle);
        }

        double estimatedMinutes = distanceMeters / 1000.0;
        return getString(R.string.route_time_format, estimatedMinutes);
    }

    private TripResponse executeRequest(Request request) throws IOException, JSONException {
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response: " + response);
            }
            if (response.body() == null) {
                throw new IOException("Respuesta vacía del servidor");
            }
            String json = response.body().string();
            return TripResponse.fromJson(json);
        }
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

        networkExecutor.execute(() -> {
            try {
                JSONObject body = new JSONObject();
                if (driverPosition != null) {
                    body.put("latitude", driverPosition.latitude());
                    body.put("longitude", driverPosition.longitude());
                }

                Request request = new Request.Builder()
                        .url(apiBaseUrl + "/api/Trips/" + tripId + "/start")
                        .post(RequestBody.create(body.toString(), JSON_MEDIA_TYPE))
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "";
                        throw new IOException("Error starting trip: " + response.code() + " " + errorBody);
                    }

                    if (response.body() == null) throw new IOException("Respuesta vacía del servidor");
                    String json = response.body().string();
                    TripResponse tripResponse = TripResponse.fromJson(json);

                    runOnUiThread(() -> {
                        Toast.makeText(this, R.string.toast_trip_started, Toast.LENGTH_SHORT).show();
                        lastTripStatusLabel = tripResponse.status;
                        activeTripAvailableSeats = tripResponse.availableSeats;
                        updateStatusText();
                        refreshButtons();
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error starting trip", e);
                runOnUiThread(() -> Toast.makeText(this, R.string.toast_trip_start_failed, Toast.LENGTH_SHORT).show());
            } finally {
                runOnUiThread(() -> {
                    setProgressVisible(false);
                    refreshButtons();
                });
            }
        });
    }

    private void finishTrip() {
        if (activeTripId == null) return;

        setProgressVisible(true);
        finishTripButton.setEnabled(false);

        final String tripId = activeTripId;
        networkExecutor.execute(() -> {
            try {
                Request request = new Request.Builder()
                        .url(apiBaseUrl + "/api/Trips/" + tripId + "/finish")
                        .post(RequestBody.create("{}", JSON_MEDIA_TYPE))
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "";
                        throw new IOException("Error finishing trip: " + response.code() + " " + errorBody);
                    }

                    if (response.body() == null) throw new IOException("Respuesta vacía del servidor");
                    String json = response.body().string();
                    TripResponse tripResponse = TripResponse.fromJson(json);

                    runOnUiThread(() -> {
                        Toast.makeText(this, R.string.toast_trip_finished, Toast.LENGTH_SHORT).show();
                        lastTripStatusLabel = tripResponse.status;
                        activeTripAvailableSeats = tripResponse.availableSeats;
                        updateStatusText();
                        refreshButtons();
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error finishing trip", e);
                runOnUiThread(() -> Toast.makeText(this, R.string.toast_trip_finish_failed, Toast.LENGTH_SHORT).show());
            } finally {
                runOnUiThread(() -> {
                    setProgressVisible(false);
                    refreshButtons();
                });
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
        if (networkExecutor != null) {
            networkExecutor.shutdownNow();
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
        networkExecutor.execute(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("passengerName", passengerName);
                
                Request request = new Request.Builder()
                        .url(apiBaseUrl + "/api/Trips/" + activeTripId + "/Reservations")
                        .post(RequestBody.create(body.toString(), JSON_MEDIA_TYPE))
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "";
                        throw new IOException("Error: " + response.code() + " " + errorBody);
                    }
                    runOnUiThread(() -> {
                        Toast.makeText(this, R.string.toast_reservation_created, Toast.LENGTH_SHORT).show();
                        activeTripAvailableSeats--;
                        updateStatusText();
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error creating reservation", e);
                runOnUiThread(() -> Toast.makeText(this, R.string.toast_reservation_failed, Toast.LENGTH_SHORT).show());
            } finally {
                runOnUiThread(() -> setProgressVisible(false));
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
        networkExecutor.execute(() -> {
            try {
                Request request = new Request.Builder()
                        .url(apiBaseUrl + "/api/Trips/" + activeTripId + "/Reservations")
                        .get()
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) throw new IOException("Error fetching reservations");
                    String json = response.body().string();
                    JSONArray array = new JSONArray(json);
                    
                    String targetReservationId = null;
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);
                        if (obj.getString("passengerName").equalsIgnoreCase(passengerName)) {
                            targetReservationId = obj.getString("id");
                            break;
                        }
                    }

                    if (targetReservationId != null) {
                        final String idToCancel = targetReservationId;
                        runOnUiThread(() -> executeCancelReservation(idToCancel));
                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(this, "No se encontró reserva para: " + passengerName, Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error finding reservation", e);
                runOnUiThread(() -> Toast.makeText(this, R.string.toast_network_error, Toast.LENGTH_SHORT).show());
            } finally {
                runOnUiThread(() -> setProgressVisible(false));
            }
        });
    }

    private void viewReservations() {
        if (activeTripId == null) return;
        setProgressVisible(true);
        networkExecutor.execute(() -> {
            try {
                Request request = new Request.Builder()
                        .url(apiBaseUrl + "/api/Trips/" + activeTripId + "/Reservations")
                        .get()
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) throw new IOException("Error fetching reservations");
                    String json = response.body().string();
                    JSONArray array = new JSONArray(json);
                    List<ReservationResponse> reservations = new ArrayList<>();
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);
                        reservations.add(new ReservationResponse(
                                obj.getString("id"),
                                obj.getString("passengerName"),
                                obj.getString("status")
                        ));
                    }
                    runOnUiThread(() -> showReservationsDialog(reservations));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error viewing reservations", e);
                runOnUiThread(() -> Toast.makeText(this, R.string.toast_network_error, Toast.LENGTH_SHORT).show());
            } finally {
                runOnUiThread(() -> setProgressVisible(false));
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
        networkExecutor.execute(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("passengerName", passengerName);

                Request request = new Request.Builder()
                        .url(apiBaseUrl + "/api/Trips/" + activeTripId + "/Reservations/board")
                        .post(RequestBody.create(body.toString(), JSON_MEDIA_TYPE))
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "";
                        throw new IOException("Error validating boarding: " + response.code() + " " + errorBody);
                    }

                    runOnUiThread(() -> {
                        Toast.makeText(this, R.string.toast_boarding_confirmed, Toast.LENGTH_SHORT).show();
                        viewReservations();
                        viewBoardedPassengers();
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error validating boarding by name", e);
                runOnUiThread(() -> Toast.makeText(this, R.string.toast_boarding_failed, Toast.LENGTH_SHORT).show());
            } finally {
                runOnUiThread(() -> setProgressVisible(false));
            }
        });
    }

    private void updatePassengerStatusManual(ReservationResponse reservation, String targetStatus, boolean refreshBoardedList) {
        setProgressVisible(true);
        networkExecutor.execute(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("status", targetStatus);

                Request request = new Request.Builder()
                        .url(apiBaseUrl + "/api/Trips/" + activeTripId + "/Reservations/" + reservation.id + "/manual-status")
                        .patch(RequestBody.create(body.toString(), JSON_MEDIA_TYPE))
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "";
                        throw new IOException("Error updating status: " + response.code() + " " + errorBody);
                    }

                    runOnUiThread(() -> {
                        Toast.makeText(this, R.string.toast_manual_status_updated, Toast.LENGTH_SHORT).show();
                        if (refreshBoardedList) {
                            viewBoardedPassengers();
                        } else {
                            viewReservations();
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating passenger status manually", e);
                runOnUiThread(() -> Toast.makeText(this, R.string.toast_manual_status_failed, Toast.LENGTH_SHORT).show());
            } finally {
                runOnUiThread(() -> setProgressVisible(false));
            }
        });
    }

    private void viewBoardedPassengers() {
        if (activeTripId == null) return;
        setProgressVisible(true);
        networkExecutor.execute(() -> {
            try {
                Request request = new Request.Builder()
                        .url(apiBaseUrl + "/api/Trips/" + activeTripId + "/Reservations/boarded")
                        .get()
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) throw new IOException("Error fetching boarded passengers");
                    String json = response.body().string();
                    JSONArray array = new JSONArray(json);
                    List<ReservationResponse> boarded = new ArrayList<>();

                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);
                        boarded.add(new ReservationResponse(
                                obj.getString("id"),
                                obj.getString("passengerName"),
                                obj.getString("status")
                        ));
                    }

                    runOnUiThread(() -> showBoardedPassengersDialog(boarded));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error viewing boarded passengers", e);
                runOnUiThread(() -> Toast.makeText(this, R.string.toast_network_error, Toast.LENGTH_SHORT).show());
            } finally {
                runOnUiThread(() -> setProgressVisible(false));
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
        networkExecutor.execute(() -> {
            try {
                Request request = new Request.Builder()
                        .url(apiBaseUrl + "/api/Reservations/" + reservationId)
                        .delete()
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) throw new IOException("Error cancelling reservation");
                    runOnUiThread(() -> {
                        Toast.makeText(this, R.string.toast_reservation_cancelled, Toast.LENGTH_SHORT).show();
                        activeTripAvailableSeats++;
                        updateStatusText();
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error cancelling reservation", e);
                runOnUiThread(() -> Toast.makeText(this, R.string.toast_reservation_failed, Toast.LENGTH_SHORT).show());
            } finally {
                runOnUiThread(() -> setProgressVisible(false));
            }
        });
    }

    private static class ReservationResponse {
        final String id;
        final String passengerName;
        final String status;

        ReservationResponse(String id, String passengerName, String status) {
            this.id = id;
            this.passengerName = passengerName;
            this.status = status;
        }

        @Override
        public String toString() {
            return passengerName + " (" + status + ")";
        }
    }

    private static class TripResponse {
        final String id;
        final double originLatitude;
        final double originLongitude;
        final Double destinationLatitude;
        final Double destinationLongitude;
        final String status;
        final int availableSeats;

        TripResponse(String id,
                     double originLatitude,
                     double originLongitude,
                     Double destinationLatitude,
                     Double destinationLongitude,
                     String status,
                     int availableSeats) {
            this.id = id;
            this.originLatitude = originLatitude;
            this.originLongitude = originLongitude;
            this.destinationLatitude = destinationLatitude;
            this.destinationLongitude = destinationLongitude;
            this.status = status;
            this.availableSeats = availableSeats;
        }

        static TripResponse fromJson(String json) throws JSONException {
            JSONObject object = new JSONObject(json);
            Double destinationLat = object.isNull("destinationLatitude") ? null : object.getDouble("destinationLatitude");
            Double destinationLng = object.isNull("destinationLongitude") ? null : object.getDouble("destinationLongitude");

            Object statusValue = object.has("status") ? object.get("status") : null;
            String statusLabel = mapTripStatusToLabel(statusValue);

            return new TripResponse(
                    object.getString("id"),
                    object.getDouble("originLatitude"),
                    object.getDouble("originLongitude"),
                    destinationLat,
                    destinationLng,
                    statusLabel,
                    object.optInt("availableSeats", 0)
            );
        }

        private static String mapTripStatusToLabel(Object statusValue) {
            if (statusValue == null || statusValue == JSONObject.NULL) return "";

            if (statusValue instanceof Number) {
                int s = ((Number) statusValue).intValue();
                if (s == 0) return "activo";
                if (s == 1) return "listo";
                if (s == 2) return "cancelado";
                if (s == 3) return "en curso";
                if (s == 4) return "finalizado";
                return String.valueOf(s);
            }

            String normalized = String.valueOf(statusValue).trim().toLowerCase();
            if (normalized.equals("0") || normalized.equals("awaitingdestination") || normalized.equals("activo")) return "activo";
            if (normalized.equals("1") || normalized.equals("ready") || normalized.equals("listo")) return "listo";
            if (normalized.equals("2") || normalized.equals("cancelled") || normalized.equals("cancelado")) return "cancelado";
            if (normalized.equals("3") || normalized.equals("inprogress") || normalized.equals("in_progress") || normalized.equals("en_curso") || normalized.equals("en curso")) return "en curso";
            if (normalized.equals("4") || normalized.equals("finished") || normalized.equals("completed") || normalized.equals("finalizado")) return "finalizado";

            // Fallback: usa lo que venga desde el servidor.
            return String.valueOf(statusValue);
        }
    }

    private static class RouteData {
        final List<Point> points;
        final double distanceMeters;

        RouteData(List<Point> points, double distanceMeters) {
            this.points = points;
            this.distanceMeters = distanceMeters;
        }
    }
}