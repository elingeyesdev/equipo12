package com.example.proyectocarpooling.data.model.user;

import org.json.JSONObject;

public class DriverProfileResponse {

    public final int availableSeats;
    public final String licensePlate;
    public final String vehicleBrand;
    public final String vehicleColor;

    public DriverProfileResponse(int availableSeats, String licensePlate, String vehicleBrand, String vehicleColor) {
        this.availableSeats = availableSeats;
        this.licensePlate = licensePlate;
        this.vehicleBrand = vehicleBrand;
        this.vehicleColor = vehicleColor;
    }

    public static DriverProfileResponse fromJson(JSONObject obj) {
        return new DriverProfileResponse(
                obj.optInt("availableSeats", 0),
                obj.optString("licensePlate", ""),
                obj.optString("vehicleBrand", ""),
                obj.optString("vehicleColor", "")
        );
    }
}
