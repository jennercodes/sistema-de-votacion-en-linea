package com.votacion.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Utilidad de conexión a la base de datos.
 *
 * <p>Las credenciales se leen exclusivamente de variables de entorno; no hay
 * valores por defecto inseguros (antes se asumía {@code root} sin contraseña).
 * Si falta alguna variable requerida, la clase falla al inicializar con un
 * mensaje explícito en lugar de conectarse en silencio con credenciales
 * adivinadas.</p>
 *
 * <p>El fallback a H2 en memoria es <b>opt-in</b>: solo se activa cuando
 * {@code DB_FALLBACK_H2=true}. En caso contrario, un fallo de conexión a MySQL
 * se propaga tal cual, sin ocultar el problema real.</p>
 *
 * <h2>Variables de entorno</h2>
 * <ul>
 *   <li>{@code DB_URL} — URL JDBC de MySQL (requerida, no vacía).</li>
 *   <li>{@code DB_USER} — usuario MySQL (requerida, no vacía).</li>
 *   <li>{@code DB_PASSWORD} — contraseña MySQL (requerida; puede ser vacía,
 *       pero debe estar definida explícitamente).</li>
 *   <li>{@code DB_FALLBACK_H2} — {@code true} para permitir el fallback a H2 en
 *       memoria si MySQL no responde (opcional, por defecto desactivado).</li>
 * </ul>
 */
public class DBConnection {

    private static final String DB_URL = requireEnv("DB_URL");
    private static final String DB_USER = requireEnv("DB_USER");
    private static final String DB_PASSWORD = requireEnvAllowEmpty("DB_PASSWORD");
    private static final boolean FALLBACK_H2 = envBool("DB_FALLBACK_H2");

    private static boolean useH2 = false;

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("No se pudo cargar el driver de MySQL", e);
        }
        if (FALLBACK_H2) {
            try {
                Class.forName("org.h2.Driver");
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(
                        "DB_FALLBACK_H2=true pero el driver de H2 no está en el classpath", e);
            }
        }
    }

    public static Connection getConnection() throws SQLException {
        if (useH2) {
            return getH2Connection();
        }
        try {
            return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        } catch (SQLException e) {
            if (!FALLBACK_H2) {
                // Sin fallback opt-in: no ocultar el fallo real de MySQL.
                throw e;
            }
            System.err.println("Alerta: No se pudo conectar a MySQL (" + e.getMessage()
                    + "). DB_FALLBACK_H2=true → activando fallback a base de datos H2 en memoria.");
            useH2 = true;
            return getH2Connection();
        }
    }

    private static Connection getH2Connection() throws SQLException {
        String h2Url = "jdbc:h2:mem:votacion_db;MODE=MySQL;DB_CLOSE_DELAY=-1;"
                + "INIT=RUNSCRIPT FROM 'classpath:schema_h2.sql'";
        return DriverManager.getConnection(h2Url, "sa", "");
    }

    /** Requiere una variable de entorno presente y no vacía. */
    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Falta la variable de entorno requerida: " + name
                    + ". Configura DB_URL, DB_USER y DB_PASSWORD antes de arrancar la aplicación.");
        }
        return value;
    }

    /** Requiere una variable de entorno definida; admite valor vacío (p. ej. contraseña vacía). */
    private static String requireEnvAllowEmpty(String name) {
        String value = System.getenv(name);
        if (value == null) {
            throw new IllegalStateException(
                    "Falta la variable de entorno requerida: " + name
                    + " (puede ser vacía, pero debe estar definida explícitamente).");
        }
        return value;
    }

    private static boolean envBool(String name) {
        return "true".equalsIgnoreCase(System.getenv(name));
    }
}
