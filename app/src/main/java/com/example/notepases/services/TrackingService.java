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
    public static final String ACTION_REAL_LOCATION_UPDATE = "com.example.notepases.REAL_LOCATION_UPDATE";
    public static final String EXTRA_LAT = "EXTRA_LAT";
    public static final String EXTRA_LNG = "EXTRA_LNG";


    private static final String CHANNEL_ID = "NoTePases_Channel";
    private static final int NOTIFICATION_ID = 1001;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private double destLat;
    private double destLng;
    private int alertRadius = 300;

    private boolean alertAlreadyTriggered = false;
    private boolean isSimulationMode = true;
    private String contactNumber;


    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            if (intent != null){
                String action = intent.getAction();

                /// SIlencia alarma y detine el monitoreo al llegar
                if ("ACTION_MUTE_ALERT".equals(action)) {
                 alertAlreadyTriggered = true;
                 stopLocationUpdates();
                 stopForeground(true);

                 Intent stopSimIntent = new Intent("ACTION_STOP_SIMULATOR");
                 stopSimIntent.setPackage(getPackageName());
                 sendBroadcast(stopSimIntent);

                 return START_NOT_STICKY;


                }
                /// este if reinicia la bandera para un nuevo viaje
                if ("ACTION_RESET_ALERT".equals(action)) {
                    alertAlreadyTriggered = false;
                    return START_STICKY;

                }
                /// esto es opcional para mas adelante si se quiere detener el servicio de manera manual y forzada
                if(ACTION_STOP_SERVICE.equals(action)){
                    stopLocationUpdates();
                    stopForeground(true);
                    stopSelf();
                    return START_NOT_STICKY;
                }

                // Actualizar datos de destino y radio
                if (intent.hasExtra("DEST_LAT") && intent.hasExtra("DEST_LNG")) {
                    destLat = intent.getDoubleExtra("DEST_LAT", 0.0);
                    destLng = intent.getDoubleExtra("DEST_LNG", 0.0);
                }
                if (intent.hasExtra("RADIUS_METERS")) {
                    alertRadius = intent.getIntExtra("RADIUS_METERS", 300);
                }
                if (intent.hasExtra("CONTACT_NUMBER")){
                    contactNumber = intent.getStringExtra("CONTACT_NUMBER");
                }
                //verifica que modo eligio el usuario en el switch
                if (intent.hasExtra("IS_SIMULATION")){
                    isSimulationMode = intent.getBooleanExtra("IS_SIMULATION", true);
                    alertAlreadyTriggered = false;
                }

                // Recibiendo pasos del simulador
                if (intent.hasExtra("SIMULATED_LAT") && intent.hasExtra("SIMULATED_LNG")) {

                    if (!isSimulationMode || alertAlreadyTriggered) return START_NOT_STICKY; //Ignorar si cambio a mood real

                    double simLat = intent.getDoubleExtra("SIMULATED_LAT", 0.0);
                    double simLng = intent.getDoubleExtra("SIMULATED_LNG", 0.0);

                    float distance = LocationUtils.calculateDistanceInMeters(simLat, simLng, destLat, destLng);
                    updateNotification("[Simulacion]Distancia al destino: " + (int) distance + " m");

                    // Si ingresó al radio de alerta, disparar alarma y detener servicio
                    if (distance <= alertRadius && !alertAlreadyTriggered) {
                        alertAlreadyTriggered = true;
                        triggerArrivalAlert();
                    }
                    return START_STICKY;
                }
                Notification notification  = buildNotification(
                        isSimulationMode ? "Iniciando simulacion..." : "Esperando señal del gps Real..."
                );
                startForeground(NOTIFICATION_ID, notification);

                if (isSimulationMode){
                    stopLocationUpdates();
                } else {
                    startLocationUpdates();
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return START_STICKY;
    }



    private void startLocationUpdates() {
        stopLocationUpdates();

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 4000)
                .setMinUpdateIntervalMillis(2000)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null || isSimulationMode) return;

                for (android.location.Location location : locationResult.getLocations()) {
                    float currentDistance = LocationUtils.calculateDistanceInMeters(
                            location.getLatitude(),
                            location.getLongitude(),
                            destLat,
                            destLng
                    );
                    Intent broadcastIntent = new Intent(ACTION_REAL_LOCATION_UPDATE);
                    broadcastIntent.setPackage(getPackageName());
                    broadcastIntent.putExtra(EXTRA_LAT, location.getLatitude());
                    broadcastIntent.putExtra(EXTRA_LNG, location.getLongitude());
                    sendBroadcast(broadcastIntent);

                    updateNotification("[gpsreal] Distancia restante: " + (int) currentDistance + " m");

                    if (currentDistance <= alertRadius && !alertAlreadyTriggered) {
                        alertAlreadyTriggered = true;
                        triggerArrivalAlert();
                        break;
                    }
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    private void stopLocationUpdates(){
        if (fusedLocationClient != null && locationCallback != null){
            fusedLocationClient.removeLocationUpdates(locationCallback);
            locationCallback = null;
        }
    }

    private void triggerArrivalAlert() {
        // Opcional: Envío de SMS si está implementado SmsUtils
        try {
            if (contactNumber != null && !contactNumber.isEmpty()){
                String message = "NoTePases: Estoy ingresando a mi radio de destino.";
                com.example.notepases.utils.SmsUtils.sendArrivalSms(this, contactNumber, message);

            }
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
        stopLocationUpdates();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}