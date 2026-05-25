package com.example.proyectocarpooling.data.model.user;

public class LoginUserRequest {

    public final String email;
    public final String password;

    public LoginUserRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }
}
