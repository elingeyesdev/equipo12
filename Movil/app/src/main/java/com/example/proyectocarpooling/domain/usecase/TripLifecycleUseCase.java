package com.example.proyectocarpooling.domain.usecase;

import com.example.proyectocarpooling.data.model.TripResponse;
import com.example.proyectocarpooling.domain.repository.TripRepository;
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

    public TripResponse startTrip(String tripId, Point driverPosition) throws IOException {
        return repository.startTrip(tripId, driverPosition);
    }

    public TripResponse finishTrip(String tripId) throws IOException {
        return repository.finishTrip(tripId);
    }
}
