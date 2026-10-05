package com.world.i2v.i2vauthapi.service;

import com.world.i2v.i2vauthapi.dto.request.ExitRequestDto;
import com.world.i2v.i2vauthapi.dto.response.ClearanceStepDto;
import com.world.i2v.i2vauthapi.dto.response.ExitResponseDto;
import com.world.i2v.i2vauthapi.entity.*;
import com.world.i2v.i2vauthapi.exception.*;
import com.world.i2v.i2vauthapi.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExitService {

    private final ExitRequestRepository exitRequestRepository;
    private final EmployeeUserRepository employeeRepository;
    private final ClearanceStepRepository clearanceStepRepository;
    private final EmailService emailService;

    // ==================== EMPLOYEE SIDE ====================

    /**
     * Get the current employee's most recent exit request.
     * Throws ResourceNotFoundException if none exists (frontend interprets as 404).
     */
    @Transactional(readOnly = true)   // Keep session open for lazy loading
    public ExitResponseDto getMyExitRequest(Long userId) {
        EmployeeUser employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));

        ExitRequest exitRequest = exitRequestRepository
                .findTopByEmployeeIdOrderByAppliedAtDesc(employee.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No active exit request"));

        return mapToDto(exitRequest);
    }

    /**
     * Submit a new resignation request for the employee.
     * Creates the ExitRequest with PENDING status and two default clearance steps
     * (Manager Approval & HR Clearance).
     */
    @Transactional
    public ExitResponseDto submitExitRequest(Long userId, ExitRequestDto dto) {
        EmployeeUser employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));

        // Prevent duplicate pending requests
        boolean alreadyPending = exitRequestRepository
                .findTopByEmployeeIdOrderByAppliedAtDesc(employee.getId())
                .filter(er -> er.getStatus() == ExitRequest.ExitStatus.PENDING)
                .isPresent();
        if (alreadyPending) {
            throw new BadRequestException("You already have a pending exit request.");
        }

        ExitRequest exitRequest = ExitRequest.builder()
                .employee(employee)
                .place(dto.getPlace())
                .reason(dto.getReason())
                .lastWorkingDate(dto.getLastWorkingDate())
                .comments(dto.getComments())
                .status(ExitRequest.ExitStatus.PENDING)   // explicit, but Builder uses default
                .build();

        // Create two default clearance steps
        List<ClearanceStep> steps = List.of(
                ClearanceStep.builder()
                        .step("Manager Approval")
                        .assignedTo("manager")
                        .status(ExitRequest.ExitStatus.PENDING)
                        .exitRequest(exitRequest)
                        .build(),
                ClearanceStep.builder()
                        .step("HR Clearance")
                        .assignedTo("hr")
                        .status(ExitRequest.ExitStatus.PENDING)
                        .exitRequest(exitRequest)
                        .build()
        );
        exitRequest.setClearanceSteps(steps);

        ExitRequest saved = exitRequestRepository.save(exitRequest);
        log.info("Exit request submitted for employee {} (ID:{})", employee.getEmployeeCode(), employee.getId());
        
        // Send resignation notification email to employee and manager(s)
        User user = employee.getUser();
        if (user != null && user.getEmail() != null) {
            String[] managerEmails = getManagerEmails(employee);
            emailService.sendResignationNotificationEmail(
                    user,
                    dto.getPlace(),
                    dto.getReason(),
                    dto.getLastWorkingDate().toString(),
                    dto.getComments(),
                    managerEmails
            );
        }
        
        return mapToDto(saved);
    }

    @Transactional
    public ExitResponseDto reopenMyExitRequest(Long userId, Long exitRequestId) {
        ExitRequest exitRequest = getOwnedRejectedRequest(userId, exitRequestId);
        reopenRejectedRequest(exitRequest);
        ExitRequest saved = exitRequestRepository.save(exitRequest);
        log.info("Exit request {} reopened by employee user {}", exitRequestId, userId);
        return mapToDto(saved);
    }

    @Transactional
    public void hardDeleteMyExitRequest(Long userId, Long exitRequestId) {
        ExitRequest exitRequest = getOwnedRejectedRequest(userId, exitRequestId);
        exitRequestRepository.delete(exitRequest);
        log.info("Exit request {} hard deleted by employee user {}", exitRequestId, userId);
    }

    @Transactional(readOnly = true)
    public void sendExitNotificationEmail(Long userId, Long exitRequestId) {
        EmployeeUser employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));
        
        ExitRequest exitRequest = exitRequestRepository.findById(exitRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Exit request not found"));
        
        if (!exitRequest.getEmployee().getId().equals(employee.getId())) {
            throw new BadRequestException("You can only access your own exit request.");
        }
        
        User user = employee.getUser();
        if (user != null && user.getEmail() != null) {
            String[] managerEmails = getManagerEmails(employee);
            emailService.sendResignationNotificationEmail(
                    user,
                    exitRequest.getPlace(),
                    exitRequest.getReason(),
                    exitRequest.getLastWorkingDate().toString(),
                    exitRequest.getComments(),
                    managerEmails
            );
            log.info("Exit notification email sent for exit request {} to {} and managers", exitRequestId, user.getEmail());
        } else {
            log.warn("No email address found for employee {}", employee.getEmployeeCode());
        }
    }
    
    /**
     * Extract manager email(s) from the employee's reporting chain
     */
    private String[] getManagerEmails(EmployeeUser employee) {
        java.util.List<String> managerEmails = new java.util.ArrayList<>();
        
        // Get direct manager
        if (employee.getReportingTo() != null) {
            EmployeeUser manager = employee.getReportingTo();
            User managerUser = manager.getUser();
            if (managerUser != null && managerUser.getEmail() != null && !managerUser.getEmail().isEmpty()) {
                managerEmails.add(managerUser.getEmail());
                log.info("Added manager email: {} for employee {}", managerUser.getEmail(), employee.getEmployeeCode());
            }
        }
        
        return managerEmails.toArray(new String[0]);
    }

    public void reopenRejectedRequest(ExitRequest exitRequest) {
        if (exitRequest.getStatus() != ExitRequest.ExitStatus.REJECTED) {
            throw new BadRequestException("Only rejected exit requests can be reopened.");
        }
        exitRequest.setStatus(ExitRequest.ExitStatus.PENDING);
        exitRequest.getClearanceSteps().forEach(step -> {
            step.setStatus(ExitRequest.ExitStatus.PENDING);
            step.setCompletedAt(null);
        });
    }

    public void validateRejectedRequest(ExitRequest exitRequest) {
        if (exitRequest.getStatus() != ExitRequest.ExitStatus.REJECTED) {
            throw new BadRequestException("Only rejected exit requests can be deleted.");
        }
    }

    private ExitRequest getOwnedRejectedRequest(Long userId, Long exitRequestId) {
        EmployeeUser employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));
        ExitRequest exitRequest = exitRequestRepository.findById(exitRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Exit request not found"));
        if (!exitRequest.getEmployee().getId().equals(employee.getId())) {
            throw new BadRequestException("You can only manage your own exit request.");
        }
        validateRejectedRequest(exitRequest);
        return exitRequest;
    }

    // ==================== SHARED MAPPER METHODS (also used by admin) ====================

    /**
     * Map an ExitRequest entity to the full response DTO.
     * PUBLIC so ExitManagementService can reuse it.
     */
    public ExitResponseDto mapToDto(ExitRequest exitRequest) {
        EmployeeUser emp = exitRequest.getEmployee();
        String empName = (emp.getUser() != null) ? emp.getUser().getFullName() : "Unknown";
        return ExitResponseDto.builder()
                .id(exitRequest.getId())
                .employeeId(emp.getId())
                .employeeCode(emp.getEmployeeCode())
                .employeeName(empName)
                .designation(emp.getDesignation() != null ? emp.getDesignation().getTitle() : null)
                .unitName(emp.getUnit() != null ? emp.getUnit().getName() : null)
                .squadName(emp.getSquad() != null ? emp.getSquad().getName() : null)
                .territoryName(emp.getTerritory() != null ? emp.getTerritory().getName() : null)
                .fotAccessLevelName(emp.getFotAccessLevel() != null ? emp.getFotAccessLevel().getName() : null)
                .place(exitRequest.getPlace())
                .reason(exitRequest.getReason())
                .lastWorkingDate(exitRequest.getLastWorkingDate())
                .comments(exitRequest.getComments())
                .status(exitRequest.getStatus().name())
                .appliedAt(exitRequest.getAppliedAt())
                .clearanceSteps(
                        exitRequest.getClearanceSteps().stream()
                                .map(this::mapClearanceStep)
                                .collect(Collectors.toList())
                )
                .build();
    }

    /**
     * Map a single ClearanceStep entity to its DTO.
     * PUBLIC so ExitManagementService can reuse it.
     */
    public ClearanceStepDto mapClearanceStep(ClearanceStep cs) {
        return ClearanceStepDto.builder()
                .id(cs.getId())
                .step(cs.getStep())
                .assignedTo(cs.getAssignedTo())
                .status(cs.getStatus().name())
                .completedAt(cs.getCompletedAt())
                .build();
    }
}
