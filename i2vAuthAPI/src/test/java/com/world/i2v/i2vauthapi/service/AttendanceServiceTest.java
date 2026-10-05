package com.world.i2v.i2vauthapi.service;

import com.world.i2v.i2vauthapi.entity.Attendance;
import com.world.i2v.i2vauthapi.entity.Attendance.AttendanceStatus;
import com.world.i2v.i2vauthapi.entity.Department;
import com.world.i2v.i2vauthapi.entity.EmployeeUser;
import com.world.i2v.i2vauthapi.entity.User;
import com.world.i2v.i2vauthapi.exception.BadRequestException;
import com.world.i2v.i2vauthapi.repository.AttendanceRepository;
import com.world.i2v.i2vauthapi.repository.EmployeeUserRepository;
import com.world.i2v.i2vauthapi.repository.HolidayRepository;
import com.world.i2v.i2vauthapi.repository.LeaveRequestRepository;
import com.world.i2v.i2vauthapi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Smoke + unit test suite for {@link AttendanceService}.
 * Mirrors the style/conventions used in {@link DesignationServiceTest} and
 * {@link DepartmentServiceTest}: plain Mockito mocks (no
 * {@code @ExtendWith(MockitoExtension.class)}), covering every public method
 * and every branch of {@link AttendanceService}.
 *
 * Note: {@link AttendanceCalculator} is mocked in full, so its late-threshold /
 * official-end-time results are fully controlled by the test rather than by the
 * real system clock. This lets branch coverage (late vs. not-late, early vs.
 * not-early) be deterministic regardless of when the test suite runs.
 */
class AttendanceServiceTest {

    private AttendanceRepository attendanceRepository;
    private EmployeeUserRepository employeeRepository;
    private UserRepository userRepository;
    private AuditService auditService;
    private CompanySettingsService settingsService;
    private AttendanceCalculator calculator;
    private HolidayRepository holidayRepository;
    private LeaveRequestRepository leaveRequestRepository;
    private AttendanceService attendanceService;

    @BeforeEach
    void setUp() {
        attendanceRepository = mock(AttendanceRepository.class);
        employeeRepository = mock(EmployeeUserRepository.class);
        userRepository = mock(UserRepository.class);
        auditService = mock(AuditService.class);
        settingsService = mock(CompanySettingsService.class);
        calculator = mock(AttendanceCalculator.class);
        holidayRepository = mock(HolidayRepository.class);
        leaveRequestRepository = mock(LeaveRequestRepository.class);
        attendanceService = new AttendanceService(attendanceRepository, employeeRepository, userRepository,
                auditService, settingsService, calculator, holidayRepository, leaveRequestRepository);
    }

