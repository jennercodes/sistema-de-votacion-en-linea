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
                    insertarOpciones(conn, encuesta.getId(), opciones);
                } else {
                    actualizar(conn, encuesta);
                    // Reconciliar en sitio en lugar de borrar + reinsertar: así se
                    // conservan los IDs de las opciones que se mantienen y, con ellos,
                    // los votos ya emitidos (borrar opciones los eliminaría en cascada).
                    reconciliarOpciones(conn, encuesta.getId(), opciones);
                }
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

    /**
     * Actualiza las opciones de una encuesta existente <b>sin destruir los votos</b>.
     *
     * <p>Empareja los textos entrantes con las opciones existentes por posición
     * (orden): las coincidentes se actualizan con {@code UPDATE} conservando su
     * {@code id} —y por tanto sus votos—; si hay más textos que opciones se
     * insertan las nuevas; si hay menos, se eliminan las sobrantes (y solo esos
     * votos se pierden en cascada, que es justo lo esperado al quitar una opción).</p>
     *
     * <p>Limitación conocida: si el administrador reordena las opciones, los votos
     * quedan asociados a la opción que ocupe cada posición. Aceptable para el alcance
     * del curso y muy preferible a borrar todos los votos en cada edición.</p>
     */
    private void reconciliarOpciones(Connection conn, int encuestaId, List<String> opciones) throws SQLException {
        // Textos válidos (mismo criterio que insertarOpciones: se ignoran los vacíos)
        List<String> textos = new ArrayList<>();
        for (String t : opciones) {
            if (t != null && !t.isBlank()) {
                textos.add(t.trim());
            }
        }

        // IDs de las opciones existentes, en orden
        List<Integer> existentes = new ArrayList<>();
        String sel = "SELECT id FROM opciones WHERE encuesta_id = ? ORDER BY orden ASC, id ASC";
        try (PreparedStatement ps = conn.prepareStatement(sel)) {
            ps.setInt(1, encuestaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    existentes.add(rs.getInt("id"));
                }
            }
        }

        int comunes = Math.min(existentes.size(), textos.size());

        // 1) Actualizar en sitio las que se mantienen (conserva id -> conserva votos)
        String upd = "UPDATE opciones SET texto = ?, orden = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(upd)) {
            for (int i = 0; i < comunes; i++) {
                ps.setString(1, textos.get(i));
                ps.setInt(2, i + 1);
                ps.setInt(3, existentes.get(i));
                ps.addBatch();
            }
            ps.executeBatch();
        }

        // 2) Insertar las nuevas (si ahora hay más opciones)
        if (textos.size() > existentes.size()) {
            String ins = "INSERT INTO opciones (encuesta_id, texto, orden) VALUES (?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(ins)) {
                for (int i = existentes.size(); i < textos.size(); i++) {
                    ps.setInt(1, encuestaId);
                    ps.setString(2, textos.get(i));
                    ps.setInt(3, i + 1);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }

        // 3) Eliminar las sobrantes (si ahora hay menos): solo esas pierden sus votos
        if (existentes.size() > textos.size()) {
            String del = "DELETE FROM opciones WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(del)) {
                for (int i = textos.size(); i < existentes.size(); i++) {
                    ps.setInt(1, existentes.get(i));
                    ps.addBatch();
                }
                ps.executeBatch();
            }
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
