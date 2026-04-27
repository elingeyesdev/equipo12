package com.example.proyectocarpooling.presentation.history.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.proyectocarpooling.R;
import com.example.proyectocarpooling.data.local.ApiBaseUrlProvider;
import com.example.proyectocarpooling.data.local.SessionManager;
import com.example.proyectocarpooling.data.model.history.TripHistoryDetailItem;
import com.example.proyectocarpooling.data.model.history.TripHistoryParticipantItem;
import com.example.proyectocarpooling.data.remote.user.TripHistoryRemoteDataSource;
import com.example.proyectocarpooling.presentation.main.ui.MainActivity;
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
    private Button viewRouteButton;
    private Button participantsButton;
    private TripHistoryDetailItem currentDetail;
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
        viewRouteButton = findViewById(R.id.historyDetailViewRouteButton);
        participantsButton = findViewById(R.id.historyDetailParticipantsButton);
        if (viewRouteButton != null) {
            viewRouteButton.setEnabled(false);
            viewRouteButton.setOnClickListener(v -> openMapWithRoute());
        }
        if (participantsButton != null) {
            participantsButton.setEnabled(false);
            participantsButton.setOnClickListener(v -> showParticipantsDialog());
        }

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
        currentDetail = d;
        String content = String.format(Locale.getDefault(),
                "Categoria: %s\n" +
                        "Estado: %s\n" +
                        "Creado: %s\n" +
                        "Inicio real: %s\n" +
                        "Finalizacion real: %s\n" +
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
                emptyIfBlank(d.startedAt, "N/D"),
                emptyIfBlank(d.finishedAt, "N/D"),
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
        boolean hasRoute = d.destinationLatitude != null && d.destinationLongitude != null;
        if (viewRouteButton != null) {
            viewRouteButton.setEnabled(hasRoute);
        }
        if (participantsButton != null) {
            participantsButton.setEnabled(d.participants != null && !d.participants.isEmpty());
        }
    }

    private void openMapWithRoute() {
        TripHistoryDetailItem d = currentDetail;
        if (d == null || d.destinationLatitude == null || d.destinationLongitude == null) {
            Toast.makeText(this, R.string.history_detail_no_route, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(MainActivity.EXTRA_APPLY_FAVORITE_KIND, "route");
        intent.putExtra(MainActivity.EXTRA_APPLY_ORIGIN_LAT, d.originLatitude);
        intent.putExtra(MainActivity.EXTRA_APPLY_ORIGIN_LNG, d.originLongitude);
        intent.putExtra(MainActivity.EXTRA_APPLY_DEST_LAT, d.destinationLatitude);
        intent.putExtra(MainActivity.EXTRA_APPLY_DEST_LNG, d.destinationLongitude);
        intent.putExtra(MainActivity.EXTRA_HISTORY_ROUTE_PREVIEW, true);
        startActivity(intent);
        finish();
    }

    private void showParticipantsDialog() {
        TripHistoryDetailItem d = currentDetail;
        if (d == null || d.participants == null || d.participants.isEmpty()) {
            Toast.makeText(this, R.string.history_detail_no_participants, Toast.LENGTH_SHORT).show();
            return;
        }
        String[] rows = new String[d.participants.size()];
        for (int i = 0; i < d.participants.size(); i++) {
            TripHistoryParticipantItem p = d.participants.get(i);
            rows[i] = String.format(Locale.getDefault(), "%s · %s · %s",
                    emptyIfBlank(p.name, "Participante"),
                    emptyIfBlank(p.statusLabel, "N/D"),
                    emptyIfBlank(p.reservedAt, "N/D"));
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.history_detail_participants_title)
                .setItems(rows, null)
                .setPositiveButton(android.R.string.ok, null)
                .show();
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
