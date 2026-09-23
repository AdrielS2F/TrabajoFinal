package com.example.notepases.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.notepases.Adapter.ContactoAdapter;
import com.example.notepases.R;
import com.example.notepases.database.ContactosDAO;
import com.example.notepases.models.Contacto;

import java.util.ArrayList;

public class ContactosActivity extends AppCompatActivity {

    private ListView listView;
    private Button btnAgregar;

    private ContactosDAO dao;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contactos);

        listView = findViewById(R.id.listViewContactos);
        btnAgregar = findViewById(R.id.btnAgregar);

        dao = new ContactosDAO(this);
        cargarLista();

        btnAgregar.setOnClickListener(v -> mostrarDialogoAgregar());
    }



    private void cargarLista() {
        ArrayList<Contacto> listaContactos = dao.getListadoContactos();
        ContactoAdapter adapter = new ContactoAdapter(this, listaContactos);
        listView.setAdapter(adapter);
    }


    // ESTO EVITA CREAR OTRO .XML, APARECE VENTANA EMERGENTE
    private void mostrarDialogoAgregar() {
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        EditText editNombre = new EditText(this);
        editNombre.setHint("Nombre");
        layout.addView(editNombre);

        EditText editTelefono = new EditText(this);
        editTelefono.setHint("Teléfono");
        layout.addView(editTelefono);

        new AlertDialog.Builder(this)
                .setTitle("Nuevo contacto")
                .setView(layout)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    Contacto c = new Contacto();
                    c.setNombre(editNombre.getText().toString());
                    c.setTelefono(editTelefono.getText().toString());
                    dao.AgregarContacto(c);
                    cargarLista();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}