package com.example.proyectocarpooling.presentation.match.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proyectocarpooling.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DriverMatchActivity extends AppCompatActivity {

    public static final String EXTRA_DESTINATION_LABEL = "extra_destination_label";

    private TextView destinationSummaryText;
    private TextView driverNameText;
    private TextView driverRouteText;
    private TextView driverSeatsText;
    private TextView driverDistanceText;
    private TextView driverTimeText;
    private TextView cardCounterText;
    private Button rejectButton;
    private Button acceptButton;

    private final List<DriverCandidate> candidates = new ArrayList<>();
    private int currentIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_match);

        destinationSummaryText = findViewById(R.id.destinationSummaryText);
        driverNameText = findViewById(R.id.driverNameText);
        driverRouteText = findViewById(R.id.driverRouteText);
        driverSeatsText = findViewById(R.id.driverSeatsText);
        driverDistanceText = findViewById(R.id.driverDistanceText);
        driverTimeText = findViewById(R.id.driverTimeText);
        cardCounterText = findViewById(R.id.cardCounterText);
        rejectButton = findViewById(R.id.rejectDriverButton);
        acceptButton = findViewById(R.id.acceptDriverButton);

        String destinationLabel = getIntent().getStringExtra(EXTRA_DESTINATION_LABEL);
        if (destinationLabel == null || destinationLabel.trim().isEmpty()) {
            destinationLabel = getString(R.string.driver_match_default_destination);
        }
        destinationSummaryText.setText(getString(R.string.driver_match_destination_summary, destinationLabel));

        setTitle(R.string.driver_match_title);

        loadMockCandidates();
        renderCurrentCard();

        rejectButton.setOnClickListener(v -> swipeLeft());
        acceptButton.setOnClickListener(v -> swipeRight());
    }

    private void loadMockCandidates() {
        candidates.clear();
        candidates.add(new DriverCandidate("Carlos Rojas", "Villa Esperanza -> Universidad", 3, 0.8, 5));
        candidates.add(new DriverCandidate("Andrea Ruiz", "Los Pinos -> Universidad", 2, 1.4, 8));
        candidates.add(new DriverCandidate("Miguel Soto", "Centro -> Universidad", 1, 2.1, 11));
        candidates.add(new DriverCandidate("Daniela Choque", "Plan 3000 -> Universidad", 4, 2.9, 14));
        // Ya vienen en orden por cercanía para simular prioridad por distancia a tu ruta.
    }

    private void swipeLeft() {
        if (currentIndex >= candidates.size()) {
            return;
        }
        DriverCandidate rejected = candidates.get(currentIndex);
        Toast.makeText(this, getString(R.string.driver_match_rejected, rejected.driverName), Toast.LENGTH_SHORT).show();
        currentIndex++;
        renderCurrentCard();
    }

    private void swipeRight() {
        if (currentIndex >= candidates.size()) {
            return;
        }
        DriverCandidate accepted = candidates.get(currentIndex);
        Toast.makeText(this, getString(R.string.driver_match_accepted, accepted.driverName), Toast.LENGTH_SHORT).show();
        finish();
    }

    private void renderCurrentCard() {
        if (currentIndex >= candidates.size()) {
            driverNameText.setText(R.string.driver_match_no_results_title);
            driverRouteText.setText(R.string.driver_match_no_results_subtitle);
            driverSeatsText.setText("");
            driverDistanceText.setText("");
            driverTimeText.setText("");
            cardCounterText.setText(R.string.driver_match_no_results_counter);
            rejectButton.setEnabled(false);
            acceptButton.setEnabled(false);
            return;
        }

        DriverCandidate current = candidates.get(currentIndex);
        driverNameText.setText(current.driverName);
        driverRouteText.setText(getString(R.string.driver_match_route_format, current.routeDescription));
        driverSeatsText.setText(getString(R.string.driver_match_seats_format, current.availableSeats));
        driverDistanceText.setText(getString(R.string.driver_match_distance_format,
                String.format(Locale.US, "%.1f", current.distanceKm)));
        driverTimeText.setText(getString(R.string.driver_match_time_format, current.etaMinutes));
        cardCounterText.setText(getString(R.string.driver_match_counter_format, currentIndex + 1, candidates.size()));
        rejectButton.setEnabled(true);
        acceptButton.setEnabled(true);
    }

    private static class DriverCandidate {
        private final String driverName;
        private final String routeDescription;
        private final int availableSeats;
        private final double distanceKm;
        private final int etaMinutes;

        private DriverCandidate(
                String driverName,
                String routeDescription,
                int availableSeats,
                double distanceKm,
                int etaMinutes) {
            this.driverName = driverName;
            this.routeDescription = routeDescription;
            this.availableSeats = availableSeats;
            this.distanceKm = distanceKm;
            this.etaMinutes = etaMinutes;
        }
    }
}
