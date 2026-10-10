# jdbc-seminar — JDBC demo project (PostgreSQL + Docker)

Menu-driven demos for a 30-min JDBC seminar: `Statement`, SQL injection,
`PreparedStatement`, batch, `CallableStatement`, transactions, plus a HikariCP bonus.

## 1. Prerequisites

Java 17+, Maven 3.8+, Docker + Docker Compose.

## 2. Start PostgreSQL

```bash
docker compose up -d
docker compose ps              # db should be healthy
```

Container `jdbc-postgres` runs `postgres:16-alpine`, database `jdbcdb`,
seeded from `db/init.sql` (users, students, accounts + procedure/function).
Override via `.env` (see `.env.example`):
`POSTGRES_DB / POSTGRES_USER / POSTGRES_PASSWORD / DB_PORT / DB_URL / DB_USER / DB_PASSWORD`.

Fresh start:

```bash
docker compose down -v   # wipes pgdata
docker compose up -d
```

## 3. Run the menu

```bash
mvn -q compile exec:java
```

```
1 Statement (static SQL)
2 SQL injection: Statement vs PreparedStatement
3 PreparedStatement (safe login)
4 Batch execution (PreparedStatement)
5 CallableStatement (procedure + function)
6 Transaction (transfer, commit/rollback)
7 HikariCP pool (bonus)
8 Reset seed data
0 Exit
```

Non-interactive smoke run (no typing, defaults for every prompt):

```bash
mvn -q compile exec:java -Dexec.args="auto"
```

Use option **8** before each live demo so re-runs are deterministic.

## 4. Live injection script (demo 2)

1. Choose `2`, log in normally: user `ravi`, password `pass123` → 1 row.
2. Re-run with username `' OR '1'='1' -- ` (note trailing space), any password →
   every user row returns (bypass: `--` comments out the password check).
3. Note the canned attack + safe `PreparedStatement` comparison printed right after:
   same evil input matches 0 rows because `?` binds it as data, not code.

## 5. What each demo proves

| # | Demo | Key calls |
|---|------|-----------|
| 1 | `StatementDemo` | `executeQuery` → `ResultSet`, `executeUpdate` → row count, `getMetaData`, `wasNull` |
| 2 | `SqlInjectionDemo` | vulnerable concatenation vs `?` binding with identical input |
| 3 | `PreparedStatementDemo` | `setString` (1-based), `executeQuery`, `RETURN_GENERATED_KEYS` |
| 4 | `BatchDemo` | `addBatch` / `executeBatch` in one transaction, `BatchUpdateException` → `rollback` |
| 5 | `CallableDemo` | `{call get_student_by_id(?, ?)}` OUT param, `{? = call count_students()}`, `wasNull` |
| 6 | `TransactionDemo` | `setAutoCommit(false)`, `SELECT ... FOR UPDATE`, debit+credit, `commit`/`rollback` |
| 7 | `HikariCpDemo` | one `HikariDataSource`, borrow/use/`close`-returns-to-pool |

## 6. Best practices used

`try-with-resources` everywhere · `?` placeholders (except the labelled vuln demo) ·
no hardcoded credentials (env vars) · explicit `commit`/`rollback` + `setAutoCommit(true)`
restore · `SELECT ... FOR UPDATE` + `READ_COMMITTED` in transfers ·
`BatchUpdateException` handling · `SQLState`/error-code logging, never silent catch.

## 7. Layout

```
docker-compose.yml  postgres + init.sql mount + healthcheck
db/init.sql         schema, seed, procedure/function
pom.xml             postgresql driver, HikariCP, slf4j-simple
src/main/java/com/seminar/jdbc/
  Main.java  menu + auto mode
  config/DbConfig.java  DriverManager from env
  config/DatabaseSeeder.java  reset seed (option 8)
  demo/StatementDemo.java  demo/SqlInjectionDemo.java
  demo/PreparedStatementDemo.java  demo/BatchDemo.java
  demo/CallableDemo.java  demo/TransactionDemo.java  demo/HikariCpDemo.java
  util/SqlErrors.java
```

---

## SEMINAR NOTES (original)

# JDBC — How Java talks to your database

## Two worlds, one gap
Java app (Core Java) ⇄ ? Database (DBMS + SQL). Every database speaks its own
protocol. **JDBC** is the one standard way to bridge them.

## What is JDBC?
Java application → JDBC API (java.sql · javax.sql) → JDBC Driver (vendor .jar,
the "adapter") → MySQL / Oracle / PostgreSQL. Same code, any database: swap
only the driver.

## Four driver types
Type 1 JDBC-ODBC bridge (removed in Java 8) · Type 2 Native API · Type 3 Network
protocol · Type 4 Thin, pure Java — use this (e.g. PostgreSQL JDBC).

## 5 steps, every time
1 Load driver (`Class.forName`, automatic since JDBC 4) → 2 Connect
(`getConnection`) → 3 Create statement → 4 Execute → 5 Close (try-with-resources).

## Core objects
DriverManager → Connection → Statement → ResultSet. Each one creates the next.

## Statement / PreparedStatement / CallableStatement
- `Statement`: static SQL, injection risk.
- `PreparedStatement`: SQL with `?` parameters, safe + precompiled. Default choice.
- `CallableStatement`: stored procedures, `{call proc(?)}`.

## SQL injection
Input `' OR '1'='1` pasted into concatenated SQL makes `WHERE` always true.
With `PreparedStatement` the same input is just a string → no match.

## ResultSet
Cursor: `next/previous/first/last/absolute(n)`; read via `getInt("id")` /
`getString(2)`; types `FORWARD_ONLY` (default), `SCROLL_INSENSITIVE`,
`SCROLL_SENSITIVE`; `CONCUR_UPDATABLE`; `getMetaData()`; `wasNull()` for NULL.

## Which method runs what?
`SELECT` → `executeQuery()` → `ResultSet`. `INSERT/UPDATE/DELETE` →
`executeUpdate()` → rows affected. Many rows → batch.

## Batch
`addBatch()` × N then one `executeBatch()` → 1 round trip, returns `int[]`.
Flush every 500–1000 rows; `catch BatchUpdateException` + rollback.

## Transactions
`setAutoCommit(false)` → debit → credit → `commit()` / `rollback()`. ACID in code.

## Pooling
Without pool: open → query → close per request (slow). With pool: borrow → use →
return. Use `DataSource` with HikariCP.

## Do / Don't
Do: PreparedStatement, try-with-resources, pool connections, commit/rollback.
Don't: concatenate SQL, hardcode passwords, leave connections open, ignore SQLException.
