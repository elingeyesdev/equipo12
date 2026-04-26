package com.example.proyectocarpooling.presentation.history.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyectocarpooling.R;
import com.example.proyectocarpooling.data.model.history.TripHistorySummaryItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TripHistoryAdapter extends RecyclerView.Adapter<TripHistoryAdapter.Holder> {

    public interface Listener {
        void onViewDetail(TripHistorySummaryItem item);
    }

    private final List<TripHistorySummaryItem> items = new ArrayList<>();
    private final Listener listener;

    public TripHistoryAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setItems(List<TripHistorySummaryItem> list) {
        items.clear();
        if (list != null) {
            items.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trip_history_summary, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        TripHistorySummaryItem item = items.get(position);
        holder.route.setText(String.format(Locale.getDefault(), "%s → %s", item.originLabel, item.destinationLabel));
        holder.status.setText(String.format(Locale.getDefault(), "%s · %s", item.statusLabel, item.createdAt));
        holder.detailButton.setOnClickListener(v -> listener.onViewDetail(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView route;
        final TextView status;
        final Button detailButton;

        Holder(@NonNull View itemView) {
            super(itemView);
            route = itemView.findViewById(R.id.historyItemRoute);
            status = itemView.findViewById(R.id.historyItemStatus);
            detailButton = itemView.findViewById(R.id.historyItemDetailButton);
        }
    }
}
