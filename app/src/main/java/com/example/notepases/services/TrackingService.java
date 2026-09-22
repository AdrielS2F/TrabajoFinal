package com.example.notepases.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.example.notepases.activities.AlertActivity;
import com.example.notepases.activities.MainActivity;
import com.example.notepases.utils.LocationUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

public class TrackingService extends Service {

    public static final String ACTION_STOP_SERVICE = "com.example.notepases.STOP_SERVICE";
    private static final String CHANNEL_ID = "NoTePases_Channel";
    private static final int NOTIFICATION_ID = 1001;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private double destLat;
    private double destLng;
    private int alertRadius = 300;

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP_SERVICE.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }

        if (intent != null) {
            // Actualizar datos de destino y radio
            if (intent.hasExtra("DEST_LAT") && intent.hasExtra("DEST_LNG")) {
                destLat = intent.getDoubleExtra("DEST_LAT", 0.0);
                destLng = intent.getDoubleExtra("DEST_LNG", 0.0);
            }
            if (intent.hasExtra("RADIUS_METERS")) {
                alertRadius = intent.getIntExtra("RADIUS_METERS", 300);
            }

            // EVALUAR UBICACIÓN SIMULADA
            if (intent.hasExtra("SIMULATED_LAT") && intent.hasExtra("SIMULATED_LNG")) {
                double simLat = intent.getDoubleExtra("SIMULATED_LAT", 0.0);
                double simLng = intent.getDoubleExtra("SIMULATED_LNG", 0.0);

                float distance = LocationUtils.calculateDistanceInMeters(simLat, simLng, destLat, destLng);

                updateNotification("Distancia al destino: " + (int) distance + " m");

                // Si ingresó al radio de alerta, disparar alarma y detener servicio
                if (distance <= alertRadius) {
                    triggerArrivalAlert();
                    stopSelf();
                }
                return START_STICKY;
            }
        }

        Notification notification = buildNotification("Monitoreando tu viaje...");
        startForeground(NOTIFICATION_ID, notification);
        startLocationUpdates();

        return START_STICKY;
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(3000)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;

                for (android.location.Location location : locationResult.getLocations()) {
                    float currentDistance = LocationUtils.calculateDistanceInMeters(
                            location.getLatitude(),
                            location.getLongitude(),
                            destLat,
                            destLng
                    );

                    updateNotification("Distancia restante: " + (int) currentDistance + " m");

                    if (currentDistance <= alertRadius) {
                        triggerArrivalAlert();
                        stopSelf();
                        break;
                    }
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    private void triggerArrivalAlert() {
        // Opcional: Envío de SMS si está implementado SmsUtils
        try {
            String emergencyContact = "1122334455";
            String message = "NoTePases: Estoy ingresando a mi radio de destino. ¡Llegando!";
            com.example.notepases.utils.SmsUtils.sendArrivalSms(emergencyContact, message);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Abrir la pantalla de alerta
        Intent alertIntent = new Intent(this, AlertActivity.class);
        alertIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(alertIntent);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Monitoreo de Viaje - NoTePases",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification(String contentText) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("NoTePases en ejecución")
                .setContentText(contentText)
                .setSmallIcon(android.R.drawable.ic_dialog_map)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    private void updateNotification(String contentText) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, buildNotification(contentText));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}