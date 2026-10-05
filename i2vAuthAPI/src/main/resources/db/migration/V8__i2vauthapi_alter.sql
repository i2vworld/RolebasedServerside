-- ============================================================
-- i2vAuthAPI Schema Alter Migration V8
-- Updates existing tables to match entity definitions
-- ============================================================
SET QUOTED_IDENTIFIER ON;
SET ARITHABORT ON;

DECLARE @con_name NVARCHAR(200);
DECLARE @con_cursor CURSOR;
DECLARE @fk_name NVARCHAR(200);
DECLARE @fk_cursor CURSOR;
DECLARE @sql NVARCHAR(MAX);

-- ============================================================
-- 1. AUDIT_LOGS
-- ============================================================
IF OBJECT_ID(N'dbo.audit_logs', N'U') IS NOT NULL
BEGIN
    SET @fk_cursor = CURSOR FOR SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA='dbo' AND TABLE_NAME='audit_logs' AND CONSTRAINT_TYPE='FOREIGN KEY';
    OPEN @fk_cursor;
    FETCH NEXT FROM @fk_cursor INTO @fk_name;
    WHILE @@FETCH_STATUS = 0
    BEGIN
        EXEC('ALTER TABLE dbo.audit_logs DROP CONSTRAINT ' + @fk_name);
        FETCH NEXT FROM @fk_cursor INTO @fk_name;
    END;
    CLOSE @fk_cursor;
    DEALLOCATE @fk_cursor;
    DROP TABLE dbo.audit_logs;
END;

CREATE TABLE dbo.audit_logs (
    id              UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID() PRIMARY KEY,
    user_id         BIGINT         NULL,
    action          NVARCHAR(50)   NOT NULL,
    entity_type     NVARCHAR(50)   NOT NULL,
    entity_id       BIGINT         NULL,
    ip_address      NVARCHAR(45)   NULL,
    user_agent      TEXT           NULL,
    request_method  NVARCHAR(10)   NULL,
    request_uri     NVARCHAR(500)  NULL,
    request_params  TEXT           NULL,
    response_status INT            NULL,
    execution_time_ms INT          NULL,
    created_at      DATETIME2      NULL,
    CONSTRAINT FK_audit_logs_user FOREIGN KEY (user_id) REFERENCES dbo.users(id)
);
CREATE INDEX IX_audit_logs_user ON dbo.audit_logs (user_id);
CREATE INDEX IX_audit_logs_action ON dbo.audit_logs (action);

-- ============================================================
-- 2. ATTENDANCE
-- ============================================================
IF OBJECT_ID(N'dbo.attendance', N'U') IS NOT NULL
BEGIN
    SET @con_cursor = CURSOR FOR SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA='dbo' AND TABLE_NAME='attendance' AND CONSTRAINT_TYPE IN ('CHECK', 'UNIQUE');
    OPEN @con_cursor;
    FETCH NEXT FROM @con_cursor INTO @con_name;
    WHILE @@FETCH_STATUS = 0
    BEGIN
        EXEC('ALTER TABLE dbo.attendance DROP CONSTRAINT ' + @con_name);
        FETCH NEXT FROM @con_cursor INTO @con_name;
    END;
    CLOSE @con_cursor;
    DEALLOCATE @con_cursor;

    SET @sql = N'';
    SELECT @sql = @sql + 'DROP INDEX ' + QUOTENAME(name) + ' ON dbo.attendance;'
    FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.attendance') AND is_unique = 1 AND is_primary_key = 0;
    IF @sql <> '' EXEC sp_executesql @sql;

    IF COL_LENGTH(N'dbo.attendance', N'status') IS NOT NULL
        ALTER TABLE dbo.attendance ALTER COLUMN status NVARCHAR(50) NULL;

    IF COL_LENGTH(N'dbo.attendance', N'check_in_ip') IS NOT NULL
        ALTER TABLE dbo.attendance ALTER COLUMN check_in_ip NVARCHAR(45) NULL;

    IF COL_LENGTH(N'dbo.attendance', N'check_out_ip') IS NOT NULL
        ALTER TABLE dbo.attendance ALTER COLUMN check_out_ip NVARCHAR(45) NULL;

    IF COL_LENGTH(N'dbo.attendance', N'check_in_location') IS NOT NULL
        ALTER TABLE dbo.attendance ALTER COLUMN check_in_location NVARCHAR(500) NULL;

    IF COL_LENGTH(N'dbo.attendance', N'check_out_location') IS NOT NULL
        ALTER TABLE dbo.attendance ALTER COLUMN check_out_location NVARCHAR(500) NULL;
END;

-- ============================================================
-- 3. CLEARANCE_STEPS
-- ============================================================
IF OBJECT_ID(N'dbo.clearance_steps', N'U') IS NOT NULL
BEGIN
    SET @fk_cursor = CURSOR FOR SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA='dbo' AND TABLE_NAME='clearance_steps' AND CONSTRAINT_TYPE='FOREIGN KEY';
    OPEN @fk_cursor;
    FETCH NEXT FROM @fk_cursor INTO @fk_name;
    WHILE @@FETCH_STATUS = 0
    BEGIN
        EXEC('ALTER TABLE dbo.clearance_steps DROP CONSTRAINT ' + @fk_name);
        FETCH NEXT FROM @fk_cursor INTO @fk_name;
    END;
    CLOSE @fk_cursor;
    DEALLOCATE @fk_cursor;

    SET @con_cursor = CURSOR FOR SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA='dbo' AND TABLE_NAME='clearance_steps' AND CONSTRAINT_TYPE IN ('CHECK', 'UNIQUE');
    OPEN @con_cursor;
    FETCH NEXT FROM @con_cursor INTO @con_name;
    WHILE @@FETCH_STATUS = 0
    BEGIN
        EXEC('ALTER TABLE dbo.clearance_steps DROP CONSTRAINT ' + @con_name);
        FETCH NEXT FROM @con_cursor INTO @con_name;
    END;
    CLOSE @con_cursor;
    DEALLOCATE @con_cursor;

    SET @sql = N'';
    SELECT @sql = @sql + 'ALTER TABLE dbo.clearance_steps DROP CONSTRAINT ' + QUOTENAME(name) + ';'
    FROM sys.default_constraints WHERE parent_object_id = OBJECT_ID('dbo.clearance_steps');
    IF @sql <> '' EXEC sp_executesql @sql;

    IF COL_LENGTH(N'dbo.clearance_steps', N'assigned_to_employee_id') IS NOT NULL
        ALTER TABLE dbo.clearance_steps DROP COLUMN assigned_to_employee_id;

    IF COL_LENGTH(N'dbo.clearance_steps', N'remarks') IS NOT NULL
        ALTER TABLE dbo.clearance_steps DROP COLUMN remarks;

    IF COL_LENGTH(N'dbo.clearance_steps', N'assigned_to') IS NULL
        ALTER TABLE dbo.clearance_steps ADD assigned_to NVARCHAR(50) NOT NULL DEFAULT '';

    IF COL_LENGTH(N'dbo.clearance_steps', N'step') IS NOT NULL
        ALTER TABLE dbo.clearance_steps ALTER COLUMN step NVARCHAR(200) NOT NULL;

    IF COL_LENGTH(N'dbo.clearance_steps', N'status') IS NOT NULL
        ALTER TABLE dbo.clearance_steps ALTER COLUMN status NVARCHAR(20) NOT NULL;
