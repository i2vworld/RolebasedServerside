package com.world.i2v.i2vauthapi.service;

import com.world.i2v.i2vauthapi.entity.Holiday;
import com.world.i2v.i2vauthapi.repository.HolidayRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HolidayServiceTest {

    private HolidayRepository holidayRepository;

    private HolidayService holidayService;

    @BeforeEach
    void setUp() {
        holidayRepository = mock(HolidayRepository.class);
        holidayService = new HolidayService(holidayRepository);
    }

    @Test
    void smokeTest_serviceIsConstructed() {
        assertNotNull(holidayService);
    }

    // ==================== getHolidaysByYear ====================

    @Test
    void getHolidaysByYear_returnsHolidaysForYear() {
        Holiday h = new Holiday();
        when(holidayRepository.findByYear(2026)).thenReturn(List.of(h));

        List<Holiday> result = holidayService.getHolidaysByYear(2026);

        assertEquals(1, result.size());
        verify(holidayRepository).findByYear(2026);
    }

    @Test
    void getHolidaysByYear_noHolidays_returnsEmptyList() {
        when(holidayRepository.findByYear(2026)).thenReturn(List.of());

        List<Holiday> result = holidayService.getHolidaysByYear(2026);

        assertTrue(result.isEmpty());
    }

    // ==================== getAllActiveHolidays ====================

    @Test
    void getAllActiveHolidays_returnsActiveHolidays() {
        Holiday h = new Holiday();
        when(holidayRepository.findByIsActive(true)).thenReturn(List.of(h));

        List<Holiday> result = holidayService.getAllActiveHolidays();

        assertEquals(1, result.size());
    }

    // ==================== getHolidayByDate ====================

    @Test
    void getHolidayByDate_exactMatch_returnsHoliday() {
        LocalDate date = LocalDate.of(2026, 1, 26);
        Holiday h = new Holiday();
        when(holidayRepository.findByHolidayDateAndIsActive(date, true)).thenReturn(Optional.of(h));

        Optional<Holiday> result = holidayService.getHolidayByDate(date);

        assertTrue(result.isPresent());
        verify(holidayRepository, never()).findRecurringHolidayByMonthAndDay(anyInt(), anyInt());
    }

    @Test
    void getHolidayByDate_noExactMatch_fallsBackToRecurring() {
        LocalDate date = LocalDate.of(2026, 1, 26);
        Holiday recurring = new Holiday();
        when(holidayRepository.findByHolidayDateAndIsActive(date, true)).thenReturn(Optional.empty());
        when(holidayRepository.findRecurringHolidayByMonthAndDay(1, 26)).thenReturn(Optional.of(recurring));

        Optional<Holiday> result = holidayService.getHolidayByDate(date);

        assertTrue(result.isPresent());
        assertSame(recurring, result.get());
    }

    @Test
    void getHolidayByDate_noMatch_returnsEmpty() {
        LocalDate date = LocalDate.of(2026, 1, 26);
        when(holidayRepository.findByHolidayDateAndIsActive(date, true)).thenReturn(Optional.empty());
        when(holidayRepository.findRecurringHolidayByMonthAndDay(1, 26)).thenReturn(Optional.empty());

        Optional<Holiday> result = holidayService.getHolidayByDate(date);

        assertFalse(result.isPresent());
    }

    // ==================== isHoliday ====================

    @Test
    void isHoliday_exactMatch_returnsTrue() {
        LocalDate date = LocalDate.of(2026, 1, 26);
        when(holidayRepository.findByHolidayDateAndIsActive(date, true)).thenReturn(Optional.of(new Holiday()));

        assertTrue(holidayService.isHoliday(date));
        verify(holidayRepository, never()).findRecurringHolidayByMonthAndDay(anyInt(), anyInt());
    }

    @Test
    void isHoliday_recurringMatch_returnsTrue() {
        LocalDate date = LocalDate.of(2026, 1, 26);
        when(holidayRepository.findByHolidayDateAndIsActive(date, true)).thenReturn(Optional.empty());
        when(holidayRepository.findRecurringHolidayByMonthAndDay(1, 26)).thenReturn(Optional.of(new Holiday()));

        assertTrue(holidayService.isHoliday(date));
    }

    @Test
    void isHoliday_noMatch_returnsFalse() {
        LocalDate date = LocalDate.of(2026, 1, 26);
        when(holidayRepository.findByHolidayDateAndIsActive(date, true)).thenReturn(Optional.empty());
        when(holidayRepository.findRecurringHolidayByMonthAndDay(1, 26)).thenReturn(Optional.empty());

        assertFalse(holidayService.isHoliday(date));
    }

    // ==================== saveHoliday ====================

    @Test
    void saveHoliday_isActiveNull_defaultsToTrue() {
        Holiday holiday = new Holiday();
        holiday.setIsActive(null);
        when(holidayRepository.save(any(Holiday.class))).thenAnswer(inv -> inv.getArgument(0));

        Holiday result = holidayService.saveHoliday(holiday);

        assertTrue(result.getIsActive());
    }

    @Test
    void saveHoliday_isActiveAlreadySet_preservesValue() {
        Holiday holiday = new Holiday();
        holiday.setIsActive(false);
        when(holidayRepository.save(any(Holiday.class))).thenAnswer(inv -> inv.getArgument(0));

        Holiday result = holidayService.saveHoliday(holiday);

        assertFalse(result.getIsActive());
    }

    // ==================== deleteHoliday ====================

    @Test
    void deleteHoliday_existingHoliday_marksInactiveAndSaves() {
        Holiday holiday = new Holiday();
        holiday.setId(1L);
        holiday.setIsActive(true);
        when(holidayRepository.findById(1L)).thenReturn(Optional.of(holiday));

        holidayService.deleteHoliday(1L);

        assertFalse(holiday.getIsActive());
        verify(holidayRepository).save(holiday);
    }

    @Test
    void deleteHoliday_notFound_doesNothing() {
        when(holidayRepository.findById(1L)).thenReturn(Optional.empty());

        holidayService.deleteHoliday(1L);

        verify(holidayRepository, never()).save(any());
    }

    // ==================== clearCache ====================

    @Test
    void clearCache_doesNotThrow() {
        assertDoesNotThrow(() -> holidayService.clearCache());
    }
}
