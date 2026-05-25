package com.example.proyectocarpooling.presentation.history;

import com.example.proyectocarpooling.data.model.history.TripHistorySummaryItem;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Filtra y ordena ítems de historial en memoria (lista ya cargada del servidor).
 */
public final class TripHistoryListFilter {

    public enum StatusFilter {
        ALL,
        FINISHED,
        CANCELLED
    }

    public enum SortMode {
        RECENT_FIRST,
        OLDEST_FIRST,
        ONLY_CANCELLED_RECENT
    }

    private TripHistoryListFilter() {
    }

    /**
     * Meses distintos presentes en la lista, de más reciente a más antiguo (para el spinner).
     */
    public static List<YearMonth> distinctMonthsDescending(List<TripHistorySummaryItem> items) {
        Set<YearMonth> seen = new HashSet<>();
        List<YearMonth> months = new ArrayList<>();
        if (items == null) {
            return months;
        }
        for (TripHistorySummaryItem item : items) {
            YearMonth ym = parseYearMonth(item.createdAt);
            if (ym != null && seen.add(ym)) {
                months.add(ym);
            }
        }
        months.sort(Comparator.reverseOrder());
        return months;
    }

    public static List<TripHistorySummaryItem> apply(
            List<TripHistorySummaryItem> source,
            String searchText,
            StatusFilter statusFilter,
            YearMonth monthFilterOrNull,
            SortMode sortMode
    ) {
        List<TripHistorySummaryItem> list = new ArrayList<>();
        if (source != null) {
            list.addAll(source);
        }

        String q = searchText == null ? "" : searchText.trim().toLowerCase(Locale.getDefault());
        if (!q.isEmpty()) {
            List<TripHistorySummaryItem> next = new ArrayList<>();
            for (TripHistorySummaryItem item : list) {
                String o = item.originLabel == null ? "" : item.originLabel.toLowerCase(Locale.getDefault());
                String d = item.destinationLabel == null ? "" : item.destinationLabel.toLowerCase(Locale.getDefault());
                if (o.contains(q) || d.contains(q)) {
                    next.add(item);
                }
            }
            list = next;
        }

        if (monthFilterOrNull != null) {
            List<TripHistorySummaryItem> next = new ArrayList<>();
            for (TripHistorySummaryItem item : list) {
                YearMonth ym = parseYearMonth(item.createdAt);
                if (monthFilterOrNull.equals(ym)) {
                    next.add(item);
                }
            }
            list = next;
        }

        SortMode effectiveSort = sortMode == null ? SortMode.RECENT_FIRST : sortMode;

        if (effectiveSort == SortMode.ONLY_CANCELLED_RECENT) {
            List<TripHistorySummaryItem> next = new ArrayList<>();
            for (TripHistorySummaryItem item : list) {
                if (isCancelled(item.statusLabel)) {
                    next.add(item);
                }
            }
            list = next;
        } else if (statusFilter == StatusFilter.FINISHED) {
            List<TripHistorySummaryItem> next = new ArrayList<>();
            for (TripHistorySummaryItem item : list) {
                if (isFinished(item.statusLabel)) {
                    next.add(item);
                }
            }
            list = next;
        } else if (statusFilter == StatusFilter.CANCELLED) {
            List<TripHistorySummaryItem> next = new ArrayList<>();
            for (TripHistorySummaryItem item : list) {
                if (isCancelled(item.statusLabel)) {
                    next.add(item);
                }
            }
            list = next;
        }

        Comparator<TripHistorySummaryItem> byDate = Comparator.comparingLong(TripHistoryListFilter::parseCreatedAtMillis);
        if (effectiveSort == SortMode.OLDEST_FIRST) {
            Collections.sort(list, byDate);
        } else {
            Collections.sort(list, byDate.reversed());
        }

        return list;
    }

    static long parseCreatedAtMillis(TripHistorySummaryItem item) {
        return parseCreatedAtMillis(item.createdAt);
    }

    static long parseCreatedAtMillis(String createdAt) {
        if (createdAt == null || createdAt.trim().isEmpty()) {
            return 0L;
        }
        String s = createdAt.trim().replace(' ', 'T');
        try {
            if (s.length() >= 19) {
                LocalDateTime ldt = LocalDateTime.parse(s.substring(0, 19), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                return ldt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            }
            LocalDate d = LocalDate.parse(s.substring(0, Math.min(10, s.length())), DateTimeFormatter.ISO_LOCAL_DATE);
            return d.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (DateTimeParseException e) {
            return 0L;
        }
    }

    static YearMonth parseYearMonth(String createdAt) {
        if (createdAt == null || createdAt.length() < 7) {
            return null;
        }
        try {
            return YearMonth.parse(createdAt.substring(0, 7), DateTimeFormatter.ofPattern("yyyy-MM", Locale.US));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    static boolean isCancelled(String statusLabel) {
        return normalizedKind(statusLabel) == 2;
    }

    static boolean isFinished(String statusLabel) {
        return normalizedKind(statusLabel) == 4;
    }

    /** 0 unknown, 2 cancelled, 4 finished, -1 other (activo, listo, en curso…) */
    private static int normalizedKind(String raw) {
        if (raw == null) {
            return 0;
        }
        String s = raw.trim().toLowerCase(Locale.US);
        if (s.equals("2") || s.contains("cancel")) {
            return 2;
        }
        if (s.equals("4") || s.contains("finaliz") || s.contains("finished") || s.contains("completed")) {
            return 4;
        }
        return -1;
    }
}
