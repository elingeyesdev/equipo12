package com.example.proyectocarpooling.domain.usecase.history;

import com.example.proyectocarpooling.data.model.history.TripHistoryDetailItem;
import com.example.proyectocarpooling.data.model.history.TripHistoryListResult;
import com.example.proyectocarpooling.domain.repository.history.TripHistoryRepository;

import java.io.IOException;

public class TripHistoryUseCase {

    private final TripHistoryRepository repository;

    public TripHistoryUseCase(TripHistoryRepository repository) {
        this.repository = repository;
    }

    public TripHistoryListResult listHistory(String userId, String passengerName) throws IOException {
        return repository.listHistory(userId, passengerName);
    }

    public TripHistoryDetailItem getHistoryDetail(String userId, String tripId, String passengerName) throws IOException {
        return repository.getHistoryDetail(userId, tripId, passengerName);
    }

    public void hideHistoryTrip(String userId, String tripId) throws IOException {
        repository.hideHistoryTrip(userId, tripId);
    }

    public void restoreHistoryTrip(String userId, String tripId) throws IOException {
        repository.restoreHistoryTrip(userId, tripId);
    }
}
