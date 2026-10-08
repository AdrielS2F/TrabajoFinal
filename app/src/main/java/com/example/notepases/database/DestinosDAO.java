package com.example.notepases.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.example.notepases.models.Destino;

import java.util.ArrayList;

public class DestinosDAO {

    public static String destinosColumnaID = "_ID";
    private String destinoColumnaNombre = "nombre";
    private String destinoColumnaUbicacion = "ubicacion";
    private String destinoColumnaRadio = "radio";
    private OpenHelper openHelper;
    private String destinosTabla = "Destinos";

    public DestinosDAO(Context context) {
        openHelper = new OpenHelper(context, "notepases.db", null, 1);
    }

    public void AgregarDestino(Destino D) {
        ContentValues valores = new ContentValues();
        valores.put(destinoColumnaNombre, D.getNombre());
        valores.put(destinoColumnaUbicacion, D.getUbicacion());
        valores.put(destinoColumnaRadio, D.getRadio());
        openHelper.getWritableDatabase().insert(destinosTabla, null, valores);
    }

    // VERIFICACIÓN 1: Comprobar si ya existe un destino guardado con el mismo apodo/nombre
    public boolean existeNombreDestino(String nombre) {
        Cursor cursor = openHelper.getReadableDatabase().query(
                destinosTabla,
                new String[]{destinosColumnaID},
                destinoColumnaNombre + " = ? COLLATE NOCASE",
                new String[]{nombre.trim()},
                null, null, null);

        boolean existe = cursor.getCount() > 0;
        cursor.close();
        return existe;
    }

    // VERIFICACIÓN 2: Comprobar si ya existe un destino guardado con las mismas coordenadas
    public boolean existeUbicacionDestino(String ubicacion) {
        Cursor cursor = openHelper.getReadableDatabase().query(
                destinosTabla,
                new String[]{destinosColumnaID},
                destinoColumnaUbicacion + " = ?",
                new String[]{ubicacion.trim()},
                null, null, null);

        boolean existe = cursor.getCount() > 0;
        cursor.close();
        return existe;
    }

    public ArrayList<Destino> getListadoDestinos() {
        ArrayList<Destino> listaDestinos = new ArrayList<Destino>();
        Cursor mcursor = openHelper.getReadableDatabase().query("Destinos",
                new String[]{destinosColumnaID, destinoColumnaNombre, destinoColumnaUbicacion, destinoColumnaRadio},
                null, null, null, null, null);

        try {
            if (mcursor.moveToFirst()) {
                do {
                    Destino d = new Destino();
                    d.setId(mcursor.getInt(0));
                    d.setNombre(mcursor.getString(1));
                    d.setUbicacion(mcursor.getString(2));
                    d.setRadio(Integer.parseInt(mcursor.getString(3)));
                    listaDestinos.add(d);
                } while (mcursor.moveToNext());
            }
        } finally {
            mcursor.close();
        }
        return listaDestinos;
    }

    public void EliminarDestino(Destino d) {
        String id = String.valueOf(d.getId());
        openHelper.getWritableDatabase().delete(destinosTabla, "_ID=?", new String[]{id});
    }

    public void ModificarDestino(Destino d) {
        ContentValues valores = new ContentValues();
        valores.put(destinoColumnaNombre, d.getNombre());
        valores.put(destinoColumnaUbicacion, d.getUbicacion());
        valores.put(destinoColumnaRadio, d.getRadio());

        String id = String.valueOf(d.getId());
        openHelper.getWritableDatabase().update(destinosTabla, valores, "_ID=?", new String[]{id});
    }
}