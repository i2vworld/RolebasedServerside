package com.world.i2v.i2vauthapi.service;

import com.world.i2v.i2vauthapi.dto.request.DesignationModuleAccessRequest;
import com.world.i2v.i2vauthapi.dto.response.DesignationModuleAccessResponse;
import com.world.i2v.i2vauthapi.dto.response.ModuleResponse;
import com.world.i2v.i2vauthapi.entity.*;
import com.world.i2v.i2vauthapi.exception.BadRequestException;
import com.world.i2v.i2vauthapi.exception.ResourceNotFoundException;
import com.world.i2v.i2vauthapi.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.world.i2v.i2vauthapi.entity.Module;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
public class ModuleAccessService {

    private final DepartmentModuleRepository departmentModuleRepository;
    private final DesignationModuleAccessRepository accessRepository;
    private final DepartmentRepository departmentRepository;
    private final DesignationRepository designationRepository;
    private final ModuleRepository moduleRepository;
    private final PermissionService permissionService;
    private final EmployeeUserRepository employeeRepository;

    public ModuleAccessService(DepartmentModuleRepository departmentModuleRepository,
                               DesignationModuleAccessRepository accessRepository,
                               DepartmentRepository departmentRepository,
                               DesignationRepository designationRepository,
                               ModuleRepository moduleRepository,
                               PermissionService permissionService,
                               EmployeeUserRepository employeeRepository) {
        this.departmentModuleRepository = departmentModuleRepository;
        this.accessRepository = accessRepository;
        this.departmentRepository = departmentRepository;
        this.designationRepository = designationRepository;
        this.moduleRepository = moduleRepository;
        this.permissionService = permissionService;
        this.employeeRepository = employeeRepository;
    }

    // ==================== PUBLIC API METHODS ====================

