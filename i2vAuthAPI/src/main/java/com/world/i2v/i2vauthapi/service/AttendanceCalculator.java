package com.world.i2v.i2vauthapi.service;

import com.world.i2v.i2vauthapi.entity.Attendance;
import com.world.i2v.i2vauthapi.entity.EmployeeUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class AttendanceCalculator {

    private static final LocalTime DEFAULT_LATE_THRESHOLD = LocalTime.of(10, 0);
    private static final LocalTime DEFAULT_OFFICIAL_END_TIME = LocalTime.of(18, 0);
    private static final LocalTime DEFAULT_START_TIME = LocalTime.of(9, 0);
    private static final int DEFAULT_WORKING_HOURS = 9;

    public long countPresentDays(List<Attendance> attendances) {
        return attendances.stream().filter(a -> a.getCheckInTime() != null).count();
    }

    public long countLateDays(List<Attendance> attendances) {
        return attendances.stream()
                .filter(a -> a.getIsLate() != null && a.getIsLate())
                .count();
    }

    public BigDecimal calculateTotalHours(List<Attendance> attendances) {
        return attendances.stream()
                .filter(a -> a.getTotalHours() != null)
                .map(Attendance::getTotalHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public double calculateAttendanceRate(long presentDays, int totalWorkingDays) {
        return totalWorkingDays > 0 ? (double) presentDays / totalWorkingDays * 100 : 0;
    }

    public int countWorkingDays(EmployeeUser employee, LocalDate startDate, LocalDate endDate,
                                 CompanySettingsService settingsService) {
        if (employee.getDepartment() != null) {
            Integer workingDays = settingsService.getWorkingDays(
                    employee.getDepartment().getId(), startDate, endDate);
            if (workingDays != null) {
                return workingDays;
            }
        }
        return getDefaultWorkingDays(startDate, endDate);
    }

    public int getDefaultWorkingDays(LocalDate startDate, LocalDate endDate) {
        int workingDays = 0;
        LocalDate date = startDate;
        while (!date.isAfter(endDate)) {
            int dayValue = date.getDayOfWeek().getValue();
            if (dayValue != 6 && dayValue != 7) {
                workingDays++;
            }
            date = date.plusDays(1);
        }
        return workingDays;
    }

    public Map<String, Object> buildSummaryMap(EmployeeUser employee, int year, int month,
                                                List<Attendance> attendances, int totalWorkingDays,
                                                CompanySettingsService settingsService) {
        long presentDays = countPresentDays(attendances);
        long absentDays = totalWorkingDays - presentDays;
        long lateDays = countLateDays(attendances);
        BigDecimal totalHours = calculateTotalHours(attendances);
        double attendanceRate = calculateAttendanceRate(presentDays, totalWorkingDays);

        List<Map<String, Object>> dailyBreakdown = attendances.stream().map(a -> {
            Map<String, Object> day = new HashMap<>();
            day.put("date", a.getAttendanceDate().toString());
            day.put("checkIn", a.getCheckInTime() != null ? a.getCheckInTime().toString() : null);
            day.put("checkOut", a.getCheckOutTime() != null ? a.getCheckOutTime().toString() : null);
            day.put("hours", a.getTotalHours());
            day.put("isLate", a.getIsLate());
            day.put("lateMinutes", a.getLateMinutes());
            day.put("status", a.getStatus() != null ? a.getStatus().name() : null);
            return day;
        }).collect(Collectors.toList());

        Map<String, Object> summary = new HashMap<>();
        summary.put("employeeId", employee.getId());
        summary.put("employeeName", employee.getUser() != null ? employee.getUser().getFullName() : "Unknown");
        summary.put("employeeCode", employee.getEmployeeCode());
        summary.put("departmentName", employee.getDepartment() != null ? employee.getDepartment().getName() : null);
        summary.put("designationTitle", employee.getDesignation() != null ? employee.getDesignation().getTitle() : null);
        summary.put("year", year);
        summary.put("month", month);
        summary.put("totalWorkingDays", totalWorkingDays);
        summary.put("presentDays", presentDays);
        summary.put("absentDays", absentDays);
        summary.put("lateDays", lateDays);
        summary.put("totalHours", totalHours);
        summary.put("attendanceRate", Math.round(attendanceRate * 100.0) / 100.0);
        summary.put("dailyBreakdown", dailyBreakdown);
        return summary;
    }

    public LocalTime getLateThreshold(CompanySettingsService settingsService) {
        LocalTime lateThreshold = settingsService.getLateThreshold();
        return lateThreshold != null ? lateThreshold : DEFAULT_LATE_THRESHOLD;
    }

    public LocalTime getOfficialEndTime(CompanySettingsService settingsService) {
        LocalTime officialEndTime = settingsService.getOfficialEndTime();
        return officialEndTime != null ? officialEndTime : DEFAULT_OFFICIAL_END_TIME;
    }

    public LocalTime getOfficialStartTime(CompanySettingsService settingsService) {
        LocalTime startTime = settingsService.getOfficialStartTime();
        return startTime != null ? startTime : DEFAULT_START_TIME;
    }

    public Integer getWorkingHoursPerDay(CompanySettingsService settingsService) {
        Integer hours = settingsService.getWorkingHoursPerDay();
        return hours != null ? hours : DEFAULT_WORKING_HOURS;
    }
}
