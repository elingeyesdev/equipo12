package com.example.proyectocarpooling.presentation.favorites.ui;

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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyectocarpooling.R;
import com.example.proyectocarpooling.data.local.ApiBaseUrlProvider;
import com.example.proyectocarpooling.data.local.SessionManager;
import com.example.proyectocarpooling.data.model.user.UserFavoriteItem;
import com.example.proyectocarpooling.data.remote.user.FavoritesRemoteDataSource;
import com.example.proyectocarpooling.presentation.auth.ui.LoginActivity;
import com.example.proyectocarpooling.presentation.main.ui.MainActivity;

import org.json.JSONException;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FavoritePlacesActivity extends AppCompatActivity implements FavoritesAdapter.Listener {

    public static final String EXTRA_PICK_MODE = "extra_pick_mode";

    private Toolbar toolbar;
    private ProgressBar progress;
    private TextView empty;
    private RecyclerView recycler;
    private FavoritesAdapter adapter;
    private SessionManager sessionManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private boolean pickMode;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite_places);

        sessionManager = new SessionManager(this);
        if (!sessionManager.hasActiveSession()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

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

        loadFavorites();
    }

    private void loadFavorites() {
        String userId = sessionManager.getUserId();
        if (userId == null || userId.isBlank()) {
            Toast.makeText(this, R.string.favorites_error_session, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        progress.setVisibility(View.VISIBLE);
        empty.setVisibility(View.GONE);

        executor.execute(() -> {
            try {
                FavoritesRemoteDataSource api = new FavoritesRemoteDataSource(ApiBaseUrlProvider.get(this));
                List<UserFavoriteItem> list = api.listFavorites(userId);
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    adapter.setItems(list);
                    boolean isEmpty = list.isEmpty();
                    empty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                    recycler.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                });
            } catch (IOException | JSONException e) {
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(this, R.string.favorites_load_error, Toast.LENGTH_SHORT).show();
                });
            }
        });
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
                        if (which == 0) {
                            navigateToMainWithFavorite(item, false);
                        } else {
                            confirmDelete(item);
                        }
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
                        if (which == 0) {
                            navigateToMainWithFavorite(item, false);
                        } else if (which == 1) {
                            navigateToMainWithFavorite(item, true);
                        } else {
                            confirmDelete(item);
                        }
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
        String userId = sessionManager.getUserId();
        progress.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            try {
                new FavoritesRemoteDataSource(ApiBaseUrlProvider.get(this)).deleteFavorite(userId, item.id);
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(this, R.string.favorite_deleted_toast, Toast.LENGTH_SHORT).show();
                    loadFavorites();
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(this, R.string.favorites_load_error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * @param asOriginForPlace solo aplica si el favorito es lugar (un solo punto): true = origen, false = destino.
     */
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
