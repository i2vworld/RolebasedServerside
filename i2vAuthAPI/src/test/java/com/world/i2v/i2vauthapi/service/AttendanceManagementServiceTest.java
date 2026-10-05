package com.world.i2v.i2vauthapi.service;

import com.world.i2v.i2vauthapi.dto.request.AttendanceRequest;
import com.world.i2v.i2vauthapi.dto.response.AttendanceResponse;
import com.world.i2v.i2vauthapi.dto.response.AttendanceStatisticsResponse;
import com.world.i2v.i2vauthapi.entity.Attendance;
import com.world.i2v.i2vauthapi.entity.Attendance.AttendanceStatus;
import com.world.i2v.i2vauthapi.entity.Department;
import com.world.i2v.i2vauthapi.entity.Designation;
import com.world.i2v.i2vauthapi.entity.EmployeeUser;
import com.world.i2v.i2vauthapi.entity.User;
import com.world.i2v.i2vauthapi.exception.BadRequestException;
import com.world.i2v.i2vauthapi.exception.ResourceNotFoundException;
import com.world.i2v.i2vauthapi.repository.AttendanceRepository;
import com.world.i2v.i2vauthapi.repository.EmployeeUserRepository;
import com.world.i2v.i2vauthapi.repository.HolidayRepository;
import com.world.i2v.i2vauthapi.repository.LeaveRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Smoke + unit test suite for {@link AttendanceManagementService}, the admin-facing
 * service backing attendance management endpoints (marking/updating/deleting
 * attendance, company-wide statistics, bulk marking, pending attendance, CSV export).
 * Uses a real {@link AttendanceCalculator} (pure logic) with a mocked
 * {@link CompanySettingsService} so default thresholds are deterministic.
 */
class AttendanceManagementServiceTest {

    private AttendanceRepository attendanceRepository;
    private EmployeeUserRepository employeeRepository;
    private AuditService auditService;
    private CompanySettingsService settingsService;
    private AttendanceCalculator calculator;
    private HolidayRepository holidayRepository;
    private LeaveRequestRepository leaveRequestRepository;
    private AttendanceManagementService service;

    @BeforeEach
    void setUp() {
        attendanceRepository = mock(AttendanceRepository.class);
        employeeRepository = mock(EmployeeUserRepository.class);
        auditService = mock(AuditService.class);
        settingsService = mock(CompanySettingsService.class);
        calculator = new AttendanceCalculator();
        holidayRepository = mock(HolidayRepository.class);
        leaveRequestRepository = mock(LeaveRequestRepository.class);
        service = new AttendanceManagementService(attendanceRepository, employeeRepository,
                auditService, settingsService, calculator, holidayRepository, leaveRequestRepository);
    }

    private EmployeeUser buildEmployee(Long id, String code, String fullName, String email,
                                        String deptName, String designationTitle) {
        EmployeeUser employee = new EmployeeUser();
        employee.setId(id);
        employee.setEmployeeCode(code);
        if (fullName != null) {
            User user = new User();
            user.setFullName(fullName);
            user.setEmail(email);
            employee.setUser(user);
        }
        if (deptName != null) {
            Department department = new Department();
            department.setId(1L);
            department.setName(deptName);
            employee.setDepartment(department);
        }
        if (designationTitle != null) {
            Designation designation = new Designation();
            designation.setTitle(designationTitle);
            employee.setDesignation(designation);
        }
        return employee;
    }

    private Attendance buildAttendance(Long id, EmployeeUser employee, LocalDate date, LocalTime checkIn) {
        Attendance attendance = new Attendance();
        attendance.setId(id);
        attendance.setEmployee(employee);
        attendance.setAttendanceDate(date);
        attendance.setCheckInTime(checkIn);
        attendance.setStatus(AttendanceStatus.PRESENT);
        return attendance;
    }

    // ---------------------------------------------------------------
    // getEmployeeAttendancePageable
    // ---------------------------------------------------------------

