package com.example.proyectocarpooling.presentation.history.ui;

import com.example.proyectocarpooling.presentation.BaseActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyectocarpooling.CarPoolingApplication;
import com.example.proyectocarpooling.R;
import com.example.proyectocarpooling.data.local.SessionManager;
import com.example.proyectocarpooling.data.model.history.TripHistoryDetailItem;
import com.example.proyectocarpooling.presentation.history.HistoryUiHelper;
import com.example.proyectocarpooling.presentation.main.ui.MainActivity;
import com.example.proyectocarpooling.presentation.support.ui.SupportActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;

import java.util.Locale;

public class TripHistoryDetailActivity extends BaseActivity {
    public static final String EXTRA_TRIP_ID = "extra_trip_id";

    private MaterialToolbar toolbar;
    private View loadingOverlay;
    private MaterialButton reportButton, viewRouteButton, participantsButton;

    private TextView routeTitle, heroSubtitle, statusBadge, categoryLabel;
    private LinearLayout rowStarted, rowFinished;
    private TextView valueCreated, valueStarted, valueFinished, valueUpdated;
    private TextView originLine, destLine, coordsLine;
    private TextView driverNameText, vehicleLine, plateLine;
    private TextView statReservations, statBoarded, statCancelled;
    private View cardYou;
    private TextView youName, youReservation;

