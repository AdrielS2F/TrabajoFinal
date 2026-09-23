package com.example.notepases.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.notepases.R;
import com.example.notepases.models.Contacto;

import java.util.ArrayList;
import java.util.List;

public class ContactoAdapter extends ArrayAdapter<Contacto> {


    private final List<Contacto> items;


    public ContactoAdapter(@NonNull Context context, @NonNull List<Contacto> items) {
        super(context, 0, items);
        this.items = items;
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


        Contacto item = items.get(position);

        txtNombre.setText(item.getNombre());
        txtTelefono.setText(item.getTelefono());


        return view;
    }
}
