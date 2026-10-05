package com.world.i2v.i2vauthapi.Controller;

import com.world.i2v.i2vauthapi.dto.response.ApiResponse;
import com.world.i2v.i2vauthapi.entity.CompanySettings;
import com.world.i2v.i2vauthapi.entity.Department;
import com.world.i2v.i2vauthapi.entity.EmployeeUser;
import com.world.i2v.i2vauthapi.repository.CompanySettingsRepository;
import com.world.i2v.i2vauthapi.repository.DepartmentRepository;
import com.world.i2v.i2vauthapi.repository.EmployeeUserRepository;
import com.world.i2v.i2vauthapi.security.UserDetailsImpl;
import com.world.i2v.i2vauthapi.service.CompanySettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final CompanySettingsService settingsService;
    private final CompanySettingsRepository settingsRepository;
    private final EmployeeUserRepository employeeRepository;
    private final DepartmentRepository departmentRepository;

    @GetMapping
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<Map<String, String>>> getAllSettings() {
        try {
            Map<String, String> result = new HashMap<>();
            settingsRepository.findAll().stream()
                    .filter(setting -> Boolean.TRUE.equals(setting.getIsActive()))
                    .forEach(setting -> result.put(setting.getSettingKey(), setting.getSettingValue()));
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("Error getting all settings: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.success(new HashMap<>()));
        }
    }

    @GetMapping("/{key}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<String>> getSetting(@PathVariable String key) {
        try {
            return ResponseEntity.ok(ApiResponse.success(settingsService.getSetting(key)));
        } catch (Exception e) {
            log.error("Error getting setting {}: {}", key, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.success(null));
        }
    }

    @PutMapping("/{key}")
    @PreAuthorize("hasRole('EMPLOYEE') and @securityUtils.canManageAttendance()")
    public ResponseEntity<ApiResponse<CompanySettings>> updateSetting(
            @PathVariable String key,
            @RequestBody Map<String, String> body) {
        String value = body.get("value");
        if (value == null || value.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Setting value is required"));
        }

        try {
            CompanySettings saved = settingsService.saveSetting(key, value, body.get("description"));
            return ResponseEntity.ok(ApiResponse.success("Setting updated successfully", saved));
        } catch (Exception e) {
            log.error("Error updating setting {}: {}", key, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to update setting: " + e.getMessage()));
        }
    }

    @PostMapping("/clear-cache")
    @PreAuthorize("hasRole('EMPLOYEE') and @securityUtils.canManageAttendance()")
    public ResponseEntity<ApiResponse<Void>> clearCache() {
        settingsService.clearCache();
        return ResponseEntity.ok(ApiResponse.success("Cache cleared successfully", null));
    }

    @GetMapping("/my-weekly-off")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<String>>> getMyWeeklyOff(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        try {
            EmployeeUser employee = employeeRepository.findByUserId(currentUser.getId()).orElse(null);
            List<String> weeklyOff = null;

            if (employee != null && employee.getDepartment() != null) {
                weeklyOff = settingsService.getWeeklyOffDays(employee.getDepartment().getId());
            }

            if (weeklyOff == null) {
                String defaultOff = settingsService.getSetting("DEFAULT_WEEKLY_OFF");
                if (defaultOff != null) {
                    weeklyOff = Arrays.asList(defaultOff.split(","));
                }
            }

            return ResponseEntity.ok(ApiResponse.success(weeklyOff));
        } catch (Exception e) {
            log.error("Error getting my weekly off: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.success(null));
        }
    }

    @GetMapping("/department/{departmentId}/weekly-off")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<String>>> getDepartmentWeeklyOff(
            @PathVariable Long departmentId) {
        try {
            return ResponseEntity.ok(ApiResponse.success(settingsService.getWeeklyOffDays(departmentId)));
        } catch (Exception e) {
            log.error("Error getting department weekly off: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.success(null));
        }
    }

    @GetMapping("/department-weekly-off/all")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllDepartmentWeeklyOff() {
        try {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Department dept : departmentRepository.findAll()) {
                Map<String, Object> item = new HashMap<>();
                item.put("departmentId", dept.getId());
                item.put("departmentName", dept.getName());

                List<String> weeklyOff = settingsService.getWeeklyOffDays(dept.getId());
                item.put("weeklyOffDays", weeklyOff != null ? String.join(",", weeklyOff) : null);

                result.add(item);
            }
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("Error getting department weekly off: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.success(new ArrayList<>()));
        }
    }

    @PutMapping("/department/{departmentId}/weekly-off")
    @PreAuthorize("hasRole('EMPLOYEE') and @securityUtils.canManageAttendance()")
    public ResponseEntity<ApiResponse<Void>> updateDepartmentWeeklyOff(
            @PathVariable Long departmentId,
            @RequestBody Map<String, String> body) {
        String weeklyOffDays = body.get("weeklyOffDays");
        if (weeklyOffDays == null || weeklyOffDays.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("weeklyOffDays is required"));
        }

        settingsService.updateDepartmentWeeklyOff(departmentId, weeklyOffDays);
        return ResponseEntity.ok(ApiResponse.success("Department weekly off updated", null));
    }

    @GetMapping("/is-weekly-off")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<Boolean>> isWeeklyOff(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @RequestParam String date) {
        try {
            EmployeeUser employee = employeeRepository.findByUserId(currentUser.getId()).orElse(null);
            if (employee == null || employee.getDepartment() == null) {
                return ResponseEntity.ok(ApiResponse.success(false));
            }
            boolean result = settingsService.isWeeklyOff(employee.getDepartment().getId(), LocalDate.parse(date));
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("Error checking weekly off: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.success(false));
        }
    }

    @GetMapping("/late-threshold")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<String>> getLateThreshold() {
        try {
            LocalTime value = settingsService.getLateThreshold();
            return ResponseEntity.ok(ApiResponse.success(value != null ? value.toString() : null));
        } catch (Exception e) {
            log.error("Error getting late threshold: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.success(null));
        }
    }

    @GetMapping("/official-end-time")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<String>> getOfficialEndTime() {
        try {
            LocalTime value = settingsService.getOfficialEndTime();
            return ResponseEntity.ok(ApiResponse.success(value != null ? value.toString() : null));
        } catch (Exception e) {
            log.error("Error getting official end time: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.success(null));
        }
    }
}
