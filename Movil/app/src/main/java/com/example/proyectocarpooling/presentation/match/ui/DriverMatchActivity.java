package com.example.proyectocarpooling.presentation.match.ui;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.example.proyectocarpooling.R;
import com.example.proyectocarpooling.data.local.ApiBaseUrlProvider;
import com.example.proyectocarpooling.data.local.SessionManager;
import com.example.proyectocarpooling.data.model.DriverTripMatch;
import com.example.proyectocarpooling.data.remote.TripsRemoteDataSource;
import com.example.proyectocarpooling.data.repository.TripRepositoryImpl;
import com.example.proyectocarpooling.domain.repository.TripRepository;
import com.example.proyectocarpooling.presentation.main.ui.MainActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DriverMatchActivity extends AppCompatActivity {

    public static final String EXTRA_DESTINATION_LABEL = "extra_destination_label";
    public static final String EXTRA_REF_LATITUDE = "extra_ref_latitude";
    public static final String EXTRA_REF_LONGITUDE = "extra_ref_longitude";
    /** Devuelto en {@link #setResult(int, Intent)} cuando la reserva se creó correctamente. */
    public static final String EXTRA_RESULT_TRIP_ID = "extra_result_trip_id";
    /** Origen/destino del viaje del conductor (para dibujar la ruta en el mapa del pasajero). */
    public static final String EXTRA_RESULT_ORIGIN_LAT = "extra_result_origin_lat";
    public static final String EXTRA_RESULT_ORIGIN_LNG = "extra_result_origin_lng";
    public static final String EXTRA_RESULT_DEST_LAT = "extra_result_dest_lat";
    public static final String EXTRA_RESULT_DEST_LNG = "extra_result_dest_lng";

    private static final String TAG = "DriverMatch";

    private static final float ROTATION_FACTOR = 0.055f;
    private static final float DEFAULT_CARD_ELEVATION_DP = 10f;

    private TextView destinationSummaryText;
    private CardView matchCard;
    private TextView tripStatusChip;
    private TextView distanceHeroValue;
    private TextView distanceHeroHint;
    private TextView driverNameText;
    private TextView driverRouteText;
    private TextView driverSeatsText;
    private TextView driverTimeText;
    private TextView cardCounterText;
    private TextView swipeHintText;
    private Button rejectButton;
    private Button acceptButton;
    private Button viewRouteButton;

    private final List<DriverCandidate> candidates = new ArrayList<>();
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private boolean candidatesLoading;
    private int currentIndex = 0;

    private int touchSlop;
    private float downRawX;
    private float downRawY;
    private boolean dragStarted;
    private boolean swipeAnimating;

    private float defaultCardElevationPx;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_match);

        defaultCardElevationPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                DEFAULT_CARD_ELEVATION_DP,
                getResources().getDisplayMetrics());

        destinationSummaryText = findViewById(R.id.destinationSummaryText);
        matchCard = findViewById(R.id.matchCard);
        tripStatusChip = findViewById(R.id.tripStatusChip);
        distanceHeroValue = findViewById(R.id.distanceHeroValue);
        distanceHeroHint = findViewById(R.id.distanceHeroHint);
        driverNameText = findViewById(R.id.driverNameText);
        driverRouteText = findViewById(R.id.driverRouteText);
        driverSeatsText = findViewById(R.id.driverSeatsText);
        driverTimeText = findViewById(R.id.driverTimeText);
        cardCounterText = findViewById(R.id.cardCounterText);
        swipeHintText = findViewById(R.id.swipeHintText);
        rejectButton = findViewById(R.id.rejectDriverButton);
        acceptButton = findViewById(R.id.acceptDriverButton);
        viewRouteButton = findViewById(R.id.viewDriverRouteButton);

        touchSlop = ViewConfiguration.get(this).getScaledTouchSlop();

        String destinationLabel = getIntent().getStringExtra(EXTRA_DESTINATION_LABEL);
        if (destinationLabel == null || destinationLabel.trim().isEmpty()) {
            destinationLabel = getString(R.string.driver_match_default_destination);
        }
        destinationSummaryText.setText(getString(R.string.driver_match_destination_summary, destinationLabel));

        setTitle(R.string.driver_match_title);

        matchCard.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (v.getWidth() > 0 && v.getHeight() > 0) {
                matchCard.setPivotX(matchCard.getWidth() / 2f);
                matchCard.setPivotY(matchCard.getHeight() / 2f);
            }
        });

        setupCardSwipe(findViewById(R.id.cardInnerContent));

        rejectButton.setOnClickListener(v -> runRejectWithAnimation());
        acceptButton.setOnClickListener(v -> runAcceptWithAnimation());
        viewRouteButton.setOnClickListener(v -> openMainWithCurrentCandidateRoute());

        fetchMatchCandidatesFromApi();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ioExecutor.shutdownNow();
    }

    private void fetchMatchCandidatesFromApi() {
        double refLat = getIntent().getDoubleExtra(EXTRA_REF_LATITUDE, Double.NaN);
        double refLon = getIntent().getDoubleExtra(EXTRA_REF_LONGITUDE, Double.NaN);
        if (Double.isNaN(refLat) || Double.isNaN(refLon)) {
            Toast.makeText(this, R.string.driver_match_fetch_error, Toast.LENGTH_SHORT).show();
            candidatesLoading = false;
            candidates.clear();
            renderCurrentCard();
            return;
        }

        candidatesLoading = true;
        candidates.clear();
        currentIndex = 0;
        renderCurrentCard();

        String apiBase = ApiBaseUrlProvider.get(this);
        String mapboxToken = getString(R.string.mapbox_access_token);
        TripsRemoteDataSource dataSource = new TripsRemoteDataSource(apiBase, mapboxToken);

        ioExecutor.execute(() -> {
            try {
                List<DriverTripMatch> remote = dataSource.searchTripMatchCandidates(refLat, refLon);
                runOnUiThread(() -> {
                    candidatesLoading = false;
                    candidates.clear();
                    for (DriverTripMatch m : remote) {
                        candidates.add(mapRemoteToCandidate(m));
                    }
                    currentIndex = 0;
                    renderCurrentCard();
                });
            } catch (IOException e) {
                Log.e(TAG, "Error cargando candidatos", e);
                runOnUiThread(() -> {
                    candidatesLoading = false;
                    candidates.clear();
                    Toast.makeText(DriverMatchActivity.this, R.string.driver_match_fetch_error, Toast.LENGTH_SHORT).show();
                    renderCurrentCard();
                });
            }
        });
    }

    private static DriverCandidate mapRemoteToCandidate(DriverTripMatch m) {
        String route = String.format(Locale.US, "%.4f,%.4f → %.4f,%.4f",
                m.originLatitude, m.originLongitude,
                m.destinationLatitude, m.destinationLongitude);
        return new DriverCandidate(
                m.tripId,
                m.driverName,
                route,
                m.availableSeats,
                m.distanceKm,
                m.etaMinutes,
                statusEnumToChipKey(m.status),
                m.originLatitude,
                m.originLongitude,
                m.destinationLatitude,
                m.destinationLongitude);
    }

    private static String statusEnumToChipKey(int status) {
        switch (status) {
            case 0:
                return "activo";
            case 1:
                return "listo";
            case 2:
                return "cancelado";
            case 3:
                return "en curso";
            case 4:
                return "finalizado";
            default:
                return "desconocido";
        }
    }

    /**
     * El gesto va sobre el contenido interior para capturar bien los toques sobre textos.
     * Las transformaciones se aplican a {@link #matchCard} (borde + elevación visibles).
     */
    private void setupCardSwipe(View touchLayer) {
        touchLayer.setOnTouchListener((v, event) -> {
            if (candidatesLoading || swipeAnimating || currentIndex >= candidates.size()) {
                return false;
            }

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downRawX = event.getRawX();
                    downRawY = event.getRawY();
                    dragStarted = false;
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                    matchCard.animate().cancel();
                    return true;

                case MotionEvent.ACTION_MOVE: {
                    float dx = event.getRawX() - downRawX;
                    float dy = event.getRawY() - downRawY;
                    if (!dragStarted && (Math.abs(dx) > touchSlop || Math.abs(dy) > touchSlop)) {
                        dragStarted = true;
                    }
                    if (dragStarted) {
                        if (Math.abs(dx) > Math.abs(dy) * 0.7f) {
                            v.getParent().requestDisallowInterceptTouchEvent(true);
                        }
                        matchCard.setTranslationX(dx);
                        matchCard.setRotation(dx * ROTATION_FACTOR);
                        float w = Math.max(matchCard.getWidth(), 1);
                        float absDx = Math.abs(dx);
                        float scale = 1f - Math.min(0.09f, (absDx / w) * 0.15f);
                        matchCard.setScaleX(scale);
                        matchCard.setScaleY(scale);
                        float extraDp = Math.min(16f, (absDx / w) * 22f);
                        matchCard.setCardElevation(defaultCardElevationPx + TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP, extraDp, getResources().getDisplayMetrics()));
                    }
                    return true;
                }

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: {
                    float totalDx = event.getRawX() - downRawX;
                    float threshold = Math.max(matchCard.getWidth(), 200) * 0.22f;
                    if (dragStarted && totalDx < -threshold) {
                        runSwipeLeftAnimation();
                    } else if (dragStarted && totalDx > threshold) {
                        runSwipeRightAnimation();
                    } else {
                        springBackCard();
                    }
                    dragStarted = false;
                    v.getParent().requestDisallowInterceptTouchEvent(false);
                    return true;
                }

                default:
                    return false;
            }
        });
    }

    private void springBackCard() {
        matchCard.animate()
                .translationX(0f)
                .rotation(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(200)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> matchCard.setCardElevation(defaultCardElevationPx))
                .start();
    }

    private void runRejectWithAnimation() {
        if (candidatesLoading || swipeAnimating || currentIndex >= candidates.size()) {
            return;
        }
        runSwipeLeftAnimation();
    }

    private void runAcceptWithAnimation() {
        if (candidatesLoading || swipeAnimating || currentIndex >= candidates.size()) {
            return;
        }
        runSwipeRightAnimation();
    }

    private void runSwipeLeftAnimation() {
        swipeAnimating = true;
        setButtonsEnabled(false);
        int screenW = getResources().getDisplayMetrics().widthPixels;
        matchCard.animate()
                .translationX(-screenW * 1.15f)
                .rotation(-22f)
                .alpha(0.92f)
                .setDuration(200)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> {
                    DriverCandidate rejected = candidates.get(currentIndex);
                    Toast.makeText(this, getString(R.string.driver_match_rejected, rejected.driverName), Toast.LENGTH_SHORT).show();
                    currentIndex++;
                    renderCurrentCard();
                    if (currentIndex < candidates.size()) {
                        matchCard.setTranslationX(screenW * 0.45f);
                        matchCard.setRotation(18f);
                        matchCard.setAlpha(0.35f);
                        matchCard.setScaleX(0.96f);
                        matchCard.setScaleY(0.96f);
                        matchCard.setCardElevation(defaultCardElevationPx);
                        matchCard.animate()
                                .translationX(0f)
                                .rotation(0f)
                                .alpha(1f)
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(260)
                                .setInterpolator(new DecelerateInterpolator())
                                .withEndAction(() -> {
                                    swipeAnimating = false;
                                    setButtonsEnabled(true);
                                })
                                .start();
                    } else {
                        resetCardTransformInstant();
                        swipeAnimating = false;
                        setButtonsEnabled(false);
                    }
                })
                .start();
    }

    private void runSwipeRightAnimation() {
        swipeAnimating = true;
        setButtonsEnabled(false);
        int screenW = getResources().getDisplayMetrics().widthPixels;
        matchCard.animate()
                .translationX(screenW * 1.15f)
                .rotation(22f)
                .alpha(0.92f)
                .setDuration(200)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> {
                    DriverCandidate accepted = candidates.get(currentIndex);
                    submitReservationAfterAnimation(accepted);
                })
                .start();
    }

    private void submitReservationAfterAnimation(DriverCandidate accepted) {
        SessionManager sessionManager = new SessionManager(this);
        if (!sessionManager.hasActiveSession()) {
            Toast.makeText(this, R.string.driver_match_login_required, Toast.LENGTH_LONG).show();
            swipeAnimating = false;
            resetCardTransformInstant();
            setButtonsEnabled(true);
            return;
        }

        final String passengerName = sessionManager.getFullName().trim();
        if (passengerName.isEmpty()) {
            Toast.makeText(this, R.string.driver_match_login_required, Toast.LENGTH_LONG).show();
            swipeAnimating = false;
            resetCardTransformInstant();
            setButtonsEnabled(true);
            return;
        }

        String apiBase = ApiBaseUrlProvider.get(this);
        String mapboxToken = getString(R.string.mapbox_access_token);
        TripRepository repository = new TripRepositoryImpl(new TripsRemoteDataSource(apiBase, mapboxToken));

        ioExecutor.execute(() -> {
            try {
                repository.createReservation(accepted.tripId, passengerName);
                runOnUiThread(() -> {
                    Toast.makeText(this, getString(R.string.driver_match_reservation_ok, accepted.driverName), Toast.LENGTH_LONG).show();
                    Intent data = new Intent();
                    data.putExtra(EXTRA_RESULT_TRIP_ID, accepted.tripId);
                    data.putExtra(EXTRA_RESULT_ORIGIN_LAT, accepted.originLatitude);
                    data.putExtra(EXTRA_RESULT_ORIGIN_LNG, accepted.originLongitude);
                    data.putExtra(EXTRA_RESULT_DEST_LAT, accepted.destinationLatitude);
                    data.putExtra(EXTRA_RESULT_DEST_LNG, accepted.destinationLongitude);
                    setResult(RESULT_OK, data);
                    swipeAnimating = false;
                    finish();
                });
            } catch (IOException e) {
                Log.e(TAG, "createReservation", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.driver_match_reservation_failed, Toast.LENGTH_LONG).show();
                    swipeAnimating = false;
                    resetCardTransformInstant();
                    setButtonsEnabled(true);
                });
            }
        });
    }

    private void resetCardTransformInstant() {
        matchCard.animate().cancel();
        matchCard.setTranslationX(0f);
        matchCard.setRotation(0f);
        matchCard.setScaleX(1f);
        matchCard.setScaleY(1f);
        matchCard.setAlpha(1f);
        matchCard.setCardElevation(defaultCardElevationPx);
    }

    private void setButtonsEnabled(boolean enabled) {
        boolean canInteract = enabled && !candidatesLoading && currentIndex < candidates.size();
        rejectButton.setEnabled(canInteract);
        acceptButton.setEnabled(canInteract);
        if (viewRouteButton != null && viewRouteButton.getVisibility() == View.VISIBLE) {
            viewRouteButton.setEnabled(canInteract);
        }
    }

    /**
     * Abre el mapa principal con la ruta origen→destino del conductor mostrado en la tarjeta actual.
     * El estudiante puede volver atrás para seguir comparando conductores.
     */
    private void openMainWithCurrentCandidateRoute() {
        if (candidatesLoading || currentIndex >= candidates.size()) {
            return;
        }
        DriverCandidate current = candidates.get(currentIndex);
        if (!current.hasRouteEndpoints()) {
            Toast.makeText(this, R.string.driver_match_view_route_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(MainActivity.EXTRA_APPLY_FAVORITE_KIND, "route");
        intent.putExtra(MainActivity.EXTRA_APPLY_ORIGIN_LAT, current.getOriginLatitude());
        intent.putExtra(MainActivity.EXTRA_APPLY_ORIGIN_LNG, current.getOriginLongitude());
        intent.putExtra(MainActivity.EXTRA_APPLY_DEST_LAT, current.getDestinationLatitude());
        intent.putExtra(MainActivity.EXTRA_APPLY_DEST_LNG, current.getDestinationLongitude());
        intent.putExtra(MainActivity.EXTRA_HISTORY_ROUTE_PREVIEW, true);
        startActivity(intent);
    }

    private void renderCurrentCard() {
        if (candidatesLoading && candidates.isEmpty()) {
            tripStatusChip.setVisibility(View.GONE);
            swipeHintText.setVisibility(View.GONE);
            distanceHeroValue.setText(R.string.driver_match_distance_value_loading);
            distanceHeroHint.setText(R.string.driver_match_distance_hint);
            driverNameText.setText(R.string.driver_match_name_loading);
            driverRouteText.setText(R.string.driver_match_route_loading);
            driverSeatsText.setText(R.string.driver_match_seats_loading);
            driverTimeText.setText(R.string.driver_match_time_loading);
            cardCounterText.setText(R.string.driver_match_counter_loading);
            rejectButton.setEnabled(false);
            acceptButton.setEnabled(false);
            if (viewRouteButton != null) {
                viewRouteButton.setVisibility(View.GONE);
            }
            return;
        }

        if (currentIndex >= candidates.size()) {
            tripStatusChip.setVisibility(View.GONE);
            swipeHintText.setVisibility(View.GONE);
            distanceHeroValue.setText(R.string.driver_match_distance_value_loading);
            distanceHeroHint.setText("");
            driverNameText.setText(R.string.driver_match_no_results_title);
            driverRouteText.setText(R.string.driver_match_no_results_subtitle);
            driverSeatsText.setText("");
            driverTimeText.setText("");
            cardCounterText.setText(R.string.driver_match_no_results_counter);
            rejectButton.setEnabled(false);
            acceptButton.setEnabled(false);
            if (viewRouteButton != null) {
                viewRouteButton.setVisibility(View.GONE);
            }
            return;
        }

        tripStatusChip.setVisibility(View.VISIBLE);
        swipeHintText.setVisibility(View.VISIBLE);
        DriverCandidate current = candidates.get(currentIndex);
        applyTripStatusChip(current.tripStatusKey);
        distanceHeroValue.setText(String.format(Locale.US, "%.1f", current.distanceKm));
        distanceHeroHint.setText(R.string.driver_match_distance_hint);
        driverNameText.setText(current.driverName);
        driverRouteText.setText(getString(R.string.driver_match_route_format, current.routeDescription));
        driverSeatsText.setText(getString(R.string.driver_match_seats_format, current.availableSeats));
        driverTimeText.setText(getString(R.string.driver_match_time_format, current.etaMinutes));
        cardCounterText.setText(getString(R.string.driver_match_counter_format, currentIndex + 1, candidates.size()));
        rejectButton.setEnabled(!swipeAnimating && !candidatesLoading);
        acceptButton.setEnabled(!swipeAnimating && !candidatesLoading);
        if (viewRouteButton != null) {
            boolean hasRoute = current.hasRouteEndpoints();
            viewRouteButton.setVisibility(hasRoute ? View.VISIBLE : View.GONE);
            viewRouteButton.setEnabled(hasRoute && !swipeAnimating && !candidatesLoading);
        }
    }

    private void applyTripStatusChip(String rawStatus) {
        String normalized = rawStatus == null ? "" : rawStatus.trim().toLowerCase(Locale.US);
        int labelRes;
        int colorRes;

        if (normalized.equals("listo") || normalized.equals("ready") || normalized.equals("1")) {
            labelRes = R.string.driver_match_status_listo;
            colorRes = R.color.match_status_ready;
        } else if (normalized.equals("en curso") || normalized.equals("en_curso") || normalized.equals("inprogress") || normalized.equals("3")) {
            labelRes = R.string.driver_match_status_en_curso;
            colorRes = R.color.match_status_in_progress;
        } else if (normalized.equals("cancelado") || normalized.equals("cancelled") || normalized.equals("2")) {
            labelRes = R.string.driver_match_status_cancelado;
            colorRes = R.color.match_status_cancelled;
        } else if (normalized.equals("finalizado") || normalized.equals("finished") || normalized.equals("4")) {
            labelRes = R.string.driver_match_status_finalizado;
            colorRes = R.color.match_status_finished;
        } else if (normalized.equals("activo") || normalized.equals("awaitingdestination") || normalized.equals("pending") || normalized.equals("0")) {
            labelRes = R.string.driver_match_status_activo;
            colorRes = R.color.match_status_active;
        } else {
            labelRes = R.string.driver_match_status_desconocido;
            colorRes = R.color.carpool_primary;
        }

        tripStatusChip.setText(labelRes);
        int bgColor = ContextCompat.getColor(this, colorRes);
        float radiusPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 10f, getResources().getDisplayMetrics());
        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(radiusPx);
        drawable.setColor(bgColor);
        tripStatusChip.setBackground(drawable);
        tripStatusChip.setTextColor(ContextCompat.getColor(this, R.color.white));
    }

    private static class DriverCandidate {
        private final String tripId;
        private final String driverName;
        private final String routeDescription;
        private final int availableSeats;
        private final double distanceKm;
        private final int etaMinutes;
        private final String tripStatusKey;
        private final double originLatitude;
        private final double originLongitude;
        private final double destinationLatitude;
        private final double destinationLongitude;

        private DriverCandidate(
                String tripId,
                String driverName,
                String routeDescription,
                int availableSeats,
                double distanceKm,
                int etaMinutes,
                String tripStatusKey,
                double originLatitude,
                double originLongitude,
                double destinationLatitude,
                double destinationLongitude) {
            this.tripId = tripId;
            this.driverName = driverName;
            this.routeDescription = routeDescription;
            this.availableSeats = availableSeats;
            this.distanceKm = distanceKm;
            this.etaMinutes = etaMinutes;
            this.tripStatusKey = tripStatusKey;
            this.originLatitude = originLatitude;
            this.originLongitude = originLongitude;
            this.destinationLatitude = destinationLatitude;
            this.destinationLongitude = destinationLongitude;
        }

        /** Ruta mostrable: origen y destino distintos del sentinel típico (0,0) cuando aún no hay destino. */
        boolean hasRouteEndpoints() {
            return coordsLikelySet(originLatitude, originLongitude)
                    && coordsLikelySet(destinationLatitude, destinationLongitude);
        }

        private static boolean coordsLikelySet(double lat, double lng) {
            return Math.abs(lat) > 1e-6 || Math.abs(lng) > 1e-6;
        }

        double getOriginLatitude() {
            return originLatitude;
        }

        double getOriginLongitude() {
            return originLongitude;
        }

        double getDestinationLatitude() {
            return destinationLatitude;
        }

        double getDestinationLongitude() {
            return destinationLongitude;
        }
    }
}
