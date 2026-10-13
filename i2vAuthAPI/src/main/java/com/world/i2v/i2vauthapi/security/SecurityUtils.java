package com.world.i2v.i2vauthapi.security;

import com.world.i2v.i2vauthapi.Config.ModuleRouteConfig;
import com.world.i2v.i2vauthapi.entity.Department;
import com.world.i2v.i2vauthapi.entity.Designation;
import com.world.i2v.i2vauthapi.entity.EmployeeUser;
import com.world.i2v.i2vauthapi.repository.EmployeeUserRepository;
import com.world.i2v.i2vauthapi.service.PermissionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Component("securityUtils")
@Transactional(readOnly = true)
public class SecurityUtils {

    private final PermissionService permissionService;
    private final EmployeeUserRepository employeeRepository;
    private final ModuleRouteConfig moduleRouteConfig;

    public SecurityUtils(PermissionService permissionService,
                         EmployeeUserRepository employeeRepository,
                         ModuleRouteConfig moduleRouteConfig) {
        this.permissionService = permissionService;
        this.employeeRepository = employeeRepository;
        this.moduleRouteConfig = moduleRouteConfig;
    }

    // ==================== DYNAMIC PERMISSION CHECKS ====================

    public boolean hasModulePermission(String moduleCode, String accessLevel) {
        return moduleRouteConfig.getRouteInfo(moduleCode)
                .map(info -> checkPermissionWithStrictFallback(accessLevel, info.getAllRoutes()))
                .orElse(false);
    }

    public boolean hasRoutePermission(String routePath, String accessLevel) {
        if (!moduleRouteConfig.isValidRoute(routePath)) {
            return false;
        }
        return checkPermissionWithStrictFallback(accessLevel, routePath);
    }

    public boolean hasGroupPermission(String groupName, String accessLevel) {
        java.util.List<String> routes = moduleRouteConfig.getRoutesByGroup(groupName);
        if (routes.isEmpty()) {
            return false;
        }
        return checkPermissionWithStrictFallback(accessLevel, routes.toArray(new String[0]));
    }

    // ==================== CONVENIENCE METHODS ====================

    public boolean canViewEmployees() {
        return hasModulePermission("EMPLOYEES", "read") ||
                hasGroupPermission("employees", "read");
    }

    public boolean canManageEmployees() {
        return hasModulePermission("EMPLOYEES", "write") ||
                hasGroupPermission("employees", "write");
    }

    public boolean canManageUsers() {
        return hasModulePermission("USERS", "admin") ||
                hasModulePermission("USER_MGMT", "admin") ||
                hasGroupPermission("users", "admin");
    }

    public boolean canReadDepartments() {
        return hasModulePermission("DEPARTMENTS", "read") ||
                hasGroupPermission("departments", "read");
    }

    public boolean canManageDepartments() {
        return hasModulePermission("DEPARTMENTS", "write") ||
                hasGroupPermission("departments", "write");
    }

    public boolean canReadDesignations() {
        return hasModulePermission("DESIGNATIONS", "read") ||
                hasGroupPermission("designations", "read");
    }

    public boolean canManageDesignations() {
        return hasModulePermission("DESIGNATIONS", "write") ||
                hasGroupPermission("designations", "write");
    }

    public boolean canReadModules() {
        return hasModulePermission("MODULES", "read") ||
                hasGroupPermission("modules", "read");
    }

    public boolean canManageModules() {
        return hasModulePermission("MODULES", "write") ||
                hasGroupPermission("modules", "write");
    }

    public boolean canManageModuleAccess() {
        return hasModulePermission("MODULE_ACCESS", "admin") ||
                hasModulePermission("PERMISSIONS", "admin") ||
                hasGroupPermission("module-access", "admin");
    }

    public boolean canReadAttendance() {
        if (hasModulePermission("ATTENDANCE", "read") ||
                hasModulePermission("ATTENDANCE_MANAGEMENT", "read") ||
                hasGroupPermission("attendance", "read")) {
            return true;
        }

        // Production safety fallback: if no explicit attendance permission entry exists yet,
        // authenticated employees should still be able to view their own attendance history
        // and summary instead of being blocked by empty/uncached module access data.
        Long currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            return false;
        }

