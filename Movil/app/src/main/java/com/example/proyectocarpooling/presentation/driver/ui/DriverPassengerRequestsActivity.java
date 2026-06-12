package com.example.proyectocarpooling.presentation.driver.ui;

import com.example.proyectocarpooling.presentation.BaseActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyectocarpooling.R;
import com.example.proyectocarpooling.data.model.ReservationResponse;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

public class DriverPassengerRequestsActivity extends BaseActivity {

    public static final String EXTRA_TRIP_ID = "extra_trip_id";

    private static final int VIEW_TYPE_HEADER = 1;
    private static final int VIEW_TYPE_ITEM = 2;

    private MaterialToolbar toolbar;
    private TextView tripSummaryText, countText, emptyText;
    private RecyclerView recyclerView;
    private RequestsAdapter adapter;

    private DriverPassengerRequestsViewModel viewModel;
    private final List<ListItem> items = new ArrayList<>();
    private String tripId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_passenger_requests);

        tripId = getIntent().getStringExtra(EXTRA_TRIP_ID);
        if (tripId == null || tripId.trim().isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("Error de carga")
                    .setMessage(R.string.driver_passenger_requests_load_error)
                    .setPositiveButton("Aceptar", (d, w) -> finish())
                    .setCancelable(false)
                    .show();
            return;
        }

        viewModel = new ViewModelProvider(this).get(DriverPassengerRequestsViewModel.class);

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

        observeViewModel();
        viewModel.loadReservations(tripId);
    }

    private void observeViewModel() {
        viewModel.getReservations().observe(this, data -> {
            if (data != null) {
                applyLists(data.pending, data.confirmed, data.boarded);
            }
        });

        viewModel.getSuccessEvent().observe(this, msg -> {
            if (msg != null) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                viewModel.loadReservations(tripId);
            }
        });

        viewModel.getErrorEvent().observe(this, error -> {
            if (error != null) {
                String cleanError = sanitizeError(error);
                new AlertDialog.Builder(this)
                        .setTitle("Solicitudes de Pasajeros")
                        .setMessage(cleanError)
                        .setPositiveButton("Aceptar", null)
                        .show();
            }
        });
    }

    private boolean onToolbarMenuItem(MenuItem item) {
        if (item.getItemId() == R.id.action_refresh_requests) {
            viewModel.loadReservations(tripId);
            return true;
        }
        return false;
    }

    private static String shortTripId(String id) {
        String t = id.trim();
        return t.length() <= 12 ? t : t.substring(0, 8) + "...";
    }

    private void applyLists(List<ReservationResponse> pending, List<ReservationResponse> confirmed, List<ReservationResponse> boarded) {
        rebuildFlatItems(pending, confirmed, boarded);
        adapter.notifyDataSetChanged();

        int n = pending.size();
        int m = confirmed.size();
        int b = boarded.size();
        countText.setText(getString(R.string.driver_passenger_requests_count_sections, n, m, b));
        boolean empty = n + m + b == 0;
        emptyText.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void rebuildFlatItems(List<ReservationResponse> pending, List<ReservationResponse> confirmed, List<ReservationResponse> boarded) {
        items.clear();
        if (!pending.isEmpty()) {
            items.add(ListItem.header(getString(R.string.driver_passenger_requests_section_pending, pending.size()), 1));
            for (ReservationResponse r : pending) {
                items.add(ListItem.item(r, 1));
            }
        }
        if (!confirmed.isEmpty()) {
            items.add(ListItem.header(getString(R.string.driver_passenger_requests_section_confirmed, confirmed.size()), 2));
            for (ReservationResponse r : confirmed) {
                items.add(ListItem.item(r, 2));
            }
        }
        if (!boarded.isEmpty()) {
            items.add(ListItem.header(getString(R.string.driver_passenger_requests_section_boarded, boarded.size()), 3));
            for (ReservationResponse r : boarded) {
                items.add(ListItem.item(r, 3));
            }
        }
    }

    private void confirmReject(ReservationResponse reservation) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.driver_passenger_requests_confirm_reject_title)
                .setMessage(getString(R.string.driver_passenger_requests_confirm_reject_msg, reservation.getPassengerName()))
                .setNegativeButton(R.string.dialog_button_cancel, null)
                .setPositiveButton(R.string.dialog_button_confirm, (d, w) -> viewModel.rejectReservation(tripId, reservation.id))
                .show();
    }

    private void confirmAccept(ReservationResponse reservation) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.driver_passenger_requests_accept_title)
                .setMessage(getString(R.string.driver_passenger_requests_accept_msg, reservation.getPassengerName()))
                .setPositiveButton(R.string.dialog_button_confirm, (d, w) -> viewModel.acceptReservation(tripId, reservation.id))
                .setNegativeButton(R.string.dialog_button_close, null)
                .show();
    }

    private void executeBoard(ReservationResponse reservation) {
        final EditText input = new EditText(this);
        input.setHint("Código de abordaje del pasajero");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        setupErrorClearer(input);

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Verificar código de abordaje")
                .setView(input)
                .setPositiveButton("Confirmar", null)
                .setNegativeButton("Cancelar", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button confirmBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            confirmBtn.setOnClickListener(v -> {
                String code = input.getText().toString().trim();
                if (code.isEmpty()) {
                    setErrorState(input, true, "Ingrese el código");
                    return;
                }
                dialog.dismiss();
                viewModel.verifyAndBoardPassenger(tripId, reservation.id, code);
            });
        });
        dialog.show();
    }

    private static String formatRequestTime(String iso) {
        if (iso == null || iso.isEmpty()) return "-";
        String s = iso.replace('T', ' ');
        return s.length() > 16 ? s.substring(0, 16) : s;
    }

    private static class ListItem {
        final boolean isHeader;
        final int listType; // 1 = pending, 2 = confirmed, 3 = boarded
        final String headerText;
        final ReservationResponse reservation;

        private ListItem(boolean isHeader, int listType, String headerText, ReservationResponse reservation) {
            this.isHeader = isHeader;
            this.listType = listType;
            this.headerText = headerText;
            this.reservation = reservation;
        }

        static ListItem header(String text, int listType) {
            return new ListItem(true, listType, text, null);
        }

        static ListItem item(ReservationResponse r, int listType) {
            return new ListItem(false, listType, null, r);
        }
    }

    private class RequestsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        @Override
        public int getItemViewType(int position) {
            return items.get(position).isHeader ? VIEW_TYPE_HEADER : VIEW_TYPE_ITEM;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == VIEW_TYPE_HEADER) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_section_header, parent, false);
                return new HeaderVH(v);
            } else {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_passenger_request, parent, false);
                return new ItemVH(v);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ListItem item = items.get(position);
            if (item.isHeader) {
                HeaderVH h = (HeaderVH) holder;
                h.headerText.setText(item.headerText);
            } else {
                ItemVH h = (ItemVH) holder;
                ReservationResponse r = item.reservation;
                h.name.setText(r.getPassengerName());
                h.meta.setText(getString(R.string.driver_passenger_requests_solicitado, formatRequestTime(r.createdAt)));

                if (item.listType == 1) { // pending
                    h.accept.setVisibility(View.VISIBLE);
                    h.reject.setVisibility(View.VISIBLE);
                    h.board.setVisibility(View.GONE);
                    h.reject.setOnClickListener(v -> confirmReject(r));
                    h.accept.setOnClickListener(v -> confirmAccept(r));
                } else if (item.listType == 2) { // confirmed
                    h.accept.setVisibility(View.GONE);
                    h.reject.setVisibility(View.GONE);
                    h.board.setVisibility(View.VISIBLE);
                    h.board.setOnClickListener(v -> executeBoard(r));
                } else { // boarded
                    h.accept.setVisibility(View.GONE);
                    h.reject.setVisibility(View.GONE);
                    h.board.setVisibility(View.GONE); // Already boarded
                }
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    static class HeaderVH extends RecyclerView.ViewHolder {
        final TextView headerText;

        HeaderVH(View itemView) {
            super(itemView);
            headerText = itemView.findViewById(R.id.sectionHeaderText);
        }
    }

    static class ItemVH extends RecyclerView.ViewHolder {
        final TextView name, meta;
        final Button reject, accept, board;

        ItemVH(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.passengerNameText);
            meta = itemView.findViewById(R.id.passengerMetaText);
            reject = itemView.findViewById(R.id.rejectRequestButton);
            accept = itemView.findViewById(R.id.acceptRequestButton);
            board = itemView.findViewById(R.id.boardRequestButton);
        }
    }
}
