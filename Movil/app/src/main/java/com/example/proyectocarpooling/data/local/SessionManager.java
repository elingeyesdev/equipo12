package com.example.proyectocarpooling.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.proyectocarpooling.data.model.user.UserResponse;

import java.util.HashSet;
import java.util.Set;

public class SessionManager {

    private static final String PREF_NAME = "carpooling_session";
    private static final String KEY_LOGGED_IN = "logged_in";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_FULL_NAME = "full_name";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PHONE = "phone";
    private static final String KEY_ROLE = "role";
    private static final String KEY_PROFILE_PICTURE = "profile_picture";
    private static final String KEY_EXPIRES_AT = "expires_at";
    /** Viaje activo del conductor en este dispositivo (clave por usuario). */
    private static final String KEY_DRIVER_ACTIVE_TRIP_PREFIX = "driver_active_trip_";
    /** Viaje reservado por el pasajero en este dispositivo. */
    private static final String KEY_PASSENGER_BOOKED_TRIP_ID = "passenger_booked_trip_id";
    private static final String KEY_PASSENGER_BOOKED_RESERVATION_ID = "passenger_booked_reservation_id";
    private static final String KEY_PASSENGER_BOOKING_CODE = "passenger_boarding_code";
    private static final String KEY_PASSENGER_DRIVER_NAME = "passenger_driver_name";
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
                .putString(KEY_PROFILE_PICTURE, user.profilePicture)
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

