package com.world.i2v.i2vauthapi.Controller;

import com.world.i2v.i2vauthapi.dto.response.ApiResponse;
import com.world.i2v.i2vauthapi.dto.response.ExitResponseDto;
import com.world.i2v.i2vauthapi.security.UserDetailsImpl;
import com.world.i2v.i2vauthapi.service.ManagerExitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/manager/exit-requests")
@RequiredArgsConstructor
public class ManagerExitController {

    private final ManagerExitService managerExitService;

    @GetMapping
    @PreAuthorize("hasRole('EMPLOYEE') and @securityUtils.canManageEmployees()")
    public ResponseEntity<ApiResponse<List<ExitResponseDto>>> getSubordinateExitRequests(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        List<ExitResponseDto> requests = managerExitService.getSubordinateExitRequests(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(requests));
    }

    @PutMapping("/{exitRequestId}/steps/{stepId}/approve")
    @PreAuthorize("hasRole('EMPLOYEE') and @securityUtils.canManageEmployees()")
    public ResponseEntity<ApiResponse<Void>> approveStep(
            @PathVariable Long exitRequestId,
            @PathVariable Long stepId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        managerExitService.approveStep(exitRequestId, stepId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Manager approved step", null));
    }

    @PutMapping("/{exitRequestId}/steps/{stepId}/reject")
    @PreAuthorize("hasRole('EMPLOYEE') and @securityUtils.canManageEmployees()")
    public ResponseEntity<ApiResponse<Void>> rejectStep(
            @PathVariable Long exitRequestId,
            @PathVariable Long stepId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        String remarks = body.getOrDefault("remarks", "");
        managerExitService.rejectStep(exitRequestId, stepId, currentUser.getId(), remarks);
        return ResponseEntity.ok(ApiResponse.success("Manager rejected step", null));
    }

    @PutMapping("/{exitRequestId}/reopen")
    @PreAuthorize("hasRole('EMPLOYEE') and @securityUtils.canManageEmployees()")
    public ResponseEntity<ApiResponse<Void>> reopenExitRequest(
            @PathVariable Long exitRequestId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        managerExitService.reopenExitRequest(exitRequestId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Exit request reopened", null));
    }

    @DeleteMapping("/{exitRequestId}")
    @PreAuthorize("hasRole('EMPLOYEE') and @securityUtils.canManageEmployees()")
    public ResponseEntity<ApiResponse<Void>> hardDeleteExitRequest(
            @PathVariable Long exitRequestId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        managerExitService.hardDeleteExitRequest(exitRequestId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Exit request deleted", null));
    }
}
