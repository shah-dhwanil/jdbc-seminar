package com.seminar.jdbc.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Central place for plain-DriverManager connections.
 *
 * <p>Best practices: credentials come from the environment (never hardcoded),
 * every caller uses try-with-resources so the Connection is always closed,
 * and this class holds no mutable state.
 */
public final class DbConfig {

    private DbConfig() {
    }

    public static String url() {
        return env("DB_URL", "jdbc:postgresql://localhost:5432/jdbcdb");
    }

    public static String user() {
        return env("DB_USER", "jdbcuser");
    }

    public static String password() {
        return env("DB_PASSWORD", "jdbcpass");
    }

    /** Opens a new physical connection. Caller MUST use try-with-resources. */
    public static Connection getConnection() throws SQLException {
        // Since JDBC 4 (Java 6+) the driver self-registers; no Class.forName needed.
        return DriverManager.getConnection(url(), user(), password());
    }

    /** Prints config without leaking the password (seminar-friendly). */
    public static void printConfig() {
        System.out.println("DB_URL=" + url() + " DB_USER=" + user() + " DB_PASSWORD=****");
    }

    private static String env(String name, String def) {
        String v = System.getenv(name);
        return (v == null || v.isBlank()) ? def : v;
    }
}
