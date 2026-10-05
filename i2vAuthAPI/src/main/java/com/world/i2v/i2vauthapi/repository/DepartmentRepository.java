package com.world.i2v.i2vauthapi.repository;

import com.world.i2v.i2vauthapi.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    Optional<Department> findByName(String name);

    Optional<Department> findByCode(String code);

    List<Department> findByIsActive(Boolean isActive);

    List<Department> findByParentDepartmentIsNull();

    @Query("SELECT d FROM Department d LEFT JOIN FETCH d.manager WHERE d.id = :id")
    Optional<Department> findByIdWithManager(@Param("id") Long id);

    @Query("SELECT d FROM Department d LEFT JOIN FETCH d.parentDepartment WHERE d.id = :id")
    Optional<Department> findByIdWithParent(@Param("id") Long id);

    boolean existsByName(String name);

    boolean existsByCode(String code);
}