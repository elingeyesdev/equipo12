package com.example.proyectocarpooling.presentation.payment.ui;

import com.example.proyectocarpooling.presentation.BaseActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.example.proyectocarpooling.CarPoolingApplication;
import com.example.proyectocarpooling.R;
import com.example.proyectocarpooling.data.local.SessionManager;
import com.example.proyectocarpooling.data.model.payment.PaymentItem;
import com.example.proyectocarpooling.data.model.payment.PaymentMethodItem;
import com.example.proyectocarpooling.data.model.payment.UserPaymentMethodItem;
import com.example.proyectocarpooling.data.remote.payment.PaymentRemoteDataSource;
import com.example.proyectocarpooling.presentation.auth.ui.LoginActivity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DriverPaymentsActivity extends BaseActivity {

    private SessionManager sessionManager;
    private PaymentRemoteDataSource paymentRemote;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private ProgressBar progress;
    private EditText aliasInput;
    private EditText bankInput;
    private EditText holderInput;
    private ImageView qrPreview;
    private Button pickQrButton;
    private Button saveQrButton;
    private LinearLayout currentQrContainer;
    private LinearLayout pendingContainer;
    private TextView pendingEmptyText;
    private LinearLayout refundsContainer;
    private TextView refundsEmptyText;
    private int qrPaymentMethodId;
    private String selectedQrImageBase64;

    private final ActivityResultLauncher<String> qrImagePicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    handleQrImage(uri);
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_payments);

        sessionManager = ((CarPoolingApplication) getApplication()).getSessionManager();
        if (!sessionManager.hasActiveSession()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        paymentRemote = ((CarPoolingApplication) getApplication()).getPaymentRemoteDataSource();

        bindViews();
        setupToolbar();
        pickQrButton.setOnClickListener(v -> qrImagePicker.launch("image/*"));
        saveQrButton.setOnClickListener(v -> saveQr());
        loadScreen();
    }

    private void bindViews() {
        progress = findViewById(R.id.driverPaymentsProgress);
        aliasInput = findViewById(R.id.driverPaymentAliasInput);
        bankInput = findViewById(R.id.driverPaymentBankInput);
        holderInput = findViewById(R.id.driverPaymentHolderInput);
        qrPreview = findViewById(R.id.driverPaymentQrPreview);
        pickQrButton = findViewById(R.id.driverPaymentPickQrButton);
        saveQrButton = findViewById(R.id.driverPaymentSaveQrButton);
        currentQrContainer = findViewById(R.id.driverPaymentCurrentQrContainer);
        pendingContainer = findViewById(R.id.driverPaymentPendingContainer);
        pendingEmptyText = findViewById(R.id.driverPaymentPendingEmpty);
        refundsContainer = findViewById(R.id.driverPaymentRefundsContainer);
        refundsEmptyText = findViewById(R.id.driverPaymentRefundsEmpty);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.driverPaymentsToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadScreen() {
        setLoading(true);
        executor.execute(() -> {
            try {
                List<PaymentMethodItem> methods = paymentRemote.getPaymentMethods();
                int qrId = 0;
                for (PaymentMethodItem method : methods) {
                    if ("QR_BANK".equalsIgnoreCase(method.code)) {
                        qrId = method.id;
                        break;
                    }
                }
                List<UserPaymentMethodItem> userMethods = paymentRemote.getUserPaymentMethods(sessionManager.getUserId());
                List<PaymentItem> payments = paymentRemote.getUserPayments(sessionManager.getUserId());
                int finalQrId = qrId;
                runOnUiThread(() -> {
                    qrPaymentMethodId = finalQrId;
                    setLoading(false);
                    renderSavedQr(userMethods);
                    renderPendingPayments(payments);
                    renderPendingRefunds(payments);
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    showError(e.getMessage());
                });
            }
        });
    }

    private void renderSavedQr(List<UserPaymentMethodItem> userMethods) {
        currentQrContainer.removeAllViews();
        boolean hasQr = false;
        for (UserPaymentMethodItem method : userMethods) {
            if ("QR_BANK".equalsIgnoreCase(method.paymentMethodCode)) {
                hasQr = true;
                TextView row = buildInfoRow(
                        (method.alias.isEmpty() ? "QR bancario" : method.alias) + "\n" +
                                (method.bankName.isEmpty() ? "Banco sin especificar" : method.bankName) + " · " +
                                (method.accountHolderName.isEmpty() ? "Titular sin especificar" : method.accountHolderName)
                );
                currentQrContainer.addView(row);
            }
        }
        if (!hasQr) {
            currentQrContainer.addView(buildInfoRow("Aun no tienes QR registrado. Agrega uno para que el pasajero lo vea antes de pagar."));
        }
    }

    private void renderPendingPayments(List<PaymentItem> payments) {
        pendingContainer.removeAllViews();
        int count = 0;
        for (PaymentItem payment : payments) {
            if (payment.isPendingManual()) {
                count++;
                pendingContainer.addView(buildPendingPaymentCard(payment));
            }
        }
        pendingEmptyText.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
    }

    private void renderPendingRefunds(List<PaymentItem> payments) {
        refundsContainer.removeAllViews();
        int count = 0;
        if (payments != null) {
            for (PaymentItem payment : payments) {
                if (payment.refunds != null) {
                    for (com.example.proyectocarpooling.data.model.payment.RefundItem refund : payment.refunds) {
                        if (refund.status == 1) { // Requested
                            count++;
                            refundsContainer.addView(buildPendingRefundCard(payment, refund));
                        }
                    }
                }
            }
        }
        refundsEmptyText.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
    }

    private View buildPendingRefundCard(PaymentItem payment, com.example.proyectocarpooling.data.model.payment.RefundItem refund) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_info_field);
        int pad = dp(14);
        card.setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(cardLp);

        TextView title = new TextView(this);
        title.setText(String.format(Locale.US, "Reembolso: %s · %.2f %s", payment.passengerName, refund.amount, payment.currency));
        title.setTextColor(ContextCompat.getColor(this, R.color.carpool_text_primary));
        title.setTextSize(15);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        card.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Motivo: " + refund.reason + "\nReserva " + shortId(payment.reservationId));
        subtitle.setTextColor(ContextCompat.getColor(this, R.color.carpool_text_secondary));
        subtitle.setTextSize(13);
        subtitle.setPadding(0, dp(6), 0, dp(10));
        card.addView(subtitle);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(0, dp(40), 1f);

        Button approve = new Button(this);
        approve.setText("Aprobar");
        approve.setTextColor(ContextCompat.getColor(this, R.color.button_text_color));
        approve.setTextSize(13);
        approve.setTypeface(approve.getTypeface(), android.graphics.Typeface.BOLD);
        approve.setBackgroundResource(R.drawable.bg_primary_action);
        approve.setOnClickListener(v -> processRefund(refund.id, true));
        buttons.addView(approve, btnLp);

        View spacer = new View(this);
        LinearLayout.LayoutParams spacerLp = new LinearLayout.LayoutParams(dp(10), dp(40));
        buttons.addView(spacer, spacerLp);

        Button reject = new Button(this);
        reject.setText("Rechazar");
        reject.setTextColor(ContextCompat.getColor(this, R.color.carpool_text_primary));
        reject.setTextSize(13);
        reject.setTypeface(reject.getTypeface(), android.graphics.Typeface.BOLD);
        reject.setBackgroundResource(R.drawable.bg_info_field);
        reject.setOnClickListener(v -> processRefund(refund.id, false));
        buttons.addView(reject, btnLp);

        card.addView(buttons);
        return card;
    }

    private void processRefund(String refundId, boolean approve) {
        if (approve) {
            new AlertDialog.Builder(this)
                    .setTitle("Aprobar reembolso")
                    .setMessage("¿Estás seguro de que deseas aprobar esta solicitud de reembolso?")
                    .setPositiveButton("Sí, aprobar", (d, w) -> {
                        setLoading(true);
                        executor.execute(() -> {
                            try {
                                paymentRemote.approveRefund(sessionManager.getUserId(), refundId);
                                runOnUiThread(() -> {
                                    setLoading(false);
                                    Toast.makeText(this, "Reembolso aprobado.", Toast.LENGTH_SHORT).show();
                                    loadScreen();
                                });
                            } catch (IOException e) {
                                runOnUiThread(() -> {
                                    setLoading(false);
                                    showError(e.getMessage());
                                });
                            }
                        });
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        } else {
            final EditText input = new EditText(this);
            input.setHint("Ej. El viaje sí se realizó correctamente.");
            int pad = dp(20);
            LinearLayout container = new LinearLayout(this);
            container.setOrientation(LinearLayout.VERTICAL);
            container.setPadding(pad, pad, pad, pad);
            container.addView(input);

            new AlertDialog.Builder(this)
                    .setTitle("Rechazar reembolso")
                    .setMessage("Ingresa el motivo del rechazo:")
                    .setView(container)
                    .setPositiveButton("Rechazar", (d, w) -> {
                        String notes = input.getText().toString().trim();
                        if (notes.isEmpty()) {
                            Toast.makeText(this, "Debes ingresar un motivo.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        setLoading(true);
                        executor.execute(() -> {
                            try {
                                paymentRemote.rejectRefund(sessionManager.getUserId(), refundId, notes);
                                runOnUiThread(() -> {
                                    setLoading(false);
                                    Toast.makeText(this, "Reembolso rechazado.", Toast.LENGTH_SHORT).show();
                                    loadScreen();
                                });
                            } catch (IOException e) {
                                runOnUiThread(() -> {
                                    setLoading(false);
                                    showError(e.getMessage());
                                });
                            }
                        });
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        }
    }

    private View buildPendingPaymentCard(PaymentItem payment) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_info_field);
        int pad = dp(14);
        card.setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(cardLp);

        TextView title = new TextView(this);
        title.setText(String.format(Locale.US, "%s · %.2f %s", payment.passengerName, payment.amount, payment.currency));
        title.setTextColor(ContextCompat.getColor(this, R.color.carpool_text_primary));
        title.setTextSize(15);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        card.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText(payment.paymentMethodName + "\nReserva " + shortId(payment.reservationId));
        subtitle.setTextColor(ContextCompat.getColor(this, R.color.carpool_text_secondary));
        subtitle.setTextSize(13);
        subtitle.setPadding(0, dp(6), 0, dp(10));
        card.addView(subtitle);

        Button confirm = new Button(this);
        confirm.setText("Recibi el pago");
        confirm.setTextColor(ContextCompat.getColor(this, R.color.button_text_color));
        confirm.setTextSize(14);
        confirm.setTypeface(confirm.getTypeface(), android.graphics.Typeface.BOLD);
        confirm.setBackgroundResource(R.drawable.bg_primary_action);
        confirm.setOnClickListener(v -> confirmPayment(payment));
        card.addView(confirm, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(46)));
        return card;
    }

    private TextView buildInfoRow(String text) {
        TextView row = new TextView(this);
        row.setText(text);
        row.setTextColor(ContextCompat.getColor(this, R.color.carpool_text_primary));
        row.setTextSize(14);
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

    private void saveQr() {
        String alias = aliasInput.getText() != null ? aliasInput.getText().toString().trim() : "";
        String bank = bankInput.getText() != null ? bankInput.getText().toString().trim() : "";
        String holder = holderInput.getText() != null ? holderInput.getText().toString().trim() : "";

        if (qrPaymentMethodId <= 0) {
            Toast.makeText(this, "Metodo QR no disponible.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedQrImageBase64 == null || selectedQrImageBase64.isEmpty()) {
            Toast.makeText(this, "Selecciona una imagen QR.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (alias.isEmpty()) alias = "QR principal";
        if (holder.isEmpty()) holder = sessionManager.getFullName();

        String finalAlias = alias;
        String finalHolder = holder;
        setLoading(true);
        saveQrButton.setEnabled(false);
        executor.execute(() -> {
            try {
                paymentRemote.createQrPaymentMethod(sessionManager.getUserId(), qrPaymentMethodId,
                        finalAlias, bank, finalHolder, selectedQrImageBase64);
                runOnUiThread(() -> {
                    setLoading(false);
                    saveQrButton.setEnabled(true);
                    Toast.makeText(this, "QR guardado.", Toast.LENGTH_SHORT).show();
                    loadScreen();
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    saveQrButton.setEnabled(true);
                    showError(e.getMessage());
                });
            }
        });
    }

    private void handleQrImage(Uri uri) {
        setLoading(true);
        executor.execute(() -> {
            try {
                Bitmap rawBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                if (rawBitmap == null) {
                    runOnUiThread(() -> {
                        setLoading(false);
                        Toast.makeText(this, "No se pudo cargar la imagen QR.", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }
                Bitmap bitmap = resizeBitmapIfNeeded(rawBitmap, 1024);
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, output);
                String base64 = "data:image/jpeg;base64," + Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP);

                runOnUiThread(() -> {
                    setLoading(false);
                    qrPreview.setImageURI(uri);
                    selectedQrImageBase64 = base64;
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(this, "No se pudo leer la imagen QR.", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private Bitmap resizeBitmapIfNeeded(Bitmap source, int maxDimension) {
        int width = source.getWidth();
        int height = source.getHeight();
        if (width <= maxDimension && height <= maxDimension) {
            return source;
        }
        float ratio = (float) width / (float) height;
        int newWidth, newHeight;
        if (width > height) {
            newWidth = maxDimension;
            newHeight = Math.round(maxDimension / ratio);
        } else {
            newHeight = maxDimension;
            newWidth = Math.round(maxDimension * ratio);
        }
        return Bitmap.createScaledBitmap(source, newWidth, newHeight, true);
    }

    private void confirmPayment(PaymentItem payment) {
        new AlertDialog.Builder(this)
                .setTitle("Confirmar pago")
                .setMessage("Confirmar que recibiste " + String.format(Locale.US, "%.2f %s", payment.amount, payment.currency)
                        + " de " + payment.passengerName + "?")
                .setPositiveButton("Si, recibi", (d, w) -> {
                    setLoading(true);
                    executor.execute(() -> {
                        try {
                            paymentRemote.confirmPayment(sessionManager.getUserId(), payment.id, "Confirmado desde la app movil");
                            runOnUiThread(() -> {
                                setLoading(false);
                                Toast.makeText(this, "Pago confirmado.", Toast.LENGTH_SHORT).show();
                                loadScreen();
                            });
                        } catch (IOException e) {
                            runOnUiThread(() -> {
                                setLoading(false);
                                showError(e.getMessage());
                            });
                        }
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void showError(String raw) {
        new AlertDialog.Builder(this)
                .setTitle("Cobros")
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
}
