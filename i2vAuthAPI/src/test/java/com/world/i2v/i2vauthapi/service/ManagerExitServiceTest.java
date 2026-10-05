package com.world.i2v.i2vauthapi.service;

import com.world.i2v.i2vauthapi.dto.response.ExitResponseDto;
import com.world.i2v.i2vauthapi.entity.*;
import com.world.i2v.i2vauthapi.exception.BadRequestException;
import com.world.i2v.i2vauthapi.exception.ResourceNotFoundException;
import com.world.i2v.i2vauthapi.repository.ClearanceStepRepository;
import com.world.i2v.i2vauthapi.repository.EmployeeUserRepository;
import com.world.i2v.i2vauthapi.repository.ExitRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ManagerExitServiceTest {

    private ExitRequestRepository exitRequestRepository;
    private EmployeeUserRepository employeeRepository;
    private ClearanceStepRepository clearanceStepRepository;
    private ExitService exitService;

    private ManagerExitService managerExitService;

    @BeforeEach
    void setUp() {
        exitRequestRepository = mock(ExitRequestRepository.class);
        employeeRepository = mock(EmployeeUserRepository.class);
        clearanceStepRepository = mock(ClearanceStepRepository.class);
        exitService = mock(ExitService.class);

        managerExitService = new ManagerExitService(
                exitRequestRepository, employeeRepository, clearanceStepRepository, exitService);
    }

    // ==================== helpers ====================

    private EmployeeUser buildEmployee(Long id) {
        EmployeeUser e = new EmployeeUser();
        e.setId(id);
        e.setEmployeeCode("EMP" + id);
        return e;
    }

    private Territory buildTerritory(Long id) {
        Territory t = new Territory();
        t.setId(id);
        return t;
    }

    private Squad buildSquad(Long id) {
        Squad s = new Squad();
        s.setId(id);
        return s;
    }

    private Unit buildUnit(Long id) {
        Unit u = new Unit();
        u.setId(id);
        return u;
    }

    private ExitRequest buildExitRequest(Long id, EmployeeUser employee, ExitRequest.ExitStatus status) {
        return ExitRequest.builder()
                .id(id)
                .employee(employee)
                .place("Office")
                .reason("Personal")
                .lastWorkingDate(LocalDate.now().plusDays(30))
                .status(status)
                .clearanceSteps(new ArrayList<>())
                .build();
    }

    private ClearanceStep buildStep(Long id, String assignedTo, ExitRequest.ExitStatus status, ExitRequest exitRequest) {
        ClearanceStep step = ClearanceStep.builder()
                .id(id)
                .step(assignedTo.equals("manager") ? "Manager Approval" : "HR Clearance")
                .assignedTo(assignedTo)
                .status(status)
                .exitRequest(exitRequest)
                .build();
        exitRequest.getClearanceSteps().add(step);
        return step;
    }

    // ==================== smoke ====================

    @Test
    void smokeTest_serviceIsConstructed() {
        assertNotNull(managerExitService);
    }

    // ==================== getSubordinateExitRequests ====================

    @Test
    void getSubordinateExitRequests_managerNotFound_throws() {
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> managerExitService.getSubordinateExitRequests(1L));
    }

    @Test
    void getSubordinateExitRequests_noSubordinates_returnsEmptyList() {
        EmployeeUser manager = buildEmployee(1L);
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(manager));
        when(employeeRepository.findAll()).thenReturn(List.of(manager));

        List<ExitResponseDto> result = managerExitService.getSubordinateExitRequests(1L);

        assertTrue(result.isEmpty());
        verifyNoInteractions(exitRequestRepository);
    }

    @Test
    void getSubordinateExitRequests_directReport_included() {
        EmployeeUser manager = buildEmployee(1L);
        EmployeeUser subordinate = buildEmployee(2L);
        subordinate.setReportingTo(manager);

        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(manager));
        when(employeeRepository.findAll()).thenReturn(List.of(manager, subordinate));

        ExitRequest request = buildExitRequest(10L, subordinate, ExitRequest.ExitStatus.PENDING);
        when(exitRequestRepository.findAllByOrderByAppliedAtDesc()).thenReturn(List.of(request));
        when(exitService.mapToDto(request)).thenReturn(ExitResponseDto.builder().id(10L).build());

        List<ExitResponseDto> result = managerExitService.getSubordinateExitRequests(1L);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getId());
    }

    @Test
    void getSubordinateExitRequests_sameTerritory_included() {
        EmployeeUser manager = buildEmployee(1L);
        manager.setTerritory(buildTerritory(100L));
        EmployeeUser peer = buildEmployee(2L);
        peer.setTerritory(buildTerritory(100L));

        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(manager));
        when(employeeRepository.findAll()).thenReturn(List.of(manager, peer));

        ExitRequest request = buildExitRequest(11L, peer, ExitRequest.ExitStatus.PENDING);
        when(exitRequestRepository.findAllByOrderByAppliedAtDesc()).thenReturn(List.of(request));
        when(exitService.mapToDto(request)).thenReturn(ExitResponseDto.builder().id(11L).build());

        List<ExitResponseDto> result = managerExitService.getSubordinateExitRequests(1L);

        assertEquals(1, result.size());
    }

    @Test
    void getSubordinateExitRequests_sameSquad_included() {
        EmployeeUser manager = buildEmployee(1L);
        manager.setSquad(buildSquad(200L));
        EmployeeUser peer = buildEmployee(2L);
        peer.setSquad(buildSquad(200L));

        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(manager));
        when(employeeRepository.findAll()).thenReturn(List.of(manager, peer));

        ExitRequest request = buildExitRequest(12L, peer, ExitRequest.ExitStatus.PENDING);
        when(exitRequestRepository.findAllByOrderByAppliedAtDesc()).thenReturn(List.of(request));
        when(exitService.mapToDto(request)).thenReturn(ExitResponseDto.builder().id(12L).build());

        List<ExitResponseDto> result = managerExitService.getSubordinateExitRequests(1L);

        assertEquals(1, result.size());
    }

    @Test
    void getSubordinateExitRequests_sameUnit_included() {
        EmployeeUser manager = buildEmployee(1L);
        manager.setUnit(buildUnit(300L));
        EmployeeUser peer = buildEmployee(2L);
        peer.setUnit(buildUnit(300L));

        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(manager));
        when(employeeRepository.findAll()).thenReturn(List.of(manager, peer));

        ExitRequest request = buildExitRequest(13L, peer, ExitRequest.ExitStatus.PENDING);
        when(exitRequestRepository.findAllByOrderByAppliedAtDesc()).thenReturn(List.of(request));
        when(exitService.mapToDto(request)).thenReturn(ExitResponseDto.builder().id(13L).build());

        List<ExitResponseDto> result = managerExitService.getSubordinateExitRequests(1L);

        assertEquals(1, result.size());
    }

    @Test
    void getSubordinateExitRequests_unrelatedEmployeeExcluded() {
        EmployeeUser manager = buildEmployee(1L);
        EmployeeUser unrelated = buildEmployee(3L);

        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(manager));
        when(employeeRepository.findAll()).thenReturn(List.of(manager, unrelated));

        List<ExitResponseDto> result = managerExitService.getSubordinateExitRequests(1L);

        assertTrue(result.isEmpty());
        verifyNoInteractions(exitRequestRepository);
    }

    @Test
    void getSubordinateExitRequests_filtersOutNonSubordinateRequests() {
        EmployeeUser manager = buildEmployee(1L);
        EmployeeUser subordinate = buildEmployee(2L);
        subordinate.setReportingTo(manager);
        EmployeeUser stranger = buildEmployee(4L);

        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(manager));
        when(employeeRepository.findAll()).thenReturn(List.of(manager, subordinate));

        ExitRequest subRequest = buildExitRequest(14L, subordinate, ExitRequest.ExitStatus.PENDING);
        ExitRequest strangerRequest = buildExitRequest(15L, stranger, ExitRequest.ExitStatus.PENDING);
        when(exitRequestRepository.findAllByOrderByAppliedAtDesc())
                .thenReturn(List.of(strangerRequest, subRequest));
        when(exitService.mapToDto(subRequest)).thenReturn(ExitResponseDto.builder().id(14L).build());

        List<ExitResponseDto> result = managerExitService.getSubordinateExitRequests(1L);

        assertEquals(1, result.size());
        assertEquals(14L, result.get(0).getId());
        verify(exitService, never()).mapToDto(strangerRequest);
    }

    // ==================== approveStep ====================

    @Test
    void approveStep_success_stepApprovedButOverallNotUpdated() {
        EmployeeUser manager = buildEmployee(1L);
        EmployeeUser subordinate = buildEmployee(2L);
        subordinate.setReportingTo(manager);

        ExitRequest request = buildExitRequest(20L, subordinate, ExitRequest.ExitStatus.PENDING);
        ClearanceStep managerStep = buildStep(30L, "manager", ExitRequest.ExitStatus.PENDING, request);
        ClearanceStep hrStep = buildStep(31L, "hr", ExitRequest.ExitStatus.PENDING, request);

        when(clearanceStepRepository.findById(30L)).thenReturn(Optional.of(managerStep));
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(manager));

        managerExitService.approveStep(20L, 30L, 1L);

        assertEquals(ExitRequest.ExitStatus.APPROVED, managerStep.getStatus());
        assertNotNull(managerStep.getCompletedAt());
        verify(clearanceStepRepository).save(managerStep);
        // hr step is still pending, so overall status should not flip to approved
        assertEquals(ExitRequest.ExitStatus.PENDING, request.getStatus());
        verify(exitRequestRepository, never()).save(any());
    }

    @Test
    void approveStep_allStepsApproved_updatesOverallStatus() {
        EmployeeUser manager = buildEmployee(1L);
        EmployeeUser subordinate = buildEmployee(2L);
        subordinate.setReportingTo(manager);

        ExitRequest request = buildExitRequest(21L, subordinate, ExitRequest.ExitStatus.PENDING);
        ClearanceStep managerStep = buildStep(32L, "manager", ExitRequest.ExitStatus.PENDING, request);

        when(clearanceStepRepository.findById(32L)).thenReturn(Optional.of(managerStep));
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(manager));

        managerExitService.approveStep(21L, 32L, 1L);

        assertEquals(ExitRequest.ExitStatus.APPROVED, request.getStatus());
        verify(exitRequestRepository).save(request);
    }

    @Test
    void approveStep_stepNotFound_throws() {
        when(clearanceStepRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> managerExitService.approveStep(1L, 99L, 1L));
    }

    @Test
    void approveStep_stepBelongsToDifferentRequest_throws() {
        EmployeeUser subordinate = buildEmployee(2L);
        ExitRequest request = buildExitRequest(22L, subordinate, ExitRequest.ExitStatus.PENDING);
        ClearanceStep step = buildStep(33L, "manager", ExitRequest.ExitStatus.PENDING, request);

        when(clearanceStepRepository.findById(33L)).thenReturn(Optional.of(step));

        assertThrows(BadRequestException.class,
                () -> managerExitService.approveStep(999L, 33L, 1L));
    }

    @Test
    void approveStep_notManagerStep_throws() {
        EmployeeUser subordinate = buildEmployee(2L);
        ExitRequest request = buildExitRequest(23L, subordinate, ExitRequest.ExitStatus.PENDING);
        ClearanceStep step = buildStep(34L, "hr", ExitRequest.ExitStatus.PENDING, request);

        when(clearanceStepRepository.findById(34L)).thenReturn(Optional.of(step));

        assertThrows(BadRequestException.class,
                () -> managerExitService.approveStep(23L, 34L, 1L));
    }

    @Test
    void approveStep_stepNotPending_throws() {
        EmployeeUser subordinate = buildEmployee(2L);
        ExitRequest request = buildExitRequest(24L, subordinate, ExitRequest.ExitStatus.PENDING);
        ClearanceStep step = buildStep(35L, "manager", ExitRequest.ExitStatus.APPROVED, request);

        when(clearanceStepRepository.findById(35L)).thenReturn(Optional.of(step));

        assertThrows(BadRequestException.class,
                () -> managerExitService.approveStep(24L, 35L, 1L));
    }

    @Test
    void approveStep_managerNotFound_throws() {
        EmployeeUser subordinate = buildEmployee(2L);
        ExitRequest request = buildExitRequest(25L, subordinate, ExitRequest.ExitStatus.PENDING);
        ClearanceStep step = buildStep(36L, "manager", ExitRequest.ExitStatus.PENDING, request);

        when(clearanceStepRepository.findById(36L)).thenReturn(Optional.of(step));
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> managerExitService.approveStep(25L, 36L, 1L));
    }

    @Test
    void approveStep_subordinateHasNoReportingManager_throws() {
        EmployeeUser manager = buildEmployee(1L);
        EmployeeUser subordinate = buildEmployee(2L); // reportingTo left null
        ExitRequest request = buildExitRequest(26L, subordinate, ExitRequest.ExitStatus.PENDING);
        ClearanceStep step = buildStep(37L, "manager", ExitRequest.ExitStatus.PENDING, request);

        when(clearanceStepRepository.findById(37L)).thenReturn(Optional.of(step));
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(manager));

        assertThrows(BadRequestException.class,
                () -> managerExitService.approveStep(26L, 37L, 1L));
    }

    @Test
    void approveStep_notTheReportingManager_throws() {
        EmployeeUser manager = buildEmployee(1L);
        EmployeeUser otherManager = buildEmployee(9L);
        EmployeeUser subordinate = buildEmployee(2L);
        subordinate.setReportingTo(otherManager);

        ExitRequest request = buildExitRequest(27L, subordinate, ExitRequest.ExitStatus.PENDING);
        ClearanceStep step = buildStep(38L, "manager", ExitRequest.ExitStatus.PENDING, request);

        when(clearanceStepRepository.findById(38L)).thenReturn(Optional.of(step));
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(manager));

        assertThrows(BadRequestException.class,
                () -> managerExitService.approveStep(27L, 38L, 1L));
    }

    // ==================== rejectStep ====================

    @Test
    void rejectStep_success_rejectsStepAndRequest() {
        EmployeeUser manager = buildEmployee(1L);
        EmployeeUser subordinate = buildEmployee(2L);
        subordinate.setReportingTo(manager);

        ExitRequest request = buildExitRequest(40L, subordinate, ExitRequest.ExitStatus.PENDING);
        ClearanceStep step = buildStep(50L, "manager", ExitRequest.ExitStatus.PENDING, request);

        when(clearanceStepRepository.findById(50L)).thenReturn(Optional.of(step));
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(manager));

        managerExitService.rejectStep(40L, 50L, 1L, "Not eligible");

        assertEquals(ExitRequest.ExitStatus.REJECTED, step.getStatus());
        assertNotNull(step.getCompletedAt());
        assertEquals(ExitRequest.ExitStatus.REJECTED, request.getStatus());
        assertNotNull(request.getUpdatedAt());
        verify(clearanceStepRepository).save(step);
        verify(exitRequestRepository).save(request);
    }

    @Test
    void rejectStep_stepNotFound_throws() {
        when(clearanceStepRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> managerExitService.rejectStep(1L, 999L, 1L, "reason"));
    }

    // ==================== reopenExitRequest ====================

    @Test
    void reopenExitRequest_success_delegatesToExitService() {
        EmployeeUser manager = buildEmployee(1L);
        EmployeeUser subordinate = buildEmployee(2L);
        subordinate.setReportingTo(manager);
        ExitRequest request = buildExitRequest(60L, subordinate, ExitRequest.ExitStatus.REJECTED);

        when(exitRequestRepository.findById(60L)).thenReturn(Optional.of(request));
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(manager));

        managerExitService.reopenExitRequest(60L, 1L);

        verify(exitService).reopenRejectedRequest(request);
        verify(exitRequestRepository).save(request);
    }

    @Test
    void reopenExitRequest_requestNotFound_throws() {
        when(exitRequestRepository.findById(61L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> managerExitService.reopenExitRequest(61L, 1L));
    }

    @Test
    void reopenExitRequest_managerNotFound_throws() {
        EmployeeUser subordinate = buildEmployee(2L);
        ExitRequest request = buildExitRequest(62L, subordinate, ExitRequest.ExitStatus.REJECTED);
        when(exitRequestRepository.findById(62L)).thenReturn(Optional.of(request));
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> managerExitService.reopenExitRequest(62L, 1L));
    }

    @Test
    void reopenExitRequest_notReportingManager_throws() {
        EmployeeUser manager = buildEmployee(1L);
        EmployeeUser subordinate = buildEmployee(2L); // reportingTo null
        ExitRequest request = buildExitRequest(63L, subordinate, ExitRequest.ExitStatus.REJECTED);
        when(exitRequestRepository.findById(63L)).thenReturn(Optional.of(request));
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(manager));

        assertThrows(BadRequestException.class,
                () -> managerExitService.reopenExitRequest(63L, 1L));
        verify(exitService, never()).reopenRejectedRequest(any());
    }

    // ==================== hardDeleteExitRequest ====================

    @Test
    void hardDeleteExitRequest_success_deletesRequest() {
        EmployeeUser manager = buildEmployee(1L);
        EmployeeUser subordinate = buildEmployee(2L);
        subordinate.setReportingTo(manager);
        ExitRequest request = buildExitRequest(70L, subordinate, ExitRequest.ExitStatus.REJECTED);

        when(exitRequestRepository.findById(70L)).thenReturn(Optional.of(request));
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(manager));

        managerExitService.hardDeleteExitRequest(70L, 1L);

        verify(exitService).validateRejectedRequest(request);
        verify(exitRequestRepository).delete(request);
    }

    @Test
    void hardDeleteExitRequest_requestNotFound_throws() {
        when(exitRequestRepository.findById(71L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> managerExitService.hardDeleteExitRequest(71L, 1L));
    }

    @Test
    void hardDeleteExitRequest_notReportingManager_throws() {
        EmployeeUser manager = buildEmployee(1L);
        EmployeeUser subordinate = buildEmployee(2L); // reportingTo null
        ExitRequest request = buildExitRequest(72L, subordinate, ExitRequest.ExitStatus.REJECTED);
        when(exitRequestRepository.findById(72L)).thenReturn(Optional.of(request));
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(manager));

        assertThrows(BadRequestException.class,
                () -> managerExitService.hardDeleteExitRequest(72L, 1L));
        verify(exitRequestRepository, never()).delete(any());
    }
}