        return employeeRepository.findByUserId(currentUserId).isPresent();
    }

    public boolean canManageAttendance() {
        return hasModulePermission("ATTENDANCE", "write") ||
                hasModulePermission("ATTENDANCE_MANAGEMENT", "write") ||
                hasGroupPermission("attendance", "write");
    }

    public boolean canReadLeaves() {
        return hasModulePermission("LEAVES", "read") ||
                hasGroupPermission("leaves", "read");
    }

    public boolean canManageLeaves() {
        return hasModulePermission("LEAVES", "write") ||
                hasGroupPermission("leaves", "write");
    }

    // ==================== CORE PERMISSION LOGIC (FIXED) ====================

    /**
     * FIXED: Check each route independently
     * Returns true if ANY route grants access
     */
    private boolean checkPermissionWithStrictFallback(String accessLevel, String... routes) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return false;
        }

        // If no routes specified, check managerial power
        if (routes == null || routes.length == 0) {
            return hasManagerialPower();
        }

        // Check each route independently
        for (String route : routes) {
            // Check explicit ALLOW for this specific route
            Boolean explicitAllow = hasExplicitAllowForRoute(accessLevel, route);
            if (explicitAllow != null && explicitAllow) {
                log.debug("ACCESS GRANTED for route: {} (user: {})", route, userId);
                return true;
            }
        }

        // No explicit ALLOW found - check if user has managerial power
        if (hasManagerialPower()) {
            log.debug("ACCESS GRANTED by managerial power (user: {})", userId);
            return true;
        }

        log.debug("ACCESS DENIED: No routes have access (user: {})", userId);
        return false;
    }

    /**
     * Check if a SINGLE route has explicit ALLOW
     */
    private Boolean hasExplicitAllowForRoute(String accessLevel, String route) {
        Long userId = getCurrentUserId();
        if (userId == null || route == null) {
            return null;
        }

        Map<String, Object> permissions = permissionService.getUserPermissions(userId);
        Object modulesByRouteObj = permissions.get("modulesByRoute");

        if (!(modulesByRouteObj instanceof Map)) {
            return null;
        }

        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> modulesByRoute =
                (Map<String, Map<String, Object>>) modulesByRouteObj;

        String normalizedRoute = permissionService.normalizeRoutePathPublic(route);
        Map<String, Object> modulePerms = modulesByRoute.get(normalizedRoute);

        if (modulePerms != null) {
            String accessLevelName = (String) modulePerms.get("accessLevelName");

            // ONLY check this specific module - ignore NONE for other modules!
            if (!"NONE".equals(accessLevelName) &&
                    checkAccessLevel(modulePerms, accessLevel)) {
                return true;
            }
        }
        return null;
    }

    /**
     * Check if a SINGLE route has explicit DENY
     */
    private Boolean hasExplicitDenyForRoute(String route) {
        Long userId = getCurrentUserId();
        if (userId == null || route == null) {
            return null;
        }

        Map<String, Object> permissions = permissionService.getUserPermissions(userId);
        Object modulesByRouteObj = permissions.get("modulesByRoute");

        if (!(modulesByRouteObj instanceof Map)) {
            return null;
        }

        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> modulesByRoute =
                (Map<String, Map<String, Object>>) modulesByRouteObj;

        String normalizedRoute = permissionService.normalizeRoutePathPublic(route);
        Map<String, Object> modulePerms = modulesByRoute.get(normalizedRoute);

        if (modulePerms != null) {
            String accessLevelName = (String) modulePerms.get("accessLevelName");
            if ("NONE".equals(accessLevelName)) {
                return true;
            }
        }
        return null;
    }

    private boolean checkAccessLevel(Map<String, Object> modulePerms, String requiredAccess) {
        String accessLevelName = (String) modulePerms.get("accessLevelName");
        if (accessLevelName == null) return false;

        switch (requiredAccess.toLowerCase()) {
            case "read":
                Boolean canRead = (Boolean) modulePerms.get("read");
                return Boolean.TRUE.equals(canRead) ||
                        "WRITE".equals(accessLevelName) ||
                        "ADMIN".equals(accessLevelName);
            case "write":
            case "create":
            case "update":
                Boolean canCreate = (Boolean) modulePerms.get("create");
                Boolean canUpdate = (Boolean) modulePerms.get("update");
                return Boolean.TRUE.equals(canCreate) ||
                        Boolean.TRUE.equals(canUpdate) ||
                        "WRITE".equals(accessLevelName) ||
                        "ADMIN".equals(accessLevelName);
            case "delete":
                Boolean canDelete = (Boolean) modulePerms.get("delete");
                return Boolean.TRUE.equals(canDelete) ||
                        "ADMIN".equals(accessLevelName);
            case "approve":
                Boolean canApprove = (Boolean) modulePerms.get("approve");
                return Boolean.TRUE.equals(canApprove) ||
                        "ADMIN".equals(accessLevelName);
            case "admin":
                return "ADMIN".equals(accessLevelName);
            default:
                return false;
        }
    }

    public boolean hasManagerialPower() {
        EmployeeUser employee = getCurrentEmployee();
        if (employee == null) {
            return false;
        }

        Designation designation = employee.getDesignation();
        if (designation != null && Boolean.TRUE.equals(designation.getIsManagerial())) {
            return true;
        }

        Department department = employee.getDepartment();
        if (department != null && department.getManager() != null) {
            Long currentUserId = getCurrentUserId();
            if (currentUserId != null && currentUserId.equals(department.getManager().getId())) {
                return true;
            }
        }

        return false;
    }

    public boolean isCurrentUser(Long userId) {
        Long currentUserId = getCurrentUserId();
        return currentUserId != null && currentUserId.equals(userId);
    }

    public Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() &&
                auth.getPrincipal() instanceof UserDetailsImpl) {
            return ((UserDetailsImpl) auth.getPrincipal()).getId();
        }
        return null;
    }

    private EmployeeUser getCurrentEmployee() {
        Long currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            return null;
        }
        return employeeRepository.findByUserId(currentUserId).orElse(null);
    }
}