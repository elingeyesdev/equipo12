package com.example.proyectocarpooling.presentation.support.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyectocarpooling.CarPoolingApplication;
import com.example.proyectocarpooling.R;
import com.example.proyectocarpooling.data.local.SessionManager;
import com.example.proyectocarpooling.data.model.support.SupportTicketItem;
import com.example.proyectocarpooling.presentation.auth.ui.LoginActivity;
import com.example.proyectocarpooling.presentation.support.SupportUiHelper;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

public class SupportActivity extends AppCompatActivity implements SupportTicketsAdapter.Listener {

    public static final String EXTRA_TRIP_ID = "extra_support_trip_id";
    public static final String EXTRA_RESERVATION_ID = "extra_support_reservation_id";
    public static final String EXTRA_CATEGORY = "extra_support_category";
    public static final String EXTRA_OPEN_CREATE_DIALOG = "extra_open_create_dialog";

    public static final int CATEGORY_TRIP = 1;
    public static final int CATEGORY_RESERVATION = 2;

    private static final int[] CATEGORY_VALUES = {1, 2, 3, 4, 5};

    private SessionManager sessionManager;
    private SupportViewModel viewModel;
    private SupportTicketsAdapter adapter;
    private ProgressBar progress;
    private View empty;
    private String linkedTripId;
    private String linkedReservationId;
    private int preselectedCategory;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_support);

        sessionManager = ((CarPoolingApplication) getApplication()).getSessionManager();
        if (!sessionManager.hasActiveSession()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        viewModel = new ViewModelProvider(this).get(SupportViewModel.class);
        readLinkExtras();
        applySessionLinksIfMissing();

        Toolbar toolbar = findViewById(R.id.supportToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        progress = findViewById(R.id.supportProgress);
        empty = findViewById(R.id.supportEmpty);
        RecyclerView recycler = findViewById(R.id.supportRecycler);
        ExtendedFloatingActionButton fab = findViewById(R.id.supportFabNew);

        adapter = new SupportTicketsAdapter(this);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        fab.setOnClickListener(v -> showCreateDialog());

        observeViewModel();
        loadTickets();

        if (getIntent().getBooleanExtra(EXTRA_OPEN_CREATE_DIALOG, false)
                || preselectedCategory == CATEGORY_TRIP
                || preselectedCategory == CATEGORY_RESERVATION) {
            showCreateDialog();
        }
    }

    private void readLinkExtras() {
        Intent intent = getIntent();
        linkedTripId = emptyToNull(intent.getStringExtra(EXTRA_TRIP_ID));
        linkedReservationId = emptyToNull(intent.getStringExtra(EXTRA_RESERVATION_ID));
        preselectedCategory = intent.getIntExtra(EXTRA_CATEGORY, 0);
    }

    private void applySessionLinksIfMissing() {
        if (linkedTripId == null && sessionManager.hasPassengerBookedTrip()) {
            linkedTripId = emptyToNull(sessionManager.getPassengerBookedTripId());
        }
        if (linkedTripId == null) {
            linkedTripId = emptyToNull(sessionManager.getDriverActiveTripId());
        }
        if (linkedReservationId == null) {
            linkedReservationId = emptyToNull(sessionManager.getPassengerBookedReservationId());
        }
    }

    private void observeViewModel() {
        viewModel.getLoading().observe(this, loading ->
                progress.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE));

        viewModel.getTickets().observe(this, list -> {
            adapter.setItems(list);
            boolean isEmpty = list == null || list.isEmpty();
            empty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        });

        viewModel.getErrorEvent().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.getTicketCreatedEvent().observe(this, ticket -> {
            if (ticket != null) {
                showTicketCreatedSuccess(ticket);
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sessionManager != null && sessionManager.hasActiveSession()) {
            loadTickets();
        }
    }

    private void loadTickets() {
        viewModel.loadTickets(sessionManager.getUserId());
    }

    private void showCreateDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_support_ticket, null);
        Spinner categorySpinner = dialogView.findViewById(R.id.supportDialogCategory);
        TextInputEditText subjectInput = dialogView.findViewById(R.id.supportDialogSubject);
        TextInputEditText descriptionInput = dialogView.findViewById(R.id.supportDialogDescription);
        TextView linkInfo = dialogView.findViewById(R.id.supportDialogLinkInfo);

        String[] categoryLabels = getResources().getStringArray(R.array.support_category_labels);
        ArrayAdapter<String> adapterSpinner = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, categoryLabels);
        categorySpinner.setAdapter(adapterSpinner);

        if (preselectedCategory == CATEGORY_TRIP) {
            categorySpinner.setSelection(0);
        } else if (preselectedCategory == CATEGORY_RESERVATION) {
            categorySpinner.setSelection(1);
        }

        AdapterView.OnItemSelectedListener linkListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateLinkInfo(linkInfo, CATEGORY_VALUES[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                updateLinkInfo(linkInfo, CATEGORY_TRIP);
            }
        };
        categorySpinner.setOnItemSelectedListener(linkListener);
        updateLinkInfo(linkInfo, CATEGORY_VALUES[categorySpinner.getSelectedItemPosition()]);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.support_dialog_create_title)
                .setView(dialogView)
                .setPositiveButton(R.string.support_dialog_send, null)
                .setNegativeButton(R.string.dialog_button_cancel, null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String subject = subjectInput.getText() != null
                    ? subjectInput.getText().toString().trim() : "";
            String description = descriptionInput.getText() != null
                    ? descriptionInput.getText().toString().trim() : "";

            if (subject.length() < 3) {
                Toast.makeText(this, R.string.support_error_subject_min, Toast.LENGTH_LONG).show();
                return;
            }
            if (description.length() < 10) {
                Toast.makeText(this, R.string.support_error_description_min, Toast.LENGTH_LONG).show();
                return;
            }

            int categoryIndex = categorySpinner.getSelectedItemPosition();
            int category = CATEGORY_VALUES[Math.max(0, Math.min(categoryIndex, CATEGORY_VALUES.length - 1))];

            String tripId = null;
            String reservationId = null;
            if (category == CATEGORY_TRIP) {
                tripId = linkedTripId;
                if (tripId == null) {
                    Toast.makeText(this, R.string.support_error_trip_required, Toast.LENGTH_LONG).show();
                    return;
                }
            } else if (category == CATEGORY_RESERVATION) {
                reservationId = linkedReservationId;
                if (reservationId == null) {
                    Toast.makeText(this, R.string.support_error_reservation_required, Toast.LENGTH_LONG).show();
                    return;
                }
            }

            dialog.dismiss();
            viewModel.createTicket(
                    sessionManager.getUserId(),
                    category,
                    subject,
                    description,
                    tripId,
                    reservationId
            );
        }));
        dialog.show();
    }

    private void updateLinkInfo(TextView linkInfo, int category) {
        if (category == CATEGORY_TRIP) {
            if (linkedTripId != null) {
                linkInfo.setText(getString(R.string.support_link_info_trip, shortId(linkedTripId)));
            } else {
                linkInfo.setText(R.string.support_link_info_trip_missing);
            }
            return;
        }
        if (category == CATEGORY_RESERVATION) {
            if (linkedReservationId != null) {
                linkInfo.setText(getString(R.string.support_link_info_reservation, shortId(linkedReservationId)));
            } else {
                linkInfo.setText(R.string.support_link_info_reservation_missing);
            }
            return;
        }
        linkInfo.setText(R.string.support_link_info_no_link);
    }

    private void showTicketCreatedSuccess(SupportTicketItem ticket) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_support_ticket_success, null);
        TextView subjectView = dialogView.findViewById(R.id.supportSuccessSubject);
        TextView referenceView = dialogView.findViewById(R.id.supportSuccessReference);
        TextView statusView = dialogView.findViewById(R.id.supportSuccessStatus);

        subjectView.setText(getString(R.string.support_success_subject_line, ticket.subject));
        String reference = SupportUiHelper.formatReference(ticket.id);
        referenceView.setText(getString(R.string.support_success_reference, reference));
        statusView.setText(ticket.statusLabel != null && !ticket.statusLabel.isEmpty()
                ? ticket.statusLabel
                : getString(R.string.support_status_open));

        new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setCancelable(true)
                .setPositiveButton(R.string.support_success_ok, (d, w) -> d.dismiss())
                .show();

        View anchor = findViewById(R.id.supportRoot);
        if (anchor != null) {
            Snackbar.make(anchor, getString(R.string.support_success_snackbar, reference), Snackbar.LENGTH_LONG).show();
        }

        viewModel.clearTicketCreatedEvent();
    }

    @Override
    public void onTicketClicked(SupportTicketItem item) {
        Intent intent = new Intent(this, SupportTicketDetailActivity.class);
        intent.putExtra(SupportTicketDetailActivity.EXTRA_TICKET_ID, item.id);
        startActivity(intent);
    }

    private static String shortId(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        String t = raw.trim();
        return t.length() <= 12 ? t : t.substring(0, 8) + "\u2026";
    }

    @Nullable
    private static String emptyToNull(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
