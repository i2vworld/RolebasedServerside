package com.world.i2v.i2vauthapi.Controller;

import com.world.i2v.i2vauthapi.dto.request.AttendanceRequest;
import com.world.i2v.i2vauthapi.dto.response.ApiResponse;
import com.world.i2v.i2vauthapi.dto.response.AttendanceResponse;
import com.world.i2v.i2vauthapi.dto.response.AttendanceStatisticsResponse;
import com.world.i2v.i2vauthapi.dto.response.AttendanceSummaryResponse;
import com.world.i2v.i2vauthapi.service.AttendanceManagementService;
import com.world.i2v.i2vauthapi.service.AttendanceCorrectionService;
import com.world.i2v.i2vauthapi.security.UserDetailsImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/attendance")
@RequiredArgsConstructor
public class AttendanceManagementController {

    private final AttendanceManagementService attendanceManagementService;
    private final AttendanceCorrectionService attendanceCorrectionService;

    @GetMapping("/employees")
    @PreAuthorize("hasRole('EMPLOYEE') and @securityUtils.canManageAttendance()")
    public ResponseEntity<ApiResponse<Page<Map<String, Object>>>> getAllEmployeesWithAttendance(
            @RequestParam int year, @RequestParam int month, Pageable pageable) {
        try {
            Page<Map<String, Object>> result = attendanceManagementService
                    .getAllEmployeesWithAttendancePageable(year, month, pageable);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("Error getting employees with attendance: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.success(Page.empty()));
        }
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasRole('EMPLOYEE') and @securityUtils.canManageAttendance()")
    public ResponseEntity<ApiResponse<Page<AttendanceResponse>>> getEmployeeAttendance(
            @PathVariable Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Pageable pageable) {
        try {
            Page<AttendanceResponse> attendances = attendanceManagementService
                    .getEmployeeAttendancePageable(employeeId, startDate, endDate, pageable);
            return ResponseEntity.ok(ApiResponse.success(attendances));
        } catch (Exception e) {
            log.error("Error getting employee attendance: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.success(Page.empty()));
        }
    }

    @GetMapping("/employee/{employeeId}/summary")
    @PreAuthorize("hasRole('EMPLOYEE') and @securityUtils.canManageAttendance()")
    public ResponseEntity<ApiResponse<AttendanceSummaryResponse>> getEmployeeAttendanceSummary(
            @PathVariable Long employeeId, @RequestParam int year, @RequestParam int month) {
        try {
            Map<String, Object> summary = attendanceManagementService
                    .getEmployeeAttendanceSummary(employeeId, year, month);
            return ResponseEntity.ok(ApiResponse.success(buildSummaryResponse(summary)));
        } catch (Exception e) {
            log.error("Error getting employee summary: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.success(null));
        }
    }

    @GetMapping("/employee/{employeeId}/today")
    @PreAuthorize("hasRole('EMPLOYEE') and @securityUtils.canManageAttendance()")
    public ResponseEntity<ApiResponse<AttendanceResponse>> getEmployeeTodayAttendance(
            @PathVariable Long employeeId) {
        try {
            AttendanceResponse response = attendanceManagementService.getEmployeeTodayAttendance(employeeId);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Error getting today's attendance for employee {}: {}", employeeId, e.getMessage());
            return ResponseEntity.ok(ApiResponse.success(null));
        }
    }

    @PostMapping("/mark")
    @PreAuthorize("hasRole('EMPLOYEE') and @securityUtils.canManageAttendance()")
    public ResponseEntity<ApiResponse<AttendanceResponse>> markAttendance(
            @Valid @RequestBody AttendanceRequest request) {
        try {
            AttendanceResponse attendance = attendanceManagementService.markAttendance(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Attendance marked successfully", attendance));
        } catch (Exception e) {
            log.error("Error marking attendance: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{attendanceId}")
    @PreAuthorize("hasRole('EMPLOYEE') and @securityUtils.canManageAttendance()")
    public ResponseEntity<ApiResponse<AttendanceResponse>> updateAttendance(
            @PathVariable Long attendanceId, @Valid @RequestBody AttendanceRequest request) {
        try {
            AttendanceResponse attendance = attendanceManagementService.updateAttendance(attendanceId, request);
            return ResponseEntity.ok(ApiResponse.success("Attendance updated successfully", attendance));
        } catch (Exception e) {
            log.error("Error updating attendance: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{attendanceId}")
    @PreAuthorize("hasRole('EMPLOYEE') and @securityUtils.canManageAttendance()")
    public ResponseEntity<ApiResponse<Void>> deleteAttendance(@PathVariable Long attendanceId) {
        try {
            attendanceManagementService.deleteAttendance(attendanceId);
            return ResponseEntity.ok(ApiResponse.success("Attendance deleted successfully", null));
        } catch (Exception e) {
            log.error("Error deleting attendance: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasRole('EMPLOYEE') and @securityUtils.canManageAttendance()")
    public ResponseEntity<ApiResponse<AttendanceStatisticsResponse>> getCompanyAttendanceStatistics(
            @RequestParam int year, @RequestParam int month) {
        try {
            AttendanceStatisticsResponse statistics = attendanceManagementService
                    .getCompanyAttendanceStatistics(year, month);
            return ResponseEntity.ok(ApiResponse.success(statistics));
        } catch (Exception e) {
            log.error("Error getting statistics: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.success(null));
        }
    }

    @PostMapping("/bulk-mark")
    @PreAuthorize("hasRole('EMPLOYEE') and @securityUtils.canManageAttendance()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> bulkMarkAttendance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestBody List<Long> employeeIds, @RequestParam String status) {
        try {
            Map<String, Object> result = attendanceManagementService.bulkMarkAttendance(date, employeeIds, status);
            return ResponseEntity.ok(ApiResponse.success("Bulk attendance marked", result));
        } catch (Exception e) {
            log.error("Error bulk marking attendance: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('EMPLOYEE') and @securityUtils.canManageAttendance()")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPendingAttendance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            List<Map<String, Object>> pending = attendanceManagementService.getPendingAttendance(startDate, endDate);
            return ResponseEntity.ok(ApiResponse.success(pending));
        } catch (Exception e) {
            log.error("Error getting pending attendance: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.success(List.of()));
        }
    }

    @GetMapping("/export")
    @PreAuthorize("hasRole('EMPLOYEE') and @securityUtils.canManageAttendance()")
    public ResponseEntity<byte[]> exportAttendanceReport(
            @RequestParam int year, @RequestParam int month,
            @RequestParam(required = false) Long departmentId) {
        try {
            return attendanceManagementService.exportAttendanceReport(year, month, departmentId);
        } catch (Exception e) {
            log.error("Error exporting attendance report: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new byte[0]);
        }
    }

    @GetMapping("/corrections/pending")
    @PreAuthorize("hasRole('EMPLOYEE') and @securityUtils.canManageAttendance()")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPendingCorrectionRequests() {
        try {
            return ResponseEntity.ok(ApiResponse.success(attendanceCorrectionService.getPendingRequests()));
        } catch (Exception e) {
            log.error("Error getting pending attendance corrections: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/corrections/{requestId}/approve")
    @PreAuthorize("hasRole('EMPLOYEE') and @securityUtils.canManageAttendance()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> approveCorrectionRequest(
            @PathVariable Long requestId,
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @RequestParam(required = false) String remarks) {
        try {
            Map<String, Object> result = attendanceCorrectionService.approve(requestId, currentUser.getId(), remarks);
            return ResponseEntity.ok(ApiResponse.success("Attendance correction approved", result));
        } catch (Exception e) {
            log.error("Error approving attendance correction {}: {}", requestId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/corrections/{requestId}/reject")
    @PreAuthorize("hasRole('EMPLOYEE') and @securityUtils.canManageAttendance()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> rejectCorrectionRequest(
            @PathVariable Long requestId,
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @RequestParam(required = false) String remarks) {
        try {
            Map<String, Object> result = attendanceCorrectionService.reject(requestId, currentUser.getId(), remarks);
            return ResponseEntity.ok(ApiResponse.success("Attendance correction rejected", result));
        } catch (Exception e) {
            log.error("Error rejecting attendance correction {}: {}", requestId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        }
    }
    // ==================== HELPERS ====================

    @SuppressWarnings("unchecked")
    private AttendanceSummaryResponse buildSummaryResponse(Map<String, Object> summary) {
        return AttendanceSummaryResponse.builder()
                .employeeId(toLong(summary.get("employeeId")))
                .employeeCode(toStr(summary.get("employeeCode")))
                .employeeName(toStr(summary.get("employeeName")))
                .departmentName(toStr(summary.get("departmentName")))
                .designationTitle(toStr(summary.get("designationTitle")))
                .year((int) summary.get("year"))
                .month((int) summary.get("month"))
                .totalWorkingDays((int) summary.get("totalWorkingDays"))
                .presentDays((long) summary.get("presentDays"))
                .absentDays((long) summary.get("absentDays"))
                .lateDays((long) summary.get("lateDays"))
                .leaveDays(toLong(summary.get("leaveDays"), 0L))
                .holidayDays(toLong(summary.get("holidayDays"), 0L))
                .totalHours(summary.get("totalHours") != null
                        ? new BigDecimal(summary.get("totalHours").toString()) : null)
                .attendanceRate(((Number) summary.get("attendanceRate")).doubleValue())
                .dailyBreakdown((List<AttendanceSummaryResponse.DailyAttendanceDetail>) summary.get("dailyBreakdown"))
                .build();
    }

    private String toStr(Object value) {
        return value != null ? value.toString() : null;
    }

    private Long toLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private Long toLong(Object value, Long fallback) {
        Long result = toLong(value);
        return result != null ? result : fallback;
    }
}
