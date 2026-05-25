package com.example.proyectocarpooling.presentation.support;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.proyectocarpooling.R;

import java.util.Locale;

public final class SupportUiHelper {

    private SupportUiHelper() {
    }

    public static int statusColorRes(int status, String statusLabel) {
        if (status == 1) {
            return R.color.match_status_active;
        }
        if (status == 2) {
            return R.color.match_status_in_progress;
        }
        if (status == 3) {
            return R.color.match_status_finished;
        }
        if (status == 4) {
            return R.color.match_status_cancelled;
        }
        String s = statusLabel == null ? "" : statusLabel.trim().toLowerCase(Locale.US);
        if (s.contains("abierto") || s.equals("open")) {
            return R.color.match_status_active;
        }
        if (s.contains("revis") || s.contains("review")) {
            return R.color.match_status_in_progress;
        }
        if (s.contains("resuelt") || s.contains("resolved")) {
            return R.color.match_status_finished;
        }
        if (s.contains("cerrad") || s.contains("closed")) {
            return R.color.match_status_cancelled;
        }
        return R.color.carpool_primary;
    }

    public static void applyStatusPill(TextView view, Context context, int status, String statusLabel, String text) {
        int colorRes = statusColorRes(status, statusLabel);
        int bgColor = ContextCompat.getColor(context, colorRes);
        float radiusPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 20f, context.getResources().getDisplayMetrics());
        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(radiusPx);
        drawable.setColor(bgColor);
        view.setBackground(drawable);
        view.setTextColor(ContextCompat.getColor(context, R.color.white));
        view.setText(text);
    }

    public static String formatReference(String ticketId) {
        if (ticketId == null || ticketId.isEmpty()) {
            return "------";
        }
        String compact = ticketId.replace("-", "");
        if (compact.length() >= 8) {
            return compact.substring(0, 8).toUpperCase(Locale.US);
        }
        return compact.toUpperCase(Locale.US);
    }

    public static String formatDateTime(String iso) {
        if (iso == null || iso.trim().isEmpty()) {
            return "\u2014";
        }
        String t = iso.replace('T', ' ');
        return t.length() > 18 ? t.substring(0, 16) : t;
    }

    public static String shortId(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "\u2014";
        }
        String t = raw.trim();
        return t.length() <= 12 ? t : t.substring(0, 8) + "\u2026";
    }
}
