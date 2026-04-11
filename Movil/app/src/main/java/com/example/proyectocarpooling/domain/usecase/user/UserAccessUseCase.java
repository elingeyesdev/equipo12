package com.example.proyectocarpooling.domain.usecase.user;

import com.example.proyectocarpooling.data.model.user.LoginUserRequest;
import com.example.proyectocarpooling.data.model.user.RegisterUserRequest;
import com.example.proyectocarpooling.data.model.user.UserResponse;
import com.example.proyectocarpooling.domain.repository.user.UserRepository;

import java.io.IOException;

public class UserAccessUseCase {

    private final UserRepository repository;

    public UserAccessUseCase(UserRepository repository) {
        this.repository = repository;
    }

    public UserResponse register(RegisterUserRequest request) throws IOException {
        return repository.register(request);
    }

    public UserResponse login(LoginUserRequest request) throws IOException {
        return repository.login(request);
    }

    public UserResponse getById(String userId) throws IOException {
        return repository.getById(userId);
    }

    public UserResponse getByEmail(String email) throws IOException {
        return repository.getByEmail(email);
    }
}
