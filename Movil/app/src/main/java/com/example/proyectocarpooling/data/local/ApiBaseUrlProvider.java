package com.example.proyectocarpooling.data.local;

import android.content.Context;
import android.os.Build;

import com.example.proyectocarpooling.R;

public final class ApiBaseUrlProvider {

    private ApiBaseUrlProvider() {
    }

    public static String get(Context context) {
        if (isEmulator()) {
            return context.getString(R.string.api_base_url_emulator);
        }
        return context.getString(R.string.api_base_url_device);
    }

    private static boolean isEmulator() {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")
                || "google_sdk".equals(Build.PRODUCT);
    }
}
