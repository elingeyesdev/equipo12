package com.example.proyectocarpooling.domain.repository.trip;

import com.example.proyectocarpooling.data.model.trip.RecurringReservation;
import com.example.proyectocarpooling.data.model.trip.TripSchedule;

import java.io.IOException;
import java.util.List;

public interface TripScheduleRepository {
    TripSchedule createSchedule(String driverUserId, double originLatitude, double originLongitude, String originAddress,
                               double destinationLatitude, double destinationLongitude, String destinationAddress,
                               String departureTime, String daysOfWeek, String startDate, String endDate,
                               String vehicleId, int offeredSeats, double fareAmount) throws IOException;
    List<TripSchedule> getDriverSchedules(String driverUserId) throws IOException;
    List<TripSchedule> getActiveSchedules() throws IOException;
    boolean toggleSchedule(String id, boolean active) throws IOException;
    boolean deleteSchedule(String id) throws IOException;
    RecurringReservation subscribe(String tripScheduleId, String passengerUserId, int seatsReserved) throws IOException;
    List<RecurringReservation> getPassengerSubscriptions(String passengerUserId) throws IOException;
    boolean cancelSubscription(String id) throws IOException;
}