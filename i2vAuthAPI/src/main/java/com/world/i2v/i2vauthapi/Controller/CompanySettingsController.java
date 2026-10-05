package com.world.i2v.i2vauthapi.Controller;

import com.world.i2v.i2vauthapi.dto.response.ApiResponse;
import com.world.i2v.i2vauthapi.service.CompanySettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/company-settings")
@RequiredArgsConstructor
public class CompanySettingsController {

    private final CompanySettingsService companySettingsService;

    @GetMapping
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllSettings() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("lateThreshold",
                formatTimeOrDefault(companySettingsService.getLateThreshold(), LocalTime.of(10, 0)));
        settings.put("officialStartTime",
                formatTimeOrDefault(companySettingsService.getOfficialStartTime(), LocalTime.of(9, 0)));
        settings.put("officialEndTime",
                formatTimeOrDefault(companySettingsService.getOfficialEndTime(), LocalTime.of(18, 0)));
        settings.put("workingHoursPerDay", companySettingsService.getWorkingHoursPerDay());
        settings.put("defaultWeeklyOff",
                companySettingsService.getSetting("DEFAULT_WEEKLY_OFF", "SATURDAY,SUNDAY"));
        return ResponseEntity.ok(ApiResponse.success(settings));
    }

    @GetMapping("/department/{departmentId}/weekly-off")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<String>> getDepartmentWeeklyOff(@PathVariable Long departmentId) {
        String weeklyOff = String.join(",", companySettingsService.getWeeklyOffDays(departmentId));
        return ResponseEntity.ok(ApiResponse.success(weeklyOff));
    }

    private String formatTimeOrDefault(LocalTime value, LocalTime fallback) {
        return (value != null ? value : fallback).toString();
    }
}
