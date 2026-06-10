package com.votacion.dao;

import com.votacion.model.Categoria;
import com.votacion.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategoriaDAO {

    public List<Categoria> listar() {
        String sql = "SELECT id, nombre, descripcion FROM categoria ORDER BY nombre ASC";
        List<Categoria> lista = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new Categoria(
                        rs.getInt("id"),
                        rs.getString("nombre"),
                        rs.getString("descripcion")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar categorías", e);
        }
        return lista;
    }

    public Categoria buscarPorId(int id) {
        String sql = "SELECT id, nombre, descripcion FROM categoria WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Categoria(
                            rs.getInt("id"),
                            rs.getString("nombre"),
                            rs.getString("descripcion")
                    );
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar categoría " + id, e);
        }
        return null;
    }
}
