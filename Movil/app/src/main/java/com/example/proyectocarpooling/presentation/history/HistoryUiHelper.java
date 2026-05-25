package com.example.proyectocarpooling.presentation.history;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.proyectocarpooling.R;

import java.util.Locale;

public final class HistoryUiHelper {

    private HistoryUiHelper() {
    }

    public static int statusColorRes(String statusLabel) {
        String s = statusLabel == null ? "" : statusLabel.trim().toLowerCase(Locale.US);
        if (s.equals("listo") || s.equals("ready") || s.equals("1")) {
            return R.color.match_status_ready;
        }
        if (s.equals("en curso") || s.equals("en_curso") || s.equals("inprogress") || s.equals("3")) {
            return R.color.match_status_in_progress;
        }
        if (s.equals("cancelado") || s.equals("cancelled") || s.equals("2")) {
            return R.color.match_status_cancelled;
        }
        if (s.equals("finalizado") || s.equals("finished") || s.equals("4")) {
            return R.color.match_status_finished;
        }
        if (s.equals("activo") || s.equals("awaitingdestination") || s.equals("pending") || s.equals("0")) {
            return R.color.match_status_active;
        }
        return R.color.carpool_primary;
    }

    public static void applyStatusPill(TextView view, Context context, String statusLabel, String text) {
        int colorRes = statusColorRes(statusLabel);
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
}
