package com.example.notepases.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

import com.example.notepases.Adapter.ContactoAdapter;
import com.example.notepases.R;
import com.example.notepases.database.ContactosDAO;
import com.example.notepases.models.Contacto;
import com.example.notepases.models.Validaciones;

import java.util.ArrayList;

public class ContactosActivity extends AppCompatActivity {

    private ListView listView;
    private Button btnAgregar;
    private ImageButton btnVolver;
    private ContactosDAO dao;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contactos);

        listView = findViewById(R.id.listViewContactos);
        btnAgregar = findViewById(R.id.btnAgregar);
        btnVolver = findViewById(R.id.btnVolver);

        dao = new ContactosDAO(this);
        cargarLista();

        btnAgregar.setOnClickListener(v -> mostrarDialogoAgregar());

        btnVolver.setOnClickListener(v -> finish());

        findViewById(R.id.btnImportar).setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                    == PackageManager.PERMISSION_GRANTED) {
                abrirSelectorContactos();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_CONTACTS}, REQ_READ_CONTACTS);
            }
        });
    }

    private static final int REQ_READ_CONTACTS = 2001;

    private final ActivityResultLauncher<Intent> selector = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), r -> {
                if (r.getResultCode() == RESULT_OK && r.getData() != null && r.getData().getData() != null) {
                    guardarContactoElegido(r.getData().getData());
                }
            });

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        super.onRequestPermissionsResult(code, perms, res);
        if (code == REQ_READ_CONTACTS) {
            if (res.length > 0 && res[0] == PackageManager.PERMISSION_GRANTED) {
                abrirSelectorContactos();
            } else {
                Toast.makeText(this, "Sin permiso no se pueden importar contactos", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void abrirSelectorContactos() {
        selector.launch(new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI));
    }

    private void guardarContactoElegido(Uri uri) {
        try (Cursor c = getContentResolver().query(uri, new String[]{
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER}, null, null, null)) {
            if (c == null || !c.moveToFirst()) return;
            String nombre = c.getString(0);
            String tel = c.getString(1) == null ? "" : c.getString(1).replaceAll("\\D", "");
            if (tel.length() < 10 || tel.length() > 15) {
                Toast.makeText(this, "El número debe tener entre 10 y 15 dígitos", Toast.LENGTH_SHORT).show();
                return;
            }
            for (Contacto x : dao.getListadoContactos()) {
                if (x.getTelefono().equals(tel)) {
                    Toast.makeText(this, "Ese teléfono ya está registrado", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            Contacto nuevo = new Contacto();
            nuevo.setNombre(nombre);
            nuevo.setTelefono(tel);
            dao.AgregarContacto(nuevo);
            cargarLista();
            Toast.makeText(this, "Contacto importado", Toast.LENGTH_SHORT).show();
        }
    }



    private void cargarLista() {
        ArrayList<Contacto> listaContactos = dao.getListadoContactos();
        ContactoAdapter adapter = new ContactoAdapter(this, listaContactos);
        listView.setAdapter(adapter);
    }


    // ESTO EVITA CREAR OTRO .XML, APARECE VENTANA EMERGENTE (DIALOG)
    public void mostrarDialogoAgregar() {

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_guardar_editar_contacto, null);
        TextView txtTitulo = dialogView.findViewById(R.id.txtTitulo);
        EditText etNombre = dialogView.findViewById(R.id.etNombre);
        EditText etTelefono = dialogView.findViewById(R.id.etTelefono);
        Button btnGuardar = dialogView.findViewById(R.id.btnGuardar_Modificar);
        Button btnCancelar = dialogView.findViewById(R.id.btnCancelar);

        txtTitulo.setText("Nuevo contacto");

        AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView).create();


        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnGuardar.setOnClickListener(v -> {
            // SI ESTA MAL SE QUEDA EN EL DIALOG MOSTRANDO ERROR
            if (!Validaciones.validarContacto(etNombre, etTelefono, 0)) {
                return;
            }

            Contacto c = new Contacto();
            c.setNombre(etNombre.getText().toString());
            c.setTelefono(etTelefono.getText().toString());

            dao.AgregarContacto(c);
            cargarLista();

            Toast.makeText(getApplicationContext(), "Contacto agregado", Toast.LENGTH_SHORT).show();

            // CIERRE MANUAL CUANDO ESTA OK
            dialog.dismiss();
        });

        btnCancelar.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    }