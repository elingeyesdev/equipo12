package com.example.proyectocarpooling.data.model.history;

public class TripHistoryParticipantItem {
    public final String name;
    public final String statusLabel;
    public final String reservedAt;

    public TripHistoryParticipantItem(String name, String statusLabel, String reservedAt) {
        this.name = name;
        this.statusLabel = statusLabel;
        this.reservedAt = reservedAt;
    }
}
