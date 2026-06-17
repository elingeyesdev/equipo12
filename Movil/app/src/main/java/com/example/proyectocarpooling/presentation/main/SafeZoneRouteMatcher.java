package com.example.proyectocarpooling.presentation.main;

import com.example.proyectocarpooling.data.model.safezones.SafeZoneItem;
import com.mapbox.geojson.Point;

import java.util.List;

public final class SafeZoneRouteMatcher {

    public static final double NEAR_ROUTE_THRESHOLD_METERS = 500.0;

    private SafeZoneRouteMatcher() {
    }

    public static class Suggestion {
        public final SafeZoneItem zone;
        public final double distanceMeters;

        public Suggestion(SafeZoneItem zone, double distanceMeters) {
            this.zone = zone;
            this.distanceMeters = distanceMeters;
        }

    }

    public static Suggestion findBestSuggestion(List<SafeZoneItem> zones, List<Point> routePoints) {
        if (zones == null || zones.isEmpty() || routePoints == null || routePoints.size() < 2) {
            return null;
        }

        Suggestion best = null;
        for (SafeZoneItem zone : zones) {
            if (zone.latitude == 0d && zone.longitude == 0d) {
                continue;
            }

            double distanceMeters = minDistanceToRouteMeters(zone.latitude, zone.longitude, routePoints);
            if (distanceMeters > NEAR_ROUTE_THRESHOLD_METERS) {
                continue;
            }

            if (best == null || distanceMeters < best.distanceMeters) {
                best = new Suggestion(zone, distanceMeters);
            }
        }
        return best;
    }

    static double minDistanceToRouteMeters(double zoneLat, double zoneLng, List<Point> routePoints) {
        double min = Double.MAX_VALUE;
        for (int i = 0; i < routePoints.size() - 1; i++) {
            Point start = routePoints.get(i);
            Point end = routePoints.get(i + 1);
            for (int step = 0; step <= 4; step++) {
                double t = step / 4.0;
                double sampleLat = start.latitude() + t * (end.latitude() - start.latitude());
                double sampleLng = start.longitude() + t * (end.longitude() - start.longitude());
                min = Math.min(min, distanceMeters(zoneLat, zoneLng, sampleLat, sampleLng));
            }
        }
        return min;
    }

    static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        return distanceKm(lat1, lon1, lat2, lon2) * 1000.0;
    }

    private static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        final double earthRadius = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }
}
