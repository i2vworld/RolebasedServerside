package com.world.i2v.i2vauthapi.repository;

import com.world.i2v.i2vauthapi.entity.AttendanceCorrection;
import com.world.i2v.i2vauthapi.entity.AttendanceCorrection.CorrectionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceCorrectionRepository extends JpaRepository<AttendanceCorrection, Long> {
    List<AttendanceCorrection> findByEmployeeIdOrderBySubmittedAtDesc(Long employeeId);

    List<AttendanceCorrection> findByStatusOrderBySubmittedAtAsc(CorrectionStatus status);

    boolean existsByEmployeeIdAndAttendanceDateAndStatus(
            Long employeeId, LocalDate attendanceDate, CorrectionStatus status);
}