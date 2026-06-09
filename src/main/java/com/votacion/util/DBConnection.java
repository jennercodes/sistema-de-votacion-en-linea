package com.votacion.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Utilidad de conexión a MySQL con fallback a H2 en memoria si falla.
 */
public class DBConnection {

    private static final String DEFAULT_DB_URL =
            "jdbc:mysql://localhost:3306/votacion_db";
    private static final String DEFAULT_DB_USER = "root";
    private static final String DEFAULT_DB_PASSWORD = "";

    private static final String DB_URL = envOrDefault("DB_URL", DEFAULT_DB_URL);
    private static final String DB_USER = envOrDefault("DB_USER", DEFAULT_DB_USER);
    private static final String DB_PASSWORD = envOrDefault("DB_PASSWORD", DEFAULT_DB_PASSWORD);

    private static boolean useH2 = false;

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("No se pudo cargar el driver de MySQL", e);
        }
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            // Ignorado si H2 no está en el classpath
        }
    }

    public static Connection getConnection() throws SQLException {
        if (useH2) {
            return getH2Connection();
        }
        try {
            return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        } catch (SQLException e) {
            System.err.println("Alerta: No se pudo conectar a MySQL (" + e.getMessage() + "). Activando fallback a base de datos H2 en memoria.");
            useH2 = true;
            return getH2Connection();
        }
    }

    private static Connection getH2Connection() throws SQLException {
        String h2Url = "jdbc:h2:mem:votacion_db;MODE=MySQL;DB_CLOSE_DELAY=-1;INIT=RUNSCRIPT FROM 'classpath:schema_h2.sql'";
        return DriverManager.getConnection(h2Url, "sa", "");
    }

    private static String envOrDefault(String name, String fallback) {
        String value = System.getenv(name);
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
