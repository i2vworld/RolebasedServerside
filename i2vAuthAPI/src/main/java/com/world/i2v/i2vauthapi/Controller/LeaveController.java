package com.world.i2v.i2vauthapi.Controller;

import com.world.i2v.i2vauthapi.dto.request.LeaveRequestDto;
import com.world.i2v.i2vauthapi.dto.response.ApiResponse;
import com.world.i2v.i2vauthapi.entity.EmployeeUser;
import com.world.i2v.i2vauthapi.entity.LeaveRequest;
import com.world.i2v.i2vauthapi.repository.EmployeeUserRepository;
import com.world.i2v.i2vauthapi.repository.LeaveRequestRepository;
import com.world.i2v.i2vauthapi.security.UserDetailsImpl;
import com.world.i2v.i2vauthapi.service.LeaveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/leaves")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;
    private final LeaveRequestRepository leaveRepository;
    private final EmployeeUserRepository employeeRepository;

    // ==================== MY LEAVES ====================

    @GetMapping("/my-leaves")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyLeaves(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        try {
            EmployeeUser employee = employeeRepository.findByUserId(currentUser.getId()).orElse(null);
            if (employee == null) {
                return ResponseEntity.ok(ApiResponse.success(new ArrayList<>()));
            }

            List<Map<String, Object>> leaveData = leaveRepository.findByEmployeeId(employee.getId()).stream()
                    .map(this::mapLeaveToData)
                    .toList();
            return ResponseEntity.ok(ApiResponse.success(leaveData));
        } catch (Exception e) {
            log.error("Error getting my leaves: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.success(new ArrayList<>()));
        }
    }

    @GetMapping("/my-balance")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyLeaveBalance(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        try {
            return ResponseEntity.ok(ApiResponse.success(leaveService.getLeaveBalance(currentUser.getId())));
        } catch (Exception e) {
            log.error("Error getting leave balance: {}", e.getMessage(), e);
            Map<String, Object> defaultBalance = new HashMap<>();
            defaultBalance.put("annual", Map.of("used", 0, "total", 24, "remaining", 24));
            defaultBalance.put("sick", Map.of("used", 0, "total", 12, "remaining", 12));
            defaultBalance.put("casual", Map.of("used", 0, "total", 12, "remaining", 12));
            return ResponseEntity.ok(ApiResponse.success(defaultBalance));
        }
    }

    // ==================== APPLY/CANCEL ====================

    @PostMapping("/apply")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> applyLeave(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @Valid @RequestBody LeaveRequestDto request) {
        try {
            request.setEmployeeId(currentUser.getId());
            LeaveRequest leave = leaveService.applyLeave(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Leave applied successfully", mapLeaveToData(leave)));
        } catch (Exception e) {
            log.error("Error applying leave: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> cancelLeave(
            @PathVariable Long id,
            @RequestParam String reason,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        try {
            LeaveRequest leave = leaveService.cancelLeave(id, currentUser.getId(), reason);
            return ResponseEntity.ok(ApiResponse.success("Leave cancelled successfully",
                    Map.of("id", leave.getId(), "status", leave.getStatus().name())));
        } catch (Exception e) {
            log.error("Error cancelling leave: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    // ==================== TEAM LEAVES ====================

    @GetMapping("/team-leaves")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTeamLeaves(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        try {
            EmployeeUser currentEmployee = employeeRepository.findByUserId(currentUser.getId()).orElse(null);
            if (currentEmployee == null) {
                return ResponseEntity.ok(ApiResponse.success(new ArrayList<>()));
            }

            List<Map<String, Object>> teamLeaves = new ArrayList<>();
            for (EmployeeUser emp : employeeRepository.findByReportingToId(currentEmployee.getId())) {
                for (LeaveRequest leave : leaveRepository.findByEmployeeId(emp.getId())) {
                    Map<String, Object> item = mapLeaveToData(leave);
                    item.put("employeeId", emp.getId());
                    item.put("employeeCode", emp.getEmployeeCode());
                    item.put("employeeName", emp.getUser() != null ? emp.getUser().getFullName() : "Unknown");
                    item.put("departmentName", emp.getDepartment() != null ? emp.getDepartment().getName() : "Unknown");
                    teamLeaves.add(item);
                }
            }
            return ResponseEntity.ok(ApiResponse.success(teamLeaves));
        } catch (Exception e) {
            log.error("Error getting team leaves: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.success(new ArrayList<>()));
        }
    }

    @GetMapping("/team-pending")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTeamPendingLeaves(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        try {
            EmployeeUser currentEmployee = employeeRepository.findByUserId(currentUser.getId()).orElse(null);
            if (currentEmployee == null) {
                return ResponseEntity.ok(ApiResponse.success(Map.of("data", new ArrayList<>(), "canApprove", false)));
            }

            List<EmployeeUser> directReports = employeeRepository.findByReportingToId(currentEmployee.getId());
            List<Map<String, Object>> pendingLeaves = new ArrayList<>();

            for (EmployeeUser emp : directReports) {
                for (LeaveRequest leave : leaveRepository.findByEmployeeIdAndStatus(
                        emp.getId(), LeaveRequest.LeaveStatus.PENDING)) {
                    Map<String, Object> item = mapLeaveToData(leave);
                    item.put("employeeId", emp.getId());
                    item.put("employeeCode", emp.getEmployeeCode());
                    item.put("employeeName", emp.getUser() != null ? emp.getUser().getFullName() : "Unknown");
                    item.put("departmentName", emp.getDepartment() != null ? emp.getDepartment().getName() : "Unknown");
                    pendingLeaves.add(item);
                }
            }

            return ResponseEntity.ok(ApiResponse.success(Map.of(
                    "data", pendingLeaves,
                    "canApprove", !directReports.isEmpty(),
                    "pendingCount", pendingLeaves.size()
            )));
        } catch (Exception e) {
            log.error("Error getting team pending leaves: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.success(Map.of("data", new ArrayList<>(), "canApprove", false)));
        }
    }

    // ==================== ALL LEAVES ====================

    @GetMapping("/all-leaves")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllLeaves() {
        try {
            List<Map<String, Object>> leaveData = leaveRepository.findAll().stream()
                    .map(this::mapLeaveToData)
                    .toList();
            return ResponseEntity.ok(ApiResponse.success(leaveData));
        } catch (Exception e) {
            log.error("Error getting all leaves: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to load leave requests: " + e.getMessage()));
        }
    }

    @GetMapping("/all-pending")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllPendingLeaves() {
        try {
            List<Map<String, Object>> pendingLeaves = leaveRepository.findAll().stream()
                    .filter(leave -> leave.getStatus() == LeaveRequest.LeaveStatus.PENDING)
                    .map(this::mapLeaveToData)
                    .toList();

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("data", pendingLeaves);
            response.put("pendingCount", pendingLeaves.size());
            response.put("canApprove", true);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Error getting all pending leaves: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new LinkedHashMap<>();
            errorResponse.put("data", new ArrayList<>());
            errorResponse.put("pendingCount", 0);
            errorResponse.put("canApprove", false);
            return ResponseEntity.ok(ApiResponse.success(errorResponse));
        }
    }

    @GetMapping("/all-statistics")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllStatistics(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        try {
            int currentYear = year != null ? year : java.time.LocalDate.now().getYear();

            List<LeaveRequest> filteredLeaves = leaveRepository.findAll().stream()
                    .filter(leave -> leave.getStartDate() != null
                            && leave.getStartDate().getYear() == currentYear)
                    .filter(leave -> month == null || leave.getStartDate().getMonthValue() == month)
                    .toList();

            long totalRequests = filteredLeaves.size();
            long pending = countByStatus(filteredLeaves, LeaveRequest.LeaveStatus.PENDING);
            long approved = countByStatus(filteredLeaves, LeaveRequest.LeaveStatus.APPROVED);
            long rejected = countByStatus(filteredLeaves, LeaveRequest.LeaveStatus.REJECTED);

            double totalDays = filteredLeaves.stream()
                    .mapToDouble(leave -> leave.getTotalDays() != null ? leave.getTotalDays().doubleValue() : 0)
                    .sum();

            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("year", currentYear);
            stats.put("month", month);
            stats.put("totalRequests", totalRequests);
            stats.put("pending", pending);
            stats.put("approved", approved);
            stats.put("rejected", rejected);
            stats.put("totalDays", Math.round(totalDays * 10) / 10.0);
            stats.put("totalEmployees", employeeRepository.count());
            return ResponseEntity.ok(ApiResponse.success(stats));
        } catch (Exception e) {
            log.error("Error getting all statistics: {}", e.getMessage(), e);
            Map<String, Object> emptyStats = new LinkedHashMap<>();
            emptyStats.put("totalRequests", 0);
            emptyStats.put("pending", 0);
            emptyStats.put("approved", 0);
            emptyStats.put("rejected", 0);
            emptyStats.put("totalDays", 0);
            emptyStats.put("totalEmployees", 0);
            return ResponseEntity.ok(ApiResponse.success(emptyStats));
        }
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getLeavesByEmployee(
            @PathVariable Long employeeId) {
        try {
            if (!employeeRepository.existsById(employeeId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Employee not found"));
            }

            List<Map<String, Object>> leaveData = leaveRepository.findByEmployeeId(employeeId).stream()
                    .map(this::mapLeaveToData)
                    .toList();
            return ResponseEntity.ok(ApiResponse.success(leaveData));
        } catch (Exception e) {
            log.error("Error getting leaves for employee {}: {}", employeeId, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.success(new ArrayList<>()));
        }
    }

    // ==================== APPROVE/REJECT ====================

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> approveLeave(
            @PathVariable Long id,
            @RequestParam(required = false) String remarks,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        LeaveRequest leave = leaveService.approveLeave(id, currentUser.getId(), remarks);
        return ResponseEntity.ok(ApiResponse.success("Leave approved successfully",
                Map.of("id", leave.getId(), "status", leave.getStatus().name())));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> rejectLeave(
            @PathVariable Long id,
            @RequestParam(required = false) String remarks,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        LeaveRequest leave = leaveService.rejectLeave(id, currentUser.getId(), remarks);
        return ResponseEntity.ok(ApiResponse.success("Leave rejected successfully",
                Map.of("id", leave.getId(), "status", leave.getStatus().name())));
    }

    // ==================== HELPERS ====================

    private long countByStatus(List<LeaveRequest> leaves, LeaveRequest.LeaveStatus status) {
        return leaves.stream().filter(leave -> leave.getStatus() == status).count();
    }

    private Map<String, Object> mapLeaveToData(LeaveRequest leave) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", leave.getId());
        item.put("leaveType", leave.getLeaveType() != null ? leave.getLeaveType().name() : null);
        item.put("startDate", leave.getStartDate() != null ? leave.getStartDate().toString() : null);
        item.put("endDate", leave.getEndDate() != null ? leave.getEndDate().toString() : null);
        item.put("totalDays", leave.getTotalDays() != null ? leave.getTotalDays() : 0);
        item.put("reason", leave.getReason() != null ? leave.getReason() : "");
        item.put("status", leave.getStatus() != null ? leave.getStatus().name() : null);
        item.put("appliedAt", leave.getAppliedAt() != null ? leave.getAppliedAt().toString() : null);
        item.put("contactDuringLeave", leave.getContactDuringLeave() != null ? leave.getContactDuringLeave() : "");
        item.put("alternateContact", leave.getAlternateContact() != null ? leave.getAlternateContact() : "");
        item.put("reviewRemarks", leave.getReviewRemarks() != null ? leave.getReviewRemarks() : "");

        EmployeeUser emp = leave.getEmployee();
        if (emp != null) {
            item.put("employeeId", emp.getId());
            item.put("employeeCode", emp.getEmployeeCode() != null ? emp.getEmployeeCode() : "");
            item.put("employeeName", emp.getUser() != null && emp.getUser().getFullName() != null
                    ? emp.getUser().getFullName() : "Unknown");
            item.put("employeeEmail", emp.getUser() != null && emp.getUser().getEmail() != null
                    ? emp.getUser().getEmail() : "");
            item.put("departmentId", emp.getDepartment() != null ? emp.getDepartment().getId() : null);
            item.put("departmentName", emp.getDepartment() != null && emp.getDepartment().getName() != null
                    ? emp.getDepartment().getName() : "Unknown");
            item.put("designationId", emp.getDesignation() != null ? emp.getDesignation().getId() : null);
            item.put("designationTitle", emp.getDesignation() != null && emp.getDesignation().getTitle() != null
                    ? emp.getDesignation().getTitle() : "Unknown");
        } else {
            item.put("employeeId", null);
            item.put("employeeCode", "");
            item.put("employeeName", "Unknown");
            item.put("employeeEmail", "");
            item.put("departmentId", null);
            item.put("departmentName", "Unknown");
            item.put("designationId", null);
            item.put("designationTitle", "Unknown");
        }

        return item;
    }
}
