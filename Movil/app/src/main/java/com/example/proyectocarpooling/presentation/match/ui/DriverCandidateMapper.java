package com.example.proyectocarpooling.presentation.match.ui;

import com.example.proyectocarpooling.data.model.trip.DriverTripMatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class DriverCandidateMapper {

    private DriverCandidateMapper() {}

    public static List<DriverMatchViewModel.DriverCandidate> mapList(List<DriverTripMatch> remoteList) {
        List<DriverMatchViewModel.DriverCandidate> list = new ArrayList<>();
        for (DriverTripMatch m : remoteList) {
            list.add(mapSingle(m));
        }
        return list;
    }

    public static DriverMatchViewModel.DriverCandidate mapSingle(DriverTripMatch m) {
        String route = String.format(Locale.US, "%.4f,%.4f -> %.4f,%.4f",
                m.originLatitude, m.originLongitude,
                m.destinationLatitude, m.destinationLongitude);
        return new DriverMatchViewModel.DriverCandidate(
                m.tripId, m.driverName, route, m.availableSeats, m.distanceKm,
                m.etaMinutes, m.statusLabel,
                m.originLatitude, m.originLongitude,
                m.destinationLatitude, m.destinationLongitude,
                buildVehicleInfo(m.vehicleBrand, m.vehicleColor, m.vehiclePlate, m.fareAmount),
                m.driverProfilePicture);
    }

    private static String buildVehicleInfo(String brand, String color, String plate, double fareAmount) {
        StringBuilder sb = new StringBuilder();
        if (brand != null && !brand.isEmpty()) sb.append(brand);
        if (color != null && !color.isEmpty()) {
            if (sb.length() > 0) sb.append(" - ");
            sb.append(color);
        }
        if (plate != null && !plate.isEmpty()) {
            if (sb.length() > 0) sb.append(" - ");
            sb.append(plate);
        }
        if (sb.length() > 0) sb.append(" - ");
        sb.append(String.format(Locale.US, "%.2f Bs", fareAmount));
        return sb.toString();
    }
}
