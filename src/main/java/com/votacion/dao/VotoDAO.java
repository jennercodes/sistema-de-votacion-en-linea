package com.votacion.dao;

import com.votacion.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Acceso a datos para votos.
 */
public class VotoDAO {

    public void registrar(int usuarioId, int encuestaId, int opcionId) throws SQLException {
        String sqlParticipacion = "INSERT INTO registro_participacion (usuario_id, encuesta_id) VALUES (?, ?)";
        String sqlVoto = "INSERT INTO votos (usuario_id, encuesta_id, opcion_id) VALUES (?, ?, ?)";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement psP = conn.prepareStatement(sqlParticipacion)) {
                    psP.setInt(1, usuarioId);
                    psP.setInt(2, encuestaId);
                    psP.executeUpdate();
                }
                try (PreparedStatement psV = conn.prepareStatement(sqlVoto)) {
                    psV.setInt(1, usuarioId);
                    psV.setInt(2, encuestaId);
                    psV.setInt(3, opcionId);
                    psV.executeUpdate();
                }
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex; // Re-throw so the bean can handle duplicate key (code 1062)
            }
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

    /**
     * Obtiene el conteo de votos por opción para varias encuestas en una sola consulta.
     * Evita el problema N+1 al pintar el panel de destacadas del inicio.
     *
     * @return mapa {@code encuestaId → (texto de opción → nº de votos)}. Cada sub-mapa
     *         viene ordenado por votos desc (la opción líder primero), luego por
     *         {@code orden}. Las encuestas sin opciones no aparecen en el mapa.
     */
    public Map<Integer, LinkedHashMap<String, Integer>> obtenerResultadosPorEncuestas(List<Integer> encuestaIds) {
        Map<Integer, LinkedHashMap<String, Integer>> out = new LinkedHashMap<>();
        if (encuestaIds == null || encuestaIds.isEmpty()) {
            return out;
        }

        String placeholders = encuestaIds.stream()
                .map(id -> "?")
                .collect(Collectors.joining(","));
        String sql =
                "SELECT o.encuesta_id AS eid, o.texto AS opcion, COUNT(v.id) AS total " +
                "FROM opciones o " +
                "LEFT JOIN votos v ON v.opcion_id = o.id " +
                "WHERE o.encuesta_id IN (" + placeholders + ") " +
                "GROUP BY o.encuesta_id, o.id, o.texto " +
                "ORDER BY o.encuesta_id ASC, total DESC, o.orden ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < encuestaIds.size(); i++) {
                ps.setInt(i + 1, encuestaIds.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int eid = rs.getInt("eid");
                    out.computeIfAbsent(eid, k -> new LinkedHashMap<>())
                       .put(rs.getString("opcion"), rs.getInt("total"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener resultados por encuestas", e);
        }
        return out;
    }

    public boolean haVotado(int usuarioId, int encuestaId) {
        String sql = "SELECT COUNT(*) FROM registro_participacion WHERE usuario_id = ? AND encuesta_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            ps.setInt(2, encuestaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al verificar participación", e);
        }
        return false;
    }
}
