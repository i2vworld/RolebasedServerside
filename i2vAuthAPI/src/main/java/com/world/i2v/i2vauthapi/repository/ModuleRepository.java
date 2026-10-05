package com.world.i2v.i2vauthapi.repository;

import com.world.i2v.i2vauthapi.entity.Module;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ModuleRepository extends JpaRepository<Module, Long> {

    Optional<Module> findByName(String name);

    Optional<Module> findByCode(String code);

    List<Module> findByIsActive(Boolean isActive);  // 🔥 ADD THIS

    List<Module> findByParentModuleIsNull();

    List<Module> findByParentModuleId(Long parentId);

    List<Module> findByIsVisible(Boolean isVisible);

    @Query("SELECT m FROM Module m WHERE m.isActive = true ORDER BY m.displayOrder ASC, m.name ASC")
    List<Module> findAllActiveOrdered();

    boolean existsByCode(String code);
}