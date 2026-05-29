package com.example.proyectocarpooling.presentation;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Apply programmatic custom theme styling recursively after views are fully inflated!
        DynamicThemeManager.applyTheme(this);
    }
}
