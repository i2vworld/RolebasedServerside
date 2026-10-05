package com.world.i2v.i2vauthapi.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UnitRequest {

    @NotBlank(message = "Unit name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @Size(max = 20, message = "Code cannot exceed 20 characters")
    private String code;

    private String description;

    private String country;

    private Integer accessLevel = 1;

    private Boolean isActive = true;
}
