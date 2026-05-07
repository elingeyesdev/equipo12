package com.example.proyectocarpooling.presentation.history.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyectocarpooling.R;
import com.example.proyectocarpooling.data.model.history.TripHistoryParticipantItem;
import com.example.proyectocarpooling.presentation.history.HistoryUiHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HistoryParticipantsAdapter extends RecyclerView.Adapter<HistoryParticipantsAdapter.Holder> {

    private final List<TripHistoryParticipantItem> items = new ArrayList<>();

    public void setItems(List<TripHistoryParticipantItem> list) {
        items.clear();
        if (list != null) {
            items.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history_participant, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        TripHistoryParticipantItem p = items.get(position);
        String name = emptyIfBlank(p.name, "Participante");
        h.name.setText(name);
        h.avatar.setText(initialLetter(name));
        String statusShown = emptyIfBlank(p.statusLabel, "Sin estado");
        HistoryUiHelper.applyStatusPill(h.status, h.itemView.getContext(), p.statusLabel, statusShown);
        h.date.setText(h.itemView.getContext().getString(
                R.string.history_participant_reserved_line,
                emptyIfBlank(p.reservedAt, "—")));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView avatar;
        final TextView name;
        final TextView status;
        final TextView date;

        Holder(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.participantAvatar);
            name = itemView.findViewById(R.id.participantName);
            status = itemView.findViewById(R.id.participantStatusChip);
            date = itemView.findViewById(R.id.participantDate);
        }
    }

    private static String initialLetter(String name) {
        String t = name == null ? "?" : name.trim();
        if (t.isEmpty()) {
            return "?";
        }
        return String.valueOf(Character.toUpperCase(t.charAt(0)));
    }

    private static String emptyIfBlank(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }
}
