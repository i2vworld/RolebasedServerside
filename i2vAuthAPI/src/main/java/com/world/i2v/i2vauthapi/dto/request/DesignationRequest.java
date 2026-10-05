package com.world.i2v.i2vauthapi.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DesignationRequest {

    @NotBlank(message = "Designation title is required")
    @Size(min = 2, max = 100, message = "Title must be between 2 and 100 characters")
    private String title;

    @Size(max = 20, message = "Code cannot exceed 20 characters")
    private String code;

    private String description;

    private Integer level = 1;

    private Long departmentId;

    private BigDecimal minSalary;

    private BigDecimal maxSalary;

    private Boolean isActive = true;

    private Boolean isManagerial = false;
}