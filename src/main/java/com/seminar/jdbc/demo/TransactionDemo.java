package com.seminar.jdbc.demo;

import com.seminar.jdbc.config.DbConfig;
import com.seminar.jdbc.util.SqlErrors;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

/**
 * 6 - Transaction: money transfer is all-or-nothing (ACID).
 * setAutoCommit(false) -> debit + credit -> commit(); any failure -> rollback().
 * SELECT ... FOR UPDATE locks both rows so concurrent transfers can't interleave.
 */
public final class TransactionDemo {

    private TransactionDemo() {
    }

    public static void run() {
        runWith(new BigDecimal("200.00"), new BigDecimal("999999.00"));
    }

    public static void run(Scanner sc) {
        System.out.print("Transfer amount Alice(1) -> Bob(2) [200.00]: ");
        String line = sc.nextLine().trim();
        BigDecimal amount;
        try {
            amount = line.isEmpty() ? new BigDecimal("200.00") : new BigDecimal(line);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                System.out.println("Amount must be positive, using 200.00.");
                amount = new BigDecimal("200.00");
            }
        } catch (NumberFormatException e) {
            System.out.println("Bad number, using 200.00.");
            amount = new BigDecimal("200.00");
        }
        runWith(amount, new BigDecimal("999999.00"));
    }

    private static void runWith(BigDecimal okAmount, BigDecimal failAmount) {
        System.out.println("\n--- 6. Transaction (all-or-nothing transfer) ---");
        printBalances("before");

        boolean ok = transfer(1, 2, okAmount);
        System.out.println("transfer " + okAmount + " -> " + (ok ? "COMMITTED." : "ROLLED BACK."));
        printBalances("after success");

        boolean bad = transfer(1, 2, failAmount);
        System.out.println("transfer " + failAmount + " -> "
                + (bad ? "COMMITTED (unexpected!)." : "ROLLED BACK (balances unchanged)."));
        printBalances("after failure");
    }

    /** Returns true on commit, false on rollback. */
    static boolean transfer(int fromId, int toId, BigDecimal amount) {
        try (Connection con = DbConfig.getConnection()) {
            con.setAutoCommit(false);
            con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            try {
                BigDecimal fromBal = lockAndRead(con, fromId);
                BigDecimal toBal = lockAndRead(con, toId);
                if (fromBal == null || toBal == null) {
                    throw new SQLException("Account not found (from=" + fromId + ", to=" + toId + ")");
                }
                if (fromBal.compareTo(amount) < 0) {
                    throw new SQLException("Insufficient funds: balance=" + fromBal + ", need=" + amount);
                }
                try (PreparedStatement debit = con.prepareStatement(
                             "UPDATE accounts SET balance = balance - ? WHERE id = ?");
                     PreparedStatement credit = con.prepareStatement(
                             "UPDATE accounts SET balance = balance + ? WHERE id = ?")) {
                    debit.setBigDecimal(1, amount);
                    debit.setInt(2, fromId);
                    credit.setBigDecimal(1, amount);
                    credit.setInt(2, toId);
                    if (debit.executeUpdate() != 1 || credit.executeUpdate() != 1) {
                        throw new SQLException("Transfer touched != 1 row per account");
                    }
                }
                con.commit();
                return true;
            } catch (SQLException e) {
                con.rollback(); // <-- the whole point: debit is undone too
                System.out.println("  rolled back: " + e.getMessage());
                return false;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            SqlErrors.print(e);
            return false;
        }
    }

    private static BigDecimal lockAndRead(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT balance FROM accounts WHERE id = ? FOR UPDATE")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : null;
            }
        }
    }

    private static void printBalances(String when) {
        try (Connection con = DbConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT id, owner, balance FROM accounts ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            System.out.println("balances " + when + ":");
            while (rs.next()) {
                System.out.println("  id=" + rs.getInt("id") + " " + rs.getString("owner")
                        + " balance=" + rs.getBigDecimal("balance"));
            }
        } catch (SQLException e) {
            SqlErrors.print(e);
        }
    }
}
