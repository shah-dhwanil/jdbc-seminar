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
        cfg.setMaximumPoolSize(5);
        cfg.setMinimumIdle(2);
        cfg.setConnectionTimeout(5_000);

        try (HikariDataSource ds = new HikariDataSource(cfg)) {
            for (int i = 1; i <= 3; i++) {
                // getConnection() borrows; close() below hands it back to the pool.
                try (Connection con = ds.getConnection();
                     PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM students");
                     ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    System.out.println("  borrow #" + i + ": students=" + rs.getInt(1)
                            + " (pool active=" + ds.getHikariPoolMXBean().getActiveConnections()
                            + ", idle=" + ds.getHikariPoolMXBean().getIdleConnections() + ")");
                }
            }
            System.out.println("Pool closed (ds.close()). Rule: app owns ONE DataSource; "
                    + "threads only borrow/return per request.");
        } catch (SQLException e) {
            System.err.println("Pool demo failed: " + e.getMessage());
        }
    }
}
