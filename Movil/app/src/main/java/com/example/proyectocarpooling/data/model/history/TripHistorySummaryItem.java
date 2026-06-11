package com.example.proyectocarpooling.data.model.history;

import org.json.JSONException;
import org.json.JSONObject;

public class TripHistorySummaryItem {
    public final String tripId;
    public final String category;
    public final String originLabel;
    public final String destinationLabel;
    public final String statusLabel;
    public final String createdAt;

    public TripHistorySummaryItem(
            String tripId,
            String category,
            String originLabel,
            String destinationLabel,
            String statusLabel,
            String createdAt
    ) {
        this.tripId = tripId;
        this.category = category;
        this.originLabel = originLabel;
        this.destinationLabel = destinationLabel;
        this.statusLabel = statusLabel;
        this.createdAt = createdAt;
    }

    private static String optString(JSONObject o, String key, String fallback) {
        if (o.isNull(key)) return fallback;
        String s = o.optString(key, fallback);
        return "null".equalsIgnoreCase(s) ? fallback : s;
    }

    public static TripHistorySummaryItem fromJson(JSONObject o) throws JSONException {
        return new TripHistorySummaryItem(
                optString(o, "tripId", ""),
                optString(o, "category", ""),
                optString(o, "originLabel", ""),
                optString(o, "destinationLabel", ""),
                optString(o, "statusLabel", ""),
                optString(o, "createdAt", "")
        );
    }
}