END;

-- ============================================================
-- 4. DEPARTMENTS
-- ============================================================
IF OBJECT_ID(N'dbo.departments', N'U') IS NOT NULL
BEGIN
    SET @con_cursor = CURSOR FOR SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA='dbo' AND TABLE_NAME='departments' AND CONSTRAINT_TYPE IN ('CHECK', 'UNIQUE');
    OPEN @con_cursor;
    FETCH NEXT FROM @con_cursor INTO @con_name;
    WHILE @@FETCH_STATUS = 0
    BEGIN
        EXEC('ALTER TABLE dbo.departments DROP CONSTRAINT ' + @con_name);
        FETCH NEXT FROM @con_cursor INTO @con_name;
    END;
    CLOSE @con_cursor;
    DEALLOCATE @con_cursor;

    SET @sql = N'';
    SELECT @sql = @sql + 'DROP INDEX ' + QUOTENAME(name) + ' ON dbo.departments;'
    FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.departments') AND is_unique = 1 AND is_primary_key = 0;
    IF @sql <> '' EXEC sp_executesql @sql;

    IF COL_LENGTH(N'dbo.departments', N'is_hierarchy_unit') IS NOT NULL
        ALTER TABLE dbo.departments ALTER COLUMN is_hierarchy_unit BIT NULL;

    IF COL_LENGTH(N'dbo.departments', N'name') IS NOT NULL
        ALTER TABLE dbo.departments ALTER COLUMN name NVARCHAR(100) NOT NULL;

    IF COL_LENGTH(N'dbo.departments', N'code') IS NOT NULL
        ALTER TABLE dbo.departments ALTER COLUMN code NVARCHAR(20) NULL;

    IF COL_LENGTH(N'dbo.departments', N'location') IS NOT NULL
        ALTER TABLE dbo.departments ALTER COLUMN location NVARCHAR(255) NULL;

    IF COL_LENGTH(N'dbo.departments', N'email') IS NOT NULL
        ALTER TABLE dbo.departments ALTER COLUMN email NVARCHAR(100) NULL;

    IF COL_LENGTH(N'dbo.departments', N'phone') IS NOT NULL
        ALTER TABLE dbo.departments ALTER COLUMN phone NVARCHAR(20) NULL;
END;

-- ============================================================
-- 5. DESIGNATIONS
-- ============================================================
IF OBJECT_ID(N'dbo.designations', N'U') IS NOT NULL
BEGIN
    SET @con_cursor = CURSOR FOR SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA='dbo' AND TABLE_NAME='designations' AND CONSTRAINT_TYPE IN ('CHECK', 'UNIQUE');
    OPEN @con_cursor;
    FETCH NEXT FROM @con_cursor INTO @con_name;
    WHILE @@FETCH_STATUS = 0
    BEGIN
        EXEC('ALTER TABLE dbo.designations DROP CONSTRAINT ' + @con_name);
        FETCH NEXT FROM @con_cursor INTO @con_name;
    END;
    CLOSE @con_cursor;
    DEALLOCATE @con_cursor;

    SET @sql = N'';
    SELECT @sql = @sql + 'DROP INDEX ' + QUOTENAME(name) + ' ON dbo.designations;'
    FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.designations') AND is_unique = 1 AND is_primary_key = 0;
    IF @sql <> '' EXEC sp_executesql @sql;

    IF COL_LENGTH(N'dbo.designations', N'title') IS NOT NULL
        ALTER TABLE dbo.designations ALTER COLUMN title NVARCHAR(100) NOT NULL;

    IF COL_LENGTH(N'dbo.designations', N'code') IS NOT NULL
        ALTER TABLE dbo.designations ALTER COLUMN code NVARCHAR(20) NULL;

    IF COL_LENGTH(N'dbo.designations', N'department_id') IS NOT NULL
        ALTER TABLE dbo.designations ALTER COLUMN department_id BIGINT NULL;
END;

