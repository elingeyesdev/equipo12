package com.example.proyectocarpooling.presentation.schedules.ui;

import android.Manifest;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.example.proyectocarpooling.R;
import com.example.proyectocarpooling.data.remote.search.MapboxGeocodingRemoteDataSource;
import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.MapView;
import com.mapbox.maps.Style;
import com.mapbox.maps.plugin.Plugin;
import com.mapbox.maps.plugin.annotation.AnnotationPlugin;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions;
import com.mapbox.maps.plugin.gestures.GesturesUtils;
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin;
import com.mapbox.maps.plugin.locationcomponent.LocationComponentUtils;
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener;

import org.json.JSONException;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MapPickerActivity extends AppCompatActivity {

    public static final String EXTRA_RESULT_ADDRESS = "result_address";
    public static final String EXTRA_RESULT_LATITUDE = "result_latitude";
    public static final String EXTRA_RESULT_LONGITUDE = "result_longitude";

    private Toolbar toolbar;
    private MapView mapView;
    private TextView tvSelectedAddress;
    private Button btnCancel;
    private Button btnConfirm;

    private MapboxGeocodingRemoteDataSource geocoder;
    private ExecutorService backgroundExecutor;

    private PointAnnotationManager pointAnnotationManager;
    private PointAnnotation selectionAnnotation;
    private Bitmap markerBitmap;

    private Point selectedPoint;
    private String selectedAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_picker);

        geocoder = new MapboxGeocodingRemoteDataSource(getString(R.string.mapbox_access_token));
        backgroundExecutor = Executors.newSingleThreadExecutor();

        bindViews();
        setupToolbar();
        initializeMap();
    }

    private void bindViews() {
        toolbar = findViewById(R.id.mapPickerToolbar);
        mapView = findViewById(R.id.mapPickerView);
        tvSelectedAddress = findViewById(R.id.tvSelectedAddress);
        btnCancel = findViewById(R.id.btnCancelSelection);
        btnConfirm = findViewById(R.id.btnConfirmSelection);

        btnCancel.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        btnConfirm.setOnClickListener(v -> {
            if (selectedPoint != null && selectedAddress != null) {
                Intent data = new Intent();
                data.putExtra(EXTRA_RESULT_ADDRESS, selectedAddress);
                data.putExtra(EXTRA_RESULT_LATITUDE, selectedPoint.latitude());
                data.putExtra(EXTRA_RESULT_LONGITUDE, selectedPoint.longitude());
                setResult(RESULT_OK, data);
                finish();
            }
        });
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });
    }

    private final ActivityResultLauncher<String[]> locationPermissionRequest =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarseLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                if ((fineLocationGranted != null && fineLocationGranted) || (coarseLocationGranted != null && coarseLocationGranted)) {
                    setupLocationComponent();
                }
            });

    private void initializeMap() {
        mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS, style -> {
            initializeAnnotations();
            setupMapClickListener();
            
            mapView.getMapboxMap().setCamera(
                    new CameraOptions.Builder()
                            .center(Point.fromLngLat(-66.1568, -17.3895))
                            .zoom(15.0)
                            .build()
            );

            checkLocationPermissions();
        });
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

    private void setupLocationComponent() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationComponentPlugin locationComponentPlugin = LocationComponentUtils.getLocationComponent(mapView);
            locationComponentPlugin.setEnabled(true);
            locationComponentPlugin.addOnIndicatorPositionChangedListener(new OnIndicatorPositionChangedListener() {
                private boolean initialPositionSet = false;
                @Override
                public void onIndicatorPositionChanged(Point point) {
                    if (!initialPositionSet) {
                        initialPositionSet = true;
                        runOnUiThread(() -> updateSelection(point));
                        locationComponentPlugin.removeOnIndicatorPositionChangedListener(this);
                    }
                }
            });
        }
    }

    private void initializeAnnotations() {
        AnnotationPlugin annotationPlugin = (AnnotationPlugin) mapView.getPlugin(Plugin.MAPBOX_ANNOTATION_PLUGIN_ID);
        if (annotationPlugin != null) {
            pointAnnotationManager = com.mapbox.maps.plugin.annotation.generated.PointAnnotationManagerKt.createPointAnnotationManager(annotationPlugin, null);
        }
        markerBitmap = createMarkerBitmap();
    }

    private void setupMapClickListener() {
        GesturesUtils.getGestures(mapView).addOnMapClickListener(point -> {
            updateSelection(point);
            return true;
        });
    }

    private void updateSelection(Point point) {
        if (pointAnnotationManager != null) {
            if (selectionAnnotation == null) {
                PointAnnotationOptions options = new PointAnnotationOptions()
                        .withPoint(point)
                        .withIconImage(markerBitmap);
                selectionAnnotation = pointAnnotationManager.create(options);
            } else {
                selectionAnnotation.setPoint(point);
                pointAnnotationManager.update(selectionAnnotation);
            }
        }

        mapView.getMapboxMap().setCamera(
                new CameraOptions.Builder()
                        .center(point)
                        .build()
        );

        tvSelectedAddress.setText("Obteniendo dirección...");
        btnConfirm.setEnabled(false);

        backgroundExecutor.execute(() -> {
            try {
                String address = geocoder.reverseGeocode(point.latitude(), point.longitude());
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (address != null && !address.trim().isEmpty()) {
                        selectedAddress = address.trim();
                    } else {
                        selectedAddress = String.format(Locale.US, "%.5f, %.5f", point.latitude(), point.longitude());
                    }
                    selectedPoint = point;
                    tvSelectedAddress.setText(selectedAddress);
                    btnConfirm.setEnabled(true);
                });
            } catch (IOException | JSONException e) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    selectedAddress = String.format(Locale.US, "%.5f, %.5f", point.latitude(), point.longitude());
                    selectedPoint = point;
                    tvSelectedAddress.setText(selectedAddress);
                    btnConfirm.setEnabled(true);
                });
            }
        });
    }

    private Bitmap createMarkerBitmap() {
        int size = 96;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(Color.parseColor("#E53935"));

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

    @Override
    protected void onDestroy() {
        if (backgroundExecutor != null) {
            backgroundExecutor.shutdownNow();
        }
        super.onDestroy();
    }
}
