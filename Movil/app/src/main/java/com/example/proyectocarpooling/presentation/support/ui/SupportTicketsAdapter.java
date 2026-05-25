package com.example.proyectocarpooling.presentation.support.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyectocarpooling.R;
import com.example.proyectocarpooling.data.model.support.SupportTicketItem;

import java.util.ArrayList;
import java.util.List;

public class SupportTicketsAdapter extends RecyclerView.Adapter<SupportTicketsAdapter.Holder> {

    public interface Listener {
        void onTicketClicked(SupportTicketItem item);
    }

    private final List<SupportTicketItem> items = new ArrayList<>();
    private final Listener listener;

    public SupportTicketsAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setItems(List<SupportTicketItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_support_ticket, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        SupportTicketItem item = items.get(position);
        holder.bind(item);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTicketClicked(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        private final TextView subject;
        private final TextView meta;
        private final TextView status;

        Holder(@NonNull View itemView) {
            super(itemView);
            subject = itemView.findViewById(R.id.supportItemSubject);
            meta = itemView.findViewById(R.id.supportItemMeta);
            status = itemView.findViewById(R.id.supportItemStatus);
        }

        void bind(SupportTicketItem item) {
            subject.setText(item.subject);
            String category = item.categoryLabel != null && !item.categoryLabel.isEmpty()
                    ? item.categoryLabel
                    : itemView.getContext().getString(R.string.support_category_other);
            String link = "";
            if (item.category == 1 && item.tripId != null && !item.tripId.isEmpty()) {
                link = " · Viaje";
            } else if (item.category == 2 && item.reservationId != null && !item.reservationId.isEmpty()) {
                link = " · Reserva";
            }
            meta.setText(category + link + " · " + formatDate(item.createdAt));
            status.setText(item.statusLabel != null && !item.statusLabel.isEmpty()
                    ? item.statusLabel
                    : itemView.getContext().getString(R.string.support_status_open));
        }

        private static String formatDate(String iso) {
            if (iso == null || iso.length() < 10) {
                return "";
            }
            return iso.substring(0, 10);
        }
    }
}
