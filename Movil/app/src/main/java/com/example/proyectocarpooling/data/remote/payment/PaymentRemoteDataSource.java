package com.example.proyectocarpooling.data.remote.payment;

import com.example.proyectocarpooling.data.model.payment.PaymentItem;
import com.example.proyectocarpooling.data.model.payment.PaymentMethodItem;
import com.example.proyectocarpooling.data.model.payment.UserPaymentMethodItem;

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

public class PaymentRemoteDataSource {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient httpClient;
    private final String apiBaseUrl;

    public PaymentRemoteDataSource(String apiBaseUrl) {
        String sanitizedBaseUrl = apiBaseUrl;
        if (sanitizedBaseUrl.endsWith("/")) {
            sanitizedBaseUrl = sanitizedBaseUrl.substring(0, sanitizedBaseUrl.length() - 1);
        }
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(message -> android.util.Log.d("PaymentsApi", message));
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .build();
        this.apiBaseUrl = sanitizedBaseUrl;
    }

    public List<PaymentMethodItem> getPaymentMethods() throws IOException {
        Request request = new Request.Builder()
                .url(apiBaseUrl + "/api/payment-methods")
                .get()
                .build();
        try {
            JSONArray array = executeArray(request);
            List<PaymentMethodItem> items = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                items.add(PaymentMethodItem.fromJson(array.getJSONObject(i)));
            }
            return items;
        } catch (JSONException e) {
            throw new IOException("Respuesta invalida de metodos de pago", e);
        }
    }

    public List<UserPaymentMethodItem> getUserPaymentMethods(String userId) throws IOException {
        Request request = authedBuilder(userId)
                .url(apiBaseUrl + "/api/users/" + userId + "/payment-methods")
                .get()
                .build();
        return executeUserPaymentMethodList(request);
    }

    public UserPaymentMethodItem createQrPaymentMethod(String userId, int paymentMethodId,
                                                       String alias, String bankName,
                                                       String holderName, String qrImageUrl) throws IOException {
        JSONObject body = new JSONObject();
        try {
            body.put("paymentMethodId", paymentMethodId);
            body.put("alias", alias);
            body.put("bankName", bankName);
            body.put("accountHolderName", holderName);
            body.put("qrImageUrl", qrImageUrl);
            body.put("isDefault", true);
        } catch (JSONException e) {
            throw new IOException("No se pudo construir el QR", e);
        }
        Request request = authedBuilder(userId)
                .url(apiBaseUrl + "/api/users/" + userId + "/payment-methods")
                .post(RequestBody.create(body.toString(), JSON_MEDIA_TYPE))
                .build();
        try {
            return UserPaymentMethodItem.fromJson(executeObject(request));
        } catch (JSONException e) {
            throw new IOException("Respuesta invalida de QR", e);
        }
    }

    public List<UserPaymentMethodItem> getDriverPaymentMethods(String userId, String reservationId) throws IOException {
        Request request = authedBuilder(userId)
                .url(apiBaseUrl + "/api/reservations/" + reservationId + "/driver-payment-methods")
                .get()
                .build();
        return executeUserPaymentMethodList(request);
    }

    public PaymentItem createPayment(String userId, String reservationId, int paymentMethodId,
                                     String userPaymentMethodId, String description) throws IOException {
        JSONObject body = new JSONObject();
        try {
            body.put("reservationId", reservationId);
            body.put("paymentMethodId", paymentMethodId);
            if (userPaymentMethodId != null && !userPaymentMethodId.trim().isEmpty()) {
                body.put("userPaymentMethodId", userPaymentMethodId.trim());
            }
            body.put("currency", "BOB");
            body.put("description", description);
        } catch (JSONException e) {
            throw new IOException("No se pudo construir el pago", e);
        }
        Request request = authedBuilder(userId)
                .url(apiBaseUrl + "/api/users/" + userId + "/payments")
                .post(RequestBody.create(body.toString(), JSON_MEDIA_TYPE))
                .build();
        try {
            return PaymentItem.fromJson(executeObject(request));
        } catch (JSONException e) {
            throw new IOException("Respuesta invalida de pago", e);
        }
    }

    public PaymentItem simulatePayment(String userId, String paymentId, boolean approve) throws IOException {
        JSONObject body = new JSONObject();
        try {
            body.put("approve", approve);
            body.put("responseCode", approve ? "APPROVED" : "REJECTED");
            body.put("responseMessage", approve ? "Pago aprobado en simulacion" : "Pago rechazado en simulacion");
        } catch (JSONException e) {
            throw new IOException("No se pudo construir simulacion", e);
        }
        Request request = authedBuilder(userId)
                .url(apiBaseUrl + "/api/payments/" + paymentId + "/simulate")
                .post(RequestBody.create(body.toString(), JSON_MEDIA_TYPE))
                .build();
        try {
            return PaymentItem.fromJson(executeObject(request));
        } catch (JSONException e) {
            throw new IOException("Respuesta invalida de simulacion", e);
        }
    }

    public PaymentItem confirmPayment(String userId, String paymentId, String notes) throws IOException {
        JSONObject body = new JSONObject();
        try {
            body.put("notes", notes);
        } catch (JSONException e) {
            throw new IOException("No se pudo construir confirmacion", e);
        }
        Request request = authedBuilder(userId)
                .url(apiBaseUrl + "/api/payments/" + paymentId + "/confirm")
                .post(RequestBody.create(body.toString(), JSON_MEDIA_TYPE))
                .build();
        try {
            return PaymentItem.fromJson(executeObject(request));
        } catch (JSONException e) {
            throw new IOException("Respuesta invalida de confirmacion", e);
        }
    }

    public List<PaymentItem> getUserPayments(String userId) throws IOException {
        Request request = authedBuilder(userId)
                .url(apiBaseUrl + "/api/users/" + userId + "/payments")
                .get()
                .build();
        try {
            return PaymentItem.listFromJson(executeArray(request));
        } catch (Exception e) {
            throw new IOException("Respuesta invalida de pagos", e);
        }
    }

    public List<PaymentItem> getReservationPayments(String userId, String reservationId) throws IOException {
        Request request = authedBuilder(userId)
                .url(apiBaseUrl + "/api/reservations/" + reservationId + "/payments")
                .get()
                .build();
        try {
            return PaymentItem.listFromJson(executeArray(request));
        } catch (Exception e) {
            throw new IOException("Respuesta invalida de pagos de reserva", e);
        }
    }

    public PaymentItem cancelPayment(String userId, String paymentId) throws IOException {
        Request request = authedBuilder(userId)
                .url(apiBaseUrl + "/api/payments/" + paymentId + "/cancel")
                .post(RequestBody.create("", JSON_MEDIA_TYPE))
                .build();
        try {
            return PaymentItem.fromJson(executeObject(request));
        } catch (JSONException e) {
            throw new IOException("Respuesta invalida de cancelacion de pago", e);
        }
    }

    private List<UserPaymentMethodItem> executeUserPaymentMethodList(Request request) throws IOException {
        try {
            JSONArray array = executeArray(request);
            List<UserPaymentMethodItem> items = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                items.add(UserPaymentMethodItem.fromJson(array.getJSONObject(i)));
            }
            return items;
        } catch (JSONException e) {
            throw new IOException("Respuesta invalida de metodos guardados", e);
        }
    }

    private Request.Builder authedBuilder(String userId) {
        return new Request.Builder().addHeader("X-User-Id", userId);
    }

    private JSONObject executeObject(Request request) throws IOException, JSONException {
        String body = executeString(request);
        return new JSONObject(body);
    }

    private JSONArray executeArray(Request request) throws IOException, JSONException {
        String body = executeString(request);
        return new JSONArray(body);
    }

    private String executeString(Request request) throws IOException {
        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new IOException("Error de pagos: " + response.code() + " " + responseBody);
            }
            if (responseBody.isEmpty()) {
                throw new IOException("Respuesta vacia del servidor");
            }
            return responseBody;
        }
    }
}
