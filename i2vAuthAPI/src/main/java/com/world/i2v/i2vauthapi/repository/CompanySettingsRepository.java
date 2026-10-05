package com.world.i2v.i2vauthapi.repository;

import com.world.i2v.i2vauthapi.entity.CompanySettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanySettingsRepository extends JpaRepository<CompanySettings, Long> {

    Optional<CompanySettings> findBySettingKey(String settingKey);

    Optional<CompanySettings> findBySettingKeyAndIsActive(String settingKey, Boolean isActive);
}