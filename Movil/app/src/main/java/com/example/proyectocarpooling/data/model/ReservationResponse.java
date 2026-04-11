package com.example.proyectocarpooling.data.model;

public class ReservationResponse {

    public final String id;
    public final String passengerName;
    public final String status;

    public ReservationResponse(String id, String passengerName, String status) {
        this.id = id;
        this.passengerName = passengerName;
        this.status = status;
    }

    @Override
    public String toString() {
        return passengerName + " (" + status + ")";
    }
}
