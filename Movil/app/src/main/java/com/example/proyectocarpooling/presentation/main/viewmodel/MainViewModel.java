package com.example.proyectocarpooling.presentation.main.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.example.proyectocarpooling.R;
import com.example.proyectocarpooling.data.local.ApiBaseUrlProvider;
import com.example.proyectocarpooling.data.local.SessionManager;
import com.example.proyectocarpooling.data.model.ReservationResponse;
import com.example.proyectocarpooling.data.model.TripResponse;
import com.example.proyectocarpooling.data.remote.TripsRemoteDataSource;
import com.example.proyectocarpooling.data.repository.TripRepositoryImpl;
import com.example.proyectocarpooling.domain.model.CreateTripResult;
import com.example.proyectocarpooling.domain.repository.TripRepository;
import com.example.proyectocarpooling.domain.usecase.CreateTripUseCase;
import com.example.proyectocarpooling.domain.usecase.ReservationUseCase;
import com.example.proyectocarpooling.domain.usecase.TripLifecycleUseCase;
import com.mapbox.geojson.Point;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainViewModel extends AndroidViewModel {

    public interface ResultCallback<T> {
        void onSuccess(T result);

        void onError(String message);
    }

    public interface SimpleCallback {
        void onSuccess();

        void onError(String message);
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final CreateTripUseCase createTripUseCase;
    private final TripLifecycleUseCase tripLifecycleUseCase;
    private final ReservationUseCase reservationUseCase;

    private Point selectedOrigin;
    private Point selectedDestination;
    private String activeTripId;
    private String lastTripStatusLabel;
    private int activeTripAvailableSeats;
    private String lastRouteTimeLabel;

    public MainViewModel(@NonNull Application application) {
        super(application);
        String apiBaseUrl = ApiBaseUrlProvider.get(application);
        String mapboxToken = application.getString(R.string.mapbox_access_token);

        TripsRemoteDataSource remoteDataSource = new TripsRemoteDataSource(apiBaseUrl, mapboxToken);
        TripRepository repository = new TripRepositoryImpl(remoteDataSource);

        createTripUseCase = new CreateTripUseCase(repository);
        tripLifecycleUseCase = new TripLifecycleUseCase(repository);
        reservationUseCase = new ReservationUseCase(repository);
    }

    public Point getSelectedOrigin() {
        return selectedOrigin;
    }

    public void setSelectedOrigin(Point selectedOrigin) {
        this.selectedOrigin = selectedOrigin;
    }

    public Point getSelectedDestination() {
        return selectedDestination;
    }

    public void setSelectedDestination(Point selectedDestination) {
        this.selectedDestination = selectedDestination;
    }

    public String getActiveTripId() {
        return activeTripId;
    }

    public void setActiveTripId(String activeTripId) {
        this.activeTripId = activeTripId;
    }

    public String getLastTripStatusLabel() {
        return lastTripStatusLabel;
    }

    public void setLastTripStatusLabel(String lastTripStatusLabel) {
        this.lastTripStatusLabel = lastTripStatusLabel;
    }

    public int getActiveTripAvailableSeats() {
        return activeTripAvailableSeats;
    }

    public void setActiveTripAvailableSeats(int activeTripAvailableSeats) {
        this.activeTripAvailableSeats = activeTripAvailableSeats;
    }

    public String getLastRouteTimeLabel() {
        return lastRouteTimeLabel;
    }

    public void setLastRouteTimeLabel(String lastRouteTimeLabel) {
        this.lastRouteTimeLabel = lastRouteTimeLabel;
    }

    public void createTrip(Point origin, Point destination, ResultCallback<CreateTripResult> callback) {
        SessionManager sessionManager = new SessionManager(getApplication());
        String driverName = sessionManager.isDriver() ? sessionManager.getFullName() : null;
        String driverUserId = sessionManager.isDriver() ? sessionManager.getUserId() : null;
        executeWithResult(() -> createTripUseCase.execute(origin, destination, driverName, driverUserId), callback);
    }

    public void cancelTrip(String tripId, ResultCallback<TripResponse> callback) {
        executeWithResult(() -> tripLifecycleUseCase.cancelTrip(tripId), callback);
    }

    public void startTrip(String tripId, Point driverPosition, ResultCallback<TripResponse> callback) {
        executeWithResult(() -> tripLifecycleUseCase.startTrip(tripId, driverPosition), callback);
    }

    public void finishTrip(String tripId, ResultCallback<TripResponse> callback) {
        executeWithResult(() -> tripLifecycleUseCase.finishTrip(tripId), callback);
    }

    public void createReservation(String tripId, String passengerName, SimpleCallback callback) {
        executeSimple(() -> reservationUseCase.createReservation(tripId, passengerName), callback);
    }

    public void getReservations(String tripId, ResultCallback<List<ReservationResponse>> callback) {
        executeWithResult(() -> reservationUseCase.getReservations(tripId), callback);
    }

    public void getBoardedPassengers(String tripId, ResultCallback<List<ReservationResponse>> callback) {
        executeWithResult(() -> reservationUseCase.getBoardedPassengers(tripId), callback);
    }

    public void markBoardedByName(String tripId, String passengerName, SimpleCallback callback) {
        executeSimple(() -> reservationUseCase.markBoardedByName(tripId, passengerName), callback);
    }

    public void updateReservationStatus(String tripId, String reservationId, String targetStatus, SimpleCallback callback) {
        executeSimple(() -> reservationUseCase.updateReservationStatus(tripId, reservationId, targetStatus), callback);
    }

    public void cancelReservation(String reservationId, SimpleCallback callback) {
        executeSimple(() -> reservationUseCase.cancelReservation(reservationId), callback);
    }

    private <T> void executeWithResult(TaskSupplier<T> taskSupplier, ResultCallback<T> callback) {
        executor.execute(() -> {
            try {
                T result = taskSupplier.run();
                mainHandler.post(() -> callback.onSuccess(result));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(buildErrorMessage(e)));
            }
        });
    }

    private void executeSimple(SimpleTask task, SimpleCallback callback) {
        executor.execute(() -> {
            try {
                task.run();
                mainHandler.post(callback::onSuccess);
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(buildErrorMessage(e)));
            }
        });
    }

    private String buildErrorMessage(Exception e) {
        String message = e.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return "Error de red inesperado";
        }
        return message;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdownNow();
    }

    private interface TaskSupplier<T> {
        T run() throws Exception;
    }

    private interface SimpleTask {
        void run() throws Exception;
    }
}
