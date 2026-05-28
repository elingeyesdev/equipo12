package com.example.proyectocarpooling.presentation.search.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyectocarpooling.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SearchTripAdapter extends RecyclerView.Adapter<SearchTripAdapter.Holder> {

    public interface Listener {
        void onViewRoute(SearchTripActivity.SearchTripResultItem item);
        void onReserveTrip(SearchTripActivity.SearchTripResultItem item);
        void onCancelReservation(SearchTripActivity.SearchTripResultItem item);
        void onBoardTrip(SearchTripActivity.SearchTripResultItem item);
    }

    private final List<SearchTripActivity.SearchTripResultItem> items = new ArrayList<>();
    private final Listener listener;
    private final String accessorUserId;

    public SearchTripAdapter(Listener listener, String accessorUserId) {
        this.listener = listener;
        this.accessorUserId = accessorUserId;
    }

    public void setItems(List<SearchTripActivity.SearchTripResultItem> list) {
        items.clear();
        if (list != null) {
            items.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search_trip_result, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        SearchTripActivity.SearchTripResultItem item = items.get(position);
        holder.routeText.setText(item.routeDescription);
        holder.driverText.setText(item.driverName);
        holder.statusText.setText(holder.itemView.getContext().getString(R.string.search_trip_status_format, item.statusLabel));
        holder.distanceText.setText(holder.itemView.getContext().getString(R.string.search_trip_distance_format, item.distanceKm));
        holder.timeText.setText(holder.itemView.getContext().getString(R.string.search_trip_estimated_time_format, formatEta(item.etaMinutes)));
        holder.seatsText.setText(String.format(Locale.getDefault(), "Cupos: %d", item.availableSeats));

        if (item.driverRating != null) {
            holder.ratingText.setText(holder.itemView.getContext().getString(R.string.search_trip_estimated_rating_format, item.driverRating));
        } else {
            holder.ratingText.setText("Calificación: --");
        }

        holder.vehicleText.setText(item.vehicleInfo == null || item.vehicleInfo.isEmpty() ? "" : item.vehicleInfo);
        holder.viewButton.setOnClickListener(v -> listener.onViewRoute(item));
        holder.card.setOnClickListener(v -> listener.onViewRoute(item));

        // Enforce visibility of action buttons
        if (accessorUserId != null && accessorUserId.equals(item.driverUserId)) {
            holder.actionsContainer.setVisibility(View.GONE);
        } else {
            holder.actionsContainer.setVisibility(View.VISIBLE);
            if (item.myReservationStatus == null) {
                holder.reserveButton.setVisibility(View.VISIBLE);
                holder.cancelButton.setVisibility(View.GONE);
                holder.boardButton.setVisibility(View.GONE);
                boolean canBook = item.availableSeats > 0 && 
                        ("Scheduled".equalsIgnoreCase(item.statusLabel) || "Ready".equalsIgnoreCase(item.statusLabel) || "Programado".equalsIgnoreCase(item.statusLabel) || "Listo".equalsIgnoreCase(item.statusLabel) || "Activo".equalsIgnoreCase(item.statusLabel));
                holder.reserveButton.setEnabled(canBook);
            } else if ("Pending".equalsIgnoreCase(item.myReservationStatus)) {
                holder.reserveButton.setVisibility(View.GONE);
                holder.cancelButton.setVisibility(View.VISIBLE);
                holder.boardButton.setVisibility(View.GONE);
                holder.cancelButton.setEnabled(true);
            } else if ("Confirmed".equalsIgnoreCase(item.myReservationStatus)) {
                holder.reserveButton.setVisibility(View.GONE);
                holder.cancelButton.setVisibility(View.VISIBLE);
                holder.boardButton.setVisibility(View.VISIBLE);
                holder.cancelButton.setEnabled(true);
                boolean canBoard = "Ready".equalsIgnoreCase(item.statusLabel) || "InProgress".equalsIgnoreCase(item.statusLabel) || "Listo".equalsIgnoreCase(item.statusLabel) || "En curso".equalsIgnoreCase(item.statusLabel) || "Activo".equalsIgnoreCase(item.statusLabel);
                holder.boardButton.setEnabled(canBoard);
            } else { // "Boarded" or other
                holder.actionsContainer.setVisibility(View.GONE);
            }
        }

        holder.reserveButton.setOnClickListener(v -> listener.onReserveTrip(item));
        holder.cancelButton.setOnClickListener(v -> listener.onCancelReservation(item));
        holder.boardButton.setOnClickListener(v -> listener.onBoardTrip(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private static String formatEta(int etaMinutes) {
        int safeMinutes = Math.max(0, etaMinutes);
        int hours = safeMinutes / 60;
        int minutes = safeMinutes % 60;
        if (hours > 0) {
            return String.format(Locale.getDefault(), "%dh %02dm", hours, minutes);
        }
        return String.format(Locale.getDefault(), "%dm", minutes);
    }

    static class Holder extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final TextView routeText;
        final TextView driverText;
        final TextView statusText;
        final TextView distanceText;

        final TextView ratingText;
        final TextView timeText;
        final TextView seatsText;
        final TextView vehicleText;
        final MaterialButton viewButton;
        final View actionsContainer;
        final MaterialButton reserveButton;
        final MaterialButton cancelButton;
        final MaterialButton boardButton;

        Holder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.searchTripCard);
            routeText = itemView.findViewById(R.id.searchTripRouteText);
            driverText = itemView.findViewById(R.id.searchTripDriverText);
            statusText = itemView.findViewById(R.id.searchTripStatusText);
            distanceText = itemView.findViewById(R.id.searchTripDistanceText);
            ratingText = itemView.findViewById(R.id.searchTripRatingText);
            timeText = itemView.findViewById(R.id.searchTripTimeText);
            seatsText = itemView.findViewById(R.id.searchTripSeatsText);
            vehicleText = itemView.findViewById(R.id.searchTripVehicleText);
            viewButton = itemView.findViewById(R.id.searchTripViewButton);
            actionsContainer = itemView.findViewById(R.id.searchTripActionsContainer);
            reserveButton = itemView.findViewById(R.id.searchTripReserveButton);
            cancelButton = itemView.findViewById(R.id.searchTripCancelButton);
            boardButton = itemView.findViewById(R.id.searchTripBoardButton);
        }
    }
}