-- ============================================================
-- 6. EMPLOYEE_USERS
-- ============================================================
IF OBJECT_ID(N'dbo.employee_users', N'U') IS NOT NULL
BEGIN
    SET @con_cursor = CURSOR FOR SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA='dbo' AND TABLE_NAME='employee_users' AND CONSTRAINT_TYPE IN ('CHECK', 'UNIQUE');
    OPEN @con_cursor;
    FETCH NEXT FROM @con_cursor INTO @con_name;
    WHILE @@FETCH_STATUS = 0
    BEGIN
        EXEC('ALTER TABLE dbo.employee_users DROP CONSTRAINT ' + @con_name);
        FETCH NEXT FROM @con_cursor INTO @con_name;
    END;
    CLOSE @con_cursor;
    DEALLOCATE @con_cursor;

    SET @sql = N'';
    SELECT @sql = @sql + 'DROP INDEX ' + QUOTENAME(name) + ' ON dbo.employee_users;'
    FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.employee_users') AND is_unique = 1 AND is_primary_key = 0;
    IF @sql <> '' EXEC sp_executesql @sql;

    IF COL_LENGTH(N'dbo.employee_users', N'employee_code') IS NOT NULL
        ALTER TABLE dbo.employee_users ALTER COLUMN employee_code NVARCHAR(50) NOT NULL;

    IF COL_LENGTH(N'dbo.employee_users', N'gender') IS NOT NULL
        ALTER TABLE dbo.employee_users ALTER COLUMN gender NVARCHAR(20) NULL;

    IF COL_LENGTH(N'dbo.employee_users', N'employment_type') IS NOT NULL
        ALTER TABLE dbo.employee_users ALTER COLUMN employment_type NVARCHAR(50) NULL;

    IF COL_LENGTH(N'dbo.employee_users', N'employment_status') IS NOT NULL
        ALTER TABLE dbo.employee_users ALTER COLUMN employment_status NVARCHAR(50) NULL;

    IF COL_LENGTH(N'dbo.employee_users', N'seating_location') IS NOT NULL
        ALTER TABLE dbo.employee_users ALTER COLUMN seating_location NVARCHAR(100) NULL;

    IF COL_LENGTH(N'dbo.employee_users', N'shift_timing') IS NOT NULL
        ALTER TABLE dbo.employee_users ALTER COLUMN shift_timing NVARCHAR(50) NULL;

    IF COL_LENGTH(N'dbo.employee_users', N'weekly_off') IS NOT NULL
        ALTER TABLE dbo.employee_users ALTER COLUMN weekly_off NVARCHAR(20) NULL;

    IF COL_LENGTH(N'dbo.employee_users', N'extension') IS NOT NULL
        ALTER TABLE dbo.employee_users ALTER COLUMN extension NVARCHAR(10) NULL;

    IF COL_LENGTH(N'dbo.employee_users', N'work_email') IS NOT NULL
        ALTER TABLE dbo.employee_users ALTER COLUMN work_email NVARCHAR(100) NULL;

    IF COL_LENGTH(N'dbo.employee_users', N'work_phone') IS NOT NULL
        ALTER TABLE dbo.employee_users ALTER COLUMN work_phone NVARCHAR(20) NULL;

    IF COL_LENGTH(N'dbo.employee_users', N'phone_number') IS NOT NULL
        ALTER TABLE dbo.employee_users ALTER COLUMN phone_number NVARCHAR(20) NULL;

    IF COL_LENGTH(N'dbo.employee_users', N'alternate_phone') IS NOT NULL
        ALTER TABLE dbo.employee_users ALTER COLUMN alternate_phone NVARCHAR(20) NULL;

    IF COL_LENGTH(N'dbo.employee_users', N'city') IS NOT NULL
        ALTER TABLE dbo.employee_users ALTER COLUMN city NVARCHAR(50) NULL;

    IF COL_LENGTH(N'dbo.employee_users', N'state') IS NOT NULL
        ALTER TABLE dbo.employee_users ALTER COLUMN state NVARCHAR(50) NULL;

    IF COL_LENGTH(N'dbo.employee_users', N'country') IS NOT NULL
        ALTER TABLE dbo.employee_users ALTER COLUMN country NVARCHAR(50) NULL;

    IF COL_LENGTH(N'dbo.employee_users', N'postal_code') IS NOT NULL
        ALTER TABLE dbo.employee_users ALTER COLUMN postal_code NVARCHAR(20) NULL;

    IF COL_LENGTH(N'dbo.employee_users', N'blood_group') IS NOT NULL
        ALTER TABLE dbo.employee_users ALTER COLUMN blood_group NVARCHAR(5) NULL;

    IF COL_LENGTH(N'dbo.employee_users', N'emergency_contact_name') IS NOT NULL
        ALTER TABLE dbo.employee_users ALTER COLUMN emergency_contact_name NVARCHAR(100) NULL;

    IF COL_LENGTH(N'dbo.employee_users', N'emergency_contact_phone') IS NOT NULL
        ALTER TABLE dbo.employee_users ALTER COLUMN emergency_contact_phone NVARCHAR(20) NULL;

    IF COL_LENGTH(N'dbo.employee_users', N'emergency_contact_relation') IS NOT NULL
        ALTER TABLE dbo.employee_users ALTER COLUMN emergency_contact_relation NVARCHAR(50) NULL;

    IF COL_LENGTH(N'dbo.employee_users', N'bank_account_number') IS NOT NULL
        ALTER TABLE dbo.employee_users ALTER COLUMN bank_account_number NVARCHAR(50) NULL;

    IF COL_LENGTH(N'dbo.employee_users', N'bank_name') IS NOT NULL
        ALTER TABLE dbo.employee_users ALTER COLUMN bank_name NVARCHAR(100) NULL;

    IF COL_LENGTH(N'dbo.employee_users', N'bank_ifsc_code') IS NOT NULL
        ALTER TABLE dbo.employee_users ALTER COLUMN bank_ifsc_code NVARCHAR(20) NULL;

    IF COL_LENGTH(N'dbo.employee_users', N'pan_number') IS NOT NULL
        ALTER TABLE dbo.employee_users ALTER COLUMN pan_number NVARCHAR(10) NULL;

    IF COL_LENGTH(N'dbo.employee_users', N'aadhar_number') IS NOT NULL
        ALTER TABLE dbo.employee_users ALTER COLUMN aadhar_number NVARCHAR(20) NULL;

    IF COL_LENGTH(N'dbo.employee_users', N'pf_number') IS NOT NULL
        ALTER TABLE dbo.employee_users ALTER COLUMN pf_number NVARCHAR(50) NULL;

    IF COL_LENGTH(N'dbo.employee_users', N'esi_number') IS NOT NULL
        ALTER TABLE dbo.employee_users ALTER COLUMN esi_number NVARCHAR(50) NULL;

    IF COL_LENGTH(N'dbo.employee_users', N'uan_number') IS NOT NULL
        ALTER TABLE dbo.employee_users ALTER COLUMN uan_number NVARCHAR(20) NULL;

    IF COL_LENGTH(N'dbo.employee_users', N'kyc_text_url') IS NOT NULL
        ALTER TABLE dbo.employee_users ALTER COLUMN kyc_text_url NVARCHAR(512) NULL;

    IF COL_LENGTH(N'dbo.employee_users', N'pan_front_url') IS NOT NULL
        ALTER TABLE dbo.employee_users ALTER COLUMN pan_front_url NVARCHAR(512) NULL;

    IF COL_LENGTH(N'dbo.employee_users', N'pan_back_url') IS NOT NULL
        ALTER TABLE dbo.employee_users ALTER COLUMN pan_back_url NVARCHAR(512) NULL;

    IF COL_LENGTH(N'dbo.employee_users', N'aadhaar_front_url') IS NOT NULL
        ALTER TABLE dbo.employee_users ALTER COLUMN aadhaar_front_url NVARCHAR(512) NULL;

    IF COL_LENGTH(N'dbo.employee_users', N'aadhaar_back_url') IS NOT NULL
        ALTER TABLE dbo.employee_users ALTER COLUMN aadhaar_back_url NVARCHAR(512) NULL;

    IF COL_LENGTH(N'dbo.employee_users', N'employee_photo_url') IS NOT NULL
        ALTER TABLE dbo.employee_users ALTER COLUMN employee_photo_url NVARCHAR(512) NULL;

    IF COL_LENGTH(N'dbo.employee_users', N'signature_url') IS NOT NULL
        ALTER TABLE dbo.employee_users ALTER COLUMN signature_url NVARCHAR(512) NULL;

    IF COL_LENGTH(N'dbo.employee_users', N'bank_document_url') IS NOT NULL
        ALTER TABLE dbo.employee_users ALTER COLUMN bank_document_url NVARCHAR(512) NULL;
