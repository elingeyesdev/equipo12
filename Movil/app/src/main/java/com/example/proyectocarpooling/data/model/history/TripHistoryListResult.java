package com.example.proyectocarpooling.data.model.history;

import java.util.List;

public class TripHistoryListResult {
    public final TripHistoryStats summary;
    public final List<TripHistorySummaryItem> driverHistory;
    public final List<TripHistorySummaryItem> studentHistory;

    public TripHistoryListResult(TripHistoryStats summary, List<TripHistorySummaryItem> driverHistory, List<TripHistorySummaryItem> studentHistory) {
        this.summary = summary;
        this.driverHistory = driverHistory;
        this.studentHistory = studentHistory;
    }
}
