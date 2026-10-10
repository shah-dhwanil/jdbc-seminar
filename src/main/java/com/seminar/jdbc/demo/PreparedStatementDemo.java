package com.seminar.jdbc.demo;

import com.seminar.jdbc.config.DbConfig;
import com.seminar.jdbc.util.SqlErrors;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

/**
 * 3 - PreparedStatement: SQL with ? placeholders, values bound as data.
 * Precompiled by the DB, immune to injection, and faster for repeated use.
 */
public final class PreparedStatementDemo {

    private PreparedStatementDemo() {
    }

    public static void run() {
        runWith("ravi", "pass123");
    }

    public static void run(Scanner sc) {
        System.out.print("Username [ravi]: ");
        String u = sc.nextLine().trim();
        System.out.print("Password [pass123]: ");
        String p = sc.nextLine();
        runWith(u.isEmpty() ? "ravi" : u, p.isEmpty() ? "pass123" : p);
    }

    private static void runWith(String user, String pass) {
        System.out.println("\n--- 3. PreparedStatement (safe login + insert) ---");

        // 1) Parameterised SELECT: input can never change the SQL structure.
        String login = "SELECT id, username FROM users WHERE username = ? AND password = ?";
        try (Connection con = DbConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(login)) {
            ps.setString(1, user);
            ps.setString(2, pass);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    System.out.println("Welcome " + rs.getString("username") + " (id=" + rs.getInt("id") + ")");
                } else {
                    System.out.println("Invalid login for username='" + user + "'");
                }
            }
        } catch (SQLException e) {
            SqlErrors.print(e);
            return;
        }

        // 2) Parameterised INSERT with generated keys, cleaned up afterwards.
        String demoUser = "demo_" + (System.currentTimeMillis() % 100000);
        String insert = "INSERT INTO users (username, password) VALUES (?, ?)";
        try (Connection con = DbConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, demoUser);
            ps.setString(2, "tmp");
            int rows = ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                long id = keys.next() ? keys.getLong(1) : -1;
                System.out.println("executeUpdate(INSERT) -> " + rows + " row(s), generated id=" + id);
            }
            try (PreparedStatement del = con.prepareStatement("DELETE FROM users WHERE username = ?")) {
                del.setString(1, demoUser);
                System.out.println("cleanup DELETE -> " + del.executeUpdate() + " row(s).");
            }
        } catch (SQLException e) {
            SqlErrors.print(e);
        }
    }
}
