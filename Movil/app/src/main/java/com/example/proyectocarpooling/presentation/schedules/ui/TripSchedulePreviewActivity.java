package com.example.proyectocarpooling.presentation.schedules.ui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;

import com.example.proyectocarpooling.CarPoolingApplication;
import com.example.proyectocarpooling.R;
import com.example.proyectocarpooling.data.local.SessionManager;
import com.example.proyectocarpooling.data.model.trip.TripSchedule;
import com.example.proyectocarpooling.presentation.BaseActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.MapView;
import com.mapbox.maps.Style;
import com.mapbox.maps.plugin.Plugin;
import com.mapbox.maps.plugin.annotation.AnnotationPlugin;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions;
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotation;
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManager;
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TripSchedulePreviewActivity extends BaseActivity {

    private SessionManager sessionManager;
    private TripSchedulesViewModel viewModel;
    private ExecutorService backgroundExecutor;

    private MaterialToolbar toolbar;
    private MapView mapView;
    private TextView tvTime;
    private TextView tvDays;
    private TextView tvDriver;
    private TextView tvDetails;
    private MaterialButton btnSubscribe;

    private PointAnnotationManager pointAnnotationManager;
    private PolylineAnnotationManager polylineAnnotationManager;
    private PolylineAnnotation routeAnnotation;

    private String scheduleId;
    private String driverId;
    private String driverName;
    private double originLat;
    private double originLng;
    private String originAddress;
    private double destLat;
    private double destLng;
    private String destAddress;
    private String time;
    private String days;
    private int seats;
    private double fare;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_schedule_preview);

        sessionManager = new SessionManager(this);
        viewModel = new ViewModelProvider(this).get(TripSchedulesViewModel.class);
        backgroundExecutor = Executors.newSingleThreadExecutor();

        readIntentExtras();
        bindViews();
        setupToolbar();
        bindDataToViews();
        initializeMap();
        observeViewModel();
    }

    private void readIntentExtras() {
        scheduleId = getIntent().getStringExtra("EXTRA_SCHEDULE_ID");
        driverId = getIntent().getStringExtra("EXTRA_DRIVER_ID");
        driverName = getIntent().getStringExtra("EXTRA_DRIVER_NAME");
        originLat = getIntent().getDoubleExtra("EXTRA_ORIGIN_LAT", 0.0);
        originLng = getIntent().getDoubleExtra("EXTRA_ORIGIN_LNG", 0.0);
        originAddress = getIntent().getStringExtra("EXTRA_ORIGIN_ADDRESS");
        destLat = getIntent().getDoubleExtra("EXTRA_DEST_LAT", 0.0);
        destLng = getIntent().getDoubleExtra("EXTRA_DEST_LNG", 0.0);
        destAddress = getIntent().getStringExtra("EXTRA_DEST_ADDRESS");
        time = getIntent().getStringExtra("EXTRA_TIME");
        days = getIntent().getStringExtra("EXTRA_DAYS");
        seats = getIntent().getIntExtra("EXTRA_SEATS", 4);
        fare = getIntent().getDoubleExtra("EXTRA_FARE", 10.0);
    }

    private void bindViews() {
        toolbar = findViewById(R.id.previewToolbar);
        mapView = findViewById(R.id.mapViewPreview);
        tvTime = findViewById(R.id.tvPreviewTime);
        tvDays = findViewById(R.id.tvPreviewDays);
        tvDriver = findViewById(R.id.tvPreviewDriver);
        tvDetails = findViewById(R.id.tvPreviewDetails);
        btnSubscribe = findViewById(R.id.btnSubscribe);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void bindDataToViews() {
        tvTime.setText(time);
        tvDays.setText(formatDaysOfWeek(days));
        tvDriver.setText("Conductor: " + (driverName != null ? driverName : ""));
        tvDetails.setText("Asientos: " + seats + " | Tarifa: " + fare + " Bs.");

        boolean previewOnly = getIntent().getBooleanExtra("EXTRA_PREVIEW_ONLY", false);
        if (previewOnly) {
            btnSubscribe.setVisibility(View.GONE);
        } else {
            btnSubscribe.setVisibility(View.VISIBLE);
            btnSubscribe.setOnClickListener(v -> {
                if (scheduleId != null) {
                    TripSchedule dummy = new TripSchedule(
                            scheduleId, driverId, driverName,
                            originLat, originLng, originAddress,
                            destLat, destLng, destAddress,
                            time, days, "", null, null, seats, fare, true
                    );
                    viewModel.subscribeToSchedule(dummy, sessionManager.getUserId(), 1);
                }
            });
        }
    }

    private void initializeMap() {
        mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS, style -> {
            initializeAnnotations();
            drawMarkers();
            fetchAndDrawRoutePreview();
        });
    }

    private void initializeAnnotations() {
        AnnotationPlugin annotationPlugin = (AnnotationPlugin) mapView.getPlugin(Plugin.MAPBOX_ANNOTATION_PLUGIN_ID);
        if (annotationPlugin != null) {
            pointAnnotationManager = com.mapbox.maps.plugin.annotation.generated.PointAnnotationManagerKt.createPointAnnotationManager(annotationPlugin, null);
            polylineAnnotationManager = com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManagerKt.createPolylineAnnotationManager(annotationPlugin, null);
        }
    }

    private void drawMarkers() {
        if (pointAnnotationManager == null) return;

        Bitmap originBitmap = createMarkerBitmap(Color.parseColor("#1E88E5"));
        Bitmap destBitmap = createMarkerBitmap(Color.parseColor("#E53935"));

        PointAnnotationOptions originOptions = new PointAnnotationOptions()
                .withPoint(Point.fromLngLat(originLng, originLat))
                .withIconImage(originBitmap);
        pointAnnotationManager.create(originOptions);

        PointAnnotationOptions destOptions = new PointAnnotationOptions()
                .withPoint(Point.fromLngLat(destLng, destLat))
                .withIconImage(destBitmap);
        pointAnnotationManager.create(destOptions);
    }

    private void fetchAndDrawRoutePreview() {
        Point origin = Point.fromLngLat(originLng, originLat);
        Point destination = Point.fromLngLat(destLng, destLat);

        backgroundExecutor.execute(() -> {
            try {
                CarPoolingApplication app = (CarPoolingApplication) getApplication();
                com.example.proyectocarpooling.data.model.trip.RouteData route = app.getTripRepository().fetchRoute(origin, destination);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    drawRoute(route.points);
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error al trazar la ruta en el mapa", Toast.LENGTH_SHORT).show();
                    // Fallback zoom to origin
                    mapView.getMapboxMap().setCamera(new CameraOptions.Builder()
                            .center(origin)
                            .zoom(13.0)
                            .build());
                });
            }
        });
    }

    private void drawRoute(List<Point> routePoints) {
        if (polylineAnnotationManager == null || routePoints == null || routePoints.size() < 2) {
            return;
        }

        if (routeAnnotation != null) {
            polylineAnnotationManager.delete(routeAnnotation);
        }

        PolylineAnnotationOptions options = new PolylineAnnotationOptions()
                .withPoints(routePoints)
                .withLineColor("#1E88E5")
                .withLineWidth(7.0)
                .withLineOpacity(0.9);

        routeAnnotation = polylineAnnotationManager.create(options);

        // Center camera
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

    private double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double theta = lon1 - lon2;
        double dist = Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2))
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(theta));
        dist = Math.acos(dist);
        dist = Math.toDegrees(dist);
        dist = dist * 60 * 1.1515 * 1.609344;
        return dist;
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

    private String formatDaysOfWeek(String daysCommaSeparated) {
        if (daysCommaSeparated == null || daysCommaSeparated.isEmpty()) return "Ninguno";
        String[] days = daysCommaSeparated.split(",");
        StringBuilder sb = new StringBuilder();
        for (String day : days) {
            if (sb.length() > 0) sb.append(", ");
            switch (day.trim()) {
                case "1": sb.append("Lun"); break;
                case "2": sb.append("Mar"); break;
                case "3": sb.append("Mié"); break;
                case "4": sb.append("Jue"); break;
                case "5": sb.append("Vie"); break;
                case "6": sb.append("Sáb"); break;
                case "0": sb.append("Dom"); break;
                default: sb.append("Día ").append(day);
            }
        }
        return sb.toString();
    }

    private void observeViewModel() {
        viewModel.getSuccessMessage().observe(this, msg -> {
            if (msg != null) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, sanitizeError(error), Toast.LENGTH_LONG).show();
            }
        });

        viewModel.getLoading().observe(this, loading -> {
            btnSubscribe.setEnabled(!loading);
            btnSubscribe.setText(loading ? "Procesando..." : "Suscribirse a esta Ruta");
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mapView != null) mapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mapView != null) mapView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mapView != null) mapView.onDestroy();
        backgroundExecutor.shutdown();
    }
}
