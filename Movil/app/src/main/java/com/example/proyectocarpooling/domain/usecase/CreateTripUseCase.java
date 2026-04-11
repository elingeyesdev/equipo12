package com.example.proyectocarpooling.domain.usecase;

import com.example.proyectocarpooling.data.model.RouteData;
import com.example.proyectocarpooling.data.model.TripResponse;
import com.example.proyectocarpooling.domain.model.CreateTripResult;
import com.example.proyectocarpooling.domain.repository.TripRepository;
import com.mapbox.geojson.Point;

import java.io.IOException;

public class CreateTripUseCase {

    private final TripRepository repository;

    public CreateTripUseCase(TripRepository repository) {
        this.repository = repository;
    }

    public CreateTripResult execute(Point origin, Point destination) throws IOException {
        TripResponse trip = repository.createTrip(origin, destination);
        RouteData route = repository.fetchRoute(origin, destination);
        return new CreateTripResult(trip, route);
    }
}
