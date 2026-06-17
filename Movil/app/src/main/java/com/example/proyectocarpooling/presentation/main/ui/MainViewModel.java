package com.example.proyectocarpooling.presentation.main.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.proyectocarpooling.BackgroundTaskRunner;
import com.example.proyectocarpooling.CarPoolingApplication;
import com.example.proyectocarpooling.data.local.SessionManager;
import com.example.proyectocarpooling.data.model.trip.ReservationResponse;
import com.example.proyectocarpooling.data.model.trip.TripResponse;
import com.example.proyectocarpooling.data.model.user.VehicleResponse;
import com.example.proyectocarpooling.data.remote.search.MapboxGeocodingRemoteDataSource;
import com.example.proyectocarpooling.domain.model.trip.CreateTripResult;
import com.example.proyectocarpooling.domain.repository.trip.TripRepository;
import com.example.proyectocarpooling.domain.repository.user.UserRepository;
import com.example.proyectocarpooling.domain.usecase.trip.CreateTripUseCase;
import com.example.proyectocarpooling.domain.usecase.trip.ReservationUseCase;
import com.example.proyectocarpooling.domain.usecase.trip.TripLifecycleUseCase;
import com.mapbox.geojson.Point;

import org.json.JSONObject;

import java.util.List;

public class MainViewModel extends AndroidViewModel {

    public interface ResultCallback<T> {
        void onSuccess(T result);
        void onError(String message);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onError(String message);
    }

    private final CreateTripUseCase createTripUseCase;
    private final TripLifecycleUseCase tripLifecycleUseCase;
    private final ReservationUseCase reservationUseCase;
    private final UserRepository userRepository;
    private final BackgroundTaskRunner taskRunner;
    private final SessionManager sessionManager;

    private final MutableLiveData<Point> selectedOrigin = new MutableLiveData<>();
    private final MutableLiveData<Point> selectedDestination = new MutableLiveData<>();
    private final MutableLiveData<String> selectedOriginAddress = new MutableLiveData<>();
    private final MutableLiveData<String> selectedDestinationAddress = new MutableLiveData<>();
    private final MutableLiveData<String> activeTripId = new MutableLiveData<>();
    private final MutableLiveData<String> lastTripStatusLabel = new MutableLiveData<>();
    private final MutableLiveData<Integer> activeTripAvailableSeats = new MutableLiveData<>(0);
    private final MutableLiveData<String> lastRouteTimeLabel = new MutableLiveData<>();

    public MainViewModel(@NonNull Application application) {
        super(application);
        CarPoolingApplication app = (CarPoolingApplication) application;
        TripRepository tripRepository = app.getTripRepository();
        userRepository = app.getUserRepository();
        sessionManager = app.getSessionManager();
        taskRunner = app.getTaskRunner();

        createTripUseCase = new CreateTripUseCase(tripRepository);
        tripLifecycleUseCase = new TripLifecycleUseCase(tripRepository);
        reservationUseCase = new ReservationUseCase(tripRepository);
    }

    public SessionManager getSessionManager() { return sessionManager; }

    public LiveData<Point> getSelectedOrigin() { return selectedOrigin; }
    public void setSelectedOrigin(Point p) { this.selectedOrigin.setValue(p); }

    public LiveData<String> getSelectedOriginAddress() { return selectedOriginAddress; }
    public void setSelectedOriginAddress(String s) { this.selectedOriginAddress.setValue(s); }

    public LiveData<Point> getSelectedDestination() { return selectedDestination; }
    public void setSelectedDestination(Point p) { this.selectedDestination.setValue(p); }

    public LiveData<String> getSelectedDestinationAddress() { return selectedDestinationAddress; }
    public void setSelectedDestinationAddress(String s) { this.selectedDestinationAddress.setValue(s); }

    public LiveData<String> getActiveTripId() { return activeTripId; }
    public void setActiveTripId(String id) { this.activeTripId.setValue(id); }

    public LiveData<String> getLastTripStatusLabel() { return lastTripStatusLabel; }
    public void setLastTripStatusLabel(String s) { this.lastTripStatusLabel.setValue(s); }

    public LiveData<Integer> getActiveTripAvailableSeats() { return activeTripAvailableSeats; }
    public void setActiveTripAvailableSeats(int n) { this.activeTripAvailableSeats.setValue(n); }

    public LiveData<String> getLastRouteTimeLabel() { return lastRouteTimeLabel; }
    public void setLastRouteTimeLabel(String s) { this.lastRouteTimeLabel.setValue(s); }

    public void reverseGeocodePoint(final Point point, final boolean isOrigin, String token) {
        if (point == null) return;
        taskRunner.runWithResult(() -> {
            MapboxGeocodingRemoteDataSource geocoder = new MapboxGeocodingRemoteDataSource(token);
            return geocoder.reverseGeocode(point.latitude(), point.longitude());
        }, adapt(new ResultCallback<String>() {
            @Override
            public void onSuccess(String address) {
                if (isOrigin) {
                    selectedOriginAddress.postValue(address);
                } else {
                    selectedDestinationAddress.postValue(address);
                }
            }

            @Override
            public void onError(String message) {
                // Ignore or log error
            }
        }));
    }

