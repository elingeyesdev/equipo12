package com.example.proyectocarpooling.presentation.chat.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyectocarpooling.R;
import com.example.proyectocarpooling.data.model.ChatMessage;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MessageViewHolder> {

    private final List<ChatMessage> messages;
    private final String currentUserId;

    public ChatAdapter(List<ChatMessage> messages, String currentUserId) {
        this.messages = messages;
        this.currentUserId = currentUserId != null ? currentUserId.trim() : "";
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        boolean isSentByMe = message.senderUserId.equalsIgnoreCase(currentUserId);

        if (isSentByMe) {
            holder.sentContainer.setVisibility(View.VISIBLE);
            holder.receivedContainer.setVisibility(View.GONE);

            holder.sentText.setText(message.messageText);
            holder.sentTime.setText(formatTime(message.createdAt));

            // Marcar doble check si hay más lectores en el chat grupal
            boolean isReadByOthers = message.readByUserIds.size() > 1;
            if (isReadByOthers) {
                holder.sentReadCheck.setImageResource(android.R.drawable.checkbox_on_background); // Check verde/azul en estilo premium
                holder.sentReadCheck.setVisibility(View.VISIBLE);
            } else {
                holder.sentReadCheck.setImageResource(android.R.drawable.checkbox_off_background); // Check simple
                holder.sentReadCheck.setVisibility(View.VISIBLE);
            }
        } else {
            holder.sentContainer.setVisibility(View.GONE);
            holder.receivedContainer.setVisibility(View.VISIBLE);

            holder.receivedSenderName.setText(message.senderFullName);
            holder.receivedText.setText(message.messageText);
            holder.receivedTime.setText(formatTime(message.createdAt));

            String initials = generateInitials(message.senderFullName);
            holder.receivedAvatarInitials.setText(initials);

            android.content.Context ctx = holder.itemView.getContext();
            com.example.proyectocarpooling.presentation.BaseActivity activity = null;
            while (ctx instanceof android.content.ContextWrapper) {
                if (ctx instanceof com.example.proyectocarpooling.presentation.BaseActivity) {
                    activity = (com.example.proyectocarpooling.presentation.BaseActivity) ctx;
                    break;
                }
                ctx = ((android.content.ContextWrapper) ctx).getBaseContext();
            }

            if (activity != null) {
                activity.loadBase64Image(
                        message.senderProfilePicture,
                        holder.receivedAvatarImage,
                        holder.receivedAvatarPlaceholder
                );
            } else {
                holder.receivedAvatarImage.setVisibility(View.GONE);
                holder.receivedAvatarPlaceholder.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    private static String formatTime(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            return "";
        }
        try {
            // El backend retorna ISO8601 (ej. "2026-05-23T15:00:30.123Z" o similar)
            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            parser.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = parser.parse(rawDate);
            
            SimpleDateFormat formatter = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            return date != null ? formatter.format(date) : "";
        } catch (ParseException e) {
            // Fallback si el parseo falla (limpiar la cabecera ISO si es legible directamente)
            if (rawDate.length() >= 16) {
                return rawDate.substring(11, 16);
            }
            return rawDate;
        }
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        final LinearLayout sentContainer;
        final TextView sentText;
        final TextView sentTime;
        final ImageView sentReadCheck;

        final LinearLayout receivedContainer;
        final TextView receivedSenderName;
        final TextView receivedText;
        final TextView receivedTime;
        final ImageView receivedAvatarImage;
        final View receivedAvatarPlaceholder;
        final TextView receivedAvatarInitials;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            sentContainer = itemView.findViewById(R.id.sentContainer);
            sentText = itemView.findViewById(R.id.sentText);
            sentTime = itemView.findViewById(R.id.sentTime);
            sentReadCheck = itemView.findViewById(R.id.sentReadCheck);

            receivedContainer = itemView.findViewById(R.id.receivedContainer);
            receivedSenderName = itemView.findViewById(R.id.receivedSenderName);
            receivedText = itemView.findViewById(R.id.receivedText);
            receivedTime = itemView.findViewById(R.id.receivedTime);
            receivedAvatarImage = itemView.findViewById(R.id.receivedAvatarImage);
            receivedAvatarPlaceholder = itemView.findViewById(R.id.receivedAvatarPlaceholder);
            receivedAvatarInitials = itemView.findViewById(R.id.receivedAvatarInitials);
        }
    }

    private static String generateInitials(String fullName) {
        if (fullName == null || fullName.isEmpty()) return "U";
        String[] parts = fullName.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();
        for (int i = 0; i < Math.min(2, parts.length); i++) {
            if (parts[i].length() > 0) initials.append(parts[i].charAt(0));
        }
        return initials.length() > 0 ? initials.toString().toUpperCase() : "U";
    }
}
