package com.example.proyectocarpooling.data.remote;

import com.example.proyectocarpooling.data.model.DriverTripMatch;
import com.example.proyectocarpooling.data.model.ReservationResponse;
import com.example.proyectocarpooling.data.model.RouteData;
import com.example.proyectocarpooling.data.model.TripResponse;
import com.mapbox.geojson.Point;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

public class TripsRemoteDataSource {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");
    private static final String TAG = "TripsApi";

    private final OkHttpClient httpClient;
    private final String apiBaseUrl;
    private final String mapboxAccessToken;

    public TripsRemoteDataSource(String apiBaseUrl, String mapboxAccessToken) {
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
        this.mapboxAccessToken = mapboxAccessToken;
    }

    public TripResponse createTrip(Point origin, Point destination, String driverNameOrNull, String driverUserIdOrNull) throws IOException {
        return createTrip(origin, destination, driverNameOrNull, driverUserIdOrNull, null);
    }

    public TripResponse createTrip(Point origin, Point destination, String driverNameOrNull, String driverUserIdOrNull, String vehicleIdOrNull) throws IOException {
        return createTrip(origin, destination, driverNameOrNull, driverUserIdOrNull, vehicleIdOrNull, 10.0);
    }

    public TripResponse createTrip(Point origin, Point destination, String driverNameOrNull, String driverUserIdOrNull, String vehicleIdOrNull, double fareAmount) throws IOException {
        TripResponse createdTrip = sendOriginRequest(apiBaseUrl + "/api/Trips/origin", origin, driverNameOrNull, driverUserIdOrNull, vehicleIdOrNull, fareAmount);
        if (destination != null) {
            createdTrip = sendCoordinateRequest(apiBaseUrl + "/api/Trips/" + createdTrip.id + "/destination", destination);
        }
        return createdTrip;
    }

