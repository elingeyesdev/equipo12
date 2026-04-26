package com.example.proyectocarpooling.data.model.history;

import java.util.List;

public class TripHistoryListResult {
    public final List<TripHistorySummaryItem> driverHistory;
    public final List<TripHistorySummaryItem> studentHistory;

    public TripHistoryListResult(List<TripHistorySummaryItem> driverHistory, List<TripHistorySummaryItem> studentHistory) {
        this.driverHistory = driverHistory;
        this.studentHistory = studentHistory;
    }
}
