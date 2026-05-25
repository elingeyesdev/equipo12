package com.example.proyectocarpooling.data.model;

import org.json.JSONException;
import org.json.JSONObject;

public class ReservationResponse {

    public final String id;
    public final String tripId;
    public final String passengerUserId;
    public final String passengerName;
    public final int seatsReserved;
    public final String status;
    public final int statusId;
    public final String createdAt;

    public ReservationResponse(String id, String tripId, String passengerUserId, String passengerName,
                               int seatsReserved, String status, int statusId, String createdAt) {
        this.id = id;
        this.tripId = tripId;
        this.passengerUserId = passengerUserId;
        this.passengerName = passengerName;
        this.seatsReserved = seatsReserved;
        this.status = status;
        this.statusId = statusId;
        this.createdAt = createdAt;
    }

    public static ReservationResponse fromJson(JSONObject obj) {
        return new ReservationResponse(
                obj.optString("id", ""),
                obj.optString("tripId", ""),
                obj.optString("passengerUserId", ""),
                obj.optString("passengerName", ""),
                obj.optInt("seatsReserved", 1),
                obj.optString("status", ""),
                obj.optInt("statusId", 0),
                obj.optString("createdAt", "")
        );
    }

    public String getPassengerName() { return passengerName; }

    @Override
    public String toString() {
        return passengerName + " (" + status + ")";
    }
}
