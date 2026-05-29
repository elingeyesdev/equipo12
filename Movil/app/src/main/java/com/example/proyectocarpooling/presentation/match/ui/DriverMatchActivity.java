package com.example.proyectocarpooling.presentation.match.ui;

import com.example.proyectocarpooling.presentation.BaseActivity;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
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
import androidx.lifecycle.ViewModelProvider;

import com.example.proyectocarpooling.CarPoolingApplication;
import com.example.proyectocarpooling.R;
import com.example.proyectocarpooling.data.local.SessionManager;
import com.example.proyectocarpooling.presentation.main.ui.MainActivity;

import java.util.List;
import java.util.Locale;

public class DriverMatchActivity extends BaseActivity {

    public static final String EXTRA_DESTINATION_LABEL = "extra_destination_label";
    public static final String EXTRA_REF_LATITUDE = "extra_ref_latitude";
    public static final String EXTRA_REF_LONGITUDE = "extra_ref_longitude";
    public static final String EXTRA_RESULT_TRIP_ID = "extra_result_trip_id";
    public static final String EXTRA_RESULT_ORIGIN_LAT = "extra_result_origin_lat";
    public static final String EXTRA_RESULT_ORIGIN_LNG = "extra_result_origin_lng";
    public static final String EXTRA_RESULT_DEST_LAT = "extra_result_dest_lat";
    public static final String EXTRA_RESULT_DEST_LNG = "extra_result_dest_lng";
    public static final String EXTRA_RESULT_DRIVER_NAME = "extra_result_driver_name";

    private static final float ROTATION_FACTOR = 0.055f;
    private static final float DEFAULT_CARD_ELEVATION_DP = 10f;

    private TextView destinationSummaryText, tripStatusChip, distanceHeroValue, distanceHeroHint;
    private TextView driverNameText, driverRouteText, driverVehicleText, driverSeatsText, driverTimeText;
    private TextView cardCounterText, swipeHintText, viewRouteHintText;
    private Button rejectButton, acceptButton, viewRouteButton;
    private CardView matchCard;

