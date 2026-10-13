package com.world.i2v.i2vauthapi.Controller;

import com.world.i2v.i2vauthapi.dto.request.*;
import com.world.i2v.i2vauthapi.dto.response.ApiResponse;
import com.world.i2v.i2vauthapi.dto.response.JwtResponse;
import com.world.i2v.i2vauthapi.exception.BadRequestException;
import com.world.i2v.i2vauthapi.exception.UnauthorizedException;
import com.world.i2v.i2vauthapi.security.UserDetailsImpl;
import com.world.i2v.i2vauthapi.service.AuthService;
import com.world.i2v.i2vauthapi.service.SessionService;
import com.world.i2v.i2vauthapi.util.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SessionService sessionService;

    // ==================== AUTH ====================

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup(@Valid @RequestBody SignupRequest request) {
        authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", null));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<JwtResponse>> login(@Valid @RequestBody LoginRequest request,
                                                          HttpServletRequest httpRequest) {
        String ipAddress = RequestUtils.getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        JwtResponse response = authService.login(request, ipAddress, userAgent);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BadRequestException("Invalid Authorization header");
        }
        authService.logout(authHeader.substring(7));
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<JwtResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        JwtResponse response = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset link sent to your email", null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully", null));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        if (currentUser == null) {
            throw new UnauthorizedException("Authentication required");
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }
        authService.changePassword(currentUser.getId(), request.getOldPassword(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success("Password updated successfully", null));
    }

    // ==================== SESSION MANAGEMENT ====================

    @GetMapping("/sessions")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMySessions(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(ApiResponse.success(sessionService.getActiveSessions(currentUser.getId())));
    }

    @DeleteMapping("/sessions/{sessionId}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<Void>> revokeSession(
            @PathVariable Long sessionId,
            @RequestHeader(value = "X-Current-Token", required = false) String currentToken) {
        sessionService.revokeSession(sessionId, currentToken);
        return ResponseEntity.ok(ApiResponse.success("Session revoked", null));
    }

    @PostMapping("/sessions/revoke-all")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> revokeAllOtherSessions(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @RequestHeader(value = "X-Current-Token", required = false) String currentToken) {
        int revokedCount = sessionService.revokeAllOtherSessions(currentUser.getId(), currentToken);
        return ResponseEntity.ok(ApiResponse.success("Other sessions revoked",
                Map.of("revokedCount", revokedCount)));
    }
}
