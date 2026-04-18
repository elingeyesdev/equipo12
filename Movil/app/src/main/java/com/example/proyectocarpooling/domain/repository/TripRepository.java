package com.example.proyectocarpooling.domain.repository;

import com.example.proyectocarpooling.data.model.DriverTripMatch;
import com.example.proyectocarpooling.data.model.ReservationResponse;
import com.example.proyectocarpooling.data.model.RouteData;
import com.example.proyectocarpooling.data.model.TripResponse;
import com.mapbox.geojson.Point;

import java.io.IOException;
import java.util.List;

public interface TripRepository {

    TripResponse createTrip(Point origin, Point destination, String driverNameOrNull, String driverUserIdOrNull) throws IOException;

    List<DriverTripMatch> searchTripMatchCandidates(double referenceLatitude, double referenceLongitude) throws IOException;

    TripResponse getTripByIdIfPresent(String tripId) throws IOException;

    TripResponse findActiveTripForDriver(String driverUserId, String driverDisplayNameForFallback) throws IOException;

    TripResponse cancelTrip(String tripId) throws IOException;

    RouteData fetchRoute(Point origin, Point destination) throws IOException;

    TripResponse startTrip(String tripId, Point driverPosition) throws IOException;

    TripResponse finishTrip(String tripId) throws IOException;

    void createReservation(String tripId, String passengerName) throws IOException;

    List<ReservationResponse> getReservations(String tripId) throws IOException;

    List<ReservationResponse> getBoardedPassengers(String tripId) throws IOException;

    void markBoardedByName(String tripId, String passengerName) throws IOException;

    void updateReservationStatus(String tripId, String reservationId, String targetStatus) throws IOException;

    void cancelReservation(String reservationId) throws IOException;
}
