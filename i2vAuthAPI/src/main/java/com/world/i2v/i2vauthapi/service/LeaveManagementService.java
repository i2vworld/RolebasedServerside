package com.world.i2v.i2vauthapi.service;

// Remove or comment out this entire file if not needed
// Or replace with this simplified version

import com.world.i2v.i2vauthapi.dto.request.LeaveRequestDto;
import com.world.i2v.i2vauthapi.entity.EmployeeUser;
import com.world.i2v.i2vauthapi.entity.LeaveRequest;
import com.world.i2v.i2vauthapi.entity.User;
import com.world.i2v.i2vauthapi.exception.BadRequestException;
import com.world.i2v.i2vauthapi.exception.ResourceNotFoundException;
import com.world.i2v.i2vauthapi.repository.EmployeeUserRepository;
import com.world.i2v.i2vauthapi.repository.LeaveRequestRepository;
import com.world.i2v.i2vauthapi.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class LeaveManagementService {

    private static final Logger log = LoggerFactory.getLogger(LeaveManagementService.class);

    private final LeaveRequestRepository leaveRepository;
    private final EmployeeUserRepository employeeRepository;
    private final UserRepository userRepository;

    public LeaveManagementService(LeaveRequestRepository leaveRepository,
                                  EmployeeUserRepository employeeRepository,
                                  UserRepository userRepository) {
        this.leaveRepository = leaveRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
    }

    // =====================================================
    // APPLY LEAVE
    // =====================================================

    @Transactional
    public LeaveRequest applyLeave(LeaveRequestDto request) {
        log.info("Applying leave for user ID: {}", request.getEmployeeId());

        EmployeeUser employee = employeeRepository.findByUserId(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found for user ID: " + request.getEmployeeId()));

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("End date cannot be before start date");
        }

        long days = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;
        request.setTotalDays(BigDecimal.valueOf(days));

        List<LeaveRequest> overlapping = leaveRepository.findActiveLeaveOnDate(
                employee.getId(), request.getStartDate());
        if (!overlapping.isEmpty()) {
            throw new BadRequestException("You already have a leave request for this period");
        }

        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setEmployee(employee);
        leaveRequest.setLeaveType(LeaveRequest.LeaveType.valueOf(request.getLeaveType()));
        leaveRequest.setStartDate(request.getStartDate());
        leaveRequest.setEndDate(request.getEndDate());
        leaveRequest.setTotalDays(request.getTotalDays());
        leaveRequest.setReason(request.getReason());
        leaveRequest.setStatus(LeaveRequest.LeaveStatus.PENDING);
        leaveRequest.setContactDuringLeave(request.getContactDuringLeave());
        leaveRequest.setAlternateContact(request.getAlternateContact());

        LeaveRequest saved = leaveRepository.save(leaveRequest);
        log.info("Leave applied successfully for employee: {}", employee.getEmployeeCode());
        return saved;
    }

    // =====================================================
    // GET EMPLOYEE LEAVES - Using simple method
    // =====================================================

    @Transactional(readOnly = true)
    public List<LeaveRequest> getEmployeeLeaves(Long userId) {
        EmployeeUser employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found for user ID: " + userId));
        return leaveRepository.findByEmployeeId(employee.getId());
    }

    // =====================================================
    // GET MY PENDING LEAVES
    // =====================================================

    @Transactional(readOnly = true)
    public List<LeaveRequest> getMyPendingLeaves(Long userId) {
        EmployeeUser employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found for user ID: " + userId));
        return leaveRepository.findByEmployeeIdAndStatus(employee.getId(), LeaveRequest.LeaveStatus.PENDING);
    }

    // =====================================================
    // GET LEAVE BALANCE
    // =====================================================

    @Transactional(readOnly = true)
    public Map<String, Object> getLeaveBalance(Long userId) {
        EmployeeUser employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found for user ID: " + userId));

        int currentYear = LocalDate.now().getYear();

        long annualUsed = leaveRepository.countApprovedLeavesByType(
                employee.getId(), LeaveRequest.LeaveType.ANNUAL, currentYear);
        long sickUsed = leaveRepository.countApprovedLeavesByType(
                employee.getId(), LeaveRequest.LeaveType.SICK, currentYear);
        long casualUsed = leaveRepository.countApprovedLeavesByType(
                employee.getId(), LeaveRequest.LeaveType.CASUAL, currentYear);

        int annualTotal = employee.getAnnualLeavesEntitled() != null ? employee.getAnnualLeavesEntitled() : 24;
        int sickTotal = employee.getSickLeavesEntitled() != null ? employee.getSickLeavesEntitled() : 12;
        int casualTotal = employee.getCasualLeavesEntitled() != null ? employee.getCasualLeavesEntitled() : 12;

        Map<String, Object> balance = new HashMap<>();
        balance.put("annual", Map.of("used", annualUsed, "total", annualTotal, "remaining", annualTotal - annualUsed));
        balance.put("sick", Map.of("used", sickUsed, "total", sickTotal, "remaining", sickTotal - sickUsed));
        balance.put("casual", Map.of("used", casualUsed, "total", casualTotal, "remaining", casualTotal - casualUsed));

        return balance;
    }

    // =====================================================
    // CANCEL LEAVE
    // =====================================================

    @Transactional
    public LeaveRequest cancelLeave(Long id, Long userId, String reason) {
        log.info("Cancelling leave request ID: {} by user: {}", id, userId);

        LeaveRequest leave = leaveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found"));

        EmployeeUser employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found for user ID: " + userId));

        if (!leave.getEmployee().getId().equals(employee.getId())) {
            throw new BadRequestException("You can only cancel your own leave requests");
        }

        if (leave.getStatus() != LeaveRequest.LeaveStatus.PENDING) {
            throw new BadRequestException("Only pending leave requests can be cancelled");
        }

        leave.setStatus(LeaveRequest.LeaveStatus.CANCELLED);
        leave.setCancelledAt(LocalDateTime.now());
        leave.setCancellationReason(reason);

        LeaveRequest saved = leaveRepository.save(leave);
        log.info("Leave {} cancelled", id);
        return saved;
    }
}