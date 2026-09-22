package com.example.notepases.utils;

import android.telephony.SmsManager;
import android.util.Log;

public class SmsUtils {

    /**
     * Envía un mensaje de texto SMS en segundo plano cuando se alcanza el radio de alerta (RF 5).
     */
    public static boolean sendArrivalSms(String phoneNumber, String message) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }

        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            return true;
        } catch (Exception e) {
            Log.e("SmsUtils", "Error al enviar el SMS: " + e.getMessage());
            return false;
        }
    }
}