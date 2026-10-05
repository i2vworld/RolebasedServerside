package com.world.i2v.i2vauthapi.repository;

import com.world.i2v.i2vauthapi.entity.DesignationModuleAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DesignationModuleAccessRepository extends JpaRepository<DesignationModuleAccess, Long> {

    List<DesignationModuleAccess> findByDesignationIdAndIsActive(Long designationId, Boolean isActive);

    Optional<DesignationModuleAccess> findByDesignationIdAndModuleId(Long designationId, Long moduleId);

    @Query("SELECT dma FROM DesignationModuleAccess dma " +
            "WHERE dma.designation.id = :designationId " +
            "AND dma.module.code = :moduleCode " +
            "AND dma.isActive = true")
    Optional<DesignationModuleAccess> findByDesignationAndModuleCode(
            @Param("designationId") Long designationId,
            @Param("moduleCode") String moduleCode);
}