package com.world.i2v.i2vauthapi.repository;

import com.world.i2v.i2vauthapi.entity.EmployeeUser;
import com.world.i2v.i2vauthapi.entity.EmployeeUser.EmploymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeUserRepository extends JpaRepository<EmployeeUser, Long> {

    Optional<EmployeeUser> findByEmployeeCode(String employeeCode);

    List<EmployeeUser> findByDepartmentId(Long departmentId);

    List<EmployeeUser> findByDesignationId(Long designationId);

    List<EmployeeUser> findByReportingToId(Long reportingToId);

    List<EmployeeUser> findByEmploymentStatus(EmploymentStatus status);

    @Query("SELECT e FROM EmployeeUser e JOIN FETCH e.user WHERE e.id = :id")
    Optional<EmployeeUser> findByIdWithUser(@Param("id") Long id);

    @Query("SELECT e FROM EmployeeUser e JOIN FETCH e.user WHERE e.user.id = :userId")
    Optional<EmployeeUser> findByUserIdWithUser(@Param("userId") Long userId);

    @Query("SELECT e FROM EmployeeUser e WHERE e.joiningDate BETWEEN :startDate AND :endDate")
    List<EmployeeUser> findByJoiningDateBetween(@Param("startDate") LocalDate startDate,
                                                @Param("endDate") LocalDate endDate);

    @Query("SELECT e FROM EmployeeUser e WHERE e.user.id = :userId")
    Optional<EmployeeUser> findByUserId(@Param("userId") Long userId);

    @Query("SELECT e FROM EmployeeUser e JOIN FETCH e.user LEFT JOIN FETCH e.department LEFT JOIN FETCH e.designation")
    List<EmployeeUser> findAllWithAssociations();

    boolean existsByEmployeeCode(String employeeCode);

    boolean existsByPanNumber(String panNumber);

    boolean existsByAadharNumber(String aadharNumber);

    boolean existsByUserId(Long userId);
}