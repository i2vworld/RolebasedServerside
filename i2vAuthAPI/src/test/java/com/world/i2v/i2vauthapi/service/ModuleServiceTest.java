package com.world.i2v.i2vauthapi.service;

import com.world.i2v.i2vauthapi.Config.ModuleRouteConfig;
import com.world.i2v.i2vauthapi.dto.request.ModuleRequest;
import com.world.i2v.i2vauthapi.dto.response.ModuleResponse;
import com.world.i2v.i2vauthapi.entity.Module;
import com.world.i2v.i2vauthapi.exception.BadRequestException;
import com.world.i2v.i2vauthapi.exception.ResourceNotFoundException;
import com.world.i2v.i2vauthapi.repository.ModuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ModuleServiceTest {

    private ModuleRepository moduleRepository;
    private ModuleRouteConfig moduleRouteConfig;
    private ModuleService moduleService;

    @BeforeEach
    void setUp() {
        moduleRepository = mock(ModuleRepository.class);
        moduleRouteConfig = mock(ModuleRouteConfig.class);
        moduleService = new ModuleService(moduleRepository, moduleRouteConfig);
    }

    private ModuleRequest buildRequest(String name, String code) {
        ModuleRequest request = new ModuleRequest();
        request.setName(name);
        request.setCode(code);
        request.setDescription("desc");
        request.setIcon("icon");
        request.setRoutePath("/route");
        request.setDisplayOrder(1);
        request.setIsActive(true);
        request.setIsVisible(true);
        request.setRequiresPremium(false);
        return request;
    }

    private Module buildModule(Long id, String name, String code) {
        Module module = new Module();
        module.setId(id);
        module.setName(name);
        module.setCode(code);
        module.setDescription("desc");
        module.setIcon("icon");
        module.setRoutePath("/route");
        module.setDisplayOrder(1);
        module.setIsActive(true);
        module.setIsVisible(true);
        module.setRequiresPremium(false);
        return module;
    }

    // ---------- Smoke test ----------

    @Test
    void smokeTest_serviceCanBeConstructed() {
        assertNotNull(moduleService);
    }

    // ---------- createModule ----------

    @Test
    void createModule_duplicateCode_throwsBadRequestException() {
        ModuleRequest request = buildRequest("Employee Mgmt", "EMP");
        when(moduleRepository.existsByCode("EMP")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> moduleService.createModule(request));
        verify(moduleRepository, never()).save(any());
    }

    @Test
    void createModule_duplicateName_throwsBadRequestException() {
        ModuleRequest request = buildRequest("Employee Mgmt", "EMP");
        when(moduleRepository.existsByCode("EMP")).thenReturn(false);
        when(moduleRepository.findByName("Employee Mgmt")).thenReturn(Optional.of(buildModule(1L, "Employee Mgmt", "EMP")));

        assertThrows(BadRequestException.class, () -> moduleService.createModule(request));
        verify(moduleRepository, never()).save(any());
    }

    @Test
    void createModule_success_withCode() {
        ModuleRequest request = buildRequest("Employee Mgmt", "EMP");
        when(moduleRepository.existsByCode("EMP")).thenReturn(false);
        when(moduleRepository.findByName("Employee Mgmt")).thenReturn(Optional.empty());
        when(moduleRepository.save(any(Module.class))).thenAnswer(invocation -> {
            Module module = invocation.getArgument(0);
            module.setId(1L);
            return module;
        });

        ModuleResponse response = moduleService.createModule(request);

        assertNotNull(response);
        assertEquals("EMP", response.getCode());
        assertEquals("Employee Mgmt", response.getName());
    }

    @Test
    void createModule_success_withoutCode_generatesCodeFromName() {
        ModuleRequest request = buildRequest("Employee Mgmt", null);
        when(moduleRepository.findByName("Employee Mgmt")).thenReturn(Optional.empty());
        when(moduleRepository.save(any(Module.class))).thenAnswer(invocation -> {
            Module module = invocation.getArgument(0);
            module.setId(1L);
            return module;
        });

        ModuleResponse response = moduleService.createModule(request);

        assertEquals("EMPLOYEE_MGMT", response.getCode());
        verify(moduleRepository, never()).existsByCode(any());
    }

    @Test
    void createModule_withParentModule_setsParent() {
        ModuleRequest request = buildRequest("Child", "CHILD");
        request.setParentModuleId(5L);
        Module parent = buildModule(5L, "Parent", "PARENT");

        when(moduleRepository.existsByCode("CHILD")).thenReturn(false);
        when(moduleRepository.findByName("Child")).thenReturn(Optional.empty());
        when(moduleRepository.findById(5L)).thenReturn(Optional.of(parent));
        when(moduleRepository.save(any(Module.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ModuleResponse response = moduleService.createModule(request);

        assertEquals(5L, response.getParentModuleId());
        assertEquals("Parent", response.getParentModuleName());
    }

    @Test
    void createModule_withInvalidParentModule_throwsResourceNotFoundException() {
        ModuleRequest request = buildRequest("Child", "CHILD");
        request.setParentModuleId(99L);

        when(moduleRepository.existsByCode("CHILD")).thenReturn(false);
        when(moduleRepository.findByName("Child")).thenReturn(Optional.empty());
        when(moduleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> moduleService.createModule(request));
    }

    // ---------- getAllModules ----------

    @Test
    void getAllModules_returnsMappedList() {
        when(moduleRepository.findAll()).thenReturn(List.of(buildModule(1L, "A", "A_CODE")));

        List<ModuleResponse> result = moduleService.getAllModules();

        assertEquals(1, result.size());
        assertEquals("A_CODE", result.get(0).getCode());
    }

    @Test
    void getAllModules_empty_returnsEmptyList() {
        when(moduleRepository.findAll()).thenReturn(Collections.emptyList());

        List<ModuleResponse> result = moduleService.getAllModules();

        assertTrue(result.isEmpty());
    }

    @Test
    void getAllModules_repositoryThrows_returnsEmptyList() {
        when(moduleRepository.findAll()).thenThrow(new RuntimeException("db error"));

        List<ModuleResponse> result = moduleService.getAllModules();

        assertTrue(result.isEmpty());
    }

    // ---------- getActiveModules ----------

    @Test
    void getActiveModules_returnsMappedList() {
        when(moduleRepository.findAllActiveOrdered()).thenReturn(List.of(buildModule(1L, "A", "A_CODE")));

        List<ModuleResponse> result = moduleService.getActiveModules();

        assertEquals(1, result.size());
    }

    @Test
    void getActiveModules_empty_returnsEmptyList() {
        when(moduleRepository.findAllActiveOrdered()).thenReturn(Collections.emptyList());

        List<ModuleResponse> result = moduleService.getActiveModules();

        assertTrue(result.isEmpty());
    }

    @Test
    void getActiveModules_repositoryThrows_returnsEmptyList() {
        when(moduleRepository.findAllActiveOrdered()).thenThrow(new RuntimeException("db error"));

        List<ModuleResponse> result = moduleService.getActiveModules();

        assertTrue(result.isEmpty());
    }

    // ---------- getModuleById ----------

    @Test
    void getModuleById_found_returnsResponse() {
        when(moduleRepository.findById(1L)).thenReturn(Optional.of(buildModule(1L, "A", "A_CODE")));

        ModuleResponse response = moduleService.getModuleById(1L);

        assertEquals(1L, response.getId());
    }

    @Test
    void getModuleById_notFound_throwsResourceNotFoundException() {
        when(moduleRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> moduleService.getModuleById(1L));
    }

    // ---------- updateModule ----------

    @Test
    void updateModule_notFound_throwsResourceNotFoundException() {
        ModuleRequest request = buildRequest("A", "A_CODE");
        when(moduleRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> moduleService.updateModule(1L, request));
    }

    @Test
    void updateModule_duplicateCode_throwsBadRequestException() {
        Module existing = buildModule(1L, "A", "A_CODE");
        ModuleRequest request = buildRequest("A", "B_CODE");

        when(moduleRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(moduleRepository.existsByCode("B_CODE")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> moduleService.updateModule(1L, request));
    }

    @Test
    void updateModule_success() {
        Module existing = buildModule(1L, "A", "A_CODE");
        ModuleRequest request = buildRequest("Updated", "A_CODE");

        when(moduleRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(moduleRepository.save(any(Module.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ModuleResponse response = moduleService.updateModule(1L, request);

        assertEquals("Updated", response.getName());
    }

    @Test
    void updateModule_selfAsParent_throwsBadRequestException() {
        Module existing = buildModule(1L, "A", "A_CODE");
        ModuleRequest request = buildRequest("A", "A_CODE");
        request.setParentModuleId(1L);

        when(moduleRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThrows(BadRequestException.class, () -> moduleService.updateModule(1L, request));
    }

    @Test
    void updateModule_withValidParent_setsParent() {
        Module existing = buildModule(1L, "A", "A_CODE");
        Module parent = buildModule(2L, "Parent", "PARENT_CODE");
        ModuleRequest request = buildRequest("A", "A_CODE");
        request.setParentModuleId(2L);

        when(moduleRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(moduleRepository.findById(2L)).thenReturn(Optional.of(parent));
        when(moduleRepository.save(any(Module.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ModuleResponse response = moduleService.updateModule(1L, request);

        assertEquals(2L, response.getParentModuleId());
    }

    @Test
    void updateModule_withInvalidParent_throwsResourceNotFoundException() {
        Module existing = buildModule(1L, "A", "A_CODE");
        ModuleRequest request = buildRequest("A", "A_CODE");
        request.setParentModuleId(99L);

        when(moduleRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(moduleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> moduleService.updateModule(1L, request));
    }

    @Test
    void updateModule_nullParentModuleId_clearsParent() {
        Module existing = buildModule(1L, "A", "A_CODE");
        existing.setParentModule(buildModule(2L, "Parent", "PARENT_CODE"));
        ModuleRequest request = buildRequest("A", "A_CODE");
        request.setParentModuleId(null);

        when(moduleRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(moduleRepository.save(any(Module.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ModuleResponse response = moduleService.updateModule(1L, request);

        assertNull(response.getParentModuleId());
    }

    // ---------- deleteModule ----------

    @Test
    void deleteModule_notFound_throwsResourceNotFoundException() {
        when(moduleRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> moduleService.deleteModule(1L));
    }

    @Test
    void deleteModule_success_deactivatesModule() {
        Module existing = buildModule(1L, "A", "A_CODE");
        when(moduleRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(moduleRepository.save(any(Module.class))).thenAnswer(invocation -> invocation.getArgument(0));

        moduleService.deleteModule(1L);

        assertFalse(existing.getIsActive());
        verify(moduleRepository).save(existing);
    }
}
