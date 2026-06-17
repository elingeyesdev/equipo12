package com.example.proyectocarpooling.domain.model.trip;

import com.example.proyectocarpooling.data.model.trip.RouteData;
import com.example.proyectocarpooling.data.model.trip.TripResponse;

public class CreateTripResult {

    public final TripResponse trip;
    public final RouteData route;

    public CreateTripResult(TripResponse trip, RouteData route) {
        this.trip = trip;
        this.route = route;
    }
}
