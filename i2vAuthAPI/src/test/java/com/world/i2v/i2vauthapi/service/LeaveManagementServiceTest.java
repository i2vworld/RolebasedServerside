package com.world.i2v.i2vauthapi.service;

import com.world.i2v.i2vauthapi.dto.request.LeaveRequestDto;
import com.world.i2v.i2vauthapi.entity.EmployeeUser;
import com.world.i2v.i2vauthapi.entity.LeaveRequest;
import com.world.i2v.i2vauthapi.entity.LeaveRequest.LeaveStatus;
import com.world.i2v.i2vauthapi.entity.LeaveRequest.LeaveType;
import com.world.i2v.i2vauthapi.exception.BadRequestException;
import com.world.i2v.i2vauthapi.exception.ResourceNotFoundException;
import com.world.i2v.i2vauthapi.repository.EmployeeUserRepository;
import com.world.i2v.i2vauthapi.repository.LeaveRequestRepository;
import com.world.i2v.i2vauthapi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LeaveManagementServiceTest {

    private LeaveRequestRepository leaveRepository;
    private EmployeeUserRepository employeeRepository;
    private UserRepository userRepository;

    private LeaveManagementService leaveManagementService;

    @BeforeEach
    void setUp() {
        leaveRepository = mock(LeaveRequestRepository.class);
        employeeRepository = mock(EmployeeUserRepository.class);
        userRepository = mock(UserRepository.class);

        leaveManagementService = new LeaveManagementService(leaveRepository, employeeRepository, userRepository);
    }

    private EmployeeUser buildEmployee(Long id) {
        EmployeeUser e = new EmployeeUser();
        e.setId(id);
        e.setEmployeeCode("EMP-" + id);
        return e;
    }

    private LeaveRequestDto buildRequest(Long employeeId, String type, LocalDate start, LocalDate end) {
        LeaveRequestDto dto = new LeaveRequestDto();
        dto.setEmployeeId(employeeId);
        dto.setLeaveType(type);
        dto.setStartDate(start);
        dto.setEndDate(end);
        dto.setReason("Personal reasons");
        return dto;
    }

    // ==================== smoke ====================

    @Test
    void smokeTest_serviceIsConstructed() {
        assertNotNull(leaveManagementService);
    }

    // ==================== applyLeave ====================

    @Test
    void applyLeave_employeeNotFound_throws() {
        LeaveRequestDto dto = buildRequest(1L, "ANNUAL", LocalDate.now(), LocalDate.now().plusDays(1));
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> leaveManagementService.applyLeave(dto));
    }

    @Test
    void applyLeave_endBeforeStart_throws() {
        EmployeeUser employee = buildEmployee(1L);
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(employee));

        LeaveRequestDto dto = buildRequest(1L, "ANNUAL", LocalDate.now(), LocalDate.now().minusDays(1));

        assertThrows(BadRequestException.class, () -> leaveManagementService.applyLeave(dto));
    }

    @Test
    void applyLeave_invalidLeaveType_throwsIllegalArgument() {
        EmployeeUser employee = buildEmployee(1L);
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(employee));

        LeaveRequestDto dto = buildRequest(1L, "INVALID_TYPE", LocalDate.now(), LocalDate.now().plusDays(1));
        when(leaveRepository.findActiveLeaveOnDate(eq(1L), any())).thenReturn(Collections.emptyList());

        assertThrows(IllegalArgumentException.class, () -> leaveManagementService.applyLeave(dto));
    }

    @Test
    void applyLeave_overlappingLeave_throws() {
        EmployeeUser employee = buildEmployee(1L);
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(employee));

        LeaveRequestDto dto = buildRequest(1L, "ANNUAL", LocalDate.now(), LocalDate.now().plusDays(1));
        when(leaveRepository.findActiveLeaveOnDate(eq(1L), any()))
                .thenReturn(List.of(new LeaveRequest()));

        assertThrows(BadRequestException.class, () -> leaveManagementService.applyLeave(dto));
    }

    @Test
    void applyLeave_success_savesLeave() {
        EmployeeUser employee = buildEmployee(1L);
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(employee));

        LeaveRequestDto dto = buildRequest(1L, "SICK", LocalDate.now(), LocalDate.now().plusDays(2));
        when(leaveRepository.findActiveLeaveOnDate(eq(1L), any())).thenReturn(Collections.emptyList());

        LeaveRequest savedLeave = new LeaveRequest();
        savedLeave.setId(200L);
        when(leaveRepository.save(any(LeaveRequest.class))).thenReturn(savedLeave);

        LeaveRequest result = leaveManagementService.applyLeave(dto);

        assertEquals(200L, result.getId());
        verify(leaveRepository).save(argThat(l ->
                l.getLeaveType() == LeaveType.SICK
                        && l.getStatus() == LeaveStatus.PENDING
                        && l.getEmployee() == employee
                        && l.getTotalDays() != null));
    }

    // ==================== getEmployeeLeaves ====================

    @Test
    void getEmployeeLeaves_employeeNotFound_throws() {
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> leaveManagementService.getEmployeeLeaves(1L));
    }

    @Test
    void getEmployeeLeaves_success_returnsList() {
        EmployeeUser employee = buildEmployee(1L);
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(employee));
        List<LeaveRequest> leaves = List.of(new LeaveRequest());
        when(leaveRepository.findByEmployeeId(1L)).thenReturn(leaves);

        List<LeaveRequest> result = leaveManagementService.getEmployeeLeaves(1L);

        assertEquals(leaves, result);
    }

    // ==================== getMyPendingLeaves ====================

    @Test
    void getMyPendingLeaves_employeeNotFound_throws() {
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> leaveManagementService.getMyPendingLeaves(1L));
    }

    @Test
    void getMyPendingLeaves_success_returnsPendingOnly() {
        EmployeeUser employee = buildEmployee(1L);
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(employee));
        List<LeaveRequest> pending = List.of(new LeaveRequest());
        when(leaveRepository.findByEmployeeIdAndStatus(1L, LeaveStatus.PENDING)).thenReturn(pending);

        List<LeaveRequest> result = leaveManagementService.getMyPendingLeaves(1L);

        assertEquals(pending, result);
    }

    // ==================== getLeaveBalance ====================

    @Test
    void getLeaveBalance_employeeNotFound_throws() {
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> leaveManagementService.getLeaveBalance(1L));
    }

    @Test
    void getLeaveBalance_success_returnsDefaultEntitlements() {
        EmployeeUser employee = buildEmployee(1L);
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(employee));
        when(leaveRepository.countApprovedLeavesByType(eq(1L), eq(LeaveType.ANNUAL), anyInt())).thenReturn(2L);
        when(leaveRepository.countApprovedLeavesByType(eq(1L), eq(LeaveType.SICK), anyInt())).thenReturn(1L);
        when(leaveRepository.countApprovedLeavesByType(eq(1L), eq(LeaveType.CASUAL), anyInt())).thenReturn(0L);

        Map<String, Object> balance = leaveManagementService.getLeaveBalance(1L);

        Map<?, ?> annual = (Map<?, ?>) balance.get("annual");
        assertEquals(24, annual.get("total"));
        assertEquals(22L, annual.get("remaining"));
    }

    @Test
    void getLeaveBalance_success_usesCustomEntitlements() {
        EmployeeUser employee = buildEmployee(1L);
        employee.setSickLeavesEntitled(20);
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(employee));
        when(leaveRepository.countApprovedLeavesByType(anyLong(), any(), anyInt())).thenReturn(0L);

        Map<String, Object> balance = leaveManagementService.getLeaveBalance(1L);

        Map<?, ?> sick = (Map<?, ?>) balance.get("sick");
        assertEquals(20, sick.get("total"));
    }

    // ==================== cancelLeave ====================

    @Test
    void cancelLeave_leaveNotFound_throws() {
        when(leaveRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> leaveManagementService.cancelLeave(1L, 1L, "reason"));
    }

    @Test
    void cancelLeave_employeeNotFound_throws() {
        when(leaveRepository.findById(1L)).thenReturn(Optional.of(new LeaveRequest()));
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> leaveManagementService.cancelLeave(1L, 1L, "reason"));
    }

    @Test
    void cancelLeave_notOwnLeave_throws() {
        EmployeeUser owner = buildEmployee(1L);
        EmployeeUser requester = buildEmployee(2L);

        LeaveRequest leave = new LeaveRequest();
        leave.setEmployee(owner);
        when(leaveRepository.findById(1L)).thenReturn(Optional.of(leave));
        when(employeeRepository.findByUserId(2L)).thenReturn(Optional.of(requester));

        assertThrows(BadRequestException.class, () -> leaveManagementService.cancelLeave(1L, 2L, "reason"));
    }

    @Test
    void cancelLeave_notPending_throws() {
        EmployeeUser employee = buildEmployee(1L);
        LeaveRequest leave = new LeaveRequest();
        leave.setEmployee(employee);
        leave.setStatus(LeaveStatus.APPROVED);
        when(leaveRepository.findById(1L)).thenReturn(Optional.of(leave));
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(employee));

        assertThrows(BadRequestException.class, () -> leaveManagementService.cancelLeave(1L, 1L, "reason"));
    }

    @Test
    void cancelLeave_success_cancels() {
        EmployeeUser employee = buildEmployee(1L);
        LeaveRequest leave = new LeaveRequest();
        leave.setId(1L);
        leave.setEmployee(employee);
        leave.setStatus(LeaveStatus.PENDING);
        when(leaveRepository.findById(1L)).thenReturn(Optional.of(leave));
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(employee));
        when(leaveRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        LeaveRequest result = leaveManagementService.cancelLeave(1L, 1L, "Change of plans");

        assertEquals(LeaveStatus.CANCELLED, result.getStatus());
        assertNotNull(result.getCancelledAt());
        assertEquals("Change of plans", result.getCancellationReason());
    }
}
