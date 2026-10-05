package com.world.i2v.i2vauthapi.Controller;

import com.world.i2v.i2vauthapi.dto.request.ExitRequestDto;
import com.world.i2v.i2vauthapi.dto.response.ApiResponse;
import com.world.i2v.i2vauthapi.dto.response.ExitResponseDto;
import com.world.i2v.i2vauthapi.security.UserDetailsImpl;
import com.world.i2v.i2vauthapi.service.ExitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class ExitController {

    private final ExitService exitService;

    @GetMapping("/me/exit")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<ExitResponseDto>> getMyExitRequest(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        ExitResponseDto exitDto = exitService.getMyExitRequest(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(exitDto));
    }

    @PostMapping("/exit")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<ExitResponseDto>> submitExitRequest(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @Valid @RequestBody ExitRequestDto requestDto) {
        ExitResponseDto responseDto = exitService.submitExitRequest(currentUser.getId(), requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Resignation request submitted", responseDto));
    }

    @PutMapping("/exit/{exitRequestId}/reopen")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<ExitResponseDto>> reopenExitRequest(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @PathVariable Long exitRequestId) {
        ExitResponseDto responseDto = exitService.reopenMyExitRequest(currentUser.getId(), exitRequestId);
        return ResponseEntity.ok(ApiResponse.success("Exit request reopened", responseDto));
    }

    @DeleteMapping("/exit/{exitRequestId}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<Void>> deleteExitRequest(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @PathVariable Long exitRequestId) {
        exitService.hardDeleteMyExitRequest(currentUser.getId(), exitRequestId);
        return ResponseEntity.ok(ApiResponse.success("Exit request deleted", null));
    }

    @PostMapping("/exit/{exitRequestId}/send-notification")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<Void>> sendExitNotification(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @PathVariable Long exitRequestId) {
        exitService.sendExitNotificationEmail(currentUser.getId(), exitRequestId);
        return ResponseEntity.ok(ApiResponse.success("Notification email sent", null));
    }
}