    private TripHistoryDetailItem currentDetail;
    private SessionManager sessionManager;
    private TripHistoryDetailViewModel viewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_history_detail);

        String tripId = getIntent().getStringExtra(EXTRA_TRIP_ID);
        if (tripId == null || tripId.isBlank()) {
            finish();
            return;
        }

        sessionManager = ((CarPoolingApplication) getApplication()).getSessionManager();
        viewModel = new ViewModelProvider(this).get(TripHistoryDetailViewModel.class);

        toolbar = findViewById(R.id.historyDetailToolbar);
        loadingOverlay = findViewById(R.id.historyDetailLoadingOverlay);

        routeTitle = findViewById(R.id.historyDetailRouteTitle);
        heroSubtitle = findViewById(R.id.historyDetailHeroSubtitle);
        statusBadge = findViewById(R.id.historyDetailStatusBadge);
        categoryLabel = findViewById(R.id.historyDetailCategoryLabel);

        valueCreated = findViewById(R.id.historyDetailValueCreated);
        valueStarted = findViewById(R.id.historyDetailValueStarted);
        valueFinished = findViewById(R.id.historyDetailValueFinished);
        valueUpdated = findViewById(R.id.historyDetailValueUpdated);
        rowStarted = findViewById(R.id.historyDetailRowStarted);
        rowFinished = findViewById(R.id.historyDetailRowFinished);

        originLine = findViewById(R.id.historyDetailOriginLine);
        destLine = findViewById(R.id.historyDetailDestLine);
        coordsLine = findViewById(R.id.historyDetailCoordsLine);

        driverNameText = findViewById(R.id.historyDetailDriverName);
        vehicleLine = findViewById(R.id.historyDetailVehicleLine);
        plateLine = findViewById(R.id.historyDetailPlateLine);

        statReservations = findViewById(R.id.historyDetailStatReservations);
        statBoarded = findViewById(R.id.historyDetailStatBoarded);
        statCancelled = findViewById(R.id.historyDetailStatCancelled);

        cardYou = findViewById(R.id.historyDetailCardYou);
        youName = findViewById(R.id.historyDetailYouName);
        youReservation = findViewById(R.id.historyDetailYouReservation);

        reportButton = findViewById(R.id.historyDetailReportButton);
        viewRouteButton = findViewById(R.id.historyDetailViewRouteButton);
        participantsButton = findViewById(R.id.historyDetailParticipantsButton);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        reportButton.setOnClickListener(v -> openSupportReport());

        viewRouteButton.setEnabled(false);
        viewRouteButton.setOnClickListener(v -> openMapWithRoute());

        participantsButton.setEnabled(false);
        participantsButton.setOnClickListener(v -> showParticipantsBottomSheet());

        observeViewModel();
        loadDetail(tripId);
    }

    private void observeViewModel() {
        viewModel.getDetail().observe(this, d -> {
            if (d != null) {
                loadingOverlay.setVisibility(View.GONE);
                toolbar.setSubtitle(shortTripIdLabel(d.tripId));
                renderDetail(d);
            }
        });

        viewModel.getErrorEvent().observe(this, error -> {
            if (error != null) {
                loadingOverlay.setVisibility(View.GONE);
                new AlertDialog.Builder(this)
                        .setTitle("Error de carga")
                        .setMessage(R.string.history_load_error)
                        .setPositiveButton("Aceptar", (d, w) -> finish())
                        .setCancelable(false)
                        .show();
            }
        });
    }

    private void loadDetail(String tripId) {
        String userId = sessionManager.getUserId();
        if (userId == null || userId.isBlank()) { finish(); return; }
        loadingOverlay.setVisibility(View.VISIBLE);
        viewRouteButton.setEnabled(false);
        participantsButton.setEnabled(false);
        viewModel.loadDetail(userId, tripId, sessionManager.getFullName());
    }

    private static String shortTripIdLabel(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        String t = raw.trim();
        return t.length() <= 14 ? "ID · " + t : "ID · " + t.substring(0, 8) + "\u2026";
    }

    private void renderDetail(TripHistoryDetailItem d) {
        currentDetail = d;
        routeTitle.setText(getString(R.string.history_item_route_arrow,
                emptyIfBlank(d.originLabel, "\u2014"), emptyIfBlank(d.destinationLabel, "\u2014")));
        heroSubtitle.setText(getString(R.string.history_detail_hero_meta, compactId(d.tripId), compactDate(d.createdAt)));

        HistoryUiHelper.applyStatusPill(statusBadge, this, d.statusLabel,
                emptyIfBlank(d.statusLabel, getString(R.string.history_status_unknown)));
        categoryLabel.setText(getString(R.string.history_detail_category_format,
                emptyIfBlank(d.category, getString(R.string.history_category_unknown))));

        bindSchedule();

        originLine.setText(getString(R.string.history_detail_origin_line, emptyIfBlank(d.originLabel, "\u2014")));
        destLine.setText(getString(R.string.history_detail_dest_line, emptyIfBlank(d.destinationLabel, "\u2014")));

        String destCoords;
        if (d.destinationLatitude != null && d.destinationLongitude != null) {
            destCoords = String.format(Locale.US, "%.5f \u00b7 %.5f", d.destinationLatitude, d.destinationLongitude);
        } else {
            destCoords = getString(R.string.history_detail_no_coords_dest_inline);
        }
        coordsLine.setText(String.format(Locale.US, "%s %.5f \u00b7 %.5f\n%s %s",
                getString(R.string.history_detail_coords_origin_prefix), d.originLatitude, d.originLongitude,
                getString(R.string.history_detail_coords_dest_prefix), destCoords));

        driverNameText.setText(emptyIfBlank(d.driverName, getString(R.string.history_detail_driver_fallback)));
        vehicleLine.setText(getString(R.string.history_detail_vehicle_line,
                emptyIfBlank(d.driverVehicleBrand, "\u2014"), emptyIfBlank(d.driverVehicleColor, "\u2014")));
        plateLine.setText(getString(R.string.history_detail_plate_line, emptyIfBlank(d.driverLicensePlate, "\u2014")));

        statReservations.setText(String.format(Locale.US, "%d", d.reservationCount));
        statBoarded.setText(String.format(Locale.US, "%d", d.boardedCount));
        statCancelled.setText(String.format(Locale.US, "%d", d.cancelledCount));

        bindYouSection(d);

        boolean hasRoute = d.destinationLatitude != null && d.destinationLongitude != null;
        viewRouteButton.setEnabled(hasRoute);
        participantsButton.setEnabled(d.participants != null && !d.participants.isEmpty());
    }

    private void bindSchedule() {
        TripHistoryDetailItem d = currentDetail;
        if (d == null) return;
        valueCreated.setText(compactDate(d.createdAt));
        bindOptionalTimelineRow(rowStarted, valueStarted, d.startedAt);
        bindOptionalTimelineRow(rowFinished, valueFinished, d.finishedAt);
        valueUpdated.setText(compactDate(d.updatedAt));
    }

    private void bindOptionalTimelineRow(LinearLayout row, TextView value, String raw) {
        String v = emptyIfBlank(raw, "");
        boolean useNd = raw == null || raw.trim().isEmpty();
        row.setVisibility(useNd ? View.GONE : View.VISIBLE);
        value.setText(useNd ? "\u2014" : compactDate(raw));
    }

    private void bindYouSection(TripHistoryDetailItem d) {
        boolean show = !(d.passengerName == null || d.passengerName.trim().isEmpty())
                || !(d.passengerReservationStatus == null || d.passengerReservationStatus.trim().isEmpty());
        cardYou.setVisibility(show ? View.VISIBLE : View.GONE);
        if (!show) return;
        youName.setText(getString(R.string.history_detail_you_reserved_as,
                emptyIfBlank(d.passengerName, getString(R.string.history_detail_you_anonymous))));
        youReservation.setText(getString(R.string.history_detail_you_status,
                emptyIfBlank(d.passengerReservationStatus, "\u2014")));
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

    private void showParticipantsBottomSheet() {
        TripHistoryDetailItem d = currentDetail;
        if (d == null || d.participants == null || d.participants.isEmpty()) {
            Toast.makeText(this, R.string.history_detail_no_participants, Toast.LENGTH_SHORT).show();
            return;
        }
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheet = getLayoutInflater().inflate(R.layout.bottom_sheet_history_participants, null);
        RecyclerView rv = sheet.findViewById(R.id.historyParticipantsRecycler);
        rv.setLayoutManager(new LinearLayoutManager(this));
        HistoryParticipantsAdapter adapter = new HistoryParticipantsAdapter();
        adapter.setItems(d.participants);
        rv.setAdapter(adapter);
        dialog.setContentView(sheet);
        dialog.show();

        View bottom = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottom != null) {
            BottomSheetBehavior.from(bottom).setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }

    private static String emptyIfBlank(String value, String fallback) {
        return (value == null || value.trim().isEmpty()) ? fallback : value.trim();
    }

    private static String compactDate(String iso) {
        if (iso == null || iso.trim().isEmpty()) return "\u2014";
        String t = iso.replace('T', ' ');
        return t.length() > 18 ? t.substring(0, 16) : t;
    }

    private static String compactId(String id) {
        if (id == null || id.isEmpty()) return "\u2014";
        String t = id.trim();
        return t.length() <= 10 ? t : t.substring(0, 8) + "\u2026";
    }

    private void openSupportReport() {
        TripHistoryDetailItem d = currentDetail;
        if (d == null || d.tripId == null || d.tripId.isBlank()) {
            return;
        }
        Intent intent = new Intent(this, SupportActivity.class);
        intent.putExtra(SupportActivity.EXTRA_TRIP_ID, d.tripId);
        intent.putExtra(SupportActivity.EXTRA_OPEN_CREATE_DIALOG, true);

        boolean isPassenger = d.passengerReservationId != null && !d.passengerReservationId.isBlank();
        boolean isDriver = "driver".equalsIgnoreCase(d.category);

        if (isPassenger) {
            intent.putExtra(SupportActivity.EXTRA_RESERVATION_ID, d.passengerReservationId);
            intent.putExtra(SupportActivity.EXTRA_CATEGORY, SupportActivity.CATEGORY_RESERVATION);
        } else if (isDriver) {
            intent.putExtra(SupportActivity.EXTRA_CATEGORY, SupportActivity.CATEGORY_TRIP);
        } else {
            intent.putExtra(SupportActivity.EXTRA_CATEGORY, SupportActivity.CATEGORY_TRIP);
        }
        startActivity(intent);
    }
}
