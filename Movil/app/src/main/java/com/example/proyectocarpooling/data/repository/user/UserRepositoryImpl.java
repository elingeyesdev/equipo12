package com.example.proyectocarpooling.data.repository.user;

import com.example.proyectocarpooling.data.model.user.LoginUserRequest;
import com.example.proyectocarpooling.data.model.user.RegisterUserRequest;
import com.example.proyectocarpooling.data.model.user.UpdateUserRequest;
import com.example.proyectocarpooling.data.model.user.UserResponse;
import com.example.proyectocarpooling.data.remote.user.UsersRemoteDataSource;
import com.example.proyectocarpooling.domain.repository.user.UserRepository;

import java.io.IOException;

public class UserRepositoryImpl implements UserRepository {

    private final UsersRemoteDataSource remoteDataSource;

    public UserRepositoryImpl(UsersRemoteDataSource remoteDataSource) {
        this.remoteDataSource = remoteDataSource;
    }

    @Override
    public UserResponse register(RegisterUserRequest request) throws IOException {
        return remoteDataSource.register(request);
    }

    @Override
    public UserResponse login(LoginUserRequest request) throws IOException {
        return remoteDataSource.login(request);
    }

    @Override
    public UserResponse getById(String userId) throws IOException {
        return remoteDataSource.getById(userId);
    }

    @Override
    public UserResponse getByEmail(String email) throws IOException {
        return remoteDataSource.getByEmail(email);
    }

    @Override
    public UserResponse update(String userId, UpdateUserRequest request) throws IOException {
        return remoteDataSource.update(userId, request);
    }

    @Override
    public void logout() throws IOException {
        remoteDataSource.logout();
    }
}
