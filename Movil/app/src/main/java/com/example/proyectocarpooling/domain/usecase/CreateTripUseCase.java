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

    public CreateTripResult execute(Point origin, Point destination, String driverNameOrNull, String driverUserIdOrNull) throws IOException {
        return execute(origin, destination, driverNameOrNull, driverUserIdOrNull, null);
    }

    public CreateTripResult execute(Point origin, Point destination, String driverNameOrNull, String driverUserIdOrNull, String vehicleIdOrNull) throws IOException {
        return execute(origin, destination, driverNameOrNull, driverUserIdOrNull, vehicleIdOrNull, 10.0);
    }

    public CreateTripResult execute(Point origin, Point destination, String driverNameOrNull, String driverUserIdOrNull, String vehicleIdOrNull, double fareAmount) throws IOException {
        TripResponse trip = repository.createTrip(origin, destination, driverNameOrNull, driverUserIdOrNull, vehicleIdOrNull, fareAmount);
        RouteData route = repository.fetchRoute(origin, destination);
        return new CreateTripResult(trip, route);
    }
}
