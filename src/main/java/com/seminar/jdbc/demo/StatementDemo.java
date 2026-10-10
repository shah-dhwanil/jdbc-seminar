package com.seminar.jdbc.demo;

import com.seminar.jdbc.config.DbConfig;
import com.seminar.jdbc.util.SqlErrors;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 1 - Statement: static SQL with no user input.
 *
 * <p>Statement is fine when the SQL text is fixed. The moment any user input
 * is concatenated into it, switch to PreparedStatement (see demo 2 and 3).
 */
public final class StatementDemo {

    private StatementDemo() {
    }

    public static void run() {
        System.out.println("\n--- 1. Statement (static SQL) ---");
        String select = "SELECT id, name FROM students ORDER BY id";
        try (Connection con = DbConfig.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(select)) {

            ResultSetMetaData md = rs.getMetaData();
            System.out.println("Columns: " + md.getColumnName(1) + ", " + md.getColumnName(2));

            int rows = 0;
            while (rs.next()) {
                // get by index AND by name: both are 1-based for indexes.
                System.out.println("  id=" + rs.getInt(1) + " name=" + rs.getString("name"));
                rows++;
            }
            System.out.println("executeQuery() -> ResultSet with " + rows + " row(s).");

            if (rs.wasNull()) {
                System.out.println("(last column read was SQL NULL)");
            }
        } catch (SQLException e) {
            SqlErrors.print(e);
            return;
        }

        // executeUpdate() returns affected-row count for INSERT/UPDATE/DELETE.
        try (Connection con = DbConfig.getConnection();
             Statement st = con.createStatement()) {
            int inserted = st.executeUpdate(
                    "INSERT INTO students (id, name) VALUES (999, 'Temp_Statement') ON CONFLICT (id) DO NOTHING");
            System.out.println("executeUpdate(INSERT) -> " + inserted + " row(s).");
            int deleted = st.executeUpdate("DELETE FROM students WHERE id = 999");
            System.out.println("executeUpdate(DELETE) -> " + deleted + " row(s). (cleanup)");
        } catch (SQLException e) {
            SqlErrors.print(e);
        }
    }
}
