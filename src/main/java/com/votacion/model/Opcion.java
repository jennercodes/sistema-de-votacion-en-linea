package com.votacion.model;

import java.io.Serializable;

public class Opcion implements Serializable {

    private int id;
    private String texto;
    private int orden;

    public Opcion() {}

    public Opcion(int id, String texto, int orden) {
        this.id = id;
        this.texto = texto;
        this.orden = orden;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }

    public int getOrden() { return orden; }
    public void setOrden(int orden) { this.orden = orden; }
}
