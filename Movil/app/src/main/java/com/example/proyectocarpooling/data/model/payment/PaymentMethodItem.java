package com.example.proyectocarpooling.data.model.payment;

import org.json.JSONObject;

public class PaymentMethodItem {
    public final int id;
    public final String code;
    public final String name;
    public final String description;
    public final int type;
    public final boolean requiresManualConfirmation;
    public final boolean supportsRefunds;

    public PaymentMethodItem(int id, String code, String name, String description,
                             int type, boolean requiresManualConfirmation, boolean supportsRefunds) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.description = description;
        this.type = type;
        this.requiresManualConfirmation = requiresManualConfirmation;
        this.supportsRefunds = supportsRefunds;
    }

    public static PaymentMethodItem fromJson(JSONObject obj) {
        return new PaymentMethodItem(
                obj.optInt("id", 0),
                obj.optString("code", ""),
                obj.optString("name", ""),
                obj.optString("description", ""),
                obj.optInt("type", 0),
                obj.optBoolean("requiresManualConfirmation", false),
                obj.optBoolean("supportsRefunds", true)
        );
    }

    public boolean isCash() {
        return "CASH".equalsIgnoreCase(code);
    }

    public boolean isQr() {
        return "QR_BANK".equalsIgnoreCase(code);
    }

    public boolean isSimulatedCard() {
        return "CARD_SIM".equalsIgnoreCase(code);
    }
}
