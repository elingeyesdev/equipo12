package com.example.proyectocarpooling.domain.repository.history;

import com.example.proyectocarpooling.data.model.history.TripHistoryDetailItem;
import com.example.proyectocarpooling.data.model.history.TripHistoryListResult;

import java.io.IOException;

public interface TripHistoryRepository {
    TripHistoryListResult listHistory(String userId, String passengerName) throws IOException;
    TripHistoryDetailItem getHistoryDetail(String userId, String tripId, String passengerName) throws IOException;
    void hideHistoryTrip(String userId, String tripId) throws IOException;
    void restoreHistoryTrip(String userId, String tripId) throws IOException;
}
