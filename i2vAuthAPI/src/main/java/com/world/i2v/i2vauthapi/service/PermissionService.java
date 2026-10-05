package com.world.i2v.i2vauthapi.service;

import com.world.i2v.i2vauthapi.entity.Department;
import com.world.i2v.i2vauthapi.entity.DepartmentModule;
import com.world.i2v.i2vauthapi.entity.Designation;
import com.world.i2v.i2vauthapi.entity.DesignationModuleAccess;
import com.world.i2v.i2vauthapi.entity.EmployeeUser;
import com.world.i2v.i2vauthapi.entity.User;
import com.world.i2v.i2vauthapi.repository.DepartmentModuleRepository;
import com.world.i2v.i2vauthapi.repository.DesignationModuleAccessRepository;
import com.world.i2v.i2vauthapi.repository.EmployeeUserRepository;
import com.world.i2v.i2vauthapi.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class PermissionService {

    private final DesignationModuleAccessRepository accessRepository;
    private final DepartmentModuleRepository departmentModuleRepository;
    private final EmployeeUserRepository employeeRepository;
    private final UserRepository userRepository;

    public PermissionService(DesignationModuleAccessRepository accessRepository,
                             DepartmentModuleRepository departmentModuleRepository,
                             EmployeeUserRepository employeeRepository,
                             UserRepository userRepository) {
        this.accessRepository = accessRepository;
        this.departmentModuleRepository = departmentModuleRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "userPermissions", key = "#userId")
    public Map<String, Object> getUserPermissions(Long userId) {
        Map<String, Object> permissions = new HashMap<>();
        Map<String, Map<String, Object>> modulePermissions = new HashMap<>();
        Map<String, Map<String, Object>> modulePermissionsByRoute = new HashMap<>();

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("getUserPermissions: no user found for userId={}", userId);
            return permissions;
        }

        EmployeeUser employee = employeeRepository.findByUserId(userId).orElse(null);
        if (employee == null) {
            log.warn("getUserPermissions: no employee profile found for userId={} (email={})",
                    userId, user.getEmail());
            permissions.put("isEmployee", false);
            permissions.put("message", "No employee profile found");
            return permissions;
        }

        Department department = employee.getDepartment();
        Designation designation = employee.getDesignation();

        if (department == null || designation == null) {
            log.warn("getUserPermissions: employeeId={} (userId={}) has missing department={} or designation={}",
                    employee.getId(), userId, department != null, designation != null);
            permissions.put("isEmployee", true);
            permissions.put("hasDepartment", department != null);
            permissions.put("hasDesignation", designation != null);
            return permissions;
        }

        List<DepartmentModule> deptModules = departmentModuleRepository
                .findByDepartmentIdAndIsActive(department.getId(), true);

        List<DesignationModuleAccess> accesses = accessRepository
                .findByDesignationIdAndIsActive(designation.getId(), true);

        log.info("getUserPermissions: userId={} employeeId={} employeeCode={} departmentId={} designationId={} activeDeptModules={} designationAccesses={}",
                userId, employee.getId(), employee.getEmployeeCode(),
                department.getId(), designation.getId(), deptModules.size(), accesses.size());

        Map<Long, DesignationModuleAccess> accessMap = new HashMap<>();
        for (DesignationModuleAccess access : accesses) {
            if (access != null && access.getModule() != null) {
                accessMap.put(access.getModule().getId(), access);
            }
        }

        Set<Long> processedModuleIds = new LinkedHashSet<>();

        for (DepartmentModule dm : deptModules) {
            com.world.i2v.i2vauthapi.entity.Module module = dm.getModule();
            if (module == null) {
                continue;
            }
            addModulePermissions(module, accessMap.get(module.getId()), processedModuleIds,
                    modulePermissions, modulePermissionsByRoute);
        }

        // Repair: include modules granted to the designation even when the module was never
        // assigned to the department (department_module row missing). An existing but inactive
        // department_module row means the module was deliberately removed, so it stays excluded.
        Map<Long, Boolean> deptModuleStates = new HashMap<>();
        List<DepartmentModule> allDeptModules = departmentModuleRepository
                .findByDepartmentId(department.getId());
        if (allDeptModules != null) {
            for (DepartmentModule dm : allDeptModules) {
                if (dm != null && dm.getModule() != null) {
                    deptModuleStates.put(dm.getModule().getId(),
                            Boolean.TRUE.equals(dm.getIsActive()));
                }
            }
        }

        for (DesignationModuleAccess access : accesses) {
            if (access == null || access.getModule() == null) {
                continue;
            }
            Long moduleId = access.getModule().getId();
            if (deptModuleStates.containsKey(moduleId)) {
                continue;
            }
            addModulePermissions(access.getModule(), access, processedModuleIds,
                    modulePermissions, modulePermissionsByRoute);
        }

        permissions.put("isEmployee", true);
        permissions.put("userId", userId);
        permissions.put("employeeCode", employee.getEmployeeCode());
        permissions.put("departmentId", department.getId());
        permissions.put("departmentName", department.getName());
        permissions.put("designationId", designation.getId());
        permissions.put("designationTitle", designation.getTitle());
        permissions.put("modules", modulePermissions);
        permissions.put("modulesByRoute", modulePermissionsByRoute);
        permissions.put("moduleList", new ArrayList<>(modulePermissions.values()));
        permissions.put("role", user.getRole().name());

        List<String> routeKeys = modulePermissionsByRoute.entrySet().stream()
                .map(e -> e.getKey() + "->" + e.getValue().get("accessLevelName"))
                .toList();
        permissions.put("_debug", Map.of(
                "employeeId", employee.getId(),
                "departmentId", department.getId(),
                "designationId", designation.getId(),
                "activeDeptModules", deptModules.size(),
                "designationAccesses", accesses.size(),
                "moduleCodes", modulePermissions.keySet(),
                "routeKeys", routeKeys
        ));
        log.info("getUserPermissions: userId={} employeeId={} departmentId={} designationId={} moduleCodes={} routeKeys={}",
                userId, employee.getId(), department.getId(), designation.getId(),
                modulePermissions.keySet(), routeKeys);
        if (modulePermissions.isEmpty()) {
            log.warn("getUserPermissions: userId={} ended with ZERO module permissions. activeDeptModules={} designationAccesses={}",
                    userId, deptModules.size(), accesses.size());
        }

        return permissions;
    }

    private void addModulePermissions(com.world.i2v.i2vauthapi.entity.Module module,
                                      DesignationModuleAccess access,
                                      Set<Long> processedModuleIds,
                                      Map<String, Map<String, Object>> modulePermissions,
                                      Map<String, Map<String, Object>> modulePermissionsByRoute) {
        if (module == null || !processedModuleIds.add(module.getId())) {
            return;
        }

        Map<String, Object> perms = new HashMap<>();
        String moduleCode = module.getCode();
        String normalizedCode = moduleCode == null ? null : moduleCode.trim().toUpperCase();
        String normalizedRoutePath = normalizeRoutePath(module.getRoutePath());

        perms.put("moduleId", module.getId());
        perms.put("moduleName", module.getName());
        perms.put("moduleCode", moduleCode);
        perms.put("routePath", normalizedRoutePath);
        perms.put("description", module.getDescription());
        perms.put("icon", module.getIcon());
        perms.put("displayOrder", module.getDisplayOrder());
        perms.put("isVisible", Boolean.TRUE.equals(module.getIsVisible()));
        perms.put("isActive", Boolean.TRUE.equals(module.getIsActive()));

        boolean hasValidAccess = access != null &&
                access.getIsActive() != null &&
                access.getIsActive();

        if (hasValidAccess) {
            perms.put("read", Boolean.TRUE.equals(access.getCanRead()));
            perms.put("create", Boolean.TRUE.equals(access.getCanCreate()));
            perms.put("update", Boolean.TRUE.equals(access.getCanUpdate()));
            perms.put("delete", Boolean.TRUE.equals(access.getCanDelete()));
            perms.put("approve", Boolean.TRUE.equals(access.getCanApprove()));
            perms.put("export", Boolean.TRUE.equals(access.getCanExport()));
            perms.put("import", Boolean.TRUE.equals(access.getCanImport()));
            perms.put("accessLevel", true);
            perms.put("accessLevelName", access.getAccessLevel() != null ?
                    access.getAccessLevel().name() : "NONE");
        } else {
            perms.put("read", false);
            perms.put("create", false);
            perms.put("update", false);
            perms.put("delete", false);
            perms.put("approve", false);
            perms.put("export", false);
            perms.put("import", false);
            perms.put("accessLevel", false);
            perms.put("accessLevelName", "NONE");
        }

        if (normalizedCode != null && !normalizedCode.isBlank()) {
            modulePermissions.put(normalizedCode, perms);
        }
        if (normalizedRoutePath != null) {
            modulePermissionsByRoute.put(normalizedRoutePath, perms);
        } else {
            log.warn("getUserPermissions: module '{}' (id={}) has no route_path; route-based checks will never match it",
                    module.getName(), module.getId());
        }
    }

    public boolean hasAccess(Long userId, String moduleCode, String requiredAccess) {
        Map<String, Object> permissions = getUserPermissions(userId);

        Object isEmployee = permissions.get("isEmployee");
        if (!(isEmployee instanceof Boolean) || !(Boolean) isEmployee) {
            return false;
        }

        Object modulesObj = permissions.get("modules");
        if (!(modulesObj instanceof Map)) {
            return false;
        }

        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> modules = (Map<String, Map<String, Object>>) modulesObj;

        if (modules == null || modules.isEmpty()) {
            return false;
        }

        Map<String, Object> modulePerms = findModulePermissionsByCode(modules, moduleCode);
        if (modulePerms == null) {
            return false;
        }

        return hasRequiredAccess(modulePerms, requiredAccess);
    }

    public boolean hasAccessByRoute(Long userId, String routePath, String requiredAccess) {
        String normalizedRoutePath = normalizeRoutePath(routePath);
        if (normalizedRoutePath == null) {
            return false;
        }

        Map<String, Object> permissions = getUserPermissions(userId);

        Object isEmployee = permissions.get("isEmployee");
        if (!(isEmployee instanceof Boolean) || !(Boolean) isEmployee) {
            return false;
        }

        Object modulesByRouteObj = permissions.get("modulesByRoute");
        if (modulesByRouteObj instanceof Map<?, ?> modulesByRouteRaw) {
            @SuppressWarnings("unchecked")
            Map<String, Map<String, Object>> modulesByRoute =
                    (Map<String, Map<String, Object>>) modulesByRouteRaw;
            Map<String, Object> modulePerms = findModulePermissionsByRoute(modulesByRoute, normalizedRoutePath);
            if (modulePerms != null) {
                return hasRequiredAccess(modulePerms, requiredAccess);
            }
        }

        return false;
    }

    private boolean hasRequiredAccess(Map<String, Object> modulePerms, String requiredAccess) {
        String normalizedAccess = requiredAccess == null ? "" : requiredAccess.toLowerCase();

        switch (normalizedAccess) {
            case "read":
                return Boolean.TRUE.equals(modulePerms.get("read"));
            case "create":
            case "write":
                return Boolean.TRUE.equals(modulePerms.get("create"))
                        || Boolean.TRUE.equals(modulePerms.get("update"));
            case "update":
                return Boolean.TRUE.equals(modulePerms.get("update"));
            case "delete":
                return Boolean.TRUE.equals(modulePerms.get("delete"));
            case "approve":
                return Boolean.TRUE.equals(modulePerms.get("approve"));
            case "admin":
                Object accessLevelName = modulePerms.get("accessLevelName");
                return "ADMIN".equals(String.valueOf(accessLevelName));
            default:
                return false;
        }
    }

    private Map<String, Object> findModulePermissionsByCode(
            Map<String, Map<String, Object>> modules, String moduleCode) {
        if (modules == null || modules.isEmpty() || moduleCode == null || moduleCode.isBlank()) {
            return null;
        }

        Map<String, Object> exact = modules.get(moduleCode);
        if (exact != null) return exact;

        String normalizedCode = moduleCode.trim().toUpperCase();
        Map<String, Object> normalized = modules.get(normalizedCode);
        if (normalized != null) return normalized;

        for (Map.Entry<String, Map<String, Object>> entry : modules.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(moduleCode)) {
                return entry.getValue();
            }
        }

        return null;
    }

    private Map<String, Object> findModulePermissionsByRoute(
            Map<String, Map<String, Object>> modulesByRoute, String routePath) {
        if (modulesByRoute == null || modulesByRoute.isEmpty() ||
                routePath == null || routePath.isBlank()) {
            return null;
        }

        Map<String, Object> exact = modulesByRoute.get(routePath);
        if (exact != null) return exact;

        for (Map.Entry<String, Map<String, Object>> entry : modulesByRoute.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(routePath)) {
                return entry.getValue();
            }
        }

        return null;
    }

    private String normalizeRoutePath(String routePath) {
        if (routePath == null) return null;

        String normalized = routePath.trim();
        if (normalized.isEmpty()) return null;

        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }

        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        return normalized.toLowerCase();
    }

    // Public utility methods
