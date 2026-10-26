IF OBJECT_ID(N'dbo.attendance_correction_requests', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.attendance_correction_requests (
        id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_attendance_correction_requests PRIMARY KEY,
        employee_id BIGINT NOT NULL,
        attendance_date DATE NOT NULL,
        current_check_in_time TIME(0) NULL,
        current_check_out_time TIME(0) NULL,
        requested_check_in_time TIME(0) NULL,
        requested_check_out_time TIME(0) NULL,
        reason NVARCHAR(MAX) NOT NULL,
        status NVARCHAR(20) NOT NULL,
        submitted_at DATETIME2 NOT NULL,
        reviewed_by BIGINT NULL,
        reviewed_at DATETIME2 NULL,
        review_remarks NVARCHAR(MAX) NULL,
        CONSTRAINT FK_attendance_correction_requests_employee
            FOREIGN KEY (employee_id) REFERENCES dbo.employee_users(id),
        CONSTRAINT FK_attendance_correction_requests_reviewer
            FOREIGN KEY (reviewed_by) REFERENCES dbo.users(id),
        CONSTRAINT CK_attendance_correction_requests_status
            CHECK (status IN (N'PENDING', N'APPROVED', N'REJECTED'))
    );

    CREATE INDEX IX_attendance_correction_requests_status_submitted
        ON dbo.attendance_correction_requests(status, submitted_at);

    CREATE UNIQUE INDEX UX_attendance_correction_requests_pending_employee_date
        ON dbo.attendance_correction_requests(employee_id, attendance_date)
        WHERE status = N'PENDING';
END;