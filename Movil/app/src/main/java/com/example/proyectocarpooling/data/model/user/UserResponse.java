package com.example.proyectocarpooling.data.model.user;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class UserResponse {

    public final String id;
    public final String fullName;
    public final String email;
    public final String phoneNumber;
    public final String role;
    public final String profilePicture;
    public final DriverProfileResponse driverProfile;
    public final List<VehicleResponse> vehicles;
    public final String createdAt;

    public UserResponse(String id, String fullName, String email, String phoneNumber, String role, String profilePicture,
                        DriverProfileResponse driverProfile, List<VehicleResponse> vehicles, String createdAt) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.role = role;
        this.profilePicture = profilePicture;
        this.driverProfile = driverProfile;
        this.vehicles = vehicles;
        this.createdAt = createdAt;
    }

    public static UserResponse fromJson(String json) throws JSONException {
        JSONObject obj = new JSONObject(json);
        DriverProfileResponse driverProfile = null;
        if (!obj.isNull("driverProfile")) {
            JSONObject profileObj = obj.optJSONObject("driverProfile");
            if (profileObj != null) {
                driverProfile = DriverProfileResponse.fromJson(profileObj);
            }
        }

        List<VehicleResponse> vehicles = new ArrayList<>();
        if (!obj.isNull("vehicles")) {
            JSONArray arr = obj.getJSONArray("vehicles");
            for (int i = 0; i < arr.length(); i++) {
                vehicles.add(VehicleResponse.fromJson(arr.getJSONObject(i)));
            }
        }

        return new UserResponse(
                obj.getString("id"),
                obj.getString("fullName"),
                obj.getString("email"),
                obj.optString("phoneNumber", null),
                obj.optString("role", "student"),
                obj.optString("profilePicture", ""),
                driverProfile,
                vehicles,
                obj.optString("createdAt", "")
        );
    }
}
