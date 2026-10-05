-- Strip fractional seconds from attendance TIME columns.
-- Hibernate 6.2 (HHH-17229) fails reading SQL Server TIME values with
-- fractional seconds: mssql-jdbc returns java.sql.Time anchored at
-- 1900-01-01, so Hibernate derives a negative nano-of-second and throws
-- "Invalid value for NanoOfSecond". Whole-second values are safe.

UPDATE dbo.attendance
SET check_in_time = DATEADD(SECOND, DATEDIFF(SECOND, 0, check_in_time), 0)
WHERE check_in_time IS NOT NULL
  AND CAST(check_in_time AS TIME(0)) <> check_in_time;

UPDATE dbo.attendance
SET check_out_time = DATEADD(SECOND, DATEDIFF(SECOND, 0, check_out_time), 0)
WHERE check_out_time IS NOT NULL
  AND CAST(check_out_time AS TIME(0)) <> check_out_time;
