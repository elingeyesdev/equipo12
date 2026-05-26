package com.example.proyectocarpooling.presentation.support.ui;

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
import com.example.proyectocarpooling.data.remote.SupportChatRemoteDataSource;
import com.example.proyectocarpooling.presentation.chat.ui.ChatAdapter;

import java.util.ArrayList;
import java.util.List;

public class SupportChatActivity extends AppCompatActivity {

    public static final String EXTRA_TICKET_ID = "extra_support_chat_ticket_id";
    public static final String EXTRA_CHAT_TITLE = "extra_support_chat_title";

    private String ticketId;
    private String currentUserId;

    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private final List<ChatMessage> messageList = new ArrayList<>();

    private EditText chatInputText;
    private ImageButton chatSendButton;
    private TextView chatHeaderTitle;
    private TextView chatAvatarInitials;
    private SupportChatRemoteDataSource remoteDataSource;
    private BackgroundTaskRunner taskRunner;
    private SessionManager sessionManager;

    private final Handler pollingHandler = new Handler(Looper.getMainLooper());
    private Runnable pollingRunnable;
    private static final int POLLING_INTERVAL_MS = 3000;
    private boolean isPollingActive = false;
    private boolean chatClosed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        ticketId = getIntent().getStringExtra(EXTRA_TICKET_ID);
        String chatTitle = getIntent().getStringExtra(EXTRA_CHAT_TITLE);

        if (ticketId == null || ticketId.isBlank()) {
            Toast.makeText(this, R.string.support_chat_invalid_ticket, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        CarPoolingApplication app = (CarPoolingApplication) getApplication();
        sessionManager = app.getSessionManager();
        remoteDataSource = app.getSupportChatRemoteDataSource();
        taskRunner = app.getTaskRunner();
        currentUserId = sessionManager.getUserId();

        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        chatInputText = findViewById(R.id.chatInputText);
        chatSendButton = findViewById(R.id.chatSendButton);
        chatAvatarInitials = findViewById(R.id.chatAvatarInitials);
        chatHeaderTitle = findViewById(R.id.chatHeaderTitle);
        ImageButton chatBackButton = findViewById(R.id.chatBackButton);

        String title = chatTitle != null && !chatTitle.isBlank()
                ? chatTitle
                : getString(R.string.support_chat_title);
        chatHeaderTitle.setText(title);
        chatAvatarInitials.setText(getString(R.string.support_chat_avatar));

        chatBackButton.setOnClickListener(v -> finish());

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        chatRecyclerView.setLayoutManager(layoutManager);
        chatAdapter = new ChatAdapter(messageList, currentUserId);
        chatRecyclerView.setAdapter(chatAdapter);

        chatSendButton.setOnClickListener(v -> performSendMessage());
        setupPolling();
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
                () -> remoteDataSource.getMessages(currentUserId, ticketId),
                new BackgroundTaskRunner.ResultCallback<List<ChatMessage>>() {
                    @Override
                    public void onSuccess(List<ChatMessage> result) {
                        if (result.size() != messageList.size()) {
                            messageList.clear();
                            messageList.addAll(result);
                            chatAdapter.notifyDataSetChanged();
                            chatRecyclerView.smoothScrollToPosition(Math.max(0, messageList.size() - 1));
                        }
                    }

                    @Override
                    public void onError(String message) {
                        if (showLoadingToast) {
                            Toast.makeText(
                                    SupportChatActivity.this,
                                    getString(R.string.support_chat_load_error, message),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                }
        );
    }

    private void performSendMessage() {
        if (chatClosed) {
            Toast.makeText(this, R.string.support_chat_closed, Toast.LENGTH_SHORT).show();
            return;
        }

        String text = chatInputText.getText().toString().trim();
        if (text.isEmpty()) {
            return;
        }

        chatInputText.setText("");
        chatSendButton.setEnabled(false);

        taskRunner.runWithResult(
                () -> remoteDataSource.sendMessage(currentUserId, ticketId, text),
                new BackgroundTaskRunner.ResultCallback<ChatMessage>() {
                    @Override
                    public void onSuccess(ChatMessage result) {
                        chatSendButton.setEnabled(true);
                        messageList.add(result);
                        chatAdapter.notifyItemInserted(messageList.size() - 1);
                        chatRecyclerView.smoothScrollToPosition(messageList.size() - 1);
                    }

                    @Override
                    public void onError(String message) {
                        chatSendButton.setEnabled(true);
                        chatInputText.setText(text);
                        if (message != null && message.toLowerCase().contains("cerrado")) {
                            setChatClosedUi(true);
                        }
                        Toast.makeText(
                                SupportChatActivity.this,
                                getString(R.string.support_chat_send_error, message),
                                Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    private void setChatClosedUi(boolean closed) {
        chatClosed = closed;
        chatInputText.setEnabled(!closed);
        chatSendButton.setEnabled(!closed);
    }
}
