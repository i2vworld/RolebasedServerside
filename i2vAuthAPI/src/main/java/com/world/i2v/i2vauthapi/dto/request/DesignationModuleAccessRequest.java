package com.world.i2v.i2vauthapi.dto.request;

import lombok.Data;

@Data
public class DesignationModuleAccessRequest {
    private Long designationId;
    private Long moduleId;
    private String accessLevel = "READ";
    private Boolean canCreate = false;
    private Boolean canRead = true;
    private Boolean canUpdate = false;
    private Boolean canDelete = false;
    private Boolean canApprove = false;
    private Boolean isActive = true;
}