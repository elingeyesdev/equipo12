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

    public static TripHistorySummaryItem fromJson(JSONObject o) throws JSONException {
        return new TripHistorySummaryItem(
                o.optString("tripId", ""),
                o.optString("category", ""),
                o.optString("originLabel", ""),
                o.optString("destinationLabel", ""),
                o.optString("statusLabel", ""),
                o.optString("createdAt", "")
        );
    }
}
