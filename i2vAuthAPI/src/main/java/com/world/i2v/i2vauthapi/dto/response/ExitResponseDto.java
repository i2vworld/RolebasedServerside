package com.world.i2v.i2vauthapi.dto.response;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExitResponseDto {
    private Long id;
    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private String designation;
    private String unitName;
    private String squadName;
    private String territoryName;
    private String fotAccessLevelName;
    private String place;
    private String reason;
    private LocalDate lastWorkingDate;
    private String comments;
    private String status;             // "PENDING", "APPROVED", "REJECTED"
    private LocalDateTime appliedAt;
    private List<ClearanceStepDto> clearanceSteps;
}
