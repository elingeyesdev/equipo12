package com.example.proyectocarpooling.data.local;

import android.content.Context;
import android.util.Log;

import com.example.proyectocarpooling.data.model.ChatMessage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class ChatLocalCache {

    private static final String TAG = "ChatLocalCache";
    private static final String FILE_PREFIX = "chat_cache_";

    private ChatLocalCache() {
    }

    /**
     * Guarda la lista de mensajes en un archivo JSON local en el almacenamiento seguro de la app.
     */
    public static void saveCache(Context context, String tripId, List<ChatMessage> messages) {
        if (context == null || tripId == null || tripId.isBlank() || messages == null) {
            return;
        }

        JSONArray arr = new JSONArray();
        try {
            for (ChatMessage m : messages) {
                JSONObject obj = new JSONObject();
                obj.put("id", m.id);
                obj.put("senderUserId", m.senderUserId);
                obj.put("senderFullName", m.senderFullName);
                obj.put("messageText", m.messageText);
                obj.put("createdAt", m.createdAt);
                obj.put("senderProfilePicture", m.senderProfilePicture);

                JSONArray reads = new JSONArray();
                for (String r : m.readByUserIds) {
                    reads.put(r);
                }
                obj.put("readByUserIds", reads);
                arr.put(obj);
            }

            File file = new File(context.getFilesDir(), FILE_PREFIX + tripId.trim() + ".json");
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(arr.toString().getBytes());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al guardar cache local de mensajes", e);
        }
    }

    /**
     * Carga la lista de mensajes guardada localmente para un viaje específico.
     */
    public static List<ChatMessage> loadCache(Context context, String tripId) {
        List<ChatMessage> list = new ArrayList<>();
        if (context == null || tripId == null || tripId.isBlank()) {
            return list;
        }

        File file = new File(context.getFilesDir(), FILE_PREFIX + tripId.trim() + ".json");
        if (!file.exists()) {
            return list;
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] data = new byte[(int) file.length()];
            int read = fis.read(data);
            if (read > 0) {
                String json = new String(data, 0, read);
                JSONArray arr = new JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    list.add(ChatMessage.fromJson(arr.getJSONObject(i)));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al leer cache local de mensajes", e);
        }
        return list;
    }
}
