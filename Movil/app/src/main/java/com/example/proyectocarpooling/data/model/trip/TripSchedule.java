package com.example.proyectocarpooling.data.model.trip;

import org.json.JSONException;
import org.json.JSONObject;

public class TripSchedule {
    public final String id;
    public final String driverUserId;
    public final String driverName;
    public final double originLatitude;
    public final double originLongitude;
    public final String originAddress;
    public final double destinationLatitude;
    public final double destinationLongitude;
    public final String destinationAddress;
    public final String departureTime; // e.g., "07:00:00"
    public final String daysOfWeek; // e.g., "1,2,3,4,5"
    public final String startDate;
    public final String endDate;
    public final String vehicleId;
    public final int offeredSeats;
    public final double fareAmount;
    public final boolean isActive;

    public TripSchedule(String id, String driverUserId, String driverName,
                        double originLatitude, double originLongitude, String originAddress,
                        double destinationLatitude, double destinationLongitude, String destinationAddress,
                        String departureTime, String daysOfWeek, String startDate, String endDate,
                        String vehicleId, int offeredSeats, double fareAmount, boolean isActive) {
        this.id = id;
        this.driverUserId = driverUserId;
        this.driverName = driverName;
        this.originLatitude = originLatitude;
        this.originLongitude = originLongitude;
        this.originAddress = originAddress;
        this.destinationLatitude = destinationLatitude;
        this.destinationLongitude = destinationLongitude;
        this.destinationAddress = destinationAddress;
        this.departureTime = departureTime;
        this.daysOfWeek = daysOfWeek;
        this.startDate = startDate;
        this.endDate = endDate;
        this.vehicleId = vehicleId;
        this.offeredSeats = offeredSeats;
        this.fareAmount = fareAmount;
        this.isActive = isActive;
    }

    public static TripSchedule fromJson(JSONObject obj) throws JSONException {
        JSONObject origin = obj.getJSONObject("origin");
        JSONObject destination = obj.getJSONObject("destination");

        return new TripSchedule(
                obj.getString("id"),
                obj.getString("driverUserId"),
                obj.optString("driverName", ""),
                origin.getDouble("latitude"),
                origin.getDouble("longitude"),
                origin.optString("addressLabel", ""),
                destination.getDouble("latitude"),
                destination.getDouble("longitude"),
                destination.optString("addressLabel", ""),
                obj.getString("departureTime"),
                obj.getString("daysOfWeek"),
                obj.getString("startDate"),
                obj.optString("endDate", null),
                obj.optString("vehicleId", null),
                obj.optInt("offeredSeats", 4),
                obj.optDouble("fareAmount", 10.0),
                obj.optBoolean("isActive", true)
        );
    }
}