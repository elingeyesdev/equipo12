package com.example.proyectocarpooling.data.remote.trip;

import com.example.proyectocarpooling.data.model.trip.RecurringReservation;
import com.example.proyectocarpooling.data.model.trip.TripSchedule;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

public class TripScheduleRemoteDataSource {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");
    private static final String TAG = "TripScheduleApi";

    private final OkHttpClient httpClient;
    private final String apiBaseUrl;

    public TripScheduleRemoteDataSource(String apiBaseUrl) {
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

    public TripSchedule createSchedule(String driverUserId, double originLatitude, double originLongitude, String originAddress,
                                       double destinationLatitude, double destinationLongitude, String destinationAddress,
                                       String departureTime, String daysOfWeek, String startDate, String endDate,
                                       String vehicleId, int offeredSeats, double fareAmount) throws IOException {
        try {
            JSONObject obj = new JSONObject();
            obj.put("driverUserId", driverUserId);
            obj.put("originLatitude", originLatitude);
            obj.put("originLongitude", originLongitude);
            obj.put("originAddress", originAddress != null ? originAddress : "");
            obj.put("destinationLatitude", destinationLatitude);
            obj.put("destinationLongitude", destinationLongitude);
            obj.put("destinationAddress", destinationAddress != null ? destinationAddress : "");
            obj.put("departureTime", departureTime);
            obj.put("daysOfWeek", daysOfWeek);
            obj.put("startDate", startDate);
            if (endDate != null) obj.put("endDate", endDate);
            if (vehicleId != null) obj.put("vehicleId", vehicleId);
            obj.put("offeredSeats", offeredSeats);
            obj.put("fareAmount", fareAmount);

            RequestBody body = RequestBody.create(obj.toString(), JSON_MEDIA_TYPE);
            Request request = new Request.Builder()
                    .url(apiBaseUrl + "/api/TripSchedule")
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    throw new IOException("Unexpected response: " + response.code() + " " + errorBody);
                }
                if (response.body() == null) throw new IOException("Server returned empty body");
                return TripSchedule.fromJson(new JSONObject(response.body().string()));
            }
        } catch (JSONException e) {
            throw new IOException("Error building JSON request", e);
        }
    }

    public List<TripSchedule> getDriverSchedules(String driverUserId) throws IOException {
        Request request = new Request.Builder()
                .url(apiBaseUrl + "/api/TripSchedule/driver/" + driverUserId)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                throw new IOException("Unexpected response: " + response.code() + " " + errorBody);
            }
            if (response.body() == null) throw new IOException("Server returned empty body");
            JSONArray array = new JSONArray(response.body().string());
            List<TripSchedule> list = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                list.add(TripSchedule.fromJson(array.getJSONObject(i)));
            }
            return list;
        } catch (JSONException e) {
            throw new IOException("Invalid JSON response", e);
        }
    }

    public List<TripSchedule> getActiveSchedules() throws IOException {
        Request request = new Request.Builder()
                .url(apiBaseUrl + "/api/TripSchedule")
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                throw new IOException("Unexpected response: " + response.code() + " " + errorBody);
            }
            if (response.body() == null) throw new IOException("Server returned empty body");
            JSONArray array = new JSONArray(response.body().string());
            List<TripSchedule> list = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                list.add(TripSchedule.fromJson(array.getJSONObject(i)));
            }
            return list;
        } catch (JSONException e) {
            throw new IOException("Invalid JSON response", e);
        }
    }

    public boolean toggleSchedule(String id, boolean active) throws IOException {
        RequestBody body = RequestBody.create("", null);
        Request request = new Request.Builder()
                .url(apiBaseUrl + "/api/TripSchedule/" + id + "/toggle?active=" + active)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            return response.isSuccessful();
        }
    }

    public boolean deleteSchedule(String id) throws IOException {
        Request request = new Request.Builder()
                .url(apiBaseUrl + "/api/TripSchedule/" + id)
                .delete()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            return response.isSuccessful();
        }
    }

    public RecurringReservation subscribe(String tripScheduleId, String passengerUserId, int seatsReserved) throws IOException {
        try {
            JSONObject obj = new JSONObject();
            obj.put("tripScheduleId", tripScheduleId);
            obj.put("passengerUserId", passengerUserId);
            obj.put("seatsReserved", seatsReserved);

            RequestBody body = RequestBody.create(obj.toString(), JSON_MEDIA_TYPE);
            Request request = new Request.Builder()
                    .url(apiBaseUrl + "/api/RecurringReservation")
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    throw new IOException("Unexpected response: " + response.code() + " " + errorBody);
                }
                if (response.body() == null) throw new IOException("Server returned empty body");
                return RecurringReservation.fromJson(new JSONObject(response.body().string()));
            }
        } catch (JSONException e) {
            throw new IOException("Error building JSON request", e);
        }
    }

    public List<RecurringReservation> getPassengerSubscriptions(String passengerUserId) throws IOException {
        Request request = new Request.Builder()
                .url(apiBaseUrl + "/api/RecurringReservation/passenger/" + passengerUserId)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                throw new IOException("Unexpected response: " + response.code() + " " + errorBody);
            }
            if (response.body() == null) throw new IOException("Server returned empty body");
            JSONArray array = new JSONArray(response.body().string());
            List<RecurringReservation> list = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                list.add(RecurringReservation.fromJson(array.getJSONObject(i)));
            }
            return list;
        } catch (JSONException e) {
            throw new IOException("Invalid JSON response", e);
        }
    }

    public boolean cancelSubscription(String id) throws IOException {
        RequestBody body = RequestBody.create("", null);
        Request request = new Request.Builder()
                .url(apiBaseUrl + "/api/RecurringReservation/" + id + "/cancel")
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            return response.isSuccessful();
        }
    }
}