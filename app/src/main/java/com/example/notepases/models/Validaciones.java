package com.example.notepases.models;

import android.widget.EditText;

import com.example.notepases.database.ContactosDAO;

import java.util.ArrayList;

public class Validaciones {

    public static boolean validarContacto(EditText editNombre, EditText editTelefono, int idContacto) {
        boolean estado = true;
        editNombre.setError(null);
        editTelefono.setError(null);

        if (editNombre.getText().toString().isEmpty()) {
            editNombre.setError("El campo está vacío");
            estado = false;
        } else if (editNombre.getText().toString().length() < 3) {
            editNombre.setError("Debe tener más de 3 caracteres");
            estado = false;
        } else if (!editNombre.getText().toString().matches("^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ ]+$")) {
            editNombre.setError("El nombre solo puede contener letras");
            estado = false;
        }


        if (editTelefono.getText().toString().isEmpty()) {
            editTelefono.setError("El campo está vacío");
            estado = false;
        } else if (editTelefono.getText().toString().length() != 10) {
            editTelefono.setError("El teléfono debe tener 10 dígitos");
            estado = false;
        } else if (!editTelefono.getText().toString().matches("^[0-9]+$")) {
            editTelefono.setError("Solo se permiten números");
            estado = false;
        } else {

            ContactosDAO dao = new ContactosDAO(editTelefono.getContext());

            ArrayList<Contacto> lista = dao.getListadoContactos();

            String telefonoIngresado = editTelefono.getText().toString();

            for (int i = 0; i < lista.size(); i++) {

                if (lista.get(i).getTelefono().equals(telefonoIngresado) && lista.get(i).getId() != idContacto) {
                    editTelefono.setError("Ese teléfono ya está registrado");
                    estado = false;
                }
            }

        }
        return estado;
    }
}