    private DriverMatchViewModel viewModel;
    private SessionManager sessionManager;
    private List<DriverMatchViewModel.DriverCandidate> candidates;
    private int currentIndex = 0;
    private boolean swipeAnimating;
    private boolean dragStarted;
    private int touchSlop;
    private float downRawX, downRawY;
    private float defaultCardElevationPx;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_match);

        sessionManager = ((CarPoolingApplication) getApplication()).getSessionManager();
        viewModel = new ViewModelProvider(this).get(DriverMatchViewModel.class);

        defaultCardElevationPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                DEFAULT_CARD_ELEVATION_DP, getResources().getDisplayMetrics());

        bindViews();
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

        observeViewModel();
        fetchMatchCandidatesFromApi();
    }

    private void bindViews() {
        destinationSummaryText = findViewById(R.id.destinationSummaryText);
        matchCard = findViewById(R.id.matchCard);
        tripStatusChip = findViewById(R.id.tripStatusChip);
        distanceHeroValue = findViewById(R.id.distanceHeroValue);
        distanceHeroHint = findViewById(R.id.distanceHeroHint);
        driverNameText = findViewById(R.id.driverNameText);
        driverRouteText = findViewById(R.id.driverRouteText);
        driverVehicleText = findViewById(R.id.driverVehicleText);
        driverSeatsText = findViewById(R.id.driverSeatsText);
        driverTimeText = findViewById(R.id.driverTimeText);
        cardCounterText = findViewById(R.id.cardCounterText);
        swipeHintText = findViewById(R.id.swipeHintText);
        viewRouteHintText = findViewById(R.id.viewDriverRouteHintText);
        rejectButton = findViewById(R.id.rejectDriverButton);
        acceptButton = findViewById(R.id.acceptDriverButton);
        viewRouteButton = findViewById(R.id.viewDriverRouteButton);
    }

    private void observeViewModel() {
        viewModel.getLoading().observe(this, loading -> {
            if (loading && (candidates == null || candidates.isEmpty())) {
                renderLoadingState();
            }
        });

        viewModel.getCandidates().observe(this, list -> {
            candidates = list;
            currentIndex = 0;
            renderCurrentCard();
        });

        viewModel.getReservationResult().observe(this, candidate -> {
            if (candidate != null) {
                Toast.makeText(this, getString(R.string.driver_match_reservation_ok, candidate.driverName), Toast.LENGTH_LONG).show();
                Intent data = new Intent();
                data.putExtra(EXTRA_RESULT_TRIP_ID, candidate.tripId);
                data.putExtra(EXTRA_RESULT_ORIGIN_LAT, candidate.originLatitude);
                data.putExtra(EXTRA_RESULT_ORIGIN_LNG, candidate.originLongitude);
                data.putExtra(EXTRA_RESULT_DEST_LAT, candidate.destinationLatitude);
                data.putExtra(EXTRA_RESULT_DEST_LNG, candidate.destinationLongitude);
                data.putExtra(EXTRA_RESULT_DRIVER_NAME, candidate.driverName);
                setResult(RESULT_OK, data);
                swipeAnimating = false;
                finish();
            }
        });

        viewModel.getErrorEvent().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                swipeAnimating = false;
                resetCardTransformInstant();
                setButtonsEnabled(true);
            }
        });
    }

    private void fetchMatchCandidatesFromApi() {
        double refLat = getIntent().getDoubleExtra(EXTRA_REF_LATITUDE, Double.NaN);
        double refLon = getIntent().getDoubleExtra(EXTRA_REF_LONGITUDE, Double.NaN);
        if (Double.isNaN(refLat) || Double.isNaN(refLon)) {
            Toast.makeText(this, R.string.driver_match_fetch_error, Toast.LENGTH_SHORT).show();
            return;
        }
        viewModel.fetchCandidates(refLat, refLon);
    }

    private void setupCardSwipe(View touchLayer) {
        touchLayer.setOnTouchListener((v, event) -> {
            if (swipeAnimating || currentIndex >= (candidates != null ? candidates.size() : 0)) {
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
        matchCard.animate().translationX(0f).rotation(0f).scaleX(1f).scaleY(1f)
                .setDuration(200).setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> matchCard.setCardElevation(defaultCardElevationPx)).start();
    }

    private void runRejectWithAnimation() {
        if (swipeAnimating || candidates == null || currentIndex >= candidates.size()) return;
        runSwipeLeftAnimation();
    }

    private void runAcceptWithAnimation() {
        if (swipeAnimating || candidates == null || currentIndex >= candidates.size()) return;
        runSwipeRightAnimation();
    }

    private void runSwipeLeftAnimation() {
        swipeAnimating = true;
        setButtonsEnabled(false);
        int screenW = getResources().getDisplayMetrics().widthPixels;
        matchCard.animate().translationX(-screenW * 1.15f).rotation(-22f).alpha(0.92f)
                .setDuration(200).setInterpolator(new DecelerateInterpolator()).withEndAction(() -> {
                    DriverMatchViewModel.DriverCandidate rejected = candidates.get(currentIndex);
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
                        matchCard.animate().translationX(0f).rotation(0f).alpha(1f).scaleX(1f).scaleY(1f)
                                .setDuration(260).setInterpolator(new DecelerateInterpolator())
                                .withEndAction(() -> { swipeAnimating = false; setButtonsEnabled(true); }).start();
                    } else {
                        resetCardTransformInstant();
                        swipeAnimating = false;
                        setButtonsEnabled(false);
                    }
                }).start();
    }

    private void runSwipeRightAnimation() {
        swipeAnimating = true;
        setButtonsEnabled(false);
        int screenW = getResources().getDisplayMetrics().widthPixels;
        matchCard.animate().translationX(screenW * 1.15f).rotation(22f).alpha(0.92f)
                .setDuration(200).setInterpolator(new DecelerateInterpolator()).withEndAction(() -> {
                    DriverMatchViewModel.DriverCandidate accepted = candidates.get(currentIndex);
                    String passengerUserId = sessionManager.getUserId();
                    if (!sessionManager.hasActiveSession() || passengerUserId.isEmpty()) {
                        Toast.makeText(this, R.string.driver_match_login_required, Toast.LENGTH_LONG).show();
                        swipeAnimating = false;
                        resetCardTransformInstant();
                        setButtonsEnabled(true);
                        return;
                    }
                    viewModel.createReservation(accepted, passengerUserId);
                }).start();
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
        boolean canInteract = enabled && candidates != null && currentIndex < candidates.size();
        rejectButton.setEnabled(canInteract);
        acceptButton.setEnabled(canInteract);
        if (viewRouteButton != null && viewRouteButton.getVisibility() == View.VISIBLE) {
            viewRouteButton.setEnabled(canInteract);
        }
    }

    private void openMainWithCurrentCandidateRoute() {
        if (candidates == null || currentIndex >= candidates.size()) return;
        DriverMatchViewModel.DriverCandidate current = candidates.get(currentIndex);
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
        intent.putExtra(MainActivity.EXTRA_ROUTE_PREVIEW_CONTEXT, MainActivity.ROUTE_PREVIEW_CONTEXT_DRIVER_MATCH);
        intent.putExtra(MainActivity.EXTRA_ROUTE_PREVIEW_DRIVER_NAME, current.getDriverName());
        intent.putExtra(MainActivity.EXTRA_ROUTE_PREVIEW_TRIP_ID, current.getTripId());
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (MainActivity.sReservationCompletedFromBanner) {
            MainActivity.sReservationCompletedFromBanner = false;
            setResult(RESULT_OK, MainActivity.sReservationResultData);
            finish();
        }
    }

    private void renderLoadingState() {
        tripStatusChip.setVisibility(View.GONE);
        swipeHintText.setVisibility(View.GONE);
        distanceHeroValue.setText(R.string.driver_match_distance_value_loading);
        distanceHeroHint.setText(R.string.driver_match_distance_hint);
        driverNameText.setText(R.string.driver_match_name_loading);
        driverRouteText.setText(R.string.driver_match_route_loading);
        driverVehicleText.setText("");
        driverSeatsText.setText(R.string.driver_match_seats_loading);
        driverTimeText.setText(R.string.driver_match_time_loading);
        cardCounterText.setText(R.string.driver_match_counter_loading);
        rejectButton.setEnabled(false);
        acceptButton.setEnabled(false);
        if (viewRouteButton != null) viewRouteButton.setVisibility(View.GONE);
        if (viewRouteHintText != null) viewRouteHintText.setVisibility(View.GONE);
    }

    private void renderCurrentCard() {
        if (candidates == null || candidates.isEmpty()) {
            tripStatusChip.setVisibility(View.GONE);
            swipeHintText.setVisibility(View.GONE);
            distanceHeroValue.setText(R.string.driver_match_distance_value_loading);
            distanceHeroHint.setText("");
            driverNameText.setText(R.string.driver_match_no_results_title);
            driverRouteText.setText(R.string.driver_match_no_results_subtitle);
            driverVehicleText.setText("");
            driverSeatsText.setText("");
            driverTimeText.setText("");
            cardCounterText.setText(R.string.driver_match_no_results_counter);
            rejectButton.setEnabled(false);
            acceptButton.setEnabled(false);
            if (viewRouteButton != null) viewRouteButton.setVisibility(View.GONE);
            if (viewRouteHintText != null) viewRouteHintText.setVisibility(View.GONE);
            return;
        }

        if (currentIndex >= candidates.size()) {
            tripStatusChip.setVisibility(View.GONE);
            swipeHintText.setVisibility(View.GONE);
            distanceHeroValue.setText(R.string.driver_match_distance_value_loading);
            distanceHeroHint.setText("");
            driverNameText.setText(R.string.driver_match_no_results_title);
            driverRouteText.setText(R.string.driver_match_no_results_subtitle);
            driverVehicleText.setText("");
            driverSeatsText.setText("");
            driverTimeText.setText("");
            cardCounterText.setText(R.string.driver_match_no_results_counter);
            rejectButton.setEnabled(false);
            acceptButton.setEnabled(false);
            if (viewRouteButton != null) viewRouteButton.setVisibility(View.GONE);
            if (viewRouteHintText != null) viewRouteHintText.setVisibility(View.GONE);
            return;
        }

        tripStatusChip.setVisibility(View.VISIBLE);
        swipeHintText.setVisibility(View.VISIBLE);
        DriverMatchViewModel.DriverCandidate current = candidates.get(currentIndex);
        applyTripStatusChip(current.tripStatusKey);
        distanceHeroValue.setText(String.format(Locale.US, "%.1f", current.distanceKm));
        distanceHeroHint.setText(R.string.driver_match_distance_hint);
        driverNameText.setText(current.driverName);
        driverRouteText.setText(getString(R.string.driver_match_route_format, current.routeDescription));
        driverVehicleText.setText(current.vehicleInfo);
        driverVehicleText.setVisibility(current.vehicleInfo != null && !current.vehicleInfo.isEmpty() ? View.VISIBLE : View.GONE);
        driverSeatsText.setText(getString(R.string.driver_match_seats_format, current.availableSeats));
        driverTimeText.setText(getString(R.string.driver_match_time_format, current.etaMinutes));
        cardCounterText.setText(getString(R.string.driver_match_counter_format, currentIndex + 1, candidates.size()));
        rejectButton.setEnabled(!swipeAnimating);
        acceptButton.setEnabled(!swipeAnimating);
        if (viewRouteButton != null) {
            boolean hasRoute = current.hasRouteEndpoints();
            viewRouteButton.setVisibility(hasRoute ? View.VISIBLE : View.GONE);
            viewRouteButton.setEnabled(hasRoute && !swipeAnimating);
        }
        if (viewRouteHintText != null) {
            viewRouteHintText.setVisibility(current.hasRouteEndpoints() ? View.VISIBLE : View.GONE);
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
        float radiusPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10f, getResources().getDisplayMetrics());
        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(radiusPx);
        drawable.setColor(bgColor);
        tripStatusChip.setBackground(drawable);
        tripStatusChip.setTextColor(ContextCompat.getColor(this, R.color.white));
    }
}
