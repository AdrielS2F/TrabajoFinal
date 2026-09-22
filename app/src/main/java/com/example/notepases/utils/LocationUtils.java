package com.example.notepases.utils;

import android.location.Location;

public class LocationUtils {

    /**
     * Calcula la distancia en metros entre dos puntos geográficos (Latitud y Longitud)
     * utilizando la API nativa de Android (Fórmula de Haversine).
     */
    public static float calculateDistanceInMeters(double startLat, double startLng, double endLat, double endLng) {
        float[] results = new float[1];
        Location.distanceBetween(startLat, startLng, endLat, endLng, results);
        return results[0];
    }

    /**
     * Valida que el radio mínimo sea respetado (Manejo de errores del RF 2).
     * Si es menor a 50 metros, reajusta al límite inferior válido.
     */
    public static int validateRadius(int inputRadius) {
        int MIN_RADIUS = 50;
        if (inputRadius < MIN_RADIUS) {
            return MIN_RADIUS;
        }
        return inputRadius;
    }
}