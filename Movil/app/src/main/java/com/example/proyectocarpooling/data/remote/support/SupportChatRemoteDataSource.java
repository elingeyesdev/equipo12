package com.example.proyectocarpooling.data.remote.support;

import com.example.proyectocarpooling.data.model.chat.ChatMessage;

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

public class SupportChatRemoteDataSource {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");
    private static final String TAG = "SupportChatApi";

    private final OkHttpClient httpClient;
    private final String apiBaseUrl;

    public SupportChatRemoteDataSource(String apiBaseUrl) {
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

    private String messagesPath(String userId, String ticketId) {
        return String.format(
                Locale.US,
                "%s/api/users/%s/support-tickets/%s/messages",
                apiBaseUrl,
                userId.trim(),
                ticketId.trim());
    }

    public List<ChatMessage> getMessages(String userId, String ticketId) throws IOException, JSONException {
        Request request = new Request.Builder()
                .url(messagesPath(userId, ticketId))
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

    public ChatMessage sendMessage(String userId, String ticketId, String messageText)
            throws IOException, JSONException {
        JSONObject body = new JSONObject();
        body.put("messageText", messageText);

        Request request = new Request.Builder()
                .url(messagesPath(userId, ticketId))
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

            return ChatMessage.fromJson(new JSONObject(response.body().string()));
        }
    }
}
