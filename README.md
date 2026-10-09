# jdbc-seminar

SEMESTER 5 SEMINAR

# JDBC

How Java talks to your database

\[Your name\] · \[Course / Semester\] · 30 min

## Two worlds, one gap

Java appCore Java

⇄ ?

DatabaseDBMS + SQL

Every database speaks its own protocol.

**JDBC** is the one standard way to bridge them.

## What is JDBC?

Java application

↓

JDBC APIjava.sql · javax.sql

↓

JDBC Drivervendor .jar, the "adapter"

→

MySQL

Oracle

PostgreSQL

Same code, any database: swap only the driver.

## Four driver types

Type 1JDBC-ODBC bridgeremoved in Java 8

Type 2Native APIneeds native libs

Type 3Network protocolneeds middleware

Type 4Thin, pure Java✓ use this

Examples: MySQL Connector/J, PostgreSQL JDBC.

## 5 steps, every time

1Load driver

→

2Connect

→

3Create statement

→

4Execute

→

5Close

Class.forName

getConnection

createStatement

executeQuery

close

Since JDBC 4, step 1 is automatic.

## First program

```
// 2 connect · 3 statement · 4 execute (auto-closed)
try (Connection con = DriverManager.getConnection(url, user, pass);
     Statement st = con.createStatement();
     ResultSet rs = st.executeQuery("SELECT * FROM students")) {
  while (rs.next())
    System.out.println(rs.getInt("id") + " " + rs.getString("name"));
}
```

jdbc:mysql://localhost:3306/college protocol · database · host:port · schema

## The core objects

DriverManagerfinds the driver

→

Connectionsession with the DB

→

Statementcarries your SQL

→

ResultSetrows that come back

Each one creates the next.

## Statement: example with input data

```
String name = "Ravi", pw = "pass123";   // input data
String sql = "SELECT * FROM users "
    + "WHERE name='" + name + "' AND pw='" + pw + "'";
try (Statement st = con.createStatement();
     ResultSet rs = st.executeQuery(sql)) {
  if (rs.next()) System.out.println("Welcome " + rs.getString("name"));
  else System.out.println("Invalid login");
}
```

SQL sent to the DB: SELECT * FROM users WHERE name='Ravi' AND pw='pass123'

Input is pasted into the SQL text. Works, but it is unsafe.

## Pick the right statement

StatementStatic SQLinjection risk

PreparedStatementSQL with **?** parameterssafe + precompiled

CallableStatementStored procedures{call proc(?)}

Default choice: **PreparedStatement**.

## SQL injection

Login input: ' OR '1'='1

Concatenated SQL

```
WHERE name='' OR '1'='1'
```

condition is always true → logs in as anyone

vs

PreparedStatement

```
WHERE name=?   ← bound as data
```

input is just a string → no match

## PreparedStatement: same example, now safe

```
String name = "Ravi", pw = "pass123";   // same input data
String sql = "SELECT * FROM users WHERE name=? AND pw=?";
try (PreparedStatement ps = con.prepareStatement(sql)) {
  ps.setString(1, name);
  ps.setString(2, pw);
  try (ResultSet rs = ps.executeQuery()) {
    if (rs.next()) System.out.println("Welcome " + rs.getString("name"));
    else System.out.println("Invalid login");
  }
}
```

1\. SQL with **?** is sent and precompiled

2\. setString binds values (1-based index)

3\. Input can never change the SQL

## ResultSet: a moving cursor

|  | id | name |
| --- | --- | --- |
| before first |  |  |
| ▶ | 1 | Asha |
| ▶ now | 2 | Ravi |
|  | 3 | Meera |

| Topic | Details |
| --- | --- |
| **Move** | next() previous() first() last() absolute(n) |
| **Read** | getInt("id") or getString(2): by name or 1-based index |
| **Type** | FORWARD_ONLY (default), SCROLL_INSENSITIVE, SCROLL_SENSITIVE |
| **Update** | CONCUR_UPDATABLE: updateString() then updateRow() |
| **Describe** | getMetaData(): column count, names, types |
| **Watch out** | wasNull() for SQL NULL; close it (try-with-resources) |

## Which method runs what?

SELECT

→

executeQuery()

→

ResultSet

INSERT · UPDATE · DELETE

→

executeUpdate()

→

rows affected (int)

Many rows at once? Batch operations are next.

## Batch operations: group the work

One by oneJava → DB, 1000 times→ → → → → → →1000 network round trips

BatchaddBatch() ×1000 (kept in memory)↓executeBatch() sends all together1 round trip

Works with **Statement** and **PreparedStatement**

INSERT · UPDATE · DELETE only, no SELECT

Returns int\[\]: count per statement

## Batch example: insert many students

```
con.setAutoCommit(false);   // all rows succeed or none
String sql = "INSERT INTO students(id, name) VALUES (?, ?)";
try (PreparedStatement ps = con.prepareStatement(sql)) {
  ps.setInt(1, 101); ps.setString(2, "Asha");  ps.addBatch();
  ps.setInt(1, 102); ps.setString(2, "Ravi");  ps.addBatch();
  ps.setInt(1, 103); ps.setString(2, "Meera"); ps.addBatch();
  int[] counts = ps.executeBatch();   // one trip to the DB
  con.commit();
}
```

Tip: in a real loop, call executeBatch() every 500 to 1000 rows. On failure, catch BatchUpdateException and rollback.

## Transactions: all or nothing

setAutoCommit(false)

→

debit A−500

→

credit B+500

→

commit()both worked

rollback()any failure

ACID from your DBMS course, now in Java code.

## Connection pooling

Without a poolopen → query → closeon every request: slow

With a pool● ● ● ● ready connectionsborrow → use → return

Use DataSource with HikariCP or Apache DBCP.

## Where JDBC fits

Hibernate / JPAmaps objects to tables

Spring JdbcTemplateless boilerplate

JDBCthe foundation

Database

Learn JDBC first: every framework above runs on it.

## Do and don't

Do

PreparedStatement

try-with-resources

Pool connections

Commit or rollback

Don't

Concatenate SQL

Hardcode passwords

Leave connections open

Ignore SQLException

## Go deeper

**Oracle** The Java Tutorials: JDBC Database Access

**Book** Core Java Vol. II, Cay Horstmann (JDBC chapter)

**Book** Database System Concepts, Silberschatz et al.

**Docs** java.sql API · MySQL Connector/J · PostgreSQL JDBC

**Pooling** HikariCP README

**Spring** JdbcTemplate reference

**Tutorials** Baeldung · GeeksforGeeks · Jenkov

Questions?
