package com.world.i2v.i2vauthapi.service;

import com.world.i2v.i2vauthapi.dto.request.AttendanceCorrectionRequest;
import com.world.i2v.i2vauthapi.entity.Attendance;
import com.world.i2v.i2vauthapi.entity.Attendance.AttendanceStatus;
import com.world.i2v.i2vauthapi.entity.AttendanceCorrection;
import com.world.i2v.i2vauthapi.entity.AttendanceCorrection.CorrectionStatus;
import com.world.i2v.i2vauthapi.entity.EmployeeUser;
import com.world.i2v.i2vauthapi.entity.User;
import com.world.i2v.i2vauthapi.exception.BadRequestException;
import com.world.i2v.i2vauthapi.exception.ResourceNotFoundException;
import com.world.i2v.i2vauthapi.repository.AttendanceCorrectionRepository;
import com.world.i2v.i2vauthapi.repository.AttendanceRepository;
import com.world.i2v.i2vauthapi.repository.EmployeeUserRepository;
import com.world.i2v.i2vauthapi.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class AttendanceCorrectionService {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final AttendanceCorrectionRepository correctionRepository;
    private final AttendanceRepository attendanceRepository;
    private final EmployeeUserRepository employeeRepository;
    private final UserRepository userRepository;
    private final AttendanceCalculator calculator;
    private final CompanySettingsService settingsService;
    private final AuditService auditService;

    public AttendanceCorrectionService(AttendanceCorrectionRepository correctionRepository,
                                       AttendanceRepository attendanceRepository,
                                       EmployeeUserRepository employeeRepository,
                                       UserRepository userRepository,
                                       AttendanceCalculator calculator,
                                       CompanySettingsService settingsService,
                                       AuditService auditService) {
        this.correctionRepository = correctionRepository;
        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.calculator = calculator;
        this.settingsService = settingsService;
        this.auditService = auditService;
    }

    public Map<String, Object> submit(Long userId, AttendanceCorrectionRequest request) {
        EmployeeUser employee = employeeRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        if (request.getAttendanceDate().isAfter(today)) {
            throw new BadRequestException("Attendance correction can only be requested for today or an earlier date");
        }
        int correctionCutoffDay = settingsService.getAttendanceCorrectionCutoffDay();
        if (today.getDayOfMonth() > correctionCutoffDay) {
            throw new BadRequestException("Attendance correction requests close after day "
                    + correctionCutoffDay + " of each month for payroll processing");
        }
        if (request.getAttendanceDate().getYear() != today.getYear()
                || request.getAttendanceDate().getMonthValue() != today.getMonthValue()) {
            throw new BadRequestException("Attendance corrections are only available for the current payroll month");
        }
        validateTimes(request.getRequestedCheckInTime(), request.getRequestedCheckOutTime());
        if (correctionRepository.existsByEmployeeIdAndAttendanceDateAndStatus(
                employee.getId(), request.getAttendanceDate(), CorrectionStatus.PENDING)) {
            throw new BadRequestException("A correction request for this date is already pending");
        }

        AttendanceCorrection correction = new AttendanceCorrection();
        correction.setEmployee(employee);
        correction.setAttendanceDate(request.getAttendanceDate());
        correction.setRequestedCheckInTime(request.getRequestedCheckInTime());
        correction.setRequestedCheckOutTime(request.getRequestedCheckOutTime());
        correction.setReason(request.getReason().trim());
        correction.setStatus(CorrectionStatus.PENDING);

        Attendance existingAttendance = attendanceRepository
                .findByEmployeeIdAndAttendanceDate(employee.getId(), request.getAttendanceDate())
                .orElse(null);
        if ((existingAttendance == null || existingAttendance.getCheckInTime() == null)
                && request.getRequestedCheckInTime() == null) {
            throw new BadRequestException("A check-in time is required to correct an absent day");
        }
        if (existingAttendance != null) {
            correction.setCurrentCheckInTime(existingAttendance.getCheckInTime());
            correction.setCurrentCheckOutTime(existingAttendance.getCheckOutTime());
        }

        AttendanceCorrection saved = correctionRepository.save(correction);
        auditService.logAction(employee.getUser(), "SUBMIT_ATTENDANCE_CORRECTION",
                "ATTENDANCE_CORRECTION", saved.getId(), request.getAttendanceDate().toString());
        return toMap(saved);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMyRequests(Long userId) {
        EmployeeUser employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));
        return correctionRepository.findByEmployeeIdOrderBySubmittedAtDesc(employee.getId())
                .stream().map(this::toMap).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPendingRequests() {
        return correctionRepository.findByStatusOrderBySubmittedAtAsc(CorrectionStatus.PENDING)
                .stream().map(this::toMap).toList();
    }

    public Map<String, Object> approve(Long requestId, Long reviewerUserId, String remarks) {
        AttendanceCorrection correction = getPendingRequest(requestId, "approved");
        User reviewer = userRepository.findById(reviewerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Reviewing user not found"));
        EmployeeUser employee = correction.getEmployee();

        Attendance attendance = attendanceRepository
                .findByEmployeeIdAndAttendanceDate(employee.getId(), correction.getAttendanceDate())
                .orElseGet(() -> {
                    Attendance created = new Attendance();
                    created.setEmployee(employee);
                    created.setAttendanceDate(correction.getAttendanceDate());
                    return created;
                });

        if (correction.getRequestedCheckInTime() != null) {
            attendance.setCheckInTime(correction.getRequestedCheckInTime());
        }
        if (correction.getRequestedCheckOutTime() != null) {
            attendance.setCheckOutTime(correction.getRequestedCheckOutTime());
        }
        validateTimes(attendance.getCheckInTime(), attendance.getCheckOutTime());
        if (attendance.getCheckInTime() == null) {
            throw new BadRequestException("A check-in time is required to mark attendance as present");
        }

        attendance.setStatus(AttendanceStatus.PRESENT);
        if (attendance.getCheckInTime() != null && attendance.getCheckOutTime() != null) {
            long workedMinutes = calculateWorkedMinutes(attendance.getCheckInTime(), attendance.getCheckOutTime());
            attendance.setTotalHours(BigDecimal.valueOf(workedMinutes / 60.0).setScale(2, RoundingMode.HALF_UP));
        } else {
            attendance.setTotalHours(null);
        }

        LocalTime checkIn = attendance.getCheckInTime();
        LocalTime lateThreshold = calculator.getLateThreshold(settingsService);
        boolean isLate = checkIn != null && checkIn.isAfter(lateThreshold);
        attendance.setIsLate(isLate);
        attendance.setLateMinutes(isLate ? (int) ChronoUnit.MINUTES.between(lateThreshold, checkIn) : 0);

        LocalTime checkOut = attendance.getCheckOutTime();
        LocalTime officialEndTime = calculator.getOfficialEndTime(settingsService);
        boolean crossesMidnight = checkIn != null && checkOut != null && checkOut.isBefore(checkIn);
        boolean earlyDeparture = !crossesMidnight && checkOut != null && checkOut.isBefore(officialEndTime);
        attendance.setEarlyDeparture(earlyDeparture);
        attendance.setEarlyMinutes(earlyDeparture ? (int) ChronoUnit.MINUTES.between(checkOut, officialEndTime) : 0);
        attendance.setApprovedBy(reviewer);
        attendance.setApprovedAt(LocalDateTime.now());
        String correctionRemark = "Attendance correction approved: " + correction.getReason();
        attendance.setRemarks(attendance.getRemarks() == null || attendance.getRemarks().isBlank()
                ? correctionRemark : attendance.getRemarks() + System.lineSeparator() + correctionRemark);
        attendanceRepository.save(attendance);

        markReviewed(correction, reviewer, CorrectionStatus.APPROVED, remarks);
        auditService.logAction(reviewer, "APPROVE_ATTENDANCE_CORRECTION",
                "ATTENDANCE_CORRECTION", correction.getId(), employee.getEmployeeCode());
        return toMap(correction);
    }

    public Map<String, Object> reject(Long requestId, Long reviewerUserId, String remarks) {
        AttendanceCorrection correction = getPendingRequest(requestId, "rejected");
        User reviewer = userRepository.findById(reviewerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Reviewing user not found"));
        markReviewed(correction, reviewer, CorrectionStatus.REJECTED, remarks);
        auditService.logAction(reviewer, "REJECT_ATTENDANCE_CORRECTION",
                "ATTENDANCE_CORRECTION", correction.getId(), correction.getEmployee().getEmployeeCode());
        return toMap(correction);
    }

    private AttendanceCorrection getPendingRequest(Long requestId, String action) {
        AttendanceCorrection correction = correctionRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance correction request not found"));
        if (correction.getStatus() != CorrectionStatus.PENDING) {
            throw new BadRequestException("Only pending correction requests can be " + action);
        }
        return correction;
    }

    private void markReviewed(AttendanceCorrection correction, User reviewer,
                             CorrectionStatus status, String remarks) {
        correction.setStatus(status);
        correction.setReviewedBy(reviewer);
        correction.setReviewedAt(LocalDateTime.now());
        correction.setReviewRemarks(remarks == null || remarks.isBlank() ? null : remarks.trim());
        correctionRepository.save(correction);
    }

    private void validateTimes(LocalTime checkIn, LocalTime checkOut) {
        if (checkIn == null && checkOut == null) {
            throw new BadRequestException("Provide at least one corrected attendance time");
        }
        if (checkIn != null && checkIn.equals(checkOut)) {
            throw new BadRequestException("Check-in and check-out cannot be the same time");
        }
    }

    private long calculateWorkedMinutes(LocalTime checkIn, LocalTime checkOut) {
        long minutes = ChronoUnit.MINUTES.between(checkIn, checkOut);
        return minutes < 0 ? minutes + (24 * 60) : minutes;
    }

    private Map<String, Object> toMap(AttendanceCorrection correction) {
        Map<String, Object> result = new LinkedHashMap<>();
        EmployeeUser employee = correction.getEmployee();
        result.put("id", correction.getId());
        result.put("employeeId", employee.getId());
        result.put("employeeName", employee.getUser() != null ? employee.getUser().getFullName() : "Unknown");
        result.put("employeeCode", employee.getEmployeeCode());
        result.put("attendanceDate", correction.getAttendanceDate().toString());
        result.put("currentCheckInTime", formatTime(correction.getCurrentCheckInTime()));
        result.put("currentCheckOutTime", formatTime(correction.getCurrentCheckOutTime()));
        result.put("requestedCheckInTime", formatTime(correction.getRequestedCheckInTime()));
        result.put("requestedCheckOutTime", formatTime(correction.getRequestedCheckOutTime()));
        LocalTime calculatedCheckIn = correction.getRequestedCheckInTime() != null
                ? correction.getRequestedCheckInTime() : correction.getCurrentCheckInTime();
        LocalTime calculatedCheckOut = correction.getRequestedCheckOutTime() != null
                ? correction.getRequestedCheckOutTime() : correction.getCurrentCheckOutTime();
        result.put("calculatedCheckInTime", formatTime(calculatedCheckIn));
        result.put("calculatedCheckOutTime", formatTime(calculatedCheckOut));
        boolean requestedCrossesMidnight = calculatedCheckIn != null && calculatedCheckOut != null
                && calculatedCheckOut.isBefore(calculatedCheckIn);
        result.put("requestedCrossesMidnight", requestedCrossesMidnight);
        result.put("requestedTotalHours", calculatedCheckIn != null && calculatedCheckOut != null
                && !calculatedCheckIn.equals(calculatedCheckOut)
                ? BigDecimal.valueOf(calculateWorkedMinutes(calculatedCheckIn, calculatedCheckOut) / 60.0)
                    .setScale(2, RoundingMode.HALF_UP)
                : null);
        result.put("reason", correction.getReason());
        result.put("status", correction.getStatus().name());
        result.put("submittedAt", correction.getSubmittedAt() != null ? correction.getSubmittedAt().toString() : null);
        result.put("reviewedAt", correction.getReviewedAt() != null ? correction.getReviewedAt().toString() : null);
        result.put("reviewRemarks", correction.getReviewRemarks());
        return result;
    }

    private String formatTime(LocalTime time) {
        return time != null ? time.format(TIME_FORMATTER) : null;
    }
}