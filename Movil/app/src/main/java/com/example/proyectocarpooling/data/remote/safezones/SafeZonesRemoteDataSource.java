package com.example.proyectocarpooling.data.remote.safezones;

import com.example.proyectocarpooling.data.model.safezones.SafeZoneItem;

import org.json.JSONException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

public class SafeZonesRemoteDataSource {

    private static final String TAG = "SafeZonesApi";

    private final OkHttpClient httpClient;
    private final String apiBaseUrl;

    public SafeZonesRemoteDataSource(String apiBaseUrl) {
        String sanitizedBaseUrl = apiBaseUrl;
        if (sanitizedBaseUrl.endsWith("/")) {
            sanitizedBaseUrl = sanitizedBaseUrl.substring(0, sanitizedBaseUrl.length() - 1);
        }

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(message -> android.util.Log.d(TAG, message));
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);

        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .build();
        this.apiBaseUrl = sanitizedBaseUrl;
    }

    public List<SafeZoneItem> fetchActiveSafeZones() throws IOException {
        Request request = new Request.Builder()
                .url(apiBaseUrl + "/api/safe-zones")
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("No se pudieron cargar las zonas seguras: HTTP " + response.code());
            }

            String body = response.body().string();
            try {
                JSONArray array = new JSONArray(body);
                List<SafeZoneItem> zones = new ArrayList<>(array.length());
                for (int i = 0; i < array.length(); i++) {
                    JSONObject item = array.getJSONObject(i);
                    zones.add(new SafeZoneItem(
                            item.optString("id", ""),
                            item.optString("name", "Zona segura"),
                            item.optString("description", null),
                            item.optDouble("latitude", 0d),
                            item.optDouble("longitude", 0d),
                            item.optString("addressLabel", null),
                            item.optInt("purpose", 0),
                            item.optString("purposeLabel", "")
                    ));
                }
                return zones;
            } catch (JSONException exception) {
                throw new IOException("La respuesta de zonas seguras no tiene un formato JSON válido", exception);
            }
        }
    }
}
