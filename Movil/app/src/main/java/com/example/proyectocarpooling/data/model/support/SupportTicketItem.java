package com.example.proyectocarpooling.data.model.support;

import org.json.JSONException;
import org.json.JSONObject;

public class SupportTicketItem {

    public final String id;
    public final String userId;
    public final String tripId;
    public final String reservationId;
    public final int category;
    public final String categoryLabel;
    public final String subject;
    public final String description;
    public final int status;
    public final String statusLabel;
    public final String createdAt;
    public final String updatedAt;

    public SupportTicketItem(
            String id,
            String userId,
            String tripId,
            String reservationId,
            int category,
            String categoryLabel,
            String subject,
            String description,
            int status,
            String statusLabel,
            String createdAt,
            String updatedAt
    ) {
        this.id = id;
        this.userId = userId;
        this.tripId = tripId;
        this.reservationId = reservationId;
        this.category = category;
        this.categoryLabel = categoryLabel;
        this.subject = subject;
        this.description = description;
        this.status = status;
        this.statusLabel = statusLabel;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static SupportTicketItem fromJson(JSONObject o) throws JSONException {
        String tripId = null;
        if (!o.isNull("tripId") && o.has("tripId")) {
            tripId = o.optString("tripId", null);
            if (tripId != null && tripId.isEmpty()) {
                tripId = null;
            }
        }
        String reservationId = null;
        if (!o.isNull("reservationId") && o.has("reservationId")) {
            reservationId = o.optString("reservationId", null);
            if (reservationId != null && reservationId.isEmpty()) {
                reservationId = null;
            }
        }
        String updatedAt = null;
        if (!o.isNull("updatedAt")) {
            updatedAt = o.optString("updatedAt", null);
        }
        return new SupportTicketItem(
                o.getString("id"),
                o.getString("userId"),
                tripId,
                reservationId,
                o.getInt("category"),
                o.optString("categoryLabel", ""),
                o.getString("subject"),
                o.getString("description"),
                o.getInt("status"),
                o.optString("statusLabel", ""),
                o.getString("createdAt"),
                updatedAt
        );
    }
}
