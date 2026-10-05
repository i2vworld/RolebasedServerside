package com.world.i2v.i2vauthapi.service;

import com.world.i2v.i2vauthapi.dto.request.DesignationRequest;
import com.world.i2v.i2vauthapi.dto.response.DesignationResponse;
import com.world.i2v.i2vauthapi.entity.Department;
import com.world.i2v.i2vauthapi.entity.Designation;
import com.world.i2v.i2vauthapi.entity.EmployeeUser;
import com.world.i2v.i2vauthapi.exception.BadRequestException;
import com.world.i2v.i2vauthapi.exception.ResourceNotFoundException;
import com.world.i2v.i2vauthapi.repository.DepartmentRepository;
import com.world.i2v.i2vauthapi.repository.DesignationRepository;
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
 * Line-by-line unit/smoke tests for {@link DesignationService}.
 * Every public method and every branch (success + failure paths) is exercised.
 */
class DesignationServiceTest {

    private DesignationRepository designationRepository;
    private DepartmentRepository departmentRepository;
    private AuditService auditService;
    private DesignationService designationService;

    @BeforeEach
    void setUp() {
        designationRepository = mock(DesignationRepository.class);
        departmentRepository = mock(DepartmentRepository.class);
        auditService = mock(AuditService.class);
        designationService = new DesignationService(designationRepository, departmentRepository, auditService);
    }

    // ---------- smoke test ----------

    @Test
    void smokeTest_serviceConstructsAndBasicCallSucceeds() {
        when(designationRepository.findAll()).thenReturn(List.of());
        assertDoesNotThrow(() -> designationService.getAllDesignations());
    }

    // ---------- createDesignation ----------

