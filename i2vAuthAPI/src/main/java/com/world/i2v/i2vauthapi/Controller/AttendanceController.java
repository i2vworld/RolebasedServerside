package com.world.i2v.i2vauthapi.Controller;

import com.world.i2v.i2vauthapi.dto.request.AttendanceCorrectionRequest;
import com.world.i2v.i2vauthapi.dto.response.ApiResponse;
import com.world.i2v.i2vauthapi.entity.Attendance;
import com.world.i2v.i2vauthapi.security.UserDetailsImpl;
import com.world.i2v.i2vauthapi.service.AttendanceService;
import com.world.i2v.i2vauthapi.service.AttendanceCorrectionService;
import com.world.i2v.i2vauthapi.util.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
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

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final AttendanceService attendanceService;
    private final AttendanceCorrectionService attendanceCorrectionService;

    @PostMapping("/check-in")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkIn(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String ipAddress,
            HttpServletRequest request) {
        try {
            log.info("Check-in request for user: {}", currentUser.getId());
            String clientIp = ipAddress != null ? ipAddress : RequestUtils.getClientIp(request);
            Attendance attendance = attendanceService.checkIn(currentUser.getId(), location, clientIp);

            Map<String, Object> response = Map.of(
                    "id", attendance.getId(),
                    "checkInTime", formatLocalTime(attendance.getCheckInTime()),
                    "attendanceDate", attendance.getAttendanceDate().toString(),
                    "isLate", attendance.getIsLate(),
                    "lateMinutes", attendance.getLateMinutes()
            );
            return ResponseEntity.ok(ApiResponse.success("Checked in successfully", response));
        } catch (Exception e) {
            log.error("Check-in error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/check-out")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkOut(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String ipAddress,
            HttpServletRequest request) {
        try {
            log.info("Check-out request for user: {}", currentUser.getId());
            String clientIp = ipAddress != null ? ipAddress : RequestUtils.getClientIp(request);
            Attendance attendance = attendanceService.checkOut(currentUser.getId(), location, clientIp);

            Map<String, Object> response = Map.of(
                    "id", attendance.getId(),
                    "checkInTime", formatLocalTime(attendance.getCheckInTime()),
                    "checkOutTime", formatLocalTime(attendance.getCheckOutTime()),
                    "totalHours", attendance.getTotalHours(),
                    "attendanceDate", attendance.getAttendanceDate().toString()
            );
            return ResponseEntity.ok(ApiResponse.success("Checked out successfully", response));
        } catch (Exception e) {
            log.error("Check-out error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/today")
    @PreAuthorize("isAuthenticated() and @securityUtils.canReadAttendance()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTodayAttendance(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        try {
            Attendance attendance = attendanceService.getTodayAttendance(currentUser.getId());
            if (attendance == null) {
                return ResponseEntity.ok(ApiResponse.success(null));
            }
            return ResponseEntity.ok(ApiResponse.success(toAttendanceMap(attendance)));
        } catch (Exception e) {
            log.error("Error getting today's attendance: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.success(null));
        }
    }

    @GetMapping("/history")
    @PreAuthorize("isAuthenticated() and @securityUtils.canReadAttendance()")
    public ResponseEntity<ApiResponse<Page<Map<String, Object>>>> getAttendanceHistory(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Pageable pageable) {
        try {
            LocalDate effectiveEndDate = endDate != null ? endDate : LocalDate.now();
            LocalDate effectiveStartDate = startDate != null
                    ? startDate
                    : effectiveEndDate.withDayOfMonth(1);
            Page<Map<String, Object>> attendances = attendanceService.getAttendanceHistoryPageable(
                            currentUser.getId(), effectiveStartDate, effectiveEndDate, pageable)
                    .map(this::toHistoryMap);
            return ResponseEntity.ok(ApiResponse.success(attendances));
        } catch (Exception e) {
            log.error("Error fetching attendance history: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.success(Page.empty()));
        }
    }

    @GetMapping({"/summary", "/summery"})
    @PreAuthorize("isAuthenticated() and @securityUtils.canReadAttendance()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAttendanceSummary(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        try {
            LocalDate today = LocalDate.now();
            int effectiveYear = year != null ? year : today.getYear();
            int effectiveMonth = month != null ? month : today.getMonthValue();
            Map<String, Object> summary = attendanceService.getAttendanceSummary(
                    currentUser.getId(), effectiveYear, effectiveMonth);
            return ResponseEntity.ok(ApiResponse.success(summary));
        } catch (Exception e) {
            log.error("Error fetching summary: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.success(Map.of()));
        }
    }

    @PostMapping("/corrections")
    @PreAuthorize("isAuthenticated() and @securityUtils.canReadAttendance()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> submitCorrection(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @Valid @RequestBody AttendanceCorrectionRequest correctionRequest) {
        try {
            Map<String, Object> result = attendanceCorrectionService.submit(currentUser.getId(), correctionRequest);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Attendance correction submitted for review", result));
        } catch (Exception e) {
            log.error("Error submitting attendance correction: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/corrections/my")
    @PreAuthorize("isAuthenticated() and @securityUtils.canReadAttendance()")
    public ResponseEntity<ApiResponse<java.util.List<Map<String, Object>>>> getMyCorrections(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        try {
            return ResponseEntity.ok(ApiResponse.success(attendanceCorrectionService.getMyRequests(currentUser.getId())));
        } catch (Exception e) {
            log.error("Error getting attendance correction requests: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        }
    }
    // ==================== HELPERS ====================

    private Map<String, Object> toAttendanceMap(Attendance attendance) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", attendance.getId());
        map.put("checkInTime", formatLocalTime(attendance.getCheckInTime()));
        map.put("checkOutTime", formatLocalTime(attendance.getCheckOutTime()));
        map.put("totalHours", attendance.getTotalHours());
        map.put("attendanceDate", attendance.getAttendanceDate().toString());
        map.put("isLate", attendance.getIsLate());
        map.put("lateMinutes", attendance.getLateMinutes());
        return map;
    }

    private Map<String, Object> toHistoryMap(Attendance attendance) {
        Map<String, Object> map = toAttendanceMap(attendance);
        map.put("status", attendance.getStatus() != null ? attendance.getStatus().name() : null);
        return map;
    }

    private String formatLocalTime(LocalTime time) {
        if (time == null) {
            return null;
        }
        try {
            return time.format(TIME_FORMATTER);
        } catch (DateTimeException e) {
            log.error("Invalid time value: {}", time);
            return null;
        }
    }
}
