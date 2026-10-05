package com.world.i2v.i2vauthapi.service;

import com.world.i2v.i2vauthapi.Config.ModuleRouteConfig;
import com.world.i2v.i2vauthapi.dto.request.ModuleRequest;
import com.world.i2v.i2vauthapi.dto.response.ModuleResponse;
import com.world.i2v.i2vauthapi.exception.BadRequestException;
import com.world.i2v.i2vauthapi.exception.ResourceNotFoundException;
import com.world.i2v.i2vauthapi.repository.ModuleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class ModuleService {

    private final ModuleRepository moduleRepository;
    private final ModuleRouteConfig moduleRouteConfig;

    public ModuleService(ModuleRepository moduleRepository, ModuleRouteConfig moduleRouteConfig) {
        this.moduleRepository = moduleRepository;
        this.moduleRouteConfig = moduleRouteConfig;
    }

    @CacheEvict(value = "userPermissions", allEntries = true)
    public ModuleResponse createModule(ModuleRequest request) {
        if (request.getCode() != null && moduleRepository.existsByCode(request.getCode())) {
            throw new BadRequestException("Module with code '" + request.getCode() + "' already exists");
        }

        if (moduleRepository.findByName(request.getName()).isPresent()) {
            throw new BadRequestException("Module with name '" + request.getName() + "' already exists");
        }

        com.world.i2v.i2vauthapi.entity.Module module = new com.world.i2v.i2vauthapi.entity.Module();
        module.setName(request.getName());
        module.setCode(request.getCode() != null ? request.getCode() : generateCode(request.getName()));
        module.setDescription(request.getDescription());
        module.setIcon(request.getIcon());
        module.setRoutePath(request.getRoutePath());
        module.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
        module.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        module.setIsVisible(request.getIsVisible() != null ? request.getIsVisible() : true);
        module.setRequiresPremium(request.getRequiresPremium() != null ? request.getRequiresPremium() : false);

        if (request.getParentModuleId() != null) {
            com.world.i2v.i2vauthapi.entity.Module parentModule = moduleRepository.findById(request.getParentModuleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent module not found"));
            module.setParentModule(parentModule);
        }

        com.world.i2v.i2vauthapi.entity.Module saved = moduleRepository.save(module);
        log.info("Module created: {} ({})", saved.getName(), saved.getCode());

        moduleRouteConfig.refreshRoutes();

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ModuleResponse> getAllModules() {
        try {
            List<com.world.i2v.i2vauthapi.entity.Module> modules = moduleRepository.findAll();
            if (modules == null || modules.isEmpty()) {
                log.info("No modules found in database");
                return new ArrayList<>();
            }
            return modules.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching all modules: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Transactional(readOnly = true)
    public List<ModuleResponse> getActiveModules() {
        try {
            List<com.world.i2v.i2vauthapi.entity.Module> modules = moduleRepository.findAllActiveOrdered();
            if (modules == null || modules.isEmpty()) {
                log.info("No active modules found");
                return new ArrayList<>();
            }
            return modules.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching active modules: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Transactional(readOnly = true)
    public ModuleResponse getModuleById(Long id) {
        com.world.i2v.i2vauthapi.entity.Module module = moduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Module", "id", id));
        return mapToResponse(module);
    }

    @CacheEvict(value = "userPermissions", allEntries = true)
    public ModuleResponse updateModule(Long id, ModuleRequest request) {
        com.world.i2v.i2vauthapi.entity.Module module = moduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Module", "id", id));

        if (request.getCode() != null && !request.getCode().equals(module.getCode())
                && moduleRepository.existsByCode(request.getCode())) {
            throw new BadRequestException("Module with code '" + request.getCode() + "' already exists");
        }

        module.setName(request.getName());
        if (request.getCode() != null) {
            module.setCode(request.getCode());
        }
        module.setDescription(request.getDescription());
        module.setIcon(request.getIcon());
        module.setRoutePath(request.getRoutePath());
        module.setDisplayOrder(request.getDisplayOrder());
        module.setIsActive(request.getIsActive());
        module.setIsVisible(request.getIsVisible());
        module.setRequiresPremium(request.getRequiresPremium());

        if (request.getParentModuleId() != null) {
            if (request.getParentModuleId().equals(id)) {
                throw new BadRequestException("Module cannot be its own parent");
            }
            com.world.i2v.i2vauthapi.entity.Module parentModule = moduleRepository.findById(request.getParentModuleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent module not found"));
            module.setParentModule(parentModule);
        } else {
            module.setParentModule(null);
        }

        com.world.i2v.i2vauthapi.entity.Module updated = moduleRepository.save(module);
        log.info("Module updated: {} ({})", updated.getName(), updated.getCode());

        moduleRouteConfig.refreshRoutes();

        return mapToResponse(updated);
    }

    @CacheEvict(value = "userPermissions", allEntries = true)
    public void deleteModule(Long id) {
        com.world.i2v.i2vauthapi.entity.Module module = moduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Module", "id", id));

        module.setIsActive(false);
        moduleRepository.save(module);
        log.info("Module deactivated: {} ({})", module.getName(), module.getCode());

        moduleRouteConfig.refreshRoutes();
    }

    private String generateCode(String name) {
        return name.toUpperCase()
                .replaceAll("[^A-Z0-9]", "_")
                .replaceAll("_+", "_");
    }

    private ModuleResponse mapToResponse(com.world.i2v.i2vauthapi.entity.Module module) {
        if (module == null) return null;

        ModuleResponse.ModuleResponseBuilder builder = ModuleResponse.builder()
                .id(module.getId())
                .name(module.getName())
                .code(module.getCode())
                .description(module.getDescription())
                .icon(module.getIcon())
                .routePath(module.getRoutePath())
                .displayOrder(module.getDisplayOrder())
                .isActive(module.getIsActive())
                .isVisible(module.getIsVisible())
                .requiresPremium(module.getRequiresPremium())
                .createdAt(module.getCreatedAt())
                .updatedAt(module.getUpdatedAt());

        if (module.getParentModule() != null) {
            builder.parentModuleId(module.getParentModule().getId());
            builder.parentModuleName(module.getParentModule().getName());
        }

        return builder.build();
    }
}