package com.example.notepases.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.notepases.R;
import com.example.notepases.services.TrackingService;
import com.example.notepases.utils.DemoLocationSimulator;
import com.example.notepases.utils.LocationUtils;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1001;

    private MapView mapView;
    private EditText etOrigin, etDestination;
    private Button btnSearch, btnStartTracking;
    private SeekBar sbRadius;
    private TextView tvRadiusLabel;

    private GeoPoint originPoint;
    private GeoPoint destinationPoint;

    private int selectedRadius = 300;
    private final int MIN_RADIUS = 120;

    private Marker originMarker;
    private Marker destinationMarker;
    private Polygon radiusCircle;

    private DemoLocationSimulator simulator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().setUserAgentValue("NoTePasesApp/1.0 (contacto@notepases.com)");
        Configuration.getInstance().load(getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));

        setContentView(R.layout.activity_main);

        // Vistas
        mapView = findViewById(R.id.mapView);
        etOrigin = findViewById(R.id.etOrigin);
        etDestination = findViewById(R.id.etDestination);
        btnSearch = findViewById(R.id.btnSearch);
        btnStartTracking = findViewById(R.id.btnStartTracking);
        sbRadius = findViewById(R.id.sbRadius);
        tvRadiusLabel = findViewById(R.id.tvRadiusLabel);

        Button btnZoomIn = findViewById(R.id.btnZoomIn);
        Button btnZoomOut = findViewById(R.id.btnZoomOut);

        simulator = new DemoLocationSimulator();

        // Configuración del mapa osmdroid
        mapView.setTileSource(new org.osmdroid.tileprovider.tilesource.XYTileSource(
                "OSM_Hot", 0, 19, 256, ".png",
                new String[]{"https://a.tile.openstreetmap.fr/hot/", "https://b.tile.openstreetmap.fr/hot/"}));

        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(14.0);

        // Ubicación inicial por defecto (Obelisco)
        GeoPoint startPoint = new GeoPoint(-34.6037, -58.3816);
        mapView.getController().setCenter(startPoint);

        // Botones de Zoom
        btnZoomIn.setOnClickListener(v -> mapView.getController().zoomIn());
        btnZoomOut.setOnClickListener(v -> mapView.getController().zoomOut());

        checkPermissions();

        // Configurar ícono de limpiar texto (cruz) en Origen y Destino
        setupClearButton(etOrigin);
        setupClearButton(etDestination);

        // Listeners principales
        btnSearch.setOnClickListener(v -> searchLocations());
        btnStartTracking.setOnClickListener(v -> startTrackingService());

        // Control de Slider de Radio
        sbRadius.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                selectedRadius = MIN_RADIUS + progress;
                selectedRadius = LocationUtils.validateRadius(selectedRadius);
                tvRadiusLabel.setText("Radio de Alerta: " + selectedRadius + " m");
                updateMapGraphics();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }


    @android.annotation.SuppressLint("ClickableViewAccessibility")
    private void setupClearButton(EditText editText) {
        editText.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                Drawable drawableEnd = editText.getCompoundDrawables()[2];
                if (drawableEnd != null) {
                    int drawableWidth = drawableEnd.getBounds().width();
                    if (event.getRawX() >= (editText.getRight() - drawableWidth - editText.getPaddingEnd())) {

                        // 1. Limpiar el texto de la caja
                        editText.setText("");

                        // 2. Limpiar el punto y el marcador correspondiente en el mapa
                        if (editText.getId() == R.id.etOrigin) {
                            originPoint = null;
                            if (originMarker != null) {
                                mapView.getOverlays().remove(originMarker);
                                originMarker = null;
                            }
                        } else if (editText.getId() == R.id.etDestination) {
                            destinationPoint = null;
                            if (destinationMarker != null) {
                                mapView.getOverlays().remove(destinationMarker);
                                destinationMarker = null;
                            }
                            if (radiusCircle != null) {
                                mapView.getOverlays().remove(radiusCircle);
                                radiusCircle = null;
                            }
                        }

                        // 3. Detener la simulación en curso si existía
                        if (simulator != null) {
                            simulator.stopSimulation();
                        }

                        // 4. Redibujar el mapa
                        mapView.invalidate();

                        return true;
                    }
                }
            }
            return false;
        });
    }

    private void searchLocations() {
        String originText = etOrigin.getText().toString().trim();
        String destText = etDestination.getText().toString().trim();

        if (destText.isEmpty()) {
            Toast.makeText(this, "Por favor ingresá un Destino", Toast.LENGTH_SHORT).show();
            return;
        }

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            // 1. Geolocalizar Destino
            List<Address> destAddresses = geocoder.getFromLocationName(destText, 1);
            if (destAddresses != null && !destAddresses.isEmpty()) {
                Address destAddr = destAddresses.get(0);
                destinationPoint = new GeoPoint(destAddr.getLatitude(), destAddr.getLongitude());
            } else {
                Toast.makeText(this, "No se encontró el Destino", Toast.LENGTH_SHORT).show();
                return;
            }

            // 2. Geolocalizar Origen
            if (!originText.isEmpty()) {
                List<Address> originAddresses = geocoder.getFromLocationName(originText, 1);
                if (originAddresses != null && !originAddresses.isEmpty()) {
                    Address origAddr = originAddresses.get(0);
                    originPoint = new GeoPoint(origAddr.getLatitude(), origAddr.getLongitude());
                } else {
                    originPoint = (GeoPoint) mapView.getMapCenter();
                }
            } else {
                originPoint = (GeoPoint) mapView.getMapCenter();
            }

            mapView.getController().animateTo(originPoint);
            mapView.getController().setZoom(14.0);

            updateMapGraphics();
            Toast.makeText(this, "Ruta cargada correctamente", Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            Toast.makeText(this, "Error de conexión en la búsqueda", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateMapGraphics() {
        mapView.getOverlays().clear();

        // Marcador Origen con ícono personalizado
        if (originPoint != null) {
            originMarker = new Marker(mapView);
            originMarker.setPosition(originPoint);
            originMarker.setTitle("Origen");
            originMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

            // Asignar el ícono personalizado de Origen desde drawable
            originMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_origin_pin));

            mapView.getOverlays().add(originMarker);
        }

        // Marcador Destino + Círculo de Alerta Amarillo
        if (destinationPoint != null) {
            destinationMarker = new Marker(mapView);
            destinationMarker.setPosition(destinationPoint);
            destinationMarker.setTitle("Destino de Alerta");
            destinationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            destinationMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_location_pin));
            mapView.getOverlays().add(destinationMarker);

            radiusCircle = new Polygon();
            radiusCircle.setPoints(Polygon.pointsAsCircle(destinationPoint, (double) selectedRadius));
            radiusCircle.getFillPaint().setColor(0x32FFD700);
            radiusCircle.getOutlinePaint().setColor(0xFFFFD700);
            radiusCircle.getOutlinePaint().setStrokeWidth(3.0f);
            mapView.getOverlays().add(radiusCircle);
        }

        mapView.invalidate();
    }

    private void startTrackingService() {
        if (destinationPoint == null) {
            Toast.makeText(this, "Primero buscá un destino", Toast.LENGTH_SHORT).show();
            return;
        }

        if (originPoint == null) {
            originPoint = (GeoPoint) mapView.getMapCenter();
        }

        // 1. Iniciar servicio
        Intent serviceIntent = new Intent(this, TrackingService.class);
        serviceIntent.putExtra("DEST_LAT", destinationPoint.getLatitude());
        serviceIntent.putExtra("DEST_LNG", destinationPoint.getLongitude());
        serviceIntent.putExtra("RADIUS_METERS", selectedRadius);

        ContextCompat.startForegroundService(this, serviceIntent);

        Toast.makeText(this, "Obteniendo ruta e iniciando monitoreo...", Toast.LENGTH_SHORT).show();

        // 2. Iniciar simulación por calles
        simulator.startSimulation(this, originPoint, destinationPoint, newPoint -> {
            mapView.getController().animateTo(newPoint);
            if (originMarker != null) {
                originMarker.setPosition(newPoint);
            }
            mapView.invalidate();
        });
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_SCROLL) {
            float scroll = event.getAxisValue(MotionEvent.AXIS_VSCROLL);
            if (scroll > 0) {
                mapView.getController().zoomIn();
                return true;
            } else if (scroll < 0) {
                mapView.getController().zoomOut();
                return true;
            }
        }
        return super.onGenericMotionEvent(event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }
}