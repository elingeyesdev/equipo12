package com.example.proyectocarpooling.data.model.payment;

import org.json.JSONObject;

public class UserPaymentMethodItem {
    public final String id;
    public final String userId;
    public final int paymentMethodId;
    public final String paymentMethodCode;
    public final String paymentMethodName;
    public final String alias;
    public final String maskedValue;
    public final String qrImageUrl;
    public final String bankName;
    public final String accountHolderName;
    public final boolean isDefault;

    public UserPaymentMethodItem(String id, String userId, int paymentMethodId,
                                 String paymentMethodCode, String paymentMethodName,
                                 String alias, String maskedValue, String qrImageUrl,
                                 String bankName, String accountHolderName, boolean isDefault) {
        this.id = id;
        this.userId = userId;
        this.paymentMethodId = paymentMethodId;
        this.paymentMethodCode = paymentMethodCode;
        this.paymentMethodName = paymentMethodName;
        this.alias = alias;
        this.maskedValue = maskedValue;
        this.qrImageUrl = qrImageUrl;
        this.bankName = bankName;
        this.accountHolderName = accountHolderName;
        this.isDefault = isDefault;
    }

    public static UserPaymentMethodItem fromJson(JSONObject obj) {
        return new UserPaymentMethodItem(
                obj.optString("id", ""),
                obj.optString("userId", ""),
                obj.optInt("paymentMethodId", 0),
                obj.optString("paymentMethodCode", ""),
                obj.optString("paymentMethodName", ""),
                obj.optString("alias", ""),
                obj.optString("maskedValue", ""),
                obj.optString("qrImageUrl", ""),
                obj.optString("bankName", ""),
                obj.optString("accountHolderName", ""),
                obj.optBoolean("isDefault", false)
        );
    }
}
