package com.votacion.dao;

import com.votacion.model.Categoria;
import com.votacion.model.Encuesta;
import com.votacion.model.Opcion;
import com.votacion.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EncuestaDAO {

    public List<Encuesta> listar() {
        String sql =
                "SELECT e.id, e.titulo, e.descripcion, e.activa, e.fecha_creacion, e.fecha_fin, " +
                "       c.id AS categoria_id, c.nombre AS categoria_nombre, c.descripcion AS categoria_desc, " +
                "       o.id AS opcion_id, o.texto AS opcion_texto, o.orden AS opcion_orden " +
                "FROM encuesta e " +
                "LEFT JOIN categoria c ON e.categoria_id = c.id " +
                "LEFT JOIN opciones o ON o.encuesta_id = e.id " +
                "ORDER BY e.fecha_creacion DESC, e.id DESC, o.orden ASC";

        Map<Integer, Encuesta> porId = new LinkedHashMap<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id");
                Encuesta encuesta = porId.get(id);
                if (encuesta == null) {
                    encuesta = new Encuesta();
                    encuesta.setId(id);
                    encuesta.setTitulo(rs.getString("titulo"));
                    encuesta.setDescripcion(rs.getString("descripcion"));
                    encuesta.setActiva(rs.getBoolean("activa"));
                    Timestamp ts = rs.getTimestamp("fecha_creacion");
                    if (ts != null) {
                        encuesta.setFechaCreacion(ts.toLocalDateTime());
                    }
                    int catId = rs.getInt("categoria_id");
                    if (!rs.wasNull()) {
                        Categoria cat = new Categoria(catId, rs.getString("categoria_nombre"), rs.getString("categoria_desc"));
                        encuesta.setCategoria(cat);
                    }

                    Timestamp tf = rs.getTimestamp("fecha_fin");
                    if (tf != null) {
                        encuesta.setFechaFin(tf.toLocalDateTime());
                    }
                    porId.put(id, encuesta);
                }

                int opcionId = rs.getInt("opcion_id");
                if (!rs.wasNull()) {
                    encuesta.getOpciones().add(new Opcion(
                            opcionId,
                            rs.getString("opcion_texto"),
                            rs.getInt("opcion_orden")));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error al listar encuestas", e);
        }

        return new ArrayList<>(porId.values());
    }

    public Encuesta buscarPorId(int id) {
        String sql =
                "SELECT e.id, e.titulo, e.descripcion, e.activa, e.fecha_creacion, e.fecha_fin, " +
                "       c.id AS categoria_id, c.nombre AS categoria_nombre, c.descripcion AS categoria_desc, " +
                "       o.id AS opcion_id, o.texto AS opcion_texto, o.orden AS opcion_orden " +
                "FROM encuesta e " +
                "LEFT JOIN categoria c ON e.categoria_id = c.id " +
                "LEFT JOIN opciones o ON o.encuesta_id = e.id " +
                "WHERE e.id = ? " +
                "ORDER BY o.orden ASC";

        Encuesta encuesta = null;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (encuesta == null) {
                        encuesta = new Encuesta();
                        encuesta.setId(rs.getInt("id"));
                        encuesta.setTitulo(rs.getString("titulo"));
                        encuesta.setDescripcion(rs.getString("descripcion"));
                        encuesta.setActiva(rs.getBoolean("activa"));
                        Timestamp ts = rs.getTimestamp("fecha_creacion");
                        if (ts != null) {
                            encuesta.setFechaCreacion(ts.toLocalDateTime());
                        }
                        int catId = rs.getInt("categoria_id");
                        if (!rs.wasNull()) {
                            Categoria cat = new Categoria(catId, rs.getString("categoria_nombre"), rs.getString("categoria_desc"));
                            encuesta.setCategoria(cat);
                        }

                        Timestamp tf = rs.getTimestamp("fecha_fin");
                        if (tf != null) {
                            encuesta.setFechaFin(tf.toLocalDateTime());
                        }
                    }

                    int opcionId = rs.getInt("opcion_id");
                    if (!rs.wasNull()) {
                        encuesta.getOpciones().add(new Opcion(
                                opcionId,
                                rs.getString("opcion_texto"),
                                rs.getInt("opcion_orden")));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar encuesta " + id, e);
        }

        return encuesta;
    }

    public void guardar(Encuesta encuesta, List<String> opciones) {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (encuesta.getId() == 0) {
                    insertar(conn, encuesta);
                } else {
                    actualizar(conn, encuesta);
                    borrarOpciones(conn, encuesta.getId());
                }
                insertarOpciones(conn, encuesta.getId(), opciones);
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw new RuntimeException("Error al guardar la encuesta", ex);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener conexión para guardar la encuesta", e);
        }
    }

    private void insertar(Connection conn, Encuesta encuesta) throws SQLException {
        String sql = "INSERT INTO encuesta (titulo, descripcion, activa, categoria_id, fecha_fin) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, encuesta.getTitulo());
            ps.setString(2, encuesta.getDescripcion());
            ps.setBoolean(3, encuesta.isActiva());
            if (encuesta.getCategoria() == null || encuesta.getCategoria().getId() == 0) {
                ps.setNull(4, java.sql.Types.INTEGER);
            } else {
                ps.setInt(4, encuesta.getCategoria().getId());
            }
            if (encuesta.getFechaFin() != null) {
                ps.setTimestamp(5, java.sql.Timestamp.valueOf(encuesta.getFechaFin()));
            } else {
                ps.setNull(5, java.sql.Types.TIMESTAMP);
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    encuesta.setId(keys.getInt(1));
                }
            }
        }
    }

    private void actualizar(Connection conn, Encuesta encuesta) throws SQLException {
        String sql = "UPDATE encuesta SET titulo = ?, descripcion = ?, activa = ?, categoria_id = ?, fecha_fin = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, encuesta.getTitulo());
            ps.setString(2, encuesta.getDescripcion());
            ps.setBoolean(3, encuesta.isActiva());
            if (encuesta.getCategoria() == null || encuesta.getCategoria().getId() == 0) {
                ps.setNull(4, java.sql.Types.INTEGER);
            } else {
                ps.setInt(4, encuesta.getCategoria().getId());
            }
            if (encuesta.getFechaFin() != null) {
                ps.setTimestamp(5, java.sql.Timestamp.valueOf(encuesta.getFechaFin()));
            } else {
                ps.setNull(5, java.sql.Types.TIMESTAMP);
            }
            ps.setInt(6, encuesta.getId());
            ps.executeUpdate();
        }
    }

    private void borrarOpciones(Connection conn, int encuestaId) throws SQLException {
        String sql = "DELETE FROM opciones WHERE encuesta_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, encuestaId);
            ps.executeUpdate();
        }
    }

    private void insertarOpciones(Connection conn, int encuestaId, List<String> opciones) throws SQLException {
        String sql = "INSERT INTO opciones (encuesta_id, texto, orden) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int orden = 1;
            for (String texto : opciones) {
                if (texto == null || texto.isBlank()) {
                    continue;
                }
                ps.setInt(1, encuestaId);
                ps.setString(2, texto);
                ps.setInt(3, orden++);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public void eliminar(int id) {
        String sql = "DELETE FROM encuesta WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar la encuesta " + id, e);
        }
    }

    public void actualizarEstado(Encuesta encuesta) {
        String sql = "UPDATE encuesta SET activa = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, encuesta.isActiva());
            ps.setInt(2, encuesta.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar estado de la encuesta " + encuesta.getId(), e);
        }
    }
}
