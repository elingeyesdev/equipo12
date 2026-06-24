package com.example.proyectocarpooling.domain.usecase.trip;

import com.example.proyectocarpooling.data.model.trip.RecurringReservation;
import com.example.proyectocarpooling.data.model.trip.TripSchedule;
import com.example.proyectocarpooling.domain.repository.trip.TripScheduleRepository;

import java.io.IOException;
import java.util.List;

public class ManageScheduleUseCase {

    private final TripScheduleRepository repository;

    public ManageScheduleUseCase(TripScheduleRepository repository) {
        this.repository = repository;
    }

    public TripSchedule createSchedule(String driverUserId, double originLatitude, double originLongitude, String originAddress,
                                       double destinationLatitude, double destinationLongitude, String destinationAddress,
                                       String departureTime, String daysOfWeek, String startDate, String endDate,
                                       String vehicleId, int offeredSeats, double fareAmount) throws IOException {
        return repository.createSchedule(driverUserId, originLatitude, originLongitude, originAddress,
                destinationLatitude, destinationLongitude, destinationAddress,
                departureTime, daysOfWeek, startDate, endDate, vehicleId, offeredSeats, fareAmount);
    }

    public List<TripSchedule> getDriverSchedules(String driverUserId) throws IOException {
        return repository.getDriverSchedules(driverUserId);
    }

    public List<TripSchedule> getActiveSchedules() throws IOException {
        return repository.getActiveSchedules();
    }

    public boolean toggleSchedule(String id, boolean active) throws IOException {
        return repository.toggleSchedule(id, active);
    }

    public boolean deleteSchedule(String id) throws IOException {
        return repository.deleteSchedule(id);
    }

    public RecurringReservation subscribe(String tripScheduleId, String passengerUserId, int seatsReserved) throws IOException {
        return repository.subscribe(tripScheduleId, passengerUserId, seatsReserved);
    }

    public List<RecurringReservation> getPassengerSubscriptions(String passengerUserId) throws IOException {
        return repository.getPassengerSubscriptions(passengerUserId);
    }

    public boolean cancelSubscription(String id) throws IOException {
        return repository.cancelSubscription(id);
    }

    public List<RecurringReservation> getScheduleSubscriptions(String scheduleId) throws IOException {
        return repository.getScheduleSubscriptions(scheduleId);
    }

    public boolean approveSubscription(String id) throws IOException {
        return repository.approveSubscription(id);
    }

    public boolean rejectSubscription(String id) throws IOException {
        return repository.rejectSubscription(id);
    }

    public TripSchedule getScheduleById(String id) throws IOException {
        return repository.getScheduleById(id);
    }
}