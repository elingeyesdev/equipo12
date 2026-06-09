package com.example.proyectocarpooling.domain.repository.user;

import com.example.proyectocarpooling.data.model.user.LoginUserRequest;
import com.example.proyectocarpooling.data.model.user.RegisterUserRequest;
import com.example.proyectocarpooling.data.model.user.UpdateUserRequest;
import com.example.proyectocarpooling.data.model.user.UserResponse;
import com.example.proyectocarpooling.data.model.user.VehicleResponse;

import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

public interface UserRepository {

    UserResponse register(RegisterUserRequest request) throws IOException;

    UserResponse login(LoginUserRequest request) throws IOException;

    UserResponse getById(String userId) throws IOException;

    UserResponse getByEmail(String email) throws IOException;

    UserResponse update(String userId, UpdateUserRequest request) throws IOException;

    void logout() throws IOException;

    List<VehicleResponse> getVehicles(String userId) throws IOException;

    VehicleResponse addVehicle(String userId, String licensePlate, String brand, String model,
                                String color, int vehicleYear, int totalSeats) throws IOException;

    VehicleResponse updateVehicle(String userId, String vehicleId, String licensePlate, String brand,
                                   String model, String color, int vehicleYear, int totalSeats) throws IOException;

    void deleteVehicle(String userId, String vehicleId) throws IOException;

    JSONObject getActiveReservation(String userId) throws IOException;

    void registerFcmToken(String userId, String token) throws IOException;

    void logoutDevice(String token) throws IOException;
}
