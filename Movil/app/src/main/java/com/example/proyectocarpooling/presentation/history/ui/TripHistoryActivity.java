package com.example.proyectocarpooling.presentation.history.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyectocarpooling.R;
import com.example.proyectocarpooling.data.local.ApiBaseUrlProvider;
import com.example.proyectocarpooling.data.local.SessionManager;
import com.example.proyectocarpooling.data.model.history.TripHistoryListResult;
import com.example.proyectocarpooling.data.model.history.TripHistorySummaryItem;
import com.example.proyectocarpooling.data.remote.user.TripHistoryRemoteDataSource;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TripHistoryActivity extends AppCompatActivity implements TripHistoryAdapter.Listener {

    private enum Category {STUDENT, DRIVER}

    private MaterialToolbar toolbar;
    private ProgressBar progress;
    private TextView emptyText;
    private Button studentButton;
    private Button driverButton;
    private RecyclerView recycler;
    private TripHistoryAdapter adapter;
    private SessionManager sessionManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private TripHistoryListResult loaded;
    private Category selectedCategory = Category.STUDENT;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_history);

        sessionManager = new SessionManager(this);
        toolbar = findViewById(R.id.historyToolbar);
        progress = findViewById(R.id.historyProgress);
        emptyText = findViewById(R.id.historyEmptyText);
        studentButton = findViewById(R.id.historyStudentButton);
        driverButton = findViewById(R.id.historyDriverButton);
        recycler = findViewById(R.id.historyRecycler);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        adapter = new TripHistoryAdapter(this);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        studentButton.setOnClickListener(v -> {
            selectedCategory = Category.STUDENT;
            renderCategory();
        });
        driverButton.setOnClickListener(v -> {
            selectedCategory = Category.DRIVER;
            renderCategory();
        });

        selectedCategory = sessionManager.isDriver() ? Category.DRIVER : Category.STUDENT;
        loadHistory();
    }

    private void loadHistory() {
        String userId = sessionManager.getUserId();
        if (userId == null || userId.isBlank()) {
            finish();
            return;
        }

        progress.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            try {
                TripHistoryRemoteDataSource api = new TripHistoryRemoteDataSource(ApiBaseUrlProvider.get(this));
                loaded = api.listHistory(userId, sessionManager.getFullName());
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    renderCategory();
                });
            } catch (IOException | JSONException e) {
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(this, R.string.history_load_error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void renderCategory() {
        if (loaded == null) {
            return;
        }
        List<TripHistorySummaryItem> list = selectedCategory == Category.STUDENT
                ? loaded.studentHistory
                : loaded.driverHistory;
        adapter.setItems(list);
        emptyText.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);

        studentButton.setAlpha(selectedCategory == Category.STUDENT ? 1f : 0.6f);
        driverButton.setAlpha(selectedCategory == Category.DRIVER ? 1f : 0.6f);
    }

    @Override
    public void onViewDetail(TripHistorySummaryItem item) {
        Intent intent = new Intent(this, TripHistoryDetailActivity.class);
        intent.putExtra(TripHistoryDetailActivity.EXTRA_TRIP_ID, item.tripId);
        startActivity(intent);
    }

    @Override
    public void onDelete(TripHistorySummaryItem item) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.history_delete_confirm_title)
                .setMessage(getString(R.string.history_delete_confirm_message, item.originLabel, item.destinationLabel))
                .setPositiveButton(R.string.history_delete_confirm_action, (d, w) -> deleteHistoryItem(item))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void deleteHistoryItem(TripHistorySummaryItem item) {
        String userId = sessionManager.getUserId();
        if (userId == null || userId.isBlank()) {
            return;
        }
        progress.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            try {
                TripHistoryRemoteDataSource api = new TripHistoryRemoteDataSource(ApiBaseUrlProvider.get(this));
                api.hideHistoryTrip(userId, item.tripId);
                loaded = api.listHistory(userId, sessionManager.getFullName());
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    renderCategory();
                    Snackbar snackbar = Snackbar.make(recycler, R.string.history_deleted_toast, Snackbar.LENGTH_LONG);
                    snackbar.setDuration(5000);
                    snackbar.setAction(R.string.history_undo, v -> restoreHistoryItem(item));
                    snackbar.show();
                });
            } catch (IOException | JSONException e) {
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(this, R.string.history_delete_error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void restoreHistoryItem(TripHistorySummaryItem item) {
        String userId = sessionManager.getUserId();
        if (userId == null || userId.isBlank()) {
            return;
        }
        progress.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            try {
                TripHistoryRemoteDataSource api = new TripHistoryRemoteDataSource(ApiBaseUrlProvider.get(this));
                api.restoreHistoryTrip(userId, item.tripId);
                loaded = api.listHistory(userId, sessionManager.getFullName());
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    renderCategory();
                    Toast.makeText(this, R.string.history_restored_toast, Toast.LENGTH_SHORT).show();
                });
            } catch (IOException | JSONException e) {
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(this, R.string.history_restore_error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
