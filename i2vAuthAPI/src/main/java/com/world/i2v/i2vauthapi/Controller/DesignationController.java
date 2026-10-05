package com.world.i2v.i2vauthapi.Controller;

import com.world.i2v.i2vauthapi.dto.request.DesignationRequest;
import com.world.i2v.i2vauthapi.dto.response.ApiResponse;
import com.world.i2v.i2vauthapi.dto.response.DesignationResponse;
import com.world.i2v.i2vauthapi.service.DesignationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/designations")
@RequiredArgsConstructor
public class DesignationController {

    private final DesignationService designationService;

    @GetMapping
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<DesignationResponse>>> getAllDesignations() {
        return ResponseEntity.ok(ApiResponse.success(designationService.getAllDesignations()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<DesignationResponse>> getDesignationById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(designationService.getDesignationById(id)));
    }

    @GetMapping("/department/{departmentId}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<DesignationResponse>>> getDesignationsByDepartment(
            @PathVariable Long departmentId) {
        return ResponseEntity.ok(ApiResponse.success(designationService.getDesignationsByDepartment(departmentId)));
    }

    @PostMapping
    @PreAuthorize("hasRole('EMPLOYEE') and @securityUtils.canManageDesignations()")
    public ResponseEntity<ApiResponse<DesignationResponse>> createDesignation(
            @Valid @RequestBody DesignationRequest request) {
        DesignationResponse response = designationService.createDesignation(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Designation created successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('EMPLOYEE') and @securityUtils.canManageDesignations()")
    public ResponseEntity<ApiResponse<DesignationResponse>> updateDesignation(
            @PathVariable Long id, @Valid @RequestBody DesignationRequest request) {
        DesignationResponse response = designationService.updateDesignation(id, request);
        return ResponseEntity.ok(ApiResponse.success("Designation updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('EMPLOYEE') and @securityUtils.canManageDesignations()")
    public ResponseEntity<ApiResponse<Void>> deleteDesignation(@PathVariable Long id) {
        designationService.deleteDesignation(id);
        return ResponseEntity.ok(ApiResponse.success("Designation deleted successfully", null));
    }
}