    public List<DriverTripMatch> searchTripMatchCandidates(double referenceLatitude, double referenceLongitude) throws IOException {
        String url;
        if (Double.isNaN(referenceLatitude) || Double.isNaN(referenceLongitude)) {
            url = apiBaseUrl + "/api/Trips/match-candidates";
        } else {
            url = String.format(Locale.US, "%s/api/Trips/match-candidates?referenceLatitude=%.7f&referenceLongitude=%.7f",
                    apiBaseUrl, referenceLatitude, referenceLongitude);
        }
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                throw new IOException("Unexpected response: " + response.code() + " " + errorBody);
            }
            if (response.body() == null) {
                throw new IOException("Respuesta vacia del servidor");
            }
            JSONArray array = new JSONArray(response.body().string());
            List<DriverTripMatch> list = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                list.add(DriverTripMatch.fromJson(array.getJSONObject(i)));
            }
            return list;
        } catch (JSONException e) {
            throw new IOException("Respuesta invalida del servidor", e);
        }
    }

    public TripResponse getTripByIdIfPresent(String tripId) throws IOException {
        Request request = new Request.Builder()
                .url(apiBaseUrl + "/api/Trips/" + tripId)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.code() == 404) {
                return null;
            }
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                throw new IOException("Unexpected response: " + response.code() + " " + errorBody);
            }
            if (response.body() == null) {
                throw new IOException("Respuesta vacia del servidor");
            }
            return TripResponse.fromJson(response.body().string());
        } catch (JSONException e) {
            throw new IOException("Respuesta invalida del servidor", e);
        }
    }

    public TripResponse findActiveTripForDriver(String driverUserId, String driverDisplayNameForFallback) throws IOException {
        HttpUrl parsed = HttpUrl.parse(apiBaseUrl + "/api/Trips/for-driver/" + driverUserId.trim());
        if (parsed == null) {
            throw new IOException("URL invalida para buscar viaje del conductor");
        }
        HttpUrl.Builder urlBuilder = parsed.newBuilder();
        if (driverDisplayNameForFallback != null && !driverDisplayNameForFallback.trim().isEmpty()) {
            urlBuilder.addQueryParameter("displayName", driverDisplayNameForFallback.trim());
        }

        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.code() == 404) {
                return null;
            }
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                throw new IOException("Unexpected response: " + response.code() + " " + errorBody);
            }
            if (response.body() == null) {
                throw new IOException("Respuesta vacia del servidor");
            }
            return TripResponse.fromJson(response.body().string());
        } catch (JSONException e) {
            throw new IOException("Respuesta invalida del servidor", e);
        }
    }

    public TripResponse cancelTrip(String tripId) throws IOException {
        Request request = new Request.Builder()
                .url(apiBaseUrl + "/api/Trips/" + tripId + "/cancel")
                .post(RequestBody.create("{}", JSON_MEDIA_TYPE))
                .build();
        return executeTripRequest(request);
    }

    public RouteData fetchRoute(Point origin, Point destination) throws IOException {
        String routeUrl = String.format(Locale.US,
                "https://api.mapbox.com/directions/v5/mapbox/driving/%f,%f;%f,%f?geometries=geojson&overview=full&access_token=%s",
                origin.longitude(),
                origin.latitude(),
                destination.longitude(),
                destination.latitude(),
                mapboxAccessToken);

        Request request = new Request.Builder()
                .url(routeUrl)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected route response: " + response);
            }
            if (response.body() == null) {
                throw new IOException("Respuesta vacia de la ruta");
            }

            JSONObject json = new JSONObject(response.body().string());
            JSONArray routes = json.getJSONArray("routes");
            if (routes.length() == 0) {
                throw new IOException("No se encontro ruta");
            }

            JSONObject route = routes.getJSONObject(0);
            JSONObject geometry = route.getJSONObject("geometry");
            double distanceMeters = route.optDouble("distance", 0.0);
            JSONArray coordinates = geometry.getJSONArray("coordinates");

            List<Point> points = new ArrayList<>();
            for (int i = 0; i < coordinates.length(); i++) {
                JSONArray coordinate = coordinates.getJSONArray(i);
                double longitude = coordinate.getDouble(0);
                double latitude = coordinate.getDouble(1);
                points.add(Point.fromLngLat(longitude, latitude));
            }

            return new RouteData(points, distanceMeters);
        } catch (JSONException e) {
            throw new IOException("Respuesta invalida de rutas", e);
        }
    }

    public TripResponse startTrip(String tripId, Point driverPosition) throws IOException {
        JSONObject body = new JSONObject();
        try {
            if (driverPosition != null) {
                body.put("latitude", driverPosition.latitude());
                body.put("longitude", driverPosition.longitude());
            }
        } catch (JSONException e) {
            throw new IOException("No se pudo construir request de inicio", e);
        }

        Request request = new Request.Builder()
                .url(apiBaseUrl + "/api/Trips/" + tripId + "/start")
                .post(RequestBody.create(body.toString(), JSON_MEDIA_TYPE))
                .build();

        return executeTripRequest(request);
    }

    public TripResponse finishTrip(String tripId) throws IOException {
        Request request = new Request.Builder()
                .url(apiBaseUrl + "/api/Trips/" + tripId + "/finish")
                .post(RequestBody.create("{}", JSON_MEDIA_TYPE))
                .build();

        return executeTripRequest(request);
    }

    public void createReservation(String tripId, String passengerUserId, int seatsReserved) throws IOException {
        JSONObject body = new JSONObject();
        try {
            body.put("passengerUserId", passengerUserId);
            body.put("seatsReserved", seatsReserved);
        } catch (JSONException e) {
            throw new IOException("No se pudo construir reserva", e);
        }

        Request request = new Request.Builder()
                .url(apiBaseUrl + "/api/Trips/" + tripId + "/Reservations")
                .post(RequestBody.create(body.toString(), JSON_MEDIA_TYPE))
                .build();

        executeWithoutBody(request, "Error creando reserva");
    }

    public void acceptReservation(String tripId, String reservationId) throws IOException {
        Request request = new Request.Builder()
                .url(apiBaseUrl + "/api/Trips/" + tripId + "/Reservations/" + reservationId + "/accept")
                .post(RequestBody.create("{}", JSON_MEDIA_TYPE))
                .build();
        executeWithoutBody(request, "Error aceptando reserva");
    }

    public void rejectReservation(String tripId, String reservationId) throws IOException {
        Request request = new Request.Builder()
                .url(apiBaseUrl + "/api/Trips/" + tripId + "/Reservations/" + reservationId + "/reject")
                .post(RequestBody.create("{}", JSON_MEDIA_TYPE))
                .build();
        executeWithoutBody(request, "Error rechazando reserva");
    }

    public void boardPassenger(String tripId, String reservationId) throws IOException {
        Request request = new Request.Builder()
                .url(apiBaseUrl + "/api/Trips/" + tripId + "/Reservations/" + reservationId + "/board")
                .post(RequestBody.create("{}", JSON_MEDIA_TYPE))
                .build();
        executeWithoutBody(request, "Error marcando abordaje");
    }

    public boolean verifyBoardingCode(String tripId, String reservationId, String code) throws IOException {
        JSONObject body = new JSONObject();
        try {
            body.put("code", code);
        } catch (JSONException e) {
            throw new IOException("No se pudo construir verificacion de codigo", e);
        }
        Request request = new Request.Builder()
                .url(apiBaseUrl + "/api/Trips/" + tripId + "/Reservations/" + reservationId + "/verify-code")
                .post(RequestBody.create(body.toString(), JSON_MEDIA_TYPE))
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            return response.isSuccessful();
        }
    }

    public List<ReservationResponse> getReservations(String tripId) throws IOException {
        Request request = new Request.Builder()
                .url(apiBaseUrl + "/api/Trips/" + tripId + "/Reservations/pending")
                .get()
                .build();
        return executeReservationsRequest(request, "Error obteniendo reservas");
    }

    public List<ReservationResponse> getConfirmedReservations(String tripId) throws IOException {
        Request request = new Request.Builder()
                .url(apiBaseUrl + "/api/Trips/" + tripId + "/Reservations/confirmed")
                .get()
                .build();
        return executeReservationsRequest(request, "Error obteniendo confirmadas");
    }

    public List<ReservationResponse> getBoardedPassengers(String tripId) throws IOException {
        Request request = new Request.Builder()
                .url(apiBaseUrl + "/api/Trips/" + tripId + "/Reservations/boarded")
                .get()
                .build();

        return executeReservationsRequest(request, "Error obteniendo pasajeros abordados");
    }

    public void cancelReservation(String reservationId) throws IOException {
        Request request = new Request.Builder()
                .url(apiBaseUrl + "/api/Reservations/" + reservationId)
                .delete()
                .build();

        executeWithoutBody(request, "Error cancelando reserva");
    }

    public void updateReservationStatus(String tripId, String reservationId, String targetStatus) throws IOException {
        JSONObject body = new JSONObject();
        try {
            body.put("status", targetStatus);
        } catch (JSONException e) {
            throw new IOException("Error construyendo estado de reserva", e);
        }

        Request request = new Request.Builder()
                .url(apiBaseUrl + "/api/Trips/" + tripId + "/Reservations/" + reservationId + "/status")
                .put(RequestBody.create(body.toString(), JSON_MEDIA_TYPE))
                .build();

        executeWithoutBody(request, "Error actualizando estado de reserva");
    }

    private TripResponse sendOriginRequest(String url, Point point, String driverNameOrNull, String driverUserIdOrNull) throws IOException {
        return sendOriginRequest(url, point, driverNameOrNull, driverUserIdOrNull, null, 10.0);
    }

    private TripResponse sendOriginRequest(String url, Point point, String driverNameOrNull, String driverUserIdOrNull, String vehicleIdOrNull, double fareAmount) throws IOException {
        JSONObject body = new JSONObject();
        try {
            body.put("latitude", point.latitude());
            body.put("longitude", point.longitude());
            body.put("fareAmount", fareAmount);
            if (driverNameOrNull != null && !driverNameOrNull.trim().isEmpty()) {
                body.put("driverName", driverNameOrNull.trim());
            }
            if (driverUserIdOrNull != null && !driverUserIdOrNull.trim().isEmpty()) {
                body.put("driverUserId", driverUserIdOrNull.trim());
            }
            if (vehicleIdOrNull != null && !vehicleIdOrNull.trim().isEmpty()) {
                body.put("vehicleId", vehicleIdOrNull.trim());
            }
        } catch (JSONException e) {
            throw new IOException("No se pudo construir coordenadas", e);
        }

        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(body.toString(), JSON_MEDIA_TYPE))
                .build();

        return executeTripRequest(request);
    }

    private TripResponse sendCoordinateRequest(String url, Point point) throws IOException {
        JSONObject body = new JSONObject();
        try {
            body.put("latitude", point.latitude());
            body.put("longitude", point.longitude());
        } catch (JSONException e) {
            throw new IOException("No se pudo construir coordenadas", e);
        }

        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(body.toString(), JSON_MEDIA_TYPE))
                .build();

        return executeTripRequest(request);
    }

    private TripResponse executeTripRequest(Request request) throws IOException {
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                throw new IOException("Unexpected response: " + response.code() + " " + errorBody);
            }
            if (response.body() == null) {
                throw new IOException("Respuesta vacia del servidor");
            }
            String json = response.body().string();
            return TripResponse.fromJson(json);
        } catch (JSONException e) {
            throw new IOException("Respuesta invalida del servidor", e);
        }
    }

    private List<ReservationResponse> executeReservationsRequest(Request request, String errorPrefix) throws IOException {
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                throw new IOException(errorPrefix + ": " + response.code() + " " + errorBody);
            }
            if (response.body() == null) {
                throw new IOException("Respuesta vacia del servidor");
            }

            JSONArray array = new JSONArray(response.body().string());
            List<ReservationResponse> reservations = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                reservations.add(ReservationResponse.fromJson(array.getJSONObject(i)));
            }
            return reservations;
        } catch (JSONException e) {
            throw new IOException("Respuesta invalida de reservas", e);
        }
    }

    private void executeWithoutBody(Request request, String errorPrefix) throws IOException {
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                throw new IOException(errorPrefix + ": " + response.code() + " " + errorBody);
            }
        }
    }
}
