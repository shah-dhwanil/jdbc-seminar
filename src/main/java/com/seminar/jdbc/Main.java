package com.seminar.jdbc;

import com.seminar.jdbc.config.DatabaseSeeder;
import com.seminar.jdbc.config.DbConfig;
import com.seminar.jdbc.demo.BatchDemo;
import com.seminar.jdbc.demo.CallableDemo;
import com.seminar.jdbc.demo.HikariCpDemo;
import com.seminar.jdbc.demo.PreparedStatementDemo;
import com.seminar.jdbc.demo.SqlInjectionDemo;
import com.seminar.jdbc.demo.StatementDemo;
import com.seminar.jdbc.demo.TransactionDemo;
import com.seminar.jdbc.util.SqlErrors;

import java.sql.SQLException;
import java.util.Scanner;

/**
 * Menu-driven runner for every JDBC seminar demo.
 * Interactive:  mvn exec:java   (or mvn -q exec:java)
 * Non-interactive smoke run: mvn exec:java -Dexec.args="auto"
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        DbConfig.printConfig();
        if (args.length > 0 && "auto".equalsIgnoreCase(args[0])) {
            autoRun();
            return;
        }
        try (Scanner sc = new Scanner(System.in)) {
            menuLoop(sc);
        }
    }

    /** Runs everything with defaults; used for CI smoke tests. */
    private static void autoRun() {
        System.out.println("AUTO-RUN: all demos with default inputs.");
        try {
            DatabaseSeeder.reset();
        } catch (SQLException e) {
            SqlErrors.print(e);
            System.exit(1);
        }
        StatementDemo.run();
        SqlInjectionDemo.run();
        PreparedStatementDemo.run();
        BatchDemo.run();
        CallableDemo.run();
        TransactionDemo.run();
        HikariCpDemo.run();
        System.out.println("\nAUTO-RUN complete.");
    }

    private static void menuLoop(Scanner sc) {
        while (true) {
            printMenu();
            System.out.print("Choice: ");
            String line;
            try {
                line = sc.nextLine();
            } catch (java.util.NoSuchElementException e) {
                System.out.println("\nEOF, exiting.");
                return;
            }
            switch (line.trim()) {
                case "1" -> StatementDemo.run();
                case "2" -> SqlInjectionDemo.run(sc);
                case "3" -> PreparedStatementDemo.run(sc);
                case "4" -> BatchDemo.run();
                case "5" -> CallableDemo.run();
                case "6" -> TransactionDemo.run(sc);
                case "7" -> HikariCpDemo.run();
                case "8" -> {
                    try {
                        DatabaseSeeder.reset();
                    } catch (SQLException e) {
                        SqlErrors.print(e);
                    }
                }
                case "0", "q", "exit" -> {
                    System.out.println("Bye.");
                    return;
                }
                default -> System.out.println("Pick 0-8.");
            }
        }
    }

    private static void printMenu() {
        System.out.println("""
                ============ JDBC SEMINAR ============
                1 Statement (static SQL)
                2 SQL injection: Statement vs PreparedStatement
                3 PreparedStatement (safe login)
                4 Batch execution (PreparedStatement)
                5 CallableStatement (stored function)
                6 Transaction (transfer, commit/rollback)
                7 HikariCP pool (bonus)
                8 Reset seed data
                0 Exit
                =================================""");
    }
}
