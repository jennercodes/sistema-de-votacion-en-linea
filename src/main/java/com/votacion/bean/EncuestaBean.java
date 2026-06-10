package com.votacion.bean;

import com.votacion.dao.CategoriaDAO;
import com.votacion.dao.EncuestaDAO;
import com.votacion.model.Categoria;
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
    private final CategoriaDAO categoriaDAO = new CategoriaDAO();

    private List<Encuesta> encuestas = new ArrayList<>();
    private List<Categoria> categorias = new ArrayList<>();
    private Encuesta encuestaActual = new Encuesta();
    private Integer encuestaId;
    private Integer categoriaSeleccionadaId;
    private List<String> opcionesTexto = nuevasOpcionesVacias();
    private String mensajeError;
    private String mensajeExito;

    @PostConstruct
    public void init() {
        encuestas = encuestaDAO.listar();
        categorias = categoriaDAO.listar();
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

        if (categoriaSeleccionadaId != null && categoriaSeleccionadaId > 0) {
            Categoria cat = new Categoria();
            cat.setId(categoriaSeleccionadaId);
            encuestaActual.setCategoria(cat);
        } else {
            encuestaActual.setCategoria(null);
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
        if (cargada.getCategoria() != null) {
            categoriaSeleccionadaId = cargada.getCategoria().getId();
        } else {
            categoriaSeleccionadaId = null;
        }
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

    public int getTotalEncuestas() {
        return encuestas.size();
    }

    public long getEncuestasActivas() {
        return encuestas.stream()
                .filter(Encuesta::isActiva)
                .count();
    }

    public long getEncuestasInactivas() {
        return encuestas.stream()
                .filter(e -> !e.isActiva())
                .count();
    }

    public boolean puedeAgregarOpcion() {
        return opcionesTexto.size() < MAX_OPCIONES;
    }

    public boolean puedeEliminarOpcion() {
        return opcionesTexto.size() > MIN_OPCIONES;
    }

    public List<Encuesta> getEncuestas() { return encuestas; }
    public void setEncuestas(List<Encuesta> encuestas) { this.encuestas = encuestas; }

    public List<Categoria> getCategorias() { return categorias; }
    public void setCategorias(List<Categoria> categorias) { this.categorias = categorias; }

    public Encuesta getEncuestaActual() { return encuestaActual; }
    public void setEncuestaActual(Encuesta encuestaActual) { this.encuestaActual = encuestaActual; }

    public Integer getEncuestaId() { return encuestaId; }
    public void setEncuestaId(Integer encuestaId) { this.encuestaId = encuestaId; }

    public Integer getCategoriaSeleccionadaId() { return categoriaSeleccionadaId; }
    public void setCategoriaSeleccionadaId(Integer categoriaSeleccionadaId) { this.categoriaSeleccionadaId = categoriaSeleccionadaId; }

    public List<String> getOpcionesTexto() { return opcionesTexto; }
    public void setOpcionesTexto(List<String> opcionesTexto) { this.opcionesTexto = opcionesTexto; }

    public String getMensajeError() { return mensajeError; }
    public void setMensajeError(String mensajeError) { this.mensajeError = mensajeError; }

    public String getMensajeExito() { return mensajeExito; }
    public void setMensajeExito(String mensajeExito) { this.mensajeExito = mensajeExito; }
}
