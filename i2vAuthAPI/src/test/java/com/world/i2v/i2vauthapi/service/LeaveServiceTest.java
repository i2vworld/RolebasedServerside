package com.world.i2v.i2vauthapi.service;

import com.world.i2v.i2vauthapi.dto.request.LeaveRequestDto;
import com.world.i2v.i2vauthapi.entity.EmployeeUser;
import com.world.i2v.i2vauthapi.entity.LeaveRequest;
import com.world.i2v.i2vauthapi.entity.LeaveRequest.LeaveStatus;
import com.world.i2v.i2vauthapi.entity.LeaveRequest.LeaveType;
import com.world.i2v.i2vauthapi.entity.User;
import com.world.i2v.i2vauthapi.exception.BadRequestException;
import com.world.i2v.i2vauthapi.exception.ResourceNotFoundException;
import com.world.i2v.i2vauthapi.repository.EmployeeUserRepository;
import com.world.i2v.i2vauthapi.repository.LeaveRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LeaveServiceTest {

    private LeaveRequestRepository leaveRepository;
    private EmployeeUserRepository employeeRepository;
    private AuditService auditService;

    private LeaveService leaveService;

    @BeforeEach
    void setUp() {
        leaveRepository = mock(LeaveRequestRepository.class);
        employeeRepository = mock(EmployeeUserRepository.class);
        auditService = mock(AuditService.class);

        leaveService = new LeaveService(leaveRepository, employeeRepository, auditService);
    }

    private EmployeeUser buildEmployee(Long id) {
        EmployeeUser e = new EmployeeUser();
        e.setId(id);
        User user = new User();
        user.setId(id);
        e.setUser(user);
        return e;
    }

    private LeaveRequestDto buildRequest(Long employeeId, String type, LocalDate start, LocalDate end) {
        LeaveRequestDto dto = new LeaveRequestDto();
        dto.setEmployeeId(employeeId);
        dto.setLeaveType(type);
        dto.setStartDate(start);
        dto.setEndDate(end);
        dto.setReason("Personal reasons");
        dto.setContactDuringLeave("9999999999");
        dto.setAlternateContact("8888888888");
        return dto;
    }

    // ==================== smoke ====================

    @Test
    void smokeTest_serviceIsConstructed() {
        assertNotNull(leaveService);
    }

    // ==================== applyLeave ====================

    @Test
    void applyLeave_employeeNotFound_throws() {
        LeaveRequestDto dto = buildRequest(1L, "ANNUAL", LocalDate.now(), LocalDate.now().plusDays(1));
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> leaveService.applyLeave(dto));
    }

    @Test
    void applyLeave_endBeforeStart_throws() {
        EmployeeUser employee = buildEmployee(1L);
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(employee));

        LeaveRequestDto dto = buildRequest(1L, "ANNUAL", LocalDate.now(), LocalDate.now().minusDays(1));

        assertThrows(BadRequestException.class, () -> leaveService.applyLeave(dto));
    }

    @Test
    void applyLeave_invalidLeaveType_throws() {
        EmployeeUser employee = buildEmployee(1L);
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(employee));

        LeaveRequestDto dto = buildRequest(1L, "INVALID_TYPE", LocalDate.now(), LocalDate.now().plusDays(1));

        assertThrows(BadRequestException.class, () -> leaveService.applyLeave(dto));
    }

    @Test
    void applyLeave_nullLeaveType_throws() {
        EmployeeUser employee = buildEmployee(1L);
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(employee));

        LeaveRequestDto dto = buildRequest(1L, null, LocalDate.now(), LocalDate.now().plusDays(1));

        assertThrows(BadRequestException.class, () -> leaveService.applyLeave(dto));
    }

    @Test
    void applyLeave_overlappingLeave_throws() {
        EmployeeUser employee = buildEmployee(1L);
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(employee));

        LeaveRequestDto dto = buildRequest(1L, "ANNUAL", LocalDate.now(), LocalDate.now().plusDays(1));
        LeaveRequest existing = new LeaveRequest();
        when(leaveRepository.findActiveLeaveOnDate(1L, dto.getStartDate()))
                .thenReturn(List.of(existing));

        assertThrows(BadRequestException.class, () -> leaveService.applyLeave(dto));
    }

    @Test
    void applyLeave_insufficientBalance_throws() {
        EmployeeUser employee = buildEmployee(1L);
        employee.setAnnualLeavesEntitled(5);
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(employee));

        LeaveRequestDto dto = buildRequest(1L, "ANNUAL", LocalDate.now(), LocalDate.now().plusDays(5));
        when(leaveRepository.findActiveLeaveOnDate(eq(1L), any())).thenReturn(Collections.emptyList());
        when(leaveRepository.countApprovedLeavesByType(eq(1L), eq(LeaveType.ANNUAL), anyInt())).thenReturn(3L);

        assertThrows(BadRequestException.class, () -> leaveService.applyLeave(dto));
    }

    @Test
    void applyLeave_success_savesLeaveAndAudits() {
        EmployeeUser employee = buildEmployee(1L);
        employee.setAnnualLeavesEntitled(24);
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(employee));

        LeaveRequestDto dto = buildRequest(1L, "ANNUAL", LocalDate.now(), LocalDate.now().plusDays(2));
        when(leaveRepository.findActiveLeaveOnDate(eq(1L), any())).thenReturn(Collections.emptyList());
        when(leaveRepository.countApprovedLeavesByType(eq(1L), eq(LeaveType.ANNUAL), anyInt())).thenReturn(0L);

        LeaveRequest savedLeave = new LeaveRequest();
        savedLeave.setId(100L);
        when(leaveRepository.save(any(LeaveRequest.class))).thenReturn(savedLeave);

        LeaveRequest result = leaveService.applyLeave(dto);

        assertEquals(100L, result.getId());
        verify(leaveRepository).save(argThat(l ->
                l.getLeaveType() == LeaveType.ANNUAL
                        && l.getStatus() == LeaveStatus.PENDING
                        && l.getEmployee() == employee));
        verify(auditService).logAction(eq(employee.getUser()), eq("APPLY_LEAVE"), eq("LEAVE_REQUEST"), eq(100L), anyString());
    }

    @Test
    void applyLeave_leaveTypeCaseInsensitive_success() {
        EmployeeUser employee = buildEmployee(1L);
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(employee));

        LeaveRequestDto dto = buildRequest(1L, "sick", LocalDate.now(), LocalDate.now());
        when(leaveRepository.findActiveLeaveOnDate(eq(1L), any())).thenReturn(Collections.emptyList());
        when(leaveRepository.countApprovedLeavesByType(eq(1L), eq(LeaveType.SICK), anyInt())).thenReturn(0L);
        when(leaveRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        LeaveRequest result = leaveService.applyLeave(dto);

        assertEquals(LeaveType.SICK, result.getLeaveType());
    }

    // ==================== getLeaveBalance ====================

    @Test
    void getLeaveBalance_employeeNotFound_throws() {
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> leaveService.getLeaveBalance(1L));
    }

    @Test
    void getLeaveBalance_success_returnsDefaultEntitlements() {
        EmployeeUser employee = buildEmployee(1L);
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(employee));
        when(leaveRepository.countApprovedLeavesByType(eq(1L), eq(LeaveType.ANNUAL), anyInt())).thenReturn(2L);
        when(leaveRepository.countApprovedLeavesByType(eq(1L), eq(LeaveType.SICK), anyInt())).thenReturn(1L);
        when(leaveRepository.countApprovedLeavesByType(eq(1L), eq(LeaveType.CASUAL), anyInt())).thenReturn(0L);

        Map<String, Object> balance = leaveService.getLeaveBalance(1L);

        assertNotNull(balance.get("annual"));
        assertNotNull(balance.get("sick"));
        assertNotNull(balance.get("casual"));
        Map<?, ?> annual = (Map<?, ?>) balance.get("annual");
        assertEquals(2L, annual.get("used"));
        assertEquals(24, annual.get("total"));
        assertEquals(22L, annual.get("remaining"));
    }

    @Test
    void getLeaveBalance_success_usesCustomEntitlements() {
        EmployeeUser employee = buildEmployee(1L);
        employee.setAnnualLeavesEntitled(30);
        employee.setSickLeavesEntitled(15);
        employee.setCasualLeavesEntitled(10);
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(employee));
        when(leaveRepository.countApprovedLeavesByType(anyLong(), any(), anyInt())).thenReturn(0L);

        Map<String, Object> balance = leaveService.getLeaveBalance(1L);

        Map<?, ?> annual = (Map<?, ?>) balance.get("annual");
        assertEquals(30, annual.get("total"));
    }

    // ==================== cancelLeave ====================

    @Test
    void cancelLeave_leaveNotFound_throws() {
        when(leaveRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> leaveService.cancelLeave(1L, 1L, "reason"));
    }

    @Test
    void cancelLeave_employeeNotFound_throws() {
        LeaveRequest leave = new LeaveRequest();
        when(leaveRepository.findById(1L)).thenReturn(Optional.of(leave));
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> leaveService.cancelLeave(1L, 1L, "reason"));
    }

    @Test
    void cancelLeave_notOwnLeave_throws() {
        EmployeeUser owner = buildEmployee(1L);
        EmployeeUser requester = buildEmployee(2L);

        LeaveRequest leave = new LeaveRequest();
        leave.setEmployee(owner);
        when(leaveRepository.findById(1L)).thenReturn(Optional.of(leave));
        when(employeeRepository.findByUserId(2L)).thenReturn(Optional.of(requester));

        assertThrows(BadRequestException.class, () -> leaveService.cancelLeave(1L, 2L, "reason"));
    }

    @Test
    void cancelLeave_notPending_throws() {
        EmployeeUser employee = buildEmployee(1L);
        LeaveRequest leave = new LeaveRequest();
        leave.setEmployee(employee);
        leave.setStatus(LeaveStatus.APPROVED);
        leave.setStartDate(LocalDate.now().plusDays(5));
        when(leaveRepository.findById(1L)).thenReturn(Optional.of(leave));
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(employee));

        assertThrows(BadRequestException.class, () -> leaveService.cancelLeave(1L, 1L, "reason"));
    }

    @Test
    void cancelLeave_alreadyStarted_throws() {
        EmployeeUser employee = buildEmployee(1L);
        LeaveRequest leave = new LeaveRequest();
        leave.setEmployee(employee);
        leave.setStatus(LeaveStatus.PENDING);
        leave.setStartDate(LocalDate.now());
        when(leaveRepository.findById(1L)).thenReturn(Optional.of(leave));
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(employee));

        assertThrows(BadRequestException.class, () -> leaveService.cancelLeave(1L, 1L, "reason"));
    }

    @Test
    void cancelLeave_success_cancelsAndAudits() {
        EmployeeUser employee = buildEmployee(1L);
        LeaveRequest leave = new LeaveRequest();
        leave.setId(1L);
        leave.setEmployee(employee);
        leave.setStatus(LeaveStatus.PENDING);
        leave.setStartDate(LocalDate.now().plusDays(5));
        when(leaveRepository.findById(1L)).thenReturn(Optional.of(leave));
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(employee));
        when(leaveRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        LeaveRequest result = leaveService.cancelLeave(1L, 1L, "Change of plans");

        assertEquals(LeaveStatus.CANCELLED, result.getStatus());
        assertNotNull(result.getCancelledAt());
        assertEquals("Change of plans", result.getCancellationReason());
        verify(auditService).logAction(eq(employee.getUser()), eq("CANCEL_LEAVE"), eq("LEAVE_REQUEST"), eq(1L), anyString());
    }
}
