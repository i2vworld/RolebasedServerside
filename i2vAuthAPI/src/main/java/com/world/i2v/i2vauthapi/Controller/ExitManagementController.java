package com.world.i2v.i2vauthapi.Controller;

import com.world.i2v.i2vauthapi.dto.response.ApiResponse;
import com.world.i2v.i2vauthapi.dto.response.ExitResponseDto;
import com.world.i2v.i2vauthapi.service.ExitManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/exit-requests")
@RequiredArgsConstructor
public class ExitManagementController {

    private final ExitManagementService exitManagementService;

    @GetMapping
    @PreAuthorize("hasRole('EMPLOYEE') and @securityUtils.canManageEmployees()")
    public ResponseEntity<ApiResponse<List<ExitResponseDto>>> getAllExitRequests(
            @RequestParam(required = false) String status) {
        List<ExitResponseDto> requests = exitManagementService.getAllExitRequests(status);
        return ResponseEntity.ok(ApiResponse.success(requests));
    }

    @PutMapping("/{exitRequestId}/steps/{stepId}/approve")
    @PreAuthorize("hasRole('EMPLOYEE') and @securityUtils.canManageEmployees()")
    public ResponseEntity<ApiResponse<Void>> approveStep(
            @PathVariable Long exitRequestId,
            @PathVariable Long stepId) {
        exitManagementService.approveStep(exitRequestId, stepId);
        return ResponseEntity.ok(ApiResponse.success("Step approved", null));
    }

    @PutMapping("/{exitRequestId}/steps/{stepId}/reject")
    @PreAuthorize("hasRole('EMPLOYEE') and @securityUtils.canManageEmployees()")
    public ResponseEntity<ApiResponse<Void>> rejectStep(
            @PathVariable Long exitRequestId,
            @PathVariable Long stepId,
            @RequestBody Map<String, String> body) {
        String remarks = body.getOrDefault("remarks", "");
        exitManagementService.rejectStep(exitRequestId, stepId, remarks);
        return ResponseEntity.ok(ApiResponse.success("Step rejected", null));
    }

    @PutMapping("/{exitRequestId}/reopen")
    @PreAuthorize("hasRole('EMPLOYEE') and @securityUtils.canManageEmployees()")
    public ResponseEntity<ApiResponse<Void>> reopenExitRequest(@PathVariable Long exitRequestId) {
        exitManagementService.reopenExitRequest(exitRequestId);
        return ResponseEntity.ok(ApiResponse.success("Exit request reopened", null));
    }

    @DeleteMapping("/{exitRequestId}")
    @PreAuthorize("hasRole('EMPLOYEE') and @securityUtils.canManageEmployees()")
    public ResponseEntity<ApiResponse<Void>> hardDeleteExitRequest(@PathVariable Long exitRequestId) {
        exitManagementService.hardDeleteExitRequest(exitRequestId);
        return ResponseEntity.ok(ApiResponse.success("Exit request deleted", null));
    }
}
