package com.world.i2v.i2vauthapi.Controller;

import com.world.i2v.i2vauthapi.dto.request.EmployeeJoinDto;
import com.world.i2v.i2vauthapi.dto.request.EmployeeUserRequest;
import com.world.i2v.i2vauthapi.dto.request.ProfileUpdateRequest;
import com.world.i2v.i2vauthapi.dto.response.ApiResponse;
import com.world.i2v.i2vauthapi.dto.response.EmployeeUserResponse;
import com.world.i2v.i2vauthapi.security.UserDetailsImpl;
import com.world.i2v.i2vauthapi.service.EmployeeUserService;
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
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeUserService employeeService;

    // ==================== GET ENDPOINTS ====================

    @GetMapping
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<EmployeeUserResponse>>> getAllEmployees() {
        return ResponseEntity.ok(ApiResponse.success(employeeService.getAllEmployees()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<EmployeeUserResponse>> getEmployeeById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(employeeService.getEmployeeById(id)));
    }

    @GetMapping("/code/{employeeCode}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<EmployeeUserResponse>> getEmployeeByCode(
            @PathVariable String employeeCode) {
        return ResponseEntity.ok(ApiResponse.success(employeeService.getEmployeeByCode(employeeCode)));
    }

    @GetMapping("/department/{departmentId}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<EmployeeUserResponse>>> getEmployeesByDepartment(
            @PathVariable Long departmentId) {
        return ResponseEntity.ok(ApiResponse.success(employeeService.getEmployeesByDepartment(departmentId)));
    }

    @GetMapping("/designation/{designationId}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<EmployeeUserResponse>>> getEmployeesByDesignation(
            @PathVariable Long designationId) {
        return ResponseEntity.ok(ApiResponse.success(employeeService.getEmployeesByDesignation(designationId)));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<EmployeeUserResponse>> getCurrentEmployee(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(ApiResponse.success(employeeService.getEmployeeByUserId(currentUser.getId())));
    }

    @GetMapping("/{id}/user-active")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<Boolean>> isUserActive(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(employeeService.isUserActive(id)));
    }

    // ==================== CREATE/UPDATE/DELETE ====================

    @PostMapping
    @PreAuthorize("hasRole('EMPLOYEE') and @securityUtils.canManageEmployees()")
    public ResponseEntity<ApiResponse<EmployeeUserResponse>> createEmployee(
            @Valid @RequestBody EmployeeUserRequest request) {
        EmployeeUserResponse response = employeeService.createEmployee(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Employee created successfully", response));
    }

    @PostMapping("/join")
    public ResponseEntity<ApiResponse<EmployeeUserResponse>> joinEmployee(
            @RequestBody EmployeeJoinDto request) {
        EmployeeUserResponse response = employeeService.createEmployeeFromJoin(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Employee join processed", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('EMPLOYEE') and @securityUtils.canManageEmployees()")
    public ResponseEntity<ApiResponse<EmployeeUserResponse>> updateEmployee(
            @PathVariable Long id, @Valid @RequestBody EmployeeUserRequest request) {
        EmployeeUserResponse response = employeeService.updateEmployee(id, request);
        return ResponseEntity.ok(ApiResponse.success("Employee updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<Void>> deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.ok(ApiResponse.success("Employee deleted successfully", null));
    }

    // ==================== USER ACCOUNT STATUS ====================

    @PostMapping("/{id}/deactivate-user")
    @PreAuthorize("hasRole('EMPLOYEE') and @securityUtils.canManageEmployees()")
    public ResponseEntity<ApiResponse<Void>> deactivateUser(
            @PathVariable Long id,
            @RequestParam String reason) {
        employeeService.deactivateUserAccount(id, reason);
        return ResponseEntity.ok(ApiResponse.success("User account deactivated", null));
    }

    @PostMapping("/{id}/reactivate-user")
    @PreAuthorize("hasRole('EMPLOYEE') and @securityUtils.canManageEmployees()")
    public ResponseEntity<ApiResponse<Void>> reactivateUser(
            @PathVariable Long id,
            @RequestParam String reason) {
        employeeService.reactivateUserAccount(id, reason);
        return ResponseEntity.ok(ApiResponse.success("User account reactivated", null));
    }

    // ==================== USER ACCOUNT MANAGEMENT (For Available Users without employee record) ====================

    @PostMapping("/user/{userId}/deactivate")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<Void>> deactivateUserByUserId(
            @PathVariable Long userId,
            @RequestParam(required = false) String reason) {
        employeeService.deactivateUserByUserId(userId,
                reason != null ? reason : "User deactivation requested");
        return ResponseEntity.ok(ApiResponse.success("User account deactivated successfully", null));
    }

    @PostMapping("/user/{userId}/reactivate")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<Void>> reactivateUserByUserId(
            @PathVariable Long userId,
            @RequestParam(required = false) String reason) {
        employeeService.reactivateUserByUserId(userId,
                reason != null ? reason : "User reactivation requested");
        return ResponseEntity.ok(ApiResponse.success("User account reactivated successfully", null));
    }

    // ==================== LEAVE ENTITLEMENTS ====================

    @PutMapping("/{id}/leave-entitlements")
    @PreAuthorize("hasRole('EMPLOYEE') and @securityUtils.canManageEmployees()")
    public ResponseEntity<ApiResponse<EmployeeUserResponse>> updateLeaveEntitlements(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> entitlements) {
        EmployeeUserResponse response = employeeService.updateLeaveEntitlements(id, entitlements);
        return ResponseEntity.ok(ApiResponse.success("Leave entitlements updated successfully", response));
    }

    @PostMapping("/bulk-leave-entitlements")
    @PreAuthorize("hasRole('EMPLOYEE') and @securityUtils.canManageEmployees()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> bulkUpdateLeaveEntitlements(
            @RequestBody Map<String, Object> request) {
        Long departmentId = Long.valueOf(request.get("departmentId").toString());
        String leaveType = (String) request.get("leaveType");
        Integer entitledDays = Integer.valueOf(request.get("entitledDays").toString());

        Map<String, Object> result = employeeService.bulkUpdateLeaveEntitlements(departmentId, leaveType, entitledDays);
        return ResponseEntity.ok(ApiResponse.success("Bulk update completed", result));
    }

    @GetMapping("/{id}/leave-balance")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getEmployeeLeaveBalance(
            @PathVariable Long id,
            @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(ApiResponse.success(employeeService.getLeaveBalance(id, year)));
    }

    // ==================== PROFILE ====================

    @PatchMapping("/{id}/profile")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<EmployeeUserResponse>> updateProfile(
            @PathVariable Long id,
            @Valid @RequestBody ProfileUpdateRequest request) {
        EmployeeUserResponse response = employeeService.updateProfile(id, request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated", response));
    }
}
