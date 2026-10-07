package com.example.notepases.models;

public class Destino {

    //ATTRIBUTES
    private int id;
    private String nombre;
    private String ubicacion;
    private int radio;

    //CONSTRUCTORS
    public Destino() {
    }

    public Destino(int id,String nombre, String ubicacion, int radio) {
        this.id = id;
        this.nombre = nombre;
        this.ubicacion = ubicacion;
        this.radio = radio;
    }

    //SETTERS AND GETTERS


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
    }

    public int getRadio() {
        return radio;
    }

    public void setRadio(int radio) {
        this.radio = radio;
    }
}
