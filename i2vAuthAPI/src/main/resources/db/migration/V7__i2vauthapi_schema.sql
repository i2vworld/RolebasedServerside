-- ============================================================
-- i2vAuthAPI Schema Migration
-- All tables for the i2vAuthAPI application entities
-- ============================================================

-- ============================================================
-- 1. USERS TABLE
-- ============================================================
IF OBJECT_ID(N'dbo.users', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.users (
        id            BIGINT         NOT NULL IDENTITY(1,1) PRIMARY KEY,
        full_name     NVARCHAR(100)  NOT NULL,
        email         NVARCHAR(100)  NOT NULL UNIQUE,
        password      NVARCHAR(255)  NOT NULL,
        active        BIT            NOT NULL DEFAULT 1,
        role          NVARCHAR(255)  NOT NULL DEFAULT 'GENERAL',
        email_verified BIT           NULL DEFAULT 0,
        last_login    DATETIME2      NULL,
        created_at    DATETIME2      NULL,
        updated_at    DATETIME2      NULL,
        created_by    BIGINT         NULL,
        updated_by    BIGINT         NULL,
        CONSTRAINT FK_users_created_by FOREIGN KEY (created_by) REFERENCES dbo.users(id),
        CONSTRAINT FK_users_updated_by FOREIGN KEY (updated_by) REFERENCES dbo.users(id)
    );
    CREATE INDEX IX_users_email ON dbo.users (email);
    CREATE INDEX IX_users_role ON dbo.users (role);
    CREATE INDEX IX_users_active ON dbo.users (active);
END;

-- ============================================================
-- 2. EMPLOYEE_USERS TABLE
-- ============================================================
IF OBJECT_ID(N'dbo.employee_users', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.employee_users (
        id                     BIGINT         NOT NULL PRIMARY KEY,
        employee_code          NVARCHAR(50)   NOT NULL UNIQUE,
        department_id          BIGINT         NOT NULL,
        designation_id         BIGINT         NOT NULL,
        reporting_to           BIGINT         NULL,
        unit_id                BIGINT         NULL,
        squad_id               BIGINT         NULL,
        territory_id           BIGINT         NULL,
        fot_access_level_id    BIGINT         NULL,
        joining_date           DATE           NOT NULL,
        confirmation_date      DATE           NULL,
        resignation_date       DATE           NULL,
        employment_type        NVARCHAR(50)   NULL DEFAULT 'PERMANENT',
        employment_status      NVARCHAR(50)   NULL DEFAULT 'ACTIVE',
        phone_number           NVARCHAR(20)   NULL,
        alternate_phone        NVARCHAR(20)   NULL,
        address                NVARCHAR(MAX)  NULL,
        city                   NVARCHAR(50)   NULL,
        state                  NVARCHAR(50)   NULL,
        country                NVARCHAR(50)   NULL DEFAULT 'India',
        postal_code            NVARCHAR(20)   NULL,
        date_of_birth          DATE           NULL,
        gender                 NVARCHAR(20)   NULL,
        blood_group            NVARCHAR(5)    NULL,
        emergency_contact_name NVARCHAR(100)  NULL,
        emergency_contact_phone NVARCHAR(20)  NULL,
        emergency_contact_relation NVARCHAR(50) NULL,
        bank_account_number    NVARCHAR(50)   NULL,
        bank_name              NVARCHAR(100)  NULL,
        bank_ifsc_code         NVARCHAR(20)   NULL,
        pan_number             NVARCHAR(10)   NULL,
        aadhar_number          NVARCHAR(20)   NULL,
        pf_number              NVARCHAR(50)   NULL,
        esi_number             NVARCHAR(50)   NULL,
        uan_number             NVARCHAR(20)   NULL,
        work_email             NVARCHAR(100)  NULL,
        work_phone             NVARCHAR(20)   NULL,
        extension              NVARCHAR(10)   NULL,
        seating_location       NVARCHAR(100)  NULL,
        shift_timing           NVARCHAR(50)   NULL,
        weekly_off             NVARCHAR(20)   NULL DEFAULT 'SUNDAY',
        probation_period_months INT           NULL DEFAULT 6,
        notice_period_days     INT           NULL DEFAULT 30,
        annual_leaves_entitled INT           NULL DEFAULT 24,
        sick_leaves_entitled   INT           NULL DEFAULT 12,
        casual_leaves_entitled INT           NULL DEFAULT 12,
        kyc_data               NVARCHAR(MAX)  NULL,
        kyc_text_url           NVARCHAR(512)  NULL,
        pan_front_url          NVARCHAR(512)  NULL,
        pan_back_url           NVARCHAR(512)  NULL,
        aadhaar_front_url      NVARCHAR(512)  NULL,
        aadhaar_back_url       NVARCHAR(512)  NULL,
        employee_photo_url     NVARCHAR(512)  NULL,
        signature_url          NVARCHAR(512)  NULL,
        bank_document_url      NVARCHAR(512)  NULL,
        created_at             DATETIME2      NULL,
        updated_at             DATETIME2      NULL,
        created_by             BIGINT         NULL,
        updated_by             BIGINT         NULL,
        CONSTRAINT FK_employee_users_user FOREIGN KEY (id) REFERENCES dbo.users(id),
        CONSTRAINT FK_employee_users_department FOREIGN KEY (department_id) REFERENCES dbo.departments(id),
        CONSTRAINT FK_employee_users_designation FOREIGN KEY (designation_id) REFERENCES dbo.designations(id),
        CONSTRAINT FK_employee_users_reporting_to FOREIGN KEY (reporting_to) REFERENCES dbo.employee_users(id),
        CONSTRAINT FK_employee_users_unit FOREIGN KEY (unit_id) REFERENCES dbo.units(id),
        CONSTRAINT FK_employee_users_squad FOREIGN KEY (squad_id) REFERENCES dbo.squads(id),
        CONSTRAINT FK_employee_users_territory FOREIGN KEY (territory_id) REFERENCES dbo.territories(id),
        CONSTRAINT FK_employee_users_fot_access_level FOREIGN KEY (fot_access_level_id) REFERENCES dbo.fot_access_levels(id),
        CONSTRAINT FK_employee_users_created_by FOREIGN KEY (created_by) REFERENCES dbo.users(id),
        CONSTRAINT FK_employee_users_updated_by FOREIGN KEY (updated_by) REFERENCES dbo.users(id)
    );
    CREATE INDEX IX_employee_users_department ON dbo.employee_users (department_id);
    CREATE INDEX IX_employee_users_designation ON dbo.employee_users (designation_id);
    CREATE INDEX IX_employee_users_employee_code ON dbo.employee_users (employee_code);
    CREATE INDEX IX_employee_users_reporting_to ON dbo.employee_users (reporting_to);
END;

-- ============================================================
-- 3. DEPARTMENTS TABLE
-- ============================================================
IF OBJECT_ID(N'dbo.departments', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.departments (
        id                 BIGINT         NOT NULL IDENTITY(1,1) PRIMARY KEY,
        name               NVARCHAR(100)  NOT NULL UNIQUE,
        code               NVARCHAR(20)   NULL UNIQUE,
        description        NVARCHAR(MAX)  NULL,
        parent_department_id BIGINT       NULL,
        unit_id            BIGINT         NULL,
        is_hierarchy_unit  BIT            NULL DEFAULT 0,
        manager_id         BIGINT         NULL,
        is_active          BIT            NULL DEFAULT 1,
        budget             DECIMAL(15,2)  NULL DEFAULT 0,
        location           NVARCHAR(255)  NULL,
        email              NVARCHAR(100)  NULL,
        phone              NVARCHAR(20)   NULL,
        created_at         DATETIME2      NULL,
        updated_at         DATETIME2      NULL,
        created_by         BIGINT         NULL,
        updated_by         BIGINT         NULL,
        CONSTRAINT FK_departments_parent FOREIGN KEY (parent_department_id) REFERENCES dbo.departments(id),
        CONSTRAINT FK_departments_unit FOREIGN KEY (unit_id) REFERENCES dbo.units(id),
        CONSTRAINT FK_departments_manager FOREIGN KEY (manager_id) REFERENCES dbo.users(id),
        CONSTRAINT FK_departments_created_by FOREIGN KEY (created_by) REFERENCES dbo.users(id),
        CONSTRAINT FK_departments_updated_by FOREIGN KEY (updated_by) REFERENCES dbo.users(id)
    );
    CREATE INDEX IX_departments_parent ON dbo.departments (parent_department_id);
    CREATE INDEX IX_departments_unit ON dbo.departments (unit_id);
    CREATE INDEX IX_departments_manager ON dbo.departments (manager_id);
    CREATE INDEX IX_departments_is_active ON dbo.departments (is_active);
END;

-- ============================================================
-- 4. DESIGNATIONS TABLE
-- ============================================================
IF OBJECT_ID(N'dbo.designations', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.designations (
        id            BIGINT         NOT NULL IDENTITY(1,1) PRIMARY KEY,
        title         NVARCHAR(100)  NOT NULL UNIQUE,
        code          NVARCHAR(20)   NULL UNIQUE,
        description   NVARCHAR(MAX)  NULL,
        level         INT            NULL DEFAULT 1,
        department_id BIGINT         NULL,
        min_salary    DECIMAL(15,2)  NULL,
        max_salary    DECIMAL(15,2)  NULL,
        is_active     BIT            NULL DEFAULT 1,
        is_managerial BIT            NULL DEFAULT 0,
        created_at    DATETIME2      NULL,
        updated_at    DATETIME2      NULL,
        created_by    BIGINT         NULL,
        updated_by    BIGINT         NULL,
        CONSTRAINT FK_designations_department FOREIGN KEY (department_id) REFERENCES dbo.departments(id),
        CONSTRAINT FK_designations_created_by FOREIGN KEY (created_by) REFERENCES dbo.users(id),
        CONSTRAINT FK_designations_updated_by FOREIGN KEY (updated_by) REFERENCES dbo.users(id)
    );
    CREATE INDEX IX_designations_department ON dbo.designations (department_id);
    CREATE INDEX IX_designations_code ON dbo.designations (code);
END;

-- ============================================================
-- 5. MODULES TABLE
-- ============================================================
IF OBJECT_ID(N'dbo.modules', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.modules (
        id            BIGINT         NOT NULL IDENTITY(1,1) PRIMARY KEY,
        name          NVARCHAR(100)  NOT NULL UNIQUE,
        code          NVARCHAR(50)   NULL UNIQUE,
        description   TEXT           NULL,
        parent_module_id BIGINT      NULL,
        icon          NVARCHAR(50)   NULL,
        route_path    NVARCHAR(255)  NULL,
        display_order INT            NULL DEFAULT 0,
        is_active     BIT            NULL DEFAULT 1,
        is_visible    BIT            NULL DEFAULT 1,
        requires_premium BIT         NULL DEFAULT 0,
        created_at    DATETIME2      NULL,
        updated_at    DATETIME2      NULL,
        CONSTRAINT FK_modules_parent FOREIGN KEY (parent_module_id) REFERENCES dbo.modules(id)
    );
    CREATE INDEX IX_modules_parent ON dbo.modules (parent_module_id);
    CREATE INDEX IX_modules_code ON dbo.modules (code);
END;

-- ============================================================
-- 6. UNITS TABLE
-- ============================================================
IF OBJECT_ID(N'dbo.units', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.units (
        id          BIGINT         NOT NULL IDENTITY(1,1) PRIMARY KEY,
        name        NVARCHAR(100)  NOT NULL UNIQUE,
        code        NVARCHAR(20)   NULL UNIQUE,
        description NVARCHAR(MAX)  NULL,
        country     NVARCHAR(100)  NULL,
        access_level INT           NULL DEFAULT 1,
        is_active   BIT            NULL DEFAULT 1,
        created_at  DATETIME2      NULL,
        updated_at  DATETIME2      NULL
    );
    CREATE INDEX IX_units_code ON dbo.units (code);
    CREATE INDEX IX_units_is_active ON dbo.units (is_active);
END;

-- ============================================================
-- 7. SQUADS TABLE
-- ============================================================
IF OBJECT_ID(N'dbo.squads', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.squads (
        id          BIGINT         NOT NULL IDENTITY(1,1) PRIMARY KEY,
        name        NVARCHAR(100)  NOT NULL,
        code        NVARCHAR(20)   NULL,
        description NVARCHAR(MAX)  NULL,
        unit_id     BIGINT         NOT NULL,
        access_level INT          NULL DEFAULT 1,
        is_active   BIT            NULL DEFAULT 1,
        created_at  DATETIME2      NULL,
        updated_at  DATETIME2      NULL,
        CONSTRAINT FK_squads_unit FOREIGN KEY (unit_id) REFERENCES dbo.units(id)
    );
    CREATE INDEX IX_squads_unit ON dbo.squads (unit_id);
END;

-- ============================================================
-- 8. TERRITORIES TABLE
-- ============================================================
IF OBJECT_ID(N'dbo.territories', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.territories (
        id          BIGINT         NOT NULL IDENTITY(1,1) PRIMARY KEY,
        name        NVARCHAR(100)  NOT NULL,
        code        NVARCHAR(20)   NULL,
        description NVARCHAR(MAX)  NULL,
        squad_id    BIGINT         NOT NULL,
        access_level INT          NULL DEFAULT 1,
        is_active   BIT            NULL DEFAULT 1,
        created_at  DATETIME2      NULL,
        updated_at  DATETIME2      NULL,
        CONSTRAINT FK_territories_squad FOREIGN KEY (squad_id) REFERENCES dbo.squads(id)
    );
    CREATE INDEX IX_territories_squad ON dbo.territories (squad_id);
END;

-- ============================================================
-- 9. FOT_ACCESS_LEVELS TABLE
-- ============================================================
IF OBJECT_ID(N'dbo.fot_access_levels', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.fot_access_levels (
        id          BIGINT         NOT NULL IDENTITY(1,1) PRIMARY KEY,
        name        NVARCHAR(100)  NOT NULL,
        code        NVARCHAR(20)   NULL,
        description NVARCHAR(MAX)  NULL,
        access_level INT          NULL DEFAULT 1,
        territory_id BIGINT        NOT NULL,
        is_active   BIT            NULL DEFAULT 1,
        created_at  DATETIME2      NULL,
        updated_at  DATETIME2      NULL,
        CONSTRAINT FK_fot_access_levels_territory FOREIGN KEY (territory_id) REFERENCES dbo.territories(id)
    );
    CREATE INDEX IX_fot_access_levels_territory ON dbo.fot_access_levels (territory_id);
END;

-- ============================================================
-- 10. LEAVE_REQUESTS TABLE
-- ============================================================
IF OBJECT_ID(N'dbo.leave_requests', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.leave_requests (
        id                  BIGINT         NOT NULL IDENTITY(1,1) PRIMARY KEY,
        employee_id         BIGINT         NOT NULL,
        leave_type          NVARCHAR(50)   NOT NULL,
        start_date          DATE           NOT NULL,
        end_date            DATE           NOT NULL,
        total_days          DECIMAL(5,1)   NOT NULL,
        reason              NVARCHAR(MAX)  NOT NULL,
        status              NVARCHAR(50)   NULL DEFAULT 'PENDING',
        contact_during_leave NVARCHAR(100) NULL,
        alternate_contact   NVARCHAR(100)  NULL,
        applied_at          DATETIME2      NULL,
        reviewed_by         BIGINT         NULL,
        reviewed_at         DATETIME2      NULL,
        review_remarks      NVARCHAR(MAX)  NULL,
        cancelled_at        DATETIME2      NULL,
        cancellation_reason NVARCHAR(MAX)  NULL,
        CONSTRAINT FK_leave_requests_employee FOREIGN KEY (employee_id) REFERENCES dbo.employee_users(id),
        CONSTRAINT FK_leave_requests_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES dbo.users(id)
    );
    CREATE INDEX IX_leave_requests_employee ON dbo.leave_requests (employee_id);
    CREATE INDEX IX_leave_requests_status ON dbo.leave_requests (status);
    CREATE INDEX IX_leave_requests_leave_type ON dbo.leave_requests (leave_type);
END;

-- ============================================================
-- 11. ATTENDANCE TABLE
-- ============================================================
IF OBJECT_ID(N'dbo.attendance', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.attendance (
        id              BIGINT         NOT NULL IDENTITY(1,1) PRIMARY KEY,
        employee_id     BIGINT         NOT NULL,
        attendance_date DATE           NOT NULL,
        check_in_time   TIME           NULL,
        check_out_time  TIME           NULL,
        total_hours     DECIMAL(5,2)   NULL,
        status          NVARCHAR(50)   NULL DEFAULT 'PRESENT',
        overtime_hours  DECIMAL(5,2)   NULL DEFAULT 0,
        is_late         BIT            NULL DEFAULT 0,
        late_minutes    INT            NULL DEFAULT 0,
        early_departure BIT            NULL DEFAULT 0,
        early_minutes   INT            NULL DEFAULT 0,
        check_in_location NVARCHAR(500) NULL,
        check_out_location NVARCHAR(500) NULL,
        check_in_ip     NVARCHAR(45)   NULL,
        check_out_ip    NVARCHAR(45)   NULL,
        remarks         TEXT             NULL,
        approved_by     BIGINT         NULL,
        approved_at     DATETIME2      NULL,
        created_at      DATETIME2      NULL,
        updated_at      DATETIME2      NULL,
        CONSTRAINT FK_attendance_employee FOREIGN KEY (employee_id) REFERENCES dbo.employee_users(id),
        CONSTRAINT FK_attendance_approved_by FOREIGN KEY (approved_by) REFERENCES dbo.users(id),
        CONSTRAINT UQ_attendance_employee_date UNIQUE (employee_id, attendance_date)
    );
    CREATE INDEX IX_attendance_employee ON dbo.attendance (employee_id);
    CREATE INDEX IX_attendance_date ON dbo.attendance (attendance_date);
    CREATE INDEX IX_attendance_status ON dbo.attendance (status);
END;

-- ============================================================
-- 12. EXIT_REQUESTS TABLE
-- ============================================================
IF OBJECT_ID(N'dbo.exit_requests', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.exit_requests (
        id                BIGINT         NOT NULL IDENTITY(1,1) PRIMARY KEY,
        employee_id       BIGINT         NOT NULL,
        place             NVARCHAR(100)  NOT NULL,
        reason            NVARCHAR(500)  NOT NULL,
        last_working_date DATE           NOT NULL,
        comments          NVARCHAR(1000) NULL,
        status            NVARCHAR(20)   NOT NULL DEFAULT 'PENDING',
        applied_at        DATETIME2      NOT NULL,
        updated_at        DATETIME2      NULL,
        CONSTRAINT FK_exit_requests_employee FOREIGN KEY (employee_id) REFERENCES dbo.employee_users(id)
    );
    CREATE INDEX IX_exit_requests_employee ON dbo.exit_requests (employee_id);
    CREATE INDEX IX_exit_requests_status ON dbo.exit_requests (status);
END;

-- ============================================================
-- 13. CLEARANCE_STEPS TABLE
-- ============================================================
IF OBJECT_ID(N'dbo.clearance_steps', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.clearance_steps (
        id              BIGINT         NOT NULL IDENTITY(1,1) PRIMARY KEY,
        step            NVARCHAR(200)  NOT NULL,
        assigned_to     NVARCHAR(50)   NOT NULL,
        status          NVARCHAR(20)   NOT NULL DEFAULT 'PENDING',
        completed_at    DATETIME2      NULL,
        exit_request_id BIGINT         NOT NULL,
        CONSTRAINT FK_clearance_steps_exit_request FOREIGN KEY (exit_request_id) REFERENCES dbo.exit_requests(id)
    );
    CREATE INDEX IX_clearance_steps_exit_request ON dbo.clearance_steps (exit_request_id);
    CREATE INDEX IX_clearance_steps_status ON dbo.clearance_steps (status);
END;

-- ============================================================
-- 14. HOLIDAYS TABLE
-- ============================================================
IF OBJECT_ID(N'dbo.holidays', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.holidays (
        id               BIGINT         NOT NULL IDENTITY(1,1) PRIMARY KEY,
        holiday_name     NVARCHAR(200)  NOT NULL,
        holiday_date     DATE           NOT NULL UNIQUE,
        description      NVARCHAR(500)  NULL,
        is_recurring_yearly BIT         NULL DEFAULT 0,
        is_active        BIT            NULL DEFAULT 1,
        created_at       DATETIME2      NULL,
        updated_at       DATETIME2      NULL
    );
    CREATE INDEX IX_holidays_date ON dbo.holidays (holiday_date);
    CREATE INDEX IX_holidays_active ON dbo.holidays (is_active);
END;

-- ============================================================
-- 15. NOTIFICATIONS TABLE
-- ============================================================
IF OBJECT_ID(N'dbo.notifications', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.notifications (
        id             BIGINT         NOT NULL IDENTITY(1,1) PRIMARY KEY,
        user_id        BIGINT         NOT NULL,
        type           NVARCHAR(50)   NOT NULL,
        category       NVARCHAR(50)   NOT NULL DEFAULT 'SYSTEM',
        title          NVARCHAR(255)  NOT NULL,
content        TEXT             NOT NULL,
        is_read       BIT            NULL DEFAULT 0,
        read_at        DATETIME2      NULL,
        is_sent        BIT            NULL DEFAULT 0,
        sent_at        DATETIME2      NULL,
        priority       NVARCHAR(50)   NULL DEFAULT 'MEDIUM',
        action_url     NVARCHAR(500)  NULL,
        created_at     DATETIME2      NULL,
        expires_at     DATETIME2      NULL,
        CONSTRAINT FK_notifications_user FOREIGN KEY (user_id) REFERENCES dbo.users(id)
    );
    CREATE INDEX IX_notifications_user ON dbo.notifications (user_id);
    CREATE INDEX IX_notifications_is_read ON dbo.notifications (is_read);
    CREATE INDEX IX_notifications_priority ON dbo.notifications (priority);
END;

-- ============================================================
-- 16. PASSWORD_RESET_TOKEN TABLE
-- ============================================================
IF OBJECT_ID(N'dbo.password_reset_token', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.password_reset_token (
        id            BIGINT         NOT NULL IDENTITY(1,1) PRIMARY KEY,
        token         NVARCHAR(255)  NOT NULL UNIQUE,
        user_id       BIGINT         NOT NULL,
        email         NVARCHAR(100)  NOT NULL,
        expiry        DATETIME2      NOT NULL,
        is_used       BIT            NULL DEFAULT 0,
        used_at       DATETIME2      NULL,
        created_at    DATETIME2      NULL,
        ip_address    NVARCHAR(45)   NULL,
        user_agent    TEXT             NULL,
        CONSTRAINT FK_password_reset_token_user FOREIGN KEY (user_id) REFERENCES dbo.users(id)
    );
    CREATE INDEX IX_password_reset_token_user ON dbo.password_reset_token (user_id);
    CREATE INDEX IX_password_reset_token_token ON dbo.password_reset_token (token);
END;

-- ============================================================
-- 17. USER_SESSIONS TABLE
-- ============================================================
IF OBJECT_ID(N'dbo.user_sessions', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.user_sessions (
        id             BIGINT         NOT NULL IDENTITY(1,1) PRIMARY KEY,
        user_id        BIGINT         NOT NULL,
        session_token  NVARCHAR(500)  NOT NULL UNIQUE,
        refresh_token  NVARCHAR(500)  NULL UNIQUE,
        ip_address     NVARCHAR(45)   NULL,
        user_agent     TEXT             NULL,
        device_info    TEXT             NULL,
        location       NVARCHAR(255)  NULL,
        login_at       DATETIME2      NULL,
        last_activity_at DATETIME2    NULL,
        expires_at     DATETIME2      NOT NULL,
        logout_at      DATETIME2      NULL,
        is_active      BIT            NULL DEFAULT 1,
        CONSTRAINT FK_user_sessions_user FOREIGN KEY (user_id) REFERENCES dbo.users(id)
    );
    CREATE INDEX IX_user_sessions_user ON dbo.user_sessions (user_id);
    CREATE INDEX IX_user_sessions_session_token ON dbo.user_sessions (session_token);
    CREATE INDEX IX_user_sessions_is_active ON dbo.user_sessions (is_active);
END;

-- ============================================================
-- 18. DEPARTMENT_MODULE (JUNCTION TABLE)
-- ============================================================
IF OBJECT_ID(N'dbo.department_module', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.department_module (
        id            BIGINT         NOT NULL IDENTITY(1,1) PRIMARY KEY,
        department_id BIGINT         NOT NULL,
        module_id     BIGINT         NOT NULL,
        assigned_at   DATETIME2      NULL,
        assigned_by   BIGINT         NULL,
        expires_at    DATETIME2      NULL,
        is_active     BIT            NULL DEFAULT 1,
        CONSTRAINT FK_department_module_department FOREIGN KEY (department_id) REFERENCES dbo.departments(id),
        CONSTRAINT FK_department_module_module FOREIGN KEY (module_id) REFERENCES dbo.modules(id),
        CONSTRAINT FK_department_module_assigned_by FOREIGN KEY (assigned_by) REFERENCES dbo.users(id),
        CONSTRAINT UQ_department_module_dept_module UNIQUE (department_id, module_id)
    );
    CREATE INDEX IX_department_module_department ON dbo.department_module (department_id);
    CREATE INDEX IX_department_module_module ON dbo.department_module (module_id);
END;

-- ============================================================
-- 19. DESIGNATION_MODULE_ACCESS (JUNCTION TABLE)
-- ============================================================
IF OBJECT_ID(N'dbo.designation_module_access', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.designation_module_access (
        id              BIGINT         NOT NULL IDENTITY(1,1) PRIMARY KEY,
        designation_id  BIGINT         NOT NULL,
        module_id       BIGINT         NOT NULL,
        access_level    NVARCHAR(50)   NOT NULL DEFAULT 'READ',
        can_create      BIT            NULL DEFAULT 0,
        can_read        BIT            NULL DEFAULT 1,
        can_update      BIT            NULL DEFAULT 0,
        can_delete      BIT            NULL DEFAULT 0,
        can_approve     BIT            NULL DEFAULT 0,
        can_export      BIT            NULL DEFAULT 0,
        can_import      BIT            NULL DEFAULT 0,
        granted_at      DATETIME2      NULL,
        granted_by      BIGINT         NULL,
        expires_at      DATETIME2      NULL,
        is_active       BIT            NULL DEFAULT 1,
        remarks         TEXT             NULL,
        CONSTRAINT FK_designation_module_access_designation FOREIGN KEY (designation_id) REFERENCES dbo.designations(id),
        CONSTRAINT FK_designation_module_access_module FOREIGN KEY (module_id) REFERENCES dbo.modules(id),
        CONSTRAINT FK_designation_module_access_granted_by FOREIGN KEY (granted_by) REFERENCES dbo.users(id),
        CONSTRAINT UQ_designation_module_access_designation_module UNIQUE (designation_id, module_id)
    );
    CREATE INDEX IX_designation_module_access_designation ON dbo.designation_module_access (designation_id);
    CREATE INDEX IX_designation_module_access_module ON dbo.designation_module_access (module_id);
END;

-- ============================================================
-- 20. COMPANY_SETTINGS TABLE
-- ============================================================
IF OBJECT_ID(N'dbo.company_settings', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.company_settings (
        id            BIGINT         NOT NULL IDENTITY(1,1) PRIMARY KEY,
        setting_key   NVARCHAR(100)  NOT NULL UNIQUE,
        setting_value NVARCHAR(MAX)  NOT NULL,
        description   NVARCHAR(500)  NULL,
        is_active     BIT            NULL DEFAULT 1,
        created_at    DATETIME2      NULL,
        updated_at    DATETIME2      NULL
    );
    CREATE INDEX IX_company_settings_key ON dbo.company_settings (setting_key);
    CREATE INDEX IX_company_settings_active ON dbo.company_settings (is_active);
END;

-- ============================================================
-- 21. DEPARTMENT_WEEKLY_OFF TABLE
-- ============================================================
IF OBJECT_ID(N'dbo.department_weekly_off', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.department_weekly_off (
        id               BIGINT         NOT NULL IDENTITY(1,1) PRIMARY KEY,
        department_id    BIGINT         NOT NULL,
        weekly_off_days  NVARCHAR(100)  NOT NULL,
        effective_from   DATE           NOT NULL,
        effective_to     DATE           NULL,
        is_active        BIT            NULL DEFAULT 1,
        created_at       DATETIME2      NULL,
        updated_at       DATETIME2      NULL,
        created_by       BIGINT         NULL,
        updated_by       BIGINT         NULL,
        CONSTRAINT FK_department_weekly_off_department FOREIGN KEY (department_id) REFERENCES dbo.departments(id),
        CONSTRAINT FK_department_weekly_off_created_by FOREIGN KEY (created_by) REFERENCES dbo.users(id),
        CONSTRAINT FK_department_weekly_off_updated_by FOREIGN KEY (updated_by) REFERENCES dbo.users(id)
    );
    CREATE INDEX IX_dept_weekly_off_department ON dbo.department_weekly_off (department_id);
    CREATE INDEX IX_dept_weekly_off_effective ON dbo.department_weekly_off (effective_from);
END;

-- ============================================================
-- 22. AUDIT_LOGS TABLE
-- ============================================================
IF OBJECT_ID(N'dbo.audit_logs', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.audit_logs (
        id              UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID() PRIMARY KEY,
        user_id         BIGINT         NULL,
        action          NVARCHAR(50)     NOT NULL,
        entity_type     NVARCHAR(50)     NOT NULL,
        entity_id       BIGINT           NULL,
        ip_address      NVARCHAR(45)     NULL,
        user_agent      TEXT             NULL,
        request_method  NVARCHAR(10)     NULL,
        request_uri     NVARCHAR(500)    NULL,
        request_params  TEXT             NULL,
        response_status INT              NULL,
        execution_time_ms INT            NULL,
        created_at      DATETIME2        NULL,
        CONSTRAINT FK_audit_logs_user FOREIGN KEY (user_id) REFERENCES dbo.users(id)
    );
    CREATE INDEX IX_audit_logs_user ON dbo.audit_logs (user_id);
    CREATE INDEX IX_audit_logs_action ON dbo.audit_logs (action);
    CREATE INDEX IX_audit_logs_entity ON dbo.audit_logs (entity_type, entity_id);
    CREATE INDEX IX_audit_logs_created ON dbo.audit_logs (created_at);
END;

-- ============================================================
-- MIGRATION COMPLETE
-- All 22 tables for i2vAuthAPI have been created.
-- ============================================================