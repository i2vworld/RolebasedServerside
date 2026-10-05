package com.world.i2v.i2vauthapi.service;

import com.world.i2v.i2vauthapi.dto.request.DesignationModuleAccessRequest;
import com.world.i2v.i2vauthapi.dto.response.DesignationModuleAccessResponse;
import com.world.i2v.i2vauthapi.dto.response.ModuleResponse;
import com.world.i2v.i2vauthapi.entity.*;
import com.world.i2v.i2vauthapi.entity.Module;
import com.world.i2v.i2vauthapi.exception.BadRequestException;
import com.world.i2v.i2vauthapi.exception.ResourceNotFoundException;
import com.world.i2v.i2vauthapi.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ModuleAccessServiceTest {

    private DepartmentModuleRepository departmentModuleRepository;
    private DesignationModuleAccessRepository accessRepository;
    private DepartmentRepository departmentRepository;
    private DesignationRepository designationRepository;
    private ModuleRepository moduleRepository;
    private PermissionService permissionService;
    private EmployeeUserRepository employeeRepository;
    private ModuleAccessService moduleAccessService;

    @BeforeEach
    void setUp() {
        departmentModuleRepository = mock(DepartmentModuleRepository.class);
        accessRepository = mock(DesignationModuleAccessRepository.class);
        departmentRepository = mock(DepartmentRepository.class);
        designationRepository = mock(DesignationRepository.class);
        moduleRepository = mock(ModuleRepository.class);
        permissionService = mock(PermissionService.class);
        employeeRepository = mock(EmployeeUserRepository.class);
        moduleAccessService = new ModuleAccessService(departmentModuleRepository, accessRepository,
                departmentRepository, designationRepository, moduleRepository, permissionService, employeeRepository);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Department buildDepartment(Long id, String name) {
        Department department = new Department();
        department.setId(id);
        department.setName(name);
        department.setCode(name.toUpperCase());
        department.setIsActive(true);
        return department;
    }

    private Designation buildDesignation(Long id, String title, Department department) {
        Designation designation = new Designation();
        designation.setId(id);
        designation.setTitle(title);
        designation.setCode(title.toUpperCase());
        designation.setIsActive(true);
        designation.setIsManagerial(false);
        designation.setDepartment(department);
        return designation;
    }

    private Module buildModule(Long id, String name, String code) {
        Module module = new Module();
        module.setId(id);
        module.setName(name);
        module.setCode(code);
        module.setIsActive(true);
        module.setIsVisible(true);
        return module;
    }

    private DepartmentModule buildDepartmentModule(Long id, Department department, Module module) {
        DepartmentModule dm = new DepartmentModule();
        dm.setId(id);
        dm.setDepartment(department);
        dm.setModule(module);
        dm.setIsActive(true);
        return dm;
    }

    private DesignationModuleAccess buildAccess(Long id, Designation designation, Module module) {
        DesignationModuleAccess access = new DesignationModuleAccess();
        access.setId(id);
        access.setDesignation(designation);
        access.setModule(module);
        access.setAccessLevel(AccessLevel.READ);
        access.setCanRead(true);
        access.setIsActive(true);
        return access;
    }

    // ---------- Smoke test ----------

    @Test
    void smokeTest_serviceCanBeConstructed() {
        assertNotNull(moduleAccessService);
    }

    // ---------- getModulesForDepartment ----------

    @Test
    void getModulesForDepartment_departmentNotFound_returnsEmptyList() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.empty());

        List<ModuleResponse> result = moduleAccessService.getModulesForDepartment(1L);

        assertTrue(result.isEmpty());
    }

    @Test
    void getModulesForDepartment_noModules_returnsEmptyList() {
        Department department = buildDepartment(1L, "IT");
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(departmentModuleRepository.findByDepartmentIdAndIsActive(1L, true)).thenReturn(Collections.emptyList());

        List<ModuleResponse> result = moduleAccessService.getModulesForDepartment(1L);

        assertTrue(result.isEmpty());
    }

    @Test
    void getModulesForDepartment_success_returnsMappedModules() {
        Department department = buildDepartment(1L, "IT");
        Module module = buildModule(2L, "Employees", "EMP");
        DepartmentModule dm = buildDepartmentModule(3L, department, module);

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(departmentModuleRepository.findByDepartmentIdAndIsActive(1L, true)).thenReturn(List.of(dm));

        List<ModuleResponse> result = moduleAccessService.getModulesForDepartment(1L);

        assertEquals(1, result.size());
        assertEquals("EMP", result.get(0).getCode());
    }

    @Test
    void getModulesForDepartment_skipsNullModuleReferences() {
        Department department = buildDepartment(1L, "IT");
        DepartmentModule dm = buildDepartmentModule(3L, department, null);

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(departmentModuleRepository.findByDepartmentIdAndIsActive(1L, true)).thenReturn(List.of(dm));

        List<ModuleResponse> result = moduleAccessService.getModulesForDepartment(1L);

        assertTrue(result.isEmpty());
    }

    @Test
    void getModulesForDepartment_repositoryThrows_returnsEmptyList() {
        when(departmentRepository.findById(1L)).thenThrow(new RuntimeException("db error"));

        List<ModuleResponse> result = moduleAccessService.getModulesForDepartment(1L);

        assertTrue(result.isEmpty());
    }

    // ---------- getAccessForDesignation ----------

    @Test
    void getAccessForDesignation_designationNotFound_returnsEmptyList() {
        when(designationRepository.findById(1L)).thenReturn(Optional.empty());

        List<DesignationModuleAccessResponse> result = moduleAccessService.getAccessForDesignation(1L);

        assertTrue(result.isEmpty());
    }

    @Test
    void getAccessForDesignation_success_returnsMappedAccess() {
        Designation designation = buildDesignation(1L, "Manager", null);
        Module module = buildModule(2L, "Employees", "EMP");
        DesignationModuleAccess access = buildAccess(3L, designation, module);

        when(designationRepository.findById(1L)).thenReturn(Optional.of(designation));
        when(accessRepository.findByDesignationIdAndIsActive(1L, true)).thenReturn(List.of(access));

        List<DesignationModuleAccessResponse> result = moduleAccessService.getAccessForDesignation(1L);

        assertEquals(1, result.size());
        assertEquals("READ", result.get(0).getAccessLevel());
        assertEquals("EMP", result.get(0).getModule().getCode());
    }

    @Test
    void getAccessForDesignation_empty_returnsEmptyList() {
        Designation designation = buildDesignation(1L, "Manager", null);
        when(designationRepository.findById(1L)).thenReturn(Optional.of(designation));
        when(accessRepository.findByDesignationIdAndIsActive(1L, true)).thenReturn(Collections.emptyList());

        List<DesignationModuleAccessResponse> result = moduleAccessService.getAccessForDesignation(1L);

        assertTrue(result.isEmpty());
    }

    @Test
    void getAccessForDesignation_repositoryThrows_returnsEmptyList() {
        when(designationRepository.findById(1L)).thenThrow(new RuntimeException("db error"));

        List<DesignationModuleAccessResponse> result = moduleAccessService.getAccessForDesignation(1L);

        assertTrue(result.isEmpty());
    }

    // ---------- getAccessBootstrapData ----------

    @Test
    void getAccessBootstrapData_returnsAllOptions() {
        Department department = buildDepartment(1L, "IT");
        Designation designation = buildDesignation(2L, "Manager", department);
        Module module = buildModule(3L, "Employees", "EMP");

        when(departmentRepository.findAll()).thenReturn(List.of(department));
        when(designationRepository.findAll()).thenReturn(List.of(designation));
        when(moduleRepository.findAll()).thenReturn(List.of(module));

        Map<String, Object> data = moduleAccessService.getAccessBootstrapData();

        assertTrue(data.containsKey("departments"));
        assertTrue(data.containsKey("designations"));
        assertTrue(data.containsKey("modules"));
        assertEquals(1, ((List<?>) data.get("departments")).size());
    }

    @Test
    void getAccessBootstrapData_designationWithoutDepartment_setsNulls() {
        Designation designation = buildDesignation(2L, "Manager", null);

        when(departmentRepository.findAll()).thenReturn(Collections.emptyList());
        when(designationRepository.findAll()).thenReturn(List.of(designation));
        when(moduleRepository.findAll()).thenReturn(Collections.emptyList());

        Map<String, Object> data = moduleAccessService.getAccessBootstrapData();

        @SuppressWarnings("unchecked")
        Map<String, Object> designationOption = ((List<Map<String, Object>>) data.get("designations")).get(0);
        assertNull(designationOption.get("departmentId"));
        assertNull(designationOption.get("departmentName"));
    }

    // ---------- getUserPermissions / getUserPermissionsCached / checkUserAccess ----------

    @Test
    void getUserPermissions_delegatesToPermissionService() {
        Map<String, Object> permissions = Map.of("key", "value");
        when(permissionService.getUserPermissions(1L)).thenReturn(permissions);

        Map<String, Object> result = moduleAccessService.getUserPermissions(1L);

        assertEquals(permissions, result);
    }

    @Test
    void getUserPermissionsCached_delegatesToPermissionService() {
        Map<String, Object> permissions = Map.of("key", "value");
        when(permissionService.getUserPermissions(1L)).thenReturn(permissions);

        Map<String, Object> result = moduleAccessService.getUserPermissionsCached(1L);

        assertEquals(permissions, result);
    }

    @Test
    void checkUserAccess_delegatesToPermissionService() {
        when(permissionService.hasAccess(1L, "EMP", "READ")).thenReturn(true);

        boolean result = moduleAccessService.checkUserAccess(1L, "EMP", "READ");

        assertTrue(result);
    }

    // ---------- initializeDepartmentModules ----------

    @Test
    void initializeDepartmentModules_departmentNotFound_throwsResourceNotFoundException() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> moduleAccessService.initializeDepartmentModules(1L));
    }

    @Test
    void initializeDepartmentModules_assignsNewModules() {
        Department department = buildDepartment(1L, "IT");
        Module module = buildModule(2L, "Employees", "EMP");

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(moduleRepository.findByIsActive(true)).thenReturn(List.of(module));
        when(departmentModuleRepository.findByDepartmentIdAndModuleId(1L, 2L)).thenReturn(Optional.empty());

        moduleAccessService.initializeDepartmentModules(1L);

        verify(departmentModuleRepository).save(any(DepartmentModule.class));
        verify(permissionService).clearCacheForDepartment(1L);
    }

    @Test
    void initializeDepartmentModules_reactivatesInactiveAssignment() {
        Department department = buildDepartment(1L, "IT");
        Module module = buildModule(2L, "Employees", "EMP");
        DepartmentModule existing = buildDepartmentModule(3L, department, module);
        existing.setIsActive(false);

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(moduleRepository.findByIsActive(true)).thenReturn(List.of(module));
        when(departmentModuleRepository.findByDepartmentIdAndModuleId(1L, 2L)).thenReturn(Optional.of(existing));

        moduleAccessService.initializeDepartmentModules(1L);

        assertTrue(existing.getIsActive());
        verify(departmentModuleRepository).save(existing);
    }

    @Test
    void initializeDepartmentModules_skipsAlreadyActiveAssignment() {
        Department department = buildDepartment(1L, "IT");
        Module module = buildModule(2L, "Employees", "EMP");
        DepartmentModule existing = buildDepartmentModule(3L, department, module);

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(moduleRepository.findByIsActive(true)).thenReturn(List.of(module));
        when(departmentModuleRepository.findByDepartmentIdAndModuleId(1L, 2L)).thenReturn(Optional.of(existing));

        moduleAccessService.initializeDepartmentModules(1L);

        verify(departmentModuleRepository, never()).save(any());
    }

    // ---------- initializeDesignationAccess ----------

    @Test
    void initializeDesignationAccess_designationNotFound_throwsResourceNotFoundException() {
        when(designationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> moduleAccessService.initializeDesignationAccess(1L));
    }

    @Test
    void initializeDesignationAccess_noDepartment_throwsBadRequestException() {
        Designation designation = buildDesignation(1L, "Manager", null);
        when(designationRepository.findById(1L)).thenReturn(Optional.of(designation));

        assertThrows(BadRequestException.class, () -> moduleAccessService.initializeDesignationAccess(1L));
    }

    @Test
    void initializeDesignationAccess_grantsReadAccessToNewModules() {
        Department department = buildDepartment(1L, "IT");
        Designation designation = buildDesignation(2L, "Manager", department);
        Module module = buildModule(3L, "Employees", "EMP");
        DepartmentModule dm = buildDepartmentModule(4L, department, module);

        when(designationRepository.findById(2L)).thenReturn(Optional.of(designation));
        when(departmentModuleRepository.findByDepartmentIdAndIsActive(1L, true)).thenReturn(List.of(dm));
        when(accessRepository.findByDesignationIdAndModuleId(2L, 3L)).thenReturn(Optional.empty());

        moduleAccessService.initializeDesignationAccess(2L);

        verify(accessRepository).save(any(DesignationModuleAccess.class));
        verify(permissionService).clearCacheForDesignation(2L);
    }

    @Test
    void initializeDesignationAccess_skipsExistingAccessAndNullModules() {
        Department department = buildDepartment(1L, "IT");
        Designation designation = buildDesignation(2L, "Manager", department);
        Module module = buildModule(3L, "Employees", "EMP");
        DepartmentModule dmWithModule = buildDepartmentModule(4L, department, module);
        DepartmentModule dmWithoutModule = buildDepartmentModule(5L, department, null);

        when(designationRepository.findById(2L)).thenReturn(Optional.of(designation));
        when(departmentModuleRepository.findByDepartmentIdAndIsActive(1L, true))
                .thenReturn(List.of(dmWithModule, dmWithoutModule));
        when(accessRepository.findByDesignationIdAndModuleId(2L, 3L))
                .thenReturn(Optional.of(buildAccess(6L, designation, module)));

        moduleAccessService.initializeDesignationAccess(2L);

        verify(accessRepository, never()).save(any());
    }

    // ---------- assignModulesToDepartment ----------

    @Test
    void assignModulesToDepartment_departmentNotFound_throwsResourceNotFoundException() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> moduleAccessService.assignModulesToDepartment(1L, List.of(2L)));
    }

    @Test
    void assignModulesToDepartment_moduleNotFound_throwsResourceNotFoundException() {
        Department department = buildDepartment(1L, "IT");
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(departmentModuleRepository.findByDepartmentIdAndIsActive(1L, true)).thenReturn(Collections.emptyList());
        when(moduleRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> moduleAccessService.assignModulesToDepartment(1L, List.of(2L)));
    }

    @Test
    void assignModulesToDepartment_deactivatesExistingAndAssignsNew() {
        Department department = buildDepartment(1L, "IT");
        Module oldModule = buildModule(2L, "Old", "OLD");
        Module newModule = buildModule(3L, "New", "NEW");
        DepartmentModule existingDm = buildDepartmentModule(4L, department, oldModule);

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(departmentModuleRepository.findByDepartmentIdAndIsActive(1L, true)).thenReturn(List.of(existingDm));
        when(moduleRepository.findById(3L)).thenReturn(Optional.of(newModule));
        when(departmentModuleRepository.findByDepartmentIdAndModuleId(1L, 3L)).thenReturn(Optional.empty());

        moduleAccessService.assignModulesToDepartment(1L, List.of(3L));

        assertFalse(existingDm.getIsActive());
        verify(departmentModuleRepository, atLeastOnce()).save(any(DepartmentModule.class));
        verify(permissionService).clearCacheForDepartment(1L);
    }

    @Test
    void assignModulesToDepartment_reactivatesExistingAssignment() {
        Department department = buildDepartment(1L, "IT");
        Module module = buildModule(3L, "Mod", "MOD");
        DepartmentModule existingInactive = buildDepartmentModule(5L, department, module);
        existingInactive.setIsActive(false);

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(departmentModuleRepository.findByDepartmentIdAndIsActive(1L, true)).thenReturn(Collections.emptyList());
        when(moduleRepository.findById(3L)).thenReturn(Optional.of(module));
        when(departmentModuleRepository.findByDepartmentIdAndModuleId(1L, 3L)).thenReturn(Optional.of(existingInactive));

        moduleAccessService.assignModulesToDepartment(1L, List.of(3L));

        assertTrue(existingInactive.getIsActive());
    }

    // ---------- assignAccessToDesignation ----------

    @Test
    void assignAccessToDesignation_designationNotFound_throwsResourceNotFoundException() {
        DesignationModuleAccessRequest request = new DesignationModuleAccessRequest();
        request.setDesignationId(1L);
        request.setModuleId(2L);
        request.setAccessLevel("READ");

        when(designationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> moduleAccessService.assignAccessToDesignation(request));
    }

    @Test
    void assignAccessToDesignation_moduleNotFound_throwsResourceNotFoundException() {
        Designation designation = buildDesignation(1L, "Manager", null);
        DesignationModuleAccessRequest request = new DesignationModuleAccessRequest();
        request.setDesignationId(1L);
        request.setModuleId(2L);
        request.setAccessLevel("READ");

        when(designationRepository.findById(1L)).thenReturn(Optional.of(designation));
        when(moduleRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> moduleAccessService.assignAccessToDesignation(request));
    }

    @Test
    void assignAccessToDesignation_invalidAccessLevel_throwsBadRequestException() {
        Designation designation = buildDesignation(1L, "Manager", null);
        Module module = buildModule(2L, "Employees", "EMP");
        DesignationModuleAccessRequest request = new DesignationModuleAccessRequest();
        request.setDesignationId(1L);
        request.setModuleId(2L);
        request.setAccessLevel("INVALID");

        when(designationRepository.findById(1L)).thenReturn(Optional.of(designation));
        when(moduleRepository.findById(2L)).thenReturn(Optional.of(module));

        assertThrows(BadRequestException.class, () -> moduleAccessService.assignAccessToDesignation(request));
    }

    @Test
    void assignAccessToDesignation_newAccess_success() {
        Designation designation = buildDesignation(1L, "Manager", null);
        Module module = buildModule(2L, "Employees", "EMP");
        DesignationModuleAccessRequest request = new DesignationModuleAccessRequest();
        request.setDesignationId(1L);
        request.setModuleId(2L);
        request.setAccessLevel("ADMIN");

        when(designationRepository.findById(1L)).thenReturn(Optional.of(designation));
        when(moduleRepository.findById(2L)).thenReturn(Optional.of(module));
        when(accessRepository.findByDesignationIdAndModuleId(1L, 2L)).thenReturn(Optional.empty());

        moduleAccessService.assignAccessToDesignation(request);

        verify(accessRepository).save(argThat(access ->
                access.getAccessLevel() == AccessLevel.ADMIN
                        && Boolean.TRUE.equals(access.getCanDelete())
                        && Boolean.TRUE.equals(access.getCanApprove())));
        verify(permissionService).clearCacheForDesignation(1L);
    }

    @Test
    void assignAccessToDesignation_existingAccess_updatesInPlace() {
        Designation designation = buildDesignation(1L, "Manager", null);
        Module module = buildModule(2L, "Employees", "EMP");
        DesignationModuleAccess existing = buildAccess(3L, designation, module);
        DesignationModuleAccessRequest request = new DesignationModuleAccessRequest();
        request.setDesignationId(1L);
        request.setModuleId(2L);
        request.setAccessLevel("NONE");

        when(designationRepository.findById(1L)).thenReturn(Optional.of(designation));
        when(moduleRepository.findById(2L)).thenReturn(Optional.of(module));
        when(accessRepository.findByDesignationIdAndModuleId(1L, 2L)).thenReturn(Optional.of(existing));

        moduleAccessService.assignAccessToDesignation(request);

        assertEquals(AccessLevel.NONE, existing.getAccessLevel());
        assertFalse(existing.getCanRead());
        verify(accessRepository).save(existing);
    }

    @Test
    void assignAccessToDesignation_grantingAccess_assignsModuleToDepartment() {
        Department department = buildDepartment(1L, "IT");
        Designation designation = buildDesignation(2L, "Manager", department);
        Module module = buildModule(3L, "Attendance", "ATTENDANCE");
        DesignationModuleAccessRequest request = new DesignationModuleAccessRequest();
        request.setDesignationId(2L);
        request.setModuleId(3L);
        request.setAccessLevel("READ");

        when(designationRepository.findById(2L)).thenReturn(Optional.of(designation));
        when(moduleRepository.findById(3L)).thenReturn(Optional.of(module));
        when(accessRepository.findByDesignationIdAndModuleId(2L, 3L)).thenReturn(Optional.empty());
        when(departmentModuleRepository.findByDepartmentIdAndModuleId(1L, 3L))
                .thenReturn(Optional.empty());

        moduleAccessService.assignAccessToDesignation(request);

        verify(departmentModuleRepository).save(argThat(dm ->
                dm.getDepartment().getId().equals(1L)
                        && dm.getModule().getId().equals(3L)
                        && Boolean.TRUE.equals(dm.getIsActive())));
    }

    @Test
    void assignAccessToDesignation_grantingAccess_reactivatesInactiveDepartmentModule() {
        Department department = buildDepartment(1L, "IT");
        Designation designation = buildDesignation(2L, "Manager", department);
        Module module = buildModule(3L, "Attendance", "ATTENDANCE");
        DepartmentModule inactiveDm = buildDepartmentModule(4L, department, module);
        inactiveDm.setIsActive(false);
        DesignationModuleAccessRequest request = new DesignationModuleAccessRequest();
        request.setDesignationId(2L);
        request.setModuleId(3L);
        request.setAccessLevel("WRITE");

        when(designationRepository.findById(2L)).thenReturn(Optional.of(designation));
        when(moduleRepository.findById(3L)).thenReturn(Optional.of(module));
        when(accessRepository.findByDesignationIdAndModuleId(2L, 3L)).thenReturn(Optional.empty());
        when(departmentModuleRepository.findByDepartmentIdAndModuleId(1L, 3L))
                .thenReturn(Optional.of(inactiveDm));

        moduleAccessService.assignAccessToDesignation(request);

        assertTrue(inactiveDm.getIsActive());
        verify(departmentModuleRepository).save(inactiveDm);
    }

    @Test
    void assignAccessToDesignation_settingNone_doesNotAssignModuleToDepartment() {
        Department department = buildDepartment(1L, "IT");
        Designation designation = buildDesignation(2L, "Manager", department);
        Module module = buildModule(3L, "Attendance", "ATTENDANCE");
        DesignationModuleAccessRequest request = new DesignationModuleAccessRequest();
        request.setDesignationId(2L);
        request.setModuleId(3L);
        request.setAccessLevel("NONE");

        when(designationRepository.findById(2L)).thenReturn(Optional.of(designation));
        when(moduleRepository.findById(3L)).thenReturn(Optional.of(module));
        when(accessRepository.findByDesignationIdAndModuleId(2L, 3L)).thenReturn(Optional.empty());

        moduleAccessService.assignAccessToDesignation(request);

        verify(departmentModuleRepository, never()).save(any());
    }

    // ---------- removeModuleFromDepartment ----------

    @Test
    void removeModuleFromDepartment_departmentNotFound_throwsResourceNotFoundException() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> moduleAccessService.removeModuleFromDepartment(1L, 2L));
    }

    @Test
    void removeModuleFromDepartment_moduleNotFound_throwsResourceNotFoundException() {
        Department department = buildDepartment(1L, "IT");
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(moduleRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> moduleAccessService.removeModuleFromDepartment(1L, 2L));
    }

    @Test
    void removeModuleFromDepartment_existingAssignment_deactivates() {
        Department department = buildDepartment(1L, "IT");
        Module module = buildModule(2L, "Employees", "EMP");
        DepartmentModule dm = buildDepartmentModule(3L, department, module);

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(moduleRepository.findById(2L)).thenReturn(Optional.of(module));
        when(departmentModuleRepository.findByDepartmentIdAndModuleId(1L, 2L)).thenReturn(Optional.of(dm));

        moduleAccessService.removeModuleFromDepartment(1L, 2L);

        assertFalse(dm.getIsActive());
        verify(permissionService).clearCacheForDepartment(1L);
    }

    @Test
    void removeModuleFromDepartment_noAssignment_noSaveCalled() {
        Department department = buildDepartment(1L, "IT");
        Module module = buildModule(2L, "Employees", "EMP");

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(moduleRepository.findById(2L)).thenReturn(Optional.of(module));
        when(departmentModuleRepository.findByDepartmentIdAndModuleId(1L, 2L)).thenReturn(Optional.empty());

        moduleAccessService.removeModuleFromDepartment(1L, 2L);

        verify(departmentModuleRepository, never()).save(any());
        verify(permissionService).clearCacheForDepartment(1L);
    }

    // ---------- revokeAccessFromDesignation ----------

    @Test
    void revokeAccessFromDesignation_designationNotFound_throwsResourceNotFoundException() {
        when(designationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> moduleAccessService.revokeAccessFromDesignation(1L, 2L));
    }

    @Test
    void revokeAccessFromDesignation_moduleNotFound_throwsResourceNotFoundException() {
        Designation designation = buildDesignation(1L, "Manager", null);
        when(designationRepository.findById(1L)).thenReturn(Optional.of(designation));
        when(moduleRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> moduleAccessService.revokeAccessFromDesignation(1L, 2L));
    }

    @Test
    void revokeAccessFromDesignation_existingAccess_deactivates() {
        Designation designation = buildDesignation(1L, "Manager", null);
        Module module = buildModule(2L, "Employees", "EMP");
        DesignationModuleAccess access = buildAccess(3L, designation, module);

        when(designationRepository.findById(1L)).thenReturn(Optional.of(designation));
        when(moduleRepository.findById(2L)).thenReturn(Optional.of(module));
        when(accessRepository.findByDesignationIdAndModuleId(1L, 2L)).thenReturn(Optional.of(access));

        moduleAccessService.revokeAccessFromDesignation(1L, 2L);

        assertFalse(access.getIsActive());
        verify(permissionService).clearCacheForDesignation(1L);
    }

    @Test
    void revokeAccessFromDesignation_noAccess_noSaveCalled() {
        Designation designation = buildDesignation(1L, "Manager", null);
        Module module = buildModule(2L, "Employees", "EMP");

        when(designationRepository.findById(1L)).thenReturn(Optional.of(designation));
        when(moduleRepository.findById(2L)).thenReturn(Optional.of(module));
        when(accessRepository.findByDesignationIdAndModuleId(1L, 2L)).thenReturn(Optional.empty());

        moduleAccessService.revokeAccessFromDesignation(1L, 2L);

        verify(accessRepository, never()).save(any());
    }

    // ---------- getCurrentUserEmail (via assignModulesToDepartment path) ----------

    @Test
    void assignModulesToDepartment_withAuthentication_usesAuthenticatedUserForLogging() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin@example.com", null, Collections.emptyList()));

        Department department = buildDepartment(1L, "IT");
        Module module = buildModule(2L, "Employees", "EMP");

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(departmentModuleRepository.findByDepartmentIdAndIsActive(1L, true)).thenReturn(Collections.emptyList());
        when(moduleRepository.findById(2L)).thenReturn(Optional.of(module));
        when(departmentModuleRepository.findByDepartmentIdAndModuleId(1L, 2L)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> moduleAccessService.assignModulesToDepartment(1L, List.of(2L)));
    }

    @Test
    void assignModulesToDepartment_noAuthentication_defaultsToSystem() {
        Department department = buildDepartment(1L, "IT");

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(departmentModuleRepository.findByDepartmentIdAndIsActive(1L, true)).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> moduleAccessService.assignModulesToDepartment(1L, Collections.emptyList()));
    }
}
