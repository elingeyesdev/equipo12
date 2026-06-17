package com.example.proyectocarpooling.presentation.match.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.proyectocarpooling.BackgroundTaskRunner;
import com.example.proyectocarpooling.CarPoolingApplication;
import com.example.proyectocarpooling.domain.repository.trip.TripRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DriverMatchViewModel extends AndroidViewModel {

    private final TripRepository tripRepository;
    private final BackgroundTaskRunner taskRunner;

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<List<DriverCandidate>> candidates = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> errorEvent = new MutableLiveData<>();
    private final MutableLiveData<DriverCandidate> reservationResult = new MutableLiveData<>();

    public DriverMatchViewModel(@NonNull Application application) {
        super(application);
        CarPoolingApplication app = (CarPoolingApplication) application;
        tripRepository = app.getTripRepository();
        taskRunner = app.getTaskRunner();
    }

    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<List<DriverCandidate>> getCandidates() { return candidates; }
    public LiveData<String> getErrorEvent() { return errorEvent; }
    public LiveData<DriverCandidate> getReservationResult() { return reservationResult; }

    public void fetchCandidates(double refLat, double refLon) {
        loading.setValue(true);
        taskRunner.runWithResult(
                () -> DriverCandidateMapper.mapList(
                        tripRepository.searchTripMatchCandidates(refLat, refLon)),
                new BackgroundTaskRunner.ResultCallback<List<DriverCandidate>>() {
                    @Override public void onSuccess(List<DriverCandidate> list) {
                        loading.postValue(false);
                        candidates.postValue(list);
                    }
                    @Override public void onError(String message) {
                        loading.postValue(false);
                        errorEvent.postValue("Error cargando candidatos");
                    }
                });
    }

    public void createReservation(DriverCandidate candidate, String passengerUserId) {
        taskRunner.run(
                () -> tripRepository.createReservation(candidate.tripId, passengerUserId, 1),
                new BackgroundTaskRunner.SimpleCallback() {
                    @Override public void onSuccess() {
                        reservationResult.postValue(candidate);
                    }
                    @Override public void onError(String message) {
                        errorEvent.postValue("Error creando reserva");
                    }
                });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
    }

    public static class DriverCandidate {
        public final String tripId;
        public final String driverName;
        public final String routeDescription;
        public final int availableSeats;
        public final double distanceKm;
        public final int etaMinutes;
        public final String tripStatusKey;
        public final double originLatitude, originLongitude;
        public final double destinationLatitude, destinationLongitude;
        public final String vehicleInfo;
        public final String driverProfilePicture;

        public DriverCandidate(String tripId, String driverName, String routeDescription, int availableSeats,
                         double distanceKm, int etaMinutes, String tripStatusKey,
                         double originLatitude, double originLongitude,
                         double destinationLatitude, double destinationLongitude, String vehicleInfo, String driverProfilePicture) {
            this.tripId = tripId;
            this.driverName = driverName;
            this.routeDescription = routeDescription;
            this.availableSeats = availableSeats;
            this.distanceKm = distanceKm;
            this.etaMinutes = etaMinutes;
            this.tripStatusKey = tripStatusKey;
            this.originLatitude = originLatitude;
            this.originLongitude = originLongitude;
            this.destinationLatitude = destinationLatitude;
            this.destinationLongitude = destinationLongitude;
            this.vehicleInfo = vehicleInfo;
            this.driverProfilePicture = driverProfilePicture;
        }

        public boolean hasRouteEndpoints() {
            return Math.abs(originLatitude) > 1e-6 || Math.abs(originLongitude) > 1e-6;
        }

        public double getOriginLatitude() { return originLatitude; }
        public double getOriginLongitude() { return originLongitude; }
        public double getDestinationLatitude() { return destinationLatitude; }
        public double getDestinationLongitude() { return destinationLongitude; }
        public String getDriverName() { return driverName; }
        public String getTripId() { return tripId; }
    }
}
