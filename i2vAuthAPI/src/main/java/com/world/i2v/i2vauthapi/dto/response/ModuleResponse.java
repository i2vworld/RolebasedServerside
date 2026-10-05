package com.world.i2v.i2vauthapi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModuleResponse {
    private Long id;
    private String name;
    private String code;
    private String description;
    private String icon;
    private String routePath;
    private Integer displayOrder;
    private Boolean isActive;
    private Boolean isVisible;
    private Boolean requiresPremium;
    private Long parentModuleId;
    private String parentModuleName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}