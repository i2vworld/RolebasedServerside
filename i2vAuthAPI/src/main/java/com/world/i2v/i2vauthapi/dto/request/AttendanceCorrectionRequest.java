package com.world.i2v.i2vauthapi.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class AttendanceCorrectionRequest {
    @NotNull
    private LocalDate attendanceDate;

    private LocalTime requestedCheckInTime;
    private LocalTime requestedCheckOutTime;

    @NotBlank
    @Size(max = 1000)
    private String reason;
}