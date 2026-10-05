package com.world.i2v.i2vauthapi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DesignationResponse {

    private Long id;
    private String title;
    private String code;
    private String description;
    private Integer level;
    private DepartmentResponse department;
    private BigDecimal minSalary;
    private BigDecimal maxSalary;
    private Boolean isActive;
    private Boolean isManagerial;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long employeeCount;
}