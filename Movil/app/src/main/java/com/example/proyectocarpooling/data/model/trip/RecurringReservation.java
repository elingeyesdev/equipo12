package com.example.proyectocarpooling.data.model.trip;

import org.json.JSONException;
import org.json.JSONObject;

public class RecurringReservation {
    public final String id;
    public final String tripScheduleId;
    public final String passengerUserId;
    public final String passengerName;
    public final int seatsReserved;
    public final boolean isActive;
    public final String createdAt;
    public final String originAddress;
    public final String destinationAddress;
    public final String departureTime;
    public final String daysOfWeek;
    public final String driverName;

    public RecurringReservation(String id, String tripScheduleId, String passengerUserId,
                                String passengerName, int seatsReserved, boolean isActive, String createdAt,
                                String originAddress, String destinationAddress, String departureTime,
                                String daysOfWeek, String driverName) {
        this.id = id;
        this.tripScheduleId = tripScheduleId;
        this.passengerUserId = passengerUserId;
        this.passengerName = passengerName;
        this.seatsReserved = seatsReserved;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.originAddress = originAddress;
        this.destinationAddress = destinationAddress;
        this.departureTime = departureTime;
        this.daysOfWeek = daysOfWeek;
        this.driverName = driverName;
    }

    public static RecurringReservation fromJson(JSONObject obj) throws JSONException {
        return new RecurringReservation(
                obj.getString("id"),
                obj.getString("tripScheduleId"),
                obj.getString("passengerUserId"),
                obj.optString("passengerName", ""),
                obj.optInt("seatsReserved", 1),
                obj.optBoolean("isActive", true),
                obj.optString("createdAt", ""),
                obj.optString("originAddress", ""),
                obj.optString("destinationAddress", ""),
                obj.optString("departureTime", ""),
                obj.optString("daysOfWeek", ""),
                obj.optString("driverName", "")
        );
    }
}