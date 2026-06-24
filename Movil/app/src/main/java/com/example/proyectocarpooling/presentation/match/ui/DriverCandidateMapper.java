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
        String cleanOrigin = cleanAddress(m.originAddress);
        String cleanDest = cleanAddress(m.destinationAddress);

        String originName = !cleanOrigin.isEmpty() ? cleanOrigin : String.format(Locale.US, "%.4f,%.4f", m.originLatitude, m.originLongitude);
        String destinationName = !cleanDest.isEmpty() ? cleanDest : String.format(Locale.US, "%.4f,%.4f", m.destinationLatitude, m.destinationLongitude);
        String route = originName + " -> " + destinationName;

        return new DriverMatchViewModel.DriverCandidate(
                m.tripId, m.driverName, route, m.availableSeats, m.distanceKm,
                m.etaMinutes, m.statusLabel,
                m.originLatitude, m.originLongitude,
                m.destinationLatitude, m.destinationLongitude,
                buildVehicleInfo(m.vehicleBrand, m.vehicleColor, m.vehiclePlate),
                m.driverProfilePicture,
                m.fareAmount,
                m.driverRating);
    }

    private static String cleanAddress(String address) {
        if (address == null) return "";
        String trimmed = address.trim();
        if (trimmed.isEmpty()) return "";
        int commaIndex = trimmed.indexOf(',');
        if (commaIndex != -1) {
            return trimmed.substring(0, commaIndex).trim();
        }
        return trimmed;
    }

    private static String buildVehicleInfo(String brand, String color, String plate) {
        StringBuilder sb = new StringBuilder();
        if (brand != null && !brand.isEmpty()) sb.append(brand);
        if (color != null && !color.isEmpty()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(color);
        }
        if (plate != null && !plate.isEmpty()) {
            if (sb.length() > 0) sb.append("\n");
            sb.append("Placa ").append(plate.toUpperCase(Locale.US));
        }
        return sb.toString();
    }
}
