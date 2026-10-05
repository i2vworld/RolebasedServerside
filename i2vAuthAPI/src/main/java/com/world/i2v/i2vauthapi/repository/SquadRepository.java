package com.world.i2v.i2vauthapi.repository;

import com.world.i2v.i2vauthapi.entity.Squad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SquadRepository extends JpaRepository<Squad, Long> {
    List<Squad> findByUnitId(Long unitId);
    List<Squad> findByIsActive(Boolean isActive);
    boolean existsByNameAndUnitId(String name, Long unitId);
}
