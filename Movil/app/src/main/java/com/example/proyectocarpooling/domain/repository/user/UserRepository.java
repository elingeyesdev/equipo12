package com.example.proyectocarpooling.domain.repository.user;

import com.example.proyectocarpooling.data.model.user.LoginUserRequest;
import com.example.proyectocarpooling.data.model.user.RegisterUserRequest;
import com.example.proyectocarpooling.data.model.user.UpdateUserRequest;
import com.example.proyectocarpooling.data.model.user.UserResponse;

import java.io.IOException;

public interface UserRepository {

    UserResponse register(RegisterUserRequest request) throws IOException;

    UserResponse login(LoginUserRequest request) throws IOException;

    UserResponse getById(String userId) throws IOException;

    UserResponse getByEmail(String email) throws IOException;

    UserResponse update(String userId, UpdateUserRequest request) throws IOException;

    void logout() throws IOException;
}
