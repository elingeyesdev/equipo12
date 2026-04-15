package com.example.proyectocarpooling.data.model.user;

import org.json.JSONException;
import org.json.JSONObject;

public class UserResponse {

    public final String id;
    public final String fullName;
    public final String email;
    public final String phoneNumber;
    public final String role;
    public final DriverProfileResponse driverProfile;
    public final String createdAt;

    public UserResponse(String id, String fullName, String email, String phoneNumber, String role, DriverProfileResponse driverProfile, String createdAt) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.role = role;
        this.driverProfile = driverProfile;
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

        return new UserResponse(
                obj.getString("id"),
                obj.getString("fullName"),
                obj.getString("email"),
                obj.optString("phoneNumber", null),
                obj.optString("role", "student"),
                driverProfile,
                obj.optString("createdAt", "")
        );
    }
}
