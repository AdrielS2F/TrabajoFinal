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
import com.example.notepases.models.Validaciones;

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
    public void mostrarDialogoAgregar() {

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

         EditText etNombre = new EditText(this);
        etNombre.setHint("Nombre");
        layout.addView(etNombre);

         EditText etTelefono = new EditText(this);
        etTelefono.setHint("Telefono");
        layout.addView(etTelefono);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Nuevo contacto");
        builder.setView(layout);

        // BOTONES SIN LISTENER PARA CONFIGURAR POSITIVE MANUALMENTE DESPUES
        builder.setPositiveButton("Guardar", null);
        builder.setNegativeButton("Cancelar", null);

        // SE CREA EL DIALOGO PERO NO SE MUESTRA
         AlertDialog dialog = builder.create();

        // ESTO SE EJECUTA CUANDO APARECE EL DIALOGO
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {

                // OBTENGO BOTON POSITIVE ("GUARDAR) PARA CONFIGURAR ON CLICK
                Button btnGuardar = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

                // DEFINO QUE HACE EL ON CLIC EN "GUARDAR"
                btnGuardar.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        // SI ESTA MAL SE QUEDA EN EL DIALOG MOSTRARODO ERROR
                        if (!Validaciones.validarContacto(etNombre, etTelefono)) {
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
                    }
                });
            }
        });
            //SE MUESTAR EL DIALOG Y SE ACTIVA setOnShowListener de ARRIBA
        dialog.show();
    }

    }