END;

-- ============================================================
-- 7. EXIT_REQUESTS
-- ============================================================
IF OBJECT_ID(N'dbo.exit_requests', N'U') IS NOT NULL
BEGIN
    SET @fk_cursor = CURSOR FOR SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA='dbo' AND TABLE_NAME='exit_requests' AND CONSTRAINT_TYPE='FOREIGN KEY';
    OPEN @fk_cursor;
    FETCH NEXT FROM @fk_cursor INTO @fk_name;
    WHILE @@FETCH_STATUS = 0
    BEGIN
        EXEC('ALTER TABLE dbo.exit_requests DROP CONSTRAINT ' + @fk_name);
        FETCH NEXT FROM @fk_cursor INTO @fk_name;
    END;
    CLOSE @fk_cursor;
    DEALLOCATE @fk_cursor;

    SET @con_cursor = CURSOR FOR SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA='dbo' AND TABLE_NAME='exit_requests' AND CONSTRAINT_TYPE IN ('CHECK', 'UNIQUE');
    OPEN @con_cursor;
    FETCH NEXT FROM @con_cursor INTO @con_name;
    WHILE @@FETCH_STATUS = 0
    BEGIN
        EXEC('ALTER TABLE dbo.exit_requests DROP CONSTRAINT ' + @con_name);
        FETCH NEXT FROM @con_cursor INTO @con_name;
    END;
    CLOSE @con_cursor;
    DEALLOCATE @con_cursor;

    SET @sql = N'';
    SELECT @sql = @sql + 'ALTER TABLE dbo.exit_requests DROP CONSTRAINT ' + QUOTENAME(name) + ';'
    FROM sys.default_constraints WHERE parent_object_id = OBJECT_ID('dbo.exit_requests');
    IF @sql <> '' EXEC sp_executesql @sql;

    IF COL_LENGTH(N'dbo.exit_requests', N'applied_at') IS NOT NULL
        ALTER TABLE dbo.exit_requests ALTER COLUMN applied_at DATETIME2 NOT NULL;

    IF COL_LENGTH(N'dbo.exit_requests', N'reason') IS NOT NULL
        ALTER TABLE dbo.exit_requests ALTER COLUMN reason NVARCHAR(500) NOT NULL;

    IF COL_LENGTH(N'dbo.exit_requests', N'place') IS NOT NULL
        ALTER TABLE dbo.exit_requests ALTER COLUMN place NVARCHAR(100) NOT NULL;

    IF COL_LENGTH(N'dbo.exit_requests', N'status') IS NOT NULL
        ALTER TABLE dbo.exit_requests ALTER COLUMN status NVARCHAR(20) NOT NULL;

    IF COL_LENGTH(N'dbo.exit_requests', N'comments') IS NOT NULL
        ALTER TABLE dbo.exit_requests ALTER COLUMN comments NVARCHAR(1000) NULL;

    IF COL_LENGTH(N'dbo.exit_requests', N'clearance_department') IS NOT NULL
        ALTER TABLE dbo.exit_requests DROP COLUMN clearance_department;

    IF COL_LENGTH(N'dbo.exit_requests', N'clearance_designation') IS NOT NULL
        ALTER TABLE dbo.exit_requests DROP COLUMN clearance_designation;

    IF COL_LENGTH(N'dbo.exit_requests', N'clearance_status') IS NOT NULL
        ALTER TABLE dbo.exit_requests DROP COLUMN clearance_status;

    IF COL_LENGTH(N'dbo.exit_requests', N'cleared_by') IS NOT NULL
        ALTER TABLE dbo.exit_requests DROP COLUMN cleared_by;

    IF COL_LENGTH(N'dbo.exit_requests', N'cleared_at') IS NOT NULL
        ALTER TABLE dbo.exit_requests DROP COLUMN cleared_at;
END;

-- ============================================================
-- 8. HOLIDAYS
-- ============================================================
IF OBJECT_ID(N'dbo.holidays', N'U') IS NOT NULL
BEGIN
    SET @con_cursor = CURSOR FOR SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA='dbo' AND TABLE_NAME='holidays' AND CONSTRAINT_TYPE IN ('CHECK', 'UNIQUE');
    OPEN @con_cursor;
    FETCH NEXT FROM @con_cursor INTO @con_name;
    WHILE @@FETCH_STATUS = 0
    BEGIN
        EXEC('ALTER TABLE dbo.holidays DROP CONSTRAINT ' + @con_name);
        FETCH NEXT FROM @con_cursor INTO @con_name;
    END;
    CLOSE @con_cursor;
    DEALLOCATE @con_cursor;

    SET @sql = N'';
    SELECT @sql = @sql + 'DROP INDEX ' + QUOTENAME(name) + ' ON dbo.holidays;'
    FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.holidays') AND is_unique = 1 AND is_primary_key = 0;
    IF @sql <> '' EXEC sp_executesql @sql;

    IF COL_LENGTH(N'dbo.holidays', N'holiday_name') IS NOT NULL
        ALTER TABLE dbo.holidays ALTER COLUMN holiday_name NVARCHAR(200) NOT NULL;

    IF COL_LENGTH(N'dbo.holidays', N'description') IS NOT NULL
        ALTER TABLE dbo.holidays ALTER COLUMN description NVARCHAR(500) NULL;
END;

