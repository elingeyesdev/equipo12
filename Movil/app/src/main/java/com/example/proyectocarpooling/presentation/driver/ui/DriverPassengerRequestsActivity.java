package com.example.proyectocarpooling.presentation.driver.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyectocarpooling.R;
import com.example.proyectocarpooling.data.local.ApiBaseUrlProvider;
import com.example.proyectocarpooling.data.model.ReservationResponse;
import com.example.proyectocarpooling.data.remote.TripsRemoteDataSource;
import com.example.proyectocarpooling.data.repository.TripRepositoryImpl;
import com.example.proyectocarpooling.domain.repository.TripRepository;
import com.google.android.material.appbar.MaterialToolbar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Pantalla del conductor: solicitudes activas (reservas) para el viaje actual,
 * con opciones para rechazar (cancela reserva) o aceptar (confirmación de negocio).
 */
public class DriverPassengerRequestsActivity extends AppCompatActivity {

    public static final String EXTRA_TRIP_ID = "extra_trip_id";

    private MaterialToolbar toolbar;
    private TextView tripSummaryText;
    private TextView countText;
    private TextView emptyText;
    private RecyclerView recyclerView;

    private final List<ReservationResponse> items = new ArrayList<>();
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private RequestsAdapter adapter;
    private String tripId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_passenger_requests);

        tripId = getIntent().getStringExtra(EXTRA_TRIP_ID);
        if (tripId == null || tripId.trim().isEmpty()) {
            Toast.makeText(this, R.string.driver_passenger_requests_load_error, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        toolbar = findViewById(R.id.requestsToolbar);
        tripSummaryText = findViewById(R.id.tripSummaryText);
        countText = findViewById(R.id.countText);
        emptyText = findViewById(R.id.emptyText);
        recyclerView = findViewById(R.id.requestsRecyclerView);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        toolbar.inflateMenu(R.menu.menu_driver_passenger_requests);
        toolbar.setOnMenuItemClickListener(this::onToolbarMenuItem);

        tripSummaryText.setText(getString(R.string.driver_passenger_requests_subtitle, shortTripId(tripId)));
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RequestsAdapter();
        recyclerView.setAdapter(adapter);

        loadReservations();
    }

    private boolean onToolbarMenuItem(MenuItem item) {
        if (item.getItemId() == R.id.action_refresh_requests) {
            loadReservations();
            return true;
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ioExecutor.shutdownNow();
    }

    private static String shortTripId(String id) {
        String t = id.trim();
        if (t.length() <= 12) {
            return t;
        }
        return t.substring(0, 8) + "…";
    }

    private TripRepository buildRepository() {
        String apiBase = ApiBaseUrlProvider.get(this);
        String mapboxToken = getString(R.string.mapbox_access_token);
        return new TripRepositoryImpl(new TripsRemoteDataSource(apiBase, mapboxToken));
    }

    private void loadReservations() {
        TripRepository repository = buildRepository();
        ioExecutor.execute(() -> {
            try {
                List<ReservationResponse> list = repository.getReservations(tripId);
                runOnUiThread(() -> applyList(list));
            } catch (IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(DriverPassengerRequestsActivity.this, R.string.driver_passenger_requests_load_error, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void applyList(List<ReservationResponse> list) {
        items.clear();
        items.addAll(list);
        adapter.notifyDataSetChanged();

        int n = items.size();
        countText.setText(getString(R.string.driver_passenger_requests_count, n));
        boolean empty = n == 0;
        emptyText.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void confirmReject(ReservationResponse reservation) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.driver_passenger_requests_confirm_reject_title)
                .setMessage(getString(R.string.driver_passenger_requests_confirm_reject_msg, reservation.passengerName))
                .setNegativeButton(R.string.dialog_button_cancel, null)
                .setPositiveButton(R.string.dialog_button_confirm, (d, w) -> executeReject(reservation))
                .show();
    }

    private void executeReject(ReservationResponse reservation) {
        TripRepository repository = buildRepository();
        ioExecutor.execute(() -> {
            try {
                repository.cancelReservation(reservation.id);
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.driver_passenger_requests_rejected_toast, Toast.LENGTH_SHORT).show();
                    loadReservations();
                });
            } catch (IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(this, R.string.toast_network_error, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void confirmAccept(ReservationResponse reservation) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.driver_passenger_requests_accept_title)
                .setMessage(getString(R.string.driver_passenger_requests_accept_msg, reservation.passengerName))
                .setPositiveButton(R.string.dialog_button_confirm, (d, w) ->
                        Toast.makeText(this, R.string.driver_passenger_requests_accept_toast, Toast.LENGTH_LONG).show())
                .setNegativeButton(R.string.dialog_button_close, null)
                .show();
    }

    private static String formatRequestTime(String iso) {
        if (iso == null || iso.isEmpty()) {
            return "—";
        }
        String s = iso.replace('T', ' ');
        if (s.length() > 16) {
            return s.substring(0, 16);
        }
        return s;
    }

    private class RequestsAdapter extends RecyclerView.Adapter<RequestsAdapter.VH> {

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_passenger_request, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            ReservationResponse r = items.get(position);
            h.name.setText(r.passengerName);
            h.meta.setText(getString(R.string.driver_passenger_requests_solicitado, formatRequestTime(r.createdAt)));
            h.reject.setOnClickListener(v -> confirmReject(r));
            h.accept.setOnClickListener(v -> confirmAccept(r));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class VH extends RecyclerView.ViewHolder {
            final TextView name;
            final TextView meta;
            final Button reject;
            final Button accept;

            VH(View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.passengerNameText);
                meta = itemView.findViewById(R.id.passengerMetaText);
                reject = itemView.findViewById(R.id.rejectRequestButton);
                accept = itemView.findViewById(R.id.acceptRequestButton);
            }
        }
    }
}
