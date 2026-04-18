package com.example.proyectocarpooling.presentation.match.ui;

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
import androidx.core.content.ContextCompat;

import com.example.proyectocarpooling.R;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DriverMatchActivity extends AppCompatActivity {

    public static final String EXTRA_DESTINATION_LABEL = "extra_destination_label";

    private static final float ROTATION_FACTOR = 0.055f;
    private static final float DEFAULT_CARD_ELEVATION_DP = 10f;

    private TextView destinationSummaryText;
    private MaterialCardView matchCard;
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

    private final List<DriverCandidate> candidates = new ArrayList<>();
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

        loadMockCandidates();
        renderCurrentCard();
        setupCardSwipe(findViewById(R.id.cardInnerContent));

        rejectButton.setOnClickListener(v -> runRejectWithAnimation());
        acceptButton.setOnClickListener(v -> runAcceptWithAnimation());
    }

    /**
     * El gesto va sobre el contenido interior para capturar bien los toques sobre textos.
     * Las transformaciones se aplican a {@link #matchCard} (borde + elevación visibles).
     */
    private void setupCardSwipe(View touchLayer) {
        touchLayer.setOnTouchListener((v, event) -> {
            if (swipeAnimating || currentIndex >= candidates.size()) {
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
        if (swipeAnimating || currentIndex >= candidates.size()) {
            return;
        }
        runSwipeLeftAnimation();
    }

    private void runAcceptWithAnimation() {
        if (swipeAnimating || currentIndex >= candidates.size()) {
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
                    Toast.makeText(this, getString(R.string.driver_match_accepted, accepted.driverName), Toast.LENGTH_SHORT).show();
                    resetCardTransformInstant();
                    swipeAnimating = false;
                    finish();
                })
                .start();
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
        rejectButton.setEnabled(enabled && currentIndex < candidates.size());
        acceptButton.setEnabled(enabled && currentIndex < candidates.size());
    }

    private void loadMockCandidates() {
        candidates.clear();
        candidates.add(new DriverCandidate(
                "Carlos Rojas", "Villa Esperanza → Universidad", 3, 0.8, 5, "listo"));
        candidates.add(new DriverCandidate(
                "Andrea Ruiz", "Los Pinos → Universidad", 2, 1.4, 8, "en curso"));
        candidates.add(new DriverCandidate(
                "Miguel Soto", "Centro (definiendo destino)", 1, 2.1, 11, "activo"));
        candidates.add(new DriverCandidate(
                "Lucía Vargas", "Zona Sur → Universidad", 0, 3.0, 15, "cancelado"));
        candidates.add(new DriverCandidate(
                "Daniela Choque", "Plan 3000 → Universidad", 4, 2.9, 14, "finalizado"));
    }

    private void renderCurrentCard() {
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
        rejectButton.setEnabled(!swipeAnimating);
        acceptButton.setEnabled(!swipeAnimating);
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
        private final String driverName;
        private final String routeDescription;
        private final int availableSeats;
        private final double distanceKm;
        private final int etaMinutes;
        private final String tripStatusKey;

        private DriverCandidate(
                String driverName,
                String routeDescription,
                int availableSeats,
                double distanceKm,
                int etaMinutes,
                String tripStatusKey) {
            this.driverName = driverName;
            this.routeDescription = routeDescription;
            this.availableSeats = availableSeats;
            this.distanceKm = distanceKm;
            this.etaMinutes = etaMinutes;
            this.tripStatusKey = tripStatusKey;
        }
    }
}
