-- JDBC seminar seed schema. Mounted to /docker-entrypoint-initdb.d/ via docker-compose.
-- Idempotent: safe to re-apply on a fresh container start.

DROP TABLE IF EXISTS accounts;
DROP TABLE IF EXISTS students;
DROP TABLE IF EXISTS users;
DROP FUNCTION IF EXISTS count_students();
DROP FUNCTION IF EXISTS get_student_by_id(INT);
DROP PROCEDURE IF EXISTS get_student_by_id(INT, TEXT);

CREATE TABLE users (
  id       SERIAL PRIMARY KEY,
  username TEXT UNIQUE NOT NULL,
  password TEXT NOT NULL
);

CREATE TABLE students (
  id   INT PRIMARY KEY,
  name TEXT NOT NULL
);

CREATE TABLE accounts (
  id      INT PRIMARY KEY,
  owner   TEXT NOT NULL,
  balance NUMERIC(10, 2) NOT NULL CHECK (balance >= 0)
);

INSERT INTO users (username, password) VALUES
  ('ravi',  'pass123'),
  ('asha',  'secret'),
  ('admin', 'admin123');

INSERT INTO students (id, name) VALUES
  (1, 'Asha'),
  (2, 'Ravi'),
  (3, 'Meera');

INSERT INTO accounts (id, owner, balance) VALUES
  (1, 'Alice', 1000.00),
  (2, 'Bob',    500.00);

-- Stored routine for the CallableStatement demo. A FUNCTION (not PROCEDURE)
-- because the PostgreSQL JDBC driver translates the standard {call ...}
-- escape into SELECT, which only functions support. OUT params work the same.
CREATE OR REPLACE FUNCTION get_student_by_id(IN p_id INT, OUT p_name TEXT)
LANGUAGE plpgsql
AS $$
BEGIN
  SELECT s.name INTO p_name FROM students s WHERE s.id = p_id;
  IF NOT FOUND THEN
    p_name := NULL;
  END IF;
END;
$$;

-- Scalar function for the "{ ? = call ... }" CallableStatement form.
CREATE OR REPLACE FUNCTION count_students()
RETURNS INTEGER
LANGUAGE sql STABLE
AS $$ SELECT COUNT(*)::int FROM students $$;
