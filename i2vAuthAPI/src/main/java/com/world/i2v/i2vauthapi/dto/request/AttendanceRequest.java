package com.world.i2v.i2vauthapi.dto.request;

import com.world.i2v.i2vauthapi.entity.Attendance;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
public class AttendanceRequest {

    @NotNull(message = "Employee ID is required")
    private Long employeeId;

    @NotNull(message = "Attendance date is required")
    private LocalDate attendanceDate;

    private LocalTime checkInTime;

    private LocalTime checkOutTime;

    private Attendance.AttendanceStatus status = Attendance.AttendanceStatus.PRESENT;

    private String checkInLocation;

    private String checkOutLocation;

    private String checkInIp;

    private String checkOutIp;

    private String remarks;
}
