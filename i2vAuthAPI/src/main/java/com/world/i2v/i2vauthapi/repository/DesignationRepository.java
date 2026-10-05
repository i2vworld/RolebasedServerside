package com.world.i2v.i2vauthapi.repository;

import com.world.i2v.i2vauthapi.entity.Designation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DesignationRepository extends JpaRepository<Designation, Long> {

    Optional<Designation> findByTitle(String title);

    Optional<Designation> findByCode(String code);

    List<Designation> findByIsActive(Boolean isActive);

    List<Designation> findByDepartmentId(Long departmentId);

    List<Designation> findByIsManagerial(Boolean isManagerial);

    @Query("SELECT d FROM Designation d WHERE d.level BETWEEN :minLevel AND :maxLevel")
    List<Designation> findByLevelRange(@Param("minLevel") Integer minLevel,
                                       @Param("maxLevel") Integer maxLevel);

    boolean existsByTitle(String title);

    boolean existsByCode(String code);
}