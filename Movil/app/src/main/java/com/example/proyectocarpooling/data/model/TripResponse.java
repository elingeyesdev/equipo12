package com.example.proyectocarpooling.data.model;

import org.json.JSONException;
import org.json.JSONObject;

public class TripResponse {

    public final String id;
    public final double originLatitude;
    public final double originLongitude;
    public final Double destinationLatitude;
    public final Double destinationLongitude;
    public final String status;
    public final int availableSeats;
    public final String driverName;

    public TripResponse(String id,
                        double originLatitude,
                        double originLongitude,
                        Double destinationLatitude,
                        Double destinationLongitude,
                        String status,
                        int availableSeats,
                        String driverName) {
        this.id = id;
        this.originLatitude = originLatitude;
        this.originLongitude = originLongitude;
        this.destinationLatitude = destinationLatitude;
        this.destinationLongitude = destinationLongitude;
        this.status = status;
        this.availableSeats = availableSeats;
        this.driverName = driverName == null ? "" : driverName;
    }

    public static TripResponse fromJson(String json) throws JSONException {
        JSONObject object = new JSONObject(json);
        Double destinationLat = object.isNull("destinationLatitude") ? null : object.getDouble("destinationLatitude");
        Double destinationLng = object.isNull("destinationLongitude") ? null : object.getDouble("destinationLongitude");

        Object statusValue = object.has("status") ? object.get("status") : null;
        String statusLabel = mapTripStatusToLabel(statusValue);

        return new TripResponse(
                object.getString("id"),
                object.getDouble("originLatitude"),
                object.getDouble("originLongitude"),
                destinationLat,
                destinationLng,
                statusLabel,
                object.optInt("availableSeats", 0),
                object.optString("driverName", "")
        );
    }

    private static String mapTripStatusToLabel(Object statusValue) {
        if (statusValue == null || statusValue == JSONObject.NULL) return "";

        if (statusValue instanceof Number) {
            int s = ((Number) statusValue).intValue();
            if (s == 0) return "activo";
            if (s == 1) return "listo";
            if (s == 2) return "cancelado";
            if (s == 3) return "en curso";
            if (s == 4) return "finalizado";
            return String.valueOf(s);
        }

        String normalized = String.valueOf(statusValue).trim().toLowerCase();
        if (normalized.equals("0") || normalized.equals("awaitingdestination") || normalized.equals("activo")) return "activo";
        if (normalized.equals("1") || normalized.equals("ready") || normalized.equals("listo")) return "listo";
        if (normalized.equals("2") || normalized.equals("cancelled") || normalized.equals("cancelado")) return "cancelado";
        if (normalized.equals("3") || normalized.equals("inprogress") || normalized.equals("in_progress") || normalized.equals("en_curso") || normalized.equals("en curso")) return "en curso";
        if (normalized.equals("4") || normalized.equals("finished") || normalized.equals("completed") || normalized.equals("finalizado")) return "finalizado";

        return String.valueOf(statusValue);
    }
}
