package com.example.proyectocarpooling.data.model;

import com.mapbox.geojson.Point;

import java.util.List;

public class RouteData {

    public final List<Point> points;
    public final double distanceMeters;

    public RouteData(List<Point> points, double distanceMeters) {
        this.points = points;
        this.distanceMeters = distanceMeters;
    }
}
