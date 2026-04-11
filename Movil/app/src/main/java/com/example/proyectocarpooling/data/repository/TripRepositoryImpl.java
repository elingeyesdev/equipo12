package com.example.proyectocarpooling.data.repository;

import com.example.proyectocarpooling.data.model.ReservationResponse;
import com.example.proyectocarpooling.data.model.RouteData;
import com.example.proyectocarpooling.data.model.TripResponse;
import com.example.proyectocarpooling.data.remote.TripsRemoteDataSource;
import com.example.proyectocarpooling.domain.repository.TripRepository;
import com.mapbox.geojson.Point;

import java.io.IOException;
import java.util.List;

public class TripRepositoryImpl implements TripRepository {

    private final TripsRemoteDataSource remoteDataSource;

    public TripRepositoryImpl(TripsRemoteDataSource remoteDataSource) {
        this.remoteDataSource = remoteDataSource;
    }

    @Override
    public TripResponse createTrip(Point origin, Point destination) throws IOException {
        return remoteDataSource.createTrip(origin, destination);
    }

    @Override
    public TripResponse cancelTrip(String tripId) throws IOException {
        return remoteDataSource.cancelTrip(tripId);
    }

    @Override
    public RouteData fetchRoute(Point origin, Point destination) throws IOException {
        return remoteDataSource.fetchRoute(origin, destination);
    }

    @Override
    public TripResponse startTrip(String tripId, Point driverPosition) throws IOException {
        return remoteDataSource.startTrip(tripId, driverPosition);
    }

    @Override
    public TripResponse finishTrip(String tripId) throws IOException {
        return remoteDataSource.finishTrip(tripId);
    }

    @Override
    public void createReservation(String tripId, String passengerName) throws IOException {
        remoteDataSource.createReservation(tripId, passengerName);
    }

    @Override
    public List<ReservationResponse> getReservations(String tripId) throws IOException {
        return remoteDataSource.getReservations(tripId);
    }

    @Override
    public List<ReservationResponse> getBoardedPassengers(String tripId) throws IOException {
        return remoteDataSource.getBoardedPassengers(tripId);
    }

    @Override
    public void markBoardedByName(String tripId, String passengerName) throws IOException {
        remoteDataSource.markBoardedByName(tripId, passengerName);
    }

    @Override
    public void updateReservationStatus(String tripId, String reservationId, String targetStatus) throws IOException {
        remoteDataSource.updateReservationStatus(tripId, reservationId, targetStatus);
    }

    @Override
    public void cancelReservation(String reservationId) throws IOException {
        remoteDataSource.cancelReservation(reservationId);
    }
}
