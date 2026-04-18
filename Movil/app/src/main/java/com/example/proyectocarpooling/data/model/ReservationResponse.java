package com.example.proyectocarpooling.data.model;

public class ReservationResponse {

    public final String id;
    public final String passengerName;
    public final String status;
    /** ISO-8601 del servidor o cadena vacía. */
    public final String createdAt;

    public ReservationResponse(String id, String passengerName, String status, String createdAt) {
        this.id = id;
        this.passengerName = passengerName;
        this.status = status;
        this.createdAt = createdAt == null ? "" : createdAt;
    }

    @Override
    public String toString() {
        return passengerName + " (" + status + ")";
    }
}
