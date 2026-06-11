package com.example.proyectocarpooling.presentation.account.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ProgressBar;

import com.example.proyectocarpooling.presentation.BaseActivity;

import com.example.proyectocarpooling.CarPoolingApplication;
import com.example.proyectocarpooling.R;
import com.example.proyectocarpooling.data.local.SessionManager;

public class AccountOverviewActivity extends BaseActivity {

    private org.json.JSONObject mSummary = null;
    private org.json.JSONArray mRatings = null;
    private boolean mShowingDriver = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_overview);

        SessionManager sessionManager = ((CarPoolingApplication) getApplication()).getSessionManager();

        TextView nameValue = findViewById(R.id.accountNameValue);
        TextView emailValue = findViewById(R.id.accountEmailValue);
        TextView userInitials = findViewById(R.id.accountUserInitials);
        android.widget.ImageView accountUserImage = findViewById(R.id.accountUserImage);
        android.view.View accountUserPlaceholder = findViewById(R.id.accountUserPlaceholder);
        TextView userRole = findViewById(R.id.accountRoleValue);
        Button backButton = findViewById(R.id.accountBackButton);
        Button closeButton = findViewById(R.id.accountCloseButton);

        TextView tabDriver = findViewById(R.id.tabDriver);
        TextView tabPassenger = findViewById(R.id.tabPassenger);

        String fullName = sessionManager.getFullName();
        String email = sessionManager.getEmail();
        boolean isDriver = sessionManager.isDriver();

        if (fullName == null || fullName.trim().isEmpty()) {
            fullName = "Usuario Demo";
        }
        if (email == null || email.trim().isEmpty()) {
            email = "usuario@univalle.edu";
        }

        nameValue.setText(fullName);
        emailValue.setText(email);
        userInitials.setText(generateInitials(fullName));
        if (accountUserImage != null) {
            loadBase64Image(sessionManager.getProfilePicture(), accountUserImage, accountUserPlaceholder);
        }
        String roleText = isDriver ? getString(R.string.user_role_driver) : getString(R.string.user_role_passenger);
        userRole.setText(roleText);

        backButton.setOnClickListener(v -> finish());
        closeButton.setOnClickListener(v -> finish());

        // Default: if user is passenger, default to passenger tab, if driver to driver tab
        mShowingDriver = isDriver;
        if (tabDriver != null && tabPassenger != null) {
            if (!isDriver) {
                android.view.ViewParent parent = tabDriver.getParent();
                if (parent instanceof android.view.View) {
                    ((android.view.View) parent).setVisibility(android.view.View.GONE);
                }
            } else {
                updateTabUI(mShowingDriver ? tabDriver : tabPassenger, mShowingDriver ? tabPassenger : tabDriver);
                
                tabDriver.setOnClickListener(v -> {
                    if (mShowingDriver) return;
                    mShowingDriver = true;
                    updateTabUI(tabDriver, tabPassenger);
                    renderSegmentData();
                });

                tabPassenger.setOnClickListener(v -> {
                    if (!mShowingDriver) return;
                    mShowingDriver = false;
                    updateTabUI(tabPassenger, tabDriver);
                    renderSegmentData();
                });
            }
        }

        // Cargar estadísticas y calificaciones reales del servidor
        final String userId = sessionManager.getUserId();
        if (userId != null && !userId.isEmpty()) {
            java.util.concurrent.Executor backgroundExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();
            backgroundExecutor.execute(() -> {
                try {
                    CarPoolingApplication app = (CarPoolingApplication) getApplication();
                    mSummary = app.getRatingRemoteDataSource().getUserRatingSummary(userId, userId);
                    mRatings = app.getRatingRemoteDataSource().getUserRatings(userId, userId);

                    runOnUiThread(() -> {
                        renderSegmentData();
                    });
                } catch (Exception ignored) {}
            });
        }
    }

    private void updateTabUI(TextView activeTab, TextView inactiveTab) {
        if (activeTab == null || inactiveTab == null) return;
        activeTab.setBackgroundResource(R.drawable.bg_primary_action);
        activeTab.setTextColor(getResources().getColor(R.color.white, null));
        inactiveTab.setBackgroundResource(0);
        inactiveTab.setTextColor(getResources().getColor(R.color.carpool_text_secondary, null));
    }

    private void renderSegmentData() {
        if (isFinishing() || isDestroyed()) return;

        TextView scoreValue = findViewById(R.id.accountScoreValue);
        TextView starsValue = findViewById(R.id.accountStarsValue);
        TextView reviewsValue = findViewById(R.id.accountReviewsValue);
        android.widget.LinearLayout commentsContainer = findViewById(R.id.accountCommentsContainer);

        ProgressBar bar5Star = findViewById(R.id.bar5Star);
        ProgressBar bar4Star = findViewById(R.id.bar4Star);
        ProgressBar bar3Star = findViewById(R.id.bar3Star);
        ProgressBar bar2Star = findViewById(R.id.bar2Star);
        ProgressBar bar1Star = findViewById(R.id.bar1Star);

        TextView txt5StarCount = findViewById(R.id.txt5StarCount);
        TextView txt4StarCount = findViewById(R.id.txt4StarCount);
        TextView txt3StarCount = findViewById(R.id.txt3StarCount);
        TextView txt2StarCount = findViewById(R.id.txt2StarCount);
        TextView txt1StarCount = findViewById(R.id.txt1StarCount);

        double avg = 0.0;
        int count = 0;
        org.json.JSONArray starDist = null;

        if (mSummary != null) {
            if (mShowingDriver) {
                avg = mSummary.optDouble("averageDriverScore", 0.0);
                count = mSummary.optInt("totalDriverRatingsCount", 0);
                starDist = mSummary.optJSONArray("driverStarsDistribution");
            } else {
                avg = mSummary.optDouble("averagePassengerScore", 0.0);
                count = mSummary.optInt("totalPassengerRatingsCount", 0);
                starDist = mSummary.optJSONArray("passengerStarsDistribution");
            }
        }

        // 1. Promedio, cantidad y estrellas
        if (scoreValue != null) {
            scoreValue.setText(String.format(java.util.Locale.US, "%.1f", avg));
        }
        if (reviewsValue != null) {
            reviewsValue.setText(String.valueOf(count));
        }

        if (starsValue != null) {
            StringBuilder stars = new StringBuilder();
            int roundedScore = (int) Math.round(avg);
            for (int i = 1; i <= 5; i++) {
                stars.append(i <= roundedScore ? "★" : "☆");
            }
            starsValue.setText(stars.toString());
        }

        // 2. Gráfico de distribución de estrellas
        int c1 = 0, c2 = 0, c3 = 0, c4 = 0, c5 = 0;
        if (starDist != null && starDist.length() == 5) {
            c1 = starDist.optInt(0, 0);
            c2 = starDist.optInt(1, 0);
            c3 = starDist.optInt(2, 0);
            c4 = starDist.optInt(3, 0);
            c5 = starDist.optInt(4, 0);
        }

        int totalDist = c1 + c2 + c3 + c4 + c5;
        int p1 = totalDist > 0 ? (c1 * 100 / totalDist) : 0;
        int p2 = totalDist > 0 ? (c2 * 100 / totalDist) : 0;
        int p3 = totalDist > 0 ? (c3 * 100 / totalDist) : 0;
        int p4 = totalDist > 0 ? (c4 * 100 / totalDist) : 0;
        int p5 = totalDist > 0 ? (c5 * 100 / totalDist) : 0;

        if (bar1Star != null) bar1Star.setProgress(p1);
        if (bar2Star != null) bar2Star.setProgress(p2);
        if (bar3Star != null) bar3Star.setProgress(p3);
        if (bar4Star != null) bar4Star.setProgress(p4);
        if (bar5Star != null) bar5Star.setProgress(p5);

        if (txt1StarCount != null) txt1StarCount.setText(String.valueOf(c1));
        if (txt2StarCount != null) txt2StarCount.setText(String.valueOf(c2));
        if (txt3StarCount != null) txt3StarCount.setText(String.valueOf(c3));
        if (txt4StarCount != null) txt4StarCount.setText(String.valueOf(c4));
        if (txt5StarCount != null) txt5StarCount.setText(String.valueOf(c5));

        // 3. Renderizar opiniones filtradas
        if (commentsContainer != null) {
            commentsContainer.removeAllViews();

            java.util.List<org.json.JSONObject> filteredRatings = new java.util.ArrayList<>();
            if (mRatings != null) {
                for (int i = 0; i < mRatings.length(); i++) {
                    org.json.JSONObject item = mRatings.optJSONObject(i);
                    if (item == null) continue;

                    String role = item.optString("ratingRoleLabel", "");
                    if (mShowingDriver && "passenger_to_driver".equals(role)) {
                        filteredRatings.add(item);
                    } else if (!mShowingDriver && "driver_to_passenger".equals(role)) {
                        filteredRatings.add(item);
                    }
                }
            }

            if (filteredRatings.isEmpty()) {
                TextView emptyText = new TextView(AccountOverviewActivity.this);
                emptyText.setText(mShowingDriver ? "Aún no tienes opiniones como conductor." : "Aún no tienes opiniones como pasajero.");
                emptyText.setTextSize(14);
                emptyText.setTextColor(getResources().getColor(R.color.carpool_text_secondary, null));
                emptyText.setPadding(0, 10, 0, 10);
                commentsContainer.addView(emptyText);
            } else {
                int pad = (int) (12 * getResources().getDisplayMetrics().density);
                float density = getResources().getDisplayMetrics().density;

                for (org.json.JSONObject item : filteredRatings) {
                    int score = item.optInt("score", 5);
                    String comment = item.optString("comment", "");
                    String evaluatorName = item.optString("evaluatorName", "Usuario");
                    String tags = item.optString("tags", "");

                    android.widget.LinearLayout itemLayout = new android.widget.LinearLayout(AccountOverviewActivity.this);
                    itemLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
                    itemLayout.setBackgroundResource(R.drawable.bg_info_field);
                    itemLayout.setPadding(pad, pad, pad, pad);

                    android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
                    lp.bottomMargin = (int) (10 * density);
                    itemLayout.setLayoutParams(lp);

                    // Cabecera: Nombre y Estrellas
                    android.widget.LinearLayout header = new android.widget.LinearLayout(AccountOverviewActivity.this);
                    header.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                    header.setGravity(android.view.Gravity.CENTER_VERTICAL);

                    TextView nameText = new TextView(AccountOverviewActivity.this);
                    nameText.setText(evaluatorName);
                    nameText.setTextSize(13);
                    nameText.setTextColor(getResources().getColor(R.color.carpool_text_primary, null));
                    nameText.setTypeface(null, android.graphics.Typeface.BOLD);
                    nameText.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                    header.addView(nameText);

                    TextView starsText = new TextView(AccountOverviewActivity.this);
                    StringBuilder itemStars = new StringBuilder();
                    for (int s = 1; s <= 5; s++) {
                        itemStars.append(s <= score ? "★" : "☆");
                    }
                    starsText.setText(itemStars.toString());
                    starsText.setTextSize(12);
                    starsText.setTextColor(getResources().getColor(R.color.uber_accent, null));
                    header.addView(starsText);

                    itemLayout.addView(header);

                    // Tags / Chips
                    if (tags != null && !tags.trim().isEmpty()) {
                        android.widget.LinearLayout tagsContainer = new android.widget.LinearLayout(AccountOverviewActivity.this);
                        tagsContainer.setOrientation(android.widget.LinearLayout.HORIZONTAL);

                        android.widget.LinearLayout.LayoutParams tlp = new android.widget.LinearLayout.LayoutParams(
                                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
                        tlp.topMargin = (int) (6 * density);
                        tlp.bottomMargin = (int) (4 * density);
                        tagsContainer.setLayoutParams(tlp);

                        String[] splitTags = tags.split(",");
                        int tagPaddingVert = (int) (3 * density);
                        int tagPaddingHorz = (int) (8 * density);
                        int tagMarginRight = (int) (4 * density);

                        for (String tag : splitTags) {
                            TextView tagBadge = new TextView(AccountOverviewActivity.this);
                            tagBadge.setText(tag.trim());
                            tagBadge.setTextSize(10);
                            tagBadge.setPadding(tagPaddingHorz, tagPaddingVert, tagPaddingHorz, tagPaddingVert);
                            tagBadge.setBackgroundResource(R.drawable.bg_chip_unselected);
                            tagBadge.setTextColor(getResources().getColor(R.color.carpool_text_secondary, null));

                            android.widget.LinearLayout.LayoutParams tagLp = new android.widget.LinearLayout.LayoutParams(
                                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
                            tagLp.setMargins(0, 0, tagMarginRight, 0);
                            tagBadge.setLayoutParams(tagLp);

                            tagsContainer.addView(tagBadge);
                        }

                        itemLayout.addView(tagsContainer);
                    }

                    // Comentario
                    if (comment != null && !comment.trim().isEmpty()) {
                        TextView commentText = new TextView(AccountOverviewActivity.this);
                        commentText.setText(comment.trim());
                        commentText.setTextSize(14);
                        commentText.setTextColor(getResources().getColor(R.color.carpool_text_secondary, null));
                        android.widget.LinearLayout.LayoutParams clp = new android.widget.LinearLayout.LayoutParams(
                                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
                        clp.topMargin = (int) (6 * density);
                        commentText.setLayoutParams(clp);
                        itemLayout.addView(commentText);
                    }

                    commentsContainer.addView(itemLayout);
                }
            }
        }
    }

    private String generateInitials(String fullName) {
        if (fullName == null || fullName.isEmpty()) return "UI";
        String[] parts = fullName.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();
        for (int i = 0; i < Math.min(2, parts.length); i++) {
            if (parts[i].length() > 0) initials.append(parts[i].charAt(0));
        }
        return initials.length() > 0 ? initials.toString().toUpperCase() : "UI";
    }
}
