package com.example.proyectocarpooling.presentation.splash.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proyectocarpooling.R;
import com.example.proyectocarpooling.data.local.SessionManager;
import com.example.proyectocarpooling.presentation.auth.ui.LoginActivity;
import com.example.proyectocarpooling.presentation.main.ui.MainActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        SessionManager sessionManager = new SessionManager(this);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = sessionManager.hasActiveSession()
                    ? new Intent(SplashActivity.this, MainActivity.class)
                    : new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }, 2000);
    }
}