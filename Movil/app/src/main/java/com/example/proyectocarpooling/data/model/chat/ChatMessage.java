package com.example.proyectocarpooling.data.model.chat;

import org.json.JSONException;
import org.json.JSONObject;

public class ChatMessage {

    public final String id;
    public final String senderUserId;
    public final String senderFullName;
    public final String messageText;
    public final String createdAt;
    public final String senderProfilePicture;

    public ChatMessage(String id, String senderUserId, String senderFullName, String messageText,
                       String createdAt, String senderProfilePicture) {
        this.id = id;
        this.senderUserId = senderUserId;
        this.senderFullName = senderFullName;
        this.messageText = messageText;
        this.createdAt = createdAt;
        this.senderProfilePicture = senderProfilePicture;
    }

    public static ChatMessage fromJson(JSONObject obj) throws JSONException {
        return new ChatMessage(
                obj.getString("id"),
                obj.getString("senderUserId"),
                obj.getString("senderFullName"),
                obj.getString("messageText"),
                obj.getString("createdAt"),
                obj.optString("senderProfilePicture", "")
        );
    }
}
