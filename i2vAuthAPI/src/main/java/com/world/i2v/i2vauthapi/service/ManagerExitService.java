package com.world.i2v.i2vauthapi.service;

import com.world.i2v.i2vauthapi.dto.response.ExitResponseDto;
import com.world.i2v.i2vauthapi.entity.*;
import com.world.i2v.i2vauthapi.exception.BadRequestException;
import com.world.i2v.i2vauthapi.exception.ResourceNotFoundException;
import com.world.i2v.i2vauthapi.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManagerExitService {

    private final ExitRequestRepository exitRequestRepository;
    private final EmployeeUserRepository employeeRepository;
    private final ClearanceStepRepository clearanceStepRepository;
    private final ExitService exitService;          // reuse mapping

    /**
     * Get all exit requests for subordinates of the given manager.
     */
    @Transactional(readOnly = true)
    public List<ExitResponseDto> getSubordinateExitRequests(Long managerUserId) {
        EmployeeUser manager = employeeRepository.findByUserId(managerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager profile not found"));

        // Based on your Team based Hierarchy Assignment
        List<EmployeeUser> allEmployees = employeeRepository.findAll();
        
        List<Long> subordinateIds = allEmployees.stream()
                .filter(e -> {
                    // Include direct reports
                    if (e.getReportingTo() != null && e.getReportingTo().getId().equals(manager.getId())) {
                        return true;
                    }
                    // Include those in the same territory if manager belongs to one
                    if (manager.getTerritory() != null && e.getTerritory() != null && 
                            manager.getTerritory().getId().equals(e.getTerritory().getId())) {
                        return true;
                    }
                    // Include those in the same squad if manager belongs to one
                    if (manager.getSquad() != null && e.getSquad() != null && 
                            manager.getSquad().getId().equals(e.getSquad().getId())) {
                        return true;
                    }
                    // Include those in the same unit if manager belongs to one
                    if (manager.getUnit() != null && e.getUnit() != null && 
                            manager.getUnit().getId().equals(e.getUnit().getId())) {
                        return true;
                    }
                    return false;
                })
                .map(EmployeeUser::getId)
                .toList();

        if (subordinateIds.isEmpty()) return Collections.emptyList();

        List<ExitRequest> requests = exitRequestRepository.findAllByOrderByAppliedAtDesc()
                .stream()
                .filter(er -> subordinateIds.contains(er.getEmployee().getId()))
                .toList();

        return requests.stream().map(exitService::mapToDto).collect(Collectors.toList());
    }

    /**
     * Approve a "manager" clearance step, but only if the manager is the reporting manager
     * of the employee who submitted the exit request.
     */
    @Transactional
    public void approveStep(Long exitRequestId, Long stepId, Long managerUserId) {
        ClearanceStep step = validateManagerStep(exitRequestId, stepId, managerUserId);
        step.setStatus(ExitRequest.ExitStatus.APPROVED);
        step.setCompletedAt(LocalDateTime.now());
        clearanceStepRepository.save(step);

        updateOverallStatus(step.getExitRequest());
        log.info("Manager {} approved step {} for exit request {}", managerUserId, stepId, exitRequestId);
    }

    /**
     * Reject a "manager" clearance step – same validation.
     */
    @Transactional
    public void rejectStep(Long exitRequestId, Long stepId, Long managerUserId, String remarks) {
        ClearanceStep step = validateManagerStep(exitRequestId, stepId, managerUserId);
        step.setStatus(ExitRequest.ExitStatus.REJECTED);
        step.setCompletedAt(LocalDateTime.now());
        clearanceStepRepository.save(step);

        // Reject the whole exit request as soon as any step is rejected
        ExitRequest request = step.getExitRequest();
        request.setStatus(ExitRequest.ExitStatus.REJECTED);
        request.setUpdatedAt(LocalDateTime.now());
        exitRequestRepository.save(request);

        log.info("Manager {} rejected step {} for exit request {}. Reason: {}",
                managerUserId, stepId, exitRequestId, remarks);
    }

    @Transactional
    public void reopenExitRequest(Long exitRequestId, Long managerUserId) {
        ExitRequest request = validateSubordinateRequest(exitRequestId, managerUserId);
        exitService.reopenRejectedRequest(request);
        exitRequestRepository.save(request);
        log.info("Manager {} reopened exit request {}", managerUserId, exitRequestId);
    }

    @Transactional
    public void hardDeleteExitRequest(Long exitRequestId, Long managerUserId) {
        ExitRequest request = validateSubordinateRequest(exitRequestId, managerUserId);
        exitService.validateRejectedRequest(request);
        exitRequestRepository.delete(request);
        log.info("Manager {} hard deleted exit request {}", managerUserId, exitRequestId);
    }

    // ==================== HELPER METHODS ====================

    private ExitRequest validateSubordinateRequest(Long exitRequestId, Long managerUserId) {
        ExitRequest request = exitRequestRepository.findById(exitRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Exit request not found"));
        EmployeeUser subordinate = request.getEmployee();
        EmployeeUser manager = employeeRepository.findByUserId(managerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found"));

        if (subordinate.getReportingTo() == null ||
                !subordinate.getReportingTo().getId().equals(manager.getId())) {
            throw new BadRequestException("You are not the reporting manager of this employee");
        }

        return request;
    }

    private ClearanceStep validateManagerStep(Long exitRequestId, Long stepId, Long managerUserId) {
        ClearanceStep step = clearanceStepRepository.findById(stepId)
                .orElseThrow(() -> new ResourceNotFoundException("Step not found"));
        if (!step.getExitRequest().getId().equals(exitRequestId)) {
            throw new BadRequestException("Step does not belong to the given exit request");
        }

        // Only handle steps assigned to "manager"
        if (!"manager".equalsIgnoreCase(step.getAssignedTo())) {
            throw new BadRequestException("This step is not a manager approval step");
        }
        if (step.getStatus() != ExitRequest.ExitStatus.PENDING) {
            throw new BadRequestException("Step is not pending");
        }

        // Check that the current manager is the reporting manager of the employee
        EmployeeUser subordinate = step.getExitRequest().getEmployee();
        EmployeeUser manager = employeeRepository.findByUserId(managerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found"));

        if (subordinate.getReportingTo() == null ||
                !subordinate.getReportingTo().getId().equals(manager.getId())) {
            throw new BadRequestException("You are not the reporting manager of this employee");
        }

        return step;
    }

    private void updateOverallStatus(ExitRequest exitRequest) {
        boolean allApproved = exitRequest.getClearanceSteps().stream()
                .allMatch(s -> s.getStatus() == ExitRequest.ExitStatus.APPROVED);
        if (allApproved) {
            exitRequest.setStatus(ExitRequest.ExitStatus.APPROVED);
            exitRequest.setUpdatedAt(LocalDateTime.now());
            exitRequestRepository.save(exitRequest);
        }
    }
}
