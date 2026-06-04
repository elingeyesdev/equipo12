package com.example.proyectocarpooling.presentation;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.proyectocarpooling.R;

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

    public void setErrorState(android.widget.EditText editText, boolean hasError, String errorMessage) {
        if (editText == null) return;
        if (hasError) {
            editText.setBackgroundResource(R.drawable.bg_input_error);
            editText.setError(errorMessage);
        } else {
            editText.setBackgroundResource(R.drawable.bg_input_modern);
            editText.setError(null);
        }
    }

    public void setupErrorClearer(final android.widget.EditText editText) {
        if (editText == null) return;
        editText.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                editText.setBackgroundResource(R.drawable.bg_input_modern);
                editText.setError(null);
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }

    public String sanitizeError(String rawError) {
        if (rawError == null || rawError.trim().isEmpty()) {
            return "Ha ocurrido un error inesperado. Por favor, inténtalo de nuevo.";
        }
        String lower = rawError.toLowerCase();
        if (lower.contains("fetch") || lower.contains("connect") || lower.contains("network") || 
            lower.contains("timeout") || lower.contains("socket") || lower.contains("unable to resolve host") ||
            lower.contains("http") || lower.contains("connection")) {
            return "No se pudo establecer conexión con el servidor. Por favor, verifica tu conexión a internet.";
        }
        return rawError;
    }
}
