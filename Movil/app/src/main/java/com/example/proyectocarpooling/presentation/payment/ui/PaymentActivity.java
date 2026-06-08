package com.example.proyectocarpooling.presentation.payment.ui;

import com.example.proyectocarpooling.presentation.BaseActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.example.proyectocarpooling.CarPoolingApplication;
import com.example.proyectocarpooling.R;
import com.example.proyectocarpooling.data.local.SessionManager;
import com.example.proyectocarpooling.data.model.payment.PaymentItem;
import com.example.proyectocarpooling.data.model.payment.PaymentMethodItem;
import com.example.proyectocarpooling.data.model.payment.UserPaymentMethodItem;
import com.example.proyectocarpooling.data.remote.PaymentRemoteDataSource;
import com.example.proyectocarpooling.data.model.TripResponse;
import com.example.proyectocarpooling.presentation.auth.ui.LoginActivity;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PaymentActivity extends BaseActivity {

    public static final String EXTRA_RESERVATION_ID = "extra_payment_reservation_id";
    public static final String EXTRA_TRIP_ID = "extra_payment_trip_id";
    public static final String EXTRA_DRIVER_NAME = "extra_payment_driver_name";

    private static final String METHOD_CASH = "CASH";
    private static final String METHOD_QR = "QR_BANK";
    private static final String METHOD_CARD = "CARD_SIM";

    private SessionManager sessionManager;
    private PaymentRemoteDataSource paymentRemote;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private ProgressBar progress;
    private TextView titleText;
    private TextView subtitleText;
    private TextView routeText;
    private TextView statusText;
    private TextView receiptText;
    private TextView emptyHistoryText;
    private TextView amountText;
    private Button payButton;
    private LinearLayout checkoutContent;
    private LinearLayout historyContainer;
    private CardView cashCard;
    private CardView qrCard;
    private CardView cardSimCard;
    private TextView cashSubtitle;
    private TextView qrSubtitle;
    private TextView cardSubtitle;
    private TextView qrHolderText;
    private TextView qrValueText;
    private ImageView qrImage;

    private String reservationId;
    private String tripId;
    private String driverName;
    private final List<PaymentMethodItem> methods = new ArrayList<>();
    private final List<UserPaymentMethodItem> driverMethods = new ArrayList<>();
    private PaymentMethodItem selectedMethod;
    private UserPaymentMethodItem selectedDriverMethod;
    private double fareAmount = 10.0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        sessionManager = ((CarPoolingApplication) getApplication()).getSessionManager();
        if (!sessionManager.hasActiveSession()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        paymentRemote = ((CarPoolingApplication) getApplication()).getPaymentRemoteDataSource();

        bindViews();
        setupToolbar();
        readExtras();
        setupActions();
        loadScreen();
    }

    private void bindViews() {
        progress = findViewById(R.id.paymentProgress);
        titleText = findViewById(R.id.paymentTitleText);
        subtitleText = findViewById(R.id.paymentSubtitleText);
        routeText = findViewById(R.id.paymentRouteText);
        statusText = findViewById(R.id.paymentStatusText);
        receiptText = findViewById(R.id.paymentReceiptText);
        emptyHistoryText = findViewById(R.id.paymentEmptyHistoryText);
        amountText = findViewById(R.id.paymentAmountText);
        payButton = findViewById(R.id.paymentPayButton);
        checkoutContent = findViewById(R.id.paymentCheckoutContent);
        historyContainer = findViewById(R.id.paymentHistoryContainer);
        cashCard = findViewById(R.id.paymentCashCard);
        qrCard = findViewById(R.id.paymentQrCard);
        cardSimCard = findViewById(R.id.paymentCardSimCard);
        cashSubtitle = findViewById(R.id.paymentCashSubtitle);
        qrSubtitle = findViewById(R.id.paymentQrSubtitle);
        cardSubtitle = findViewById(R.id.paymentCardSubtitle);
        qrHolderText = findViewById(R.id.paymentQrHolderText);
        qrValueText = findViewById(R.id.paymentQrValueText);
        qrImage = findViewById(R.id.paymentQrImage);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.paymentToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void readExtras() {
        Intent intent = getIntent();
        reservationId = emptyToNull(intent.getStringExtra(EXTRA_RESERVATION_ID));
        tripId = emptyToNull(intent.getStringExtra(EXTRA_TRIP_ID));
        driverName = emptyToNull(intent.getStringExtra(EXTRA_DRIVER_NAME));

        if (reservationId == null) {
            reservationId = emptyToNull(sessionManager.getPassengerBookedReservationId());
        }
        if (tripId == null) {
            tripId = emptyToNull(sessionManager.getPassengerBookedTripId());
        }
        if (driverName == null) {
            driverName = emptyToNull(sessionManager.getPassengerDriverName());
        }
    }

    private void setupActions() {
        cashCard.setOnClickListener(v -> selectMethod(METHOD_CASH));
        qrCard.setOnClickListener(v -> selectMethod(METHOD_QR));
        cardSimCard.setOnClickListener(v -> selectMethod(METHOD_CARD));
        payButton.setOnClickListener(v -> submitPayment());
    }

    private void loadScreen() {
        setLoading(true);
        executor.execute(() -> {
            try {
                if (reservationId == null) {
                    JSONObject active = ((CarPoolingApplication) getApplication())
                            .getUserRepository()
                            .getActiveReservation(sessionManager.getUserId());
                    if (active != null) {
                        reservationId = emptyToNull(active.optString("reservationId", ""));
                        tripId = emptyToNull(active.optString("tripId", ""));
                        driverName = emptyToNull(active.optString("driverName", ""));
                        sessionManager.savePassengerBookedTrip(
                                tripId,
                                reservationId,
                                active.optString("boardingCode", sessionManager.getPassengerBoardingCode()),
                                driverName
                        );
                    }
                }

                if (tripId != null) {
                    TripResponse trip = ((CarPoolingApplication) getApplication()).getTripRepository().getTripByIdIfPresent(tripId);
                    if (trip != null) {
                        fareAmount = trip.fareAmount;
                        if (driverName == null || driverName.isEmpty()) {
                            driverName = trip.driverName;
                        }
                    }
                }

                List<PaymentMethodItem> loadedMethods = paymentRemote.getPaymentMethods();
                List<UserPaymentMethodItem> loadedDriverMethods = reservationId != null
                        ? paymentRemote.getDriverPaymentMethods(sessionManager.getUserId(), reservationId)
                        : new ArrayList<>();
                List<PaymentItem> payments = reservationId != null
                        ? paymentRemote.getReservationPayments(sessionManager.getUserId(), reservationId)
                        : new ArrayList<>();

                runOnUiThread(() -> {
                    setLoading(false);
                    methods.clear();
                    methods.addAll(loadedMethods);
                    driverMethods.clear();
                    driverMethods.addAll(loadedDriverMethods);
                    renderHeader();
                    renderMethods();
                    renderReservationPaymentStatus(payments);
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    showError(e.getMessage());
                    renderHeader();
                });
            }
        });
    }

    private void renderHeader() {
        boolean hasReservation = reservationId != null && !reservationId.isEmpty();
        checkoutContent.setVisibility(hasReservation ? View.VISIBLE : View.GONE);
        titleText.setText(hasReservation ? "Pagar tu reserva" : "Mis pagos");
        subtitleText.setText(hasReservation
                ? "Elige como quieres cerrar el pago con tu conductor."
                : "Cuando tengas una reserva activa, podras pagarla desde aqui.");
        routeText.setText(hasReservation
                ? "Conductor: " + (driverName != null ? driverName : "Asignado")
                : "Sin reserva activa");
        statusText.setText(hasReservation ? "Reserva " + shortId(reservationId) : "Historial de pagos");
        amountText.setText(String.format(Locale.US, "Total definido por el conductor: %.2f BOB", fareAmount));
        receiptText.setText("");
    }

    private void renderMethods() {
        cashSubtitle.setText("Pagas al subir y el conductor confirma.");
        cardSubtitle.setText("Ambiente de prueba para el proyecto.");
        selectedDriverMethod = findDriverQrMethod();
        if (selectedDriverMethod != null) {
            qrSubtitle.setText(selectedDriverMethod.bankName.isEmpty()
                    ? "QR registrado por el conductor."
                    : selectedDriverMethod.bankName + " · " + selectedDriverMethod.alias);
            qrHolderText.setText(selectedDriverMethod.accountHolderName.isEmpty()
                    ? "Titular no especificado"
                    : selectedDriverMethod.accountHolderName);
            qrValueText.setText(selectedDriverMethod.qrImageUrl.isEmpty()
                    ? "QR disponible al confirmar con el conductor"
                    : selectedDriverMethod.qrImageUrl);
            if (selectedDriverMethod.qrImageUrl.startsWith("data:image")) {
                loadBase64Image(selectedDriverMethod.qrImageUrl, qrImage, null);
            } else {
                qrImage.setVisibility(View.GONE);
            }
        } else {
            qrSubtitle.setText("El conductor aun no registro un QR.");
            qrHolderText.setText("No disponible");
            qrValueText.setText("Puedes usar efectivo o tarjeta simulada.");
            qrImage.setVisibility(View.GONE);
        }

        selectMethod(selectedDriverMethod != null ? METHOD_QR : METHOD_CASH);
    }

    private void renderReservationPaymentStatus(List<PaymentItem> payments) {
        if (payments == null || payments.isEmpty()) {
            receiptText.setText("Pago pendiente de realizar");
            return;
        }
        for (PaymentItem payment : payments) {
            if (reservationId != null && reservationId.equalsIgnoreCase(payment.reservationId)
                    && payment.status != PaymentItem.STATUS_CANCELLED
                    && payment.status != PaymentItem.STATUS_REJECTED) {
                if (payment.status == PaymentItem.STATUS_APPROVED) {
                    receiptText.setText("Viaje pagado · " + (payment.receiptNumber.isEmpty() ? payment.externalReference : payment.receiptNumber));
                } else {
                    receiptText.setText("Pago en revision · espera confirmacion del conductor");
                }
                break;
            }
        }
    }

    private View createPaymentRow(PaymentItem payment, boolean compact) {
        TextView row = new TextView(this);
        row.setText(String.format(Locale.US, "%s · %.2f %s\n%s · %s",
                payment.paymentMethodName,
                payment.amount,
                payment.currency,
                payment.statusLabel(),
                payment.driverName.isEmpty() ? "Viaje compartido" : payment.driverName));
        row.setTextColor(ContextCompat.getColor(this, R.color.carpool_text_primary));
        row.setTextSize(compact ? 13 : 14);
        row.setLineSpacing(4f, 1f);
        row.setBackgroundResource(R.drawable.bg_info_field);
        int pad = dp(14);
        row.setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(10));
        row.setLayoutParams(lp);
        return row;
    }

    private void selectMethod(String code) {
        selectedMethod = null;
        for (PaymentMethodItem method : methods) {
            if (code.equalsIgnoreCase(method.code)) {
                selectedMethod = method;
                break;
            }
        }
        applyCardState(cashCard, METHOD_CASH.equals(code));
        applyCardState(qrCard, METHOD_QR.equals(code));
        applyCardState(cardSimCard, METHOD_CARD.equals(code));
        payButton.setEnabled(selectedMethod != null && reservationId != null
                && (!METHOD_QR.equals(code) || selectedDriverMethod != null));
    }

    private void submitPayment() {
        if (selectedMethod == null || reservationId == null) {
            Toast.makeText(this, "Selecciona un metodo de pago.", Toast.LENGTH_SHORT).show();
            return;
        }
        String selectedUserMethodId = selectedMethod.isQr() && selectedDriverMethod != null
                ? selectedDriverMethod.id
                : null;

        setLoading(true);
        payButton.setEnabled(false);
        executor.execute(() -> {
            try {
                PaymentItem payment = paymentRemote.createPayment(
                        sessionManager.getUserId(),
                        reservationId,
                        selectedMethod.id,
                        selectedUserMethodId,
                        "Pago de reserva Univalle Ride"
                );
                if (selectedMethod.isSimulatedCard()) {
                    payment = paymentRemote.simulatePayment(sessionManager.getUserId(), payment.id, true);
                }
                PaymentItem result = payment;
                runOnUiThread(() -> {
                    setLoading(false);
                    payButton.setEnabled(true);
                    showPaymentResult(result);
                    loadScreen();
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    payButton.setEnabled(true);
                    showError(e.getMessage());
                });
            }
        });
    }

    private void showPaymentResult(PaymentItem payment) {
        String message;
        if (payment.status == PaymentItem.STATUS_APPROVED) {
            message = "Pago aprobado.\n\nComprobante: " +
                    (payment.receiptNumber.isEmpty() ? payment.externalReference : payment.receiptNumber);
        } else {
            message = "Pago pendiente de confirmacion.\n\nEl conductor debe presionar Recibi el pago.";
        }
        new AlertDialog.Builder(this)
                .setTitle(payment.status == PaymentItem.STATUS_APPROVED ? "Pago listo" : "Pago pendiente")
                .setMessage(message)
                .setPositiveButton("Entendido", null)
                .show();
    }

    private UserPaymentMethodItem findDriverQrMethod() {
        for (UserPaymentMethodItem method : driverMethods) {
            if (METHOD_QR.equalsIgnoreCase(method.paymentMethodCode)) {
                return method;
            }
        }
        return null;
    }

    private void applyCardState(CardView card, boolean selected) {
        card.setCardBackgroundColor(ContextCompat.getColor(this, selected ? R.color.uber_chip_bg : R.color.white));
        card.setCardElevation(selected ? dp(3) : dp(1));
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void showError(String raw) {
        new AlertDialog.Builder(this)
                .setTitle("Pagos")
                .setMessage(sanitizeError(raw))
                .setPositiveButton("Aceptar", null)
                .show();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private static String shortId(String value) {
        if (value == null || value.length() <= 8) return value == null ? "" : value;
        return value.substring(0, 8);
    }

    @Nullable
    private static String emptyToNull(@Nullable String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