-- ============================================================
-- 9. LEAVE_REQUESTS
-- ============================================================
IF OBJECT_ID(N'dbo.leave_requests', N'U') IS NOT NULL
BEGIN
    SET @fk_cursor = CURSOR FOR SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA='dbo' AND TABLE_NAME='leave_requests' AND CONSTRAINT_TYPE='FOREIGN KEY';
    OPEN @fk_cursor;
    FETCH NEXT FROM @fk_cursor INTO @fk_name;
    WHILE @@FETCH_STATUS = 0
    BEGIN
        EXEC('ALTER TABLE dbo.leave_requests DROP CONSTRAINT ' + @fk_name);
        FETCH NEXT FROM @fk_cursor INTO @fk_name;
    END;
    CLOSE @fk_cursor;
    DEALLOCATE @fk_cursor;

    SET @con_cursor = CURSOR FOR SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA='dbo' AND TABLE_NAME='leave_requests' AND CONSTRAINT_TYPE IN ('CHECK', 'UNIQUE');
    OPEN @con_cursor;
    FETCH NEXT FROM @con_cursor INTO @con_name;
    WHILE @@FETCH_STATUS = 0
    BEGIN
        EXEC('ALTER TABLE dbo.leave_requests DROP CONSTRAINT ' + @con_name);
        FETCH NEXT FROM @con_cursor INTO @con_name;
    END;
    CLOSE @con_cursor;
    DEALLOCATE @con_cursor;

    SET @sql = N'';
    SELECT @sql = @sql + 'ALTER TABLE dbo.leave_requests DROP CONSTRAINT ' + QUOTENAME(name) + ';'
    FROM sys.default_constraints WHERE parent_object_id = OBJECT_ID('dbo.leave_requests');
    IF @sql <> '' EXEC sp_executesql @sql;

    IF COL_LENGTH(N'dbo.leave_requests', N'leave_type') IS NOT NULL
        ALTER TABLE dbo.leave_requests ALTER COLUMN leave_type NVARCHAR(50) NOT NULL;

    IF COL_LENGTH(N'dbo.leave_requests', N'status') IS NOT NULL
        ALTER TABLE dbo.leave_requests ALTER COLUMN status NVARCHAR(50) NULL;

    IF COL_LENGTH(N'dbo.leave_requests', N'total_days') IS NOT NULL
        ALTER TABLE dbo.leave_requests ALTER COLUMN total_days DECIMAL(5,1) NOT NULL;

    IF COL_LENGTH(N'dbo.leave_requests', N'reason') IS NOT NULL
        ALTER TABLE dbo.leave_requests ALTER COLUMN reason NVARCHAR(MAX) NOT NULL;

    IF COL_LENGTH(N'dbo.leave_requests', N'applied_at') IS NOT NULL
        ALTER TABLE dbo.leave_requests ALTER COLUMN applied_at DATETIME2 NOT NULL;

    IF COL_LENGTH(N'dbo.leave_requests', N'contact_during_leave') IS NOT NULL
        ALTER TABLE dbo.leave_requests ALTER COLUMN contact_during_leave NVARCHAR(100) NULL;

    IF COL_LENGTH(N'dbo.leave_requests', N'alternate_contact') IS NOT NULL
        ALTER TABLE dbo.leave_requests ALTER COLUMN alternate_contact NVARCHAR(100) NULL;

    IF COL_LENGTH(N'dbo.leave_requests', N'review_remarks') IS NOT NULL
        ALTER TABLE dbo.leave_requests ALTER COLUMN review_remarks NVARCHAR(MAX) NULL;

    IF COL_LENGTH(N'dbo.leave_requests', N'cancellation_reason') IS NOT NULL
        ALTER TABLE dbo.leave_requests ALTER COLUMN cancellation_reason NVARCHAR(MAX) NULL;

    IF COL_LENGTH(N'dbo.leave_requests', N'manager_id') IS NOT NULL
        ALTER TABLE dbo.leave_requests DROP COLUMN manager_id;

    IF COL_LENGTH(N'dbo.leave_requests', N'manager_remarks') IS NOT NULL
        ALTER TABLE dbo.leave_requests DROP COLUMN manager_remarks;
END;

-- ============================================================
-- 10. MODULES
-- ============================================================
IF OBJECT_ID(N'dbo.modules', N'U') IS NOT NULL
BEGIN
    SET @con_cursor = CURSOR FOR SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA='dbo' AND TABLE_NAME='modules' AND CONSTRAINT_TYPE IN ('CHECK', 'UNIQUE');
    OPEN @con_cursor;
    FETCH NEXT FROM @con_cursor INTO @con_name;
    WHILE @@FETCH_STATUS = 0
    BEGIN
        EXEC('ALTER TABLE dbo.modules DROP CONSTRAINT ' + @con_name);
        FETCH NEXT FROM @con_cursor INTO @con_name;
    END;
    CLOSE @con_cursor;
    DEALLOCATE @con_cursor;

    SET @sql = N'';
    SELECT @sql = @sql + 'DROP INDEX ' + QUOTENAME(name) + ' ON dbo.modules;'
    FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.modules') AND is_unique = 1 AND is_primary_key = 0;
    IF @sql <> '' EXEC sp_executesql @sql;

    IF COL_LENGTH(N'dbo.modules', N'name') IS NOT NULL
        ALTER TABLE dbo.modules ALTER COLUMN name NVARCHAR(100) NOT NULL;

    IF COL_LENGTH(N'dbo.modules', N'code') IS NOT NULL
        ALTER TABLE dbo.modules ALTER COLUMN code NVARCHAR(50) NULL;

    IF COL_LENGTH(N'dbo.modules', N'icon') IS NOT NULL
        ALTER TABLE dbo.modules ALTER COLUMN icon NVARCHAR(50) NULL;

    IF COL_LENGTH(N'dbo.modules', N'route_path') IS NOT NULL
        ALTER TABLE dbo.modules ALTER COLUMN route_path NVARCHAR(255) NULL;
END;

