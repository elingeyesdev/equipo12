package com.example.proyectocarpooling.presentation.favorites.ui;

import com.example.proyectocarpooling.presentation.BaseActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
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
import com.example.proyectocarpooling.data.model.user.UserFavoriteItem;
import com.example.proyectocarpooling.presentation.auth.ui.LoginActivity;
import com.example.proyectocarpooling.presentation.main.ui.MainActivity;

public class FavoritePlacesActivity extends BaseActivity implements FavoritesAdapter.Listener {

    public static final String EXTRA_PICK_MODE = "extra_pick_mode";

    private Toolbar toolbar;
    private ProgressBar progress;
    private TextView empty;
    private RecyclerView recycler;
    private FavoritesAdapter adapter;
    private SessionManager sessionManager;
    private FavoritesViewModel viewModel;
    private boolean pickMode;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite_places);

        sessionManager = ((CarPoolingApplication) getApplication()).getSessionManager();
        if (!sessionManager.hasActiveSession()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        viewModel = new ViewModelProvider(this).get(FavoritesViewModel.class);
        pickMode = getIntent().getBooleanExtra(EXTRA_PICK_MODE, false);

        toolbar = findViewById(R.id.favoritesToolbar);
        progress = findViewById(R.id.favoritesProgress);
        empty = findViewById(R.id.favoritesEmpty);
        recycler = findViewById(R.id.favoritesRecycler);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setTitle(pickMode ? getString(R.string.favorites_title_pick) : getString(R.string.favorites_title_manage));

        adapter = new FavoritesAdapter(pickMode, this);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        observeViewModel();
        loadFavorites();
    }

    private void observeViewModel() {
        viewModel.getLoading().observe(this, isLoading -> progress.setVisibility(isLoading ? View.VISIBLE : View.GONE));

        viewModel.getFavorites().observe(this, list -> {
            adapter.setItems(list);
            boolean isEmpty = list == null || list.isEmpty();
            empty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            recycler.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        });

        viewModel.getSuccessEvent().observe(this, msg -> {
            if (msg != null) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                loadFavorites();
            }
        });

        viewModel.getErrorEvent().observe(this, error -> {
            if (error != null) {
                String cleanError = sanitizeError(error);
                new AlertDialog.Builder(this)
                        .setTitle("Lugares Favoritos")
                        .setMessage(cleanError)
                        .setPositiveButton("Aceptar", null)
                        .show();
            }
        });
    }

    private void loadFavorites() {
        String userId = sessionManager.getUserId();
        if (userId == null || userId.isBlank()) {
            new AlertDialog.Builder(this)
                    .setTitle("Acceso requerido")
                    .setMessage(R.string.favorites_error_session)
                    .setPositiveButton("Aceptar", (d, w) -> finish())
                    .setCancelable(false)
                    .show();
            return;
        }
        viewModel.loadFavorites(userId);
    }

    @Override
    public void onRowClicked(UserFavoriteItem item) {
        if (pickMode) {
            if (item.isRoute()) {
                navigateToMainWithFavorite(item, false);
            } else {
                new AlertDialog.Builder(this)
                        .setTitle(item.title)
                        .setItems(new CharSequence[]{
                                getString(R.string.favorite_apply_as_destination),
                                getString(R.string.favorite_apply_as_origin)
                        }, (d, which) -> navigateToMainWithFavorite(item, which == 1))
                        .show();
            }
        } else {
            showManageDialog(item);
        }
    }

    private void showManageDialog(UserFavoriteItem item) {
        if (item.isRoute()) {
            new AlertDialog.Builder(this)
                    .setTitle(item.title)
                    .setItems(new CharSequence[]{
                            getString(R.string.favorite_apply_route),
                            getString(R.string.favorite_delete_confirm_action)
                    }, (d, which) -> {
                        if (which == 0) navigateToMainWithFavorite(item, false);
                        else confirmDelete(item);
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle(item.title)
                    .setItems(new CharSequence[]{
                            getString(R.string.favorite_apply_as_destination),
                            getString(R.string.favorite_apply_as_origin),
                            getString(R.string.favorite_delete_confirm_action)
                    }, (d, which) -> {
                        if (which == 0) navigateToMainWithFavorite(item, false);
                        else if (which == 1) navigateToMainWithFavorite(item, true);
                        else confirmDelete(item);
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }
    }

    private void confirmDelete(UserFavoriteItem item) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.favorite_delete_confirm_title)
                .setMessage(getString(R.string.favorite_delete_confirm_message, item.title))
                .setPositiveButton(R.string.favorite_delete_confirm_action, (d, w) -> deleteFavorite(item))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void deleteFavorite(UserFavoriteItem item) {
        viewModel.deleteFavorite(sessionManager.getUserId(), item.id);
    }

    private void navigateToMainWithFavorite(UserFavoriteItem item, boolean asOriginForPlace) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(MainActivity.EXTRA_APPLY_FAVORITE_ID, item.id);
        intent.putExtra(MainActivity.EXTRA_APPLY_FAVORITE_KIND, item.kind);
        intent.putExtra(MainActivity.EXTRA_APPLY_ORIGIN_LAT, item.originLatitude);
        intent.putExtra(MainActivity.EXTRA_APPLY_ORIGIN_LNG, item.originLongitude);
        if (item.isRoute() && item.destinationLatitude != null && item.destinationLongitude != null) {
            intent.putExtra(MainActivity.EXTRA_APPLY_DEST_LAT, item.destinationLatitude);
            intent.putExtra(MainActivity.EXTRA_APPLY_DEST_LNG, item.destinationLongitude);
        }
        intent.putExtra(MainActivity.EXTRA_APPLY_PLACE_AS_ORIGIN, asOriginForPlace);
        startActivity(intent);
        finish();
    }

    @Override
    public void onDeleteClicked(UserFavoriteItem item) {
        confirmDelete(item);
    }
}
