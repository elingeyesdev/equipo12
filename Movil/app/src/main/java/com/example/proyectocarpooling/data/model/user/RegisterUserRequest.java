package com.example.proyectocarpooling.data.model.user;

public class RegisterUserRequest {

    public final String fullName;
    public final String email;
    public final String password;
    public final String phoneNumber;

    public RegisterUserRequest(String fullName, String email, String password, String phoneNumber) {
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.phoneNumber = phoneNumber;
    }
}
