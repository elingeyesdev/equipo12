package com.example.proyectocarpooling.domain.usecase;

import com.example.proyectocarpooling.data.model.ReservationResponse;
import com.example.proyectocarpooling.domain.repository.TripRepository;

import java.io.IOException;
import java.util.List;

public class ReservationUseCase {

    private final TripRepository repository;

    public ReservationUseCase(TripRepository repository) {
        this.repository = repository;
    }

    public void createReservation(String tripId, String passengerName) throws IOException {
        repository.createReservation(tripId, passengerName);
    }

    public List<ReservationResponse> getReservations(String tripId) throws IOException {
        return repository.getReservations(tripId);
    }

    public List<ReservationResponse> getBoardedPassengers(String tripId) throws IOException {
        return repository.getBoardedPassengers(tripId);
    }

    public void markBoardedByName(String tripId, String passengerName) throws IOException {
        repository.markBoardedByName(tripId, passengerName);
    }

    public void updateReservationStatus(String tripId, String reservationId, String targetStatus) throws IOException {
        repository.updateReservationStatus(tripId, reservationId, targetStatus);
    }

    public void cancelReservation(String reservationId) throws IOException {
        repository.cancelReservation(reservationId);
    }
}
