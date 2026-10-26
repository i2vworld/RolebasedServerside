package com.world.i2v.i2vauthapi.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "attendance_correction_requests")
@Data
@EqualsAndHashCode(exclude = {"employee", "reviewedBy"})
@ToString(exclude = {"employee", "reviewedBy"})
public class AttendanceCorrection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    @JsonIgnore
    private EmployeeUser employee;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "current_check_in_time")
    private LocalTime currentCheckInTime;

    @Column(name = "current_check_out_time")
    private LocalTime currentCheckOutTime;

    @Column(name = "requested_check_in_time")
    private LocalTime requestedCheckInTime;

    @Column(name = "requested_check_out_time")
    private LocalTime requestedCheckOutTime;

    @Column(nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CorrectionStatus status = CorrectionStatus.PENDING;

    @CreationTimestamp
    @Column(name = "submitted_at", nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    @JsonIgnore
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_remarks", columnDefinition = "NVARCHAR(MAX)")
    private String reviewRemarks;

    public enum CorrectionStatus {
        PENDING, APPROVED, REJECTED
    }
}