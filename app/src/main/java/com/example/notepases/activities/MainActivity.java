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
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Build;
import android.widget.ListView;
import android.widget.BaseAdapter;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.notepases.R;
import com.example.notepases.database.DestinosDAO;
import com.example.notepases.models.Destino;
import com.example.notepases.services.TrackingService;
import com.example.notepases.utils.DemoLocationSimulator;
import com.example.notepases.utils.LocationUtils;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1001;

    private MapView mapView;
    private EditText etOrigin, etDestination;
    private Button btnSearch, btnStartTracking, btnContactos, btnDestinosFrecuentes;
    private ImageButton btnSaveFavorite;
    private SeekBar sbRadius;
    private TextView tvRadiusLabel;

    private TextView tvModeLabel, tvToggleMore;
    private Switch switchSimulationMode;
    private LinearLayout layoutToggleMore, layoutExpandableOptions;

    private GeoPoint originPoint;
    private GeoPoint destinationPoint;

    private int selectedRadius = 300;
    private final int MIN_RADIUS = 120;

    private Marker originMarker;
    private Marker destinationMarker;
    private Polygon radiusCircle;

    private DemoLocationSimulator simulator;
    private DestinosDAO destinosDAO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().setUserAgentValue("NoTePasesApp/1.0 (contacto@notepases.com)");
        Configuration.getInstance().load(getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));

        setContentView(R.layout.activity_main);

        // Instanciar DAO de Destinos Frecuentes
        destinosDAO = new DestinosDAO(this);

        // Vistas
        mapView = findViewById(R.id.mapView);
        etOrigin = findViewById(R.id.etOrigin);
        etDestination = findViewById(R.id.etDestination);
        btnSaveFavorite = findViewById(R.id.btnSaveFavorite);
        btnSearch = findViewById(R.id.btnSearch);
        btnContactos = findViewById(R.id.btnContactos);
        btnDestinosFrecuentes = findViewById(R.id.btnDestinosFrecuentes);
        btnStartTracking = findViewById(R.id.btnStartTracking);
        sbRadius = findViewById(R.id.sbRadius);
        tvRadiusLabel = findViewById(R.id.tvRadiusLabel);
        tvModeLabel = findViewById(R.id.tvModeLabel);
        switchSimulationMode = findViewById(R.id.switchSimulationMode);

        // Vistas del Panel Desplegable
        layoutToggleMore = findViewById(R.id.layoutToggleMore);
        layoutExpandableOptions = findViewById(R.id.layoutExpandableOptions);
        tvToggleMore = findViewById(R.id.tvToggleMore);

        // Estado inicial de la estrella: deshabilitada hasta buscar un destino válido
        btnSaveFavorite.setEnabled(false);
        btnSaveFavorite.setAlpha(0.4f);

        switchSimulationMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            tvModeLabel.setText(isChecked ? "Modo Simulacion" : "Modo Real");
        });

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
        btnSaveFavorite.setOnClickListener(v -> mostrarDialogoGuardarDestino());

        btnContactos.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ContactosActivity.class);
            startActivity(intent);
        });

        btnDestinosFrecuentes.setOnClickListener(v -> mostrarListaDestinosFrecuentes());

        // Listener para abrir / cerrar la pestañita desplegable
        layoutToggleMore.setOnClickListener(v -> {
            if (layoutExpandableOptions.getVisibility() == View.GONE) {
                layoutExpandableOptions.setVisibility(View.VISIBLE);
                tvToggleMore.setText("Menos opciones ▲");
            } else {
                layoutExpandableOptions.setVisibility(View.GONE);
                tvToggleMore.setText("Más opciones ▼");
            }
        });

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

                            // Deshabilitar botón estrella al borrar el destino
                            btnSaveFavorite.setEnabled(false);
                            btnSaveFavorite.setAlpha(0.4f);
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

                btnSaveFavorite.setEnabled(true);
                btnSaveFavorite.setAlpha(1.0f);
            } else {
                Toast.makeText(this, "No se encontró el Destino", Toast.LENGTH_SHORT).show();
                btnSaveFavorite.setEnabled(false);
                btnSaveFavorite.setAlpha(0.4f);
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

            updateMapGraphics();

            // 3. AJUSTAR CÁMARA PARA MOSTRAR AMBOS PUNTOS
            ajustarCamaraParaMostrarPuntos(originPoint, destinationPoint);

            Toast.makeText(this, "Ruta cargada correctamente", Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            Toast.makeText(this, "Error de conexión en la búsqueda", Toast.LENGTH_SHORT).show();
        }
    }

    private void ajustarCamaraParaMostrarPuntos(GeoPoint puntoA, GeoPoint puntoB) {
        if (puntoA == null || puntoB == null) return;

        double minLat = Math.min(puntoA.getLatitude(), puntoB.getLatitude());
        double maxLat = Math.max(puntoA.getLatitude(), puntoB.getLatitude());
        double minLng = Math.min(puntoA.getLongitude(), puntoB.getLongitude());
        double maxLng = Math.max(puntoA.getLongitude(), puntoB.getLongitude());

        org.osmdroid.util.BoundingBox boundingBox = new org.osmdroid.util.BoundingBox(
                maxLat, maxLng, minLat, minLng
        );

        int paddingPx = 120;
        mapView.post(() -> mapView.zoomToBoundingBox(boundingBox, true, paddingPx));
    }

    private void mostrarDialogoGuardarDestino() {
        if (destinationPoint == null) {
            Toast.makeText(this, "Buscá una ubicación válida antes de guardar", Toast.LENGTH_SHORT).show();
            return;
        }

        String coordenadasStr = destinationPoint.getLatitude() + "," + destinationPoint.getLongitude();

        if (destinosDAO.existeUbicacionDestino(coordenadasStr)) {
            Toast.makeText(this, "Esta ubicación ya está guardada en tus Favoritos", Toast.LENGTH_LONG).show();
            return;
        }

        String textoDestinoActual = etDestination.getText().toString().trim();

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_save_destination, null);
        EditText etDialogAlias = dialogView.findViewById(R.id.etDialogAlias);
        Button btnCancel = dialogView.findViewById(R.id.btnDialogCancel);
        Button btnSave = dialogView.findViewById(R.id.btnDialogSave);

        etDialogAlias.setText(textoDestinoActual);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String apodo = etDialogAlias.getText().toString().trim();

            if (apodo.length() < 3) {
                etDialogAlias.setError("El nombre debe tener al menos 3 caracteres");
                return;
            }

            if (destinosDAO.existeNombreDestino(apodo)) {
                etDialogAlias.setError("Ya tenés un destino guardado con este nombre");
                return;
            }

            Destino nuevoDestino = new Destino(0, apodo, coordenadasStr, selectedRadius);
            destinosDAO.AgregarDestino(nuevoDestino);

            Toast.makeText(this, "¡Destino '" + apodo + "' guardado en Favoritos!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void mostrarListaDestinosFrecuentes() {
        ArrayList<Destino> lista = destinosDAO.getListadoDestinos();

        if (lista.isEmpty()) {
            Toast.makeText(this, "No tenés destinos guardados aún", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_favoritos_lista, null);
        ListView lvDestinos = dialogView.findViewById(R.id.lvDestinosFavoritos);
        Button btnCerrar = dialogView.findViewById(R.id.btnCerrarFavoritos);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        BaseAdapter adapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return lista.size();
            }

            @Override
            public Object getItem(int position) {
                return lista.get(position);
            }

            @Override
            public long getItemId(int position) {
                return lista.get(position).getId();
            }

            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                if (convertView == null) {
                    convertView = getLayoutInflater().inflate(R.layout.item_destino_frecuente, parent, false);
                }

                Destino destino = lista.get(position);

                TextView tvNombre = convertView.findViewById(R.id.tvDestinoNombre);
                ImageButton btnDelete = convertView.findViewById(R.id.btnDeleteDestino);

                tvNombre.setText(destino.getNombre());

                // Seleccionar destino al hacer clic en el renglón/nombre
                convertView.setOnClickListener(v -> {
                    etDestination.setText(destino.getNombre());

                    String[] coords = destino.getUbicacion().split(",");
                    double lat = Double.parseDouble(coords[0]);
                    double lng = Double.parseDouble(coords[1]);

                    destinationPoint = new GeoPoint(lat, lng);
                    selectedRadius = destino.getRadio();

                    if (selectedRadius >= MIN_RADIUS) {
                        sbRadius.setProgress(selectedRadius - MIN_RADIUS);
                    }
                    tvRadiusLabel.setText("Radio de Alerta: " + selectedRadius + " m");

                    btnSaveFavorite.setEnabled(true);
                    btnSaveFavorite.setAlpha(1.0f);

                    updateMapGraphics();

                    if (originPoint != null) {
                        ajustarCamaraParaMostrarPuntos(originPoint, destinationPoint);
                    } else {
                        mapView.getController().animateTo(destinationPoint);
                        mapView.getController().setZoom(15.0);
                    }

                    Toast.makeText(MainActivity.this, "Cargado: " + destino.getNombre(), Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                });

                // Borrar destino con confirmación previa
                btnDelete.setOnClickListener(v -> {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Confirmar eliminación")
                            .setMessage("¿Estás seguro de que querés eliminar '" + destino.getNombre() + "' de tus favoritos?")
                            .setPositiveButton("ELIMINAR", (confirmDialog, which) -> {
                                destinosDAO.EliminarDestino(destino);
                                lista.remove(position);
                                notifyDataSetChanged();
                                Toast.makeText(MainActivity.this, "Destino eliminado", Toast.LENGTH_SHORT).show();

                                if (lista.isEmpty()) {
                                    dialog.dismiss();
                                }
                            })
                            .setNegativeButton("CANCELAR", null)
                            .show();
                });

                return convertView;
            }
        };

        lvDestinos.setAdapter(adapter);
        btnCerrar.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void updateMapGraphics() {
        mapView.getOverlays().clear();

        // Marcador Origen con ícono personalizado
        if (originPoint != null) {
            originMarker = new Marker(mapView);
            originMarker.setPosition(originPoint);
            originMarker.setTitle("Origen");
            originMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
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

        boolean useSimulation = switchSimulationMode.isChecked();

        if (useSimulation) {
            Toast.makeText(this, "Obteniendo ruta e iniciando monitoreo...", Toast.LENGTH_SHORT).show();

            // 2. Iniciar simulación por calles
            simulator.startSimulation(this, originPoint, destinationPoint, newPoint -> {
                mapView.getController().animateTo(newPoint);
                if (originMarker != null) {
                    originMarker.setPosition(newPoint);
                }
                mapView.invalidate();
            });
        } else {
            Toast.makeText(this, "Monitoreo REAL iniciado(gps)", Toast.LENGTH_SHORT).show();
            if (simulator != null) {
                simulator.stopSimulation();
            }
        }
    }

    private void checkPermissions() {
        // codigo para pedir el gps (ubicacion)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSION_REQUEST_CODE);
        }

        // codigo para notificaciones
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_DENIED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
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