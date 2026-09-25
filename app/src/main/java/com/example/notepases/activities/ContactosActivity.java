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
import android.widget.Toast;

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


    // ESTO EVITA CREAR OTRO .XML, APARECE VENTANA EMERGENTE (DIALOG)
    private void mostrarDialogoAgregar() {

        //CREACION LAYOUT
        LinearLayout layout = new LinearLayout(this);

        layout.setOrientation(LinearLayout.VERTICAL);

        layout.setPadding(50, 40, 50, 10);


        //CONTROLES DEL LAYOUT

        EditText editNombre = new EditText(this);

        editNombre.setHint("Nombre");

        layout.addView(editNombre);


        EditText editTelefono = new EditText(this);

        editTelefono.setHint("Teléfono");

        layout.addView(editTelefono);


        // CREACION Y CONFIGURACION VENTANA DIALOG

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Nuevo contacto");

        builder.setView(layout);  //LE ASIGNO EL LAYOUT CREADO ARRIBA

        builder.setPositiveButton("Guardar", new DialogInterface.OnClickListener() {


            // CLICK EN GUARDAR
            @Override
            public void onClick(DialogInterface dialog, int which) {

                Contacto c = new Contacto();

                c.setNombre(editNombre.getText().toString());

                c.setTelefono(editTelefono.getText().toString());

                dao.AgregarContacto(c);

                cargarLista();

                Toast.makeText(getApplicationContext(), "Contacto agregado", Toast.LENGTH_SHORT).show();
            }
        });

        //BOTON CANCELAR NEGATIVE SALIR SIN CONFIRMAR
        builder.setNegativeButton("Cancelar", null);

        builder.show();
    }

}