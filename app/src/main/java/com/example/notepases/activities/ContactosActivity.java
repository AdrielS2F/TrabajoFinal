package com.example.notepases.activities;

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