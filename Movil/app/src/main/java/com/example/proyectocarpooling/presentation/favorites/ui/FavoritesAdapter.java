package com.example.proyectocarpooling.presentation.favorites.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyectocarpooling.R;
import com.example.proyectocarpooling.data.model.user.UserFavoriteItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.Holder> {

    public interface Listener {
        void onRowClicked(UserFavoriteItem item);

        void onDeleteClicked(UserFavoriteItem item);
    }

    private final List<UserFavoriteItem> items = new ArrayList<>();
    private final boolean pickMode;
    private final Listener listener;

    public FavoritesAdapter(boolean pickMode, Listener listener) {
        this.pickMode = pickMode;
        this.listener = listener;
    }

    public void setItems(List<UserFavoriteItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_favorite, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        UserFavoriteItem item = items.get(position);
        holder.title.setText(item.title);
        String typeLabel = item.isRoute()
                ? holder.itemView.getContext().getString(R.string.favorite_type_route)
                : holder.itemView.getContext().getString(R.string.favorite_type_place);
        holder.subtitle.setText(String.format(Locale.getDefault(), "%s · %s %d",
                typeLabel,
                holder.itemView.getContext().getString(R.string.favorite_uses_prefix),
                item.useCount));

        holder.delete.setVisibility(pickMode ? View.GONE : View.VISIBLE);
        holder.delete.setOnClickListener(v -> listener.onDeleteClicked(item));
        holder.itemView.setOnClickListener(v -> listener.onRowClicked(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView subtitle;
        final ImageButton delete;

        Holder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.itemFavoriteTitle);
            subtitle = itemView.findViewById(R.id.itemFavoriteSubtitle);
            delete = itemView.findViewById(R.id.itemFavoriteDelete);
        }
    }
}
