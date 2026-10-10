package com.seminar.jdbc.util;

import java.sql.SQLException;

/** Shared SQLException reporter: never swallow exceptions silently. */
public final class SqlErrors {

    private SqlErrors() {
    }

    public static void print(SQLException e) {
        System.err.println("SQL FAILED: " + e.getMessage());
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof SQLException se) {
                System.err.println("  SQLState=" + se.getSQLState()
                        + " code=" + se.getErrorCode()
                        + " msg=" + se.getMessage());
            }
        }
    }
}
