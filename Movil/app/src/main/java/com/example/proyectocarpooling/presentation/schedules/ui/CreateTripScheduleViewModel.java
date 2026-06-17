package com.example.proyectocarpooling.presentation.schedules.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.proyectocarpooling.CarPoolingApplication;
import com.example.proyectocarpooling.data.model.trip.TripSchedule;
import com.example.proyectocarpooling.data.model.user.VehicleResponse;
import com.example.proyectocarpooling.data.remote.search.MapboxGeocodingRemoteDataSource;
import com.example.proyectocarpooling.domain.repository.user.UserRepository;
import com.example.proyectocarpooling.domain.usecase.trip.ManageScheduleUseCase;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CreateTripScheduleViewModel extends AndroidViewModel {

    private final ManageScheduleUseCase manageScheduleUseCase;
    private final UserRepository userRepository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<List<VehicleResponse>> vehicles = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> successEvent = new MutableLiveData<>(false);

    public CreateTripScheduleViewModel(@NonNull Application application) {
        super(application);
        CarPoolingApplication app = (CarPoolingApplication) application;
        this.manageScheduleUseCase = new ManageScheduleUseCase(app.getTripScheduleRepository());
        this.userRepository = app.getUserRepository();
    }

    public LiveData<List<VehicleResponse>> getVehicles() {
        return vehicles;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getSuccessEvent() {
        return successEvent;
    }

    public void loadVehicles(String userId) {
        loading.postValue(true);
        executor.execute(() -> {
            try {
                List<VehicleResponse> list = userRepository.getVehicles(userId);
                vehicles.postValue(list);
            } catch (IOException e) {
                errorMessage.postValue("Error al cargar vehículos: " + e.getMessage());
            } finally {
                loading.postValue(false);
            }
        });
    }

    public void createSchedule(String driverUserId, String originAddress, String destinationAddress,
                               String departureTime, String daysOfWeek, String startDate, String endDate,
                               String vehicleId, int offeredSeats, double fareAmount, String mapboxToken) {
        if (originAddress == null || originAddress.trim().isEmpty()) {
            errorMessage.postValue("Debe ingresar una dirección de origen");
            return;
        }
        if (destinationAddress == null || destinationAddress.trim().isEmpty()) {
            errorMessage.postValue("Debe ingresar una dirección de destino");
            return;
        }
        if (daysOfWeek == null || daysOfWeek.isEmpty()) {
            errorMessage.postValue("Debe seleccionar al menos un día");
            return;
        }
        if (departureTime == null || departureTime.isEmpty()) {
            errorMessage.postValue("Debe seleccionar la hora de salida");
            return;
        }
        if (vehicleId == null || vehicleId.isEmpty()) {
            errorMessage.postValue("Debe seleccionar un vehículo");
            return;
        }

        loading.postValue(true);
        executor.execute(() -> {
            try {
                // Geocode origin
                MapboxGeocodingRemoteDataSource geocoder = new MapboxGeocodingRemoteDataSource(mapboxToken);
                List<MapboxGeocodingRemoteDataSource.SearchSuggestion> originResults = geocoder.search(originAddress, 1);
                if (originResults.isEmpty()) {
                    errorMessage.postValue("No se pudo encontrar la ubicación del origen. Por favor sea más específico.");
                    loading.postValue(false);
                    return;
                }
                double originLatitude = originResults.get(0).latitude;
                double originLongitude = originResults.get(0).longitude;
                String finalOriginAddress = originResults.get(0).label;

                // Geocode destination
                List<MapboxGeocodingRemoteDataSource.SearchSuggestion> destinationResults = geocoder.search(destinationAddress, 1);
                if (destinationResults.isEmpty()) {
                    errorMessage.postValue("No se pudo encontrar la ubicación del destino. Por favor sea más específico.");
                    loading.postValue(false);
                    return;
                }
                double destinationLatitude = destinationResults.get(0).latitude;
                double destinationLongitude = destinationResults.get(0).longitude;
                String finalDestinationAddress = destinationResults.get(0).label;

                // Save schedule
                TripSchedule schedule = manageScheduleUseCase.createSchedule(
                        driverUserId, originLatitude, originLongitude, finalOriginAddress,
                        destinationLatitude, destinationLongitude, finalDestinationAddress,
                        departureTime, daysOfWeek, startDate, endDate, vehicleId, offeredSeats, fareAmount
                );

                if (schedule != null) {
                    successEvent.postValue(true);
                } else {
                    errorMessage.postValue("Error al crear el horario");
                }
            } catch (Exception e) {
                errorMessage.postValue("Error: " + e.getMessage());
            } finally {
                loading.postValue(false);
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}
