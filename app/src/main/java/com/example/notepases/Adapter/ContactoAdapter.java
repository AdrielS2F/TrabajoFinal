package com.example.notepases.Adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.notepases.R;
import com.example.notepases.database.ContactosDAO;
import com.example.notepases.models.Contacto;
import com.example.notepases.models.Validaciones;

import java.util.ArrayList;

public class ContactoAdapter extends ArrayAdapter<Contacto> {


    private final ArrayList<Contacto> items;

    private final ContactosDAO dao;


    public ContactoAdapter(@NonNull Context context, @NonNull ArrayList<Contacto> items) {
        super(context, 0, items);
        this.items = items;
        this.dao = new ContactosDAO(context);
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Nullable
    @Override
    public Contacto getItem(int position) {
        return items.get(position);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createItemView(position, convertView, parent);
    }



    private View createItemView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = convertView;

        if (view == null) {
            view = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_contacto, parent, false);
        }

        TextView txtNombre = view.findViewById(R.id.txtNombre);
        TextView txtTelefono = view.findViewById(R.id.txtTelefono);
        ImageButton btnEliminar = view.findViewById(R.id.btnEliminar);


        Contacto item = items.get(position);

        txtNombre.setText(item.getNombre());
        txtTelefono.setText(item.getTelefono());


        // ELIMINACION DENTRO DE ADAPTER PORQUE EL BOTON ELIMINAR ESTA DENTRO DEL ITEM.XML
        btnEliminar.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {


                // DIALOG CONFIRMACION ELIMINACION
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

                builder.setTitle("Eliminar contacto");

                builder.setMessage("¿Desea eliminar este contacto? \n\n Nombre: " + item.getNombre() + "\n Teléfono: " + item.getTelefono());

                builder.setPositiveButton("Eliminar", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        dao.EliminarContacto(item);
                        items.remove(item);
                        notifyDataSetChanged();

                        Toast.makeText(getContext(), "Contacto eliminado", Toast.LENGTH_SHORT).show();
                    }
                });

                builder.setNegativeButton("Cancelar", null);

                builder.show();
            }



        });


        // MODIFICACION DENTRO DE ADAPTER PORQUE EL BOTON ELIMINAR ESTA DENTRO DEL ITEM.XML

        ImageButton btnEditar = view.findViewById(R.id.btnEditar);

        btnEditar.setOnClickListener(v -> {

            View dialogView = LayoutInflater.from(getContext())
                    .inflate(R.layout.dialog_guardar_editar_contacto, null);

            TextView txtTitulo = dialogView.findViewById(R.id.txtTitulo);
            EditText editNombre = dialogView.findViewById(R.id.etNombre);
            EditText editTelefono = dialogView.findViewById(R.id.etTelefono);
            Button btnModificar = dialogView.findViewById(R.id.btnGuardar_Modificar);
            Button btnCancelar = dialogView.findViewById(R.id.btnCancelar);

            txtTitulo.setText("Editar contacto");
            btnModificar.setText("Modificar");
            editNombre.setText(item.getNombre());
            editTelefono.setText(item.getTelefono());

            AlertDialog dialog = new AlertDialog.Builder(getContext()).setView(dialogView).create();

            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }

            btnModificar.setOnClickListener(x -> {
                if (!Validaciones.validarContacto(editNombre, editTelefono, item.getId())) {
                    return;
                }

                item.setNombre(editNombre.getText().toString());
                item.setTelefono(editTelefono.getText().toString());

                dao.ModificarContacto(item);
                notifyDataSetChanged();

                Toast.makeText(getContext(), "Contacto modificado", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });

            btnCancelar.setOnClickListener(x -> dialog.dismiss());

            dialog.show();
        });

        return view;
    }
}
