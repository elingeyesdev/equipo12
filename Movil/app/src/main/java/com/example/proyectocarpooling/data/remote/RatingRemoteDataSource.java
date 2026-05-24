package com.example.proyectocarpooling.data.remote;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

public class RatingRemoteDataSource {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");
    private static final String TAG = "RatingRemoteApi";

    private final OkHttpClient httpClient;
    private final String apiBaseUrl;

    public RatingRemoteDataSource(String apiBaseUrl) {
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

    public JSONObject createRating(String tripId, String evaluatorUserId, String evaluatedUserId, int score, String comment, String tags) throws IOException, JSONException {
        String url = String.format(Locale.US, "%s/api/Trips/%s/ratings", apiBaseUrl, tripId.trim());

        JSONObject body = new JSONObject();
        body.put("evaluatedUserId", evaluatedUserId);
        body.put("score", score);
        body.put("comment", comment == null ? "" : comment);
        body.put("tags", tags == null ? "" : tags);

        Request request = new Request.Builder()
                .url(url)
                .header("X-User-Id", evaluatorUserId.trim())
                .post(RequestBody.create(body.toString(), JSON_MEDIA_TYPE))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String err = response.body() != null ? response.body().string() : "";
                throw new IOException("HTTP " + response.code() + " " + err);
            }
            if (response.body() == null) {
                throw new IOException("Respuesta vacía");
            }
            return new JSONObject(response.body().string());
        }
    }

    public JSONObject getUserRatingSummary(String userId, String accessorUserId) throws IOException, JSONException {
        String url = String.format(Locale.US, "%s/api/users/%s/ratings/summary", apiBaseUrl, userId.trim());

        Request request = new Request.Builder()
                .url(url)
                .header("X-User-Id", accessorUserId.trim())
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String err = response.body() != null ? response.body().string() : "";
                throw new IOException("HTTP " + response.code() + " " + err);
            }
            if (response.body() == null) {
                throw new IOException("Respuesta vacía");
            }
            return new JSONObject(response.body().string());
        }
    }

    public JSONArray getUserRatings(String userId, String accessorUserId) throws IOException, JSONException {
        String url = String.format(Locale.US, "%s/api/users/%s/ratings", apiBaseUrl, userId.trim());

        Request request = new Request.Builder()
                .url(url)
                .header("X-User-Id", accessorUserId.trim())
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String err = response.body() != null ? response.body().string() : "";
                throw new IOException("HTTP " + response.code() + " " + err);
            }
            if (response.body() == null) {
                throw new IOException("Respuesta vacía");
            }
            return new JSONArray(response.body().string());
        }
    }
}
