package com.example.notepases.activities;

import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.widget.Button;
import android.app.KeyguardManager;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.example.notepases.R;
import com.example.notepases.services.TrackingService;

public class AlertActivity extends AppCompatActivity {

    private Ringtone ringtone;
    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Configuración para encender la pantalla sobre el bloqueo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (keyguardManager != null) {
                keyguardManager.requestDismissKeyguard(this, null);
            }
        } else {
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            );
        }

        setContentView(R.layout.activity_alert);

        Button btnStopAlert = findViewById(R.id.btnStopAlert);

        // 1. Iniciar reproducción de audio por defecto
        try {
            Uri alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alertUri == null) {
                alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
            ringtone = RingtoneManager.getRingtone(getApplicationContext(), alertUri);
            if (ringtone != null) {
                ringtone.play();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 2. Iniciar vibración continua como respaldo
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            long[] pattern = {0, 1000, 500}; // Espera 0ms, vibra 1000ms, pausa 500ms
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
            } else {
                vibrator.vibrate(pattern, 0);
            }
        }

        // 3. Detener la alarma y matar el servicio en segundo plano
        btnStopAlert.setOnClickListener(v -> stopAlertAndService());
    }
    private void stopAlertAndService() {
        // Detener sonido
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
        }

        // Detener vibración
        if (vibrator != null) {
            vibrator.cancel();
        }

        // Enviar Intent explicito para detener el TrackingService
        Intent muteIntent = new Intent(this, TrackingService.class);
        muteIntent.setAction("ACTION_MUTE_ALERT");
        startService(muteIntent);

        finish(); // Cerrar la Activity
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Garantizar detención si la Activity es destruida externamente
        stopAlertAndService();
    }
}