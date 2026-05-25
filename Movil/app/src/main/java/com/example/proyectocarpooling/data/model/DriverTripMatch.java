package com.example.proyectocarpooling.data.model;

import org.json.JSONException;
import org.json.JSONObject;

public final class DriverTripMatch {

    public final String tripId;
    public final String driverName;
    public final double originLatitude;
    public final double originLongitude;
    public final String originAddress;
    public final double destinationLatitude;
    public final double destinationLongitude;
    public final String destinationAddress;
    public final String statusLabel;
    public final int availableSeats;
    public final double distanceKm;
    public final int etaMinutes;
    public final String vehicleBrand;
    public final String vehicleColor;
    public final String vehiclePlate;

    public DriverTripMatch(
            String tripId, String driverName,
            double originLatitude, double originLongitude, String originAddress,
            double destinationLatitude, double destinationLongitude, String destinationAddress,
            String statusLabel, int availableSeats, double distanceKm, int etaMinutes,
            String vehicleBrand, String vehicleColor, String vehiclePlate) {
        this.tripId = tripId;
        this.driverName = driverName;
        this.originLatitude = originLatitude;
        this.originLongitude = originLongitude;
        this.originAddress = originAddress;
        this.destinationLatitude = destinationLatitude;
        this.destinationLongitude = destinationLongitude;
        this.destinationAddress = destinationAddress;
        this.statusLabel = statusLabel;
        this.availableSeats = availableSeats;
        this.distanceKm = distanceKm;
        this.etaMinutes = etaMinutes;
        this.vehicleBrand = vehicleBrand;
        this.vehicleColor = vehicleColor;
        this.vehiclePlate = vehiclePlate;
    }

    public static DriverTripMatch fromJson(JSONObject o) throws JSONException {
        JSONObject origin = o.getJSONObject("origin");
        JSONObject destination = o.getJSONObject("destination");

        return new DriverTripMatch(
                o.getString("tripId"),
                o.optString("driverName", "Conductor"),
                origin.getDouble("latitude"),
                origin.getDouble("longitude"),
                origin.optString("addressLabel", ""),
                destination.getDouble("latitude"),
                destination.getDouble("longitude"),
                destination.optString("addressLabel", ""),
                o.optString("statusLabel", ""),
                o.optInt("availableSeats", 0),
                o.optDouble("distanceKm", 0.0),
                o.optInt("etaMinutes", 1),
                o.optString("vehicleBrand", ""),
                o.optString("vehicleColor", ""),
                o.optString("vehiclePlate", ""));
    }
}
