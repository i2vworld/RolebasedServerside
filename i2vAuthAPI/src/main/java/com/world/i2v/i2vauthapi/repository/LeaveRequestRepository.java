package com.world.i2v.i2vauthapi.repository;

import com.world.i2v.i2vauthapi.entity.LeaveRequest;
import com.world.i2v.i2vauthapi.entity.LeaveRequest.LeaveStatus;
import com.world.i2v.i2vauthapi.entity.LeaveRequest.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    List<LeaveRequest> findByEmployeeId(Long employeeId);

    List<LeaveRequest> findByEmployeeIdAndStatus(Long employeeId, LeaveStatus status);

    @Query("SELECT l FROM LeaveRequest l WHERE l.employee.id = :employeeId " +
            "AND :date BETWEEN l.startDate AND l.endDate " +
            "AND l.status IN ('PENDING', 'APPROVED')")
    List<LeaveRequest> findActiveLeaveOnDate(@Param("employeeId") Long employeeId,
                                             @Param("date") LocalDate date);

    @Query("SELECT COUNT(l) FROM LeaveRequest l WHERE l.employee.id = :employeeId " +
            "AND l.status = 'APPROVED' AND l.leaveType = :leaveType " +
            "AND YEAR(l.startDate) = :year")
    long countApprovedLeavesByType(@Param("employeeId") Long employeeId,
                                   @Param("leaveType") LeaveType leaveType,
                                   @Param("year") int year);
}