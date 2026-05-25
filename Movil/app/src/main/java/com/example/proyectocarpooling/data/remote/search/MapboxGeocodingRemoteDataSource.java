package com.example.proyectocarpooling.data.remote.search;

import com.mapbox.geojson.Point;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MapboxGeocodingRemoteDataSource {

    public static final class SearchSuggestion {
        public final String label;
        public final double latitude;
        public final double longitude;

        public SearchSuggestion(String label, double latitude, double longitude) {
            this.label = label;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public Point toPoint() {
            return Point.fromLngLat(longitude, latitude);
        }
    }

    private final OkHttpClient client;
    private final String accessToken;

    public MapboxGeocodingRemoteDataSource(String accessToken) {
        this.accessToken = accessToken;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build();
    }

    public List<SearchSuggestion> search(String query, int limit) throws IOException, JSONException {
        String trimmed = query == null ? "" : query.trim();
        if (trimmed.isEmpty()) {
            return new ArrayList<>();
        }

        String encoded = URLEncoder.encode(trimmed, StandardCharsets.UTF_8);
        String url = String.format(Locale.US,
                "https://api.mapbox.com/geocoding/v5/mapbox.places/%s.json?autocomplete=true&limit=%d&language=es&types=address,place,poi,locality&access_token=%s",
                encoded,
                Math.max(1, limit),
                accessToken);

        Request request = new Request.Builder().url(url).get().build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String body = response.body() != null ? response.body().string() : "";
                throw new IOException("HTTP " + response.code() + " " + body);
            }
            if (response.body() == null) {
                throw new IOException("Respuesta vacia");
            }

            JSONObject root = new JSONObject(response.body().string());
            JSONArray features = root.optJSONArray("features");
            List<SearchSuggestion> suggestions = new ArrayList<>();
            if (features == null) {
                return suggestions;
            }

            for (int i = 0; i < features.length(); i++) {
                JSONObject feature = features.optJSONObject(i);
                if (feature == null) {
                    continue;
                }

                JSONArray center = feature.optJSONArray("center");
                if (center == null || center.length() < 2) {
                    continue;
                }

                String label = feature.optString("place_name", feature.optString("text", trimmed));
                double longitude = center.optDouble(0, 0.0);
                double latitude = center.optDouble(1, 0.0);
                suggestions.add(new SearchSuggestion(label, latitude, longitude));
            }

            return suggestions;
        }
    }

    public SearchSuggestion geocodeSingle(String query) throws IOException, JSONException {
        List<SearchSuggestion> suggestions = search(query, 1);
        return suggestions.isEmpty() ? null : suggestions.get(0);
    }
}