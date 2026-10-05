package com.world.i2v.i2vauthapi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceStatisticsResponse {
    private int year;
    private int month;
    private int totalEmployees;
    private int totalWorkingDays;
    private long totalPresent;
    private long totalLate;
    private BigDecimal totalHours;
    private double overallAttendanceRate;
}
