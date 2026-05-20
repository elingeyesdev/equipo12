package com.example.proyectocarpooling.domain.repository.favorites;

import com.example.proyectocarpooling.data.model.user.UserFavoriteItem;

import java.io.IOException;
import java.util.List;

public interface FavoritesRepository {
    List<UserFavoriteItem> listFavorites(String userId) throws IOException;
    void createFavorite(String userId, String kind, String title,
                        double originLat, double originLng,
                        Double destLat, Double destLng) throws IOException;
    void deleteFavorite(String userId, String favoriteId) throws IOException;
}
