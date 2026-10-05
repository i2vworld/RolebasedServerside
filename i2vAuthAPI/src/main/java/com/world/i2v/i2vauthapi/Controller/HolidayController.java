package com.world.i2v.i2vauthapi.Controller;

import com.world.i2v.i2vauthapi.dto.response.ApiResponse;
import com.world.i2v.i2vauthapi.entity.Holiday;
import com.world.i2v.i2vauthapi.service.HolidayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/holidays")
@RequiredArgsConstructor
public class HolidayController {

    private final HolidayService holidayService;

    @GetMapping
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<Holiday>>> getHolidays(
            @RequestParam(required = false) Integer year) {
        try {
            List<Holiday> holidays = year != null
                    ? holidayService.getHolidaysByYear(year)
                    : holidayService.getAllActiveHolidays();
            return ResponseEntity.ok(ApiResponse.success(holidays));
        } catch (Exception e) {
            log.error("Error getting holidays: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to load holidays: " + e.getMessage()));
        }
    }

    @GetMapping("/check")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<Boolean>> isHoliday(@RequestParam String date) {
        try {
            return ResponseEntity.ok(ApiResponse.success(holidayService.isHoliday(LocalDate.parse(date))));
        } catch (Exception e) {
            log.error("Error checking holiday: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.success(false));
        }
    }

    @GetMapping("/info")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<Holiday>> getHolidayInfo(@RequestParam String date) {
        try {
            Optional<Holiday> holiday = holidayService.getHolidayByDate(LocalDate.parse(date));
            return ResponseEntity.ok(ApiResponse.success(holiday.orElse(null)));
        } catch (Exception e) {
            log.error("Error getting holiday info: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.success(null));
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('EMPLOYEE') and @securityUtils.canManageAttendance()")
    public ResponseEntity<ApiResponse<Holiday>> createHoliday(@RequestBody Holiday holiday) {
        if (holiday.getHolidayName() == null || holiday.getHolidayName().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Holiday name is required"));
        }
        if (holiday.getHolidayDate() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Holiday date is required"));
        }

        try {
            Holiday saved = holidayService.saveHoliday(holiday);
            log.info("Holiday created successfully: ID={}", saved.getId());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Holiday created successfully", saved));
        } catch (Exception e) {
            log.error("Error creating holiday: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to create holiday: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('EMPLOYEE') and @securityUtils.canManageAttendance()")
    public ResponseEntity<ApiResponse<Holiday>> updateHoliday(
            @PathVariable Long id,
            @RequestBody Holiday holiday) {
        try {
            holiday.setId(id);
            Holiday saved = holidayService.saveHoliday(holiday);
            log.info("Holiday updated successfully: ID={}", saved.getId());
            return ResponseEntity.ok(ApiResponse.success("Holiday updated successfully", saved));
        } catch (Exception e) {
            log.error("Error updating holiday: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to update holiday: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('EMPLOYEE') and @securityUtils.canManageAttendance()")
    public ResponseEntity<ApiResponse<Void>> deleteHoliday(@PathVariable Long id) {
        try {
            holidayService.deleteHoliday(id);
            return ResponseEntity.ok(ApiResponse.success("Holiday deleted successfully", null));
        } catch (Exception e) {
            log.error("Error deleting holiday: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete holiday: " + e.getMessage()));
        }
    }
}
