package com.votacion.bean;

import com.votacion.dao.AuditoriaDAO;
import com.votacion.model.AuditoriaAdmin;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Expone la bitácora de auditoría para el panel de administración.
 */
@Named
@ViewScoped
public class AuditoriaBean implements Serializable {

    private final AuditoriaDAO auditoriaDAO = new AuditoriaDAO();

    private List<AuditoriaAdmin> registros = new ArrayList<>();

    @PostConstruct
    public void init() {
        registros = auditoriaDAO.listar();
    }

    /** Color (severidad de PrimeFaces) del tag según el tipo de acción. */
    public String severidad(String accion) {
        if (accion == null) {
            return "info";
        }
        if (accion.startsWith("CREAR")) {
            return "success";
        }
        if (accion.startsWith("ELIMINAR")) {
            return "danger";
        }
        if (accion.startsWith("DESACTIVAR")) {
            return "warning";
        }
        return "info"; // EDITAR / ACTIVAR
    }

    public List<AuditoriaAdmin> getRegistros() { return registros; }
    public int getTotal() { return registros.size(); }
}