    public List<ModuleResponse> getModulesForDepartment(Long departmentId) {
        try {
            departmentRepository.findById(departmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found"));

            List<DepartmentModule> deptModules = departmentModuleRepository
                    .findByDepartmentIdAndIsActive(departmentId, true);

            if (deptModules.isEmpty()) {
                return new ArrayList<>();
            }

            return deptModules.stream()
                    .filter(dm -> dm.getModule() != null)
                    .map(dm -> mapToModuleResponse(dm.getModule()))
                    .collect(Collectors.toList());

        } catch (ResourceNotFoundException e) {
            log.warn("Department not found: {}", departmentId);
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("Error getting modules for department {}: {}", departmentId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public List<DesignationModuleAccessResponse> getAccessForDesignation(Long designationId) {
        try {
            designationRepository.findById(designationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Designation not found"));

            List<DesignationModuleAccess> accesses = accessRepository
                    .findByDesignationIdAndIsActive(designationId, true);

            if (accesses.isEmpty()) {
                return new ArrayList<>();
            }

            return accesses.stream()
                    .map(this::mapToAccessResponse)
                    .collect(Collectors.toList());

        } catch (ResourceNotFoundException e) {
            log.warn("Designation not found: {}", designationId);
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("Error getting access for designation {}: {}", designationId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public Map<String, Object> getAccessBootstrapData() {
        Map<String, Object> data = new LinkedHashMap<>();

        List<Map<String, Object>> departments = departmentRepository.findAll().stream()
                .map(this::mapDepartmentOption)
                .collect(Collectors.toList());

        List<Map<String, Object>> designations = designationRepository.findAll().stream()
                .map(this::mapDesignationOption)
                .collect(Collectors.toList());

        List<ModuleResponse> modules = moduleRepository.findAll().stream()
                .map(this::mapToModuleResponse)
                .collect(Collectors.toList());

        data.put("departments", departments);
        data.put("designations", designations);
        data.put("modules", modules);

        return data;
    }

    /**
     * Get user permissions without any auto-fix.
     * Permissions are strictly based on existing assignments.
     */
    public Map<String, Object> getUserPermissions(Long userId) {
        return permissionService.getUserPermissions(userId);
    }

    @Cacheable(value = "userPermissions", key = "#userId")
    public Map<String, Object> getUserPermissionsCached(Long userId) {
        return permissionService.getUserPermissions(userId);
    }

    public boolean checkUserAccess(Long userId, String moduleCode, String requiredAccess) {
        return permissionService.hasAccess(userId, moduleCode, requiredAccess);
    }

    // ==================== ADMIN-ONLY INITIALIZATION ====================

    /**
     * Admin-only method to initialize department modules with all active modules.
     * This should be called during setup or by an admin action, not automatically.
     */
    @Transactional
    @PreAuthorize("hasRole('EMPLOYEE') and @securityUtils.canManageModuleAccess()")
    @CacheEvict(value = "userPermissions", allEntries = true)
    public void initializeDepartmentModules(Long departmentId) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));

        List<Module> activeModules = moduleRepository.findByIsActive(true);
        int assignedCount = 0;

        for (Module module : activeModules) {
            Optional<DepartmentModule> existing = departmentModuleRepository
                    .findByDepartmentIdAndModuleId(departmentId, module.getId());

            if (existing.isEmpty()) {
                DepartmentModule dm = new DepartmentModule();
                dm.setDepartment(department);
                dm.setModule(module);
                dm.setIsActive(true);
                departmentModuleRepository.save(dm);
                assignedCount++;
            } else if (!existing.get().getIsActive()) {
                existing.get().setIsActive(true);
                departmentModuleRepository.save(existing.get());
                assignedCount++;
            }
        }

        log.info("Admin initialized {} modules for department '{}'", assignedCount, department.getName());
        permissionService.clearCacheForDepartment(departmentId);
    }

    /**
     * Admin-only method to grant default READ access to all department modules
     * for a specific designation.
     */
    @Transactional
    @PreAuthorize("hasRole('EMPLOYEE') and @securityUtils.canManageModuleAccess()")
    @CacheEvict(value = "userPermissions", allEntries = true)
    public void initializeDesignationAccess(Long designationId) {
        Designation designation = designationRepository.findById(designationId)
                .orElseThrow(() -> new ResourceNotFoundException("Designation not found"));

        if (designation.getDepartment() == null) {
            throw new BadRequestException("Designation is not associated with any department");
        }

        List<DepartmentModule> deptModules = departmentModuleRepository
                .findByDepartmentIdAndIsActive(designation.getDepartment().getId(), true);

        int grantedCount = 0;

        for (DepartmentModule dm : deptModules) {
            Module module = dm.getModule();
            if (module == null) continue;

            Optional<DesignationModuleAccess> existing = accessRepository
                    .findByDesignationIdAndModuleId(designationId, module.getId());

            if (existing.isEmpty()) {
                DesignationModuleAccess access = new DesignationModuleAccess();
                access.setDesignation(designation);
                access.setModule(module);
                access.setAccessLevel(AccessLevel.READ);
                access.setCanRead(true);
                access.setCanCreate(false);
                access.setCanUpdate(false);
                access.setCanDelete(false);
                access.setCanApprove(false);
                access.setIsActive(true);
                accessRepository.save(access);
                grantedCount++;
            }
        }

        log.info("Admin granted READ access to {} modules for designation '{}'",
                grantedCount, designation.getTitle());
        permissionService.clearCacheForDesignation(designationId);
    }

    // ==================== WRITE OPERATIONS ====================

    @Transactional
    @CacheEvict(value = "userPermissions", allEntries = true)
    public void assignModulesToDepartment(Long departmentId, List<Long> moduleIds) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));

        String currentUser = getCurrentUserEmail();

        // Deactivate existing assignments
        List<DepartmentModule> existingModules = departmentModuleRepository
                .findByDepartmentIdAndIsActive(departmentId, true);
        for (DepartmentModule dm : existingModules) {
            dm.setIsActive(false);
            departmentModuleRepository.save(dm);
        }

        // Create new assignments
        for (Long moduleId : moduleIds) {
            Module module = moduleRepository.findById(moduleId)
                    .orElseThrow(() -> new ResourceNotFoundException("Module not found: " + moduleId));

            Optional<DepartmentModule> existing = departmentModuleRepository
                    .findByDepartmentIdAndModuleId(departmentId, moduleId);

            if (existing.isPresent()) {
                existing.get().setIsActive(true);
                departmentModuleRepository.save(existing.get());
            } else {
                DepartmentModule dm = new DepartmentModule();
                dm.setDepartment(department);
                dm.setModule(module);
                dm.setIsActive(true);
                departmentModuleRepository.save(dm);
            }
        }

        log.info("Assigned {} modules to department: {} by {}", moduleIds.size(), department.getName(), currentUser);
        permissionService.clearCacheForDepartment(departmentId);
    }

    @Transactional
    @CacheEvict(value = "userPermissions", allEntries = true)
    public void assignAccessToDesignation(DesignationModuleAccessRequest request) {
        Designation designation = designationRepository.findById(request.getDesignationId())
                .orElseThrow(() -> new ResourceNotFoundException("Designation not found"));

        Module module = moduleRepository.findById(request.getModuleId())
                .orElseThrow(() -> new ResourceNotFoundException("Module not found"));

        String currentUser = getCurrentUserEmail();

        AccessLevel accessLevel;
        try {
            accessLevel = AccessLevel.valueOf(request.getAccessLevel().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid access level. Use: NONE, READ, WRITE, ADMIN");
        }

        DesignationModuleAccess access = accessRepository
                .findByDesignationIdAndModuleId(request.getDesignationId(), request.getModuleId())
                .orElse(new DesignationModuleAccess());

        access.setDesignation(designation);
        access.setModule(module);
        access.setAccessLevel(accessLevel);
        access.setCanRead(accessLevel != AccessLevel.NONE);
        access.setCanCreate(accessLevel == AccessLevel.WRITE || accessLevel == AccessLevel.ADMIN);
        access.setCanUpdate(accessLevel == AccessLevel.WRITE || accessLevel == AccessLevel.ADMIN);
        access.setCanDelete(accessLevel == AccessLevel.ADMIN);
        access.setCanApprove(accessLevel == AccessLevel.ADMIN);
        access.setIsActive(true);

        accessRepository.save(access);

        if (accessLevel != AccessLevel.NONE && designation.getDepartment() != null) {
            Department department = designation.getDepartment();
            Optional<DepartmentModule> existingDeptModule = departmentModuleRepository
                    .findByDepartmentIdAndModuleId(department.getId(), module.getId());

            if (existingDeptModule.isPresent()) {
                if (!Boolean.TRUE.equals(existingDeptModule.get().getIsActive())) {
                    existingDeptModule.get().setIsActive(true);
                    departmentModuleRepository.save(existingDeptModule.get());
                }
            } else {
                DepartmentModule dm = new DepartmentModule();
                dm.setDepartment(department);
                dm.setModule(module);
                dm.setIsActive(true);
                departmentModuleRepository.save(dm);
            }

            log.info("Ensured module '{}' is assigned to department '{}' after granting {} access to designation '{}'",
                    module.getName(), department.getName(), accessLevel, designation.getTitle());
        }

        log.info("Assigned {} access for designation: {} on module: {} by {}",
                accessLevel, designation.getTitle(), module.getName(), currentUser);

        permissionService.clearCacheForDesignation(request.getDesignationId());
    }

    @Transactional
    @CacheEvict(value = "userPermissions", allEntries = true)
    public void removeModuleFromDepartment(Long departmentId, Long moduleId) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));

        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Module not found"));

        departmentModuleRepository.findByDepartmentIdAndModuleId(departmentId, moduleId)
                .ifPresent(dm -> {
                    dm.setIsActive(false);
                    departmentModuleRepository.save(dm);
                    log.info("Removed module {} from department {} by {}",
                            module.getName(), department.getName(), getCurrentUserEmail());
                });

        permissionService.clearCacheForDepartment(departmentId);
    }

