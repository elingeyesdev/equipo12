package com.example.proyectocarpooling.data.model.safezones;

public class SafeZoneItem {
    public final String id;
    public final String name;
    public final String description;
    public final double latitude;
    public final double longitude;
    public final String addressLabel;
    public final int purpose;
    public final String purposeLabel;

    public SafeZoneItem(String id, String name, String description, double latitude, double longitude,
                        String addressLabel, int purpose, String purposeLabel) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
        this.addressLabel = addressLabel;
        this.purpose = purpose;
        this.purposeLabel = purposeLabel;
    }
}
