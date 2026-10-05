package com.world.i2v.i2vauthapi.service;

import com.world.i2v.i2vauthapi.entity.CompanySettings;
import com.world.i2v.i2vauthapi.entity.Department;
import com.world.i2v.i2vauthapi.entity.DepartmentWeeklyOff;
import com.world.i2v.i2vauthapi.exception.ResourceNotFoundException;
import com.world.i2v.i2vauthapi.repository.CompanySettingsRepository;
import com.world.i2v.i2vauthapi.repository.DepartmentRepository;
import com.world.i2v.i2vauthapi.repository.DepartmentWeeklyOffRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class CompanySettingsService {

    private final CompanySettingsRepository settingsRepository;
    private final DepartmentWeeklyOffRepository weeklyOffRepository;
    private final DepartmentRepository departmentRepository;

    public CompanySettingsService(CompanySettingsRepository settingsRepository,
                                  DepartmentWeeklyOffRepository weeklyOffRepository,
                                  DepartmentRepository departmentRepository) {
        this.settingsRepository = settingsRepository;
        this.weeklyOffRepository = weeklyOffRepository;
        this.departmentRepository = departmentRepository;
    }

    /**
     * Get setting value by key - returns null if not found
     */
    @Cacheable(value = "companySettings", key = "#settingKey")
    public String getSetting(String settingKey) {
        log.debug("Fetching setting: {} from database", settingKey);
        return settingsRepository.findBySettingKeyAndIsActive(settingKey, true)
                .map(CompanySettings::getSettingValue)
                .orElse(null);
    }

    /**
     * Get setting with default fallback - for backward compatibility
     */
    public String getSetting(String settingKey, String defaultValue) {
        String value = getSetting(settingKey);
        return value != null ? value : defaultValue;
    }

    private LocalTime parseSafeTime(String key, String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }

        if (!normalized.matches("^\\d{1,2}:\\d{2}(?::\\d{2}(?:\\.\\d{1,9})?)?$")) {
            throw new IllegalArgumentException("Invalid time format for " + key + ": '" + value + "'. Expected format: HH:mm[:ss[.nanoseconds]]");
        }

        try {
            return LocalTime.parse(normalized);
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("Invalid time format for " + key + ": '" + value + "'. Expected format: HH:mm[:ss[.nanoseconds]]", e);
        }
    }

    public LocalTime getLateThreshold() {
        String value = getSetting("LATE_THRESHOLD");
        if (value == null) {
            log.warn("LATE_THRESHOLD not configured in database");
            return null;
        }
        try {
            return parseSafeTime("LATE_THRESHOLD", value);
        } catch (IllegalArgumentException e) {
            log.error("{} Returning null.", e.getMessage());
            return null;
        }
    }

    public LocalTime getOfficialStartTime() {
        String value = getSetting("OFFICIAL_START_TIME");
        if (value == null) {
            log.warn("OFFICIAL_START_TIME not configured in database");
            return null;
        }
        try {
            return parseSafeTime("OFFICIAL_START_TIME", value);
        } catch (IllegalArgumentException e) {
            log.error("{} Returning null.", e.getMessage());
            return null;
        }
    }

    public LocalTime getOfficialEndTime() {
        String value = getSetting("OFFICIAL_END_TIME");
        if (value == null) {
            log.warn("OFFICIAL_END_TIME not configured in database");
            return null;
        }
        try {
            return parseSafeTime("OFFICIAL_END_TIME", value);
        } catch (IllegalArgumentException e) {
            log.error("{} Returning null.", e.getMessage());
            return null;
        }
    }

    public Integer getWorkingHoursPerDay() {
        String value = getSetting("WORKING_HOURS_PER_DAY");
        if (value == null) {
            log.warn("WORKING_HOURS_PER_DAY not configured in database");
            return null;
        }
        return Integer.parseInt(value);
    }

    public List<String> getWeeklyOffDays(Long departmentId) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));

        return weeklyOffRepository.findActiveByDepartmentAndDate(departmentId, today)
                .map(dwo -> Arrays.asList(dwo.getWeeklyOffDays().split(",")))
                .orElseGet(() -> {
                    String defaultOff = getSetting("DEFAULT_WEEKLY_OFF");
                    if (defaultOff == null) {
                        log.warn("No weekly off configured for department {} and no default", departmentId);
                        return null;
                    }
                    return Arrays.asList(defaultOff.split(","));
                });
    }

    public boolean isWeeklyOff(Long departmentId, LocalDate date) {
        List<String> weeklyOffDays = getWeeklyOffDays(departmentId);
        if (weeklyOffDays == null) {
            return false;
        }
        String dayOfWeek = date.getDayOfWeek().name();
        return weeklyOffDays.contains(dayOfWeek);
    }

    public Integer getWorkingDays(Long departmentId, LocalDate startDate, LocalDate endDate) {
        List<String> weeklyOffDays = getWeeklyOffDays(departmentId);
        if (weeklyOffDays == null) {
            log.warn("Cannot calculate working days - no weekly off configured");
            return null;
        }

        int workingDays = 0;
        LocalDate date = startDate;

        while (!date.isAfter(endDate)) {
            String dayOfWeek = date.getDayOfWeek().name();
            if (!weeklyOffDays.contains(dayOfWeek)) {
                workingDays++;
            }
            date = date.plusDays(1);
        }
        return workingDays;
    }

    public void updateDepartmentWeeklyOff(Long departmentId, String weeklyOffDays) {
        DepartmentWeeklyOff existing = weeklyOffRepository
                .findByDepartmentIdAndIsActive(departmentId, true)
                .orElse(null);

        if (existing != null) {
            existing.setWeeklyOffDays(weeklyOffDays);
            weeklyOffRepository.save(existing);
        } else {
            Department department = departmentRepository.findById(departmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
            DepartmentWeeklyOff newOff = new DepartmentWeeklyOff();
            newOff.setDepartment(department);
            newOff.setWeeklyOffDays(weeklyOffDays);
            newOff.setEffectiveFrom(LocalDate.now());
            newOff.setIsActive(true);
            weeklyOffRepository.save(newOff);
        }

        clearCache();
    }

    @CacheEvict(value = "companySettings", allEntries = true)
    public void clearCache() {
        log.info("✅ Company settings cache CLEARED");
    }

    public CompanySettings saveSetting(String key, String value, String description) {
        log.info("Saving setting: {} = {}", key, value);
        validateTimeSetting(key, value);

        CompanySettings setting = settingsRepository.findBySettingKey(key)
                .orElse(new CompanySettings());
        setting.setSettingKey(key);
        setting.setSettingValue(value);
        if (description != null) {
            setting.setDescription(description);
        }
        setting.setIsActive(true);

        CompanySettings saved = settingsRepository.save(setting);
        clearCache();
        log.info("✅ Setting saved and cache cleared: {} = {}", key, value);

        return saved;
    }

    private void validateTimeSetting(String key, String value) {
        if (value == null || value.isEmpty()) return;
        if ("LATE_THRESHOLD".equals(key) || "OFFICIAL_START_TIME".equals(key) || "OFFICIAL_END_TIME".equals(key)) {
            parseSafeTime(key, value);
        }
    }
}