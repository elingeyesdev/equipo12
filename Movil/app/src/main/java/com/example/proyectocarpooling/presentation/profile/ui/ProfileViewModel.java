package com.example.proyectocarpooling.presentation.profile.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.proyectocarpooling.BackgroundTaskRunner;
import com.example.proyectocarpooling.CarPoolingApplication;
import com.example.proyectocarpooling.data.model.user.UpdateUserRequest;
import com.example.proyectocarpooling.data.model.user.UserResponse;
import com.example.proyectocarpooling.data.model.user.VehicleResponse;
import com.example.proyectocarpooling.domain.usecase.user.UserAccessUseCase;

import java.util.List;

public class ProfileViewModel extends AndroidViewModel {

    private final UserAccessUseCase userAccessUseCase;
    private final BackgroundTaskRunner taskRunner;

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<UserResponse> userProfile = new MutableLiveData<>();
    private final MutableLiveData<List<VehicleResponse>> vehicleList = new MutableLiveData<>();
    private final MutableLiveData<VehicleResponse> vehicleSaved = new MutableLiveData<>();
    private final MutableLiveData<String> errorEvent = new MutableLiveData<>();
    private final MutableLiveData<String> successEvent = new MutableLiveData<>();
    private final MutableLiveData<UserResponse> profileUpdated = new MutableLiveData<>();

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        CarPoolingApplication app = (CarPoolingApplication) application;
        userAccessUseCase = new UserAccessUseCase(app.getUserRepository());
        taskRunner = app.getTaskRunner();
    }

    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<UserResponse> getUserProfile() { return userProfile; }
    public LiveData<List<VehicleResponse>> getVehicleList() { return vehicleList; }
    public LiveData<VehicleResponse> getVehicleSaved() { return vehicleSaved; }
    public LiveData<String> getErrorEvent() { return errorEvent; }
    public LiveData<String> getSuccessEvent() { return successEvent; }
    public LiveData<UserResponse> getProfileUpdated() { return profileUpdated; }

    public void loadProfile(String userId) {
        loading.setValue(true);
        taskRunner.runWithResult(() -> userAccessUseCase.getById(userId),
                new BackgroundTaskRunner.ResultCallback<UserResponse>() {
                    @Override public void onSuccess(UserResponse user) {
                        loading.postValue(false);
                        userProfile.postValue(user);
                    }
                    @Override public void onError(String message) {
                        loading.postValue(false);
                    }
                });
    }

    public void loadVehicles(String userId) {
        taskRunner.runWithResult(() -> userAccessUseCase.getVehicles(userId),
                new BackgroundTaskRunner.ResultCallback<List<VehicleResponse>>() {
                    @Override public void onSuccess(List<VehicleResponse> vehicles) {
                        vehicleList.postValue(vehicles);
                    }
                    @Override public void onError(String message) {
                        errorEvent.postValue(message);
                    }
                });
    }

    public void saveVehicle(String userId, String editingVehicleId, String plate, String brand, String color, int seats) {
        loading.setValue(true);
        taskRunner.run(() -> {
            if (editingVehicleId != null && !editingVehicleId.isEmpty()) {
                userAccessUseCase.updateVehicle(userId, editingVehicleId, plate, brand, "", color, 0, seats);
            } else {
                userAccessUseCase.addVehicle(userId, plate, brand, "", color, 0, seats);
            }
        }, new BackgroundTaskRunner.SimpleCallback() {
            @Override public void onSuccess() {
                loading.postValue(false);
                vehicleSaved.postValue(null);
            }
            @Override public void onError(String message) {
                loading.postValue(false);
                errorEvent.postValue(message);
            }
        });
    }

    public void deleteVehicle(String userId, String vehicleId) {
        taskRunner.run(() -> userAccessUseCase.deleteVehicle(userId, vehicleId),
                new BackgroundTaskRunner.SimpleCallback() {
                    @Override public void onSuccess() {
                        successEvent.postValue("Vehiculo eliminado");
                    }
                    @Override public void onError(String message) {
                        errorEvent.postValue(message);
                    }
                });
    }

    public void updateProfile(String userId, UpdateUserRequest request) {
        loading.setValue(true);
        taskRunner.runWithResult(() -> userAccessUseCase.update(userId, request),
                new BackgroundTaskRunner.ResultCallback<UserResponse>() {
                    @Override public void onSuccess(UserResponse user) {
                        loading.postValue(false);
                        profileUpdated.postValue(user);
                    }
                    @Override public void onError(String message) {
                        loading.postValue(false);
                        errorEvent.postValue(message);
                    }
                });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
    }
}
