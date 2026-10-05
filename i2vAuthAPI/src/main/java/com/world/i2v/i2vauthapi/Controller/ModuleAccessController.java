package com.world.i2v.i2vauthapi.Controller;

import com.world.i2v.i2vauthapi.dto.request.DesignationModuleAccessRequest;
import com.world.i2v.i2vauthapi.dto.response.ApiResponse;
import com.world.i2v.i2vauthapi.dto.response.DesignationModuleAccessResponse;
import com.world.i2v.i2vauthapi.dto.response.ModuleResponse;
import com.world.i2v.i2vauthapi.security.UserDetailsImpl;
import com.world.i2v.i2vauthapi.service.ModuleAccessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/module-access")
@RequiredArgsConstructor
public class ModuleAccessController {

    private final ModuleAccessService moduleAccessService;

    @GetMapping("/bootstrap")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAccessBootstrapData() {
        try {
            return ResponseEntity.ok(ApiResponse.success(moduleAccessService.getAccessBootstrapData()));
        } catch (Exception e) {
            log.error("Error getting module-access bootstrap data: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.success(Map.of(
                    "departments", new ArrayList<>(),
                    "designations", new ArrayList<>(),
                    "modules", new ArrayList<>()
            )));
        }
    }

    @GetMapping("/my-permissions")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<Object>> getMyPermissions(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(ApiResponse.success(moduleAccessService.getUserPermissions(currentUser.getId())));
    }

    @GetMapping("/department/{departmentId}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<ModuleResponse>>> getModulesForDepartment(
            @PathVariable Long departmentId) {
        try {
            return ResponseEntity.ok(ApiResponse.success(moduleAccessService.getModulesForDepartment(departmentId)));
        } catch (Exception e) {
            log.error("Error getting modules for department {}: {}", departmentId, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.success(new ArrayList<>()));
        }
    }

    @GetMapping("/designation/{designationId}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<DesignationModuleAccessResponse>>> getAccessForDesignation(
            @PathVariable Long designationId) {
        try {
            return ResponseEntity.ok(ApiResponse.success(moduleAccessService.getAccessForDesignation(designationId)));
        } catch (Exception e) {
            log.error("Error getting access for designation {}: {}", designationId, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.success(new ArrayList<>()));
        }
    }

    @PostMapping("/department/{departmentId}")
    @PreAuthorize("hasRole('EMPLOYEE') and @securityUtils.canManageModuleAccess()")
    public ResponseEntity<ApiResponse<Void>> assignModulesToDepartment(
            @PathVariable Long departmentId,
            @RequestBody List<Long> moduleIds) {
        moduleAccessService.assignModulesToDepartment(departmentId, moduleIds);
        return ResponseEntity.ok(ApiResponse.success("Modules assigned successfully", null));
    }

    @PostMapping("/designation")
    @PreAuthorize("hasRole('EMPLOYEE') and @securityUtils.canManageModuleAccess()")
    public ResponseEntity<ApiResponse<Void>> assignAccessToDesignation(
            @RequestBody DesignationModuleAccessRequest request) {
        moduleAccessService.assignAccessToDesignation(request);
        return ResponseEntity.ok(ApiResponse.success("Access assigned successfully", null));
    }

    @DeleteMapping("/department/{departmentId}/module/{moduleId}")
    @PreAuthorize("hasRole('EMPLOYEE') and @securityUtils.canManageModuleAccess()")
    public ResponseEntity<ApiResponse<Void>> removeModuleFromDepartment(
            @PathVariable Long departmentId,
            @PathVariable Long moduleId) {
        moduleAccessService.removeModuleFromDepartment(departmentId, moduleId);
        return ResponseEntity.ok(ApiResponse.success("Module removed successfully", null));
    }

    @DeleteMapping("/designation/{designationId}/module/{moduleId}")
    @PreAuthorize("hasRole('EMPLOYEE') and @securityUtils.canManageModuleAccess()")
    public ResponseEntity<ApiResponse<Void>> revokeAccessFromDesignation(
            @PathVariable Long designationId,
            @PathVariable Long moduleId) {
        moduleAccessService.revokeAccessFromDesignation(designationId, moduleId);
        return ResponseEntity.ok(ApiResponse.success("Access revoked successfully", null));
    }
}
