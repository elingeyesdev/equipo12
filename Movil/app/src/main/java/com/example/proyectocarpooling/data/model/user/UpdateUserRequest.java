package com.example.proyectocarpooling.data.model.user;

public class UpdateUserRequest {

    public final String fullName;
    public final String email;
    public final String phoneNumber;
    public final String newPassword;

    public UpdateUserRequest(String fullName, String email, String phoneNumber, String newPassword) {
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.newPassword = newPassword;
    }
}
