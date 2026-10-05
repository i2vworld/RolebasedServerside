package com.world.i2v.i2vauthapi.service;

import com.world.i2v.i2vauthapi.entity.*;
import com.world.i2v.i2vauthapi.entity.Module;
import com.world.i2v.i2vauthapi.repository.DepartmentModuleRepository;
import com.world.i2v.i2vauthapi.repository.DesignationModuleAccessRepository;
import com.world.i2v.i2vauthapi.repository.EmployeeUserRepository;
import com.world.i2v.i2vauthapi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PermissionServiceTest {

    private DesignationModuleAccessRepository accessRepository;
    private DepartmentModuleRepository departmentModuleRepository;
    private EmployeeUserRepository employeeRepository;
    private UserRepository userRepository;

    private PermissionService permissionService;

    @BeforeEach
    void setUp() {
        accessRepository = mock(DesignationModuleAccessRepository.class);
        departmentModuleRepository = mock(DepartmentModuleRepository.class);
        employeeRepository = mock(EmployeeUserRepository.class);
        userRepository = mock(UserRepository.class);

        permissionService = new PermissionService(accessRepository, departmentModuleRepository,
                employeeRepository, userRepository);
    }

    private User buildUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setFullName("Test User");
        user.setEmail("test@example.com");
        user.setRole(UserRole.EMPLOYEE);
        return user;
    }

    private Department buildDepartment(Long id, String name) {
        Department department = new Department();
        department.setId(id);
        department.setName(name);
        return department;
    }

    private Designation buildDesignation(Long id, String title) {
        Designation designation = new Designation();
        designation.setId(id);
        designation.setTitle(title);
        return designation;
    }

    private EmployeeUser buildEmployee(Long userId, Department department, Designation designation) {
        EmployeeUser employee = new EmployeeUser();
        employee.setId(1L);
        employee.setEmployeeCode("EMP-001");
        employee.setDepartment(department);
        employee.setDesignation(designation);
        return employee;
    }

    private Module buildModule(Long id, String code, String name, String routePath, boolean active, boolean visible) {
        Module module = new Module();
        module.setId(id);
        module.setCode(code);
        module.setName(name);
        module.setRoutePath(routePath);
        module.setIsActive(active);
        module.setIsVisible(visible);
        module.setDisplayOrder(1);
        module.setIcon("icon");
        module.setDescription("desc");
        return module;
    }

    private DepartmentModule buildDepartmentModule(Module module) {
        DepartmentModule dm = new DepartmentModule();
        dm.setId(1L);
        dm.setModule(module);
        dm.setIsActive(true);
        return dm;
    }

    private DesignationModuleAccess buildAccess(Module module, boolean active, AccessLevel level,
                                                 boolean read, boolean create, boolean update,
                                                 boolean delete, boolean approve) {
        DesignationModuleAccess access = new DesignationModuleAccess();
        access.setId(1L);
        access.setModule(module);
        access.setIsActive(active);
        access.setAccessLevel(level);
        access.setCanRead(read);
        access.setCanCreate(create);
        access.setCanUpdate(update);
        access.setCanDelete(delete);
        access.setCanApprove(approve);
        access.setCanExport(false);
        access.setCanImport(false);
        return access;
    }

    // ---------- Smoke test ----------

    @Test
    void smokeTest_serviceCanBeConstructed() {
        assertNotNull(permissionService);
    }

    // ---------- getUserPermissions ----------

    @Test
    void getUserPermissions_userNotFound_returnsEmptyMap() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        Map<String, Object> result = permissionService.getUserPermissions(1L);

        assertTrue(result.isEmpty());
    }

    @Test
    void getUserPermissions_noEmployeeProfile_returnsNotEmployee() {
        User user = buildUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.empty());

        Map<String, Object> result = permissionService.getUserPermissions(1L);

        assertEquals(false, result.get("isEmployee"));
        assertEquals("No employee profile found", result.get("message"));
    }

    @Test
    void getUserPermissions_missingDepartmentOrDesignation_returnsPartialInfo() {
        User user = buildUser(1L);
        EmployeeUser employee = buildEmployee(1L, null, null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(employee));

        Map<String, Object> result = permissionService.getUserPermissions(1L);

        assertEquals(true, result.get("isEmployee"));
        assertEquals(false, result.get("hasDepartment"));
        assertEquals(false, result.get("hasDesignation"));
    }

    @Test
    void getUserPermissions_withValidAccess_returnsFullPermissions() {
        User user = buildUser(1L);
        Department department = buildDepartment(10L, "Engineering");
        Designation designation = buildDesignation(20L, "Developer");
        EmployeeUser employee = buildEmployee(1L, department, designation);

        Module module = buildModule(100L, "emp-mgmt", "Employee Management", "/employees", true, true);
        DepartmentModule dm = buildDepartmentModule(module);
        DesignationModuleAccess access = buildAccess(module, true, AccessLevel.ADMIN,
                true, true, true, true, true);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(employee));
        when(departmentModuleRepository.findByDepartmentIdAndIsActive(10L, true))
                .thenReturn(List.of(dm));
        when(accessRepository.findByDesignationIdAndIsActive(20L, true))
                .thenReturn(List.of(access));

        Map<String, Object> result = permissionService.getUserPermissions(1L);

        assertEquals(true, result.get("isEmployee"));
        assertEquals("EMP-001", result.get("employeeCode"));
        assertEquals(10L, result.get("departmentId"));
        assertEquals("Engineering", result.get("departmentName"));
        assertEquals(20L, result.get("designationId"));
        assertEquals("Developer", result.get("designationTitle"));
        assertEquals("EMPLOYEE", result.get("role"));

        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> modules = (Map<String, Map<String, Object>>) result.get("modules");
        assertTrue(modules.containsKey("EMP-MGMT"));
        Map<String, Object> perms = modules.get("EMP-MGMT");
        assertEquals(true, perms.get("read"));
        assertEquals(true, perms.get("create"));
        assertEquals(true, perms.get("update"));
        assertEquals(true, perms.get("delete"));
        assertEquals(true, perms.get("approve"));
        assertEquals("ADMIN", perms.get("accessLevelName"));

        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> modulesByRoute =
                (Map<String, Map<String, Object>>) result.get("modulesByRoute");
        assertTrue(modulesByRoute.containsKey("/employees"));
    }

    @Test
    void getUserPermissions_moduleWithoutAccess_defaultsToNoPermissions() {
        User user = buildUser(1L);
        Department department = buildDepartment(10L, "Engineering");
        Designation designation = buildDesignation(20L, "Developer");
        EmployeeUser employee = buildEmployee(1L, department, designation);

        Module module = buildModule(100L, "emp-mgmt", "Employee Management", "/employees", true, true);
        DepartmentModule dm = buildDepartmentModule(module);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(employee));
        when(departmentModuleRepository.findByDepartmentIdAndIsActive(10L, true))
                .thenReturn(List.of(dm));
        when(accessRepository.findByDesignationIdAndIsActive(20L, true))
                .thenReturn(Collections.emptyList());

        Map<String, Object> result = permissionService.getUserPermissions(1L);

        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> modules = (Map<String, Map<String, Object>>) result.get("modules");
        Map<String, Object> perms = modules.get("EMP-MGMT");
        assertEquals(false, perms.get("read"));
        assertEquals("NONE", perms.get("accessLevelName"));
    }

    @Test
    void getUserPermissions_moduleWithNullModuleReference_isSkipped() {
        User user = buildUser(1L);
        Department department = buildDepartment(10L, "Engineering");
        Designation designation = buildDesignation(20L, "Developer");
        EmployeeUser employee = buildEmployee(1L, department, designation);

        DepartmentModule dm = new DepartmentModule();
        dm.setId(1L);
        dm.setModule(null);
        dm.setIsActive(true);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(employee));
        when(departmentModuleRepository.findByDepartmentIdAndIsActive(10L, true))
                .thenReturn(List.of(dm));
        when(accessRepository.findByDesignationIdAndIsActive(20L, true))
                .thenReturn(Collections.emptyList());

        Map<String, Object> result = permissionService.getUserPermissions(1L);

        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> modules = (Map<String, Map<String, Object>>) result.get("modules");
        assertTrue(modules.isEmpty());
    }

    @Test
    void getUserPermissions_designationAccessWithoutDepartmentModule_includesModule() {
        User user = buildUser(1L);
        Department department = buildDepartment(10L, "Engineering");
        Designation designation = buildDesignation(20L, "Developer");
        EmployeeUser employee = buildEmployee(1L, department, designation);

        Module module = buildModule(100L, "attendance", "Attendance", "/attendance", true, true);
        DesignationModuleAccess access = buildAccess(module, true, AccessLevel.READ,
                true, false, false, false, false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(employee));
        when(departmentModuleRepository.findByDepartmentIdAndIsActive(10L, true))
                .thenReturn(Collections.emptyList());
        when(departmentModuleRepository.findByDepartmentId(10L))
                .thenReturn(Collections.emptyList());
        when(accessRepository.findByDesignationIdAndIsActive(20L, true))
                .thenReturn(List.of(access));

        Map<String, Object> result = permissionService.getUserPermissions(1L);

        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> modules = (Map<String, Map<String, Object>>) result.get("modules");
        assertTrue(modules.containsKey("ATTENDANCE"));
        assertEquals(true, modules.get("ATTENDANCE").get("read"));

        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> modulesByRoute =
                (Map<String, Map<String, Object>>) result.get("modulesByRoute");
        assertTrue(modulesByRoute.containsKey("/attendance"));
        assertEquals(true, modulesByRoute.get("/attendance").get("read"));
    }

    @Test
    void getUserPermissions_designationAccessWithInactiveDepartmentModule_excludesModule() {
        User user = buildUser(1L);
        Department department = buildDepartment(10L, "Engineering");
        Designation designation = buildDesignation(20L, "Developer");
        EmployeeUser employee = buildEmployee(1L, department, designation);

        Module module = buildModule(100L, "attendance", "Attendance", "/attendance", true, true);
        DesignationModuleAccess access = buildAccess(module, true, AccessLevel.READ,
                true, false, false, false, false);
        DepartmentModule inactiveDm = buildDepartmentModule(module);
        inactiveDm.setIsActive(false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(employee));
        when(departmentModuleRepository.findByDepartmentIdAndIsActive(10L, true))
                .thenReturn(Collections.emptyList());
        when(departmentModuleRepository.findByDepartmentId(10L))
                .thenReturn(List.of(inactiveDm));
        when(accessRepository.findByDesignationIdAndIsActive(20L, true))
                .thenReturn(List.of(access));

        Map<String, Object> result = permissionService.getUserPermissions(1L);

        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> modules = (Map<String, Map<String, Object>>) result.get("modules");
        assertTrue(modules.isEmpty());
    }

    // ---------- hasAccess ----------

    @Test
    void hasAccess_notEmployee_returnsFalse() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertFalse(permissionService.hasAccess(1L, "emp-mgmt", "read"));
    }

    @Test
    void hasAccess_moduleNotFound_returnsFalse() {
        User user = buildUser(1L);
        Department department = buildDepartment(10L, "Engineering");
        Designation designation = buildDesignation(20L, "Developer");
        EmployeeUser employee = buildEmployee(1L, department, designation);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(employee));
        when(departmentModuleRepository.findByDepartmentIdAndIsActive(10L, true))
                .thenReturn(Collections.emptyList());
        when(accessRepository.findByDesignationIdAndIsActive(20L, true))
                .thenReturn(Collections.emptyList());

        assertFalse(permissionService.hasAccess(1L, "unknown-module", "read"));
    }

    @Test
    void hasAccess_withReadPermission_returnsTrue() {
        User user = buildUser(1L);
        Department department = buildDepartment(10L, "Engineering");
        Designation designation = buildDesignation(20L, "Developer");
        EmployeeUser employee = buildEmployee(1L, department, designation);

        Module module = buildModule(100L, "emp-mgmt", "Employee Management", "/employees", true, true);
        DepartmentModule dm = buildDepartmentModule(module);
        DesignationModuleAccess access = buildAccess(module, true, AccessLevel.READ,
                true, false, false, false, false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(employee));
        when(departmentModuleRepository.findByDepartmentIdAndIsActive(10L, true))
                .thenReturn(List.of(dm));
        when(accessRepository.findByDesignationIdAndIsActive(20L, true))
                .thenReturn(List.of(access));

        assertTrue(permissionService.hasAccess(1L, "emp-mgmt", "read"));
        assertFalse(permissionService.hasAccess(1L, "emp-mgmt", "delete"));
    }

    @Test
    void hasAccess_caseInsensitiveModuleCode_returnsTrue() {
        User user = buildUser(1L);
        Department department = buildDepartment(10L, "Engineering");
        Designation designation = buildDesignation(20L, "Developer");
        EmployeeUser employee = buildEmployee(1L, department, designation);

        Module module = buildModule(100L, "emp-mgmt", "Employee Management", "/employees", true, true);
        DepartmentModule dm = buildDepartmentModule(module);
        DesignationModuleAccess access = buildAccess(module, true, AccessLevel.WRITE,
                true, true, true, false, false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(employee));
        when(departmentModuleRepository.findByDepartmentIdAndIsActive(10L, true))
                .thenReturn(List.of(dm));
        when(accessRepository.findByDesignationIdAndIsActive(20L, true))
                .thenReturn(List.of(access));

        assertTrue(permissionService.hasAccess(1L, "Emp-Mgmt", "create"));
        assertTrue(permissionService.hasAccess(1L, "Emp-Mgmt", "write"));
    }

    // ---------- hasAccessByRoute ----------

    @Test
    void hasAccessByRoute_blankRoute_returnsFalse() {
        assertFalse(permissionService.hasAccessByRoute(1L, "", "read"));
        assertFalse(permissionService.hasAccessByRoute(1L, null, "read"));
    }

    @Test
    void hasAccessByRoute_matchingRoute_returnsTrue() {
        User user = buildUser(1L);
        Department department = buildDepartment(10L, "Engineering");
        Designation designation = buildDesignation(20L, "Developer");
        EmployeeUser employee = buildEmployee(1L, department, designation);

        Module module = buildModule(100L, "emp-mgmt", "Employee Management", "/employees/", true, true);
        DepartmentModule dm = buildDepartmentModule(module);
        DesignationModuleAccess access = buildAccess(module, true, AccessLevel.READ,
                true, false, false, false, false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(employee));
        when(departmentModuleRepository.findByDepartmentIdAndIsActive(10L, true))
                .thenReturn(List.of(dm));
        when(accessRepository.findByDesignationIdAndIsActive(20L, true))
                .thenReturn(List.of(access));

        assertTrue(permissionService.hasAccessByRoute(1L, "/employees", "read"));
        assertTrue(permissionService.hasAccessByRoute(1L, "EMPLOYEES", "read"));
    }

    @Test
    void hasAccessByRoute_noModulesByRoute_returnsFalse() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertFalse(permissionService.hasAccessByRoute(1L, "/employees", "read"));
    }

    // ---------- hasExplicitDeny ----------

    @Test
    void hasExplicitDeny_blankRoute_returnsFalse() {
        assertFalse(permissionService.hasExplicitDeny(1L, ""));
    }

    @Test
    void hasExplicitDeny_noAccess_returnsTrue() {
        User user = buildUser(1L);
        Department department = buildDepartment(10L, "Engineering");
        Designation designation = buildDesignation(20L, "Developer");
        EmployeeUser employee = buildEmployee(1L, department, designation);

        Module module = buildModule(100L, "emp-mgmt", "Employee Management", "/employees", true, true);
        DepartmentModule dm = buildDepartmentModule(module);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(employee));
        when(departmentModuleRepository.findByDepartmentIdAndIsActive(10L, true))
                .thenReturn(List.of(dm));
        when(accessRepository.findByDesignationIdAndIsActive(20L, true))
                .thenReturn(Collections.emptyList());

        assertTrue(permissionService.hasExplicitDeny(1L, "/employees"));
    }

    @Test
    void hasExplicitDeny_hasAccess_returnsFalse() {
        User user = buildUser(1L);
        Department department = buildDepartment(10L, "Engineering");
        Designation designation = buildDesignation(20L, "Developer");
        EmployeeUser employee = buildEmployee(1L, department, designation);

        Module module = buildModule(100L, "emp-mgmt", "Employee Management", "/employees", true, true);
        DepartmentModule dm = buildDepartmentModule(module);
        DesignationModuleAccess access = buildAccess(module, true, AccessLevel.READ,
                true, false, false, false, false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(employee));
        when(departmentModuleRepository.findByDepartmentIdAndIsActive(10L, true))
                .thenReturn(List.of(dm));
        when(accessRepository.findByDesignationIdAndIsActive(20L, true))
                .thenReturn(List.of(access));

        assertFalse(permissionService.hasExplicitDeny(1L, "/employees"));
    }

    // ---------- hasExplicitPermission ----------

    @Test
    void hasExplicitPermission_blankRoute_returnsFalse() {
        assertFalse(permissionService.hasExplicitPermission(1L, "", "read"));
    }

    @Test
    void hasExplicitPermission_matchingRoute_returnsTrue() {
        User user = buildUser(1L);
        Department department = buildDepartment(10L, "Engineering");
        Designation designation = buildDesignation(20L, "Developer");
        EmployeeUser employee = buildEmployee(1L, department, designation);

        Module module = buildModule(100L, "emp-mgmt", "Employee Management", "/employees", true, true);
        DepartmentModule dm = buildDepartmentModule(module);
        DesignationModuleAccess access = buildAccess(module, true, AccessLevel.READ,
                true, false, false, false, false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(employee));
        when(departmentModuleRepository.findByDepartmentIdAndIsActive(10L, true))
                .thenReturn(List.of(dm));
        when(accessRepository.findByDesignationIdAndIsActive(20L, true))
                .thenReturn(List.of(access));

        assertTrue(permissionService.hasExplicitPermission(1L, "/employees", "read"));
    }

    // ---------- normalizeRoutePathPublic ----------

    @Test
    void normalizeRoutePathPublic_variousInputs() {
        assertNull(permissionService.normalizeRoutePathPublic(null));
        assertNull(permissionService.normalizeRoutePathPublic("   "));
        assertEquals("/employees", permissionService.normalizeRoutePathPublic("employees"));
        assertEquals("/employees", permissionService.normalizeRoutePathPublic("/EMPLOYEES/"));
        assertEquals("/", permissionService.normalizeRoutePathPublic("/"));
    }

    // ---------- cache eviction methods (smoke - just verify no exceptions) ----------

    @Test
    void clearAllCache_doesNotThrow() {
        assertDoesNotThrow(() -> permissionService.clearAllCache());
    }

    @Test
    void clearCacheForDepartment_doesNotThrow() {
        assertDoesNotThrow(() -> permissionService.clearCacheForDepartment(1L));
    }

    @Test
    void clearCacheForDesignation_doesNotThrow() {
        assertDoesNotThrow(() -> permissionService.clearCacheForDesignation(1L));
    }

    @Test
    void clearCacheForUser_doesNotThrow() {
        assertDoesNotThrow(() -> permissionService.clearCacheForUser(1L));
    }
}
