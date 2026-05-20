package com.example.proyectocarpooling.data.model.user;

import org.json.JSONObject;

public class DriverProfileResponse {

    public final String licenseNumber;

    public DriverProfileResponse(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public static DriverProfileResponse fromJson(JSONObject obj) {
        return new DriverProfileResponse(
                obj.optString("licenseNumber", null)
        );
    }
}
