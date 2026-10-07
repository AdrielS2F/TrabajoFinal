package com.example.notepases.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class OpenHelper extends SQLiteOpenHelper {

    public static String ContactosCreacionTabla = "CREATE TABLE IF NOT EXISTS Contactos(_ID integer primary key autoincrement, nombre text, telefono text unique)";
    public static String DestinosCreacionTabla = "CREATE TABLE IF NOT EXISTS Destinos (_ID integer primary key autoincrement, nombre text, ubicacion text, radio integer)";

    public OpenHelper(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        String queryContactos = ContactosCreacionTabla;
        db.execSQL(queryContactos);

        String queryDestinos = DestinosCreacionTabla;
        db.execSQL(queryDestinos);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}