package com.world.i2v.i2vauthapi.repository;

import com.world.i2v.i2vauthapi.entity.ExitRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ExitRequestRepository extends JpaRepository<ExitRequest, Long> {
    Optional<ExitRequest> findTopByEmployeeIdOrderByAppliedAtDesc(Long employeeId);

    // Add this method for admin listing
    List<ExitRequest> findAllByOrderByAppliedAtDesc();
    
    // Add this method for employee deletion cascade cleanup
    List<ExitRequest> findByEmployeeId(Long employeeId);
}