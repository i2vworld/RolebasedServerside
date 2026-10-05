package com.world.i2v.i2vauthapi.service;

import com.world.i2v.i2vauthapi.dto.request.DepartmentRequest;
import com.world.i2v.i2vauthapi.dto.response.DepartmentResponse;
import com.world.i2v.i2vauthapi.entity.Department;
import com.world.i2v.i2vauthapi.entity.Unit;
import com.world.i2v.i2vauthapi.entity.User;
import com.world.i2v.i2vauthapi.entity.EmployeeUser;
import com.world.i2v.i2vauthapi.exception.BadRequestException;
import com.world.i2v.i2vauthapi.exception.ResourceNotFoundException;
import com.world.i2v.i2vauthapi.repository.DepartmentRepository;
import com.world.i2v.i2vauthapi.repository.UnitRepository;
import com.world.i2v.i2vauthapi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Smoke + unit test suite for {@link DepartmentService}.
 * Mirrors the style/conventions used in {@link DesignationServiceTest}: plain
 * Mockito mocks (no {@code @ExtendWith(MockitoExtension.class)}), covering every
 * public method and every branch of {@link DepartmentService}.
 */
class DepartmentServiceTest {

    private DepartmentRepository departmentRepository;
    private UserRepository userRepository;
    private UnitRepository unitRepository;
    private AuditService auditService;
    private DepartmentService departmentService;

    @BeforeEach
    void setUp() {
        departmentRepository = mock(DepartmentRepository.class);
        userRepository = mock(UserRepository.class);
        unitRepository = mock(UnitRepository.class);
        auditService = mock(AuditService.class);
        departmentService = new DepartmentService(departmentRepository, userRepository, unitRepository, auditService);
    }

    private DepartmentRequest buildRequest(String name, String code) {
        DepartmentRequest request = new DepartmentRequest();
        request.setName(name);
        request.setCode(code);
        request.setDescription("Handles engineering work");
        request.setIsActive(true);
        request.setBudget(BigDecimal.valueOf(50000));
        request.setLocation("HQ");
        request.setEmail("dept@example.com");
        request.setPhone("1234567890");
        return request;
    }

    // ---------- Smoke test ----------

    @Test
    void smokeTest_serviceConstructsAndBasicCallSucceeds() {
        when(departmentRepository.findAll()).thenReturn(List.of());
        List<DepartmentResponse> result = departmentService.getAllDepartments();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ---------- createDepartment ----------

    @Test
    void createDepartment_shouldThrow_whenNameAlreadyExists() {
        DepartmentRequest request = buildRequest("Engineering", null);
        when(departmentRepository.existsByName("Engineering")).thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> departmentService.createDepartment(request));
        assertEquals("Department with this name already exists", ex.getMessage());
        verify(departmentRepository, never()).save(any());
    }

