package com.world.i2v.i2vauthapi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TerritoryResponse {
    private Long id;
    private String name;
    private String code;
    private String description;
    private Long squadId;
    private String squadName;
    private Long unitId;
    private String unitName;
    private String country;
    private Integer accessLevel;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<FotAccessLevelResponse> fotAccessLevels;
    private Integer fotCount;
}
