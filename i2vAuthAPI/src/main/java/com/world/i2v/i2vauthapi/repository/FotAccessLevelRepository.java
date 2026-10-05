package com.world.i2v.i2vauthapi.repository;

import com.world.i2v.i2vauthapi.entity.FotAccessLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FotAccessLevelRepository extends JpaRepository<FotAccessLevel, Long> {
    List<FotAccessLevel> findByTerritoryId(Long territoryId);
    List<FotAccessLevel> findByIsActive(Boolean isActive);
}
