package com.example.proyectocarpooling.data.remote.user;

import com.example.proyectocarpooling.data.model.user.LoginUserRequest;
import com.example.proyectocarpooling.data.model.user.RegisterUserRequest;
import com.example.proyectocarpooling.data.model.user.UpdateUserRequest;
import com.example.proyectocarpooling.data.model.user.UserResponse;
import com.example.proyectocarpooling.data.model.user.VehicleResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
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
            body.put("role", request.role);
            body.put("profilePicture", request.profilePicture);
            if (request.phoneNumber != null && !request.phoneNumber.trim().isEmpty()) {
                body.put("phoneNumber", request.phoneNumber);
            }
            if (request.driverProfile != null) {
                JSONObject driverProfile = new JSONObject();
                driverProfile.put("availableSeats", request.driverProfile.availableSeats);
                driverProfile.put("licensePlate", request.driverProfile.licensePlate);
                driverProfile.put("vehicleBrand", request.driverProfile.vehicleBrand);
                driverProfile.put("vehicleColor", request.driverProfile.vehicleColor);
                body.put("driverProfile", driverProfile);
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
        String encodedEmail = URLEncoder.encode(email, "UTF-8");
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
            body.put("role", request.role);
            body.put("roleChangeRequested", request.roleChangeRequested);
            body.put("profilePicture", request.profilePicture);
            if (request.phoneNumber != null && !request.phoneNumber.trim().isEmpty()) {
                body.put("phoneNumber", request.phoneNumber);
            }
            if (request.newPassword != null && !request.newPassword.trim().isEmpty()) {
                body.put("newPassword", request.newPassword);
            }
            if (request.driverProfile != null) {
                JSONObject driverProfile = new JSONObject();
                driverProfile.put("availableSeats", request.driverProfile.availableSeats);
                driverProfile.put("licensePlate", request.driverProfile.licensePlate);
                driverProfile.put("vehicleBrand", request.driverProfile.vehicleBrand);
                driverProfile.put("vehicleColor", request.driverProfile.vehicleColor);
                body.put("driverProfile", driverProfile);
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

    public List<VehicleResponse> getVehicles(String userId) throws IOException {
        Request request = new Request.Builder()
                .url(apiBaseUrl + "/api/Users/" + userId + "/vehicles")
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                throw new IOException("Error consultando vehículos: " + response.code() + " " + errorBody);
            }
            if (response.body() == null) throw new IOException("Respuesta vacía");
            return VehicleResponse.fromJsonArray(response.body().string());
        } catch (JSONException e) {
            throw new IOException("Respuesta inválida de vehículos", e);
        }
    }

    public VehicleResponse addVehicle(String userId, String licensePlate, String brand, String model,
                                       String color, int vehicleYear, int totalSeats) throws IOException {
        JSONObject body = new JSONObject();
        try {
            body.put("licensePlate", licensePlate);
            body.put("brand", brand);
            body.put("model", model);
            body.put("color", color);
            body.put("vehicleYear", vehicleYear);
            body.put("totalSeats", totalSeats);
        } catch (JSONException e) {
            throw new IOException("No se pudo construir vehículo", e);
        }

        Request request = new Request.Builder()
                .url(apiBaseUrl + "/api/Users/" + userId + "/vehicles")
                .post(RequestBody.create(body.toString(), JSON_MEDIA_TYPE))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.body() == null) throw new IOException("Respuesta vacía");
            String responseBody = response.body().string();
            if (!response.isSuccessful()) {
                throw new IOException("Error agregando vehículo: " + response.code() + " " + responseBody);
            }
            return VehicleResponse.fromJson(new JSONObject(responseBody));
        } catch (JSONException e) {
            throw new IOException("Respuesta inválida", e);
        }
    }

    public VehicleResponse updateVehicle(String userId, String vehicleId, String licensePlate, String brand,
                                          String model, String color, int vehicleYear, int totalSeats) throws IOException {
        JSONObject body = new JSONObject();
        try {
            body.put("licensePlate", licensePlate);
            body.put("brand", brand);
            body.put("model", model);
            body.put("color", color);
            body.put("vehicleYear", vehicleYear);
            body.put("totalSeats", totalSeats);
        } catch (JSONException e) {
            throw new IOException("No se pudo construir vehículo", e);
        }

        Request request = new Request.Builder()
                .url(apiBaseUrl + "/api/Users/" + userId + "/vehicles/" + vehicleId)
                .put(RequestBody.create(body.toString(), JSON_MEDIA_TYPE))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.body() == null) throw new IOException("Respuesta vacía");
            String responseBody = response.body().string();
            if (!response.isSuccessful()) {
                throw new IOException("Error actualizando vehículo: " + response.code() + " " + responseBody);
            }
            return VehicleResponse.fromJson(new JSONObject(responseBody));
        } catch (JSONException e) {
            throw new IOException("Respuesta inválida", e);
        }
    }

    public void deleteVehicle(String userId, String vehicleId) throws IOException {
        Request request = new Request.Builder()
                .url(apiBaseUrl + "/api/Users/" + userId + "/vehicles/" + vehicleId)
                .delete()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                throw new IOException("Error eliminando vehículo: " + response.code() + " " + errorBody);
            }
        }
    }

    public List<VehicleResponse> getVehiclesForUser(String userId) throws IOException {
        Request request = new Request.Builder()
                .url(apiBaseUrl + "/api/Users/" + userId + "/vehicles")
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                throw new IOException("Error consultando vehiculos: " + response.code() + " " + errorBody);
            }
            if (response.body() == null) {
                throw new IOException("Respuesta vacia del servidor");
            }
            JSONArray array = new JSONArray(response.body().string());
            List<VehicleResponse> vehicles = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                vehicles.add(VehicleResponse.fromJson(array.getJSONObject(i)));
            }
            return vehicles;
        } catch (JSONException e) {
            throw new IOException("Respuesta invalida de vehiculos", e);
        }
    }

    public JSONObject getActiveReservation(String userId) throws IOException {
        Request request = new Request.Builder()
                .url(apiBaseUrl + "/api/Users/" + userId + "/active-reservation")
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.code() == 404) {
                return null;
            }
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                throw new IOException("Error consultando reserva activa: " + response.code() + " " + errorBody);
            }
            if (response.body() == null) {
                throw new IOException("Respuesta vacia del servidor");
            }
            return new JSONObject(response.body().string());
        } catch (JSONException e) {
            throw new IOException("Respuesta invalida de reserva activa", e);
        }
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

    public void registerFcmToken(String userId, String token) throws IOException {
        JSONObject body = new JSONObject();
        try {
            body.put("fcmToken", token);
            body.put("deviceName", android.os.Build.MODEL);
        } catch (JSONException e) {
            throw new IOException("No se pudo construir cuerpo del token", e);
        }

        Request request = new Request.Builder()
                .url(apiBaseUrl + "/api/Users/" + userId + "/fcm-token")
                .post(RequestBody.create(body.toString(), JSON_MEDIA_TYPE))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                throw new IOException("Error registrando FCM token: " + response.code() + " " + errorBody);
            }
        }
    }

    public void logoutDevice(String token) throws IOException {
        JSONObject body = new JSONObject();
        try {
            body.put("fcmToken", token);
        } catch (JSONException e) {
            throw new IOException("No se pudo construir cuerpo del token para cerrar sesión", e);
        }

        Request request = new Request.Builder()
                .url(apiBaseUrl + "/api/Users/logout-device")
                .post(RequestBody.create(body.toString(), JSON_MEDIA_TYPE))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                throw new IOException("Error desvinculando dispositivo: " + response.code() + " " + errorBody);
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
