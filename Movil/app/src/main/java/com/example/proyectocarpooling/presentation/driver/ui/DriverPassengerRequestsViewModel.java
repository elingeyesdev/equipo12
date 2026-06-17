package com.example.proyectocarpooling.presentation.driver.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.proyectocarpooling.BackgroundTaskRunner;
import com.example.proyectocarpooling.CarPoolingApplication;
import com.example.proyectocarpooling.data.model.trip.ReservationResponse;
import com.example.proyectocarpooling.domain.repository.trip.TripRepository;

import java.util.List;

public class DriverPassengerRequestsViewModel extends AndroidViewModel {

    private final TripRepository tripRepository;
    private final BackgroundTaskRunner taskRunner;

    private final MutableLiveData<ReservationsData> reservations = new MutableLiveData<>();
    private final MutableLiveData<String> errorEvent = new MutableLiveData<>();
    private final MutableLiveData<String> successEvent = new MutableLiveData<>();

    public DriverPassengerRequestsViewModel(@NonNull Application application) {
        super(application);
        CarPoolingApplication app = (CarPoolingApplication) application;
        tripRepository = app.getTripRepository();
        taskRunner = app.getTaskRunner();
    }

    public LiveData<ReservationsData> getReservations() { return reservations; }
    public LiveData<String> getErrorEvent() { return errorEvent; }
    public LiveData<String> getSuccessEvent() { return successEvent; }

    public void loadReservations(String tripId) {
        taskRunner.runWithResult(() -> {
            List<ReservationResponse> pending = tripRepository.getReservations(tripId);
            List<ReservationResponse> confirmed = tripRepository.getConfirmedReservations(tripId);
            List<ReservationResponse> boarded = tripRepository.getBoardedPassengers(tripId);
            return new ReservationsData(pending, confirmed, boarded);
        }, new BackgroundTaskRunner.ResultCallback<ReservationsData>() {
            @Override public void onSuccess(ReservationsData data) {
                reservations.postValue(data);
            }
            @Override public void onError(String message) {
                errorEvent.postValue("Error cargando solicitudes");
            }
        });
    }

    public void acceptReservation(String tripId, String reservationId) {
        taskRunner.run(() -> tripRepository.acceptReservation(tripId, reservationId),
                new BackgroundTaskRunner.SimpleCallback() {
                    @Override public void onSuccess() { successEvent.postValue("Reserva aceptada"); }
                    @Override public void onError(String msg) { errorEvent.postValue("Error de red"); }
                });
    }

    public void rejectReservation(String tripId, String reservationId) {
        taskRunner.run(() -> tripRepository.rejectReservation(tripId, reservationId),
                new BackgroundTaskRunner.SimpleCallback() {
                    @Override public void onSuccess() { successEvent.postValue("Reserva rechazada"); }
                    @Override public void onError(String msg) { errorEvent.postValue("Error de red"); }
                });
    }

    public void verifyAndBoardPassenger(String tripId, String reservationId, String code) {
        taskRunner.run(() -> {
            boolean isValid = tripRepository.verifyBoardingCode(tripId, reservationId, code);
            if (!isValid) throw new Exception("Codigo invalido");
        }, new BackgroundTaskRunner.SimpleCallback() {
            @Override public void onSuccess() { successEvent.postValue("Abordaje confirmado"); }
            @Override public void onError(String msg) { errorEvent.postValue(msg); }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
    }

    public static class ReservationsData {
        public final List<ReservationResponse> pending;
        public final List<ReservationResponse> confirmed;
        public final List<ReservationResponse> boarded;
        ReservationsData(List<ReservationResponse> pending, List<ReservationResponse> confirmed, List<ReservationResponse> boarded) {
            this.pending = pending;
            this.confirmed = confirmed;
            this.boarded = boarded;
        }
    }
}
