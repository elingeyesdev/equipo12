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

    public void loadBase64Image(String base64Str, android.widget.ImageView imageView, android.view.View placeholderView) {
        if (base64Str == null || base64Str.trim().isEmpty()) {
            imageView.setVisibility(android.view.View.GONE);
            if (placeholderView != null) placeholderView.setVisibility(android.view.View.VISIBLE);
            return;
        }
        try {
            if (base64Str.contains(",")) {
                base64Str = base64Str.substring(base64Str.indexOf(",") + 1);
            }
            byte[] decodedString = android.util.Base64.decode(base64Str, android.util.Base64.DEFAULT);
            android.graphics.Bitmap decodedByte = android.graphics.BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            if (decodedByte != null) {
                imageView.setImageBitmap(decodedByte);
                imageView.setVisibility(android.view.View.VISIBLE);
                if (placeholderView != null) placeholderView.setVisibility(android.view.View.GONE);
            } else {
                imageView.setVisibility(android.view.View.GONE);
                if (placeholderView != null) placeholderView.setVisibility(android.view.View.VISIBLE);
            }
        } catch (Exception e) {
            e.printStackTrace();
            imageView.setVisibility(android.view.View.GONE);
            if (placeholderView != null) placeholderView.setVisibility(android.view.View.VISIBLE);
        }
    }
}
