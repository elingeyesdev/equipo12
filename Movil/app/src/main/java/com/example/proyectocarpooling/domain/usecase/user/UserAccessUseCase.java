package com.example.proyectocarpooling.domain.usecase.user;

import com.example.proyectocarpooling.data.model.user.LoginUserRequest;
import com.example.proyectocarpooling.data.model.user.RegisterUserRequest;
import com.example.proyectocarpooling.data.model.user.UpdateUserRequest;
import com.example.proyectocarpooling.data.model.user.UserResponse;
import com.example.proyectocarpooling.data.model.user.VehicleResponse;
import com.example.proyectocarpooling.domain.repository.user.UserRepository;

import java.io.IOException;
import java.util.List;

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

    public UserResponse update(String userId, UpdateUserRequest request) throws IOException {
        return repository.update(userId, request);
    }

    public void logout() throws IOException {
        repository.logout();
    }

    public List<VehicleResponse> getVehicles(String userId) throws IOException {
        return repository.getVehicles(userId);
    }

    public VehicleResponse addVehicle(String userId, String licensePlate, String brand, String model,
                                       String color, int vehicleYear, int totalSeats) throws IOException {
        return repository.addVehicle(userId, licensePlate, brand, model, color, vehicleYear, totalSeats);
    }

    public VehicleResponse updateVehicle(String userId, String vehicleId, String licensePlate, String brand,
                                          String model, String color, int vehicleYear, int totalSeats) throws IOException {
        return repository.updateVehicle(userId, vehicleId, licensePlate, brand, model, color, vehicleYear, totalSeats);
    }

    public void deleteVehicle(String userId, String vehicleId) throws IOException {
        repository.deleteVehicle(userId, vehicleId);
    }
}
