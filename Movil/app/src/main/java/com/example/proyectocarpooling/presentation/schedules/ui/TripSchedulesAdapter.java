package com.example.proyectocarpooling.presentation.schedules.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyectocarpooling.R;
import com.example.proyectocarpooling.data.model.trip.RecurringReservation;
import com.example.proyectocarpooling.data.model.trip.TripSchedule;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.List;

public class TripSchedulesAdapter extends RecyclerView.Adapter<TripSchedulesAdapter.ViewHolder> {

    public static final int TYPE_DRIVER_SCHEDULE = 1;
    public static final int TYPE_PASSENGER_SUBSCRIPTION = 2;
    public static final int TYPE_BROWSE_SCHEDULE = 3;

    public interface Listener {
        void onToggleSchedule(TripSchedule schedule, boolean active);
        void onCancelSubscription(RecurringReservation subscription);
        void onSubscribeToSchedule(TripSchedule schedule);
        void onScheduleClick(TripSchedule schedule);
        void onSubscriptionClick(RecurringReservation subscription);
    }

    private final List<Object> items = new ArrayList<>();
    private final int viewTypeMode;
    private final Listener listener;

    public TripSchedulesAdapter(int viewTypeMode, Listener listener) {
        this.viewTypeMode = viewTypeMode;
        this.listener = listener;
    }

    public void setItems(List<?> list) {
        items.clear();
        if (list != null) {
            items.addAll(list);
        }
        notifyDataSetChanged();
    }

    public Object getItemAt(int position) {
        if (position >= 0 && position < items.size()) {
            return items.get(position);
        }
        return null;
    }

    @Override
    public int getItemViewType(int position) {
        return viewTypeMode;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trip_schedule, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Object item = items.get(position);

        if (viewTypeMode == TYPE_DRIVER_SCHEDULE && item instanceof TripSchedule) {
            TripSchedule schedule = (TripSchedule) item;
            holder.tvScheduleTime.setText(schedule.departureTime);
            holder.tvScheduleDays.setText(formatDaysOfWeek(schedule.daysOfWeek));
            holder.tvScheduleOrigin.setText("Origen: " + schedule.originAddress);
            holder.tvScheduleDestination.setText("Destino: " + schedule.destinationAddress);
            holder.tvScheduleDetails.setText("Asientos: " + schedule.offeredSeats + " | Tarifa: " + schedule.fareAmount + " Bs.");

            holder.switchScheduleActive.setVisibility(View.VISIBLE);
            holder.switchScheduleActive.setOnCheckedChangeListener(null);
            holder.switchScheduleActive.setChecked(schedule.isActive);
            holder.switchScheduleActive.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) {
                    listener.onToggleSchedule(schedule, isChecked);
                }
            });

            holder.btnCancelSubscription.setVisibility(View.GONE);
            holder.btnSubscribeAction.setVisibility(View.GONE);

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onScheduleClick(schedule);
                }
            });

        } else if (viewTypeMode == TYPE_PASSENGER_SUBSCRIPTION && item instanceof RecurringReservation) {
            RecurringReservation sub = (RecurringReservation) item;
            holder.tvScheduleTime.setText(sub.departureTime != null && !sub.departureTime.isEmpty() ? sub.departureTime : "--:--");
            holder.tvScheduleDays.setText(formatDaysOfWeek(sub.daysOfWeek));
            holder.tvScheduleOrigin.setText("Origen: " + sub.originAddress);
            holder.tvScheduleDestination.setText("Destino: " + sub.destinationAddress);
            holder.tvScheduleDetails.setText("Conductor: " + sub.driverName + " | Reservados: " + sub.seatsReserved + " | Estado: " + (sub.isAccepted ? "Aceptado" : "Pendiente"));

            holder.switchScheduleActive.setVisibility(View.GONE);
            holder.btnCancelSubscription.setVisibility(sub.isActive ? View.VISIBLE : View.GONE);
            holder.btnCancelSubscription.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCancelSubscription(sub);
                }
            });
            holder.btnSubscribeAction.setVisibility(View.GONE);

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSubscriptionClick(sub);
                }
            });

        } else if (viewTypeMode == TYPE_BROWSE_SCHEDULE && item instanceof TripSchedule) {
            TripSchedule schedule = (TripSchedule) item;
            holder.tvScheduleTime.setText(schedule.departureTime);
            holder.tvScheduleDays.setText(formatDaysOfWeek(schedule.daysOfWeek));
            holder.tvScheduleOrigin.setText("Origen: " + schedule.originAddress);
            holder.tvScheduleDestination.setText("Destino: " + schedule.destinationAddress);
            holder.tvScheduleDetails.setText("Conductor: " + schedule.driverName + " | Asientos: " + schedule.offeredSeats + " | Tarifa: " + schedule.fareAmount + " Bs.");

            holder.switchScheduleActive.setVisibility(View.GONE);
            holder.btnCancelSubscription.setVisibility(View.GONE);
            holder.btnSubscribeAction.setVisibility(View.VISIBLE);
            holder.btnSubscribeAction.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSubscribeToSchedule(schedule);
                }
            });

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onScheduleClick(schedule);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String formatDaysOfWeek(String daysCommaSeparated) {
        if (daysCommaSeparated == null || daysCommaSeparated.isEmpty()) return "Ninguno";
        String[] days = daysCommaSeparated.split(",");
        StringBuilder sb = new StringBuilder();
        for (String day : days) {
            if (sb.length() > 0) sb.append(", ");
            switch (day.trim()) {
                case "1": sb.append("Lun"); break;
                case "2": sb.append("Mar"); break;
                case "3": sb.append("Mié"); break;
                case "4": sb.append("Jue"); break;
                case "5": sb.append("Vie"); break;
                case "6": sb.append("Sáb"); break;
                case "0": sb.append("Dom"); break;
                default: sb.append("Día ").append(day);
            }
        }
        return sb.toString();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvScheduleTime;
        final TextView tvScheduleDays;
        final TextView tvScheduleOrigin;
        final TextView tvScheduleDestination;
        final TextView tvScheduleDetails;
        final SwitchMaterial switchScheduleActive;
        final MaterialButton btnCancelSubscription;
        final MaterialButton btnSubscribeAction;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvScheduleTime = itemView.findViewById(R.id.tvScheduleTime);
            tvScheduleDays = itemView.findViewById(R.id.tvScheduleDays);
            tvScheduleOrigin = itemView.findViewById(R.id.tvScheduleOrigin);
            tvScheduleDestination = itemView.findViewById(R.id.tvScheduleDestination);
            tvScheduleDetails = itemView.findViewById(R.id.tvScheduleDetails);
            switchScheduleActive = itemView.findViewById(R.id.switchScheduleActive);
            btnCancelSubscription = itemView.findViewById(R.id.btnCancelSubscription);
            btnSubscribeAction = itemView.findViewById(R.id.btnSubscribeAction);
        }
    }
}
