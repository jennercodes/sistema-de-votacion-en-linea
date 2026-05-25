package com.votacion.dao;

import com.votacion.model.Voto;
import com.votacion.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

/**
 * Acceso a datos para votos.
 */
public class VotoDAO {

    // Inserta un voto asociado a la encuesta con id=1
    public void registrar(Voto voto) throws SQLException {
        String sql = "INSERT INTO votos (encuesta_id, opcion, nombre_votante) VALUES (?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, 1); // encuesta inicial
            ps.setString(2, voto.getOpcion());
            ps.setString(3, voto.getNombre());
            ps.executeUpdate();
        }
    }

    public void registrar(int encuestaId, int opcionId, String nombreVotante) {
        throw new UnsupportedOperationException("VotoDAO.registrar pendiente de implementación");
    }

    public Map<String, Integer> obtenerResultados(int encuestaId) {
        throw new UnsupportedOperationException("VotoDAO.obtenerResultados pendiente de implementación");
    }
}