    @Transactional
    @CacheEvict(value = "userPermissions", allEntries = true)
    public void revokeAccessFromDesignation(Long designationId, Long moduleId) {
        Designation designation = designationRepository.findById(designationId)
                .orElseThrow(() -> new ResourceNotFoundException("Designation not found"));

        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Module not found"));

        accessRepository.findByDesignationIdAndModuleId(designationId, moduleId).ifPresent(access -> {
            access.setIsActive(false);
            accessRepository.save(access);
            log.info("Revoked access for designation: {} on module: {} by {}",
                    designation.getTitle(), module.getName(), getCurrentUserEmail());
        });

        permissionService.clearCacheForDesignation(designationId);
    }

    // ==================== HELPER METHODS ====================

    private String getCurrentUserEmail() {
        try {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            return "SYSTEM";
        }
    }

    private ModuleResponse mapToModuleResponse(Module module) {
        if (module == null) return null;

        return ModuleResponse.builder()
                .id(module.getId())
                .name(module.getName())
                .code(module.getCode())
                .description(module.getDescription())
                .icon(module.getIcon())
                .routePath(module.getRoutePath())
                .displayOrder(module.getDisplayOrder())
                .isActive(module.getIsActive())
                .isVisible(module.getIsVisible())
                .createdAt(module.getCreatedAt())
                .updatedAt(module.getUpdatedAt())
                .parentModuleId(module.getParentModule() != null ? module.getParentModule().getId() : null)
                .parentModuleName(module.getParentModule() != null ? module.getParentModule().getName() : null)
                .build();
    }

    private Map<String, Object> mapDepartmentOption(Department department) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", department.getId());
        item.put("name", department.getName());
        item.put("code", department.getCode());
        item.put("isActive", department.getIsActive());
        return item;
    }

    private Map<String, Object> mapDesignationOption(Designation designation) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", designation.getId());
        item.put("title", designation.getTitle());
        item.put("code", designation.getCode());
        item.put("isActive", designation.getIsActive());
        item.put("isManagerial", designation.getIsManagerial());
        if (designation.getDepartment() != null) {
            item.put("departmentId", designation.getDepartment().getId());
            item.put("departmentName", designation.getDepartment().getName());
        } else {
            item.put("departmentId", null);
            item.put("departmentName", null);
        }
        return item;
    }

    private DesignationModuleAccessResponse mapToAccessResponse(DesignationModuleAccess access) {
        if (access == null) return null;

        return DesignationModuleAccessResponse.builder()
                .id(access.getId())
                .designationId(access.getDesignation() != null ? access.getDesignation().getId() : null)
                .designationTitle(access.getDesignation() != null ? access.getDesignation().getTitle() : null)
                .module(mapToModuleResponse(access.getModule()))
                .accessLevel(access.getAccessLevel() != null ? access.getAccessLevel().name() : "NONE")
                .canCreate(access.getCanCreate() != null ? access.getCanCreate() : false)
                .canRead(access.getCanRead() != null ? access.getCanRead() : false)
                .canUpdate(access.getCanUpdate() != null ? access.getCanUpdate() : false)
                .canDelete(access.getCanDelete() != null ? access.getCanDelete() : false)
                .canApprove(access.getCanApprove() != null ? access.getCanApprove() : false)
                .isActive(access.getIsActive() != null ? access.getIsActive() : false)
                .build();
    }
}