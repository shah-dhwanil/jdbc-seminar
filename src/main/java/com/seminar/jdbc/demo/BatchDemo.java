package com.seminar.jdbc.demo;

import com.seminar.jdbc.config.DbConfig;
import com.seminar.jdbc.util.SqlErrors;

import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;

/**
 * 4 - Batch with PreparedStatement: addBatch() N times, one executeBatch().
 * One network round trip instead of N. INSERT/UPDATE/DELETE only, no SELECT.
 */
public final class BatchDemo {

    private BatchDemo() {
    }

    public static void run() {
        System.out.println("\n--- 4. Batch execution (PreparedStatement) ---");
        String[][] rows = {
            {"101", "Asha"}, {"102", "Ravi"}, {"103", "Meera"},
            {"104", "Kiran"}, {"105", "Divya"},
        };
        String sql = "INSERT INTO students (id, name) VALUES (?, ?)";

        try (Connection con = DbConfig.getConnection()) {
            // Whole batch commits or rolls back together.
            con.setAutoCommit(false);

            // Clear previous demo rows so re-runs never hit PK conflicts.
            try (PreparedStatement clean = con.prepareStatement("DELETE FROM students WHERE id >= 100")) {
                clean.executeUpdate();
            }

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                for (String[] r : rows) {
                    ps.setInt(1, Integer.parseInt(r[0]));
                    ps.setString(2, r[1]);
                    ps.addBatch(); // buffered client-side, nothing sent yet
                }
                // Real loops: flush every ~500-1000 rows to bound memory.
                int[] counts = ps.executeBatch(); // single trip to the DB
                con.commit();
                System.out.println("executeBatch() -> " + Arrays.toString(counts)
                        + " total=" + Arrays.stream(counts).sum() + " row(s), committed.");
            } catch (BatchUpdateException bue) {
                con.rollback();
                System.err.println("Batch failed, rolled back. Partial counts: "
                        + Arrays.toString(bue.getUpdateCounts()));
                SqlErrors.print(bue);
                return;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true); // restore default for pooled reuse
            }
        } catch (SQLException e) {
            SqlErrors.print(e);
        }
    }
}
