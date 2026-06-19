package com.example.proyectocarpooling.domain.usecase.trip;

import com.example.proyectocarpooling.data.model.trip.TripResponse;
import com.example.proyectocarpooling.domain.repository.trip.TripRepository;
import com.mapbox.geojson.Point;

import java.io.IOException;

public class TripLifecycleUseCase {

    private final TripRepository repository;

    public TripLifecycleUseCase(TripRepository repository) {
        this.repository = repository;
    }

    public TripResponse cancelTrip(String tripId) throws IOException {
        return repository.cancelTrip(tripId);
    }

    public TripResponse startTrip(String tripId, Point driverPosition, double fareAmount) throws IOException {
        return repository.startTrip(tripId, driverPosition, fareAmount);
    }

    public TripResponse finishTrip(String tripId) throws IOException {
        return repository.finishTrip(tripId);
    }

    public TripResponse updateTripLocation(String tripId, double latitude, double longitude) throws IOException {
        return repository.updateTripLocation(tripId, latitude, longitude);
    }
}
