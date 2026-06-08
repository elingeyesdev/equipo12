package com.example.proyectocarpooling.data.model;

import org.json.JSONException;
import org.json.JSONObject;

public class TripResponse {

    public final String id;
    public final double originLatitude;
    public final double originLongitude;
    public final String originAddress;
    public final double destinationLatitude;
    public final double destinationLongitude;
    public final String destinationAddress;
    public final String statusLabel;
    public final int statusId;
    public final int offeredSeats;
    public final int availableSeats;
    public final double fareAmount;
    public final String vehicleId;
    public final String driverName;
    public final String driverUserId;

    public TripResponse(String id, double originLatitude, double originLongitude, String originAddress,
                        double destinationLatitude, double destinationLongitude, String destinationAddress,
                        String statusLabel, int statusId, int offeredSeats, int availableSeats,
                        double fareAmount, String vehicleId, String driverName, String driverUserId) {
        this.id = id;
        this.originLatitude = originLatitude;
        this.originLongitude = originLongitude;
        this.originAddress = originAddress;
        this.destinationLatitude = destinationLatitude;
        this.destinationLongitude = destinationLongitude;
        this.destinationAddress = destinationAddress;
        this.statusLabel = statusLabel;
        this.statusId = statusId;
        this.offeredSeats = offeredSeats;
        this.availableSeats = availableSeats;
        this.fareAmount = fareAmount;
        this.vehicleId = vehicleId;
        this.driverName = driverName;
        this.driverUserId = driverUserId;
    }

    public static TripResponse fromJson(String json) throws JSONException {
        JSONObject obj = new JSONObject(json);
        JSONObject origin = obj.getJSONObject("origin");
        JSONObject destination = obj.getJSONObject("destination");

        return new TripResponse(
                obj.getString("id"),
                origin.getDouble("latitude"),
                origin.getDouble("longitude"),
                origin.optString("addressLabel", ""),
                destination.getDouble("latitude"),
                destination.getDouble("longitude"),
                destination.optString("addressLabel", ""),
                obj.optString("statusLabel", ""),
                obj.optInt("statusId", 0),
                obj.optInt("offeredSeats", 0),
                obj.optInt("availableSeats", 0),
                obj.optDouble("fareAmount", 10.0),
                obj.optString("vehicleId", null),
                obj.optString("driverName", ""),
                obj.optString("driverUserId", null)
        );
    }
}
