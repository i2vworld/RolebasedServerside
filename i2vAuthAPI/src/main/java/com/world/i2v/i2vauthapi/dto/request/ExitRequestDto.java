package com.world.i2v.i2vauthapi.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class ExitRequestDto {
    @NotBlank(message = "Place is required")
    private String place;

    @NotBlank(message = "Reason is required")
    private String reason;

    @NotNull(message = "Last working date is required")
    private LocalDate lastWorkingDate;

    private String comments;
}