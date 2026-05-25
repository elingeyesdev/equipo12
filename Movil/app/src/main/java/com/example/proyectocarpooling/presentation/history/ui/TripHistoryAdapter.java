package com.example.proyectocarpooling.presentation.history.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyectocarpooling.R;
import com.example.proyectocarpooling.data.model.history.TripHistorySummaryItem;
import com.example.proyectocarpooling.presentation.history.HistoryUiHelper;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TripHistoryAdapter extends RecyclerView.Adapter<TripHistoryAdapter.Holder> {

    public interface Listener {
        void onViewDetail(TripHistorySummaryItem item);

        void onDelete(TripHistorySummaryItem item);
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
        String routeFmt = holder.itemView.getContext().getString(
                R.string.history_item_route_arrow,
                item.originLabel.isEmpty() ? "—" : item.originLabel,
                item.destinationLabel.isEmpty() ? "—" : item.destinationLabel);
        holder.route.setText(routeFmt);
        HistoryUiHelper.applyStatusPill(
                holder.statusChip,
                holder.itemView.getContext(),
                item.statusLabel,
                item.statusLabel.isEmpty() ? holder.itemView.getContext().getString(R.string.history_status_unknown) : item.statusLabel);
        holder.meta.setText(holder.itemView.getContext().getString(R.string.history_meta_line, compactDate(item.createdAt), readableCategory(holder.itemView, item.category)));

        holder.itemView.setOnClickListener(v -> listener.onViewDetail(item));
        holder.detailButton.setOnClickListener(v -> listener.onViewDetail(item));
        holder.deleteButton.setOnClickListener(v -> listener.onDelete(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private static String readableCategory(View v, String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return v.getContext().getString(R.string.history_category_unknown);
        }
        String s = raw.trim().toLowerCase(Locale.US);
        if (s.contains("pasaj")) {
            return v.getContext().getString(R.string.history_category_student);
        }
        if (s.contains("conduct")) {
            return v.getContext().getString(R.string.history_category_driver);
        }
        return raw.trim();
    }

    private static String compactDate(String iso) {
        if (iso == null || iso.trim().isEmpty()) {
            return "—";
        }
        String t = iso.replace('T', ' ');
        return t.length() > 18 ? t.substring(0, 16) : t;
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView route;
        final TextView statusChip;
        final TextView meta;
        final MaterialButton detailButton;
        final MaterialButton deleteButton;

        Holder(@NonNull View itemView) {
            super(itemView);
            route = itemView.findViewById(R.id.historyItemRoute);
            statusChip = itemView.findViewById(R.id.historyItemStatusChip);
            meta = itemView.findViewById(R.id.historyItemMeta);
            detailButton = itemView.findViewById(R.id.historyItemDetailButton);
            deleteButton = itemView.findViewById(R.id.historyItemDeleteButton);
        }
    }
}
