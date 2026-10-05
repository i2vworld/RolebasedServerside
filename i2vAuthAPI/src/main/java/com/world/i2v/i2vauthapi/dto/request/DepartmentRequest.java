package com.world.i2v.i2vauthapi.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DepartmentRequest {

    @NotBlank(message = "Department name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @Size(max = 20, message = "Code cannot exceed 20 characters")
    private String code;

    private String description;

    private Long parentDepartmentId;

    private Long unitId;

    private Boolean isHierarchyUnit;

    private Long managerId;

    private Boolean isActive = true;

    private BigDecimal budget = BigDecimal.ZERO;

    private String location;

    @Email(message = "Invalid email format")
    private String email;

    private String phone;
}