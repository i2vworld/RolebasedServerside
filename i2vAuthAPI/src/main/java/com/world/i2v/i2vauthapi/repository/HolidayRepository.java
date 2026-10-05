package com.world.i2v.i2vauthapi.repository;

import com.world.i2v.i2vauthapi.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Long> {

    Optional<Holiday> findByHolidayDate(LocalDate date);

    Optional<Holiday> findByHolidayDateAndIsActive(LocalDate date, Boolean isActive);

    @Query("SELECT h FROM Holiday h WHERE h.isActive = true AND h.isRecurringYearly = true AND MONTH(h.holidayDate) = :month AND DAY(h.holidayDate) = :day")
    Optional<Holiday> findRecurringHolidayByMonthAndDay(@Param("month") int month, @Param("day") int day);

    @Query("SELECT h FROM Holiday h WHERE YEAR(h.holidayDate) = :year AND h.isActive = true ORDER BY h.holidayDate")
    List<Holiday> findByYear(@Param("year") int year);

    List<Holiday> findByIsActive(Boolean isActive);

    @Query("SELECT h FROM Holiday h WHERE h.holidayDate BETWEEN :startDate AND :endDate AND h.isActive = true")
    List<Holiday> findBetweenDates(@Param("startDate") LocalDate startDate,
                                   @Param("endDate") LocalDate endDate);
}