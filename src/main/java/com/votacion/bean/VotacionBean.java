package com.votacion.bean;

import com.votacion.dao.EncuestaDAO;
import com.votacion.dao.VotoDAO;
import com.votacion.model.Encuesta;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Named
@ViewScoped
public class VotacionBean implements Serializable {

    private final EncuestaDAO encuestaDAO = new EncuestaDAO();
    private final VotoDAO votoDAO = new VotoDAO();

    private List<Encuesta> encuestas = new ArrayList<>();
    private Encuesta encuestaActual;
    private Integer encuestaId;
    private Integer opcionSeleccionadaId;
    private String nombreVotante;
    private Map<String, Integer> resultados = new LinkedHashMap<>();
    private boolean votoRegistrado;
    private String mensajeError;

    @PostConstruct
    public void init() {
        encuestas = encuestaDAO.listar().stream()
                .filter(Encuesta::isActiva)
                .toList();
    }

    public void cargarEncuesta() {
        if (encuestaId == null) {
            return;
        }
        encuestaActual = encuestaDAO.buscarPorId(encuestaId);
        opcionSeleccionadaId = null;
        votoRegistrado = false;
        mensajeError = null;
    }

    public void cargarResultados() {
        cargarEncuesta();
        if (encuestaActual != null) {
            resultados = votoDAO.obtenerResultados(encuestaActual.getId());
        }
    }

    public void votar() {
        mensajeError = null;

        if (opcionSeleccionadaId == null) {
            mensajeError = "Debes seleccionar una opción antes de votar.";
            return;
        }

        votoDAO.registrar(encuestaActual.getId(), opcionSeleccionadaId, nombreVotante);
        resultados = votoDAO.obtenerResultados(encuestaActual.getId());
        votoRegistrado = true;
    }

    public double obtenerPorcentaje(int votos, int total) {
        if (total <= 0) {
            return 0.0;
        }
        return (votos * 100.0) / total;
    }

    public int getTotalVotos() {
        return resultados.values().stream()
                .mapToInt(Integer::intValue)
                .sum();
    }

    public boolean tieneResultados() {
        return !resultados.isEmpty();
    }

    public String obtenerOpcionGanadora() {
        if (resultados.isEmpty()) return "";
        return resultados.entrySet().stream()
                .max((e1, e2) -> e1.getValue().compareTo(e2.getValue()))
                .map(Map.Entry::getKey)
                .orElse("");
    }

    public int obtenerVotoMaximo() {
        if (resultados.isEmpty()) return 0;
        return resultados.values().stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);
    }

    public List<Encuesta> getEncuestas() { return encuestas; }
    public void setEncuestas(List<Encuesta> encuestas) { this.encuestas = encuestas; }

    public Encuesta getEncuestaActual() { return encuestaActual; }
    public void setEncuestaActual(Encuesta encuestaActual) { this.encuestaActual = encuestaActual; }

    public Integer getEncuestaId() { return encuestaId; }
    public void setEncuestaId(Integer encuestaId) { this.encuestaId = encuestaId; }

    public Integer getOpcionSeleccionadaId() { return opcionSeleccionadaId; }
    public void setOpcionSeleccionadaId(Integer opcionSeleccionadaId) { this.opcionSeleccionadaId = opcionSeleccionadaId; }

    public String getNombreVotante() { return nombreVotante; }
    public void setNombreVotante(String nombreVotante) { this.nombreVotante = nombreVotante; }

    public Map<String, Integer> getResultados() { return resultados; }
    public void setResultados(Map<String, Integer> resultados) { this.resultados = resultados; }

    public boolean isVotoRegistrado() { return votoRegistrado; }
    public void setVotoRegistrado(boolean votoRegistrado) { this.votoRegistrado = votoRegistrado; }

    public String getMensajeError() { return mensajeError; }
    public void setMensajeError(String mensajeError) { this.mensajeError = mensajeError; }
}
