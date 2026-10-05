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
public class FotAccessLevelResponse {
    private Long id;
    private String name;
    private String code;
    private String description;
    private Integer accessLevel;
    private Long territoryId;
    private String territoryName;
    private Long squadId;
    private String squadName;
    private Long unitId;
    private String unitName;
    private String country;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