-- ============================================================
-- 11. NOTIFICATIONS
-- ============================================================
IF OBJECT_ID(N'dbo.notifications', N'U') IS NOT NULL
BEGIN
    SET @con_cursor = CURSOR FOR SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA='dbo' AND TABLE_NAME='notifications' AND CONSTRAINT_TYPE IN ('CHECK', 'UNIQUE');
    OPEN @con_cursor;
    FETCH NEXT FROM @con_cursor INTO @con_name;
    WHILE @@FETCH_STATUS = 0
    BEGIN
        EXEC('ALTER TABLE dbo.notifications DROP CONSTRAINT ' + @con_name);
        FETCH NEXT FROM @con_cursor INTO @con_name;
    END;
    CLOSE @con_cursor;
    DEALLOCATE @con_cursor;

    SET @sql = N'';
    SELECT @sql = @sql + 'ALTER TABLE dbo.notifications DROP CONSTRAINT ' + QUOTENAME(name) + ';'
    FROM sys.default_constraints WHERE parent_object_id = OBJECT_ID('dbo.notifications');
    IF @sql <> '' EXEC sp_executesql @sql;

    IF COL_LENGTH(N'dbo.notifications', N'type') IS NOT NULL
        ALTER TABLE dbo.notifications ALTER COLUMN type NVARCHAR(50) NOT NULL;

    IF COL_LENGTH(N'dbo.notifications', N'category') IS NOT NULL
        ALTER TABLE dbo.notifications ALTER COLUMN category NVARCHAR(50) NOT NULL;

    IF COL_LENGTH(N'dbo.notifications', N'title') IS NOT NULL
        ALTER TABLE dbo.notifications ALTER COLUMN title NVARCHAR(255) NOT NULL;

    IF COL_LENGTH(N'dbo.notifications', N'content') IS NOT NULL
        ALTER TABLE dbo.notifications ALTER COLUMN content TEXT NOT NULL;

    IF COL_LENGTH(N'dbo.notifications', N'priority') IS NOT NULL
        ALTER TABLE dbo.notifications ALTER COLUMN priority NVARCHAR(50) NULL;

    IF COL_LENGTH(N'dbo.notifications', N'action_url') IS NOT NULL
        ALTER TABLE dbo.notifications ALTER COLUMN action_url NVARCHAR(500) NULL;

    IF COL_LENGTH(N'dbo.notifications', N'metadata') IS NOT NULL
        ALTER TABLE dbo.notifications DROP COLUMN metadata;
END;

-- ============================================================
-- 12. PASSWORD_RESET_TOKEN
-- ============================================================
IF OBJECT_ID(N'dbo.password_reset_token', N'U') IS NOT NULL
BEGIN
    SET @con_cursor = CURSOR FOR SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA='dbo' AND TABLE_NAME='password_reset_token' AND CONSTRAINT_TYPE IN ('CHECK', 'UNIQUE');
    OPEN @con_cursor;
    FETCH NEXT FROM @con_cursor INTO @con_name;
    WHILE @@FETCH_STATUS = 0
    BEGIN
        EXEC('ALTER TABLE dbo.password_reset_token DROP CONSTRAINT ' + @con_name);
        FETCH NEXT FROM @con_cursor INTO @con_name;
    END;
    CLOSE @con_cursor;
    DEALLOCATE @con_cursor;

    SET @sql = N'';
    SELECT @sql = @sql + 'DROP INDEX ' + QUOTENAME(name) + ' ON dbo.password_reset_token;'
    FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.password_reset_token') AND is_unique = 1 AND is_primary_key = 0;
    IF @sql <> '' EXEC sp_executesql @sql;

    IF COL_LENGTH(N'dbo.password_reset_token', N'token') IS NOT NULL
        ALTER TABLE dbo.password_reset_token ALTER COLUMN token NVARCHAR(255) NOT NULL;

    IF COL_LENGTH(N'dbo.password_reset_token', N'email') IS NOT NULL
        ALTER TABLE dbo.password_reset_token ALTER COLUMN email NVARCHAR(100) NOT NULL;

    IF COL_LENGTH(N'dbo.password_reset_token', N'ip_address') IS NOT NULL
        ALTER TABLE dbo.password_reset_token ALTER COLUMN ip_address NVARCHAR(45) NULL;
END;

-- ============================================================
-- 13. USER_SESSIONS
-- ============================================================
IF OBJECT_ID(N'dbo.user_sessions', N'U') IS NOT NULL
BEGIN
    SET @con_cursor = CURSOR FOR SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA='dbo' AND TABLE_NAME='user_sessions' AND CONSTRAINT_TYPE IN ('CHECK', 'UNIQUE');
    OPEN @con_cursor;
    FETCH NEXT FROM @con_cursor INTO @con_name;
    WHILE @@FETCH_STATUS = 0
    BEGIN
        EXEC('ALTER TABLE dbo.user_sessions DROP CONSTRAINT ' + @con_name);
        FETCH NEXT FROM @con_cursor INTO @con_name;
    END;
    CLOSE @con_cursor;
    DEALLOCATE @con_cursor;

    SET @sql = N'';
    SELECT @sql = @sql + 'DROP INDEX ' + QUOTENAME(name) + ' ON dbo.user_sessions;'
    FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.user_sessions') AND is_unique = 1 AND is_primary_key = 0;
    IF @sql <> '' EXEC sp_executesql @sql;

    IF COL_LENGTH(N'dbo.user_sessions', N'session_token') IS NOT NULL
        ALTER TABLE dbo.user_sessions ALTER COLUMN session_token NVARCHAR(500) NOT NULL;

    IF COL_LENGTH(N'dbo.user_sessions', N'refresh_token') IS NOT NULL
        ALTER TABLE dbo.user_sessions ALTER COLUMN refresh_token NVARCHAR(500) NULL;

    IF COL_LENGTH(N'dbo.user_sessions', N'ip_address') IS NOT NULL
        ALTER TABLE dbo.user_sessions ALTER COLUMN ip_address NVARCHAR(45) NULL;

    IF COL_LENGTH(N'dbo.user_sessions', N'location') IS NOT NULL
        ALTER TABLE dbo.user_sessions ALTER COLUMN location NVARCHAR(255) NULL;
END;

-- ============================================================
-- 14. DEPARTMENT_WEEKLY_OFF
-- ============================================================
IF OBJECT_ID(N'dbo.department_weekly_off', N'U') IS NOT NULL
BEGIN
    SET @con_cursor = CURSOR FOR SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA='dbo' AND TABLE_NAME='department_weekly_off' AND CONSTRAINT_TYPE IN ('CHECK', 'UNIQUE');
    OPEN @con_cursor;
    FETCH NEXT FROM @con_cursor INTO @con_name;
    WHILE @@FETCH_STATUS = 0
    BEGIN
        EXEC('ALTER TABLE dbo.department_weekly_off DROP CONSTRAINT ' + @con_name);
        FETCH NEXT FROM @con_cursor INTO @con_name;
    END;
    CLOSE @con_cursor;
    DEALLOCATE @con_cursor;

    IF COL_LENGTH(N'dbo.department_weekly_off', N'weekly_off_days') IS NOT NULL
        ALTER TABLE dbo.department_weekly_off ALTER COLUMN weekly_off_days NVARCHAR(100) NOT NULL;

    IF COL_LENGTH(N'dbo.department_weekly_off', N'effective_from') IS NOT NULL
        ALTER TABLE dbo.department_weekly_off ALTER COLUMN effective_from DATE NOT NULL;

    IF COL_LENGTH(N'dbo.department_weekly_off', N'created_by') IS NOT NULL
        ALTER TABLE dbo.department_weekly_off ALTER COLUMN created_by BIGINT NULL;

    IF COL_LENGTH(N'dbo.department_weekly_off', N'updated_by') IS NOT NULL
        ALTER TABLE dbo.department_weekly_off ALTER COLUMN updated_by BIGINT NULL;
