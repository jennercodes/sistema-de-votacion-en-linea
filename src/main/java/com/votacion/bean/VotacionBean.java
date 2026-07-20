package com.votacion.bean;

import com.votacion.dao.EncuestaDAO;
import com.votacion.dao.VotoDAO;
import com.votacion.model.Encuesta;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.sql.SQLException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Named
@ViewScoped
public class VotacionBean implements Serializable {

    private final EncuestaDAO encuestaDAO = new EncuestaDAO();
    private final VotoDAO votoDAO = new VotoDAO();

    @Inject
    private LoginBean loginBean;

    private List<Encuesta> encuestas = new ArrayList<>();
    private Encuesta encuestaActual;
    private Integer encuestaId;
    private Integer opcionSeleccionadaId;
    private String nombreVotante;
    private Map<String, Integer> resultados = new LinkedHashMap<>();
    private boolean votoRegistrado;
    private String mensajeError;

    /** Cuántas encuestas se muestran en el panel "destacadas" del inicio. */
    private static final int MAX_DESTACADAS = 3;

    /** Encuestas destacadas (top por votos) con sus resultados en vivo para el inicio. */
    private List<EncuestaDestacada> destacadas = new ArrayList<>();

    @PostConstruct
    public void init() {
        encuestas = new ArrayList<>(encuestaDAO.listar().stream()
                .filter(Encuesta::isVigente)
                .toList());
        actualizarDestacadas();
    }

    /**
     * Recalcula las encuestas destacadas: las {@link #MAX_DESTACADAS} activas con
     * más votos totales, cada una con el conteo y porcentaje por opción.
     *
     * <p>Lo invoca el {@code p:poll} del inicio cada pocos segundos, de modo que las
     * barras se refrescan en (casi) tiempo real conforme llegan votos, sin WebSockets.
     * Usa una única consulta batch ({@code obtenerResultadosPorEncuestas}) para evitar
     * N+1 al pintar el panel.</p>
     */
    public void actualizarDestacadas() {
        List<Integer> ids = encuestas.stream().map(Encuesta::getId).toList();
        Map<Integer, LinkedHashMap<String, Integer>> mapa =
                votoDAO.obtenerResultadosPorEncuestas(ids);

        List<EncuestaDestacada> lista = new ArrayList<>();
        for (Encuesta e : encuestas) {
            LinkedHashMap<String, Integer> res =
                    mapa.getOrDefault(e.getId(), new LinkedHashMap<>());
            int total = res.values().stream().mapToInt(Integer::intValue).sum();
            int maximo = res.values().stream().mapToInt(Integer::intValue).max().orElse(0);
            List<OpcionVoto> ops = new ArrayList<>();
            for (Map.Entry<String, Integer> en : res.entrySet()) {
                boolean lider = maximo > 0 && en.getValue() == maximo;
                ops.add(new OpcionVoto(en.getKey(), en.getValue(),
                        obtenerPorcentaje(en.getValue(), total), lider));
            }
            lista.add(new EncuestaDestacada(e, total, ops));
        }
        lista.sort(Comparator.comparingInt(EncuestaDestacada::getTotalVotos).reversed());
        destacadas = lista.stream().limit(MAX_DESTACADAS).toList();
    }

    public void cargarEncuesta() {
        if (encuestaId == null) {
            return;
        }
        encuestaActual = encuestaDAO.buscarPorId(encuestaId);
        opcionSeleccionadaId = null;
        votoRegistrado = false;
        mensajeError = null;
        if (loginBean != null && loginBean.isLoggedIn() && encuestaActual != null) {
            if (votoDAO.haVotado(loginBean.getUsuario().getId(), encuestaActual.getId())) {
                resultados = votoDAO.obtenerResultados(encuestaActual.getId());
                votoRegistrado = true;
            }
        }
        if (encuestaActual != null && !encuestaActual.isVigente()) {
            resultados = votoDAO.obtenerResultados(encuestaActual.getId());
        }
    }

    public void cargarResultados() {
        cargarEncuesta();
        if (encuestaActual != null) {
            resultados = votoDAO.obtenerResultados(encuestaActual.getId());
        }
    }

    public void votar() {
        mensajeError = null;

        if (encuestaActual == null || !encuestaActual.isVigente()) {
            mensajeError = "Esta encuesta ya ha finalizado y no acepta más votos.";
            return;
        }

        if (loginBean == null || !loginBean.isLoggedIn()) {
            mensajeError = "Debes iniciar sesión para poder votar.";
            return;
        }

        if (opcionSeleccionadaId == null) {
            mensajeError = "Debes seleccionar una opción antes de votar.";
            return;
        }

        try {
            int usuarioId = loginBean.getUsuario().getId();
            votoDAO.registrar(usuarioId, encuestaActual.getId(), opcionSeleccionadaId);
            resultados = votoDAO.obtenerResultados(encuestaActual.getId());
            votoRegistrado = true;
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062 || "23000".equals(e.getSQLState())) {
                mensajeError = "Ya has votado en esta encuesta. No se permite el voto duplicado.";
                resultados = votoDAO.obtenerResultados(encuestaActual.getId());
                votoRegistrado = true;
            } else {
                mensajeError = "Error al registrar el voto: " + e.getMessage();
            }
        }
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

    public List<EncuestaDestacada> getDestacadas() { return destacadas; }

    /**
     * Encuesta destacada para el inicio: la encuesta más el total de votos y el
     * desglose por opción ya calculado (texto, votos, porcentaje). Solo lectura.
     */
    public static class EncuestaDestacada implements Serializable {
        private final Encuesta encuesta;
        private final int totalVotos;
        private final List<OpcionVoto> opciones;

        public EncuestaDestacada(Encuesta encuesta, int totalVotos, List<OpcionVoto> opciones) {
            this.encuesta = encuesta;
            this.totalVotos = totalVotos;
            this.opciones = opciones;
        }

        public Encuesta getEncuesta() { return encuesta; }
        public int getTotalVotos() { return totalVotos; }
        public List<OpcionVoto> getOpciones() { return opciones; }
    }

    /** Una opción con su conteo y porcentaje ya calculado, para pintar la barra. */
    public static class OpcionVoto implements Serializable {
        private final String texto;
        private final int votos;
        private final double porcentaje;
        private final boolean lider;

        public OpcionVoto(String texto, int votos, double porcentaje, boolean lider) {
            this.texto = texto;
            this.votos = votos;
            this.porcentaje = porcentaje;
            this.lider = lider;
        }

        public String getTexto() { return texto; }
        public int getVotos() { return votos; }
        public double getPorcentaje() { return porcentaje; }
        public boolean isLider() { return lider; }
    }
}
