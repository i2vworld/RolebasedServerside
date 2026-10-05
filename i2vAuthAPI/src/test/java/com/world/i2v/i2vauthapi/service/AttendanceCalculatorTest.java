package com.world.i2v.i2vauthapi.service;

import com.world.i2v.i2vauthapi.entity.Attendance;
import com.world.i2v.i2vauthapi.entity.Attendance.AttendanceStatus;
import com.world.i2v.i2vauthapi.entity.Department;
import com.world.i2v.i2vauthapi.entity.Designation;
import com.world.i2v.i2vauthapi.entity.EmployeeUser;
import com.world.i2v.i2vauthapi.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Smoke + unit test suite for {@link AttendanceCalculator}.
 * This is the class directly responsible for building the summary/calendar
 * data consumed by the frontend Attendance calendar view, so every branch
 * (working-day counting, weekend detection, late/early thresholds, summary
 * map construction) is covered here.
 */
class AttendanceCalculatorTest {

    private CompanySettingsService settingsService;
    private AttendanceCalculator calculator;

    @BeforeEach
    void setUp() {
        settingsService = mock(CompanySettingsService.class);
        calculator = new AttendanceCalculator();
    }

    private Attendance buildAttendance(LocalDate date, LocalTime checkIn, LocalTime checkOut,
                                        boolean isLate, int lateMinutes, BigDecimal totalHours,
                                        AttendanceStatus status) {
        Attendance attendance = new Attendance();
        attendance.setAttendanceDate(date);
        attendance.setCheckInTime(checkIn);
        attendance.setCheckOutTime(checkOut);
        attendance.setIsLate(isLate);
        attendance.setLateMinutes(lateMinutes);
        attendance.setTotalHours(totalHours);
        attendance.setStatus(status);
        return attendance;
    }

    // ---------------------------------------------------------------
    // countPresentDays
    // ---------------------------------------------------------------

    @Test
    void countPresentDays_shouldCountOnlyRecordsWithCheckInTime() {
        List<Attendance> attendances = List.of(
                buildAttendance(LocalDate.now(), LocalTime.of(9, 0), null, false, 0, null, AttendanceStatus.PRESENT),
                buildAttendance(LocalDate.now(), null, null, false, 0, null, AttendanceStatus.ABSENT));

        assertEquals(1, calculator.countPresentDays(attendances));
    }

    @Test
    void countPresentDays_shouldReturnZero_whenEmptyList() {
        assertEquals(0, calculator.countPresentDays(List.of()));
    }

    // ---------------------------------------------------------------
    // countLateDays
    // ---------------------------------------------------------------

    @Test
    void countLateDays_shouldCountOnlyRecordsMarkedLate() {
        List<Attendance> attendances = List.of(
                buildAttendance(LocalDate.now(), LocalTime.of(10, 15), null, true, 15, null, AttendanceStatus.PRESENT),
                buildAttendance(LocalDate.now(), LocalTime.of(9, 0), null, false, 0, null, AttendanceStatus.PRESENT));

        assertEquals(1, calculator.countLateDays(attendances));
    }

    @Test
    void countLateDays_shouldIgnoreNullIsLate() {
        Attendance attendance = new Attendance();
        attendance.setIsLate(null);

        assertEquals(0, calculator.countLateDays(List.of(attendance)));
    }

    // ---------------------------------------------------------------
    // calculateTotalHours
    // ---------------------------------------------------------------

    @Test
    void calculateTotalHours_shouldSumNonNullValues() {
        List<Attendance> attendances = List.of(
                buildAttendance(LocalDate.now(), null, null, false, 0, new BigDecimal("8.5"), AttendanceStatus.PRESENT),
                buildAttendance(LocalDate.now(), null, null, false, 0, new BigDecimal("7.25"), AttendanceStatus.PRESENT),
                buildAttendance(LocalDate.now(), null, null, false, 0, null, AttendanceStatus.ABSENT));

        assertEquals(new BigDecimal("15.75"), calculator.calculateTotalHours(attendances));
    }

    @Test
    void calculateTotalHours_shouldReturnZero_whenNoHoursRecorded() {
        assertEquals(BigDecimal.ZERO, calculator.calculateTotalHours(List.of()));
    }

