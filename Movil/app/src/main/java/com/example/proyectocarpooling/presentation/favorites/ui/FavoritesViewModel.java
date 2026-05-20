package com.example.proyectocarpooling.presentation.favorites.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.proyectocarpooling.BackgroundTaskRunner;
import com.example.proyectocarpooling.CarPoolingApplication;
import com.example.proyectocarpooling.data.model.user.UserFavoriteItem;
import com.example.proyectocarpooling.domain.usecase.favorites.FavoritesUseCase;

import java.io.IOException;
import java.util.List;

public class FavoritesViewModel extends AndroidViewModel {

    private final FavoritesUseCase favoritesUseCase;
    private final BackgroundTaskRunner taskRunner;

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<List<UserFavoriteItem>> favorites = new MutableLiveData<>();
    private final MutableLiveData<String> errorEvent = new MutableLiveData<>();
    private final MutableLiveData<String> successEvent = new MutableLiveData<>();

    public FavoritesViewModel(@NonNull Application application) {
        super(application);
        CarPoolingApplication app = (CarPoolingApplication) application;
        favoritesUseCase = new FavoritesUseCase(app.getFavoritesRepository());
        taskRunner = app.getTaskRunner();
    }

    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<List<UserFavoriteItem>> getFavorites() { return favorites; }
    public LiveData<String> getErrorEvent() { return errorEvent; }
    public LiveData<String> getSuccessEvent() { return successEvent; }

    public void loadFavorites(String userId) {
        loading.setValue(true);
        taskRunner.runWithResult(
                () -> favoritesUseCase.listFavorites(userId),
                new BackgroundTaskRunner.ResultCallback<List<UserFavoriteItem>>() {
                    @Override public void onSuccess(List<UserFavoriteItem> list) {
                        loading.postValue(false);
                        favorites.postValue(list);
                    }
                    @Override public void onError(String message) {
                        loading.postValue(false);
                        errorEvent.postValue("Error cargando favoritos");
                    }
                });
    }

    public void deleteFavorite(String userId, String favoriteId) {
        loading.setValue(true);
        taskRunner.run(
                () -> favoritesUseCase.deleteFavorite(userId, favoriteId),
                new BackgroundTaskRunner.SimpleCallback() {
                    @Override public void onSuccess() {
                        loading.postValue(false);
                        successEvent.postValue("Favorito eliminado");
                    }
                    @Override public void onError(String message) {
                        loading.postValue(false);
                        errorEvent.postValue("Error eliminando favorito");
                    }
                });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
    }
}
