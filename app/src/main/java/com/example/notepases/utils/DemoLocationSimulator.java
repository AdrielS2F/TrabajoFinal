package com.example.notepases.utils;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import com.example.notepases.services.TrackingService;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class DemoLocationSimulator {

    public interface OnLocationUpdateListener {
        void onLocationChanged(GeoPoint newPoint);
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable simulationRunnable;
    private boolean isSimulating = false;

    public void startSimulation(Context context, GeoPoint start, GeoPoint end, OnLocationUpdateListener listener) {
        stopSimulation();
        isSimulating = true;

        new Thread(() -> {
            List<GeoPoint> routePoints = fetchRouteFromOSRM(start, end);

            // Si la consulta a OSRM falla o no trae puntos, usamos fallback con más pasos para evitar saltos grandes
            if (routePoints.size() < 2) {
                routePoints = generateLinearFallback(start, end, 30);
            }

            List<GeoPoint> finalPoints = routePoints;
            handler.post(() -> runStepByStep(context, end, finalPoints, listener));
        }).start();
    }

    private void runStepByStep(Context context, GeoPoint destination, List<GeoPoint> points, OnLocationUpdateListener listener) {
        simulationRunnable = new Runnable() {
            int index = 0;

            @Override
            public void run() {
                if (!isSimulating || index >= points.size()) {
                    return;
                }

                GeoPoint currentPoint = points.get(index);

                // 1. Mover el mapa y marcador visual
                listener.onLocationChanged(currentPoint);

                // 2. Enviar posición y destino al TrackingService
                Intent intent = new Intent(context, TrackingService.class);
                intent.putExtra("DEST_LAT", destination.getLatitude());
                intent.putExtra("DEST_LNG", destination.getLongitude());
                intent.putExtra("SIMULATED_LAT", currentPoint.getLatitude());
                intent.putExtra("SIMULATED_LNG", currentPoint.getLongitude());

                context.startService(intent);

                index++;

                if (index < points.size()) {
                    handler.postDelayed(this, 700); // Avanza fluido cada 700 ms
                } else {
                    stopSimulation();
                }
            }
        };

        handler.post(simulationRunnable);
    }

    private List<GeoPoint> fetchRouteFromOSRM(GeoPoint start, GeoPoint end) {
        List<GeoPoint> route = new ArrayList<>();
        try {
            String urlStr = "https://router.project-osrm.org/route/v1/driving/" +
                    start.getLongitude() + "," + start.getLatitude() + ";" +
                    end.getLongitude() + "," + end.getLatitude() +
                    "?overview=full&geometries=geojson";

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "NoTePasesApp/1.0 (contacto@notepases.com)");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);

            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONObject json = new JSONObject(sb.toString());
                JSONArray coordinates = json.getJSONArray("routes")
                        .getJSONObject(0)
                        .getJSONObject("geometry")
                        .getJSONArray("coordinates");

                for (int i = 0; i < coordinates.length(); i++) {
                    JSONArray point = coordinates.getJSONArray(i);
                    route.add(new GeoPoint(point.getDouble(1), point.getDouble(0)));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return route;
    }

    private List<GeoPoint> generateLinearFallback(GeoPoint start, GeoPoint end, int steps) {
        List<GeoPoint> points = new ArrayList<>();
        double latStep = (end.getLatitude() - start.getLatitude()) / steps;
        double lngStep = (end.getLongitude() - start.getLongitude()) / steps;

        for (int i = 0; i <= steps; i++) {
            points.add(new GeoPoint(
                    start.getLatitude() + (latStep * i),
                    start.getLongitude() + (lngStep * i)
            ));
        }
        return points;
    }

    public void stopSimulation() {
        isSimulating = false;
        if (simulationRunnable != null) {
            handler.removeCallbacks(simulationRunnable);
        }
    }
}