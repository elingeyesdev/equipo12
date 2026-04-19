package com.example.proyectocarpooling.data.remote.user;

import com.example.proyectocarpooling.data.model.user.UserFavoriteItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
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

public class FavoritesRemoteDataSource {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");
    private static final String TAG = "FavoritesApi";

    private final OkHttpClient httpClient;
    private final String apiBaseUrl;

    public FavoritesRemoteDataSource(String apiBaseUrl) {
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
        return String.format(Locale.US, "%s/api/users/%s/trip-bookmarks", apiBaseUrl, userId.trim());
    }

    public List<UserFavoriteItem> listFavorites(String userId) throws IOException, JSONException {
        Request request = new Request.Builder()
                .url(basePath(userId))
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
            JSONArray arr = new JSONArray(response.body().string());
            List<UserFavoriteItem> list = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                list.add(UserFavoriteItem.fromJson(arr.getJSONObject(i)));
            }
            return list;
        }
    }

    public void createFavorite(
            String userId,
            String kind,
            String title,
            double originLat,
            double originLng,
            Double destLat,
            Double destLng
    ) throws IOException {
        JSONObject body = new JSONObject();
        try {
            body.put("kind", kind);
            body.put("title", title);
            body.put("originLatitude", originLat);
            body.put("originLongitude", originLng);
            if (destLat != null && destLng != null) {
                body.put("destinationLatitude", destLat);
                body.put("destinationLongitude", destLng);
            }
        } catch (JSONException e) {
            throw new IOException(e);
        }

        Request request = new Request.Builder()
                .url(basePath(userId))
                .post(RequestBody.create(body.toString(), JSON_MEDIA_TYPE))
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String err = response.body() != null ? response.body().string() : "";
                throw new IOException("HTTP " + response.code() + " " + err);
            }
        }
    }

    public void deleteFavorite(String userId, String favoriteId) throws IOException {
        String url = basePath(userId) + "/" + favoriteId.trim();
        Request request = new Request.Builder()
                .url(url)
                .delete()
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String err = response.body() != null ? response.body().string() : "";
                throw new IOException("HTTP " + response.code() + " " + err);
            }
        }
    }

    public void recordUse(String userId, String favoriteId) throws IOException {
        String url = basePath(userId) + "/" + favoriteId.trim() + "/use";
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
}
