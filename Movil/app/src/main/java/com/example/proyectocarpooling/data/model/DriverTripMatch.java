package com.example.proyectocarpooling.data.model;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Viaje ofrecido para la pantalla de match, ordenado por proximidad en el servidor.
 */
public final class DriverTripMatch {

    public final String tripId;
    public final String driverName;
    public final double originLatitude;
    public final double originLongitude;
    public final double destinationLatitude;
    public final double destinationLongitude;
    public final int status;
    public final int availableSeats;
    public final double distanceKm;
    public final int etaMinutes;

    public DriverTripMatch(
            String tripId,
            String driverName,
            double originLatitude,
            double originLongitude,
            double destinationLatitude,
            double destinationLongitude,
            int status,
            int availableSeats,
            double distanceKm,
            int etaMinutes) {
        this.tripId = tripId;
        this.driverName = driverName;
        this.originLatitude = originLatitude;
        this.originLongitude = originLongitude;
        this.destinationLatitude = destinationLatitude;
        this.destinationLongitude = destinationLongitude;
        this.status = status;
        this.availableSeats = availableSeats;
        this.distanceKm = distanceKm;
        this.etaMinutes = etaMinutes;
    }

    public static DriverTripMatch fromJson(JSONObject o) throws JSONException {
        return new DriverTripMatch(
                o.getString("tripId"),
                o.optString("driverName", "Conductor"),
                o.getDouble("originLatitude"),
                o.getDouble("originLongitude"),
                o.getDouble("destinationLatitude"),
                o.getDouble("destinationLongitude"),
                o.optInt("status", 0),
                o.optInt("availableSeats", 0),
                o.optDouble("distanceKm", 0.0),
                o.optInt("etaMinutes", 1));
    }
}
