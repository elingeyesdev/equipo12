package com.example.proyectocarpooling.presentation.schedules.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyectocarpooling.CarPoolingApplication;
import com.example.proyectocarpooling.R;
import com.example.proyectocarpooling.data.model.trip.RecurringReservation;
import com.example.proyectocarpooling.domain.usecase.trip.ManageScheduleUseCase;
import com.example.proyectocarpooling.presentation.BaseActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TripScheduleDetailActivity extends BaseActivity {

    private String scheduleId;
    private ManageScheduleUseCase manageScheduleUseCase;
    private ExecutorService backgroundExecutor;

    private MaterialToolbar toolbar;
    private TextView tvOrigin, tvDestination, tvTime, tvDays, tvSeats, tvFare;
    private RecyclerView recyclerView;
    private TextView tvNoSubscribers;
    private ProgressBar pbSubscribers;

    private PassengersAdapter adapter;

    // Fields to hold coordinate and driver details for preview
    private double originLat;
    private double originLng;
    private double destLat;
    private double destLng;
    private String driverId;
    private String driverName;
    private String originAddress;
    private String destinationAddress;
    private String time;
    private String days;
    private int seats;
    private double fare;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_schedule_detail);

        manageScheduleUseCase = new ManageScheduleUseCase(((CarPoolingApplication) getApplication()).getTripScheduleRepository());
        backgroundExecutor = Executors.newSingleThreadExecutor();

        bindViews();
        setupToolbar();
        parseIntentExtras();
        setupRecyclerView();

        // Wire Ver Ruta en Mapa button
        MaterialButton btnViewRouteMap = findViewById(R.id.btnViewRouteMap);
        btnViewRouteMap.setOnClickListener(v -> {
            Intent previewIntent = new Intent(this, TripSchedulePreviewActivity.class);
            previewIntent.putExtra("EXTRA_SCHEDULE_ID", scheduleId);
            previewIntent.putExtra("EXTRA_DRIVER_ID", driverId);
            previewIntent.putExtra("EXTRA_DRIVER_NAME", driverName);
            previewIntent.putExtra("EXTRA_ORIGIN_LAT", originLat);
            previewIntent.putExtra("EXTRA_ORIGIN_LNG", originLng);
            previewIntent.putExtra("EXTRA_ORIGIN_ADDRESS", originAddress);
            previewIntent.putExtra("EXTRA_DEST_LAT", destLat);
            previewIntent.putExtra("EXTRA_DEST_LNG", destLng);
            previewIntent.putExtra("EXTRA_DEST_ADDRESS", destinationAddress);
            previewIntent.putExtra("EXTRA_TIME", time);
            previewIntent.putExtra("EXTRA_DAYS", days);
            previewIntent.putExtra("EXTRA_SEATS", seats);
            previewIntent.putExtra("EXTRA_FARE", fare);
            previewIntent.putExtra("EXTRA_PREVIEW_ONLY", true);
            startActivity(previewIntent);
        });

        loadSubscriptions();
    }

    private void bindViews() {
        toolbar = findViewById(R.id.detailToolbar);
        tvOrigin = findViewById(R.id.tvDetailOrigin);
        tvDestination = findViewById(R.id.tvDetailDestination);
        tvTime = findViewById(R.id.tvDetailTime);
        tvDays = findViewById(R.id.tvDetailDays);
        tvSeats = findViewById(R.id.tvDetailSeats);
        tvFare = findViewById(R.id.tvDetailFare);
        recyclerView = findViewById(R.id.rvSubscribers);
        tvNoSubscribers = findViewById(R.id.tvNoSubscribers);
        pbSubscribers = findViewById(R.id.pbSubscribers);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void parseIntentExtras() {
        Intent intent = getIntent();
        if (intent != null) {
            scheduleId = intent.getStringExtra("EXTRA_SCHEDULE_ID");
            originAddress = intent.getStringExtra("EXTRA_ORIGIN");
            destinationAddress = intent.getStringExtra("EXTRA_DESTINATION");
            time = intent.getStringExtra("EXTRA_TIME");
            days = intent.getStringExtra("EXTRA_DAYS");
            seats = intent.getIntExtra("EXTRA_SEATS", 4);
            fare = intent.getDoubleExtra("EXTRA_FARE", 10.0);
            originLat = intent.getDoubleExtra("EXTRA_ORIGIN_LAT", 0.0);
            originLng = intent.getDoubleExtra("EXTRA_ORIGIN_LNG", 0.0);
            destLat = intent.getDoubleExtra("EXTRA_DEST_LAT", 0.0);
            destLng = intent.getDoubleExtra("EXTRA_DEST_LNG", 0.0);
            driverId = intent.getStringExtra("EXTRA_DRIVER_ID");
            driverName = intent.getStringExtra("EXTRA_DRIVER_NAME");

            tvOrigin.setText("Origen: " + (originAddress != null ? originAddress : "--"));
            tvDestination.setText("Destino: " + (destinationAddress != null ? destinationAddress : "--"));
            tvTime.setText("Hora de salida: " + (time != null ? formatTime(time) : "--"));
            tvDays.setText("Días: " + formatDaysOfWeek(days));
            tvSeats.setText("Asientos ofrecidos: " + seats);
            tvFare.setText(String.format(Locale.US, "Tarifa: %.2f Bs.", fare));
        }
    }

    private String formatTime(String rawTime) {
        if (rawTime == null || rawTime.length() < 5) return rawTime;
        // e.g. "07:00:00" -> "07:00"
        return rawTime.substring(0, 5);
    }

    private String formatDaysOfWeek(String daysCommaSeparated) {
        if (daysCommaSeparated == null || daysCommaSeparated.isEmpty()) return "Ninguno";
        String[] days = daysCommaSeparated.split(",");
        StringBuilder sb = new StringBuilder();
        for (String day : days) {
            if (sb.length() > 0) sb.append(", ");
            switch (day.trim()) {
                case "1": sb.append("Lun"); break;
                case "2": sb.append("Mar"); break;
                case "3": sb.append("Mié"); break;
                case "4": sb.append("Jue"); break;
                case "5": sb.append("Vie"); break;
                case "6": sb.append("Sáb"); break;
                case "0": sb.append("Dom"); break;
                default: sb.append("Día ").append(day);
            }
        }
        return sb.toString();
    }

    private void setupRecyclerView() {
        adapter = new PassengersAdapter(new PassengersAdapter.Listener() {
            @Override
            public void onApprove(RecurringReservation subscription) {
                approvePassenger(subscription);
            }

            @Override
            public void onReject(RecurringReservation subscription) {
                rejectPassenger(subscription);
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadSubscriptions() {
        if (scheduleId == null || scheduleId.isEmpty()) return;

        pbSubscribers.setVisibility(View.VISIBLE);
        tvNoSubscribers.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);

        backgroundExecutor.execute(() -> {
            try {
                List<RecurringReservation> list = manageScheduleUseCase.getScheduleSubscriptions(scheduleId);
                runOnUiThread(() -> {
                    pbSubscribers.setVisibility(View.GONE);
                    if (isFinishing() || isDestroyed()) return;

                    if (list == null || list.isEmpty()) {
                        tvNoSubscribers.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        tvNoSubscribers.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                        adapter.setItems(list);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    pbSubscribers.setVisibility(View.GONE);
                    if (isFinishing() || isDestroyed()) return;
                    Toast.makeText(this, "Error al cargar pasajeros: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    tvNoSubscribers.setVisibility(View.VISIBLE);
                    tvNoSubscribers.setText("Error al cargar suscripciones.");
                });
            }
        });
    }

    private void approvePassenger(RecurringReservation subscription) {
        pbSubscribers.setVisibility(View.VISIBLE);
        backgroundExecutor.execute(() -> {
            try {
                boolean success = manageScheduleUseCase.approveSubscription(subscription.id);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (success) {
                        Toast.makeText(this, "Suscripción aceptada", Toast.LENGTH_SHORT).show();
                        loadSubscriptions();
                    } else {
                        pbSubscribers.setVisibility(View.GONE);
                        Toast.makeText(this, "No se pudo aceptar la suscripción", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    pbSubscribers.setVisibility(View.GONE);
                    if (isFinishing() || isDestroyed()) return;
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void rejectPassenger(RecurringReservation subscription) {
        new AlertDialog.Builder(this)
                .setTitle("Rechazar suscripción")
                .setMessage("¿Estás seguro de que deseas rechazar la suscripción de " + subscription.passengerName + "?")
                .setPositiveButton("Sí, Rechazar", (dialog, which) -> {
                    pbSubscribers.setVisibility(View.VISIBLE);
                    backgroundExecutor.execute(() -> {
                        try {
                            boolean success = manageScheduleUseCase.rejectSubscription(subscription.id);
                            runOnUiThread(() -> {
                                if (isFinishing() || isDestroyed()) return;
                                if (success) {
                                    Toast.makeText(this, "Suscripción rechazada", Toast.LENGTH_SHORT).show();
                                    loadSubscriptions();
                                } else {
                                    pbSubscribers.setVisibility(View.GONE);
                                    Toast.makeText(this, "No se pudo rechazar la suscripción", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } catch (Exception e) {
                            runOnUiThread(() -> {
                                pbSubscribers.setVisibility(View.GONE);
                                if (isFinishing() || isDestroyed()) return;
                                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
                })
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        if (backgroundExecutor != null) {
            backgroundExecutor.shutdownNow();
        }
        super.onDestroy();
    }

    // Nested Adapter class
    private static class PassengersAdapter extends RecyclerView.Adapter<PassengersAdapter.ViewHolder> {

        interface Listener {
            void onApprove(RecurringReservation subscription);
            void onReject(RecurringReservation subscription);
        }

        private final List<RecurringReservation> items = new ArrayList<>();
        private final Listener listener;

        PassengersAdapter(Listener listener) {
            this.listener = listener;
        }

        void setItems(List<RecurringReservation> list) {
            items.clear();
            if (list != null) {
                items.addAll(list);
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_schedule_passenger, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            RecurringReservation subscription = items.get(position);
            holder.tvName.setText(subscription.passengerName);
            holder.tvSeats.setText("Asiento(s) reservados: " + subscription.seatsReserved);

            // Set Initials
            String initials = "U";
            if (subscription.passengerName != null && !subscription.passengerName.trim().isEmpty()) {
                String[] parts = subscription.passengerName.trim().split("\\s+");
                if (parts.length > 1) {
                    initials = (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
                } else if (parts[0].length() > 0) {
                    initials = parts[0].substring(0, 1).toUpperCase();
                }
            }
            holder.tvInitials.setText(initials);

            // Set Status and Actions
            if (subscription.isAccepted) {
                holder.tvStatus.setText("Aceptado");
                holder.tvStatus.setBackgroundResource(R.drawable.bg_chip_unselected);
                holder.tvStatus.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.carpool_primary));
                holder.layoutActions.setVisibility(View.GONE);
            } else {
                holder.tvStatus.setText("Pendiente");
                holder.tvStatus.setBackgroundResource(R.drawable.bg_chip_unselected);
                holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#FF76686F"));
                holder.layoutActions.setVisibility(View.VISIBLE);

                holder.btnApprove.setOnClickListener(v -> {
                    if (listener != null) listener.onApprove(subscription);
                });

                holder.btnReject.setOnClickListener(v -> {
                    if (listener != null) listener.onReject(subscription);
                });
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final TextView tvInitials;
            final TextView tvName;
            final TextView tvSeats;
            final TextView tvStatus;
            final View layoutActions;
            final MaterialButton btnApprove;
            final MaterialButton btnReject;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvInitials = itemView.findViewById(R.id.tvPassengerInitials);
                tvName = itemView.findViewById(R.id.tvPassengerName);
                tvSeats = itemView.findViewById(R.id.tvPassengerSeats);
                tvStatus = itemView.findViewById(R.id.tvPassengerStatus);
                layoutActions = itemView.findViewById(R.id.layoutActions);
                btnApprove = itemView.findViewById(R.id.btnApprove);
                btnReject = itemView.findViewById(R.id.btnReject);
            }
        }
    }
}
