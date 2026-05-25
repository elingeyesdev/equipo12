package com.example.proyectocarpooling.data.remote;

import com.example.proyectocarpooling.data.model.support.SupportTicketItem;

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

public class SupportRemoteDataSource {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");
    private static final String TAG = "SupportApi";

    private final OkHttpClient httpClient;
    private final String apiBaseUrl;

    public SupportRemoteDataSource(String apiBaseUrl) {
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
        return String.format(Locale.US, "%s/api/users/%s/support-tickets", apiBaseUrl, userId.trim());
    }

    public SupportTicketItem createTicket(
            String userId,
            int category,
            String subject,
            String description,
            String tripId,
            String reservationId
    ) throws IOException, JSONException {
        JSONObject body = new JSONObject();
        body.put("category", category);
        body.put("subject", subject);
        body.put("description", description);
        if (tripId != null && !tripId.isBlank()) {
            body.put("tripId", tripId.trim());
        }
        if (reservationId != null && !reservationId.isBlank()) {
            body.put("reservationId", reservationId.trim());
        }

        Request request = new Request.Builder()
                .url(basePath(userId))
                .header("X-User-Id", userId.trim())
                .post(RequestBody.create(body.toString(), JSON_MEDIA_TYPE))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException(extractError(response));
            }
            if (response.body() == null) {
                throw new IOException("Respuesta vacía");
            }
            return SupportTicketItem.fromJson(new JSONObject(response.body().string()));
        }
    }

    public List<SupportTicketItem> listTickets(String userId) throws IOException, JSONException {
        Request request = new Request.Builder()
                .url(basePath(userId))
                .header("X-User-Id", userId.trim())
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException(extractError(response));
            }
            if (response.body() == null) {
                throw new IOException("Respuesta vacía");
            }
            JSONObject root = new JSONObject(response.body().string());
            JSONArray items = root.optJSONArray("items");
            if (items == null) {
                return new ArrayList<>();
            }
            List<SupportTicketItem> list = new ArrayList<>();
            for (int i = 0; i < items.length(); i++) {
                list.add(SupportTicketItem.fromJson(items.getJSONObject(i)));
            }
            return list;
        }
    }

    public SupportTicketItem getTicket(String userId, String ticketId) throws IOException, JSONException {
        String url = basePath(userId) + "/" + ticketId.trim();
        Request request = new Request.Builder()
                .url(url)
                .header("X-User-Id", userId.trim())
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException(extractError(response));
            }
            if (response.body() == null) {
                throw new IOException("Respuesta vacía");
            }
            return SupportTicketItem.fromJson(new JSONObject(response.body().string()));
        }
    }

    private static String extractError(Response response) throws IOException {
        String body = response.body() != null ? response.body().string() : "";
        try {
            JSONObject json = new JSONObject(body);
            if (json.has("message")) {
                return json.getString("message");
            }
        } catch (JSONException ignored) {
        }
        return "HTTP " + response.code() + (body.isEmpty() ? "" : " " + body);
    }
}
