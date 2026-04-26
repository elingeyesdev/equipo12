package com.example.proyectocarpooling.presentation.history.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.proyectocarpooling.R;
import com.example.proyectocarpooling.data.local.ApiBaseUrlProvider;
import com.example.proyectocarpooling.data.local.SessionManager;
import com.example.proyectocarpooling.data.model.history.TripHistoryDetailItem;
import com.example.proyectocarpooling.data.remote.user.TripHistoryRemoteDataSource;
import com.google.android.material.appbar.MaterialToolbar;

import org.json.JSONException;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TripHistoryDetailActivity extends AppCompatActivity {
    public static final String EXTRA_TRIP_ID = "extra_trip_id";

    private MaterialToolbar toolbar;
    private ProgressBar progress;
    private TextView detailText;
    private SessionManager sessionManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_history_detail);

        String tripId = getIntent().getStringExtra(EXTRA_TRIP_ID);
        if (tripId == null || tripId.isBlank()) {
            finish();
            return;
        }

        sessionManager = new SessionManager(this);
        toolbar = findViewById(R.id.historyDetailToolbar);
        progress = findViewById(R.id.historyDetailProgress);
        detailText = findViewById(R.id.historyDetailText);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        loadDetail(tripId);
    }

    private void loadDetail(String tripId) {
        String userId = sessionManager.getUserId();
        if (userId == null || userId.isBlank()) {
            finish();
            return;
        }
        progress.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            try {
                TripHistoryRemoteDataSource api = new TripHistoryRemoteDataSource(ApiBaseUrlProvider.get(this));
                TripHistoryDetailItem d = api.getHistoryDetail(userId, tripId, sessionManager.getFullName());
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    renderDetail(d);
                });
            } catch (IOException | JSONException e) {
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(this, R.string.history_load_error, Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    private void renderDetail(TripHistoryDetailItem d) {
        String content = String.format(Locale.getDefault(),
                "Categoria: %s\n" +
                        "Estado: %s\n" +
                        "Creado: %s\n" +
                        "Actualizado: %s\n\n" +
                        "Origen: %s\n" +
                        "Destino: %s\n" +
                        "Coordenadas origen: %.6f, %.6f\n" +
                        "Coordenadas destino: %s\n\n" +
                        "Conductor: %s\n" +
                        "Auto: %s %s\n" +
                        "Placa: %s\n\n" +
                        "Reservas totales: %d\n" +
                        "Pasajeros abordados: %d\n" +
                        "Reservas canceladas: %d\n\n" +
                        "Tu estado de reserva: %s\n" +
                        "Pasajero: %s",
                d.category,
                d.statusLabel,
                d.createdAt,
                d.updatedAt,
                d.originLabel,
                d.destinationLabel,
                d.originLatitude,
                d.originLongitude,
                d.destinationLatitude == null || d.destinationLongitude == null
                        ? "Sin destino"
                        : String.format(Locale.getDefault(), "%.6f, %.6f", d.destinationLatitude, d.destinationLongitude),
                emptyIfBlank(d.driverName, "Conductor"),
                emptyIfBlank(d.driverVehicleBrand, "N/D"),
                emptyIfBlank(d.driverVehicleColor, "N/D"),
                emptyIfBlank(d.driverLicensePlate, "N/D"),
                d.reservationCount,
                d.boardedCount,
                d.cancelledCount,
                emptyIfBlank(d.passengerReservationStatus, "N/D"),
                emptyIfBlank(d.passengerName, "N/D"));

        detailText.setText(content);
    }

    private static String emptyIfBlank(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
