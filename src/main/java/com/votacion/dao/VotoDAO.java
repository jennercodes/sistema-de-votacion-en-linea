package com.votacion.dao;

import com.votacion.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Acceso a datos para votos.
 */
public class VotoDAO {

    public void registrar(int encuestaId, int opcionId, String nombreVotante) {
        String sql = "INSERT INTO votos (encuesta_id, opcion_id, nombre_votante) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, encuestaId);
            ps.setInt(2, opcionId);
            if (nombreVotante == null || nombreVotante.isBlank()) {
                ps.setNull(3, java.sql.Types.VARCHAR);
            } else {
                ps.setString(3, nombreVotante);
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al registrar voto", e);
        }
    }

    public Map<String, Integer> obtenerResultados(int encuestaId) {
        String sql =
                "SELECT o.texto AS opcion, COUNT(v.id) AS total " +
                "FROM opciones o " +
                "LEFT JOIN votos v ON v.opcion_id = o.id " +
                "WHERE o.encuesta_id = ? " +
                "GROUP BY o.id, o.texto " +
                "ORDER BY total DESC, o.orden ASC";

        LinkedHashMap<String, Integer> resultados = new LinkedHashMap<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, encuestaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultados.put(rs.getString("opcion"), rs.getInt("total"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener resultados de encuesta " + encuestaId, e);
        }
        return resultados;
    }
}
