package com.example.proyectocarpooling.data.repository.trip;

import com.example.proyectocarpooling.data.model.trip.DriverTripMatch;
import com.example.proyectocarpooling.data.model.trip.ReservationResponse;
import com.example.proyectocarpooling.data.model.trip.RouteData;
import com.example.proyectocarpooling.data.model.trip.TripResponse;
import com.example.proyectocarpooling.data.remote.trip.TripsRemoteDataSource;
import com.example.proyectocarpooling.domain.repository.trip.TripRepository;
import com.mapbox.geojson.Point;

import java.io.IOException;
import java.util.List;

public class TripRepositoryImpl implements TripRepository {

    private final TripsRemoteDataSource remoteDataSource;

    public TripRepositoryImpl(TripsRemoteDataSource remoteDataSource) {
        this.remoteDataSource = remoteDataSource;
    }

    @Override public TripResponse createTrip(Point origin, Point destination, String driverNameOrNull, String driverUserIdOrNull) throws IOException {
        return remoteDataSource.createTrip(origin, destination, driverNameOrNull, driverUserIdOrNull);
    }
    @Override public TripResponse createTrip(Point origin, Point destination, String driverNameOrNull, String driverUserIdOrNull, String vehicleIdOrNull) throws IOException {
        return remoteDataSource.createTrip(origin, destination, driverNameOrNull, driverUserIdOrNull, vehicleIdOrNull);
    }
    @Override public TripResponse createTrip(Point origin, Point destination, String driverNameOrNull, String driverUserIdOrNull, String vehicleIdOrNull, double fareAmount) throws IOException {
        return remoteDataSource.createTrip(origin, destination, driverNameOrNull, driverUserIdOrNull, vehicleIdOrNull, fareAmount);
    }
    @Override public List<DriverTripMatch> searchTripMatchCandidates(double a, double b) throws IOException {
        return remoteDataSource.searchTripMatchCandidates(a, b);
    }
    @Override public TripResponse getTripByIdIfPresent(String id) throws IOException {
        return remoteDataSource.getTripByIdIfPresent(id);
    }
    @Override public TripResponse findActiveTripForDriver(String uid, String name) throws IOException {
        return remoteDataSource.findActiveTripForDriver(uid, name);
    }
    @Override public TripResponse cancelTrip(String id) throws IOException { return remoteDataSource.cancelTrip(id); }
    @Override public RouteData fetchRoute(Point o, Point d) throws IOException { return remoteDataSource.fetchRoute(o, d); }
    @Override public RouteData fetchRouteWithWaypoint(Point o, Point w, Point d) throws IOException {
        return remoteDataSource.fetchRouteWithWaypoint(o, w, d);
    }
    @Override public TripResponse startTrip(String id, Point p) throws IOException { return remoteDataSource.startTrip(id, p); }
    @Override public TripResponse finishTrip(String id) throws IOException { return remoteDataSource.finishTrip(id); }
    @Override public TripResponse updateTripLocation(String id, double lat, double lng) throws IOException {
        return remoteDataSource.updateTripLocation(id, lat, lng);
    }
    @Override public void createReservation(String tripId, String passengerUserId, int seats) throws IOException {
        remoteDataSource.createReservation(tripId, passengerUserId, seats);
    }
    @Override public void acceptReservation(String tripId, String reservationId) throws IOException {
        remoteDataSource.acceptReservation(tripId, reservationId);
    }
    @Override public void rejectReservation(String tripId, String reservationId) throws IOException {
        remoteDataSource.rejectReservation(tripId, reservationId);
    }
    @Override public void boardPassenger(String tripId, String reservationId) throws IOException {
        remoteDataSource.boardPassenger(tripId, reservationId);
    }
    @Override public boolean verifyBoardingCode(String tripId, String reservationId, String code) throws IOException {
        return remoteDataSource.verifyBoardingCode(tripId, reservationId, code);
    }
    @Override public List<ReservationResponse> getReservations(String tripId) throws IOException {
        return remoteDataSource.getReservations(tripId);
    }
    @Override public List<ReservationResponse> getConfirmedReservations(String tripId) throws IOException {
        return remoteDataSource.getConfirmedReservations(tripId);
    }
    @Override public List<ReservationResponse> getBoardedPassengers(String tripId) throws IOException {
        return remoteDataSource.getBoardedPassengers(tripId);
    }
    @Override public void updateReservationStatus(String tripId, String resid, String status) throws IOException {
        remoteDataSource.updateReservationStatus(tripId, resid, status);
    }
    @Override public void cancelReservation(String reservationId) throws IOException {
        remoteDataSource.cancelReservation(reservationId);
    }
}
