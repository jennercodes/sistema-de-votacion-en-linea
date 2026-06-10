package com.votacion.dao;

import com.votacion.model.Usuario;
import com.votacion.util.DBConnection;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.time.LocalDateTime;

public class UsuarioDAO {

    public Usuario buscarPorUsername(String username) {
        String sql = "SELECT id, username, password_hash, email, rol, fecha_creacion FROM usuario WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Usuario user = new Usuario();
                    user.setId(rs.getInt("id"));
                    user.setUsername(rs.getString("username"));
                    user.setPasswordHash(rs.getString("password_hash"));
                    user.setEmail(rs.getString("email"));
                    user.setRol(rs.getString("rol"));
                    Timestamp ts = rs.getTimestamp("fecha_creacion");
                    if (ts != null) {
                        user.setFechaCreacion(ts.toLocalDateTime());
                    }
                    return user;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar usuario por username: " + username, e);
        }
        return null;
    }

    public void registrar(Usuario usuario) {
        // Generamos el hash de la contraseña usando BCrypt antes de guardar
        String hashed = BCrypt.hashpw(usuario.getPasswordHash(), BCrypt.gensalt(10));
        
        String sql = "INSERT INTO usuario (username, password_hash, email, rol) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, usuario.getUsername());
            ps.setString(2, hashed);
            ps.setString(3, usuario.getEmail());
            ps.setString(4, usuario.getRol() != null ? usuario.getRol() : "VOTANTE");
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    usuario.setId(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al registrar usuario: " + usuario.getUsername(), e);
        }
    }

    public boolean existeUsername(String username) {
        String sql = "SELECT COUNT(*) FROM usuario WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al verificar existencia de username: " + username, e);
        }
        return false;
    }

    public boolean existeEmail(String email) {
        String sql = "SELECT COUNT(*) FROM usuario WHERE email = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al verificar existencia de email: " + email, e);
        }
        return false;
    }
}
