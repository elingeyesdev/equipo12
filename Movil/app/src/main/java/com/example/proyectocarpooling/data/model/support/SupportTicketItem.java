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
    public final String firstAdminReplyAt;
    public final String lastMessageAt;
    public final boolean chatEnabled;

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
            String updatedAt,
            String firstAdminReplyAt,
            String lastMessageAt,
            boolean chatEnabled
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
        this.firstAdminReplyAt = firstAdminReplyAt;
        this.lastMessageAt = lastMessageAt;
        this.chatEnabled = chatEnabled;
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
        String firstAdminReplyAt = null;
        if (!o.isNull("firstAdminReplyAt")) {
            firstAdminReplyAt = o.optString("firstAdminReplyAt", null);
        }
        String lastMessageAt = null;
        if (!o.isNull("lastMessageAt")) {
            lastMessageAt = o.optString("lastMessageAt", null);
        }
        boolean chatEnabled = o.optBoolean("chatEnabled", false);
        if (!chatEnabled && firstAdminReplyAt != null && !firstAdminReplyAt.isEmpty()) {
            chatEnabled = true;
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
                updatedAt,
                firstAdminReplyAt,
                lastMessageAt,
                chatEnabled
        );
    }
}