    // ---------------------------------------------------------------
    // calculateAttendanceRate
    // ---------------------------------------------------------------

    @Test
    void calculateAttendanceRate_shouldComputePercentage() {
        assertEquals(50.0, calculator.calculateAttendanceRate(10, 20));
    }

    @Test
    void calculateAttendanceRate_shouldReturnZero_whenTotalWorkingDaysZero() {
        assertEquals(0, calculator.calculateAttendanceRate(5, 0));
    }

    // ---------------------------------------------------------------
    // getDefaultWorkingDays
    // ---------------------------------------------------------------

    @Test
    void getDefaultWorkingDays_shouldExcludeSaturdayAndSunday() {
        // 2024-01-01 (Mon) to 2024-01-07 (Sun) => 5 working days.
        int result = calculator.getDefaultWorkingDays(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 7));
        assertEquals(5, result);
    }

    @Test
    void getDefaultWorkingDays_shouldReturnOne_whenSingleWeekday() {
        LocalDate monday = LocalDate.of(2024, 1, 1);
        assertEquals(1, calculator.getDefaultWorkingDays(monday, monday));
    }

    @Test
    void getDefaultWorkingDays_shouldReturnZero_whenSingleWeekendDay() {
        LocalDate saturday = LocalDate.of(2024, 1, 6);
        assertEquals(0, calculator.getDefaultWorkingDays(saturday, saturday));
    }

    // ---------------------------------------------------------------
    // countWorkingDays
    // ---------------------------------------------------------------

    @Test
    void countWorkingDays_shouldUseSettingsService_whenDepartmentPresentAndValueReturned() {
        Department department = new Department();
        department.setId(5L);
        EmployeeUser employee = new EmployeeUser();
        employee.setDepartment(department);
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 31);

        when(settingsService.getWorkingDays(5L, start, end)).thenReturn(22);

        assertEquals(22, calculator.countWorkingDays(employee, start, end, settingsService));
    }

    @Test
    void countWorkingDays_shouldFallBackToDefault_whenSettingsServiceReturnsNull() {
        Department department = new Department();
        department.setId(5L);
        EmployeeUser employee = new EmployeeUser();
        employee.setDepartment(department);
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 7);

        when(settingsService.getWorkingDays(5L, start, end)).thenReturn(null);

        assertEquals(5, calculator.countWorkingDays(employee, start, end, settingsService));
    }

    @Test
    void countWorkingDays_shouldFallBackToDefault_whenDepartmentNull() {
        EmployeeUser employee = new EmployeeUser();
        employee.setDepartment(null);
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 7);

        assertEquals(5, calculator.countWorkingDays(employee, start, end, settingsService));
        verifyNoInteractions(settingsService);
    }

    // ---------------------------------------------------------------
    // buildSummaryMap
    // ---------------------------------------------------------------

    @Test
    void buildSummaryMap_shouldComputeAllFieldsCorrectly() {
        User user = new User();
        user.setFullName("Jane Doe");
        Department department = new Department();
        department.setName("Engineering");
        Designation designation = new Designation();
        designation.setTitle("Software Engineer");

        EmployeeUser employee = new EmployeeUser();
        employee.setId(10L);
        employee.setUser(user);
        employee.setEmployeeCode("EMP001");
        employee.setDepartment(department);
        employee.setDesignation(designation);

        Attendance present = buildAttendance(LocalDate.of(2024, 1, 2), LocalTime.of(9, 0), LocalTime.of(18, 0),
                false, 0, new BigDecimal("9.0"), AttendanceStatus.PRESENT);
        Attendance late = buildAttendance(LocalDate.of(2024, 1, 3), LocalTime.of(10, 15), LocalTime.of(18, 0),
                true, 15, new BigDecimal("7.75"), AttendanceStatus.PRESENT);
        List<Attendance> attendances = List.of(present, late);

        Map<String, Object> summary = calculator.buildSummaryMap(employee, 2024, 1, attendances, 20, settingsService);

        assertEquals(10L, summary.get("employeeId"));
        assertEquals("Jane Doe", summary.get("employeeName"));
        assertEquals("EMP001", summary.get("employeeCode"));
        assertEquals("Engineering", summary.get("departmentName"));
        assertEquals("Software Engineer", summary.get("designationTitle"));
        assertEquals(2024, summary.get("year"));
        assertEquals(1, summary.get("month"));
        assertEquals(20, summary.get("totalWorkingDays"));
        assertEquals(2L, summary.get("presentDays"));
        assertEquals(18L, summary.get("absentDays"));
        assertEquals(1L, summary.get("lateDays"));
        assertEquals(new BigDecimal("16.75"), summary.get("totalHours"));
        assertEquals(10.0, summary.get("attendanceRate"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dailyBreakdown = (List<Map<String, Object>>) summary.get("dailyBreakdown");
        assertEquals(2, dailyBreakdown.size());
        assertEquals("2024-01-02", dailyBreakdown.get(0).get("date"));
        assertEquals("09:00", dailyBreakdown.get(0).get("checkIn"));
        assertEquals("PRESENT", dailyBreakdown.get(0).get("status"));
    }

    @Test
    void buildSummaryMap_shouldHandleMissingOptionalRelations() {
        EmployeeUser employee = new EmployeeUser();
        employee.setId(11L);
        employee.setUser(null);
        employee.setDepartment(null);
        employee.setDesignation(null);

        Map<String, Object> summary = calculator.buildSummaryMap(employee, 2024, 2, List.of(), 20, settingsService);

        assertEquals("Unknown", summary.get("employeeName"));
        assertNull(summary.get("departmentName"));
        assertNull(summary.get("designationTitle"));
        assertEquals(0L, summary.get("presentDays"));
        assertEquals(20L, summary.get("absentDays"));
        assertEquals(BigDecimal.ZERO, summary.get("totalHours"));
        assertEquals(0.0, summary.get("attendanceRate"));
    }

    // ---------------------------------------------------------------
    // getLateThreshold / getOfficialEndTime / getOfficialStartTime / getWorkingHoursPerDay
    // ---------------------------------------------------------------

    @Test
    void getLateThreshold_shouldReturnConfiguredValue_whenPresent() {
        when(settingsService.getLateThreshold()).thenReturn(LocalTime.of(9, 30));
        assertEquals(LocalTime.of(9, 30), calculator.getLateThreshold(settingsService));
    }

    @Test
    void getLateThreshold_shouldReturnDefault_whenNull() {
        when(settingsService.getLateThreshold()).thenReturn(null);
        assertEquals(LocalTime.of(10, 0), calculator.getLateThreshold(settingsService));
    }

    @Test
    void getOfficialEndTime_shouldReturnConfiguredValue_whenPresent() {
        when(settingsService.getOfficialEndTime()).thenReturn(LocalTime.of(17, 30));
        assertEquals(LocalTime.of(17, 30), calculator.getOfficialEndTime(settingsService));
    }

    @Test
    void getOfficialEndTime_shouldReturnDefault_whenNull() {
        when(settingsService.getOfficialEndTime()).thenReturn(null);
        assertEquals(LocalTime.of(18, 0), calculator.getOfficialEndTime(settingsService));
    }

    @Test
    void getOfficialStartTime_shouldReturnConfiguredValue_whenPresent() {
        when(settingsService.getOfficialStartTime()).thenReturn(LocalTime.of(8, 45));
        assertEquals(LocalTime.of(8, 45), calculator.getOfficialStartTime(settingsService));
    }

    @Test
    void getOfficialStartTime_shouldReturnDefault_whenNull() {
        when(settingsService.getOfficialStartTime()).thenReturn(null);
        assertEquals(LocalTime.of(9, 0), calculator.getOfficialStartTime(settingsService));
    }

    @Test
    void getWorkingHoursPerDay_shouldReturnConfiguredValue_whenPresent() {
        when(settingsService.getWorkingHoursPerDay()).thenReturn(8);
        assertEquals(8, calculator.getWorkingHoursPerDay(settingsService));
    }

    @Test
    void getWorkingHoursPerDay_shouldReturnDefault_whenNull() {
        when(settingsService.getWorkingHoursPerDay()).thenReturn(null);
        assertEquals(9, calculator.getWorkingHoursPerDay(settingsService));
    }
}