    @Test
    void createDesignation_shouldThrow_whenTitleAlreadyExists() {
        DesignationRequest request = new DesignationRequest();
        request.setTitle("Manager");
        when(designationRepository.existsByTitle("Manager")).thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> designationService.createDesignation(request));
        assertEquals("Designation with this title already exists", ex.getMessage());
        verify(designationRepository, never()).save(any());
    }

    @Test
    void createDesignation_shouldThrow_whenCodeAlreadyExists() {
        DesignationRequest request = new DesignationRequest();
        request.setTitle("Manager");
        request.setCode("MGR");
        when(designationRepository.existsByTitle("Manager")).thenReturn(false);
        when(designationRepository.existsByCode("MGR")).thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> designationService.createDesignation(request));
        assertEquals("Designation with this code already exists", ex.getMessage());
        verify(designationRepository, never()).save(any());
    }

    @Test
    void createDesignation_shouldThrow_whenDepartmentNotFound() {
        DesignationRequest request = new DesignationRequest();
        request.setTitle("Manager");
        request.setDepartmentId(99L);
        when(designationRepository.existsByTitle("Manager")).thenReturn(false);
        when(departmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> designationService.createDesignation(request));
        verify(designationRepository, never()).save(any());
    }

    @Test
    void createDesignation_shouldGenerateCodeAndApplyDefaults_whenOptionalFieldsMissing() {
        DesignationRequest request = new DesignationRequest();
        request.setTitle("Senior Engineer");
        request.setCode(null);
        request.setLevel(null);
        request.setIsActive(null);
        request.setIsManagerial(null);

        when(designationRepository.existsByTitle("Senior Engineer")).thenReturn(false);
        when(designationRepository.save(any(Designation.class))).thenAnswer(invocation -> {
            Designation d = invocation.getArgument(0);
            d.setId(1L);
            return d;
        });

        DesignationResponse response = designationService.createDesignation(request);

        assertEquals("SENIOR_ENGINEER", response.getCode());
        assertEquals(1, response.getLevel());
        assertTrue(response.getIsActive());
        assertFalse(response.getIsManagerial());
        assertNull(response.getDepartment());
        verify(auditService).logAction(null, "CREATE", "DESIGNATION", 1L, "Senior Engineer");
    }

    @Test
    void createDesignation_shouldUseProvidedCodeAndDepartment_whenPresent() {
        DesignationRequest request = new DesignationRequest();
        request.setTitle("Team Lead");
        request.setCode("TL");
        request.setDepartmentId(5L);
        request.setLevel(3);
        request.setIsActive(false);
        request.setIsManagerial(true);
        request.setMinSalary(BigDecimal.valueOf(1000));
        request.setMaxSalary(BigDecimal.valueOf(2000));

        Department department = new Department();
        department.setId(5L);
        department.setName("Engineering");
        department.setCode("ENG");

        when(designationRepository.existsByTitle("Team Lead")).thenReturn(false);
        when(designationRepository.existsByCode("TL")).thenReturn(false);
        when(departmentRepository.findById(5L)).thenReturn(Optional.of(department));
        when(designationRepository.save(any(Designation.class))).thenAnswer(invocation -> {
            Designation d = invocation.getArgument(0);
            d.setId(2L);
            return d;
        });

        DesignationResponse response = designationService.createDesignation(request);

        assertEquals("TL", response.getCode());
        assertEquals(3, response.getLevel());
        assertFalse(response.getIsActive());
        assertTrue(response.getIsManagerial());
        assertNotNull(response.getDepartment());
        assertEquals("Engineering", response.getDepartment().getName());
        assertEquals(BigDecimal.valueOf(1000), response.getMinSalary());
        assertEquals(BigDecimal.valueOf(2000), response.getMaxSalary());
        verify(auditService).logAction(null, "CREATE", "DESIGNATION", 2L, "Team Lead");
    }

    // ---------- getAllDesignations ----------

    @Test
    void getAllDesignations_shouldReturnEmptyList_whenNoneExist() {
        when(designationRepository.findAll()).thenReturn(List.of());

        List<DesignationResponse> result = designationService.getAllDesignations();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getAllDesignations_shouldMapAllEntities() {
        Designation d1 = new Designation();
        d1.setId(1L);
        d1.setTitle("Engineer");
        d1.setCode("ENG1");

        Designation d2 = new Designation();
        d2.setId(2L);
        d2.setTitle("Manager");
        d2.setCode("MGR1");

        when(designationRepository.findAll()).thenReturn(List.of(d1, d2));

        List<DesignationResponse> result = designationService.getAllDesignations();

        assertEquals(2, result.size());
        assertEquals("Engineer", result.get(0).getTitle());
        assertEquals("Manager", result.get(1).getTitle());
    }

    @Test
    void getAllDesignations_shouldCountEmployees_whenPresent() {
        Designation d1 = new Designation();
        d1.setId(1L);
        d1.setTitle("Engineer");
        Set<EmployeeUser> employees = new HashSet<>();
        EmployeeUser emp1 = new EmployeeUser();
        emp1.setId(101L);
        EmployeeUser emp2 = new EmployeeUser();
        emp2.setId(102L);
        employees.add(emp1);
        employees.add(emp2);
        d1.setEmployees(employees);

        when(designationRepository.findAll()).thenReturn(List.of(d1));

        List<DesignationResponse> result = designationService.getAllDesignations();

        assertEquals(2L, result.get(0).getEmployeeCount());
    }

    // ---------- getDesignationById ----------

    @Test
    void getDesignationById_shouldReturnMappedResponse_whenFound() {
        Designation designation = new Designation();
        designation.setId(10L);
        designation.setTitle("Analyst");
        when(designationRepository.findById(10L)).thenReturn(Optional.of(designation));

        DesignationResponse response = designationService.getDesignationById(10L);

        assertEquals(10L, response.getId());
        assertEquals("Analyst", response.getTitle());
    }

    @Test
    void getDesignationById_shouldThrow_whenNotFound() {
        when(designationRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> designationService.getDesignationById(404L));
    }

    // ---------- getDesignationsByDepartment ----------

    @Test
    void getDesignationsByDepartment_shouldReturnMappedList() {
        Designation d1 = new Designation();
        d1.setId(1L);
        d1.setTitle("Engineer");
        when(designationRepository.findByDepartmentId(7L)).thenReturn(List.of(d1));

        List<DesignationResponse> result = designationService.getDesignationsByDepartment(7L);

        assertEquals(1, result.size());
        assertEquals("Engineer", result.get(0).getTitle());
    }

    @Test
    void getDesignationsByDepartment_shouldReturnEmptyList_whenNoneMatch() {
        when(designationRepository.findByDepartmentId(7L)).thenReturn(List.of());

        List<DesignationResponse> result = designationService.getDesignationsByDepartment(7L);

        assertTrue(result.isEmpty());
    }

    // ---------- updateDesignation ----------

    @Test
    void updateDesignation_shouldThrow_whenNotFound() {
        when(designationRepository.findById(1L)).thenReturn(Optional.empty());
        DesignationRequest request = new DesignationRequest();
        request.setTitle("Whatever");

        assertThrows(ResourceNotFoundException.class,
                () -> designationService.updateDesignation(1L, request));
    }

    @Test
    void updateDesignation_shouldThrow_whenTitleChangedToExistingTitle() {
        Designation existing = new Designation();
        existing.setId(1L);
        existing.setTitle("Old Title");
        when(designationRepository.findById(1L)).thenReturn(Optional.of(existing));

        DesignationRequest request = new DesignationRequest();
        request.setTitle("New Title");
        when(designationRepository.existsByTitle("New Title")).thenReturn(true);

        assertThrows(BadRequestException.class,
                () -> designationService.updateDesignation(1L, request));
        verify(designationRepository, never()).save(any());
    }

    @Test
    void updateDesignation_shouldNotCheckDuplicate_whenTitleUnchanged() {
        Designation existing = new Designation();
        existing.setId(1L);
        existing.setTitle("Same Title");
        when(designationRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(designationRepository.save(any(Designation.class))).thenAnswer(inv -> inv.getArgument(0));

        DesignationRequest request = new DesignationRequest();
        request.setTitle("Same Title");
        request.setLevel(2);
        request.setIsActive(true);
        request.setIsManagerial(false);

        DesignationResponse response = designationService.updateDesignation(1L, request);

        assertEquals("Same Title", response.getTitle());
        verify(designationRepository, never()).existsByTitle(anyString());
        verify(auditService).logAction(null, "UPDATE", "DESIGNATION", 1L, "Same Title");
    }

    @Test
    void updateDesignation_shouldUpdateCode_onlyWhenProvided() {
        Designation existing = new Designation();
        existing.setId(1L);
        existing.setTitle("Title");
        existing.setCode("OLD_CODE");
        when(designationRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(designationRepository.save(any(Designation.class))).thenAnswer(inv -> inv.getArgument(0));

        DesignationRequest request = new DesignationRequest();
        request.setTitle("Title");
        request.setCode(null);
        request.setLevel(1);
        request.setIsActive(true);
        request.setIsManagerial(false);

        DesignationResponse response = designationService.updateDesignation(1L, request);

        assertEquals("OLD_CODE", response.getCode());
    }

    @Test
    void updateDesignation_shouldSetDepartment_whenDepartmentIdProvided() {
        Designation existing = new Designation();
        existing.setId(1L);
        existing.setTitle("Title");
        when(designationRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(designationRepository.save(any(Designation.class))).thenAnswer(inv -> inv.getArgument(0));

        Department department = new Department();
        department.setId(3L);
        department.setName("Sales");
        when(departmentRepository.findById(3L)).thenReturn(Optional.of(department));

        DesignationRequest request = new DesignationRequest();
        request.setTitle("Title");
        request.setDepartmentId(3L);
        request.setLevel(1);
        request.setIsActive(true);
        request.setIsManagerial(false);

        DesignationResponse response = designationService.updateDesignation(1L, request);

        assertNotNull(response.getDepartment());
        assertEquals("Sales", response.getDepartment().getName());
    }

    @Test
    void updateDesignation_shouldThrow_whenDepartmentIdProvidedButNotFound() {
        Designation existing = new Designation();
        existing.setId(1L);
        existing.setTitle("Title");
        when(designationRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(departmentRepository.findById(3L)).thenReturn(Optional.empty());

        DesignationRequest request = new DesignationRequest();
        request.setTitle("Title");
        request.setDepartmentId(3L);

        assertThrows(ResourceNotFoundException.class,
                () -> designationService.updateDesignation(1L, request));
        verify(designationRepository, never()).save(any());
    }

    @Test
    void updateDesignation_shouldClearDepartment_whenDepartmentIdNull() {
        Department department = new Department();
        department.setId(3L);
        Designation existing = new Designation();
        existing.setId(1L);
        existing.setTitle("Title");
        existing.setDepartment(department);
        when(designationRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(designationRepository.save(any(Designation.class))).thenAnswer(inv -> inv.getArgument(0));

        DesignationRequest request = new DesignationRequest();
        request.setTitle("Title");
        request.setDepartmentId(null);
        request.setLevel(1);
        request.setIsActive(true);
        request.setIsManagerial(false);

        DesignationResponse response = designationService.updateDesignation(1L, request);

        assertNull(response.getDepartment());
    }

    // ---------- deleteDesignation ----------

    @Test
    void deleteDesignation_shouldSoftDelete_whenFound() {
        Designation existing = new Designation();
        existing.setId(1L);
        existing.setTitle("Title");
        existing.setIsActive(true);
        when(designationRepository.findById(1L)).thenReturn(Optional.of(existing));

        designationService.deleteDesignation(1L);

        assertFalse(existing.getIsActive());
        verify(designationRepository).save(existing);
        verify(auditService).logAction(null, "DELETE", "DESIGNATION", 1L, "Title");
    }

    @Test
    void deleteDesignation_shouldThrow_whenNotFound() {
        when(designationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> designationService.deleteDesignation(1L));
        verify(designationRepository, never()).save(any());
    }

    // ---------- generateCode (indirectly, via createDesignation) ----------

    @Test
    void generateCode_shouldSlugifyAndCollapseUnderscores() {
        DesignationRequest request = new DesignationRequest();
        request.setTitle("Sr.  Software   Engineer!!");
        when(designationRepository.existsByTitle(request.getTitle())).thenReturn(false);
        when(designationRepository.save(any(Designation.class))).thenAnswer(inv -> inv.getArgument(0));

        DesignationResponse response = designationService.createDesignation(request);

        assertEquals("SR_SOFTWARE_ENGINEER_", response.getCode());
    }
}
