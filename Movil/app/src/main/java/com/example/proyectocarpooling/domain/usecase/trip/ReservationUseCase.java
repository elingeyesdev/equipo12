package com.example.proyectocarpooling.domain.usecase.trip;

import com.example.proyectocarpooling.data.model.trip.ReservationResponse;
import com.example.proyectocarpooling.domain.repository.trip.TripRepository;

import java.io.IOException;
import java.util.List;

public class ReservationUseCase {

    private final TripRepository repository;

    public ReservationUseCase(TripRepository repository) {
        this.repository = repository;
    }

    public void createReservation(String tripId, String passengerUserId, int seatsReserved) throws IOException {
        repository.createReservation(tripId, passengerUserId, seatsReserved);
    }

    public void acceptReservation(String tripId, String reservationId) throws IOException {
        repository.acceptReservation(tripId, reservationId);
    }

    public void rejectReservation(String tripId, String reservationId) throws IOException {
        repository.rejectReservation(tripId, reservationId);
    }

    public void boardPassenger(String tripId, String reservationId) throws IOException {
        repository.boardPassenger(tripId, reservationId);
    }

    public boolean verifyBoardingCode(String tripId, String reservationId, String code) throws IOException {
        return repository.verifyBoardingCode(tripId, reservationId, code);
    }

    public List<ReservationResponse> getReservations(String tripId) throws IOException {
        return repository.getReservations(tripId);
    }

    public List<ReservationResponse> getConfirmedReservations(String tripId) throws IOException {
        return repository.getConfirmedReservations(tripId);
    }

    public List<ReservationResponse> getBoardedPassengers(String tripId) throws IOException {
        return repository.getBoardedPassengers(tripId);
    }

    public void updateReservationStatus(String tripId, String reservationId, String targetStatus) throws IOException {
        repository.updateReservationStatus(tripId, reservationId, targetStatus);
    }

    public void cancelReservation(String reservationId) throws IOException {
        repository.cancelReservation(reservationId);
    }
}
