package com.example.proyectocarpooling.data.model.history;

public class TripHistoryStats {
    public final int passengerTripsCount;
    public final int driverTripsCount;
    public final int totalTripsCount;

    public TripHistoryStats(int passengerTripsCount, int driverTripsCount, int totalTripsCount) {
        this.passengerTripsCount = passengerTripsCount;
        this.driverTripsCount = driverTripsCount;
        this.totalTripsCount = totalTripsCount;
    }
}
