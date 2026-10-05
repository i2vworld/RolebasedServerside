package com.world.i2v.i2vauthapi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceSummaryResponse {
    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private String departmentName;
    private String designationTitle;
    private int year;
    private int month;
    private int totalWorkingDays;
    private long presentDays;
    private long absentDays;
    private long lateDays;
    private long leaveDays;
    private long holidayDays;
    private BigDecimal totalHours;
    private double attendanceRate;
    private List<DailyAttendanceDetail> dailyBreakdown;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyAttendanceDetail {
        private LocalDate date;
        private LocalTime checkIn;
        private LocalTime checkOut;
        private BigDecimal hours;
        private Boolean isLate;
        private Integer lateMinutes;
        private String status;
    }
}
