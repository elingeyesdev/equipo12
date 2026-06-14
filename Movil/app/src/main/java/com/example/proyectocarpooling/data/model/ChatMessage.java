package com.example.proyectocarpooling.data.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ChatMessage {

    public static class Reader {
        public final String userId;
        public final String fullName;

        public Reader(String userId, String fullName) {
            this.userId = userId;
            this.fullName = fullName;
        }
    }

    public final String id;
    public final String senderUserId;
    public final String senderFullName;
    public final String messageText;
    public final String createdAt;
    public final String senderProfilePicture;
    public final List<String> readByUserIds;
    public final List<Reader> readers;

    public ChatMessage(String id, String senderUserId, String senderFullName, String messageText,
                       String createdAt, String senderProfilePicture, List<String> readByUserIds, List<Reader> readers) {
        this.id = id;
        this.senderUserId = senderUserId;
        this.senderFullName = senderFullName;
        this.messageText = messageText;
        this.createdAt = createdAt;
        this.senderProfilePicture = senderProfilePicture;
        this.readByUserIds = readByUserIds;
        this.readers = readers;
    }

    public static ChatMessage fromJson(JSONObject obj) throws JSONException {
        List<String> reads = new ArrayList<>();
        if (obj.has("readByUserIds")) {
            JSONArray arr = obj.getJSONArray("readByUserIds");
            for (int i = 0; i < arr.length(); i++) {
                reads.add(arr.getString(i));
            }
        }

        List<Reader> readerList = new ArrayList<>();
        if (obj.has("readers")) {
            JSONArray arr = obj.getJSONArray("readers");
            for (int i = 0; i < arr.length(); i++) {
                JSONObject rObj = arr.getJSONObject(i);
                readerList.add(new Reader(
                        rObj.getString("userId"),
                        rObj.getString("fullName")
                ));
            }
        }

        return new ChatMessage(
                obj.getString("id"),
                obj.getString("senderUserId"),
                obj.getString("senderFullName"),
                obj.getString("messageText"),
                obj.getString("createdAt"),
                obj.optString("senderProfilePicture", ""),
                reads,
                readerList
        );
    }
}
