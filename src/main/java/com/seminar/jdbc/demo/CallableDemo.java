package com.seminar.jdbc.demo;

import com.seminar.jdbc.config.DbConfig;
import com.seminar.jdbc.util.SqlErrors;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;

/**
 * 5 - CallableStatement: calls stored routines with {call ...}.
 * Routines live in the DB (see db/init.sql); Java just binds IN/OUT params.
 * Note: get_student_by_id is a FUNCTION because the PostgreSQL JDBC driver
 * translates the {call ...} escape into SELECT, which only functions support.
 */
public final class CallableDemo {

    private CallableDemo() {
    }

    public static void run() {
        System.out.println("\n--- 5. CallableStatement (stored function + OUT params) ---");

        // Function with IN + OUT: {call get_student_by_id(?, ?)}
        try (Connection con = DbConfig.getConnection();
             CallableStatement cs = con.prepareCall("{call get_student_by_id(?, ?)}")) {
            cs.setInt(1, 1); // IN p_id
            cs.registerOutParameter(2, Types.VARCHAR); // OUT p_name
            cs.execute();
            System.out.println("call get_student_by_id(1) -> p_name=" + cs.getString(2));
        } catch (SQLException e) {
            SqlErrors.print(e);
        }

        // Missing key returns NULL OUT (wasNull check = best practice).
        try (Connection con = DbConfig.getConnection();
             CallableStatement cs = con.prepareCall("{call get_student_by_id(?, ?)}")) {
            cs.setInt(1, 9999);
            cs.registerOutParameter(2, Types.VARCHAR);
            cs.execute();
            String name = cs.getString(2);
            System.out.println("call get_student_by_id(9999) -> "
                    + (cs.wasNull() ? "NULL (no such student)" : name));
        } catch (SQLException e) {
            SqlErrors.print(e);
        }

        // Scalar function: {? = call count_students()}
        try (Connection con = DbConfig.getConnection();
             CallableStatement cs = con.prepareCall("{ ? = call count_students() }")) {
            cs.registerOutParameter(1, Types.INTEGER);
            cs.execute();
            System.out.println("count_students() -> " + cs.getInt(1) + " student(s).");
        } catch (SQLException e) {
            SqlErrors.print(e);
        }
    }
}
