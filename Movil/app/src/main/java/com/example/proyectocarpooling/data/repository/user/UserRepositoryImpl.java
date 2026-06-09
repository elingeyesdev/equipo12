package com.example.proyectocarpooling.data.repository.user;

import com.example.proyectocarpooling.data.model.user.LoginUserRequest;
import com.example.proyectocarpooling.data.model.user.RegisterUserRequest;
import com.example.proyectocarpooling.data.model.user.UpdateUserRequest;
import com.example.proyectocarpooling.data.model.user.UserResponse;
import com.example.proyectocarpooling.data.model.user.VehicleResponse;
import com.example.proyectocarpooling.data.remote.user.UsersRemoteDataSource;
import com.example.proyectocarpooling.domain.repository.user.UserRepository;

import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

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

    @Override
    public List<VehicleResponse> getVehicles(String userId) throws IOException {
        return remoteDataSource.getVehicles(userId);
    }

    @Override
    public VehicleResponse addVehicle(String userId, String licensePlate, String brand, String model,
                                       String color, int vehicleYear, int totalSeats) throws IOException {
        return remoteDataSource.addVehicle(userId, licensePlate, brand, model, color, vehicleYear, totalSeats);
    }

    @Override
    public VehicleResponse updateVehicle(String userId, String vehicleId, String licensePlate, String brand,
                                          String model, String color, int vehicleYear, int totalSeats) throws IOException {
        return remoteDataSource.updateVehicle(userId, vehicleId, licensePlate, brand, model, color, vehicleYear, totalSeats);
    }

    @Override
    public void deleteVehicle(String userId, String vehicleId) throws IOException {
        remoteDataSource.deleteVehicle(userId, vehicleId);
    }

    @Override
    public JSONObject getActiveReservation(String userId) throws IOException {
        return remoteDataSource.getActiveReservation(userId);
    }

    @Override
    public void registerFcmToken(String userId, String token) throws IOException {
        remoteDataSource.registerFcmToken(userId, token);
    }

    @Override
    public void logoutDevice(String token) throws IOException {
        remoteDataSource.logoutDevice(token);
    }
}
