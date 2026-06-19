package com.example.proyectocarpooling.presentation;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.proyectocarpooling.R;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
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
            return "No pudimos completar la accion. Intentalo de nuevo.";
        }
        String trimmed = rawError.trim();
        String lower = trimmed.toLowerCase(java.util.Locale.US);

        if (lower.contains("401") || lower.contains("unauthorized")) {
            return "Tu sesion expiro. Vuelve a iniciar sesion.";
        }
        if (lower.contains("403") || lower.contains("forbidden")) {
            return "No tienes permiso para realizar esta accion.";
        }
        if (lower.contains("400") || lower.contains("bad request") || lower.contains("unexpected response")) {
            return "Revisa los datos ingresados e intentalo nuevamente.";
        }
        if (lower.contains("404") || lower.contains("not found")) {
            return "No encontramos la informacion solicitada. Actualiza e intentalo de nuevo.";
        }
        if (lower.contains("500") || lower.contains("internal server") || lower.contains("server returned")) {
            return "El servidor no pudo procesar la solicitud. Intentalo mas tarde.";
        }
        if (lower.contains("fetch") || lower.contains("connect") || lower.contains("network")
                || lower.contains("timeout") || lower.contains("socket") || lower.contains("unable to resolve host")
                || lower.contains("http ") || lower.startsWith("http") || lower.contains("connection")) {
            return "No se pudo conectar con el servidor. Verifica tu conexion e intentalo de nuevo.";
        }
        if (lower.contains("json") || lower.contains("respuesta invalida") || lower.contains("respuesta vacia")) {
            return "Recibimos una respuesta inesperada. Intentalo nuevamente.";
        }
        if (trimmed.length() > 160) {
            return "No pudimos completar la accion. Intentalo de nuevo.";
        }
        return trimmed;
    }
}
