package com.votacion.bean;

import com.votacion.dao.EncuestaDAO;
import com.votacion.model.Encuesta;
import com.votacion.model.Opcion;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named
@ViewScoped
public class EncuestaBean implements Serializable {

    private static final int MIN_OPCIONES = 2;
    private static final int MAX_OPCIONES = 6;

    private final EncuestaDAO encuestaDAO = new EncuestaDAO();

    private List<Encuesta> encuestas = new ArrayList<>();
    private Encuesta encuestaActual = new Encuesta();
    private Integer encuestaId;
    private List<String> opcionesTexto = nuevasOpcionesVacias();
    private String mensajeError;
    private String mensajeExito;

    @PostConstruct
    public void init() {
        encuestas = encuestaDAO.listar();
    }

    public void agregarOpcion() {
        if (opcionesTexto.size() < MAX_OPCIONES) {
            opcionesTexto.add("");
        }
    }

    public void eliminarOpcion(int index) {
        if (opcionesTexto.size() > MIN_OPCIONES
                && index >= 0
                && index < opcionesTexto.size()) {
            opcionesTexto.remove(index);
        }
    }

    public String guardar() {
        limpiarMensajes();

        if (encuestaActual.getTitulo() == null || encuestaActual.getTitulo().isBlank()) {
            mensajeError = "El título de la encuesta es obligatorio.";
            return null;
        }

        long opcionesValidas = opcionesTexto.stream()
                .filter(texto -> texto != null && !texto.isBlank())
                .count();

        if (opcionesValidas < MIN_OPCIONES) {
            mensajeError = "Se requieren al menos " + MIN_OPCIONES + " opciones con texto.";
            return null;
        }

        encuestaDAO.guardar(encuestaActual, opcionesTexto);
        mensajeExito = "Encuesta guardada correctamente.";
        encuestas = encuestaDAO.listar();
        return "/admin/encuestas?faces-redirect=true";
    }

    public void cargarEncuesta() {
        if (encuestaId == null) {
            return;
        }
        Encuesta cargada = encuestaDAO.buscarPorId(encuestaId);
        if (cargada == null) {
            return;
        }
        encuestaActual = cargada;
        List<String> textos = new ArrayList<>();
        for (Opcion o : cargada.getOpciones()) {
            textos.add(o.getTexto());
        }
        while (textos.size() < MIN_OPCIONES) {
            textos.add("");
        }
        opcionesTexto = textos;
        limpiarMensajes();
    }

    public void eliminar(int id) {
        limpiarMensajes();
        encuestaDAO.eliminar(id);
        mensajeExito = "Encuesta eliminada.";
        encuestas = encuestaDAO.listar();
    }

    public void toggleActiva(Encuesta encuesta) {
        encuesta.setActiva(!encuesta.isActiva());
        encuestaDAO.actualizarEstado(encuesta);
    }

    private void limpiarMensajes() {
        mensajeError = null;
        mensajeExito = null;
    }

    private static List<String> nuevasOpcionesVacias() {
        List<String> lista = new ArrayList<>();
        for (int i = 0; i < MIN_OPCIONES; i++) {
            lista.add("");
        }
        return lista;
    }

    public List<Encuesta> getEncuestas() { return encuestas; }
    public void setEncuestas(List<Encuesta> encuestas) { this.encuestas = encuestas; }

    public Encuesta getEncuestaActual() { return encuestaActual; }
    public void setEncuestaActual(Encuesta encuestaActual) { this.encuestaActual = encuestaActual; }

    public Integer getEncuestaId() { return encuestaId; }
    public void setEncuestaId(Integer encuestaId) { this.encuestaId = encuestaId; }

    public List<String> getOpcionesTexto() { return opcionesTexto; }
    public void setOpcionesTexto(List<String> opcionesTexto) { this.opcionesTexto = opcionesTexto; }

    public String getMensajeError() { return mensajeError; }
    public void setMensajeError(String mensajeError) { this.mensajeError = mensajeError; }

    public String getMensajeExito() { return mensajeExito; }
    public void setMensajeExito(String mensajeExito) { this.mensajeExito = mensajeExito; }
}
