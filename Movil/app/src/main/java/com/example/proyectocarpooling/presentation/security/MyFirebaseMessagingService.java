package com.example.proyectocarpooling.presentation.security;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.proyectocarpooling.CarPoolingApplication;
import com.example.proyectocarpooling.R;
import com.example.proyectocarpooling.data.local.SessionManager;
import com.example.proyectocarpooling.domain.repository.user.UserRepository;
import com.example.proyectocarpooling.presentation.main.ui.MainActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.io.IOException;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    public static final String ACTION_NOTIFICATION_EVENT = "com.example.proyectocarpooling.NOTIFICATION_EVENT";
    public static final String EXTRA_NOTIFICATION_TYPE = "type";
    public static final String EXTRA_NOTIFICATION_TRIP_ID = "tripId";
    public static final String EXTRA_NOTIFICATION_TITLE = "title";
    public static final String EXTRA_NOTIFICATION_BODY = "body";

    private static final String TAG = "FCMService";
    private static final String CHANNEL_ID = "carpooling_notifications";
    private static final String CHANNEL_NAME = "Carpooling Events";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Nuevo token FCM generado: " + token);
        
        // Guardar token localmente en SharedPreferences
        SessionManager sessionManager = ((CarPoolingApplication) getApplication()).getSessionManager();
        sessionManager.saveFcmToken(token);

        // Si el usuario ya inició sesión, registrarlo inmediatamente en el backend
        if (sessionManager.isLoggedIn()) {
            sendTokenToBackend(sessionManager.getUserId(), token);
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "Mensaje de notificación recibido");

        String title = "Carpooling";
        String body = "";
        String tripId = null;
        String type = null;

        if (remoteMessage.getData().size() > 0) {
            tripId = remoteMessage.getData().get("tripId");
            type = remoteMessage.getData().get("type");
            title = remoteMessage.getData().get("title");
            body = remoteMessage.getData().get("body");
        }

        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body = remoteMessage.getNotification().getBody();
        }

        if (title == null || title.isEmpty()) {
            title = "Carpooling";
        }
        if (body == null) {
            body = "";
        }

        showNotification(title, body, tripId, type);
        notifyForegroundScreens(title, body, tripId, type);
    }

    private void notifyForegroundScreens(String title, String body, String tripId, String type) {
        Intent eventIntent = new Intent(ACTION_NOTIFICATION_EVENT);
        eventIntent.setPackage(getPackageName());
        eventIntent.putExtra(EXTRA_NOTIFICATION_TITLE, title);
        eventIntent.putExtra(EXTRA_NOTIFICATION_BODY, body);
        if (tripId != null) {
            eventIntent.putExtra(EXTRA_NOTIFICATION_TRIP_ID, tripId);
        }
        if (type != null) {
            eventIntent.putExtra(EXTRA_NOTIFICATION_TYPE, type);
        }
        sendBroadcast(eventIntent);
    }

    private void showNotification(String title, String body, String tripId, String type) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Crear el canal en Android Oreo o superior
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notificaciones sobre viajes y mensajes");
            notificationManager.createNotificationChannel(channel);
        }

        // Configurar el Intent para abrir la MainActivity al presionar la notificación
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (tripId != null) {
            intent.putExtra("tripId", tripId);
        }
        if (type != null) {
            intent.putExtra("type", type);
        }
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, (int) System.currentTimeMillis(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground_custom)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setBadgeIconType(NotificationCompat.BADGE_ICON_LARGE)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        notificationManager.notify((int) System.currentTimeMillis(), notificationBuilder.build());
    }

    private void sendTokenToBackend(String userId, String token) {
        CarPoolingApplication app = (CarPoolingApplication) getApplication();
        UserRepository userRepository = app.getUserRepository(); 
        
        app.getTaskRunner().execute(() -> {
            try {
                userRepository.registerFcmToken(userId, token);
                Log.d(TAG, "Token FCM registrado con éxito en el servidor.");
            } catch (IOException e) {
                Log.e(TAG, "Error al registrar FCM Token en el servidor", e);
            }
        });
    }
}
