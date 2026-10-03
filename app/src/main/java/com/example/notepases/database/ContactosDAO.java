package com.example.notepases.database;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;


import com.example.notepases.models.Contacto;

import java.util.ArrayList;


public class ContactosDAO {

    public static String ContactosColumnaID = "_ID";


    private String ContactosColumnaNombre = "nombre";


    private String ContactosColumnaTelefono = "telefono";

    private OpenHelper openHelper;

    private String ContactosTabla = "Contactos";



    public ContactosDAO(Context context) {   //
        openHelper = new OpenHelper(context, "notepases.db", null, 1);
    }


    public void AgregarContacto (Contacto C ) {

        ContentValues valores = new ContentValues();
        valores.put(ContactosColumnaNombre, C.getNombre());
        valores.put(ContactosColumnaTelefono,C.getTelefono());
        openHelper.getWritableDatabase().insert(ContactosTabla,null,valores);
    }


    public ArrayList<Contacto> getListadoContactos()
    {
        ArrayList<Contacto> listaContactos = new ArrayList<Contacto>();
        Cursor mcursor = openHelper.getReadableDatabase().query("Contactos",
                new String[]{ContactosColumnaID, ContactosColumnaNombre, ContactosColumnaTelefono},
                null, null, null, null, null);

        try {
            if (mcursor.moveToFirst()) {
                do {
                    Contacto c = new Contacto();
                    c.setId(mcursor.getInt(0));
                    c.setNombre(mcursor.getString(1));
                    c.setTelefono(mcursor.getString(2));
                    listaContactos.add(c);
                } while (mcursor.moveToNext());
            }
        } finally {
            mcursor.close(); // se cirra siempre aunque la tabla este vacia
        }
        return listaContactos;
    }


    public void EliminarContacto(Contacto C)
    {
        String id = String.valueOf(C.getId());
        openHelper.getWritableDatabase().delete(ContactosTabla, "_ID=?", new String[]{id});
    }


    public void ModificarContacto(Contacto C)
    {
        ContentValues valores = new ContentValues();
        valores.put(ContactosColumnaNombre, C.getNombre());
        valores.put(ContactosColumnaTelefono, C.getTelefono());

        String id = String.valueOf(C.getId());
        openHelper.getWritableDatabase().update(ContactosTabla, valores, "_ID=?", new String[]{id});
    }

}
