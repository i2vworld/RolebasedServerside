package com.world.i2v.i2vauthapi.repository;

import com.world.i2v.i2vauthapi.entity.DepartmentModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentModuleRepository extends JpaRepository<DepartmentModule, Long> {

    List<DepartmentModule> findByDepartmentIdAndIsActive(Long departmentId, Boolean isActive);

    List<DepartmentModule> findByDepartmentId(Long departmentId);

    // Add this method
    Optional<DepartmentModule> findByDepartmentIdAndModuleId(Long departmentId, Long moduleId);

    @Modifying
    @Query("DELETE FROM DepartmentModule dm WHERE dm.department.id = :departmentId")
    void deleteByDepartmentId(@Param("departmentId") Long departmentId);

    @Modifying
    @Query("DELETE FROM DepartmentModule dm WHERE dm.department.id = :departmentId AND dm.module.id = :moduleId")
    void deleteByDepartmentIdAndModuleId(@Param("departmentId") Long departmentId, @Param("moduleId") Long moduleId);
}