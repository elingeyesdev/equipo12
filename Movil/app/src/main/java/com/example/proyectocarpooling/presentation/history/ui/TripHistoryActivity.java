package com.example.proyectocarpooling.presentation.history.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyectocarpooling.R;
import com.example.proyectocarpooling.data.local.ApiBaseUrlProvider;
import com.example.proyectocarpooling.data.local.SessionManager;
import com.example.proyectocarpooling.data.model.history.TripHistoryListResult;
import com.example.proyectocarpooling.data.model.history.TripHistorySummaryItem;
import com.example.proyectocarpooling.data.remote.user.TripHistoryRemoteDataSource;
import com.example.proyectocarpooling.presentation.history.TripHistoryListFilter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;

import java.io.IOException;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TripHistoryActivity extends AppCompatActivity implements TripHistoryAdapter.Listener {

    private enum Category { STUDENT, DRIVER }

    private MaterialToolbar toolbar;
    private ProgressBar progress;
    private View historyEmptyState;
    private TextView emptyMessageText;
    private TextView summaryText;
    private TextView statPassenger;
    private TextView statDriver;
    private TextView statTotal;
    private ChipGroup categoryGroup;
    private RecyclerView recycler;
    private TripHistoryAdapter adapter;
    private SessionManager sessionManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private TripHistoryListResult loaded;
    private Category selectedCategory = Category.STUDENT;

    private TextInputEditText searchEdit;
    private Spinner statusSpinner;
    private Spinner monthSpinner;
    private Spinner sortSpinner;
    private ArrayAdapter<String> monthAdapter;
    /** Índice del spinner mes → YearMonth o null en posición 0. */
    private final List<YearMonth> monthSpinnerKeys = new ArrayList<>();
    private boolean suppressFilterCallbacks;

    private final DateTimeFormatter monthDisplayFormat =
            DateTimeFormatter.ofPattern("MMMM yyyy", new Locale("es", "ES"));

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_history);

        sessionManager = new SessionManager(this);
        toolbar = findViewById(R.id.historyToolbar);
        progress = findViewById(R.id.historyProgress);
        historyEmptyState = findViewById(R.id.historyEmptyState);
        emptyMessageText = findViewById(R.id.historyEmptyText);
        summaryText = findViewById(R.id.historySummaryText);
        statPassenger = findViewById(R.id.historyStatPassengerCount);
        statDriver = findViewById(R.id.historyStatDriverCount);
        statTotal = findViewById(R.id.historyStatTotalCount);
        categoryGroup = findViewById(R.id.historyChipGroupCategory);
        recycler = findViewById(R.id.historyRecycler);
        searchEdit = findViewById(R.id.historySearchEdit);
        statusSpinner = findViewById(R.id.historySpinnerStatus);
        monthSpinner = findViewById(R.id.historySpinnerMonth);
        sortSpinner = findViewById(R.id.historySpinnerSort);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        adapter = new TripHistoryAdapter(this);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        setupFilterSpinners();
        setupSearchField();

        selectedCategory = sessionManager.isDriver() ? Category.DRIVER : Category.STUDENT;
        int initialChipId = selectedCategory == Category.STUDENT ? R.id.historyChipStudent : R.id.historyChipDriver;
        categoryGroup.check(initialChipId);
        categoryGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == View.NO_ID) {
                return;
            }
            Category next = checkedId == R.id.historyChipStudent ? Category.STUDENT : Category.DRIVER;
            if (next == selectedCategory) {
                return;
            }
            selectedCategory = next;
            renderCategory();
        });

        loadHistory();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_trip_history, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_history_reset_filters) {
            resetFiltersUi();
            Toast.makeText(this, R.string.history_filters_reset_toast, Toast.LENGTH_SHORT).show();
            applyFiltersToCurrentCategory();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupFilterSpinners() {
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{
                        getString(R.string.history_filter_status_all),
                        getString(R.string.history_filter_status_finished),
                        getString(R.string.history_filter_status_cancelled)
                });
        statusSpinner.setAdapter(statusAdapter);

        monthAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new ArrayList<>());
        monthSpinner.setAdapter(monthAdapter);

        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{
                        getString(R.string.history_sort_recent),
                        getString(R.string.history_sort_oldest),
                        getString(R.string.history_sort_only_cancelled)
                });
        sortSpinner.setAdapter(sortAdapter);

        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!suppressFilterCallbacks && loaded != null) {
                    applyFiltersToCurrentCategory();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };
        statusSpinner.setOnItemSelectedListener(listener);
        monthSpinner.setOnItemSelectedListener(listener);
        sortSpinner.setOnItemSelectedListener(listener);
    }

    private void setupSearchField() {
        searchEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (loaded != null) {
                    applyFiltersToCurrentCategory();
                }
            }
        });
    }

    private void resetFiltersUi() {
        suppressFilterCallbacks = true;
        searchEdit.setText("");
        statusSpinner.setSelection(0);
        sortSpinner.setSelection(0);
        if (monthSpinner.getCount() > 0) {
            monthSpinner.setSelection(0);
        }
        suppressFilterCallbacks = false;
    }

    private TripHistoryListFilter.StatusFilter statusFromSpinner() {
        switch (statusSpinner.getSelectedItemPosition()) {
            case 1:
                return TripHistoryListFilter.StatusFilter.FINISHED;
            case 2:
                return TripHistoryListFilter.StatusFilter.CANCELLED;
            default:
                return TripHistoryListFilter.StatusFilter.ALL;
        }
    }

    private TripHistoryListFilter.SortMode sortFromSpinner() {
        switch (sortSpinner.getSelectedItemPosition()) {
            case 1:
                return TripHistoryListFilter.SortMode.OLDEST_FIRST;
            case 2:
                return TripHistoryListFilter.SortMode.ONLY_CANCELLED_RECENT;
            default:
                return TripHistoryListFilter.SortMode.RECENT_FIRST;
        }
    }

    @Nullable
    private YearMonth monthFromSpinner() {
        int pos = monthSpinner.getSelectedItemPosition();
        if (pos < 0 || pos >= monthSpinnerKeys.size()) {
            return null;
        }
        return monthSpinnerKeys.get(pos);
    }

    private void rebuildMonthSpinner(List<TripHistorySummaryItem> rawCategoryList) {
        YearMonth previousSelection = monthFromSpinner();

        monthSpinnerKeys.clear();
        monthSpinnerKeys.add(null);
        List<YearMonth> distinct = TripHistoryListFilter.distinctMonthsDescending(rawCategoryList);
        monthSpinnerKeys.addAll(distinct);

        List<String> labels = new ArrayList<>();
        labels.add(getString(R.string.history_filter_month_all));
        for (int i = 1; i < monthSpinnerKeys.size(); i++) {
            YearMonth ym = monthSpinnerKeys.get(i);
            labels.add(capitalizeMonth(monthDisplayFormat.format(ym)));
        }

        suppressFilterCallbacks = true;
        monthAdapter.clear();
        monthAdapter.addAll(labels);
        monthAdapter.notifyDataSetChanged();

        int newPos = 0;
        if (previousSelection != null) {
            int idx = monthSpinnerKeys.indexOf(previousSelection);
            if (idx >= 0) {
                newPos = idx;
            }
        }
        monthSpinner.setSelection(newPos);
        suppressFilterCallbacks = false;
    }

    private static String capitalizeMonth(String formatted) {
        if (formatted == null || formatted.isEmpty()) {
            return "";
        }
        return Character.toUpperCase(formatted.charAt(0)) + formatted.substring(1);
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

    private List<TripHistorySummaryItem> rawListForCategory() {
        if (loaded == null) {
            return Collections.emptyList();
        }
        List<TripHistorySummaryItem> list = selectedCategory == Category.STUDENT
                ? loaded.studentHistory
                : loaded.driverHistory;
        return list != null ? list : Collections.emptyList();
    }

    private void renderCategory() {
        if (loaded == null) {
            return;
        }
        if (loaded.summary != null) {
            statPassenger.setText(String.valueOf(loaded.summary.passengerTripsCount));
            statDriver.setText(String.valueOf(loaded.summary.driverTripsCount));
            statTotal.setText(String.valueOf(loaded.summary.totalTripsCount));
            String creative = getString(
                    R.string.history_summary_creative,
                    loaded.summary.passengerTripsCount,
                    loaded.summary.driverTripsCount,
                    loaded.summary.totalTripsCount);
            String roleHighlight = loaded.summary.driverTripsCount > 0
                    ? getString(R.string.history_summary_role_driver)
                    : getString(R.string.history_summary_role_student);
            summaryText.setText(String.format(Locale.getDefault(), "%s\n\n%s", creative, roleHighlight));
        } else {
            statPassenger.setText("0");
            statDriver.setText("0");
            statTotal.setText("0");
            summaryText.setText(R.string.history_summary_fallback);
        }

        List<TripHistorySummaryItem> raw = rawListForCategory();
        rebuildMonthSpinner(raw);
        applyFiltersToCurrentCategory();
    }

    private void applyFiltersToCurrentCategory() {
        List<TripHistorySummaryItem> raw = rawListForCategory();
        String query = searchEdit.getText() != null ? searchEdit.getText().toString() : "";

        TripHistoryListFilter.StatusFilter statusFilter = statusFromSpinner();
        TripHistoryListFilter.SortMode sortMode = sortFromSpinner();
        YearMonth month = monthFromSpinner();

        List<TripHistorySummaryItem> filtered = TripHistoryListFilter.apply(
                raw,
                query,
                statusFilter,
                month,
                sortMode
        );

        adapter.setItems(filtered);

        boolean rawEmpty = raw.isEmpty();
        boolean filteredEmpty = filtered.isEmpty();

        historyEmptyState.setVisibility(filteredEmpty ? View.VISIBLE : View.GONE);
        recycler.setVisibility(filteredEmpty ? View.GONE : View.VISIBLE);

        if (emptyMessageText != null) {
            if (rawEmpty) {
                emptyMessageText.setText(selectedCategory == Category.STUDENT
                        ? R.string.history_empty_student
                        : R.string.history_empty_driver);
            } else {
                emptyMessageText.setText(R.string.history_empty_filtered);
            }
        }
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
