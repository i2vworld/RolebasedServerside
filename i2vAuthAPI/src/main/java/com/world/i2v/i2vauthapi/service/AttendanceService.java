package com.world.i2v.i2vauthapi.service;

import com.world.i2v.i2vauthapi.dto.response.AttendanceResponse;
import com.world.i2v.i2vauthapi.entity.Attendance;
import com.world.i2v.i2vauthapi.entity.Attendance.AttendanceStatus;
import com.world.i2v.i2vauthapi.entity.EmployeeUser;
import com.world.i2v.i2vauthapi.entity.Holiday;
import com.world.i2v.i2vauthapi.entity.LeaveRequest;
import com.world.i2v.i2vauthapi.entity.User;
import com.world.i2v.i2vauthapi.exception.BadRequestException;
import com.world.i2v.i2vauthapi.repository.AttendanceRepository;
import com.world.i2v.i2vauthapi.repository.EmployeeUserRepository;
import com.world.i2v.i2vauthapi.repository.HolidayRepository;
import com.world.i2v.i2vauthapi.repository.LeaveRequestRepository;
import com.world.i2v.i2vauthapi.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class AttendanceService {

    private static final ZoneId ATTENDANCE_ZONE = ZoneId.of("Asia/Kolkata");

    private final AttendanceRepository attendanceRepository;
    private final EmployeeUserRepository employeeRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final CompanySettingsService settingsService;
    private final AttendanceCalculator calculator;
    private final HolidayRepository holidayRepository;
    private final LeaveRequestRepository leaveRequestRepository;

    public AttendanceService(AttendanceRepository attendanceRepository,
                              EmployeeUserRepository employeeRepository,
                              UserRepository userRepository,
                              AuditService auditService,
                              CompanySettingsService settingsService,
                              AttendanceCalculator calculator,
                              HolidayRepository holidayRepository,
                              LeaveRequestRepository leaveRequestRepository) {
        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.settingsService = settingsService;
        this.calculator = calculator;
        this.holidayRepository = holidayRepository;
        this.leaveRequestRepository = leaveRequestRepository;
    }

    private String sanitizeLocation(String location) {
        if (location == null) {
            return null;
        }

        String cleaned = location.trim();
        if (cleaned.isEmpty()) {
            return null;
        }

        String lower = cleaned.toLowerCase();
        if (lower.contains("permission denied") || lower.contains("not available") || lower.contains("denied")) {
            return null;
        }

        return cleaned;
    }

    public Attendance checkIn(Long userId, String location, String ipAddress) {
        log.info("Check-in request for user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));

        EmployeeUser employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Employee profile not found. Please contact HR."));

        LocalDate today = LocalDate.now(ATTENDANCE_ZONE);
        LocalTime now = LocalTime.now(ATTENDANCE_ZONE);

        if (employee.getDepartment() != null) {
            Boolean isWeeklyOff = settingsService.isWeeklyOff(employee.getDepartment().getId(), today);
            if (Boolean.TRUE.equals(isWeeklyOff)) {
                throw new BadRequestException("Today is a weekly off for your department. Cannot check in.");
            }
        }

        Attendance existing = attendanceRepository
                .findByEmployeeIdAndAttendanceDate(employee.getId(), today)
                .orElse(null);

        if (existing != null && existing.getCheckInTime() != null) {
            throw new BadRequestException("Already checked in today at " + existing.getCheckInTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        }

        Attendance attendance = existing != null ? existing : new Attendance();
        attendance.setEmployee(employee);
        attendance.setAttendanceDate(today);
        attendance.setCheckInTime(now);
        attendance.setCheckInLocation(sanitizeLocation(location));
        attendance.setCheckInIp(ipAddress);
        attendance.setStatus(AttendanceStatus.PRESENT);

        LocalTime lateThreshold = calculator.getLateThreshold(settingsService);
        if (now.isAfter(lateThreshold)) {
            attendance.setIsLate(true);
            long lateMinutes = ChronoUnit.MINUTES.between(lateThreshold, now);
            attendance.setLateMinutes((int) lateMinutes);
            log.info("Late check-in: {} minutes late", lateMinutes);
        } else {
            attendance.setIsLate(false);
            attendance.setLateMinutes(0);
        }

        Attendance saved = attendanceRepository.save(attendance);
        auditService.logAction(user, "CHECK_IN", "ATTENDANCE", saved.getId(), "Checked in at " + now);
        return saved;
    }

    public Attendance checkOut(Long userId, String location, String ipAddress) {
        log.info("Check-out request for user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));

        EmployeeUser employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Employee profile not found"));

        LocalDate today = LocalDate.now(ATTENDANCE_ZONE);
        LocalTime now = LocalTime.now(ATTENDANCE_ZONE);

        Attendance attendance = attendanceRepository
                .findByEmployeeIdAndAttendanceDate(employee.getId(), today)
                .orElseThrow(() -> new BadRequestException("No check-in record found for today. Please check in first."));

        if (attendance.getCheckOutTime() != null) {
            throw new BadRequestException("Already checked out today at " + attendance.getCheckOutTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        }

        if (attendance.getCheckInTime() == null) {
            throw new BadRequestException("Please check in first before checking out");
        }

        attendance.setCheckOutTime(now);
        attendance.setCheckOutLocation(sanitizeLocation(location));
        attendance.setCheckOutIp(ipAddress);

        long minutesWorked = ChronoUnit.MINUTES.between(attendance.getCheckInTime(), now);
        attendance.setTotalHours(BigDecimal.valueOf(minutesWorked / 60.0).setScale(2, RoundingMode.HALF_UP));

        LocalTime officialEndTime = calculator.getOfficialEndTime(settingsService);
        if (now.isBefore(officialEndTime)) {
            attendance.setEarlyDeparture(true);
            long earlyMinutes = ChronoUnit.MINUTES.between(now, officialEndTime);
            attendance.setEarlyMinutes((int) earlyMinutes);
            log.info("Early departure: {} minutes early", earlyMinutes);
        } else {
            attendance.setEarlyDeparture(false);
            attendance.setEarlyMinutes(0);
        }

        Attendance saved = attendanceRepository.save(attendance);
        auditService.logAction(user, "CHECK_OUT", "ATTENDANCE", saved.getId(),
                "Checked out at " + now + " | Total hours: " + attendance.getTotalHours());
        return saved;
    }

    @Transactional(readOnly = true)
    public Attendance getTodayAttendance(Long userId) {
        EmployeeUser employee = employeeRepository.findByUserId(userId).orElse(null);
        if (employee == null) {
            return null;
        }
        return attendanceRepository.findByEmployeeIdAndAttendanceDate(employee.getId(), LocalDate.now(ATTENDANCE_ZONE)).orElse(null);
    }

    @Transactional(readOnly = true)
    public Page<Attendance> getAttendanceHistoryPageable(Long userId, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        EmployeeUser employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Employee record not found for user ID: " + userId));
        return attendanceRepository.findByEmployeeIdAndAttendanceDateBetweenPageable(
                employee.getId(), startDate, endDate, pageable);
    }

    @Transactional(readOnly = true)
    public List<Attendance> getAttendanceHistory(Long userId, LocalDate startDate, LocalDate endDate) {
        EmployeeUser employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Employee record not found for user ID: " + userId));
        return attendanceRepository.findByEmployeeIdAndAttendanceDateBetween(
                employee.getId(), startDate, endDate);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAttendanceSummary(Long userId, int year, int month) {
        EmployeeUser employee = employeeRepository.findByUserId(userId).orElse(null);
        if (employee == null) {
            return Map.of();
        }

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<Attendance> attendances = attendanceRepository.findByEmployeeIdAndAttendanceDateBetween(
                employee.getId(), startDate, endDate);

        int totalWorkingDays = calculator.countWorkingDays(employee, startDate, endDate, settingsService);

        Long departmentId = employee.getDepartment() != null ? employee.getDepartment().getId() : null;

        // Attendance record dates (present/worked days take precedence over leave/holiday)
        Set<LocalDate> attendanceDates = attendances.stream()
                .map(Attendance::getAttendanceDate)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Holidays falling within the month (excluding weekly offs)
        Set<LocalDate> holidayDates = holidayRepository.findByYear(year).stream()
                .map(Holiday::getHolidayDate)
                .filter(Objects::nonNull)
                .filter(d -> !d.isBefore(startDate) && !d.isAfter(endDate))
                .filter(d -> !settingsService.isWeeklyOff(departmentId, d))
                .collect(Collectors.toSet());

        // Approved leave days falling within the month (excluding holidays, weekly offs and days with attendance)
        List<LeaveRequest> approvedLeaves = leaveRequestRepository.findByEmployeeIdAndStatus(
                employee.getId(), LeaveRequest.LeaveStatus.APPROVED);
        Set<LocalDate> leaveDates = new HashSet<>();
        for (LeaveRequest leave : approvedLeaves) {
            if (leave.getStartDate() == null || leave.getEndDate() == null) continue;
            LocalDate d = leave.getStartDate().isBefore(startDate) ? startDate : leave.getStartDate();
            LocalDate end = leave.getEndDate().isAfter(endDate) ? endDate : leave.getEndDate();
            while (!d.isAfter(end)) {
                if (!attendanceDates.contains(d) && !holidayDates.contains(d)
                        && !settingsService.isWeeklyOff(departmentId, d)) {
                    leaveDates.add(d);
                }
                d = d.plusDays(1);
            }
        }

        int effectiveWorkingDays = Math.max(totalWorkingDays - holidayDates.size() - leaveDates.size(), 0);

        Map<String, Object> summary = calculator.buildSummaryMap(
                employee, year, month, attendances, effectiveWorkingDays, settingsService);
        summary.put("leaveDays", (long) leaveDates.size());
        summary.put("holidayDays", (long) holidayDates.size());
        return summary;
    }
}
