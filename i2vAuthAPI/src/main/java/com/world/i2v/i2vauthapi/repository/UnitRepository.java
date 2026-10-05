package com.world.i2v.i2vauthapi.repository;

import com.world.i2v.i2vauthapi.entity.Unit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UnitRepository extends JpaRepository<Unit, Long> {
    boolean existsByName(String name);
    boolean existsByCode(String code);
    List<Unit> findByIsActive(Boolean isActive);
}