    private User buildUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setFullName("Test User");
        return user;
    }

    private EmployeeUser buildEmployee(Long id, Department department) {
        EmployeeUser employee = new EmployeeUser();
        employee.setId(id);
        employee.setDepartment(department);
        return employee;
    }

    private Department buildDepartment(Long id) {
        Department department = new Department();
        department.setId(id);
        department.setName("Engineering");
        return department;
    }

    // ---------------------------------------------------------------
    // checkIn
    // ---------------------------------------------------------------

    @Test
    void checkIn_shouldCreateNewRecord_whenNoExistingRecordAndNotLate() {
        User user = buildUser(1L);
        EmployeeUser employee = buildEmployee(10L, null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(employee));
        when(attendanceRepository.findByEmployeeIdAndAttendanceDate(eq(10L), any(LocalDate.class)))
                .thenReturn(Optional.empty());
        // Threshold far in the future so "now" is always before it -> not late.
        when(calculator.getLateThreshold(settingsService)).thenReturn(LocalTime.of(23, 59));
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(inv -> inv.getArgument(0));

        Attendance result = attendanceService.checkIn(1L, "Office", "127.0.0.1");

        assertNotNull(result);
        assertEquals(employee, result.getEmployee());
        assertEquals(LocalDate.now(), result.getAttendanceDate());
        assertNotNull(result.getCheckInTime());
        assertEquals("Office", result.getCheckInLocation());
        assertEquals("127.0.0.1", result.getCheckInIp());
        assertEquals(AttendanceStatus.PRESENT, result.getStatus());
        assertFalse(result.getIsLate());
        assertEquals(0, result.getLateMinutes());
        verify(attendanceRepository).save(any(Attendance.class));
        verify(auditService).logAction(eq(user), eq("CHECK_IN"), eq("ATTENDANCE"), any(), anyString());
    }

    @Test
    void checkIn_shouldMarkLate_whenAfterLateThreshold() {
        User user = buildUser(1L);
        EmployeeUser employee = buildEmployee(10L, null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(employee));
        when(attendanceRepository.findByEmployeeIdAndAttendanceDate(eq(10L), any(LocalDate.class)))
                .thenReturn(Optional.empty());
        // Threshold far in the past so "now" is always after it -> late.
        when(calculator.getLateThreshold(settingsService)).thenReturn(LocalTime.of(0, 1));
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(inv -> inv.getArgument(0));

        Attendance result = attendanceService.checkIn(1L, "Office", "127.0.0.1");

        assertTrue(result.getIsLate());
        assertTrue(result.getLateMinutes() > 0);
    }

    @Test
    void checkIn_shouldReuseExistingRecord_whenPresentButNotYetCheckedIn() {
        User user = buildUser(1L);
        EmployeeUser employee = buildEmployee(10L, null);
        Attendance existing = new Attendance();
        existing.setId(99L);
        existing.setEmployee(employee);
        existing.setAttendanceDate(LocalDate.now());

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(employee));
        when(attendanceRepository.findByEmployeeIdAndAttendanceDate(eq(10L), any(LocalDate.class)))
                .thenReturn(Optional.of(existing));
        when(calculator.getLateThreshold(settingsService)).thenReturn(LocalTime.of(23, 59));
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(inv -> inv.getArgument(0));

        Attendance result = attendanceService.checkIn(1L, "Office", "127.0.0.1");

        assertEquals(99L, result.getId());
        verify(attendanceRepository).save(existing);
    }

    @Test
    void checkIn_shouldThrow_whenUserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> attendanceService.checkIn(1L, "Office", "127.0.0.1"));
        assertEquals("User not found", ex.getMessage());
        verifyNoInteractions(attendanceRepository);
    }

    @Test
    void checkIn_shouldThrow_whenEmployeeNotFound() {
        User user = buildUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.empty());

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> attendanceService.checkIn(1L, "Office", "127.0.0.1"));
        assertEquals("Employee profile not found. Please contact HR.", ex.getMessage());
    }

    @Test
    void checkIn_shouldThrow_whenAlreadyCheckedInToday() {
        User user = buildUser(1L);
        EmployeeUser employee = buildEmployee(10L, null);
        Attendance existing = new Attendance();
        existing.setCheckInTime(LocalTime.of(9, 15));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(employee));
        when(attendanceRepository.findByEmployeeIdAndAttendanceDate(eq(10L), any(LocalDate.class)))
                .thenReturn(Optional.of(existing));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> attendanceService.checkIn(1L, "Office", "127.0.0.1"));
        assertTrue(ex.getMessage().contains("Already checked in today at 09:15"));
        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void checkIn_shouldThrow_whenWeeklyOffForDepartment() {
        User user = buildUser(1L);
        Department department = buildDepartment(5L);
        EmployeeUser employee = buildEmployee(10L, department);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(employee));
        when(settingsService.isWeeklyOff(eq(5L), any(LocalDate.class))).thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> attendanceService.checkIn(1L, "Office", "127.0.0.1"));
        assertEquals("Today is a weekly off for your department. Cannot check in.", ex.getMessage());
        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void checkIn_shouldSkipWeeklyOffCheck_whenDepartmentNull() {
        User user = buildUser(1L);
        EmployeeUser employee = buildEmployee(10L, null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(employee));
        when(attendanceRepository.findByEmployeeIdAndAttendanceDate(eq(10L), any(LocalDate.class)))
                .thenReturn(Optional.empty());
        when(calculator.getLateThreshold(settingsService)).thenReturn(LocalTime.of(23, 59));
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> attendanceService.checkIn(1L, "Office", "127.0.0.1"));
        verifyNoInteractions(settingsService);
    }

    @Test
    void checkIn_shouldSanitizeLocation_whenPermissionDenied() {
        User user = buildUser(1L);
        EmployeeUser employee = buildEmployee(10L, null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(employee));
        when(attendanceRepository.findByEmployeeIdAndAttendanceDate(eq(10L), any(LocalDate.class)))
                .thenReturn(Optional.empty());
        when(calculator.getLateThreshold(settingsService)).thenReturn(LocalTime.of(23, 59));
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(inv -> inv.getArgument(0));

        Attendance result = attendanceService.checkIn(1L, "Permission Denied", "127.0.0.1");

        assertNull(result.getCheckInLocation());
    }

    @Test
    void checkIn_shouldSanitizeLocation_whenBlank() {
        User user = buildUser(1L);
        EmployeeUser employee = buildEmployee(10L, null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(employee));
        when(attendanceRepository.findByEmployeeIdAndAttendanceDate(eq(10L), any(LocalDate.class)))
                .thenReturn(Optional.empty());
        when(calculator.getLateThreshold(settingsService)).thenReturn(LocalTime.of(23, 59));
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(inv -> inv.getArgument(0));

        Attendance result = attendanceService.checkIn(1L, "   ", "127.0.0.1");

        assertNull(result.getCheckInLocation());
    }

    @Test
    void checkIn_shouldKeepLocation_whenNull() {
        User user = buildUser(1L);
        EmployeeUser employee = buildEmployee(10L, null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(employee));
        when(attendanceRepository.findByEmployeeIdAndAttendanceDate(eq(10L), any(LocalDate.class)))
                .thenReturn(Optional.empty());
        when(calculator.getLateThreshold(settingsService)).thenReturn(LocalTime.of(23, 59));
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(inv -> inv.getArgument(0));

        Attendance result = attendanceService.checkIn(1L, null, "127.0.0.1");

        assertNull(result.getCheckInLocation());
    }

    // ---------------------------------------------------------------
    // checkOut
    // ---------------------------------------------------------------

    @Test
    void checkOut_shouldCompleteSuccessfully_whenNotEarly() {
        User user = buildUser(1L);
        EmployeeUser employee = buildEmployee(10L, null);
        Attendance existing = new Attendance();
        existing.setCheckInTime(LocalTime.of(9, 0));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(employee));
        when(attendanceRepository.findByEmployeeIdAndAttendanceDate(eq(10L), any(LocalDate.class)))
                .thenReturn(Optional.of(existing));
        // Official end time far in the past so "now" is always after it -> not early.
        when(calculator.getOfficialEndTime(settingsService)).thenReturn(LocalTime.of(0, 1));
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(inv -> inv.getArgument(0));

        Attendance result = attendanceService.checkOut(1L, "Office", "127.0.0.1");

        assertNotNull(result.getCheckOutTime());
        assertEquals("Office", result.getCheckOutLocation());
        assertEquals("127.0.0.1", result.getCheckOutIp());
        assertNotNull(result.getTotalHours());
        assertFalse(result.getEarlyDeparture());
        assertEquals(0, result.getEarlyMinutes());
        verify(auditService).logAction(eq(user), eq("CHECK_OUT"), eq("ATTENDANCE"), any(), anyString());
    }

    @Test
    void checkOut_shouldMarkEarlyDeparture_whenBeforeOfficialEndTime() {
        User user = buildUser(1L);
        EmployeeUser employee = buildEmployee(10L, null);
        Attendance existing = new Attendance();
        existing.setCheckInTime(LocalTime.of(9, 0));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(employee));
        when(attendanceRepository.findByEmployeeIdAndAttendanceDate(eq(10L), any(LocalDate.class)))
                .thenReturn(Optional.of(existing));
        // Official end time far in the future so "now" is always before it -> early.
        when(calculator.getOfficialEndTime(settingsService)).thenReturn(LocalTime.of(23, 59));
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(inv -> inv.getArgument(0));

        Attendance result = attendanceService.checkOut(1L, "Office", "127.0.0.1");

        assertTrue(result.getEarlyDeparture());
        assertTrue(result.getEarlyMinutes() > 0);
    }

    @Test
    void checkOut_shouldThrow_whenUserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> attendanceService.checkOut(1L, "Office", "127.0.0.1"));
        assertEquals("User not found", ex.getMessage());
    }

    @Test
    void checkOut_shouldThrow_whenEmployeeNotFound() {
        User user = buildUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.empty());

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> attendanceService.checkOut(1L, "Office", "127.0.0.1"));
        assertEquals("Employee profile not found", ex.getMessage());
    }

    @Test
    void checkOut_shouldThrow_whenNoCheckInRecordFoundToday() {
        User user = buildUser(1L);
        EmployeeUser employee = buildEmployee(10L, null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(employee));
        when(attendanceRepository.findByEmployeeIdAndAttendanceDate(eq(10L), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> attendanceService.checkOut(1L, "Office", "127.0.0.1"));
        assertEquals("No check-in record found for today. Please check in first.", ex.getMessage());
    }

    @Test
    void checkOut_shouldThrow_whenAlreadyCheckedOut() {
        User user = buildUser(1L);
        EmployeeUser employee = buildEmployee(10L, null);
        Attendance existing = new Attendance();
        existing.setCheckInTime(LocalTime.of(9, 0));
        existing.setCheckOutTime(LocalTime.of(18, 30));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(employee));
        when(attendanceRepository.findByEmployeeIdAndAttendanceDate(eq(10L), any(LocalDate.class)))
                .thenReturn(Optional.of(existing));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> attendanceService.checkOut(1L, "Office", "127.0.0.1"));
        assertTrue(ex.getMessage().contains("Already checked out today at 18:30"));
    }

    @Test
    void checkOut_shouldThrow_whenCheckInTimeNull() {
        User user = buildUser(1L);
        EmployeeUser employee = buildEmployee(10L, null);
        Attendance existing = new Attendance();
        existing.setCheckInTime(null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(employee));
        when(attendanceRepository.findByEmployeeIdAndAttendanceDate(eq(10L), any(LocalDate.class)))
                .thenReturn(Optional.of(existing));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> attendanceService.checkOut(1L, "Office", "127.0.0.1"));
        assertEquals("Please check in first before checking out", ex.getMessage());
    }

    @Test
    void checkOut_shouldSanitizeLocation_whenNotAvailable() {
        User user = buildUser(1L);
        EmployeeUser employee = buildEmployee(10L, null);
        Attendance existing = new Attendance();
        existing.setCheckInTime(LocalTime.of(9, 0));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(employee));
        when(attendanceRepository.findByEmployeeIdAndAttendanceDate(eq(10L), any(LocalDate.class)))
                .thenReturn(Optional.of(existing));
        when(calculator.getOfficialEndTime(settingsService)).thenReturn(LocalTime.of(0, 1));
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(inv -> inv.getArgument(0));

        Attendance result = attendanceService.checkOut(1L, "location not available", "127.0.0.1");

        assertNull(result.getCheckOutLocation());
    }

    // ---------------------------------------------------------------
    // getTodayAttendance
    // ---------------------------------------------------------------

    @Test
    void getTodayAttendance_shouldReturnNull_whenEmployeeNotFound() {
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertNull(attendanceService.getTodayAttendance(1L));
        verifyNoInteractions(attendanceRepository);
    }

    @Test
    void getTodayAttendance_shouldReturnAttendance_whenFound() {
        EmployeeUser employee = buildEmployee(10L, null);
        Attendance attendance = new Attendance();
        attendance.setId(5L);

        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(employee));
        when(attendanceRepository.findByEmployeeIdAndAttendanceDate(eq(10L), any(LocalDate.class)))
                .thenReturn(Optional.of(attendance));

        Attendance result = attendanceService.getTodayAttendance(1L);
        assertNotNull(result);
        assertEquals(5L, result.getId());
    }

    @Test
    void getTodayAttendance_shouldReturnNull_whenNoAttendanceToday() {
        EmployeeUser employee = buildEmployee(10L, null);

        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(employee));
        when(attendanceRepository.findByEmployeeIdAndAttendanceDate(eq(10L), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        assertNull(attendanceService.getTodayAttendance(1L));
    }

    // ---------------------------------------------------------------
    // getAttendanceHistoryPageable
    // ---------------------------------------------------------------

    @Test
    void getAttendanceHistoryPageable_shouldReturnPage() {
        EmployeeUser employee = buildEmployee(10L, null);
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 31);
        Pageable pageable = mock(Pageable.class);
        Page<Attendance> page = new PageImpl<>(List.of(new Attendance()));

        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(employee));
        when(attendanceRepository.findByEmployeeIdAndAttendanceDateBetweenPageable(10L, start, end, pageable))
                .thenReturn(page);

        Page<Attendance> result = attendanceService.getAttendanceHistoryPageable(1L, start, end, pageable);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getAttendanceHistoryPageable_shouldThrow_whenEmployeeNotFound() {
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.empty());

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> attendanceService.getAttendanceHistoryPageable(1L, LocalDate.now(), LocalDate.now(), mock(Pageable.class)));
        assertEquals("Employee record not found for user ID: 1", ex.getMessage());
    }

    // ---------------------------------------------------------------
    // getAttendanceHistory
    // ---------------------------------------------------------------

    @Test
    void getAttendanceHistory_shouldReturnList() {
        EmployeeUser employee = buildEmployee(10L, null);
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 31);
        List<Attendance> attendances = List.of(new Attendance(), new Attendance());

        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(employee));
        when(attendanceRepository.findByEmployeeIdAndAttendanceDateBetween(10L, start, end)).thenReturn(attendances);

        List<Attendance> result = attendanceService.getAttendanceHistory(1L, start, end);
        assertEquals(2, result.size());
    }

    @Test
    void getAttendanceHistory_shouldThrow_whenEmployeeNotFound() {
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.empty());

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> attendanceService.getAttendanceHistory(1L, LocalDate.now(), LocalDate.now()));
        assertEquals("Employee record not found for user ID: 1", ex.getMessage());
    }

    // ---------------------------------------------------------------
    // getAttendanceSummary
    // ---------------------------------------------------------------

    @Test
    void getAttendanceSummary_shouldReturnEmptyMap_whenEmployeeNotFound() {
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.empty());

        Map<String, Object> result = attendanceService.getAttendanceSummary(1L, 2024, 1);
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verifyNoInteractions(attendanceRepository, calculator);
    }

    @Test
    void getAttendanceSummary_shouldReturnSummary_whenEmployeeFound() {
        EmployeeUser employee = buildEmployee(10L, null);
        List<Attendance> attendances = List.of(new Attendance());
        Map<String, Object> expectedSummary = new HashMap<>();
        expectedSummary.put("presentDays", 1L);

        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(employee));
        when(attendanceRepository.findByEmployeeIdAndAttendanceDateBetween(
                eq(10L), any(LocalDate.class), any(LocalDate.class))).thenReturn(attendances);
        when(calculator.countWorkingDays(eq(employee), any(LocalDate.class), any(LocalDate.class), eq(settingsService)))
                .thenReturn(20);
        when(calculator.buildSummaryMap(eq(employee), eq(2024), eq(1), eq(attendances), eq(20), eq(settingsService)))
                .thenReturn(expectedSummary);

        Map<String, Object> result = attendanceService.getAttendanceSummary(1L, 2024, 1);
        assertEquals(1L, result.get("presentDays"));
        assertEquals(0L, result.get("leaveDays"));
        assertEquals(0L, result.get("holidayDays"));
    }
}
