package com.example.proyectocarpooling.data.remote.user;

import com.example.proyectocarpooling.data.model.history.TripHistoryDetailItem;
import com.example.proyectocarpooling.data.model.history.TripHistoryListResult;
import com.example.proyectocarpooling.data.model.history.TripHistorySummaryItem;

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

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

public class TripHistoryRemoteDataSource {
    private static final String TAG = "TripHistoryApi";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final String apiBaseUrl;

    public TripHistoryRemoteDataSource(String apiBaseUrl) {
        String sanitized = apiBaseUrl;
        if (sanitized.endsWith("/")) {
            sanitized = sanitized.substring(0, sanitized.length() - 1);
        }
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor(message -> android.util.Log.d(TAG, message));
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC);
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build();
        this.apiBaseUrl = sanitized;
    }

    private String basePath(String userId) {
        return String.format(Locale.US, "%s/api/users/%s/history", apiBaseUrl, userId.trim());
    }

    public TripHistoryListResult listHistory(String userId, String passengerName) throws IOException, JSONException {
        String url = basePath(userId) + "?passengerName=" + URLEncoder.encode(passengerName, StandardCharsets.UTF_8);
        Request request = new Request.Builder().url(url).get().build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String err = response.body() != null ? response.body().string() : "";
                throw new IOException("HTTP " + response.code() + " " + err);
            }
            if (response.body() == null) {
                throw new IOException("Respuesta vacia");
            }

            JSONObject o = new JSONObject(response.body().string());
            List<TripHistorySummaryItem> driver = parseSummaryList(o.optJSONArray("driverHistory"));
            List<TripHistorySummaryItem> student = parseSummaryList(o.optJSONArray("studentHistory"));
            return new TripHistoryListResult(driver, student);
        }
    }

    public TripHistoryDetailItem getHistoryDetail(String userId, String tripId, String passengerName) throws IOException, JSONException {
        String url = basePath(userId) + "/" + tripId.trim() + "?passengerName=" + URLEncoder.encode(passengerName, StandardCharsets.UTF_8);
        Request request = new Request.Builder().url(url).get().build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String err = response.body() != null ? response.body().string() : "";
                throw new IOException("HTTP " + response.code() + " " + err);
            }
            if (response.body() == null) {
                throw new IOException("Respuesta vacia");
            }
            return TripHistoryDetailItem.fromJson(new JSONObject(response.body().string()));
        }
    }

    public void hideHistoryTrip(String userId, String tripId) throws IOException {
        String url = basePath(userId) + "/" + tripId.trim();
        Request request = new Request.Builder().url(url).delete().build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String err = response.body() != null ? response.body().string() : "";
                throw new IOException("HTTP " + response.code() + " " + err);
            }
        }
    }

    public void restoreHistoryTrip(String userId, String tripId) throws IOException {
        String url = basePath(userId) + "/" + tripId.trim() + "/restore";
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create("{}", JSON_MEDIA_TYPE))
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String err = response.body() != null ? response.body().string() : "";
                throw new IOException("HTTP " + response.code() + " " + err);
            }
        }
    }

    private static List<TripHistorySummaryItem> parseSummaryList(JSONArray array) throws JSONException {
        List<TripHistorySummaryItem> list = new ArrayList<>();
        if (array == null) {
            return list;
        }
        for (int i = 0; i < array.length(); i++) {
            list.add(TripHistorySummaryItem.fromJson(array.getJSONObject(i)));
        }
        return list;
    }
}
