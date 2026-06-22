package com.example.proyectocarpooling.data.repository.trip;

import com.example.proyectocarpooling.data.model.trip.RecurringReservation;
import com.example.proyectocarpooling.data.model.trip.TripSchedule;
import com.example.proyectocarpooling.data.remote.trip.TripScheduleRemoteDataSource;
import com.example.proyectocarpooling.domain.repository.trip.TripScheduleRepository;

import java.io.IOException;
import java.util.List;

public class TripScheduleRepositoryImpl implements TripScheduleRepository {

    private final TripScheduleRemoteDataSource remoteDataSource;

    public TripScheduleRepositoryImpl(TripScheduleRemoteDataSource remoteDataSource) {
        this.remoteDataSource = remoteDataSource;
    }

    @Override
    public TripSchedule createSchedule(String driverUserId, double originLatitude, double originLongitude, String originAddress,
                                       double destinationLatitude, double destinationLongitude, String destinationAddress,
                                       String departureTime, String daysOfWeek, String startDate, String endDate,
                                       String vehicleId, int offeredSeats, double fareAmount) throws IOException {
        return remoteDataSource.createSchedule(driverUserId, originLatitude, originLongitude, originAddress,
                destinationLatitude, destinationLongitude, destinationAddress,
                departureTime, daysOfWeek, startDate, endDate, vehicleId, offeredSeats, fareAmount);
    }

    @Override
    public List<TripSchedule> getDriverSchedules(String driverUserId) throws IOException {
        return remoteDataSource.getDriverSchedules(driverUserId);
    }

    @Override
    public List<TripSchedule> getActiveSchedules() throws IOException {
        return remoteDataSource.getActiveSchedules();
    }

    @Override
    public boolean toggleSchedule(String id, boolean active) throws IOException {
        return remoteDataSource.toggleSchedule(id, active);
    }

    @Override
    public boolean deleteSchedule(String id) throws IOException {
        return remoteDataSource.deleteSchedule(id);
    }

    @Override
    public RecurringReservation subscribe(String tripScheduleId, String passengerUserId, int seatsReserved) throws IOException {
        return remoteDataSource.subscribe(tripScheduleId, passengerUserId, seatsReserved);
    }

    @Override
    public List<RecurringReservation> getPassengerSubscriptions(String passengerUserId) throws IOException {
        return remoteDataSource.getPassengerSubscriptions(passengerUserId);
    }

    @Override
    public boolean cancelSubscription(String id) throws IOException {
        return remoteDataSource.cancelSubscription(id);
    }

    @Override
    public List<RecurringReservation> getScheduleSubscriptions(String scheduleId) throws IOException {
        return remoteDataSource.getScheduleSubscriptions(scheduleId);
    }

    @Override
    public boolean approveSubscription(String id) throws IOException {
        return remoteDataSource.approveSubscription(id);
    }

    @Override
    public boolean rejectSubscription(String id) throws IOException {
        return remoteDataSource.rejectSubscription(id);
    }
}