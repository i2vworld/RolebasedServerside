package com.world.i2v.i2vauthapi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DesignationModuleAccessResponse {
    private Long id;
    private Long designationId;
    private String designationTitle;
    private ModuleResponse module;
    private String accessLevel;
    private Boolean canCreate;
    private Boolean canRead;
    private Boolean canUpdate;
    private Boolean canDelete;
    private Boolean canApprove;
    private Boolean isActive;
}