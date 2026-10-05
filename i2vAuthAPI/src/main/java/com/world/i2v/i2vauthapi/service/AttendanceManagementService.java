package com.world.i2v.i2vauthapi.service;

import com.world.i2v.i2vauthapi.dto.request.AttendanceRequest;
import com.world.i2v.i2vauthapi.dto.response.AttendanceResponse;
import com.world.i2v.i2vauthapi.dto.response.AttendanceStatisticsResponse;
import com.world.i2v.i2vauthapi.dto.response.AttendanceSummaryResponse;
import com.world.i2v.i2vauthapi.entity.Attendance;
import com.world.i2v.i2vauthapi.entity.Attendance.AttendanceStatus;
import com.world.i2v.i2vauthapi.entity.EmployeeUser;
import com.world.i2v.i2vauthapi.entity.Holiday;
import com.world.i2v.i2vauthapi.entity.LeaveRequest;
import com.world.i2v.i2vauthapi.exception.BadRequestException;
import com.world.i2v.i2vauthapi.exception.ResourceNotFoundException;
import com.world.i2v.i2vauthapi.repository.AttendanceRepository;
import com.world.i2v.i2vauthapi.repository.EmployeeUserRepository;
import com.world.i2v.i2vauthapi.repository.HolidayRepository;
import com.world.i2v.i2vauthapi.repository.LeaveRequestRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class AttendanceManagementService {

    private static final ZoneId ATTENDANCE_ZONE = ZoneId.of("Asia/Kolkata");

    private final AttendanceRepository attendanceRepository;
    private final EmployeeUserRepository employeeRepository;
    private final AuditService auditService;
    private final CompanySettingsService settingsService;
    private final AttendanceCalculator calculator;
    private final HolidayRepository holidayRepository;
    private final LeaveRequestRepository leaveRequestRepository;

    public AttendanceManagementService(AttendanceRepository attendanceRepository,
                                        EmployeeUserRepository employeeRepository,
                                        AuditService auditService,
                                        CompanySettingsService settingsService,
                                        AttendanceCalculator calculator,
                                        HolidayRepository holidayRepository,
                                        LeaveRequestRepository leaveRequestRepository) {
        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
        this.auditService = auditService;
        this.settingsService = settingsService;
        this.calculator = calculator;
        this.holidayRepository = holidayRepository;
        this.leaveRequestRepository = leaveRequestRepository;
    }

    @Transactional(readOnly = true)
    public Page<AttendanceResponse> getEmployeeAttendancePageable(Long employeeId, LocalDate startDate,
                                                                     LocalDate endDate, Pageable pageable) {
        EmployeeUser employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        return attendanceRepository.findByEmployeeIdAndAttendanceDateBetweenPageable(
                employeeId, startDate, endDate, pageable).map(a ->
                AttendanceResponse.fromEntity(a, employee));
    }

    @Transactional(readOnly = true)
    public AttendanceResponse getEmployeeTodayAttendance(Long employeeId) {
        return attendanceRepository.findByEmployeeIdAndAttendanceDate(employeeId, LocalDate.now(ATTENDANCE_ZONE))
                .map(a -> {
                    EmployeeUser employee = employeeRepository.findById(employeeId).orElse(null);
                    return AttendanceResponse.fromEntity(a, employee);
                })
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getEmployeeAttendanceSummary(Long employeeId, int year, int month) {
        EmployeeUser employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

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

        // Holidays falling within the month (excluding weekly offs, since they were never working days)
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

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> getAllEmployeesWithAttendancePageable(int year, int month, Pageable pageable) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        Page<EmployeeUser> employeePage = employeeRepository.findAll(pageable);
        List<Map<String, Object>> result = new ArrayList<>();

        for (EmployeeUser employee : employeePage.getContent()) {
            List<Attendance> attendances = attendanceRepository.findByEmployeeIdAndAttendanceDateBetween(
                    employee.getId(), startDate, endDate);

            long presentDays = calculator.countPresentDays(attendances);
            long lateDays = calculator.countLateDays(attendances);
            BigDecimal totalHours = calculator.calculateTotalHours(attendances);
            int totalWorkingDays = calculator.countWorkingDays(employee, startDate, endDate, settingsService);
            double attendancePercentage = calculator.calculateAttendanceRate(presentDays, totalWorkingDays);

            Map<String, Object> employeeData = new HashMap<>();
            employeeData.put("id", employee.getId());
            employeeData.put("employeeCode", employee.getEmployeeCode());
            employeeData.put("name", employee.getUser() != null ? employee.getUser().getFullName() : "Unknown");
            employeeData.put("email", employee.getUser() != null ? employee.getUser().getEmail() : "Unknown");
            employeeData.put("department", employee.getDepartment() != null ? employee.getDepartment().getName() : "Unknown");
            employeeData.put("designation", employee.getDesignation() != null ? employee.getDesignation().getTitle() : "Unknown");
            employeeData.put("presentDays", presentDays);
            employeeData.put("lateDays", lateDays);
            employeeData.put("totalHours", totalHours);
            employeeData.put("attendancePercentage", Math.round(attendancePercentage * 100.0) / 100.0);
            employeeData.put("totalWorkingDays", totalWorkingDays);
            result.add(employeeData);
        }

        return new PageImpl<>(result, pageable, employeePage.getTotalElements());
    }

    public AttendanceResponse markAttendance(AttendanceRequest request) {
        EmployeeUser employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        if (attendanceRepository.existsByEmployeeIdAndAttendanceDate(
                request.getEmployeeId(), request.getAttendanceDate())) {
            throw new BadRequestException("Attendance already marked for this date");
        }

        Attendance attendance = new Attendance();
        attendance.setEmployee(employee);
        attendance.setAttendanceDate(request.getAttendanceDate());
        attendance.setCheckInTime(request.getCheckInTime());
        attendance.setCheckOutTime(request.getCheckOutTime());
        attendance.setStatus(request.getStatus());
        attendance.setRemarks(request.getRemarks());

        if (request.getCheckInTime() != null && request.getCheckOutTime() != null) {
            long minutes = java.time.temporal.ChronoUnit.MINUTES.between(
                    request.getCheckInTime(), request.getCheckOutTime());
            attendance.setTotalHours(BigDecimal.valueOf(minutes / 60.0).setScale(2, java.math.RoundingMode.HALF_UP));
        }

        LocalTime lateThreshold = calculator.getLateThreshold(settingsService);
        if (request.getCheckInTime() != null && request.getCheckInTime().isAfter(lateThreshold)) {
            attendance.setIsLate(true);
            long lateMinutes = java.time.temporal.ChronoUnit.MINUTES.between(lateThreshold, request.getCheckInTime());
            attendance.setLateMinutes((int) lateMinutes);
        }

        Attendance saved = attendanceRepository.save(attendance);
        auditService.logAction(null, "MARK_ATTENDANCE", "ATTENDANCE", saved.getId(),
                employee.getEmployeeCode() + " - " + request.getAttendanceDate());

        return AttendanceResponse.fromEntity(saved, employee);
    }

    public AttendanceResponse updateAttendance(Long attendanceId, AttendanceRequest request) {
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance not found"));

        attendance.setCheckInTime(request.getCheckInTime());
        attendance.setCheckOutTime(request.getCheckOutTime());
        attendance.setStatus(request.getStatus());
        attendance.setRemarks(request.getRemarks());

        if (request.getCheckInTime() != null && request.getCheckOutTime() != null) {
            long minutes = java.time.temporal.ChronoUnit.MINUTES.between(
                    request.getCheckInTime(), request.getCheckOutTime());
            attendance.setTotalHours(BigDecimal.valueOf(minutes / 60.0).setScale(2, java.math.RoundingMode.HALF_UP));
        }

        LocalTime lateThreshold = calculator.getLateThreshold(settingsService);
        if (request.getCheckInTime() != null && request.getCheckInTime().isAfter(lateThreshold)) {
            attendance.setIsLate(true);
            long lateMinutes = java.time.temporal.ChronoUnit.MINUTES.between(lateThreshold, request.getCheckInTime());
            attendance.setLateMinutes((int) lateMinutes);
        } else {
            attendance.setIsLate(false);
            attendance.setLateMinutes(0);
        }

        Attendance saved = attendanceRepository.save(attendance);
        EmployeeUser employee = attendance.getEmployee();
        auditService.logAction(null, "UPDATE_ATTENDANCE", "ATTENDANCE", saved.getId(), "Updated attendance");
        return AttendanceResponse.fromEntity(saved, employee);
    }

    public void deleteAttendance(Long attendanceId) {
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance not found"));
        attendanceRepository.delete(attendance);
        auditService.logAction(null, "DELETE_ATTENDANCE", "ATTENDANCE", attendanceId, "Deleted attendance");
    }

    @Transactional(readOnly = true)
    public AttendanceStatisticsResponse getCompanyAttendanceStatistics(int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<EmployeeUser> allEmployees = employeeRepository.findAll();
        int totalEmployees = allEmployees.size();

        int totalPresent = 0;
        int totalLate = 0;
        BigDecimal totalHours = BigDecimal.ZERO;
        int totalWorkingDaysSum = 0;

        for (EmployeeUser employee : allEmployees) {
            List<Attendance> attendances = attendanceRepository.findByEmployeeIdAndAttendanceDateBetween(
                    employee.getId(), startDate, endDate);

            totalPresent += Math.toIntExact(calculator.countPresentDays(attendances));
            totalLate += Math.toIntExact(calculator.countLateDays(attendances));
            totalHours = totalHours.add(calculator.calculateTotalHours(attendances));
            totalWorkingDaysSum += calculator.countWorkingDays(employee, startDate, endDate, settingsService);
        }

        double overallAttendanceRate = calculator.calculateAttendanceRate(totalPresent, totalWorkingDaysSum);

        return AttendanceStatisticsResponse.builder()
                .year(year)
                .month(month)
                .totalEmployees(totalEmployees)
                .totalWorkingDays(totalWorkingDaysSum)
                .totalPresent(totalPresent)
                .totalLate(totalLate)
                .totalHours(totalHours)
                .overallAttendanceRate(Math.round(overallAttendanceRate * 100.0) / 100.0)
                .build();
    }

    public Map<String, Object> bulkMarkAttendance(LocalDate date, List<Long> employeeIds, String status) {
        Map<String, Object> result = new HashMap<>();
        int successCount = 0;
        int failedCount = 0;
        List<String> errors = new ArrayList<>();

        LocalTime startTime = calculator.getOfficialStartTime(settingsService);
        LocalTime endTime = calculator.getOfficialEndTime(settingsService);
        Integer workingHours = calculator.getWorkingHoursPerDay(settingsService);

        for (Long employeeId : employeeIds) {
            try {
                EmployeeUser employee = employeeRepository.findById(employeeId)
                        .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));

                if (attendanceRepository.existsByEmployeeIdAndAttendanceDate(employeeId, date)) {
                    errors.add("Attendance already exists for employee: " + employee.getEmployeeCode());
                    failedCount++;
                    continue;
                }

                Attendance attendance = new Attendance();
                attendance.setEmployee(employee);
                attendance.setAttendanceDate(date);
                attendance.setStatus(AttendanceStatus.valueOf(status.toUpperCase()));

                if ("PRESENT".equalsIgnoreCase(status) || "HALF_DAY".equalsIgnoreCase(status)) {
                    attendance.setCheckInTime(startTime);
                    attendance.setCheckOutTime(endTime);
                    attendance.setTotalHours(BigDecimal.valueOf(workingHours));
                }

                attendanceRepository.save(attendance);
                successCount++;
            } catch (Exception e) {
                errors.add("Failed for employee ID " + employeeId + ": " + e.getMessage());
                failedCount++;
            }
        }

        result.put("successCount", successCount);
        result.put("failedCount", failedCount);
        result.put("errors", errors);
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPendingAttendance(LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> pending = new ArrayList<>();
        List<EmployeeUser> allEmployees = employeeRepository.findAll();

        for (EmployeeUser employee : allEmployees) {
            LocalDate date = startDate;
            while (!date.isAfter(endDate)) {
                boolean hasAttendance = attendanceRepository.existsByEmployeeIdAndAttendanceDate(employee.getId(), date);
                if (!hasAttendance) {
                    Map<String, Object> missing = new HashMap<>();
                    missing.put("employeeId", employee.getId());
                    missing.put("employeeName", employee.getUser() != null ? employee.getUser().getFullName() : "Unknown");
                    missing.put("employeeCode", employee.getEmployeeCode());
                    missing.put("date", date.toString());
                    pending.add(missing);
                }
                date = date.plusDays(1);
            }
        }

        return pending;
    }

    public ResponseEntity<byte[]> exportAttendanceReport(int year, int month, Long departmentId) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<EmployeeUser> employees;
        if (departmentId != null) {
            employees = employeeRepository.findByDepartmentId(departmentId);
        } else {
            employees = employeeRepository.findAll();
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(out);

        writer.println("Employee Code,Employee Name,Department,Designation,Date,Check In,Check Out,Total Hours,Status,Late Minutes");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

        for (EmployeeUser employee : employees) {
            List<Attendance> attendances = attendanceRepository.findByEmployeeIdAndAttendanceDateBetween(
                    employee.getId(), startDate, endDate);

            for (Attendance attendance : attendances) {
                writer.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                        employee.getEmployeeCode(),
                        employee.getUser() != null ? employee.getUser().getFullName() : "Unknown",
                        employee.getDepartment() != null ? employee.getDepartment().getName() : "Unknown",
                        employee.getDesignation() != null ? employee.getDesignation().getTitle() : "Unknown",
                        attendance.getAttendanceDate().format(dateFormatter),
                        attendance.getCheckInTime() != null ? attendance.getCheckInTime().toString() : "",
                        attendance.getCheckOutTime() != null ? attendance.getCheckOutTime().toString() : "",
                        attendance.getTotalHours() != null ? attendance.getTotalHours() : "",
                        attendance.getStatus() != null ? attendance.getStatus().name() : "",
                        attendance.getLateMinutes() != null ? attendance.getLateMinutes() : 0);
            }
        }

        writer.flush();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "attendance_report_" + year + "_" + month + ".csv");
        return ResponseEntity.ok().headers(headers).body(out.toByteArray());
    }
}
