package com.world.i2v.i2vauthapi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentResponse {

    private Long id;
    private String name;
    private String code;
    private String description;
    private DepartmentResponse parentDepartment;
    private List<DepartmentResponse> subDepartments;
    private Long unitId;
    private String unitName;
    private String unitCode;
    private Boolean isHierarchyUnit;
    private UserResponse manager;
    private Boolean isActive;
    private BigDecimal budget;
    private String location;
    private String email;
    private String phone;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long employeeCount;
}