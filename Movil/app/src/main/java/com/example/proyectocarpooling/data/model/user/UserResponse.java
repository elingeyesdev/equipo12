package com.example.proyectocarpooling.data.model.user;

import org.json.JSONException;
import org.json.JSONObject;

public class UserResponse {

    public final String id;
    public final String fullName;
    public final String email;
    public final String phoneNumber;
    public final String createdAt;

    public UserResponse(String id, String fullName, String email, String phoneNumber, String createdAt) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.createdAt = createdAt;
    }

    public static UserResponse fromJson(String json) throws JSONException {
        JSONObject obj = new JSONObject(json);
        return new UserResponse(
                obj.getString("id"),
                obj.getString("fullName"),
                obj.getString("email"),
                obj.optString("phoneNumber", null),
                obj.optString("createdAt", "")
        );
    }
}
