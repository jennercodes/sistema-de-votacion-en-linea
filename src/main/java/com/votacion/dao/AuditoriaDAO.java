package com.votacion.dao;

import com.votacion.model.AuditoriaAdmin;
import com.votacion.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos para la bitácora de auditoría de administradores.
 */
public class AuditoriaDAO {

    /**
     * Registra una acción administrativa. Un fallo aquí NO debe interrumpir la
     * operación principal (que ya se completó), así que se traga la excepción y
     * solo la reporta por consola.
     */
    public void registrar(int usuarioId, String accion, String detalles) {
        String sql = "INSERT INTO auditoria_admin (usuario_id, accion, detalles) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            ps.setString(2, accion);
            ps.setString(3, detalles);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("No se pudo registrar la auditoría (" + accion + "): " + e.getMessage());
        }
    }

    /** Lista la bitácora completa, de lo más reciente a lo más antiguo. */
    public List<AuditoriaAdmin> listar() {
        String sql =
                "SELECT a.id, a.usuario_id, u.username, a.accion, a.detalles, a.fecha " +
                "FROM auditoria_admin a " +
                "JOIN usuario u ON u.id = a.usuario_id " +
                "ORDER BY a.fecha DESC, a.id DESC";

        List<AuditoriaAdmin> registros = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                AuditoriaAdmin a = new AuditoriaAdmin();
                a.setId(rs.getInt("id"));
                a.setUsuarioId(rs.getInt("usuario_id"));
                a.setUsername(rs.getString("username"));
                a.setAccion(rs.getString("accion"));
                a.setDetalles(rs.getString("detalles"));
                Timestamp ts = rs.getTimestamp("fecha");
                if (ts != null) {
                    a.setFecha(ts.toLocalDateTime());
                }
                registros.add(a);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar la auditoría", e);
        }
        return registros;
    }
}
