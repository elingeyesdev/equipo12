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

    private final SharedPreferences preferences;

    public SessionManager(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveUser(UserResponse user) {
        preferences.edit()
                .putBoolean(KEY_LOGGED_IN, true)
                .putString(KEY_USER_ID, user.id)
                .putString(KEY_FULL_NAME, user.fullName)
                .putString(KEY_EMAIL, user.email)
                .putString(KEY_PHONE, user.phoneNumber)
                .apply();
    }

    public boolean isLoggedIn() {
        return preferences.getBoolean(KEY_LOGGED_IN, false);
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

    public void clearSession() {
        preferences.edit().clear().apply();
    }
}
