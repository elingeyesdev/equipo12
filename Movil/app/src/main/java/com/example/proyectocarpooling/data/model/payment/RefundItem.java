package com.example.proyectocarpooling.data.model.payment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class RefundItem {
    public final String id;
    public final String paymentId;
    public final double amount;
    public final int status; // 1 = Requested, 2 = Processed, 3 = Rejected
    public final String reason;
    public final String rejectionReason;
    public final String requestedAt;
    public final String processedAt;

    public RefundItem(String id, String paymentId, double amount, int status,
                      String reason, String rejectionReason, String requestedAt, String processedAt) {
        this.id = id;
        this.paymentId = paymentId;
        this.amount = amount;
        this.status = status;
        this.reason = reason;
        this.rejectionReason = rejectionReason;
        this.requestedAt = requestedAt;
        this.processedAt = processedAt;
    }

    public static RefundItem fromJson(JSONObject obj) {
        if (obj == null) return null;
        return new RefundItem(
                obj.optString("id", ""),
                obj.optString("paymentId", ""),
                obj.optDouble("amount", 0.0),
                obj.optInt("status", 0),
                obj.optString("reason", ""),
                obj.optString("rejectionReason", ""),
                obj.optString("requestedAt", ""),
                obj.optString("processedAt", "")
        );
    }

    public static List<RefundItem> listFromJson(JSONArray array) {
        List<RefundItem> items = new ArrayList<>();
        if (array == null) return items;
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.optJSONObject(i);
            if (obj != null) {
                items.add(fromJson(obj));
            }
        }
        return items;
    }

    public String statusLabel() {
        switch (status) {
            case 1: return "Solicitado";
            case 2: return "Aprobado";
            case 3: return "Rechazado";
            default: return "Desconocido";
        }
    }
}