//    public String normalizeRoutePathPublic(String routePath) {
//        return normalizeRoutePath(routePath);
//    }
//
//    public boolean hasExplicitPermission(Long userId, String routePath, String requiredAccess) {
//        String normalizedRoutePath = normalizeRoutePath(routePath);
//        if (normalizedRoutePath == null) return false;
//
//        Map<String, Object> permissions = getUserPermissions(userId);
//
//        Object modulesByRouteObj = permissions.get("modulesByRoute");
//        if (!(modulesByRouteObj instanceof Map)) return false;
//
//        @SuppressWarnings("unchecked")
//        Map<String, Map<String, Object>> modulesByRoute =
//                (Map<String, Map<String, Object>>) modulesByRouteObj;
//
//        Map<String, Object> modulePerms = modulesByRoute.get(normalizedRoutePath);
//        if (modulePerms == null) return false;
//
//        return hasRequiredAccess(modulePerms, requiredAccess);
//    }

    public boolean hasExplicitDeny(Long userId, String routePath) {
        String normalizedRoutePath = normalizeRoutePath(routePath);
        if (normalizedRoutePath == null) return false;

        Map<String, Object> permissions = getUserPermissions(userId);

        Object modulesByRouteObj = permissions.get("modulesByRoute");
        if (!(modulesByRouteObj instanceof Map)) return false;

        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> modulesByRoute =
                (Map<String, Map<String, Object>>) modulesByRouteObj;

        Map<String, Object> modulePerms = modulesByRoute.get(normalizedRoutePath);
        if (modulePerms == null) return false;

        String accessLevelName = (String) modulePerms.get("accessLevelName");
        return "NONE".equals(accessLevelName);
    }

    @CacheEvict(value = "userPermissions", allEntries = true)
    public void clearAllCache() {
        log.info("Cleared all permission caches");
    }

    @CacheEvict(value = "userPermissions", allEntries = true)
    public void clearCacheForDepartment(Long departmentId) {
        log.info("Cleared permission cache for department: {}", departmentId);
    }

    @CacheEvict(value = "userPermissions", allEntries = true)
    public void clearCacheForDesignation(Long designationId) {
        log.info("Cleared permission cache for designation: {}", designationId);
    }

    @CacheEvict(value = "userPermissions", key = "#userId")
    public void clearCacheForUser(Long userId) {
        log.info("Cleared permission cache for user: {}", userId);
    }
    // Add to existing PermissionService.java

    /**
     * Public wrapper for normalizeRoutePath
     */
    public String normalizeRoutePathPublic(String routePath) {
        return normalizeRoutePath(routePath);
    }

    /**
     * Check if user has EXPLICIT permission for a route
     */
    public boolean hasExplicitPermission(Long userId, String routePath, String requiredAccess) {
        String normalizedRoutePath = normalizeRoutePath(routePath);
        if (normalizedRoutePath == null) {
            return false;
        }

        Map<String, Object> permissions = getUserPermissions(userId);

        Object modulesByRouteObj = permissions.get("modulesByRoute");
        if (!(modulesByRouteObj instanceof Map)) {
            return false;
        }

        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> modulesByRoute =
                (Map<String, Map<String, Object>>) modulesByRouteObj;

        Map<String, Object> modulePerms = modulesByRoute.get(normalizedRoutePath);
        if (modulePerms == null) {
            return false;
        }

        return hasRequiredAccess(modulePerms, requiredAccess);
    }
}