    public String getProfilePicture() {
        return preferences.getString(KEY_PROFILE_PICTURE, "");
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

    public void savePassengerBookedTrip(String tripId, String boardingCode, String driverName) {
        savePassengerBookedTrip(tripId, null, boardingCode, driverName);
    }

    public void savePassengerBookedTrip(
            String tripId,
            String reservationId,
            String boardingCode,
            String driverName
    ) {
        preferences.edit()
                .putString(KEY_PASSENGER_BOOKED_TRIP_ID, tripId != null ? tripId.trim() : "")
                .putString(KEY_PASSENGER_BOOKED_RESERVATION_ID, reservationId != null ? reservationId.trim() : "")
                .putString(KEY_PASSENGER_BOOKING_CODE, boardingCode != null ? boardingCode : "")
                .putString(KEY_PASSENGER_DRIVER_NAME, driverName != null ? driverName : "")
                .apply();
    }

    public String getPassengerBookedReservationId() {
        return preferences.getString(KEY_PASSENGER_BOOKED_RESERVATION_ID, "");
    }

    public String getPassengerBookedTripId() {
        return preferences.getString(KEY_PASSENGER_BOOKED_TRIP_ID, "");
    }

    public String getPassengerBoardingCode() {
        return preferences.getString(KEY_PASSENGER_BOOKING_CODE, "");
    }

    public String getPassengerDriverName() {
        return preferences.getString(KEY_PASSENGER_DRIVER_NAME, "");
    }

    public boolean hasPassengerBookedTrip() {
        String id = getPassengerBookedTripId();
        return id != null && !id.isEmpty();
    }

    public void clearPassengerBookedTrip() {
        preferences.edit()
                .remove(KEY_PASSENGER_BOOKED_TRIP_ID)
                .remove(KEY_PASSENGER_BOOKED_RESERVATION_ID)
                .remove(KEY_PASSENGER_BOOKING_CODE)
                .remove(KEY_PASSENGER_DRIVER_NAME)
                .apply();
    }

    // --- Historial: ocultar viajes localmente ---
    private static final String KEY_HIDDEN_TRIP_IDS = "hidden_trip_ids";

    public void hideHistoryTrip(String tripId) {
        Set<String> hidden = getHiddenTripIds();
        hidden.add(tripId);
        preferences.edit().putStringSet(KEY_HIDDEN_TRIP_IDS, hidden).apply();
    }

    public void restoreHistoryTrip(String tripId) {
        Set<String> hidden = getHiddenTripIds();
        hidden.remove(tripId);
        preferences.edit().putStringSet(KEY_HIDDEN_TRIP_IDS, hidden).apply();
    }

    public Set<String> getHiddenTripIds() {
        return new HashSet<>(preferences.getStringSet(KEY_HIDDEN_TRIP_IDS, new HashSet<>()));
    }

    public void clearSession() {
        preferences.edit().clear().commit();
    }

    // --- Ajustes de Colores / Temas ---
    private static final String KEY_THEME_PRIMARY_LIGHT = "theme_primary_light";
    private static final String KEY_THEME_SECONDARY_LIGHT = "theme_secondary_light";
    private static final String KEY_THEME_TEXT_LIGHT = "theme_text_light";
    private static final String KEY_THEME_BG_LIGHT = "theme_bg_light";
    private static final String KEY_THEME_CARD_LIGHT = "theme_card_light";
    private static final String KEY_THEME_BORDER_LIGHT = "theme_border_light";
    private static final String KEY_THEME_PRIMARY_DARK = "theme_primary_dark";
    private static final String KEY_THEME_SECONDARY_DARK = "theme_secondary_dark";
    private static final String KEY_THEME_TEXT_DARK = "theme_text_dark";
    private static final String KEY_THEME_BG_DARK = "theme_bg_dark";
    private static final String KEY_THEME_CARD_DARK = "theme_card_dark";
    private static final String KEY_THEME_BORDER_DARK = "theme_border_dark";

    public void saveThemeColors(String pl, String sl, String pd, String sd) {
        saveThemeColors(pl, sl, "#111827", "#FFFFFF", "#F5F5F5", "#9CA8B0", pd, sd, "#ffffff", "#121011", "#251a1e", "#6E1E3F");
    }

    public void saveThemeColors(String pl, String sl, String tl, String pd, String sd, String td) {
        saveThemeColors(pl, sl, tl, "#FFFFFF", "#F5F5F5", "#9CA8B0", pd, sd, td, "#121011", "#251a1e", "#6E1E3F");
    }

    public void saveThemeColors(String pl, String sl, String tl, String bgl, String cl, String bdl,
                                String pd, String sd, String td, String bgd, String cd, String bdd) {
        preferences.edit()
                .putString(KEY_THEME_PRIMARY_LIGHT, pl)
                .putString(KEY_THEME_SECONDARY_LIGHT, sl)
                .putString(KEY_THEME_TEXT_LIGHT, tl)
                .putString(KEY_THEME_BG_LIGHT, bgl)
                .putString(KEY_THEME_CARD_LIGHT, cl)
                .putString(KEY_THEME_BORDER_LIGHT, bdl)
                .putString(KEY_THEME_PRIMARY_DARK, pd)
                .putString(KEY_THEME_SECONDARY_DARK, sd)
                .putString(KEY_THEME_TEXT_DARK, td)
                .putString(KEY_THEME_BG_DARK, bgd)
                .putString(KEY_THEME_CARD_DARK, cd)
                .putString(KEY_THEME_BORDER_DARK, bdd)
                .apply();
    }

    public String getThemePrimaryLight() {
        return preferences.getString(KEY_THEME_PRIMARY_LIGHT, "#82254B");
    }

    public String getThemeSecondaryLight() {
        return preferences.getString(KEY_THEME_SECONDARY_LIGHT, "#6E1E3F");
    }

    public String getThemeTextLight() {
        return preferences.getString(KEY_THEME_TEXT_LIGHT, "#111827");
    }

    public String getThemeBgLight() {
        return preferences.getString(KEY_THEME_BG_LIGHT, "#FFFFFF");
    }

    public String getThemeCardLight() {
        return preferences.getString(KEY_THEME_CARD_LIGHT, "#F5F5F5");
    }

    public String getThemeBorderLight() {
        return preferences.getString(KEY_THEME_BORDER_LIGHT, "#9CA8B0");
    }

    public String getThemePrimaryDark() {
        return preferences.getString(KEY_THEME_PRIMARY_DARK, "#82254B");
    }

    public String getThemeSecondaryDark() {
        return preferences.getString(KEY_THEME_SECONDARY_DARK, "#6E1E3F");
    }

    public String getThemeTextDark() {
        return preferences.getString(KEY_THEME_TEXT_DARK, "#ffffff");
    }

    public String getThemeBgDark() {
        return preferences.getString(KEY_THEME_BG_DARK, "#121011");
    }

    public String getThemeCardDark() {
        return preferences.getString(KEY_THEME_CARD_DARK, "#251a1e");
    }

    public String getThemeBorderDark() {
        return preferences.getString(KEY_THEME_BORDER_DARK, "#6E1E3F");
    }

    private static final String KEY_FCM_TOKEN = "fcm_token";

    public void saveFcmToken(String token) {
        preferences.edit().putString(KEY_FCM_TOKEN, token).apply();
    }

    public String getFcmToken() {
        return preferences.getString(KEY_FCM_TOKEN, "");
    }
}
