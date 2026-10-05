package com.world.i2v.i2vauthapi.service;

import com.world.i2v.i2vauthapi.entity.CompanySettings;
import com.world.i2v.i2vauthapi.repository.CompanySettingsRepository;
import com.world.i2v.i2vauthapi.repository.DepartmentRepository;
import com.world.i2v.i2vauthapi.repository.DepartmentWeeklyOffRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CompanySettingsServiceTest {

    private CompanySettingsRepository settingsRepository;
    private DepartmentWeeklyOffRepository weeklyOffRepository;
    private DepartmentRepository departmentRepository;
    private CompanySettingsService companySettingsService;

    @BeforeEach
    void setUp() {
        settingsRepository = mock(CompanySettingsRepository.class);
        weeklyOffRepository = mock(DepartmentWeeklyOffRepository.class);
        departmentRepository = mock(DepartmentRepository.class);
        companySettingsService = new CompanySettingsService(settingsRepository, weeklyOffRepository, departmentRepository);
    }

    @Test
    void getLateThreshold_shouldReturnNullForInvalidStoredTime() {
        CompanySettings setting = new CompanySettings();
        setting.setSettingKey("LATE_THRESHOLD");
        setting.setSettingValue("-667000000");
        when(settingsRepository.findBySettingKeyAndIsActive("LATE_THRESHOLD", true))
                .thenReturn(Optional.of(setting));

        assertNull(companySettingsService.getLateThreshold());
    }

    @Test
    void saveSetting_shouldRejectMalformedTime() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> companySettingsService.saveSetting("OFFICIAL_END_TIME", "-667000000", "bad value")
        );

        assertTrue(ex.getMessage().contains("Invalid time format"));
    }

    @Test
    void getOfficialEndTime_shouldParseValidTime() {
        CompanySettings setting = new CompanySettings();
        setting.setSettingKey("OFFICIAL_END_TIME");
        setting.setSettingValue("18:00");
        when(settingsRepository.findBySettingKeyAndIsActive("OFFICIAL_END_TIME", true))
                .thenReturn(Optional.of(setting));

        assertEquals(LocalTime.of(18, 0), companySettingsService.getOfficialEndTime());
    }
}
