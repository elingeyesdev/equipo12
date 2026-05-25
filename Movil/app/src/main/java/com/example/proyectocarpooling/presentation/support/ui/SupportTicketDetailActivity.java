package com.example.proyectocarpooling.presentation.support.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.lifecycle.ViewModelProvider;

import com.example.proyectocarpooling.CarPoolingApplication;
import com.example.proyectocarpooling.R;
import com.example.proyectocarpooling.data.local.SessionManager;
import com.example.proyectocarpooling.data.model.support.SupportTicketItem;
import com.example.proyectocarpooling.presentation.support.SupportUiHelper;
import com.google.android.material.appbar.MaterialToolbar;

public class SupportTicketDetailActivity extends AppCompatActivity {

    public static final String EXTRA_TICKET_ID = "extra_support_ticket_id";

    private SessionManager sessionManager;
    private SupportViewModel viewModel;
    private ProgressBar progress;
    private NestedScrollView scroll;
    private TextView subjectView;
    private TextView referenceView;
    private TextView statusBadge;
    private TextView categoryView;
    private TextView createdView;
    private TextView updatedView;
    private TextView tripView;
    private TextView reservationView;
    private TextView descriptionView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_support_ticket_detail);

        String ticketId = getIntent().getStringExtra(EXTRA_TICKET_ID);
        if (ticketId == null || ticketId.isBlank()) {
            finish();
            return;
        }

        sessionManager = ((CarPoolingApplication) getApplication()).getSessionManager();
        if (!sessionManager.hasActiveSession()) {
            finish();
            return;
        }

        viewModel = new ViewModelProvider(this).get(SupportViewModel.class);
        bindViews();
        observeViewModel();
        viewModel.loadTicketDetail(sessionManager.getUserId(), ticketId.trim());
    }

    private void bindViews() {
        MaterialToolbar toolbar = findViewById(R.id.supportDetailToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        progress = findViewById(R.id.supportDetailProgress);
        scroll = findViewById(R.id.supportDetailScroll);
        subjectView = findViewById(R.id.supportDetailSubject);
        referenceView = findViewById(R.id.supportDetailReference);
        statusBadge = findViewById(R.id.supportDetailStatusBadge);
        categoryView = findViewById(R.id.supportDetailCategory);
        createdView = findViewById(R.id.supportDetailCreated);
        updatedView = findViewById(R.id.supportDetailUpdated);
        tripView = findViewById(R.id.supportDetailTrip);
        reservationView = findViewById(R.id.supportDetailReservation);
        descriptionView = findViewById(R.id.supportDetailDescription);
    }

    private void observeViewModel() {
        viewModel.getLoading().observe(this, loading ->
                progress.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE));

        viewModel.getDetailTicket().observe(this, ticket -> {
            if (ticket != null) {
                renderTicket(ticket);
                scroll.setVisibility(View.VISIBLE);
                viewModel.clearDetailTicket();
            }
        });

        viewModel.getDetailErrorEvent().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private void renderTicket(SupportTicketItem ticket) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setSubtitle("#" + SupportUiHelper.formatReference(ticket.id));
        }

        subjectView.setText(ticket.subject);
        referenceView.setText(getString(
                R.string.support_success_reference,
                SupportUiHelper.formatReference(ticket.id)));

        String statusText = ticket.statusLabel != null && !ticket.statusLabel.isEmpty()
                ? ticket.statusLabel
                : getString(R.string.support_status_open);
        SupportUiHelper.applyStatusPill(statusBadge, this, ticket.status, ticket.statusLabel, statusText);

        String category = ticket.categoryLabel != null && !ticket.categoryLabel.isEmpty()
                ? ticket.categoryLabel
                : getString(R.string.support_category_other);
        categoryView.setText(getString(R.string.support_detail_category, category));
        createdView.setText(getString(
                R.string.support_detail_created_at,
                SupportUiHelper.formatDateTime(ticket.createdAt)));

        if (ticket.updatedAt != null && !ticket.updatedAt.isBlank()) {
            updatedView.setVisibility(View.VISIBLE);
            updatedView.setText(getString(
                    R.string.support_detail_updated_at,
                    SupportUiHelper.formatDateTime(ticket.updatedAt)));
        } else {
            updatedView.setVisibility(View.GONE);
        }

        if (ticket.tripId != null && !ticket.tripId.isEmpty()) {
            tripView.setVisibility(View.VISIBLE);
            tripView.setText(getString(R.string.support_detail_trip, SupportUiHelper.shortId(ticket.tripId)));
        } else {
            tripView.setVisibility(View.GONE);
        }

        if (ticket.reservationId != null && !ticket.reservationId.isEmpty()) {
            reservationView.setVisibility(View.VISIBLE);
            reservationView.setText(getString(
                    R.string.support_detail_reservation,
                    SupportUiHelper.shortId(ticket.reservationId)));
        } else {
            reservationView.setVisibility(View.GONE);
        }

        descriptionView.setText(ticket.description);
    }
}
