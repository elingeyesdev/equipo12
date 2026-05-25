package com.example.proyectocarpooling.data.model.user;

import org.json.JSONException;
import org.json.JSONObject;

public class UserFavoriteItem {

    public final String id;
    public final String kind;
    public final String title;
    public final double originLatitude;
    public final double originLongitude;
    public final Double destinationLatitude;
    public final Double destinationLongitude;
    public final int useCount;

    public UserFavoriteItem(
            String id, String kind, String title,
            double originLatitude, double originLongitude,
            Double destinationLatitude, Double destinationLongitude,
            int useCount) {
        this.id = id;
        this.kind = kind;
        this.title = title;
        this.originLatitude = originLatitude;
        this.originLongitude = originLongitude;
        this.destinationLatitude = destinationLatitude;
        this.destinationLongitude = destinationLongitude;
        this.useCount = useCount;
    }

    public boolean isRoute() {
        return kind != null && kind.equalsIgnoreCase("route");
    }

    public static UserFavoriteItem fromJson(JSONObject o) throws JSONException {
        String id = o.getString("id");
        String kind = o.optString("kind", "place");
        String title = o.optString("title", "");

        JSONObject origin = o.getJSONObject("origin");
        double oLat = origin.getDouble("latitude");
        double oLng = origin.getDouble("longitude");

        Double dLat = null;
        Double dLng = null;
        if (o.has("destination") && !o.isNull("destination")) {
            JSONObject dest = o.getJSONObject("destination");
            dLat = dest.getDouble("latitude");
            dLng = dest.getDouble("longitude");
        }

        int useCount = o.optInt("useCount", 0);
        return new UserFavoriteItem(id, kind, title, oLat, oLng, dLat, dLng, useCount);
    }
}