END;

-- ============================================================
-- 15. DESIGNATION_MODULE_ACCESS
-- ============================================================
IF OBJECT_ID(N'dbo.designation_module_access', N'U') IS NOT NULL
BEGIN
    SET @con_cursor = CURSOR FOR SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA='dbo' AND TABLE_NAME='designation_module_access' AND CONSTRAINT_TYPE IN ('CHECK', 'UNIQUE');
    OPEN @con_cursor;
    FETCH NEXT FROM @con_cursor INTO @con_name;
    WHILE @@FETCH_STATUS = 0
    BEGIN
        EXEC('ALTER TABLE dbo.designation_module_access DROP CONSTRAINT ' + @con_name);
        FETCH NEXT FROM @con_cursor INTO @con_name;
    END;
    CLOSE @con_cursor;
    DEALLOCATE @con_cursor;

    SET @sql = N'';
    SELECT @sql = @sql + 'ALTER TABLE dbo.designation_module_access DROP CONSTRAINT ' + QUOTENAME(name) + ';'
    FROM sys.default_constraints WHERE parent_object_id = OBJECT_ID('dbo.designation_module_access');
    IF @sql <> '' EXEC sp_executesql @sql;

    IF COL_LENGTH(N'dbo.designation_module_access', N'access_level') IS NOT NULL
        ALTER TABLE dbo.designation_module_access ALTER COLUMN access_level NVARCHAR(50) NOT NULL;

    IF COL_LENGTH(N'dbo.designation_module_access', N'restrictions') IS NOT NULL
        ALTER TABLE dbo.designation_module_access DROP COLUMN restrictions;
END;

-- ============================================================
-- 16. COMPANY_SETTINGS
-- ============================================================
IF OBJECT_ID(N'dbo.company_settings', N'U') IS NOT NULL
BEGIN
    SET @con_cursor = CURSOR FOR SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA='dbo' AND TABLE_NAME='company_settings' AND CONSTRAINT_TYPE IN ('CHECK', 'UNIQUE');
    OPEN @con_cursor;
    FETCH NEXT FROM @con_cursor INTO @con_name;
    WHILE @@FETCH_STATUS = 0
    BEGIN
        EXEC('ALTER TABLE dbo.company_settings DROP CONSTRAINT ' + @con_name);
        FETCH NEXT FROM @con_cursor INTO @con_name;
    END;
    CLOSE @con_cursor;
    DEALLOCATE @con_cursor;

    SET @sql = N'';
    SELECT @sql = @sql + 'DROP INDEX ' + QUOTENAME(name) + ' ON dbo.company_settings;'
    FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.company_settings') AND is_unique = 1 AND is_primary_key = 0;
    IF @sql <> '' EXEC sp_executesql @sql;

    SET @sql = N'';
    SELECT @sql = @sql + 'ALTER TABLE dbo.company_settings DROP CONSTRAINT ' + QUOTENAME(name) + ';'
    FROM sys.default_constraints WHERE parent_object_id = OBJECT_ID('dbo.company_settings');
    IF @sql <> '' EXEC sp_executesql @sql;

    IF COL_LENGTH(N'dbo.company_settings', N'setting_key') IS NOT NULL
        ALTER TABLE dbo.company_settings ALTER COLUMN setting_key NVARCHAR(100) NOT NULL;

    IF COL_LENGTH(N'dbo.company_settings', N'setting_value') IS NOT NULL
        ALTER TABLE dbo.company_settings ALTER COLUMN setting_value NVARCHAR(MAX) NOT NULL;

    IF COL_LENGTH(N'dbo.company_settings', N'description') IS NOT NULL
        ALTER TABLE dbo.company_settings ALTER COLUMN description NVARCHAR(500) NULL;

    IF COL_LENGTH(N'dbo.company_settings', N'created_at') IS NOT NULL
        ALTER TABLE dbo.company_settings ALTER COLUMN created_at DATETIME2 NULL;

    IF COL_LENGTH(N'dbo.company_settings', N'updated_at') IS NOT NULL
        ALTER TABLE dbo.company_settings ALTER COLUMN updated_at DATETIME2 NULL;
END;

-- ============================================================
-- 17. UNITS
-- ============================================================
IF OBJECT_ID(N'dbo.units', N'U') IS NOT NULL
BEGIN
    SET @con_cursor = CURSOR FOR SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA='dbo' AND TABLE_NAME='units' AND CONSTRAINT_TYPE IN ('CHECK', 'UNIQUE');
    OPEN @con_cursor;
    FETCH NEXT FROM @con_cursor INTO @con_name;
    WHILE @@FETCH_STATUS = 0
    BEGIN
        EXEC('ALTER TABLE dbo.units DROP CONSTRAINT ' + @con_name);
        FETCH NEXT FROM @con_cursor INTO @con_name;
    END;
    CLOSE @con_cursor;
    DEALLOCATE @con_cursor;

    SET @sql = N'';
    SELECT @sql = @sql + 'DROP INDEX ' + QUOTENAME(name) + ' ON dbo.units;'
    FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.units') AND is_unique = 1 AND is_primary_key = 0;
    IF @sql <> '' EXEC sp_executesql @sql;

    IF COL_LENGTH(N'dbo.units', N'name') IS NOT NULL
        ALTER TABLE dbo.units ALTER COLUMN name NVARCHAR(100) NOT NULL;

    IF COL_LENGTH(N'dbo.units', N'code') IS NOT NULL
        ALTER TABLE dbo.units ALTER COLUMN code NVARCHAR(20) NULL;

    IF COL_LENGTH(N'dbo.units', N'country') IS NOT NULL
        ALTER TABLE dbo.units ALTER COLUMN country NVARCHAR(100) NULL;

    IF COL_LENGTH(N'dbo.units', N'is_active') IS NOT NULL
        ALTER TABLE dbo.units ALTER COLUMN is_active BIT NULL;

    IF COL_LENGTH(N'dbo.units', N'access_level') IS NOT NULL
        ALTER TABLE dbo.units ALTER COLUMN access_level INT NULL;
