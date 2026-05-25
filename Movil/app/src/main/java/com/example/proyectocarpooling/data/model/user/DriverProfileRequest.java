package com.example.proyectocarpooling.data.model.user;

public class DriverProfileRequest {

    public final int availableSeats;
    public final String licensePlate;
    public final String vehicleBrand;
    public final String vehicleColor;

    public DriverProfileRequest(int availableSeats, String licensePlate, String vehicleBrand, String vehicleColor) {
        this.availableSeats = availableSeats;
        this.licensePlate = licensePlate;
        this.vehicleBrand = vehicleBrand;
        this.vehicleColor = vehicleColor;
    }
}
