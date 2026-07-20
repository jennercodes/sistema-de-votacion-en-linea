package com.votacion.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Registro de una acción administrativa (bitácora de auditoría).
 * Corresponde a la tabla {@code auditoria_admin}.
 */
public class AuditoriaAdmin implements Serializable {

    private int id;
    private int usuarioId;
    private String username;   // resuelto por JOIN con usuario (para mostrar)
    private String accion;
    private String detalles;
    private LocalDateTime fecha;

    public AuditoriaAdmin() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUsuarioId() { return usuarioId; }
    public void setUsuarioId(int usuarioId) { this.usuarioId = usuarioId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getAccion() { return accion; }
    public void setAccion(String accion) { this.accion = accion; }

    public String getDetalles() { return detalles; }
    public void setDetalles(String detalles) { this.detalles = detalles; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}
