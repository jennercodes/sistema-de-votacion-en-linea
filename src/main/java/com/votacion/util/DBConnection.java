package com.votacion.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Utilidad de conexión a MySQL.
 *
 * Las credenciales se leen de variables de entorno con fallback a valores por
 * defecto pensados para desarrollo local:
 *   DB_URL       (default: jdbc:mysql://localhost:3306/votacion_db)
 *   DB_USER      (default: root)
 *   DB_PASSWORD  (default: cadena vacía)
 */
public class DBConnection {

    private static final String DEFAULT_DB_URL =
            "jdbc:mysql://localhost:3306/votacion_db";
    private static final String DEFAULT_DB_USER = "root";
    private static final String DEFAULT_DB_PASSWORD = "";

    private static final String DB_URL = envOrDefault("DB_URL", DEFAULT_DB_URL);
    private static final String DB_USER = envOrDefault("DB_USER", DEFAULT_DB_USER);
    private static final String DB_PASSWORD = envOrDefault("DB_PASSWORD", DEFAULT_DB_PASSWORD);

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("No se pudo cargar el driver de MySQL", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    private static String envOrDefault(String name, String fallback) {
        String value = System.getenv(name);
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
