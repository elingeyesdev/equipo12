package com.example.proyectocarpooling.domain.usecase.favorites;

import com.example.proyectocarpooling.data.model.user.UserFavoriteItem;
import com.example.proyectocarpooling.domain.repository.favorites.FavoritesRepository;

import java.io.IOException;
import java.util.List;

public class FavoritesUseCase {

    private final FavoritesRepository repository;

    public FavoritesUseCase(FavoritesRepository repository) {
        this.repository = repository;
    }

    public List<UserFavoriteItem> listFavorites(String userId) throws IOException {
        return repository.listFavorites(userId);
    }

    public void deleteFavorite(String userId, String favoriteId) throws IOException {
        repository.deleteFavorite(userId, favoriteId);
    }
}
