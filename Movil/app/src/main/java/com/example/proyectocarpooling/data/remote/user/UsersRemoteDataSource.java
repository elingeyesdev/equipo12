package com.example.proyectocarpooling.data.remote.user;

import com.example.proyectocarpooling.data.model.user.LoginUserRequest;
import com.example.proyectocarpooling.data.model.user.RegisterUserRequest;
import com.example.proyectocarpooling.data.model.user.UpdateUserRequest;
import com.example.proyectocarpooling.data.model.user.UserResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

public class UsersRemoteDataSource {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");
    private static final String TAG = "UsersApi";

    private final OkHttpClient httpClient;
    private final String apiBaseUrl;

    public UsersRemoteDataSource(String apiBaseUrl) {
        String sanitizedBaseUrl = apiBaseUrl;
        if (sanitizedBaseUrl.endsWith("/")) {
            sanitizedBaseUrl = sanitizedBaseUrl.substring(0, sanitizedBaseUrl.length() - 1);
        }

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(message -> android.util.Log.d(TAG, message));
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .build();

        this.apiBaseUrl = sanitizedBaseUrl;
    }

    public UserResponse register(RegisterUserRequest request) throws IOException {
        JSONObject body = new JSONObject();
        try {
            body.put("fullName", request.fullName);
            body.put("email", request.email);
            body.put("password", request.password);
            if (request.phoneNumber != null && !request.phoneNumber.trim().isEmpty()) {
                body.put("phoneNumber", request.phoneNumber);
            }
        } catch (JSONException e) {
            throw new IOException("No se pudo construir registro de usuario", e);
        }

        Request httpRequest = new Request.Builder()
                .url(apiBaseUrl + "/api/Users/register")
                .post(RequestBody.create(body.toString(), JSON_MEDIA_TYPE))
                .build();

        return executeUserRequest(httpRequest, "Error registrando usuario");
    }

    public UserResponse login(LoginUserRequest request) throws IOException {
        JSONObject body = new JSONObject();
        try {
            body.put("email", request.email);
            body.put("password", request.password);
        } catch (JSONException e) {
            throw new IOException("No se pudo construir login", e);
        }

        Request httpRequest = new Request.Builder()
                .url(apiBaseUrl + "/api/Users/login")
                .post(RequestBody.create(body.toString(), JSON_MEDIA_TYPE))
                .build();

        return executeUserRequest(httpRequest, "Error iniciando sesión");
    }

    public UserResponse getById(String userId) throws IOException {
        Request request = new Request.Builder()
                .url(apiBaseUrl + "/api/Users/" + userId)
                .get()
                .build();

        return executeUserRequest(request, "Error consultando usuario");
    }

    public UserResponse getByEmail(String email) throws IOException {
        String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8);
        Request request = new Request.Builder()
            .url(apiBaseUrl + "/api/Users/email/" + encodedEmail)
                .get()
                .build();

        return executeUserRequest(request, "Error consultando usuario por email");
    }

    public UserResponse update(String userId, UpdateUserRequest request) throws IOException {
        JSONObject body = new JSONObject();
        try {
            body.put("fullName", request.fullName);
            body.put("email", request.email);
            if (request.phoneNumber != null && !request.phoneNumber.trim().isEmpty()) {
                body.put("phoneNumber", request.phoneNumber);
            }
            if (request.newPassword != null && !request.newPassword.trim().isEmpty()) {
                body.put("newPassword", request.newPassword);
            }
        } catch (JSONException e) {
            throw new IOException("No se pudo construir actualización de perfil", e);
        }

        Request httpRequest = new Request.Builder()
                .url(apiBaseUrl + "/api/Users/" + userId)
                .put(RequestBody.create(body.toString(), JSON_MEDIA_TYPE))
                .build();

        return executeUserRequest(httpRequest, "Error actualizando perfil");
    }

    public void logout() throws IOException {
        Request request = new Request.Builder()
                .url(apiBaseUrl + "/api/Users/logout")
                .post(RequestBody.create("{}", JSON_MEDIA_TYPE))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                throw new IOException("Error cerrando sesión: " + response.code() + " " + errorBody);
            }
        }
    }

    private UserResponse executeUserRequest(Request request, String errorPrefix) throws IOException {
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                throw new IOException(errorPrefix + ": " + response.code() + " " + errorBody);
            }

            if (response.body() == null) {
                throw new IOException("Respuesta vacía del servidor");
            }

            return UserResponse.fromJson(response.body().string());
        } catch (JSONException e) {
            throw new IOException("Respuesta inválida de usuario", e);
        }
    }
}
