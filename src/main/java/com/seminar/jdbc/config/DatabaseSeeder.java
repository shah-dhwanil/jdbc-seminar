package com.seminar.jdbc.config;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Resets demo tables to their seed state so every menu option is repeatable
 * live on stage. Runs in one transaction: all-or-nothing.
 */
public final class DatabaseSeeder {

    private DatabaseSeeder() {
    }

    public static void reset() throws SQLException {
        String[] sql = {
            "DELETE FROM accounts",
            "DELETE FROM students",
            "DELETE FROM users",
            "INSERT INTO users (username, password) VALUES ('ravi','pass123'),('asha','secret'),('admin','admin123')",
            "INSERT INTO students (id, name) VALUES (1,'Asha'),(2,'Ravi'),(3,'Meera')",
            "INSERT INTO accounts (id, owner, balance) VALUES (1,'Alice',1000.00),(2,'Bob',500.00)"
        };
        try (Connection con = DbConfig.getConnection()) {
            con.setAutoCommit(false);
            try (Statement st = con.createStatement()) {
                for (String s : sql) {
                    st.executeUpdate(s);
                }
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
        System.out.println("Seed data reset: 3 users, 3 students, 2 accounts.");
    }
}
