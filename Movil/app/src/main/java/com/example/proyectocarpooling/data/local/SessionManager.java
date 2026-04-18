package com.example.proyectocarpooling.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.proyectocarpooling.data.model.user.UserResponse;

public class SessionManager {

    private static final String PREF_NAME = "carpooling_session";
    private static final String KEY_LOGGED_IN = "logged_in";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_FULL_NAME = "full_name";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PHONE = "phone";
    private static final String KEY_ROLE = "role";
    private static final String KEY_EXPIRES_AT = "expires_at";
    /** Viaje activo del conductor en este dispositivo (clave por usuario). */
    private static final String KEY_DRIVER_ACTIVE_TRIP_PREFIX = "driver_active_trip_";
    private static final long THIRTY_DAYS_MILLIS = 30L * 24L * 60L * 60L * 1000L;

    private final SharedPreferences preferences;

    public SessionManager(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveUser(UserResponse user) {
        long expiresAt = System.currentTimeMillis() + THIRTY_DAYS_MILLIS;

        preferences.edit()
                .putBoolean(KEY_LOGGED_IN, true)
                .putString(KEY_USER_ID, user.id)
                .putString(KEY_FULL_NAME, user.fullName)
                .putString(KEY_EMAIL, user.email)
                .putString(KEY_PHONE, user.phoneNumber)
                .putString(KEY_ROLE, user.role)
                .putLong(KEY_EXPIRES_AT, expiresAt)
                .commit();
    }

    public boolean isLoggedIn() {
        return hasActiveSession();
    }

    public boolean hasActiveSession() {
        boolean loggedIn = preferences.getBoolean(KEY_LOGGED_IN, false);
        String userId = preferences.getString(KEY_USER_ID, "");
        String email = preferences.getString(KEY_EMAIL, "");
        long expiresAt = preferences.getLong(KEY_EXPIRES_AT, 0L);

        boolean hasMinimumData = userId != null && !userId.isBlank() && email != null && !email.isBlank();
        boolean notExpired = expiresAt > System.currentTimeMillis();

        boolean valid = loggedIn && hasMinimumData && notExpired;
        if (!valid && loggedIn) {
            clearSession();
        }

        return valid;
    }

    public String getUserId() {
        return preferences.getString(KEY_USER_ID, "");
    }

    public String getFullName() {
        return preferences.getString(KEY_FULL_NAME, "Usuario invitado");
    }

    public String getEmail() {
        return preferences.getString(KEY_EMAIL, "Perfil por configurar");
    }

    public String getPhone() {
        return preferences.getString(KEY_PHONE, "");
    }

    public String getRole() {
        return preferences.getString(KEY_ROLE, "student");
    }

    public boolean isDriver() {
        String role = getRole();
        return role != null && role.trim().equalsIgnoreCase("driver");
    }

    private String driverActiveTripKey() {
        String uid = getUserId();
        if (uid == null || uid.isBlank()) {
            return KEY_DRIVER_ACTIVE_TRIP_PREFIX + "unknown";
        }
        return KEY_DRIVER_ACTIVE_TRIP_PREFIX + uid.trim();
    }

    public void saveDriverActiveTripId(String tripId) {
        if (tripId == null || tripId.isBlank()) {
            return;
        }
        preferences.edit().putString(driverActiveTripKey(), tripId.trim()).apply();
    }

    public String getDriverActiveTripId() {
        return preferences.getString(driverActiveTripKey(), "");
    }

    public void clearDriverActiveTripId() {
        preferences.edit().remove(driverActiveTripKey()).apply();
    }

    public void clearSession() {
        preferences.edit().clear().commit();
    }
}
