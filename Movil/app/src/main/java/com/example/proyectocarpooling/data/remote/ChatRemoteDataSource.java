package com.example.proyectocarpooling.data.remote;

import com.example.proyectocarpooling.data.model.ChatMessage;

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

public class ChatRemoteDataSource {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");
    private static final String TAG = "ChatRemoteApi";

    private final OkHttpClient httpClient;
    private final String apiBaseUrl;

    public ChatRemoteDataSource(String apiBaseUrl) {
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

    private String chatPath(String tripId) {
        return String.format(Locale.US, "%s/api/Trips/%s/chat", apiBaseUrl, tripId.trim());
    }

    public List<ChatMessage> getMessages(String tripId, String userId) throws IOException, JSONException {
        String url = chatPath(tripId) + "/messages";
        Request request = new Request.Builder()
                .url(url)
                .header("X-User-Id", userId.trim())
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
            List<ChatMessage> list = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                list.add(ChatMessage.fromJson(arr.getJSONObject(i)));
            }
            return list;
        }
    }

    public ChatMessage sendMessage(String tripId, String userId, String messageText) throws IOException, JSONException {
        String url = chatPath(tripId) + "/messages";
        
        JSONObject body = new JSONObject();
        body.put("messageText", messageText);

        Request request = new Request.Builder()
                .url(url)
                .header("X-User-Id", userId.trim())
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

            JSONObject obj = new JSONObject(response.body().string());
            return ChatMessage.fromJson(obj);
        }
    }

    public void markAsRead(String tripId, String userId) throws IOException {
        String url = chatPath(tripId) + "/read";
        Request request = new Request.Builder()
                .url(url)
                .header("X-User-Id", userId.trim())
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