    public void createTrip(Point origin, Point destination, ResultCallback<CreateTripResult> callback) {
        createTrip(origin, destination, null, callback);
    }

    public void createTrip(Point origin, Point destination, String vehicleId, ResultCallback<CreateTripResult> callback) {
        createTrip(origin, destination, vehicleId, 10.0, callback);
    }

    public void createTrip(Point origin, Point destination, String vehicleId, double fareAmount, ResultCallback<CreateTripResult> callback) {
        String driverName = sessionManager.isDriver() ? sessionManager.getFullName() : null;
        String driverUserId = sessionManager.isDriver() ? sessionManager.getUserId() : null;
        taskRunner.runWithResult(
                () -> createTripUseCase.execute(origin, destination, driverName, driverUserId, vehicleId, fareAmount),
                adapt(callback));
    }

    public void cancelTrip(String tripId, ResultCallback<TripResponse> callback) {
        taskRunner.runWithResult(() -> tripLifecycleUseCase.cancelTrip(tripId), adapt(callback));
    }

    public void startTrip(String tripId, Point driverPosition, ResultCallback<TripResponse> callback) {
        taskRunner.runWithResult(() -> tripLifecycleUseCase.startTrip(tripId, driverPosition), adapt(callback));
    }

    public void finishTrip(String tripId, ResultCallback<TripResponse> callback) {
        taskRunner.runWithResult(() -> tripLifecycleUseCase.finishTrip(tripId), adapt(callback));
    }

    public void updateTripLocation(String tripId, double latitude, double longitude, ResultCallback<TripResponse> callback) {
        taskRunner.runWithResult(() -> tripLifecycleUseCase.updateTripLocation(tripId, latitude, longitude), adapt(callback));
    }

    public void createReservation(String tripId, String passengerUserId, int seats, SimpleCallback callback) {
        taskRunner.run(() -> reservationUseCase.createReservation(tripId, passengerUserId, seats), adapt(callback));
    }

    public void getReservations(String tripId, ResultCallback<List<ReservationResponse>> callback) {
        taskRunner.runWithResult(() -> reservationUseCase.getReservations(tripId), adapt(callback));
    }

    public void getConfirmedReservations(String tripId, ResultCallback<List<ReservationResponse>> callback) {
        taskRunner.runWithResult(() -> reservationUseCase.getConfirmedReservations(tripId), adapt(callback));
    }

    public void getBoardedPassengers(String tripId, ResultCallback<List<ReservationResponse>> callback) {
        taskRunner.runWithResult(() -> reservationUseCase.getBoardedPassengers(tripId), adapt(callback));
    }

    public void updateReservationStatus(String tripId, String reservationId, String targetStatus, SimpleCallback callback) {
        taskRunner.run(() -> reservationUseCase.updateReservationStatus(tripId, reservationId, targetStatus), adapt(callback));
    }

    public void cancelReservation(String reservationId, SimpleCallback callback) {
        taskRunner.run(() -> reservationUseCase.cancelReservation(reservationId), adapt(callback));
    }

    public void verifyBoardingCode(String tripId, String reservationId, String code, SimpleCallback callback) {
        taskRunner.run(() -> {
            boolean ok = reservationUseCase.verifyBoardingCode(tripId, reservationId, code);
            if (!ok) throw new Exception("Codigo invalido");
        }, adapt(callback));
    }

    public void boardPassenger(String tripId, String reservationId, SimpleCallback callback) {
        taskRunner.run(() -> reservationUseCase.boardPassenger(tripId, reservationId), adapt(callback));
    }

    public void getVehiclesForUser(String userId, ResultCallback<List<VehicleResponse>> callback) {
        taskRunner.runWithResult(() -> userRepository.getVehicles(userId), adapt(callback));
    }

    public void getActiveReservation(String userId, ResultCallback<JSONObject> callback) {
        taskRunner.runWithResult(() -> userRepository.getActiveReservation(userId), adapt(callback));
    }

    private <T> BackgroundTaskRunner.ResultCallback<T> adapt(ResultCallback<T> cb) {
        return new BackgroundTaskRunner.ResultCallback<T>() {
            @Override public void onSuccess(T result) { cb.onSuccess(result); }
            @Override public void onError(String message) { cb.onError(message); }
        };
    }

    private BackgroundTaskRunner.SimpleCallback adapt(SimpleCallback cb) {
        return new BackgroundTaskRunner.SimpleCallback() {
            @Override public void onSuccess() { cb.onSuccess(); }
            @Override public void onError(String message) { cb.onError(message); }
        };
    }

    @Override
    protected void onCleared() {
        super.onCleared();
    }
}
