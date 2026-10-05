package com.world.i2v.i2vauthapi.repository;

import com.world.i2v.i2vauthapi.entity.DepartmentWeeklyOff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DepartmentWeeklyOffRepository extends JpaRepository<DepartmentWeeklyOff, Long> {

    @Query("SELECT dwo FROM DepartmentWeeklyOff dwo " +
            "WHERE dwo.department.id = :departmentId " +
            "AND dwo.isActive = true " +
            "AND dwo.effectiveFrom <= :date " +
            "AND (dwo.effectiveTo IS NULL OR dwo.effectiveTo >= :date)")
    Optional<DepartmentWeeklyOff> findActiveByDepartmentAndDate(
            @Param("departmentId") Long departmentId,
            @Param("date") LocalDate date);

    Optional<DepartmentWeeklyOff> findByDepartmentIdAndIsActive(Long departmentId, Boolean isActive);
}