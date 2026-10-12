package com.seminar.jdbc.demo;

import com.seminar.jdbc.config.DbConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 7 - Connection pooling with HikariCP: borrow a ready connection, use it,
 * close() returns it to the pool (no physical reconnect). Compare with the
 * DriverManager demos where every getConnection() opens a fresh socket.
 */
public final class HikariCpDemo {

    private HikariCpDemo() {
    }

    public static void run() {
        System.out.println("\n--- 7. HikariCP pool (vs DriverManager per-call) ---");
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(DbConfig.url());
        cfg.setUsername(DbConfig.user());
        cfg.setPassword(DbConfig.password());
        cfg.setPoolName("seminar-pool");
        cfg.setMaximumPoolSize(8);
        cfg.setMinimumIdle(3);
        cfg.setConnectionTimeout(5_000);
        cfg.setIdleTimeout(60_000);

        try (HikariDataSource ds = new HikariDataSource(cfg)) {
            // HikariCP fills minimumIdle in the background, so wait for it
            // before measuring; otherwise an immediate borrow sees total=1.
            awaitPoolFill(ds, cfg.getMinimumIdle(), 5_000);
            System.out.println("  pool warmed: " + stats(ds));

            // Hold 3 connections concurrently to prove min-idle=3.
            // Sequential borrow/use/close only ever needs 1 connection.
            int concurrent = 5;
            Connection[] held = new Connection[concurrent];
            try {
                for (int i = 0; i < concurrent; i++) {
                    held[i] = ds.getConnection();
                    System.out.println("  held #" + (i + 1) + ": students=" + countStudents(held[i])
                            + " (" + stats(ds) + ")");
                }
            } finally {
                for (Connection c : held) {
                    if (c != null) {
                        try {
                            c.close();
                        } catch (SQLException ignored) {
                            // Return-to-pool on demo teardown is best-effort.
                        }
                    }
                }
            }
            System.out.println("  after release: " + stats(ds));

            for (int i = 1; i <= 3; i++) {
                // getConnection() borrows; close() below hands it back to the pool.
                try (Connection con = ds.getConnection();
                     PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM students");
                     ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    System.out.println("  borrow #" + i + ": students=" + rs.getInt(1)
                            + " (" + stats(ds) + ")");
                }
            }
            System.out.println("Pool closed (ds.close()). Rule: app owns ONE DataSource; "
                    + "threads only borrow/return per request.");
        } catch (SQLException e) {
            System.err.println("Pool demo failed: " + e.getMessage());
        }
    }

    private static int countStudents(Connection con) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM students");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private static String stats(HikariDataSource ds) {
        return "pool active=" + ds.getHikariPoolMXBean().getActiveConnections()
                + ", idle=" + ds.getHikariPoolMXBean().getIdleConnections()
                + ", total=" + ds.getHikariPoolMXBean().getTotalConnections();
    }

    /** Waits until the pool has filled to minimumIdle (Hikari fills async). */
    private static void awaitPoolFill(HikariDataSource ds, int minimumIdle, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (ds.getHikariPoolMXBean().getTotalConnections() >= minimumIdle) {
                return;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
