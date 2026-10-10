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
 * 2 - SQL injection with Statement, then the same attack blocked by
 * PreparedStatement. The vulnerable query is INTENTIONAL here and clearly
 * labelled; never concatenate input into SQL in real code.
 */
public final class SqlInjectionDemo {

    private SqlInjectionDemo() {
    }

    public static void run() {
        runWith("ravi", "pass123");
    }

    public static void run(Scanner sc) {
        System.out.print("Username [ravi] (try: ' OR '1'='1' -- ): ");
        String u = sc.nextLine().trim();
        System.out.print("Password [pass123]: ");
        String p = sc.nextLine();
        runWith(u.isEmpty() ? "ravi" : u, p.isEmpty() ? "pass123" : p);
    }

    private static void runWith(String user, String pass) {
        System.out.println("\n--- 2. SQL injection: Statement (VULNERABLE) vs PreparedStatement (safe) ---");

        // A) What the user typed, pasted straight into SQL. DO NOT DO THIS.
        String vulnSql = "SELECT id, username FROM users WHERE username='"
                + user + "' AND password='" + pass + "'";
        System.out.println("[vulnerable] SQL sent to DB:\n  " + vulnSql);
        System.out.println("[vulnerable] your input rows: " + countWithStatement(vulnSql));

        // B) Canned attack string, always demonstrates the hole on stage.
        // The trailing "-- " comments out the password check, so the OR makes
        // the WHERE clause true for EVERY row. (Without it, AND binds tighter
        // than OR and the attack would fail - a good aside for the seminar.)
        String evilUser = "' OR '1'='1' -- ";
        String evilSql = "SELECT id, username FROM users WHERE username='"
                + evilUser + "' AND password='anything'";
        System.out.println("[vulnerable] attack input username = " + evilUser);
        System.out.println("[vulnerable] SQL sent to DB:\n  " + evilSql);
        int evilRows = countWithStatement(evilSql);
        System.out.println("[vulnerable] attack matched " + evilRows
                + " row(s) -> " + (evilRows > 0 ? "LOGGED IN AS SOMEONE ELSE (injection!)" : "rejected"));

        // C) Same evil input through a placeholder: treated as data, never code.
        String safeSql = "SELECT id, username FROM users WHERE username=? AND password=?";
        int safeRows = 0;
        try (Connection con = DbConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(safeSql)) {
            ps.setString(1, evilUser); // 1-based index; whole string is just data
            ps.setString(2, "anything");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    safeRows++;
                }
            }
        } catch (SQLException e) {
            SqlErrors.print(e);
            return;
        }
        System.out.println("[safe] PreparedStatement with ? placeholders matched "
                + safeRows + " row(s) -> " + (safeRows == 0 ? "attack BLOCKED." : "UNEXPECTED match"));
    }

    private static int countWithStatement(String sql) {
        int rows = 0;
        try (Connection con = DbConfig.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                System.out.println("    row: id=" + rs.getInt("id")
                        + " username=" + rs.getString("username"));
                rows++;
            }
        } catch (SQLException e) {
            SqlErrors.print(e);
        }
        return rows;
    }
}
