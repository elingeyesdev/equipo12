package com.example.proyectocarpooling;

import android.app.Application;

import com.example.proyectocarpooling.data.local.ApiBaseUrlProvider;
import com.example.proyectocarpooling.data.local.SessionManager;
import com.example.proyectocarpooling.data.remote.TripsRemoteDataSource;
import com.example.proyectocarpooling.data.remote.user.FavoritesRemoteDataSource;
import com.example.proyectocarpooling.data.remote.user.TripHistoryRemoteDataSource;
import com.example.proyectocarpooling.data.remote.user.UsersRemoteDataSource;
import com.example.proyectocarpooling.data.repository.TripRepositoryImpl;
import com.example.proyectocarpooling.data.repository.favorites.FavoritesRepositoryImpl;
import com.example.proyectocarpooling.data.repository.history.TripHistoryRepositoryImpl;
import com.example.proyectocarpooling.data.repository.user.UserRepositoryImpl;
import com.example.proyectocarpooling.domain.repository.TripRepository;
import com.example.proyectocarpooling.domain.repository.favorites.FavoritesRepository;
import com.example.proyectocarpooling.domain.repository.history.TripHistoryRepository;
import com.example.proyectocarpooling.domain.repository.user.UserRepository;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

public class CarPoolingApplication extends Application {

    private BackgroundTaskRunner taskRunner;
    private SessionManager sessionManager;
    private TripRepository tripRepository;
    private UserRepository userRepository;
    private FavoritesRepository favoritesRepository;
    private TripHistoryRepository tripHistoryRepository;

    @Override
    public void onCreate() {
        super.onCreate();
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor(
                msg -> android.util.Log.d("CarpoolingHttp", msg));
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC);
        taskRunner = new BackgroundTaskRunner();
    }

    public BackgroundTaskRunner getTaskRunner() {
        return taskRunner;
    }

    public SessionManager getSessionManager() {
        if (sessionManager == null) {
            sessionManager = new SessionManager(this);
        }
        return sessionManager;
    }

    public synchronized TripRepository getTripRepository() {
        if (tripRepository == null) {
            tripRepository = new TripRepositoryImpl(createTripsDataSource());
        }
        return tripRepository;
    }

    public synchronized UserRepository getUserRepository() {
        if (userRepository == null) {
            userRepository = new UserRepositoryImpl(createUsersDataSource());
        }
        return userRepository;
    }

    public synchronized FavoritesRepository getFavoritesRepository() {
        if (favoritesRepository == null) {
            favoritesRepository = new FavoritesRepositoryImpl(createFavoritesDataSource());
        }
        return favoritesRepository;
    }

    public synchronized TripHistoryRepository getTripHistoryRepository() {
        if (tripHistoryRepository == null) {
            tripHistoryRepository = new TripHistoryRepositoryImpl(createHistoryDataSource());
        }
        return tripHistoryRepository;
    }

    private TripsRemoteDataSource createTripsDataSource() {
        return new TripsRemoteDataSource(
                ApiBaseUrlProvider.get(this),
                getString(R.string.mapbox_access_token));
    }

    private UsersRemoteDataSource createUsersDataSource() {
        return new UsersRemoteDataSource(ApiBaseUrlProvider.get(this));
    }

    private FavoritesRemoteDataSource createFavoritesDataSource() {
        return new FavoritesRemoteDataSource(ApiBaseUrlProvider.get(this));
    }

    private TripHistoryRemoteDataSource createHistoryDataSource() {
        return new TripHistoryRemoteDataSource(ApiBaseUrlProvider.get(this));
    }

    private com.example.proyectocarpooling.data.remote.ChatRemoteDataSource chatRemoteDataSource;

    public synchronized com.example.proyectocarpooling.data.remote.ChatRemoteDataSource getChatRemoteDataSource() {
        if (chatRemoteDataSource == null) {
            chatRemoteDataSource = new com.example.proyectocarpooling.data.remote.ChatRemoteDataSource(ApiBaseUrlProvider.get(this));
        }
        return chatRemoteDataSource;
    }
}
