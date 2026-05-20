package com.example.proyectocarpooling.data.repository.favorites;

import com.example.proyectocarpooling.data.model.user.UserFavoriteItem;
import com.example.proyectocarpooling.data.remote.user.FavoritesRemoteDataSource;
import com.example.proyectocarpooling.domain.repository.favorites.FavoritesRepository;

import org.json.JSONException;

import java.io.IOException;
import java.util.List;

public class FavoritesRepositoryImpl implements FavoritesRepository {

    private final FavoritesRemoteDataSource remoteDataSource;

    public FavoritesRepositoryImpl(FavoritesRemoteDataSource remoteDataSource) {
        this.remoteDataSource = remoteDataSource;
    }

    @Override
    public List<UserFavoriteItem> listFavorites(String userId) throws IOException {
        try {
            return remoteDataSource.listFavorites(userId);
        } catch (JSONException e) {
            throw new IOException("Respuesta invalida de favoritos", e);
        }
    }

    @Override
    public void createFavorite(String userId, String kind, String title,
                                double originLat, double originLng,
                                Double destLat, Double destLng) throws IOException {
        remoteDataSource.createFavorite(userId, kind, title, originLat, originLng, destLat, destLng);
    }

    @Override
    public void deleteFavorite(String userId, String favoriteId) throws IOException {
        remoteDataSource.deleteFavorite(userId, favoriteId);
    }
}
