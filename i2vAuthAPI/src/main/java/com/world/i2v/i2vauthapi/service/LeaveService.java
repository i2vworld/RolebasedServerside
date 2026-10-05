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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
@Transactional
public class LeaveService {

    private final LeaveRequestRepository leaveRepository;
    private final EmployeeUserRepository employeeRepository;
    private final AuditService auditService;

    public LeaveService(LeaveRequestRepository leaveRepository,
                        EmployeeUserRepository employeeRepository,
                        AuditService auditService) {
        this.leaveRepository = leaveRepository;
        this.employeeRepository = employeeRepository;
        this.auditService = auditService;
    }

    @Transactional
    public LeaveRequest applyLeave(LeaveRequestDto request) {
        log.info("Applying leave for user ID: {}", request.getEmployeeId());

        EmployeeUser employee = employeeRepository.findByUserId(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        // Date validation
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("End date cannot be before start date");
        }

        LeaveType leaveType = parseLeaveType(request.getLeaveType());
        long days = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;

        // Check overlapping leaves
        List<LeaveRequest> overlapping = leaveRepository.findActiveLeaveOnDate(
                employee.getId(), request.getStartDate());
        if (overlapping != null && !overlapping.isEmpty()) {
            throw new BadRequestException("You already have a leave request for this period");
        }

        // ========== LEAVE BALANCE VALIDATION ==========
        int currentYear = LocalDate.now().getYear();
        long usedDays = leaveRepository.countApprovedLeavesByType(
                employee.getId(), leaveType, currentYear);

        int entitledDays = getEntitledDays(employee, leaveType);

        if (usedDays + days > entitledDays) {
            throw new BadRequestException(String.format(
                    "Insufficient leave balance. You have %d days remaining out of %d entitled days.",
                    entitledDays - usedDays, entitledDays));
        }
        // ========== END BALANCE VALIDATION ==========

        LeaveRequest leave = new LeaveRequest();
        leave.setEmployee(employee);
        leave.setLeaveType(leaveType);
        leave.setStartDate(request.getStartDate());
        leave.setEndDate(request.getEndDate());
        leave.setTotalDays(BigDecimal.valueOf(days));
        leave.setReason(request.getReason());
        leave.setStatus(LeaveStatus.PENDING);
        leave.setContactDuringLeave(request.getContactDuringLeave());
        leave.setAlternateContact(request.getAlternateContact());

        LeaveRequest saved = leaveRepository.save(leave);
        log.info("Leave applied successfully. ID: {}", saved.getId());

        auditService.logAction(employee.getUser(), "APPLY_LEAVE", "LEAVE_REQUEST", saved.getId(),
                String.format("Applied for %s leave from %s to %s (%d days)",
                        leaveType, request.getStartDate(), request.getEndDate(), days));

        return saved;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getLeaveBalance(Long userId) {
        EmployeeUser employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        int currentYear = LocalDate.now().getYear();

        long annualUsed = leaveRepository.countApprovedLeavesByType(
                employee.getId(), LeaveType.ANNUAL, currentYear);
        long sickUsed = leaveRepository.countApprovedLeavesByType(
                employee.getId(), LeaveType.SICK, currentYear);
        long casualUsed = leaveRepository.countApprovedLeavesByType(
                employee.getId(), LeaveType.CASUAL, currentYear);

        int annualTotal = employee.getAnnualLeavesEntitled() != null ? employee.getAnnualLeavesEntitled() : 24;
        int sickTotal = employee.getSickLeavesEntitled() != null ? employee.getSickLeavesEntitled() : 12;
        int casualTotal = employee.getCasualLeavesEntitled() != null ? employee.getCasualLeavesEntitled() : 12;

        Map<String, Object> balance = new HashMap<>();
        balance.put("annual", Map.of("used", annualUsed, "total", annualTotal, "remaining", annualTotal - annualUsed));
        balance.put("sick", Map.of("used", sickUsed, "total", sickTotal, "remaining", sickTotal - sickUsed));
        balance.put("casual", Map.of("used", casualUsed, "total", casualTotal, "remaining", casualTotal - casualUsed));
        return balance;
    }

    @Transactional
    public LeaveRequest cancelLeave(Long leaveId, Long userId, String reason) {
        LeaveRequest leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found"));

        EmployeeUser employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        if (!leave.getEmployee().getId().equals(employee.getId())) {
            throw new BadRequestException("You can only cancel your own leave requests");
        }

        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new BadRequestException("Only pending leave requests can be cancelled");
        }

        // Additional business rule: cannot cancel if leave has already started
        if (leave.getStartDate().isBefore(LocalDate.now()) || leave.getStartDate().isEqual(LocalDate.now())) {
            throw new BadRequestException("Cannot cancel leave that has already started");
        }

        leave.setStatus(LeaveStatus.CANCELLED);
        leave.setCancelledAt(LocalDateTime.now());
        leave.setCancellationReason(reason);

        LeaveRequest saved = leaveRepository.save(leave);
        auditService.logAction(employee.getUser(), "CANCEL_LEAVE", "LEAVE_REQUEST", saved.getId(),
                "Cancelled leave request. Reason: " + reason);

        return saved;
    }

    @Transactional
    public LeaveRequest approveLeave(Long leaveId, Long approverUserId, String remarks) {
        return reviewLeave(leaveId, approverUserId, remarks, LeaveStatus.APPROVED);
    }

    @Transactional
    public LeaveRequest rejectLeave(Long leaveId, Long approverUserId, String remarks) {
        return reviewLeave(leaveId, approverUserId, remarks, LeaveStatus.REJECTED);
    }

    private LeaveRequest reviewLeave(Long leaveId, Long approverUserId, String remarks, LeaveStatus targetStatus) {
        EmployeeUser approver = employeeRepository.findByUserId(approverUserId).orElse(null);

        LeaveRequest leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found"));

        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new BadRequestException("Only pending leave requests can be " + targetStatus.name().toLowerCase());
        }

        leave.setStatus(targetStatus);
        leave.setReviewRemarks(remarks);
        leave.setReviewedAt(LocalDateTime.now());
        if (approver != null) {
            leave.setReviewedBy(approver.getUser());
        }

        return leaveRepository.save(leave);
    }

    private LeaveType parseLeaveType(String type) {
        if (type == null) {
            throw new BadRequestException("Leave type is required");
        }
        try {
            return LeaveType.valueOf(type.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid leave type: " + type);
        }
    }

    private int getEntitledDays(EmployeeUser employee, LeaveType leaveType) {
        switch (leaveType) {
            case ANNUAL:
                return employee.getAnnualLeavesEntitled() != null ? employee.getAnnualLeavesEntitled() : 24;
            case SICK:
                return employee.getSickLeavesEntitled() != null ? employee.getSickLeavesEntitled() : 12;
            case CASUAL:
                return employee.getCasualLeavesEntitled() != null ? employee.getCasualLeavesEntitled() : 12;
            case MATERNITY:
                return 180; // Standard maternity leave
            case PATERNITY:
                return 15;  // Standard paternity leave
            case BEREAVEMENT:
                return 7;   // Standard bereavement leave
            case UNPAID:
            case WORK_FROM_HOME:
                return Integer.MAX_VALUE; // No limit
            default:
                return 0;
        }
    }
}