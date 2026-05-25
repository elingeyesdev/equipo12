package com.example.proyectocarpooling.data.model.user;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class VehicleResponse {

    public final String id;
    public final String licensePlate;
    public final String brand;
    public final String model;
    public final String color;
    public final int vehicleYear;
    public final int totalSeats;
    public final boolean isActive;
    public final boolean isVerified;

    public VehicleResponse(String id, String licensePlate, String brand, String model, String color,
                           int vehicleYear, int totalSeats, boolean isActive, boolean isVerified) {
        this.id = id;
        this.licensePlate = licensePlate;
        this.brand = brand;
        this.model = model;
        this.color = color;
        this.vehicleYear = vehicleYear;
        this.totalSeats = totalSeats;
        this.isActive = isActive;
        this.isVerified = isVerified;
    }

    public static VehicleResponse fromJson(JSONObject obj) {
        return new VehicleResponse(
                obj.optString("id", ""),
                obj.optString("licensePlate", ""),
                obj.optString("brand", ""),
                obj.optString("model", ""),
                obj.optString("color", ""),
                obj.optInt("vehicleYear", 0),
                obj.optInt("totalSeats", 4),
                obj.optBoolean("isActive", true),
                obj.optBoolean("isVerified", false)
        );
    }

    public static List<VehicleResponse> fromJsonArray(String json) throws org.json.JSONException {
        JSONArray arr = new JSONArray(json);
        List<VehicleResponse> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            list.add(fromJson(arr.getJSONObject(i)));
        }
        return list;
    }
}
