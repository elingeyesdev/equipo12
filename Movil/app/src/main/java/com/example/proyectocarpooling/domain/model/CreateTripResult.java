package com.example.proyectocarpooling.domain.model;

import com.example.proyectocarpooling.data.model.RouteData;
import com.example.proyectocarpooling.data.model.TripResponse;

public class CreateTripResult {

    public final TripResponse trip;
    public final RouteData route;

    public CreateTripResult(TripResponse trip, RouteData route) {
        this.trip = trip;
        this.route = route;
    }
}