    @Test
    void createDepartment_shouldThrow_whenCodeAlreadyExists() {
        DepartmentRequest request = buildRequest("Engineering", "ENG");
        when(departmentRepository.existsByName("Engineering")).thenReturn(false);
        when(departmentRepository.existsByCode("ENG")).thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> departmentService.createDepartment(request));
        assertEquals("Department with this code already exists", ex.getMessage());
        verify(departmentRepository, never()).save(any());
    }

    @Test
    void createDepartment_shouldThrow_whenParentDepartmentNotFound() {
        DepartmentRequest request = buildRequest("Engineering", "ENG");
        request.setParentDepartmentId(99L);
        when(departmentRepository.existsByName("Engineering")).thenReturn(false);
        when(departmentRepository.existsByCode("ENG")).thenReturn(false);
        when(departmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> departmentService.createDepartment(request));
        verify(departmentRepository, never()).save(any());
    }

    @Test
    void createDepartment_shouldThrow_whenUnitNotFound() {
        DepartmentRequest request = buildRequest("Engineering", "ENG");
        request.setUnitId(5L);
        when(departmentRepository.existsByName("Engineering")).thenReturn(false);
        when(departmentRepository.existsByCode("ENG")).thenReturn(false);
        when(unitRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> departmentService.createDepartment(request));
        verify(departmentRepository, never()).save(any());
    }

    @Test
    void createDepartment_shouldThrow_whenManagerNotFound() {
        DepartmentRequest request = buildRequest("Engineering", "ENG");
        request.setManagerId(7L);
        when(departmentRepository.existsByName("Engineering")).thenReturn(false);
        when(departmentRepository.existsByCode("ENG")).thenReturn(false);
        when(userRepository.findById(7L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> departmentService.createDepartment(request));
        verify(departmentRepository, never()).save(any());
    }

    @Test
    void createDepartment_shouldGenerateCodeAndApplyDefaults_whenOptionalFieldsMissing() {
        DepartmentRequest request = new DepartmentRequest();
        request.setName("Human Resources!!");
        request.setIsActive(null);
        when(departmentRepository.existsByName("Human Resources!!")).thenReturn(false);
        when(departmentRepository.save(any(Department.class))).thenAnswer(inv -> {
            Department d = inv.getArgument(0);
            d.setId(1L);
            return d;
        });

        DepartmentResponse response = departmentService.createDepartment(request);

        assertNotNull(response);
        assertEquals("HUMAN_RESOURCES_", response.getCode());
        assertTrue(response.getIsActive());
        verify(auditService).logAction(isNull(), eq("CREATE"), eq("DEPARTMENT"), eq(1L), eq("Human Resources!!"));
    }

    @Test
    void createDepartment_shouldUseProvidedCodeParentUnitAndManager_whenPresent() {
        DepartmentRequest request = buildRequest("Engineering", "ENG");
        request.setParentDepartmentId(10L);
        request.setUnitId(20L);
        request.setManagerId(30L);
        request.setIsHierarchyUnit(true);

        Department parent = new Department();
        parent.setId(10L);
        parent.setName("Parent Dept");

        Unit unit = new Unit();
        unit.setId(20L);
        unit.setName("Unit A");
        unit.setCode("UA");

        User manager = new User();
        manager.setId(30L);
        manager.setFullName("Jane Manager");
        manager.setEmail("jane@example.com");

        when(departmentRepository.existsByName("Engineering")).thenReturn(false);
        when(departmentRepository.existsByCode("ENG")).thenReturn(false);
        when(departmentRepository.findById(10L)).thenReturn(Optional.of(parent));
        when(unitRepository.findById(20L)).thenReturn(Optional.of(unit));
        when(userRepository.findById(30L)).thenReturn(Optional.of(manager));
        when(departmentRepository.save(any(Department.class))).thenAnswer(inv -> {
            Department d = inv.getArgument(0);
            d.setId(1L);
            return d;
        });

        DepartmentResponse response = departmentService.createDepartment(request);

        assertEquals("ENG", response.getCode());
        assertNotNull(response.getParentDepartment());
        assertEquals(10L, response.getParentDepartment().getId());
        assertEquals(20L, response.getUnitId());
        assertEquals("Unit A", response.getUnitName());
        assertEquals("UA", response.getUnitCode());
        assertNotNull(response.getManager());
        assertEquals("Jane Manager", response.getManager().getFullName());
        assertTrue(response.getIsHierarchyUnit());
    }

    // ---------- getAllDepartments ----------

    @Test
    void getAllDepartments_shouldReturnEmptyList_whenNoneExist() {
        when(departmentRepository.findAll()).thenReturn(List.of());
        assertTrue(departmentService.getAllDepartments().isEmpty());
    }

    @Test
    void getAllDepartments_shouldMapAllEntitiesAndCountEmployees() {
        Department d1 = new Department();
        d1.setId(1L);
        d1.setName("Engineering");
        d1.setCode("ENG");

        Set<EmployeeUser> employees = new HashSet<>();
        EmployeeUser e1 = new EmployeeUser();
        e1.setId(101L);
        EmployeeUser e2 = new EmployeeUser();
        e2.setId(102L);
        employees.add(e1);
        employees.add(e2);
        d1.setEmployees(employees);

        Department d2 = new Department();
        d2.setId(2L);
        d2.setName("Sales");
        d2.setCode("SAL");

        when(departmentRepository.findAll()).thenReturn(List.of(d1, d2));

        List<DepartmentResponse> result = departmentService.getAllDepartments();

        assertEquals(2, result.size());
        assertEquals(2L, result.get(0).getEmployeeCount());
        assertEquals(0L, result.get(1).getEmployeeCount());
    }

    // ---------- getDepartmentById ----------

    @Test
    void getDepartmentById_shouldReturnMappedResponse_whenFound() {
        Department department = new Department();
        department.setId(1L);
        department.setName("Engineering");
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));

        DepartmentResponse response = departmentService.getDepartmentById(1L);

        assertNotNull(response);
        assertEquals("Engineering", response.getName());
    }

    @Test
    void getDepartmentById_shouldThrow_whenNotFound() {
        when(departmentRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> departmentService.getDepartmentById(99L));
    }

    // ---------- getActiveDepartments ----------

    @Test
    void getActiveDepartments_shouldReturnOnlyActiveMapped() {
        Department department = new Department();
        department.setId(1L);
        department.setName("Engineering");
        department.setIsActive(true);
        when(departmentRepository.findByIsActive(true)).thenReturn(List.of(department));

        List<DepartmentResponse> result = departmentService.getActiveDepartments();

        assertEquals(1, result.size());
        assertTrue(result.get(0).getIsActive());
    }

    @Test
    void getActiveDepartments_shouldReturnEmptyList_whenNoneActive() {
        when(departmentRepository.findByIsActive(true)).thenReturn(List.of());
        assertTrue(departmentService.getActiveDepartments().isEmpty());
    }

    // ---------- updateDepartment ----------

    @Test
    void updateDepartment_shouldThrow_whenNotFound() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.empty());
        DepartmentRequest request = buildRequest("Engineering", "ENG");
        assertThrows(ResourceNotFoundException.class, () -> departmentService.updateDepartment(1L, request));
    }

    @Test
    void updateDepartment_shouldThrow_whenNameChangedToExistingName() {
        Department existing = new Department();
        existing.setId(1L);
        existing.setName("Old Name");
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(departmentRepository.existsByName("New Name")).thenReturn(true);

        DepartmentRequest request = buildRequest("New Name", "ENG");
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> departmentService.updateDepartment(1L, request));
        assertEquals("Department with this name already exists", ex.getMessage());
        verify(departmentRepository, never()).save(any());
    }

    @Test
    void updateDepartment_shouldNotCheckDuplicate_whenNameUnchanged() {
        Department existing = new Department();
        existing.setId(1L);
        existing.setName("Engineering");
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(departmentRepository.save(any(Department.class))).thenAnswer(inv -> inv.getArgument(0));

        DepartmentRequest request = buildRequest("Engineering", "ENG2");
        departmentService.updateDepartment(1L, request);

        verify(departmentRepository, never()).existsByName(any());
    }

    @Test
    void updateDepartment_shouldUpdateCode_onlyWhenProvided() {
        Department existing = new Department();
        existing.setId(1L);
        existing.setName("Engineering");
        existing.setCode("OLD");
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(departmentRepository.save(any(Department.class))).thenAnswer(inv -> inv.getArgument(0));

        DepartmentRequest request = buildRequest("Engineering", null);
        DepartmentResponse response = departmentService.updateDepartment(1L, request);

        assertEquals("OLD", response.getCode());
    }

    @Test
    void updateDepartment_shouldNotCheckDuplicateCode_whenCodeUnchanged() {
        Department existing = new Department();
        existing.setId(1L);
        existing.setName("Engineering");
        existing.setCode("ENG");
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(departmentRepository.save(any(Department.class))).thenAnswer(inv -> inv.getArgument(0));

        DepartmentRequest request = buildRequest("Engineering", "ENG");
        departmentService.updateDepartment(1L, request);

        verify(departmentRepository, never()).existsByCode(any());
    }

    @Test
    void updateDepartment_shouldThrow_whenCodeChangedToExistingCode() {
        Department existing = new Department();
        existing.setId(1L);
        existing.setName("Engineering");
        existing.setCode("OLD");
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(departmentRepository.existsByCode("NEW")).thenReturn(true);

        DepartmentRequest request = buildRequest("Engineering", "NEW");
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> departmentService.updateDepartment(1L, request));
        assertEquals("Department with this code already exists", ex.getMessage());
        verify(departmentRepository, never()).save(any());
    }

    @Test
    void updateDepartment_shouldThrow_whenParentIsSelf() {
        Department existing = new Department();
        existing.setId(1L);
        existing.setName("Engineering");
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(existing));

        DepartmentRequest request = buildRequest("Engineering", "ENG");
        request.setParentDepartmentId(1L);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> departmentService.updateDepartment(1L, request));
        assertEquals("Department cannot be its own parent", ex.getMessage());
        verify(departmentRepository, never()).save(any());
    }

    @Test
    void updateDepartment_shouldThrow_whenParentNotFound() {
        Department existing = new Department();
        existing.setId(1L);
        existing.setName("Engineering");
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(departmentRepository.findById(2L)).thenReturn(Optional.empty());

        DepartmentRequest request = buildRequest("Engineering", "ENG");
        request.setParentDepartmentId(2L);

        assertThrows(ResourceNotFoundException.class, () -> departmentService.updateDepartment(1L, request));
        verify(departmentRepository, never()).save(any());
    }

    @Test
    void updateDepartment_shouldSetParentUnitAndManager_whenIdsProvided() {
        Department existing = new Department();
        existing.setId(1L);
        existing.setName("Engineering");

        Department parent = new Department();
        parent.setId(2L);
        parent.setName("Parent");

        Unit unit = new Unit();
        unit.setId(3L);
        unit.setName("Unit A");
        unit.setCode("UA");

        User manager = new User();
        manager.setId(4L);
        manager.setFullName("Jane");
        manager.setEmail("jane@example.com");

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(parent));
        when(unitRepository.findById(3L)).thenReturn(Optional.of(unit));
        when(userRepository.findById(4L)).thenReturn(Optional.of(manager));
        when(departmentRepository.save(any(Department.class))).thenAnswer(inv -> inv.getArgument(0));

        DepartmentRequest request = buildRequest("Engineering", "ENG");
        request.setParentDepartmentId(2L);
        request.setUnitId(3L);
        request.setManagerId(4L);

        DepartmentResponse response = departmentService.updateDepartment(1L, request);

        assertNotNull(response.getParentDepartment());
        assertEquals(2L, response.getParentDepartment().getId());
        assertEquals(3L, response.getUnitId());
        assertNotNull(response.getManager());
        assertEquals(4L, response.getManager().getId());
    }

    @Test
    void updateDepartment_shouldThrow_whenUnitNotFound() {
        Department existing = new Department();
        existing.setId(1L);
        existing.setName("Engineering");
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(unitRepository.findById(3L)).thenReturn(Optional.empty());

        DepartmentRequest request = buildRequest("Engineering", "ENG");
        request.setUnitId(3L);

        assertThrows(ResourceNotFoundException.class, () -> departmentService.updateDepartment(1L, request));
        verify(departmentRepository, never()).save(any());
    }

    @Test
    void updateDepartment_shouldThrow_whenManagerNotFound() {
        Department existing = new Department();
        existing.setId(1L);
        existing.setName("Engineering");
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.findById(4L)).thenReturn(Optional.empty());

        DepartmentRequest request = buildRequest("Engineering", "ENG");
        request.setManagerId(4L);

        assertThrows(ResourceNotFoundException.class, () -> departmentService.updateDepartment(1L, request));
        verify(departmentRepository, never()).save(any());
    }

    @Test
    void updateDepartment_shouldClearParentUnitAndManager_whenIdsNull() {
        Department existing = new Department();
        existing.setId(1L);
        existing.setName("Engineering");
        existing.setParentDepartment(new Department());
        existing.setUnit(new Unit());
        existing.setManager(new User());

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(departmentRepository.save(any(Department.class))).thenAnswer(inv -> inv.getArgument(0));

        DepartmentRequest request = buildRequest("Engineering", "ENG");
        // parentDepartmentId, unitId, managerId left null

        DepartmentResponse response = departmentService.updateDepartment(1L, request);

        assertNull(response.getParentDepartment());
        assertNull(response.getUnitId());
        assertNull(response.getManager());
        assertFalse(response.getIsHierarchyUnit());
    }

    // ---------- deleteDepartment ----------

    @Test
    void deleteDepartment_shouldSoftDelete_whenFound() {
        Department existing = new Department();
        existing.setId(1L);
        existing.setName("Engineering");
        existing.setIsActive(true);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(departmentRepository.save(any(Department.class))).thenAnswer(inv -> inv.getArgument(0));

        departmentService.deleteDepartment(1L);

        assertFalse(existing.getIsActive());
        verify(departmentRepository).save(existing);
        verify(auditService).logAction(isNull(), eq("DELETE"), eq("DEPARTMENT"), eq(1L), eq("Engineering"));
    }

    @Test
    void deleteDepartment_shouldThrow_whenNotFound() {
        when(departmentRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> departmentService.deleteDepartment(99L));
        verify(departmentRepository, never()).save(any());
    }
}
