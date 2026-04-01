package com.example.proyectocarpooling;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.mapbox.android.gestures.MoveGestureDetector;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.MapView;
import com.mapbox.maps.Style;
import com.mapbox.maps.plugin.gestures.GesturesUtils;
import com.mapbox.maps.plugin.gestures.OnMoveListener;
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin;
import com.mapbox.maps.plugin.locationcomponent.LocationComponentUtils;
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorBearingChangedListener;
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener;

public class MainActivity extends AppCompatActivity {

    private MapView mapView;
    private ProgressBar loadingIndicator;
    private boolean isInitialPositionSet = false;

    private final ActivityResultLauncher<String[]> locationPermissionRequest =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarseLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                if (fineLocationGranted != null && fineLocationGranted) {
                    setupLocationComponent();
                } else if (coarseLocationGranted != null && coarseLocationGranted) {
                    setupLocationComponent();
                } else {
                    if (loadingIndicator != null) loadingIndicator.setVisibility(View.GONE);
                    Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
                }
            });

    private final OnIndicatorPositionChangedListener onIndicatorPositionChangedListener = point -> {
        if (!isInitialPositionSet) {
            mapView.getMapboxMap().setCamera(new CameraOptions.Builder()
                    .center(point)
                    .zoom(16.0)
                    .build());
            isInitialPositionSet = true;

            if (loadingIndicator != null) loadingIndicator.setVisibility(View.GONE);
        } else {
            mapView.getMapboxMap().setCamera(new CameraOptions.Builder()
                    .center(point)
                    .build());
        }
        GesturesUtils.getGestures(mapView).setFocalPoint(mapView.getMapboxMap().pixelForCoordinate(point));
    };

    private final OnIndicatorBearingChangedListener onIndicatorBearingChangedListener = bearing -> {
        mapView.getMapboxMap().setCamera(new CameraOptions.Builder().bearing(bearing).build());
    };

    private final OnMoveListener onMoveListener = new OnMoveListener() {
        @Override
        public void onMoveBegin(@NonNull MoveGestureDetector detector) {
            onCameraTrackingDismissed();
        }

        @Override
        public boolean onMove(@NonNull MoveGestureDetector detector) {
            return false;
        }

        @Override
        public void onMoveEnd(@NonNull MoveGestureDetector detector) {}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        mapView = findViewById(R.id.mapView);
        loadingIndicator = findViewById(R.id.loadingIndicator);

        if (mapView != null) {
            mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS, style -> {
                checkLocationPermissions();
            });
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
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
        LocationComponentPlugin locationComponentPlugin = LocationComponentUtils.getLocationComponent(mapView);
        locationComponentPlugin.setEnabled(true);
        locationComponentPlugin.addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener);
        locationComponentPlugin.addOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener);
        
        GesturesUtils.getGestures(mapView).addOnMoveListener(onMoveListener);
    }

    private void onCameraTrackingDismissed() {
        LocationComponentPlugin locationComponentPlugin = LocationComponentUtils.getLocationComponent(mapView);
        locationComponentPlugin.removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener);
        locationComponentPlugin.removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener);
        GesturesUtils.getGestures(mapView).removeOnMoveListener(onMoveListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        onCameraTrackingDismissed();
    }
}