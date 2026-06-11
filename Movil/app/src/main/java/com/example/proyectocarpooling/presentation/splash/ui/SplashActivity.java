package com.example.proyectocarpooling.presentation.splash.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import com.example.proyectocarpooling.presentation.BaseActivity;

import com.example.proyectocarpooling.CarPoolingApplication;
import com.example.proyectocarpooling.R;
import com.example.proyectocarpooling.data.local.SessionManager;
import com.example.proyectocarpooling.presentation.auth.ui.LoginActivity;
import com.example.proyectocarpooling.presentation.main.ui.MainActivity;

public class SplashActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        CarPoolingApplication app = (CarPoolingApplication) getApplication();
        SessionManager sessionManager = app.getSessionManager();

        // Fetch theme colors asynchronously on startup
        String apiBaseUrl = com.example.proyectocarpooling.data.local.ApiBaseUrlProvider.get(this);
        if (apiBaseUrl != null && !apiBaseUrl.isEmpty()) {
            app.getTaskRunner().run(() -> {
                try {
                    okhttp3.OkHttpClient client = new okhttp3.OkHttpClient.Builder()
                            .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                            .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                            .build();

                    okhttp3.Request request = new okhttp3.Request.Builder()
                            .url(apiBaseUrl.replaceAll("/$", "") + "/api/settings/theme")
                            .get()
                            .build();

                    try (okhttp3.Response response = client.newCall(request).execute()) {
                        if (response.isSuccessful() && response.body() != null) {
                            String body = response.body().string();
                            org.json.JSONObject colorsJson = new org.json.JSONObject(body);
                            String pl = colorsJson.optString("primaryLight", "#82254B");
                            String sl = colorsJson.optString("secondaryLight", "#6E1E3F");
                            String tl = colorsJson.optString("textLight", "#111827");
                            String bgl = colorsJson.optString("bgLight", "#FFFFFF");
                            String cl = colorsJson.optString("cardLight", "#F5F5F5");
                            String bdl = colorsJson.optString("borderLight", "#9CA8B0");
                            String pd = colorsJson.optString("primaryDark", "#82254B");
                            String sd = colorsJson.optString("secondaryDark", "#6E1E3F");
                            String td = colorsJson.optString("textDark", "#ffffff");
                            String bgd = colorsJson.optString("bgDark", "#121011");
                            String cd = colorsJson.optString("cardDark", "#251a1e");
                            String bdd = colorsJson.optString("borderDark", "#6E1E3F");
                            
                            sessionManager.saveThemeColors(pl, sl, tl, bgl, cl, bdl, pd, sd, td, bgd, cd, bdd);
                        }
                    }
                } catch (Exception e) {
                    android.util.Log.w("SplashActivity", "No se pudo sincronizar el tema desde el servidor: " + e.getMessage());
                }
            }, new com.example.proyectocarpooling.BackgroundTaskRunner.SimpleCallback() {
                @Override
                public void onSuccess() {}

                @Override
                public void onError(String message) {}
            });
        }

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent;
            if (sessionManager.hasActiveSession()) {
                intent = new Intent(SplashActivity.this, MainActivity.class);
                if (getIntent() != null && getIntent().getExtras() != null) {
                    intent.putExtras(getIntent().getExtras());
                }
            } else {
                intent = new Intent(SplashActivity.this, LoginActivity.class);
            }
            startActivity(intent);
            finish();
        }, 5000);
    }
}
