package com.world.i2v.i2vauthapi.service;

import com.world.i2v.i2vauthapi.entity.Holiday;
import com.world.i2v.i2vauthapi.repository.HolidayRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class HolidayService {

    private final HolidayRepository holidayRepository;

    public HolidayService(HolidayRepository holidayRepository) {
        this.holidayRepository = holidayRepository;
    }

    @Cacheable(value = "holidays", key = "#year")
    public List<Holiday> getHolidaysByYear(int year) {
        log.info("Fetching holidays for year: {}", year);
        return holidayRepository.findByYear(year);
    }

    public List<Holiday> getAllActiveHolidays() {
        return holidayRepository.findByIsActive(true);
    }

    public Optional<Holiday> getHolidayByDate(LocalDate date) {
        Optional<Holiday> exactHoliday = holidayRepository.findByHolidayDateAndIsActive(date, true);
        if (exactHoliday.isPresent()) {
            return exactHoliday;
        }

        return holidayRepository.findRecurringHolidayByMonthAndDay(date.getMonthValue(), date.getDayOfMonth());
    }

    public boolean isHoliday(LocalDate date) {
        if (holidayRepository.findByHolidayDateAndIsActive(date, true).isPresent()) {
            return true;
        }

        return holidayRepository.findRecurringHolidayByMonthAndDay(date.getMonthValue(), date.getDayOfMonth()).isPresent();
    }

    @CacheEvict(value = "holidays", allEntries = true)
    public Holiday saveHoliday(Holiday holiday) {
        log.info("Saving holiday: {}", holiday);
        if (holiday.getIsActive() == null) {
            holiday.setIsActive(true);
        }
        return holidayRepository.save(holiday);
    }

    @CacheEvict(value = "holidays", allEntries = true)
    public void deleteHoliday(Long id) {
        log.info("Deleting holiday ID: {}", id);
        holidayRepository.findById(id).ifPresent(h -> {
            h.setIsActive(false);
            holidayRepository.save(h);
        });
    }

    @CacheEvict(value = "holidays", allEntries = true)
    public void clearCache() {
        log.info("Holidays cache cleared");
    }
}