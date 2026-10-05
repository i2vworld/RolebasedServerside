package com.world.i2v.i2vauthapi.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ModuleRequest {

    @NotBlank(message = "Module name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @Size(max = 50, message = "Code cannot exceed 50 characters")
    private String code;

    private String description;

    private Long parentModuleId;

    private String icon;

    private String routePath;

    private Integer displayOrder = 0;

    private Boolean isActive = true;

    private Boolean isVisible = true;

    private Boolean requiresPremium = false;
}