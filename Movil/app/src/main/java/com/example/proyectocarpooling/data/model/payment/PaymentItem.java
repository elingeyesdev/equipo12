package com.example.proyectocarpooling.data.model.payment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PaymentItem {
    public static final int STATUS_PENDING = 1;
    public static final int STATUS_APPROVED = 2;
    public static final int STATUS_REJECTED = 3;
    public static final int STATUS_CANCELLED = 4;
    public static final int STATUS_EXPIRED = 5;
    public static final int STATUS_REFUNDED = 6;
    public static final int STATUS_PARTIALLY_REFUNDED = 7;

    public final String id;
    public final String reservationId;
    public final String passengerUserId;
    public final String passengerName;
    public final String tripId;
    public final String driverName;
    public final String paymentMethodCode;
    public final String paymentMethodName;
    public final double amount;
    public final double refundedAmount;
    public final String currency;
    public final int status;
    public final String description;
    public final String externalReference;
    public final String failureReason;
    public final String confirmedByName;
    public final String confirmedAt;
    public final String paidAt;
    public final String createdAt;
    public final String receiptNumber;
    public final List<RefundItem> refunds;

    public PaymentItem(String id, String reservationId, String passengerUserId, String passengerName,
                       String tripId, String driverName, String paymentMethodCode, String paymentMethodName,
                       double amount, double refundedAmount, String currency, int status,
                       String description, String externalReference, String failureReason,
                       String confirmedByName, String confirmedAt, String paidAt,
                       String createdAt, String receiptNumber, List<RefundItem> refunds) {
        this.id = id;
        this.reservationId = reservationId;
        this.passengerUserId = passengerUserId;
        this.passengerName = passengerName;
        this.tripId = tripId;
        this.driverName = driverName;
        this.paymentMethodCode = paymentMethodCode;
        this.paymentMethodName = paymentMethodName;
        this.amount = amount;
        this.refundedAmount = refundedAmount;
        this.currency = currency;
        this.status = status;
        this.description = description;
        this.externalReference = externalReference;
        this.failureReason = failureReason;
        this.confirmedByName = confirmedByName;
        this.confirmedAt = confirmedAt;
        this.paidAt = paidAt;
        this.createdAt = createdAt;
        this.receiptNumber = receiptNumber;
        this.refunds = refunds;
    }

    public static PaymentItem fromJson(JSONObject obj) {
        JSONObject receipt = obj.optJSONObject("receipt");
        JSONArray refundsArr = obj.optJSONArray("refunds");
        List<RefundItem> refundsList = RefundItem.listFromJson(refundsArr);
        return new PaymentItem(
                obj.optString("id", ""),
                obj.optString("reservationId", ""),
                obj.optString("passengerUserId", ""),
                obj.optString("passengerName", ""),
                obj.optString("tripId", ""),
                obj.optString("driverName", ""),
                obj.optString("paymentMethodCode", ""),
                obj.optString("paymentMethodName", ""),
                obj.optDouble("amount", 0.0),
                obj.optDouble("refundedAmount", 0.0),
                obj.optString("currency", "BOB"),
                obj.optInt("status", 0),
                obj.optString("description", ""),
                obj.optString("externalReference", ""),
                obj.optString("failureReason", ""),
                obj.optString("confirmedByName", ""),
                obj.optString("confirmedAt", ""),
                obj.optString("paidAt", ""),
                obj.optString("createdAt", ""),
                receipt != null ? receipt.optString("receiptNumber", "") : "",
                refundsList
        );
    }

    public static List<PaymentItem> listFromJson(JSONArray array) {
        List<PaymentItem> items = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.optJSONObject(i);
            if (obj != null) {
                items.add(fromJson(obj));
            }
        }
        return items;
    }

    public boolean isPendingManual() {
        return status == STATUS_PENDING && ("CASH".equalsIgnoreCase(paymentMethodCode) || "QR_BANK".equalsIgnoreCase(paymentMethodCode));
    }

    public String statusLabel() {
        switch (status) {
            case STATUS_PENDING: return "Pendiente";
            case STATUS_APPROVED: return "Pagado";
            case STATUS_REJECTED: return "Rechazado";
            case STATUS_CANCELLED: return "Cancelado";
            case STATUS_EXPIRED: return "Expirado";
            case STATUS_REFUNDED: return "Devuelto";
            case STATUS_PARTIALLY_REFUNDED: return "Devuelto parcial";
            default: return "Sin estado";
        }
    }
}
