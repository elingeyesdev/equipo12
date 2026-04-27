package com.example.proyectocarpooling.data.model.history;

import org.json.JSONObject;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

public class TripHistoryDetailItem {
    public final String tripId;
    public final String category;
    public final String statusLabel;
    public final String createdAt;
    public final String startedAt;
    public final String finishedAt;
    public final String updatedAt;
    public final String originLabel;
    public final String destinationLabel;
    public final double originLatitude;
    public final double originLongitude;
    public final Double destinationLatitude;
    public final Double destinationLongitude;
    public final String driverName;
    public final String driverVehicleBrand;
    public final String driverVehicleColor;
    public final String driverLicensePlate;
    public final int reservationCount;
    public final int boardedCount;
    public final int cancelledCount;
    public final String passengerReservationStatus;
    public final String passengerName;
    public final List<TripHistoryParticipantItem> participants;

    public TripHistoryDetailItem(
            String tripId,
            String category,
            String statusLabel,
            String createdAt,
            String startedAt,
            String finishedAt,
            String updatedAt,
            String originLabel,
            String destinationLabel,
            double originLatitude,
            double originLongitude,
            Double destinationLatitude,
            Double destinationLongitude,
            String driverName,
            String driverVehicleBrand,
            String driverVehicleColor,
            String driverLicensePlate,
            int reservationCount,
            int boardedCount,
            int cancelledCount,
            String passengerReservationStatus,
            String passengerName,
            List<TripHistoryParticipantItem> participants
    ) {
        this.tripId = tripId;
        this.category = category;
        this.statusLabel = statusLabel;
        this.createdAt = createdAt;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.updatedAt = updatedAt;
        this.originLabel = originLabel;
        this.destinationLabel = destinationLabel;
        this.originLatitude = originLatitude;
        this.originLongitude = originLongitude;
        this.destinationLatitude = destinationLatitude;
        this.destinationLongitude = destinationLongitude;
        this.driverName = driverName;
        this.driverVehicleBrand = driverVehicleBrand;
        this.driverVehicleColor = driverVehicleColor;
        this.driverLicensePlate = driverLicensePlate;
        this.reservationCount = reservationCount;
        this.boardedCount = boardedCount;
        this.cancelledCount = cancelledCount;
        this.passengerReservationStatus = passengerReservationStatus;
        this.passengerName = passengerName;
        this.participants = participants;
    }

    public static TripHistoryDetailItem fromJson(JSONObject o) {
        Double dLat = o.isNull("destinationLatitude") ? null : o.optDouble("destinationLatitude");
        Double dLng = o.isNull("destinationLongitude") ? null : o.optDouble("destinationLongitude");
        List<TripHistoryParticipantItem> participants = new ArrayList<>();
        JSONArray arr = o.optJSONArray("participants");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject p = arr.optJSONObject(i);
                if (p == null) {
                    continue;
                }
                participants.add(new TripHistoryParticipantItem(
                        p.optString("name", ""),
                        p.optString("statusLabel", ""),
                        p.optString("reservedAt", "")
                ));
            }
        }
        return new TripHistoryDetailItem(
                o.optString("tripId", ""),
                o.optString("category", ""),
                o.optString("statusLabel", ""),
                o.optString("createdAt", ""),
                o.optString("startedAt", ""),
                o.optString("finishedAt", ""),
                o.optString("updatedAt", ""),
                o.optString("originLabel", ""),
                o.optString("destinationLabel", ""),
                o.optDouble("originLatitude", 0.0),
                o.optDouble("originLongitude", 0.0),
                dLat,
                dLng,
                o.optString("driverName", ""),
                o.optString("driverVehicleBrand", ""),
                o.optString("driverVehicleColor", ""),
                o.optString("driverLicensePlate", ""),
                o.optInt("reservationCount", 0),
                o.optInt("boardedCount", 0),
                o.optInt("cancelledCount", 0),
                o.optString("passengerReservationStatus", ""),
                o.optString("passengerName", ""),
                participants
        );
    }
}
