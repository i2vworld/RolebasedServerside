package com.world.i2v.i2vauthapi.security;

import com.world.i2v.i2vauthapi.Config.ModuleRouteConfig;
import com.world.i2v.i2vauthapi.entity.EmployeeUser;
import com.world.i2v.i2vauthapi.repository.EmployeeUserRepository;
import com.world.i2v.i2vauthapi.service.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityUtilsTest {

    private PermissionService permissionService;
    private EmployeeUserRepository employeeUserRepository;
    private ModuleRouteConfig moduleRouteConfig;
    private SecurityUtils securityUtils;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        permissionService = mock(PermissionService.class);
        employeeUserRepository = mock(EmployeeUserRepository.class);
        moduleRouteConfig = mock(ModuleRouteConfig.class);
        securityUtils = new SecurityUtils(permissionService, employeeUserRepository, moduleRouteConfig);
    }

    @Test
    void canReadAttendance_shouldAllowAuthenticatedEmployeeWhenNoExplicitAttendancePermissionsExist() {
        Long userId = 99L;
        UserDetailsImpl principal = new UserDetailsImpl(
                userId,
                "emp@example.com",
                "Test Employee",
                "password",
                "EMPLOYEE",
                true,
                List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        when(moduleRouteConfig.getRouteInfo("ATTENDANCE")).thenReturn(Optional.empty());
        when(moduleRouteConfig.getRouteInfo("ATTENDANCE_MANAGEMENT")).thenReturn(Optional.empty());
        when(moduleRouteConfig.getRoutesByGroup("attendance")).thenReturn(Collections.emptyList());
        when(employeeUserRepository.findByUserId(userId)).thenReturn(Optional.of(new EmployeeUser()));

        assertTrue(securityUtils.canReadAttendance());
    }
}
