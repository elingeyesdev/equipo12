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
        String fingerprint = Build.FINGERPRINT == null ? "" : Build.FINGERPRINT.toLowerCase();
        String model = Build.MODEL == null ? "" : Build.MODEL.toLowerCase();
        String manufacturer = Build.MANUFACTURER == null ? "" : Build.MANUFACTURER.toLowerCase();
        String brand = Build.BRAND == null ? "" : Build.BRAND.toLowerCase();
        String device = Build.DEVICE == null ? "" : Build.DEVICE.toLowerCase();
        String product = Build.PRODUCT == null ? "" : Build.PRODUCT.toLowerCase();
        String hardware = Build.HARDWARE == null ? "" : Build.HARDWARE.toLowerCase();
        String board = Build.BOARD == null ? "" : Build.BOARD.toLowerCase();

        return fingerprint.startsWith("generic")
            || fingerprint.startsWith("unknown")
            || fingerprint.contains("emulator")
            || model.contains("google_sdk")
            || model.contains("emulator")
            || model.contains("android sdk built for")
            || manufacturer.contains("genymotion")
            || (brand.startsWith("generic") && device.startsWith("generic"))
            || product.contains("sdk")
            || product.contains("emulator")
            || hardware.contains("goldfish")
            || hardware.contains("ranchu")
            || board.contains("goldfish");
    }
}