END;

-- ============================================================
-- 18. SQUADS
-- ============================================================
IF OBJECT_ID(N'dbo.squads', N'U') IS NOT NULL
BEGIN
    SET @con_cursor = CURSOR FOR SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA='dbo' AND TABLE_NAME='squads' AND CONSTRAINT_TYPE IN ('CHECK', 'UNIQUE');
    OPEN @con_cursor;
    FETCH NEXT FROM @con_cursor INTO @con_name;
    WHILE @@FETCH_STATUS = 0
    BEGIN
        EXEC('ALTER TABLE dbo.squads DROP CONSTRAINT ' + @con_name);
        FETCH NEXT FROM @con_cursor INTO @con_name;
    END;
    CLOSE @con_cursor;
    DEALLOCATE @con_cursor;

    SET @sql = N'';
    SELECT @sql = @sql + 'DROP INDEX ' + QUOTENAME(name) + ' ON dbo.squads;'
    FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.squads') AND is_unique = 1 AND is_primary_key = 0;
    IF @sql <> '' EXEC sp_executesql @sql;

    IF COL_LENGTH(N'dbo.squads', N'name') IS NOT NULL
        ALTER TABLE dbo.squads ALTER COLUMN name NVARCHAR(100) NOT NULL;

    IF COL_LENGTH(N'dbo.squads', N'code') IS NOT NULL
        ALTER TABLE dbo.squads ALTER COLUMN code NVARCHAR(20) NULL;

    IF COL_LENGTH(N'dbo.squads', N'is_active') IS NOT NULL
        ALTER TABLE dbo.squads ALTER COLUMN is_active BIT NULL;
END;

-- ============================================================
-- 19. TERRITORIES
-- ============================================================
IF OBJECT_ID(N'dbo.territories', N'U') IS NOT NULL
BEGIN
    SET @con_cursor = CURSOR FOR SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA='dbo' AND TABLE_NAME='territories' AND CONSTRAINT_TYPE IN ('CHECK', 'UNIQUE');
    OPEN @con_cursor;
    FETCH NEXT FROM @con_cursor INTO @con_name;
    WHILE @@FETCH_STATUS = 0
    BEGIN
        EXEC('ALTER TABLE dbo.territories DROP CONSTRAINT ' + @con_name);
        FETCH NEXT FROM @con_cursor INTO @con_name;
    END;
    CLOSE @con_cursor;
    DEALLOCATE @con_cursor;

    SET @sql = N'';
    SELECT @sql = @sql + 'DROP INDEX ' + QUOTENAME(name) + ' ON dbo.territories;'
    FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.territories') AND is_unique = 1 AND is_primary_key = 0;
    IF @sql <> '' EXEC sp_executesql @sql;

    IF COL_LENGTH(N'dbo.territories', N'name') IS NOT NULL
        ALTER TABLE dbo.territories ALTER COLUMN name NVARCHAR(100) NOT NULL;

    IF COL_LENGTH(N'dbo.territories', N'code') IS NOT NULL
        ALTER TABLE dbo.territories ALTER COLUMN code NVARCHAR(20) NULL;

    IF COL_LENGTH(N'dbo.territories', N'is_active') IS NOT NULL
        ALTER TABLE dbo.territories ALTER COLUMN is_active BIT NULL;
END;

-- ============================================================
-- 20. FOT_ACCESS_LEVELS
-- ============================================================
IF OBJECT_ID(N'dbo.fot_access_levels', N'U') IS NOT NULL
BEGIN
    SET @con_cursor = CURSOR FOR SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA='dbo' AND TABLE_NAME='fot_access_levels' AND CONSTRAINT_TYPE IN ('CHECK', 'UNIQUE');
    OPEN @con_cursor;
    FETCH NEXT FROM @con_cursor INTO @con_name;
    WHILE @@FETCH_STATUS = 0
    BEGIN
        EXEC('ALTER TABLE dbo.fot_access_levels DROP CONSTRAINT ' + @con_name);
        FETCH NEXT FROM @con_cursor INTO @con_name;
    END;
    CLOSE @con_cursor;
    DEALLOCATE @con_cursor;

    SET @sql = N'';
    SELECT @sql = @sql + 'DROP INDEX ' + QUOTENAME(name) + ' ON dbo.fot_access_levels;'
    FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.fot_access_levels') AND is_unique = 1 AND is_primary_key = 0;
    IF @sql <> '' EXEC sp_executesql @sql;

    IF COL_LENGTH(N'dbo.fot_access_levels', N'name') IS NOT NULL
        ALTER TABLE dbo.fot_access_levels ALTER COLUMN name NVARCHAR(100) NOT NULL;

    IF COL_LENGTH(N'dbo.fot_access_levels', N'code') IS NOT NULL
        ALTER TABLE dbo.fot_access_levels ALTER COLUMN code NVARCHAR(20) NULL;

    IF COL_LENGTH(N'dbo.fot_access_levels', N'access_level') IS NOT NULL
        ALTER TABLE dbo.fot_access_levels ALTER COLUMN access_level INT NULL;

    IF COL_LENGTH(N'dbo.fot_access_levels', N'is_active') IS NOT NULL
        ALTER TABLE dbo.fot_access_levels ALTER COLUMN is_active BIT NULL;
END;

-- ============================================================
-- 21. DEPARTMENT_MODULE
-- ============================================================
IF OBJECT_ID(N'dbo.department_module', N'U') IS NOT NULL
BEGIN
    IF COL_LENGTH(N'dbo.department_module', N'is_active') IS NOT NULL
        ALTER TABLE dbo.department_module ALTER COLUMN is_active BIT NULL;
END;

-- ============================================================
-- 22. COMPANY_SETTINGS - is_active
-- ============================================================
IF OBJECT_ID(N'dbo.company_settings', N'U') IS NOT NULL
BEGIN
    IF COL_LENGTH(N'dbo.company_settings', N'is_active') IS NOT NULL
        ALTER TABLE dbo.company_settings ALTER COLUMN is_active BIT NULL;
END;
