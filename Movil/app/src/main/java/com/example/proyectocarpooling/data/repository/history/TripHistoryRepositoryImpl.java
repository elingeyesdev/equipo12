package com.example.proyectocarpooling.data.repository.history;

import com.example.proyectocarpooling.data.model.history.TripHistoryDetailItem;
import com.example.proyectocarpooling.data.model.history.TripHistoryListResult;
import com.example.proyectocarpooling.data.remote.user.TripHistoryRemoteDataSource;
import com.example.proyectocarpooling.domain.repository.history.TripHistoryRepository;

import org.json.JSONException;

import java.io.IOException;

public class TripHistoryRepositoryImpl implements TripHistoryRepository {

    private final TripHistoryRemoteDataSource remoteDataSource;

    public TripHistoryRepositoryImpl(TripHistoryRemoteDataSource remoteDataSource) {
        this.remoteDataSource = remoteDataSource;
    }

    @Override
    public TripHistoryListResult listHistory(String userId, String passengerName) throws IOException {
        try {
            return remoteDataSource.listHistory(userId, passengerName);
        } catch (JSONException e) {
            throw new IOException("Respuesta invalida de historial", e);
        }
    }

    @Override
    public TripHistoryDetailItem getHistoryDetail(String userId, String tripId, String passengerName) throws IOException {
        try {
            return remoteDataSource.getHistoryDetail(userId, tripId, passengerName);
        } catch (JSONException e) {
            throw new IOException("Respuesta invalida de detalle", e);
        }
    }

    @Override
    public void hideHistoryTrip(String userId, String tripId) throws IOException {
        remoteDataSource.hideHistoryTrip(userId, tripId);
    }

    @Override
    public void restoreHistoryTrip(String userId, String tripId) throws IOException {
        remoteDataSource.restoreHistoryTrip(userId, tripId);
    }
}
