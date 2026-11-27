package com.example.notepases.activities;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
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
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.annotation.NonNull;

import com.example.notepases.R;
import com.example.notepases.database.ContactosDAO;
import com.example.notepases.database.DestinosDAO;
import com.example.notepases.models.Contacto;
import com.example.notepases.models.Destino;
import com.example.notepases.services.TrackingService;
import com.example.notepases.utils.DemoLocationSimulator;
import com.example.notepases.utils.LocationUtils;
import android.annotation.SuppressLint;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.events.MapEventsReceiver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.app.NotificationManager;
import android.net.Uri;
import android.provider.Settings;

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
    private ArrayList<String> telefonosAsignados = new ArrayList<>();
    private boolean isTracking = false;
    private Button btnAsignarContacto;
    private String mensajePersonalizado = ""; // vacio = usar mensaje default (solo en memoria)
    private boolean incluirDistancia = true;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final android.content.BroadcastReceiver realLocationReceiver = new android.content.BroadcastReceiver() {
        @Override
        public void onReceive(android.content.Context context, Intent intent){
            if (intent == null )return;
            if ("ACTION_STOP_SIMULATOR".equals(intent.getAction())){
                if (simulator != null){
                    simulator.stopSimulation();
                }
                updateTrackingButtonState(false);
                return;
            }
            if (!switchSimulationMode.isChecked() && TrackingService.ACTION_REAL_LOCATION_UPDATE.equals(intent.getAction())){
                double lat = intent.getDoubleExtra(TrackingService.EXTRA_LAT,  0.0);
                double lng = intent.getDoubleExtra(TrackingService.EXTRA_LNG,  0.0);
                GeoPoint realPoint = new GeoPoint(lat, lng);

                originPoint = realPoint;
                if (originMarker == null){
                    updateMapGraphics();
                } else {
                    originMarker.setPosition(realPoint);
                }
                mapView.getController().animateTo(realPoint);
                mapView.invalidate();
            }
        }
    };

    private void verificarFullScreenIntent(){
        if (Build.VERSION.SDK_INT >= 34) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null && !nm.canUseFullScreenIntent()){
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                        Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        org.osmdroid.config.IConfigurationProvider osmConfig = Configuration.getInstance();
        osmConfig.load(getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        // El User-Agent y los ajustes deben ir SIEMPRE despues de load() para que no se pisen
        osmConfig.setUserAgentValue("NoTePasesApp/1.0 (contacto@notepases.com)");
        osmConfig.setTileFileSystemCacheMaxBytes(500L * 1024 * 1024);  // cache en disco: 500 MB
        osmConfig.setTileFileSystemCacheTrimBytes(400L * 1024 * 1024); // recorta hasta 400 MB al llenarse
        osmConfig.setCacheMapTileCount((short) 64);                    // tiles en memoria RAM listos para mostrar en la pantalla
        osmConfig.setCacheMapTileOvershoot((short) 16);                // tiles con 16 de margen
        osmConfig.setTileDownloadThreads((short) 8);                   // descargas en paralelo (ArcGIS CDN)
        osmConfig.setTileDownloadMaxQueueSize((short) 80);              // maximo de filas si si es que se mueve muy rapido
        osmConfig.setTileFileSystemThreads((short) 8);                 // lecturas de cache en paralelo

        setContentView(R.layout.activity_main);

        // Instanciar DAO de Destinos Frecuentes
        destinosDAO = new DestinosDAO(this);

        // Vistas
        mapView = findViewById(R.id.mapView);
        mapView.setMaxZoomLevel(19.0);
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
        btnAsignarContacto = findViewById(R.id.btnAsignarContacto);

        // Vistas del Panel Desplegable
        layoutToggleMore = findViewById(R.id.layoutToggleMore);
        layoutExpandableOptions = findViewById(R.id.layoutExpandableOptions);
        tvToggleMore = findViewById(R.id.tvToggleMore);

        // Estado inicial de la estrella: deshabilitada hasta buscar un destino válido
        btnSaveFavorite.setEnabled(false);
        btnSaveFavorite.setAlpha(0.4f);

        switchSimulationMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            tvModeLabel.setText(isChecked ? "Modo Simulación" : "Modo Real");
            if (isChecked){
                etOrigin.setEnabled(true);
                etOrigin.setHint("Origen (Opcional)");
            } else{
                if (simulator != null){
                    simulator.stopSimulation();
                }
                etOrigin.setText("");
                etOrigin.setHint("Usando tu ubicacion GPS actual...");
                etOrigin.setEnabled(false);
            }
        });

        Button btnZoomIn = findViewById(R.id.btnZoomIn);
        Button btnZoomOut = findViewById(R.id.btnZoomOut);

        simulator = new DemoLocationSimulator();

        // Configuración del mapa osmdroid (ArcGIS World Street Map - sin API key y sin bloqueos)
        mapView.setTileSource(new org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase(
                "ArcGIS_World_Street_Map", 0, 19, 256, "",
                new String[]{"https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/"}) { // antes se utilizaba openstreetmap
            @Override
            public String getTileURLString(long pMapTileIndex) {
                return getBaseUrl()
                        + org.osmdroid.util.MapTileIndex.getZoom(pMapTileIndex) + "/"
                        + org.osmdroid.util.MapTileIndex.getY(pMapTileIndex) + "/"
                        + org.osmdroid.util.MapTileIndex.getX(pMapTileIndex);
            }
        });
        mapView.setUseDataConnection(true); // asegura que el mapa tenga internet para descargar tiles nuevos

        mapView.setMultiTouchControls(true);
        mapView.setTilesScaledToDpi(true); // tiles mas nitidos en pantallas de alta densidad
        mapView.getController().setZoom(15.0);

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

        // Autocompletado de lugares (estilo Google Maps)
        setupAutocomplete((AutoCompleteTextView) etOrigin);
        setupAutocomplete((AutoCompleteTextView) etDestination);

        // Listeners principales
        btnSearch.setOnClickListener(v -> searchLocations());
        btnStartTracking.setOnClickListener(v -> {
            if (isTracking) {
                stopTrackingService();
                btnStartTracking.announceForAccessibility("Monitoreo detenido");
            } else {
                startTrackingService();
                btnStartTracking.announceForAccessibility("Monitoreo iniciado. Te avisaremos al acercarte a tu destino.");
            }
        });
        btnSaveFavorite.setOnClickListener(v -> mostrarDialogoGuardarDestino());



        btnContactos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ContactosActivity.class);
                startActivity(intent);
            }
        });

        btnDestinosFrecuentes.setOnClickListener(v -> mostrarListaDestinosFrecuentes());

        // Listener para abrir / cerrar la pestañita desplegable
        layoutToggleMore.setOnClickListener(v -> {
            if (layoutExpandableOptions.getVisibility() == View.GONE) {
                layoutExpandableOptions.setVisibility(View.VISIBLE);
                tvToggleMore.setText("Menos opciones ▲");
                layoutToggleMore.announceForAccessibility("Opciones adicionales desplegadas");
            } else {
                layoutExpandableOptions.setVisibility(View.GONE);
                tvToggleMore.setText("Más opciones ▼");
                layoutToggleMore.announceForAccessibility("Opciones adicionales contraídas");
            }
        });

        // Control de Slider de Radio
        sbRadius.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                selectedRadius = MIN_RADIUS + progress;
                selectedRadius = LocationUtils.validateRadius(selectedRadius);
                tvRadiusLabel.setText("Radio de Alerta: " + selectedRadius + " m");
                updateRadiusCircleOnly();

                // Anunciar los metros sólo cuando el usuario mueve el slider
                if (fromUser) {
                    seekBar.announceForAccessibility("Radio de alerta: " + selectedRadius + " metros");

                    // Si el viaje ya esta en curso, avisar al servicio del nuevo radio
                    if (isTracking) {
                        Intent radiusIntent = new Intent(MainActivity.this, TrackingService.class);
                        radiusIntent.setAction(TrackingService.ACTION_UPDATE_RADIUS);
                        radiusIntent.putExtra("RADIUS_METERS", selectedRadius);
                        startService(radiusIntent);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnAsignarContacto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mostrarContactosParaViaje();
            }
        });

        setupMapLongClickListener();
    }

    // ----- Autocompletado: mientras se escribe, se piden direcciones al Geocoder ---
    private void setupAutocomplete(AutoCompleteTextView campo) {
        // Adapter con la lista de sugerencias (el filtro vacío evita que Android oculte resultados)
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.item_sugerencia, new ArrayList<>()) {
            @NonNull @Override
            public android.widget.Filter getFilter() {
                return new android.widget.Filter() {
                    @Override protected FilterResults performFiltering(CharSequence c) { return new FilterResults(); }
                    @Override protected void publishResults(CharSequence c, FilterResults r) { }
                };
            }
        };
        campo.setAdapter(adapter);

        // Guarda la búsqueda agendada para poder cancelarla (array de 1 porque la lambda exige variable final)
        Runnable[] busquedaPendiente = new Runnable[1];

        campo.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                String texto = s.toString().trim();

                // Debounce: cada letra cancela la búsqueda pendiente
                mainHandler.removeCallbacks(busquedaPendiente[0]);
                if (texto.length() < 3) return;

                // ...y agenda una nueva para 500 ms después (si el usuario sigue escribiendo, se cancela)
                busquedaPendiente[0] = () -> executorService.execute(() -> {
                    ArrayList<String> sugerencias = new ArrayList<>();
                    try {
                        for (Address a : new Geocoder(MainActivity.this, Locale.getDefault()).getFromLocationName(texto, 5)) {
                            sugerencias.add(a.getAddressLine(0));
                        }
                    } catch (IOException ignored) { }

                    // Mostrar la lista en el hilo principal
                    mainHandler.post(() -> {
                        if (sugerencias.isEmpty() || sugerencias.contains(texto) || !campo.hasFocus()) return;
                        adapter.clear();
                        adapter.addAll(sugerencias);
                        campo.showDropDown();
                    });
                });
                mainHandler.postDelayed(busquedaPendiente[0], 500);
            }
        });
    }
    @SuppressLint("ClickableViewAccessibility")
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
                        if (isTracking){
                            stopTrackingService();
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

    private void mostrarRutaCargada() {
        updateMapGraphics();
        ajustarCamaraParaMostrarPuntos(originPoint, destinationPoint);
        Toast.makeText(this, "Ruta cargada correctamente", Toast.LENGTH_SHORT).show();
    }

    @SuppressLint("MissingPermission")
    private void obtenerUbicacionRealYMostrarRuta() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "se necesita permiso de GPS para el modo real", Toast.LENGTH_SHORT).show();
            checkPermissions();
            return;
        }
        FusedLocationProviderClient client =
                LocationServices.getFusedLocationProviderClient(this);
        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        originPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                        mostrarRutaCargada();
                    } else {
                        client.getLastLocation().addOnSuccessListener(last -> {
                            if (last != null) {
                                originPoint = new GeoPoint(last.getLatitude(), last.getLongitude());
                                mostrarRutaCargada();
                            } else {
                                Toast.makeText(this, "No se pudo obtener tu ubicación GPS", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error al obtener ubicación GPS", Toast.LENGTH_SHORT).show());
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

            // 2. Origen
            if (!switchSimulationMode.isChecked()) {
                // MODO REAL: usar la ubicación GPS actual del dispositivo
                obtenerUbicacionRealYMostrarRuta();
                return;
            }
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

            mostrarRutaCargada();
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

    // Al mover el slider solo se recalcula el circulo existente.
    private void updateRadiusCircleOnly() {
        if (radiusCircle == null || destinationPoint == null
                || !mapView.getOverlays().contains(radiusCircle)) {
            updateMapGraphics();
            return;
        }
        radiusCircle.setPoints(Polygon.pointsAsCircle(destinationPoint, (double) selectedRadius));
        mapView.setContentDescription("Mapa mostrando destino en " + etDestination.getText().toString() + " con radio de " + selectedRadius + " metros.");
        mapView.invalidate();
    }

    private void updateMapGraphics() {
        mapView.getOverlays().clear();
        setupMapLongClickListener();

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

            mapView.setContentDescription("Mapa mostrando destino en " + etDestination.getText().toString() + " con radio de " + selectedRadius + " metros.");
        }

        mapView.invalidate();
    }

    private void startTrackingService() {
        if (destinationPoint == null) {
            Toast.makeText(this, "Primero buscá un destino", Toast.LENGTH_SHORT).show();
            return;
        }
        boolean useSimulation = switchSimulationMode.isChecked();

        if (!useSimulation){
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, "se necesita permiso de GPS para el modo real", Toast.LENGTH_SHORT).show();
                checkPermissions();
                return;
            }
        }

        if (originPoint == null) {
            originPoint = (GeoPoint) mapView.getMapCenter();
            updateMapGraphics();
        }
        verificarFullScreenIntent();

        // 1. Iniciar servicio
        Intent serviceIntent = new Intent(this, TrackingService.class);
        serviceIntent.putExtra("DEST_LAT", destinationPoint.getLatitude());
        serviceIntent.putExtra("DEST_LNG", destinationPoint.getLongitude());
        serviceIntent.putExtra("RADIUS_METERS", selectedRadius);
        serviceIntent.putExtra("IS_SIMULATION", useSimulation);
        serviceIntent.putExtra("DEST_ADDRESS", etDestination.getText().toString());
        serviceIntent.putStringArrayListExtra("CONTACT_NUMBERS", telefonosAsignados);
        serviceIntent.putExtra("CUSTOM_MESSAGE", mensajePersonalizado);
        serviceIntent.putExtra("INCLUDE_DISTANCE", incluirDistancia);

        ContextCompat.startForegroundService(this, serviceIntent);
        updateTrackingButtonState(true); //esto cambia el boton a rojo "detener monitoreo"


        // ejecuta simulacion o gps real segun corresponda
        if (useSimulation) {
            Toast.makeText(this, "Obteniendo ruta e iniciando monitoreo...", Toast.LENGTH_SHORT).show();


            simulator.startSimulation(this, originPoint, destinationPoint, newPoint -> {
                mapView.getController().animateTo(newPoint);
                if (originMarker != null) {
                    originMarker.setPosition(newPoint);
                }
                mapView.invalidate();
            });
        } else {
            if (simulator != null) {
                simulator.stopSimulation();
            }
            Toast.makeText(this, "Monitoreo REAL iniciado(gps)", Toast.LENGTH_SHORT).show();
        }
    }


    // Oculta (sube) o vuelve a mostrar el panel superior (origen, destino, etc.) con animacion.
    // Los textos de los campos se conservan porque la vista solo se desliza, no se destruye.
    private void animarPanelSuperior(boolean ocultar) {
        final View panel = findViewById(R.id.layoutSearch);
        if (panel == null) return;
        panel.animate().cancel();
        if (ocultar) {
            View foco = getCurrentFocus();
            if (foco != null) foco.clearFocus();
            float desplazamiento = -(panel.getHeight() + panel.getTop() + 50f);
            panel.animate().translationY(desplazamiento).alpha(0f).setDuration(300)
                    .withEndAction(() -> panel.setVisibility(View.GONE)).start();
        } else {
            panel.setVisibility(View.VISIBLE);
            panel.animate().translationY(0f).alpha(1f).setDuration(300).start();
        }
    }

    private void updateTrackingButtonState(boolean tracking){
        boolean cambio = (isTracking != tracking);
        isTracking = tracking;
        if (cambio) animarPanelSuperior(tracking);
        if (tracking){
            btnStartTracking.setText("Detener Monitoreo");
            btnStartTracking.setTextColor(Color.WHITE);
            btnStartTracking.setBackgroundColor(Color.RED);
            btnStartTracking.setContentDescription("Detener Monitoreo");
        } else {
            btnStartTracking.setText("Iniciar Monitoreo");
            btnStartTracking.setTextColor(Color.BLACK);
            btnStartTracking.setBackgroundColor(Color.YELLOW);
            btnStartTracking.setContentDescription("Iniciar Monitoreo");
        }
    }
    private void stopTrackingService() {
        if (simulator != null) {
            simulator.stopSimulation();
        }
        Intent stopIntent = new Intent(this, TrackingService.class);

        stopIntent.setAction(TrackingService.ACTION_STOP_SERVICE);
        startService(stopIntent);

        updateTrackingButtonState(false);
        Toast.makeText(this, "Monitoreo detenido", Toast.LENGTH_SHORT).show();

    }
    private void checkPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        // codigo para pedir el gps (ubicacion)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
            permissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        //codigo para pedir SMS
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.SEND_SMS);
        }
        // codigo para notificaciones
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsNeeded.toArray(new String[0]),
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
        android.content.IntentFilter filter = new android.content.IntentFilter();
        filter.addAction(TrackingService.ACTION_REAL_LOCATION_UPDATE);
        filter.addAction("ACTION_STOP_SIMULATOR");
        ContextCompat.registerReceiver(this, realLocationReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
        unregisterReceiver(realLocationReceiver);
    }

    // segundo dialog: la lista de contactos guardados en la bd, para elegir uno
    private void mostrarContactosParaViaje() {

        ContactosDAO dao = new ContactosDAO(this);
        ArrayList<Contacto> contactos = dao.getListadoContactos();

        if (contactos.isEmpty()) {
            Toast.makeText(this, "No hay contactos, cargalos en Gestionar contactos", Toast.LENGTH_SHORT).show();
            return;
        }

        // INFLAR Y BUSCAR LOS ELEMENTOS
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_asignar_contactos, null);
        ListView lvContactos = dialogView.findViewById(R.id.lvContactosAsignar);
        Button btnCancelar = dialogView.findViewById(R.id.btnCancelarAsignar);
        Button btnConfirmar = dialogView.findViewById(R.id.btnConfirmarAsignar);

        // TEXTOS EN CADA RENGLON
        String[] textos = new String[contactos.size()];
        for (int i = 0; i < contactos.size(); i++) {
            textos[i] = contactos.get(i).getNombre() + " - " + contactos.get(i).getTelefono();
        }

        // ADAPTER CON DATOS RENGLON DE ARRIBA
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, R.layout.item_contacto_asignar, R.id.ctvContacto, textos);
        lvContactos.setAdapter(adapter);

        // TILDAR LOS QUE YA ESTAN ASIGNADOS
        for (int i = 0; i < contactos.size(); i++) {
            if (telefonosAsignados.contains(contactos.get(i).getTelefono())) {
                lvContactos.setItemChecked(i, true);
            }
        }


        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // BOTON PERSONALIZAR MENSAJE
        Button btnPersonalizar = dialogView.findViewById(R.id.btnPersonalizarMensaje);
        btnPersonalizar.setText(mensajePersonalizado.isEmpty() ? "Personalizar mensaje" : "Mensaje personalizado ✓");
        btnPersonalizar.setOnClickListener(v -> mostrarDialogoMensaje(btnPersonalizar));

        // BOTONES
        btnCancelar.setOnClickListener(v -> dialog.dismiss());


         // VALIDACION MAXIMO 3 CONTACTOS

        btnConfirmar.setOnClickListener(v -> {
            if (lvContactos.getCheckedItemCount() > 3) {
                Toast.makeText(this, "Elegí hasta 3 contactos", Toast.LENGTH_SHORT).show();
                return;
            }

            //LIMPIA ARRAY Y SE CHECKEA LO TILDADO

            telefonosAsignados.clear(); //
            for (int i = 0; i < contactos.size(); i++) {
                if (lvContactos.isItemChecked(i)) {
                    telefonosAsignados.add(contactos.get(i).getTelefono());
                }
            }


            // CAMBIO DE TEXTO EN BTN SEGUN SI TIENE ASIGNADO O NO CONTACTO
            if (telefonosAsignados.isEmpty()) {
                btnAsignarContacto.setText("Asignar contacto");
            } else {
                btnAsignarContacto.setText("Avisar a " + telefonosAsignados.size() + " contacto(s)");
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    // dialog para escribir el mensaje personalizado (vacio = mensaje default)
    private void mostrarDialogoMensaje(Button botonOrigen) {
        View view = getLayoutInflater().inflate(R.layout.dialog_mensaje_personalizado, null);
        EditText etMensaje = view.findViewById(R.id.etMensajePersonalizado);
        Button btnCancelarMsg = view.findViewById(R.id.btnCancelarMensaje);
        Button btnGuardarMsg = view.findViewById(R.id.btnGuardarMensaje);
        android.widget.CheckBox cbDistancia = view.findViewById(R.id.cbDistancia);
        etMensaje.setText(mensajePersonalizado);
        cbDistancia.setChecked(incluirDistancia);

        AlertDialog dlg = new AlertDialog.Builder(this).setView(view).create();
        if (dlg.getWindow() != null) {
            dlg.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        btnCancelarMsg.setOnClickListener(v -> dlg.dismiss());
        btnGuardarMsg.setOnClickListener(v -> {
            mensajePersonalizado = etMensaje.getText().toString().trim();
            incluirDistancia = cbDistancia.isChecked();
            botonOrigen.setText(mensajePersonalizado.isEmpty() ? "Personalizar mensaje" : "Mensaje personalizado ✓");
            dlg.dismiss();
        });
        dlg.show();
    }

    private void setupMapLongClickListener () {
        MapEventsReceiver mapEventsReceiver = new MapEventsReceiver() {

            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                return false;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                if (p != null){
                    onMapLongPressed(p);
                }
                return true;
            }
        };
        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(mapEventsReceiver);
        mapView.getOverlays().add(0, mapEventsOverlay);
    }

    private void onMapLongPressed(GeoPoint geoPoint) {
        // 1. Actualizar el destino al punto presionado (se conserva el origen ya definido)
        destinationPoint = geoPoint;

        // 2. Habilitar botón de guardar favorito ya que hay un destino válido
        btnSaveFavorite.setEnabled(true);
        btnSaveFavorite.setAlpha(1.0f);

        // 3. Actualizar los gráficos del mapa: redibuja el origen, y el nuevo destino con su círculo de radio.
        updateMapGraphics();

        // 4. Ejecutar la geocodificación inversa en segundo plano
        buscarDireccionEnSegundoPlano(geoPoint);
    }
    private void buscarDireccionEnSegundoPlano(GeoPoint geoPoint) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocation(geoPoint.getLatitude(), geoPoint.getLongitude(), 1, new Geocoder.GeocodeListener() {
                @Override
                public void onGeocode(@NonNull List<Address> addresses) {
                    String direccion = procesarDireccion(addresses, geoPoint);
                    actualizarUIConDireccion(direccion);
                }

                @Override
                public void onError(String errorMessage) {
                    actualizarUIConDireccion("Lat: " + geoPoint.getLatitude() + ", Lon: " + geoPoint.getLongitude());
                }
            });
        } else {
            executorService.execute(() -> {
                String direccion;
                try {
                    List<Address> addresses = geocoder.getFromLocation(geoPoint.getLatitude(), geoPoint.getLongitude(), 1);
                    direccion = procesarDireccion(addresses, geoPoint);
                } catch (IOException e) {
                    direccion = "Lat: " + geoPoint.getLatitude() + ", Lon: " + geoPoint.getLongitude();
                }

                String finalDireccion = direccion;
                mainHandler.post(() -> actualizarUIConDireccion(finalDireccion));
            });
        }
    }

    private String procesarDireccion(List<Address> addresses, GeoPoint geoPoint) {
        if (addresses != null && !addresses.isEmpty()) {
            return addresses.get(0).getAddressLine(0);
        } else {
            return "Lat: " + geoPoint.getLatitude() + ", Lon: " + geoPoint.getLongitude();
        }
    }

    private void actualizarUIConDireccion(String direccion) {
        runOnUiThread(() -> etDestination.setText(direccion));
    }
}
