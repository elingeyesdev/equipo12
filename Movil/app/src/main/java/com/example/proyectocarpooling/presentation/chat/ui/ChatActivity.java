package com.example.proyectocarpooling.presentation.chat.ui;

import com.example.proyectocarpooling.presentation.BaseActivity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyectocarpooling.BackgroundTaskRunner;
import com.example.proyectocarpooling.CarPoolingApplication;
import com.example.proyectocarpooling.R;
import com.example.proyectocarpooling.data.local.SessionManager;
import com.example.proyectocarpooling.data.model.ChatMessage;
import com.example.proyectocarpooling.data.remote.ChatRemoteDataSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChatActivity extends BaseActivity {

    private String tripId;
    private String currentUserId;

    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private final List<ChatMessage> messageList = new ArrayList<>();

    private EditText chatInputText;
    private ImageButton chatSendButton;
    private TextView chatAvatarInitials;
    private TextView chatHeaderTitle;

    private ChatRemoteDataSource remoteDataSource;
    private BackgroundTaskRunner taskRunner;
    private SessionManager sessionManager;

    // Polling en tiempo real (3 segundos)
    private final Handler pollingHandler = new Handler(Looper.getMainLooper());
    private Runnable pollingRunnable;
    private static final int POLLING_INTERVAL_MS = 3000;
    private boolean isPollingActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        tripId = getIntent().getStringExtra("trip_id");
        String chatTitle = getIntent().getStringExtra("chat_title");

        if (tripId == null || tripId.isBlank()) {
            Toast.makeText(this, "ID de viaje inválido para el chat", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        CarPoolingApplication app = (CarPoolingApplication) getApplication();
        sessionManager = app.getSessionManager();
        remoteDataSource = app.getChatRemoteDataSource();
        taskRunner = app.getTaskRunner();
        currentUserId = sessionManager.getUserId();

        // Vincular componentes
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        chatInputText = findViewById(R.id.chatInputText);
        chatSendButton = findViewById(R.id.chatSendButton);
        chatAvatarInitials = findViewById(R.id.chatAvatarInitials);
        chatHeaderTitle = findViewById(R.id.chatHeaderTitle);
        ImageButton chatBackButton = findViewById(R.id.chatBackButton);

        // Cabecera premium
        if (chatTitle != null && !chatTitle.isBlank()) {
            chatHeaderTitle.setText(chatTitle);
            chatAvatarInitials.setText(getInitials(chatTitle));
        } else {
            String roleName = sessionManager.isDriver() ? "Pasajeros de viaje" : "Tu Conductor";
            chatHeaderTitle.setText(roleName);
            chatAvatarInitials.setText(roleName.substring(0, 1).toUpperCase());
        }

        chatBackButton.setOnClickListener(v -> finish());

        // Configurar RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // El chat empieza al final
        chatRecyclerView.setLayoutManager(layoutManager);
        chatAdapter = new ChatAdapter(messageList, currentUserId);
        chatRecyclerView.setAdapter(chatAdapter);

        // Intentar precargar el chat desde la cache local offline
        List<ChatMessage> cached = com.example.proyectocarpooling.data.local.ChatLocalCache.loadCache(this, tripId);
        if (!cached.isEmpty()) {
            messageList.clear();
            messageList.addAll(cached);
            chatAdapter.notifyDataSetChanged();
            chatRecyclerView.scrollToPosition(Math.max(0, messageList.size() - 1));
        }

        // Evento Enviar
        chatSendButton.setOnClickListener(v -> performSendMessage());

        // Configurar bucle de polling asíncrono
        setupPolling();

        // Cargar historial inicial y marcar como leído
        loadChatHistory(true);
    }

    private void setupPolling() {
        pollingRunnable = new Runnable() {
            @Override
            public void run() {
                if (isPollingActive) {
                    loadChatHistory(false);
                    pollingHandler.postDelayed(this, POLLING_INTERVAL_MS);
                }
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        isPollingActive = true;
        pollingHandler.post(pollingRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        isPollingActive = false;
        pollingHandler.removeCallbacks(pollingRunnable);
    }

    private void loadChatHistory(boolean showLoadingToast) {
        taskRunner.runWithResult(
                () -> remoteDataSource.getMessages(tripId, currentUserId),
                new BackgroundTaskRunner.ResultCallback<List<ChatMessage>>() {
                    @Override
                    public void onSuccess(List<ChatMessage> result) {
                        // Comparar si hay nuevos mensajes para evitar parpadeos innecesarios
                        if (result.size() != messageList.size()) {
                            messageList.clear();
                            messageList.addAll(result);
                            chatAdapter.notifyDataSetChanged();
                            chatRecyclerView.smoothScrollToPosition(Math.max(0, messageList.size() - 1));
                            
                            // Guardar en la cache local offline
                            com.example.proyectocarpooling.data.local.ChatLocalCache.saveCache(ChatActivity.this, tripId, result);
                        }
                    }

                    @Override
                    public void onError(String message) {
                        if (showLoadingToast) {
                            Toast.makeText(ChatActivity.this, "Error al cargar mensajes: " + message, Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    private void performSendMessage() {
        String text = chatInputText.getText().toString().trim();
        if (text.isEmpty()) {
            return;
        }

        chatInputText.setText(""); // Limpiar entrada inmediatamente
        chatSendButton.setEnabled(false);

        // Envío asíncrono
        taskRunner.runWithResult(
                () -> remoteDataSource.sendMessage(tripId, currentUserId, text),
                new BackgroundTaskRunner.ResultCallback<ChatMessage>() {
                    @Override
                    public void onSuccess(ChatMessage result) {
                        chatSendButton.setEnabled(true);
                        // Añadir respuesta oficial del servidor
                        messageList.add(result);
                        chatAdapter.notifyItemInserted(messageList.size() - 1);
                        chatRecyclerView.smoothScrollToPosition(messageList.size() - 1);

                        // Guardar en la cache local offline
                        com.example.proyectocarpooling.data.local.ChatLocalCache.saveCache(ChatActivity.this, tripId, messageList);
                    }

                    @Override
                    public void onError(String message) {
                        chatSendButton.setEnabled(true);
                        chatInputText.setText(text); // Restaurar texto si falló
                        Toast.makeText(ChatActivity.this, "Error al enviar: " + message, Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private static String getInitials(String title) {
        if (title == null || title.isBlank()) return "U";
        String[] parts = title.trim().split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        }
        return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
    }
}
