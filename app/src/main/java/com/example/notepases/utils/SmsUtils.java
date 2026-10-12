package com.example.notepases.utils;

import android.content.Context;
import android.os.Build;
import android.telephony.SmsManager;
import android.util.Log;

public class SmsUtils {

    /**
     * Envía un mensaje de texto SMS en segundo plano cuando se alcanza el radio de alerta (RF 5).
     */
    public static boolean sendArrivalSms(Context context, String phoneNumber, String message) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }

        try {
            SmsManager smsManager;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && context != null) {
                smsManager = context.getSystemService(SmsManager.class);
            } else {
                smsManager = SmsManager.getDefault();
            }

            String cleanNumber = phoneNumber.replaceAll("[^0-9+]", "");
            smsManager.sendTextMessage(cleanNumber, null, message, null, null);
            return true;
        } catch (Exception e) {
            Log.e("SmsUtils", "Error al enviar el SMS: " + e.getMessage());
            return false;
        }
    }
}