package com.world.i2v.i2vauthapi.dto.response;

import com.world.i2v.i2vauthapi.entity.Attendance;
import com.world.i2v.i2vauthapi.entity.EmployeeUser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceResponse {
    private Long id;
    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private LocalDate attendanceDate;
    private LocalTime checkInTime;
    private LocalTime checkOutTime;
    private BigDecimal totalHours;
    private Attendance.AttendanceStatus status;
    private Boolean isLate;
    private Integer lateMinutes;
    private Boolean earlyDeparture;
    private Integer earlyMinutes;
    private String checkInLocation;
    private String checkOutLocation;
    private String checkInIp;
    private String checkOutIp;
    private String remarks;
    private Long approvedBy;
    private java.time.LocalDateTime approvedAt;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;

    public static AttendanceResponse fromEntity(Attendance attendance, EmployeeUser employee) {
        Long empId = employee != null ? employee.getId() : null;
        String empCode = employee != null ? employee.getEmployeeCode() : null;
        String empName = employee != null && employee.getUser() != null ? employee.getUser().getFullName() : "Unknown";
        return AttendanceResponse.builder()
                .id(attendance.getId())
                .employeeId(empId)
                .employeeCode(empCode)
                .employeeName(empName)
                .attendanceDate(attendance.getAttendanceDate())
                .checkInTime(attendance.getCheckInTime())
                .checkOutTime(attendance.getCheckOutTime())
                .totalHours(attendance.getTotalHours())
                .status(attendance.getStatus())
                .isLate(attendance.getIsLate())
                .lateMinutes(attendance.getLateMinutes())
                .earlyDeparture(attendance.getEarlyDeparture())
                .earlyMinutes(attendance.getEarlyMinutes())
                .checkInLocation(attendance.getCheckInLocation())
                .checkOutLocation(attendance.getCheckOutLocation())
                .checkInIp(attendance.getCheckInIp())
                .checkOutIp(attendance.getCheckOutIp())
                .remarks(attendance.getRemarks())
                .approvedBy(attendance.getApprovedBy() != null ? attendance.getApprovedBy().getId() : null)
                .approvedAt(attendance.getApprovedAt())
                .createdAt(attendance.getCreatedAt())
                .updatedAt(attendance.getUpdatedAt())
                .build();
    }
}