    @Test
    void getEmployeeAttendancePageable_shouldReturnMappedPage() {
        EmployeeUser employee = buildEmployee(1L, "EMP1", "John", "john@x.com", "Eng", "Dev");
        Attendance attendance = buildAttendance(1L, employee, LocalDate.of(2024, 1, 2), LocalTime.of(9, 0));
        Pageable pageable = PageRequest.of(0, 10);
        Page<Attendance> page = new PageImpl<>(List.of(attendance), pageable, 1);

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(attendanceRepository.findByEmployeeIdAndAttendanceDateBetweenPageable(
                eq(1L), any(), any(), eq(pageable))).thenReturn(page);

        Page<AttendanceResponse> result = service.getEmployeeAttendancePageable(
                1L, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("John", result.getContent().get(0).getEmployeeName());
    }

    @Test
    void getEmployeeAttendancePageable_shouldThrow_whenEmployeeNotFound() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                service.getEmployeeAttendancePageable(99L, LocalDate.now(), LocalDate.now(), PageRequest.of(0, 10)));
    }

    // ---------------------------------------------------------------
    // getEmployeeTodayAttendance
    // ---------------------------------------------------------------

    @Test
    void getEmployeeTodayAttendance_shouldReturnResponse_whenAttendanceExists() {
        EmployeeUser employee = buildEmployee(1L, "EMP1", "John", "john@x.com", "Eng", "Dev");
        Attendance attendance = buildAttendance(1L, employee, LocalDate.now(), LocalTime.of(9, 0));

        when(attendanceRepository.findByEmployeeIdAndAttendanceDate(1L, LocalDate.now()))
                .thenReturn(Optional.of(attendance));
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        AttendanceResponse response = service.getEmployeeTodayAttendance(1L);

        assertNotNull(response);
        assertEquals("John", response.getEmployeeName());
    }

    @Test
    void getEmployeeTodayAttendance_shouldReturnNull_whenNoAttendanceToday() {
        when(attendanceRepository.findByEmployeeIdAndAttendanceDate(1L, LocalDate.now()))
                .thenReturn(Optional.empty());

        assertNull(service.getEmployeeTodayAttendance(1L));
        verify(employeeRepository, never()).findById(anyLong());
    }

    // ---------------------------------------------------------------
    // getEmployeeAttendanceSummary
    // ---------------------------------------------------------------

    @Test
    void getEmployeeAttendanceSummary_shouldBuildSummary() {
        EmployeeUser employee = buildEmployee(1L, "EMP1", "John", "john@x.com", "Eng", "Dev");
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(attendanceRepository.findByEmployeeIdAndAttendanceDateBetween(eq(1L), any(), any()))
                .thenReturn(List.of());
        when(settingsService.getWorkingDays(eq(1L), any(), any())).thenReturn(null);

        Map<String, Object> summary = service.getEmployeeAttendanceSummary(1L, 2024, 1);

        assertEquals(2024, summary.get("year"));
        assertEquals(1, summary.get("month"));
        assertEquals("John", summary.get("employeeName"));
    }

    @Test
    void getEmployeeAttendanceSummary_shouldThrow_whenEmployeeNotFound() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                service.getEmployeeAttendanceSummary(99L, 2024, 1));
    }

    // ---------------------------------------------------------------
    // getAllEmployeesWithAttendancePageable
    // ---------------------------------------------------------------

    @Test
    void getAllEmployeesWithAttendancePageable_shouldBuildMapForEachEmployee() {
        EmployeeUser withDetails = buildEmployee(1L, "EMP1", "John", "john@x.com", "Eng", "Dev");
        EmployeeUser withoutDetails = buildEmployee(2L, "EMP2", null, null, null, null);
        Pageable pageable = PageRequest.of(0, 10);
        Page<EmployeeUser> employeePage = new PageImpl<>(List.of(withDetails, withoutDetails), pageable, 2);

        when(employeeRepository.findAll(pageable)).thenReturn(employeePage);
        when(attendanceRepository.findByEmployeeIdAndAttendanceDateBetween(eq(1L), any(), any()))
                .thenReturn(List.of(buildAttendance(1L, withDetails, LocalDate.of(2024, 1, 2), LocalTime.of(9, 0))));
        when(attendanceRepository.findByEmployeeIdAndAttendanceDateBetween(eq(2L), any(), any()))
                .thenReturn(List.of());
        when(settingsService.getWorkingDays(eq(1L), any(), any())).thenReturn(null);

        Page<Map<String, Object>> result = service.getAllEmployeesWithAttendancePageable(2024, 1, pageable);

        assertEquals(2, result.getContent().size());
        Map<String, Object> first = result.getContent().get(0);
        assertEquals("John", first.get("name"));
        assertEquals("Eng", first.get("department"));
        Map<String, Object> second = result.getContent().get(1);
        assertEquals("Unknown", second.get("name"));
        assertEquals("Unknown", second.get("department"));
    }

    // ---------------------------------------------------------------
    // markAttendance
    // ---------------------------------------------------------------

    @Test
    void markAttendance_shouldCreateAttendance_andComputeHours_whenNotLate() {
        EmployeeUser employee = buildEmployee(1L, "EMP1", "John", "john@x.com", "Eng", "Dev");
        AttendanceRequest request = new AttendanceRequest();
        request.setEmployeeId(1L);
        request.setAttendanceDate(LocalDate.of(2024, 1, 2));
        request.setCheckInTime(LocalTime.of(9, 0));
        request.setCheckOutTime(LocalTime.of(18, 0));
        request.setStatus(AttendanceStatus.PRESENT);

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(attendanceRepository.existsByEmployeeIdAndAttendanceDate(1L, request.getAttendanceDate()))
                .thenReturn(false);
        when(settingsService.getLateThreshold()).thenReturn(null);
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(inv -> {
            Attendance a = inv.getArgument(0);
            a.setId(100L);
            return a;
        });

        AttendanceResponse response = service.markAttendance(request);

        assertEquals(100L, response.getId());
        assertEquals(new BigDecimal("9.00"), response.getTotalHours());
        assertFalse(response.getIsLate());
        assertEquals(0, response.getLateMinutes());
        verify(auditService).logAction(isNull(), eq("MARK_ATTENDANCE"), eq("ATTENDANCE"), eq(100L), any());
    }

    @Test
    void markAttendance_shouldMarkLate_whenCheckInAfterThreshold() {
        EmployeeUser employee = buildEmployee(1L, "EMP1", "John", "john@x.com", "Eng", "Dev");
        AttendanceRequest request = new AttendanceRequest();
        request.setEmployeeId(1L);
        request.setAttendanceDate(LocalDate.of(2024, 1, 2));
        request.setCheckInTime(LocalTime.of(10, 15));
        request.setStatus(AttendanceStatus.PRESENT);

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(attendanceRepository.existsByEmployeeIdAndAttendanceDate(1L, request.getAttendanceDate()))
                .thenReturn(false);
        when(settingsService.getLateThreshold()).thenReturn(null); // default 10:00
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(inv -> inv.getArgument(0));

        AttendanceResponse response = service.markAttendance(request);

        assertTrue(response.getIsLate());
        assertEquals(15, response.getLateMinutes());
    }

    @Test
    void markAttendance_shouldThrow_whenEmployeeNotFound() {
        AttendanceRequest request = new AttendanceRequest();
        request.setEmployeeId(99L);
        request.setAttendanceDate(LocalDate.now());
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.markAttendance(request));
    }

    @Test
    void markAttendance_shouldThrow_whenAttendanceAlreadyMarked() {
        EmployeeUser employee = buildEmployee(1L, "EMP1", "John", "john@x.com", "Eng", "Dev");
        AttendanceRequest request = new AttendanceRequest();
        request.setEmployeeId(1L);
        request.setAttendanceDate(LocalDate.of(2024, 1, 2));

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(attendanceRepository.existsByEmployeeIdAndAttendanceDate(1L, request.getAttendanceDate()))
                .thenReturn(true);

        assertThrows(BadRequestException.class, () -> service.markAttendance(request));
        verify(attendanceRepository, never()).save(any());
    }

    // ---------------------------------------------------------------
    // updateAttendance
    // ---------------------------------------------------------------

    @Test
    void updateAttendance_shouldMarkLate_andSave() {
        EmployeeUser employee = buildEmployee(1L, "EMP1", "John", "john@x.com", "Eng", "Dev");
        Attendance existing = buildAttendance(5L, employee, LocalDate.of(2024, 1, 2), LocalTime.of(9, 0));

        AttendanceRequest request = new AttendanceRequest();
        request.setCheckInTime(LocalTime.of(10, 30));
        request.setCheckOutTime(LocalTime.of(18, 0));
        request.setStatus(AttendanceStatus.PRESENT);

        when(attendanceRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(settingsService.getLateThreshold()).thenReturn(null);
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(inv -> inv.getArgument(0));

        AttendanceResponse response = service.updateAttendance(5L, request);

        assertTrue(response.getIsLate());
        assertEquals(30, response.getLateMinutes());
        verify(auditService).logAction(isNull(), eq("UPDATE_ATTENDANCE"), eq("ATTENDANCE"), eq(5L), any());
    }

    @Test
    void updateAttendance_shouldClearLateFlags_whenCheckInBeforeThreshold() {
        EmployeeUser employee = buildEmployee(1L, "EMP1", "John", "john@x.com", "Eng", "Dev");
        Attendance existing = buildAttendance(5L, employee, LocalDate.of(2024, 1, 2), LocalTime.of(10, 15));
        existing.setIsLate(true);
        existing.setLateMinutes(15);

        AttendanceRequest request = new AttendanceRequest();
        request.setCheckInTime(LocalTime.of(9, 0));
        request.setStatus(AttendanceStatus.PRESENT);

        when(attendanceRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(settingsService.getLateThreshold()).thenReturn(null);
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(inv -> inv.getArgument(0));

        AttendanceResponse response = service.updateAttendance(5L, request);

        assertFalse(response.getIsLate());
        assertEquals(0, response.getLateMinutes());
    }

    @Test
    void updateAttendance_shouldThrow_whenNotFound() {
        when(attendanceRepository.findById(999L)).thenReturn(Optional.empty());
        AttendanceRequest request = new AttendanceRequest();

        assertThrows(ResourceNotFoundException.class, () -> service.updateAttendance(999L, request));
    }

    // ---------------------------------------------------------------
    // deleteAttendance
    // ---------------------------------------------------------------

    @Test
    void deleteAttendance_shouldDelete_andLogAudit() {
        Attendance attendance = buildAttendance(5L, null, LocalDate.now(), LocalTime.of(9, 0));
        when(attendanceRepository.findById(5L)).thenReturn(Optional.of(attendance));

        service.deleteAttendance(5L);

        verify(attendanceRepository).delete(attendance);
        verify(auditService).logAction(isNull(), eq("DELETE_ATTENDANCE"), eq("ATTENDANCE"), eq(5L), any());
    }

    @Test
    void deleteAttendance_shouldThrow_whenNotFound() {
        when(attendanceRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.deleteAttendance(999L));
    }

    // ---------------------------------------------------------------
    // getCompanyAttendanceStatistics
    // ---------------------------------------------------------------

    @Test
    void getCompanyAttendanceStatistics_shouldAggregateAcrossEmployees() {
        EmployeeUser e1 = buildEmployee(1L, "EMP1", "John", "j@x.com", "Eng", "Dev");
        EmployeeUser e2 = buildEmployee(2L, "EMP2", "Jane", "ja@x.com", "Eng", "Dev");
        when(employeeRepository.findAll()).thenReturn(List.of(e1, e2));
        when(attendanceRepository.findByEmployeeIdAndAttendanceDateBetween(eq(1L), any(), any()))
                .thenReturn(List.of(buildAttendance(1L, e1, LocalDate.of(2024, 1, 2), LocalTime.of(9, 0))));
        when(attendanceRepository.findByEmployeeIdAndAttendanceDateBetween(eq(2L), any(), any()))
                .thenReturn(List.of());
        when(settingsService.getWorkingDays(eq(1L), any(), any())).thenReturn(null);

        AttendanceStatisticsResponse stats = service.getCompanyAttendanceStatistics(2024, 1);

        assertEquals(2, stats.getTotalEmployees());
        assertEquals(1, stats.getTotalPresent());
        assertEquals(0, stats.getTotalLate());
    }

    @Test
    void getCompanyAttendanceStatistics_shouldReturnZeroRate_whenNoEmployees() {
        when(employeeRepository.findAll()).thenReturn(List.of());

        AttendanceStatisticsResponse stats = service.getCompanyAttendanceStatistics(2024, 1);

        assertEquals(0, stats.getTotalEmployees());
        assertEquals(0.0, stats.getOverallAttendanceRate());
    }

    // ---------------------------------------------------------------
    // bulkMarkAttendance
    // ---------------------------------------------------------------

    @Test
    void bulkMarkAttendance_shouldMarkPresent_andSetCheckInOutFromSettings() {
        EmployeeUser employee = buildEmployee(1L, "EMP1", "John", "j@x.com", "Eng", "Dev");
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(attendanceRepository.existsByEmployeeIdAndAttendanceDate(1L, LocalDate.of(2024, 1, 2)))
                .thenReturn(false);
        when(settingsService.getOfficialStartTime()).thenReturn(null);
        when(settingsService.getOfficialEndTime()).thenReturn(null);
        when(settingsService.getWorkingHoursPerDay()).thenReturn(null);

        Map<String, Object> result = service.bulkMarkAttendance(
                LocalDate.of(2024, 1, 2), List.of(1L), "PRESENT");

        assertEquals(1, result.get("successCount"));
        assertEquals(0, result.get("failedCount"));
        verify(attendanceRepository).save(argThat(a ->
                a.getCheckInTime().equals(LocalTime.of(9, 0)) && a.getCheckOutTime().equals(LocalTime.of(18, 0))));
    }

    @Test
    void bulkMarkAttendance_shouldSkip_whenAlreadyExists() {
        EmployeeUser employee = buildEmployee(1L, "EMP1", "John", "j@x.com", "Eng", "Dev");
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(attendanceRepository.existsByEmployeeIdAndAttendanceDate(1L, LocalDate.of(2024, 1, 2)))
                .thenReturn(true);

        Map<String, Object> result = service.bulkMarkAttendance(
                LocalDate.of(2024, 1, 2), List.of(1L), "PRESENT");

        assertEquals(0, result.get("successCount"));
        assertEquals(1, result.get("failedCount"));
        @SuppressWarnings("unchecked")
        List<String> errors = (List<String>) result.get("errors");
        assertEquals(1, errors.size());
        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void bulkMarkAttendance_shouldRecordFailure_whenEmployeeNotFound() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        Map<String, Object> result = service.bulkMarkAttendance(
                LocalDate.of(2024, 1, 2), List.of(99L), "PRESENT");

        assertEquals(0, result.get("successCount"));
        assertEquals(1, result.get("failedCount"));
    }

    @Test
    void bulkMarkAttendance_shouldRecordFailure_whenStatusInvalid() {
        EmployeeUser employee = buildEmployee(1L, "EMP1", "John", "j@x.com", "Eng", "Dev");
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(attendanceRepository.existsByEmployeeIdAndAttendanceDate(1L, LocalDate.of(2024, 1, 2)))
                .thenReturn(false);

        Map<String, Object> result = service.bulkMarkAttendance(
                LocalDate.of(2024, 1, 2), List.of(1L), "NOT_A_STATUS");

        assertEquals(0, result.get("successCount"));
        assertEquals(1, result.get("failedCount"));
    }

    @Test
    void bulkMarkAttendance_shouldNotSetCheckInOut_whenStatusIsAbsent() {
        EmployeeUser employee = buildEmployee(1L, "EMP1", "John", "j@x.com", "Eng", "Dev");
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(attendanceRepository.existsByEmployeeIdAndAttendanceDate(1L, LocalDate.of(2024, 1, 2)))
                .thenReturn(false);

        Map<String, Object> result = service.bulkMarkAttendance(
                LocalDate.of(2024, 1, 2), List.of(1L), "ABSENT");

        assertEquals(1, result.get("successCount"));
        verify(attendanceRepository).save(argThat(a -> a.getCheckInTime() == null && a.getCheckOutTime() == null));
    }

    // ---------------------------------------------------------------
    // getPendingAttendance
    // ---------------------------------------------------------------

    @Test
    void getPendingAttendance_shouldListDatesWithoutAttendance() {
        EmployeeUser employee = buildEmployee(1L, "EMP1", "John", "j@x.com", "Eng", "Dev");
        when(employeeRepository.findAll()).thenReturn(List.of(employee));

        LocalDate day1 = LocalDate.of(2024, 1, 1);
        LocalDate day2 = LocalDate.of(2024, 1, 2);
        when(attendanceRepository.existsByEmployeeIdAndAttendanceDate(1L, day1)).thenReturn(true);
        when(attendanceRepository.existsByEmployeeIdAndAttendanceDate(1L, day2)).thenReturn(false);

        List<Map<String, Object>> pending = service.getPendingAttendance(day1, day2);

        assertEquals(1, pending.size());
        assertEquals("2024-01-02", pending.get(0).get("date"));
        assertEquals("John", pending.get(0).get("employeeName"));
    }

    // ---------------------------------------------------------------
    // exportAttendanceReport
    // ---------------------------------------------------------------

    @Test
    void exportAttendanceReport_shouldBuildCsv_forAllEmployees() {
        EmployeeUser employee = buildEmployee(1L, "EMP1", "John", "j@x.com", "Eng", "Dev");
        Attendance attendance = buildAttendance(1L, employee, LocalDate.of(2024, 1, 2), LocalTime.of(9, 0));
        attendance.setCheckOutTime(LocalTime.of(18, 0));
        attendance.setTotalHours(new BigDecimal("9.00"));

        when(employeeRepository.findAll()).thenReturn(List.of(employee));
        when(attendanceRepository.findByEmployeeIdAndAttendanceDateBetween(eq(1L), any(), any()))
                .thenReturn(List.of(attendance));

        ResponseEntity<byte[]> response = service.exportAttendanceReport(2024, 1, null);

        String csv = new String(response.getBody());
        assertTrue(csv.contains("Employee Code,Employee Name"));
        assertTrue(csv.contains("EMP1,John,Eng,Dev"));
        verify(employeeRepository, never()).findByDepartmentId(any());
    }

    @Test
    void exportAttendanceReport_shouldFilterByDepartment_whenDepartmentIdProvided() {
        EmployeeUser employee = buildEmployee(1L, "EMP1", "John", "j@x.com", "Eng", "Dev");
        when(employeeRepository.findByDepartmentId(1L)).thenReturn(List.of(employee));
        when(attendanceRepository.findByEmployeeIdAndAttendanceDateBetween(eq(1L), any(), any()))
                .thenReturn(List.of());

        ResponseEntity<byte[]> response = service.exportAttendanceReport(2024, 1, 1L);

        assertNotNull(response.getBody());
        verify(employeeRepository, never()).findAll();
    }
}
