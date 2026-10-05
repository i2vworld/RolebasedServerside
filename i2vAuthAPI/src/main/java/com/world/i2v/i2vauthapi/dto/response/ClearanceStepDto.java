package com.world.i2v.i2vauthapi.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClearanceStepDto {
    private Long id;
    private String step;
    private String assignedTo;
    private String status;               // "PENDING", "APPROVED", "REJECTED"
    private LocalDateTime completedAt;
}