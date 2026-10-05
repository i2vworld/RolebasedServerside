package com.world.i2v.i2vauthapi.Controller;

import com.world.i2v.i2vauthapi.Config.ModuleRouteConfig;
import com.world.i2v.i2vauthapi.dto.request.ModuleRequest;
import com.world.i2v.i2vauthapi.dto.response.ApiResponse;
import com.world.i2v.i2vauthapi.dto.response.ModuleResponse;
import com.world.i2v.i2vauthapi.service.ModuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/modules")
@RequiredArgsConstructor
public class ModuleController {

    private final ModuleService moduleService;
    private final ModuleRouteConfig moduleRouteConfig;

    @GetMapping
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<ModuleResponse>>> getAllModules() {
        return listOrEmpty("Error getting all modules", moduleService::getAllModules);
    }

    @GetMapping("/active")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<ModuleResponse>>> getActiveModules() {
        return listOrEmpty("Error getting active modules", moduleService::getActiveModules);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<ModuleResponse>> getModuleById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.success(moduleService.getModuleById(id)));
        } catch (Exception e) {
            log.error("Error getting module by id {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Module not found"));
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('EMPLOYEE') and @securityUtils.canManageModules()")
    public ResponseEntity<ApiResponse<ModuleResponse>> createModule(@Valid @RequestBody ModuleRequest request) {
        ModuleResponse response = moduleService.createModule(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Module created successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('EMPLOYEE') and @securityUtils.canManageModules()")
    public ResponseEntity<ApiResponse<ModuleResponse>> updateModule(
            @PathVariable Long id, @Valid @RequestBody ModuleRequest request) {
        ModuleResponse response = moduleService.updateModule(id, request);
        return ResponseEntity.ok(ApiResponse.success("Module updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('EMPLOYEE') and @securityUtils.canManageModules()")
    public ResponseEntity<ApiResponse<Void>> deleteModule(@PathVariable Long id) {
        moduleService.deleteModule(id);
        return ResponseEntity.ok(ApiResponse.success("Module deleted successfully", null));
    }

    @PostMapping("/refresh-routes")
    @PreAuthorize("hasRole('EMPLOYEE') and @securityUtils.canManageModuleAccess()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> refreshModuleRoutes() {
        moduleRouteConfig.refreshRoutes();

        Map<String, Object> response = new HashMap<>();
        response.put("activeRoutes", moduleRouteConfig.getAllActiveRoutes().size());
        response.put("moduleCodes", moduleRouteConfig.getAllModuleCodes());
        response.put("functionalGroups", moduleRouteConfig.getAllFunctionalGroups());
        response.put("routes", moduleRouteConfig.getAllRouteInfos().stream()
                .map(this::toRouteInfoMap)
                .collect(Collectors.toList()));

        log.info("Module routes refreshed. Active routes: {}", moduleRouteConfig.getAllActiveRoutes().size());
        return ResponseEntity.ok(ApiResponse.success("Module routes refreshed from database", response));
    }

    @GetMapping("/active-routes")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getActiveRoutes() {
        List<Map<String, Object>> routes = moduleRouteConfig.getAllRouteInfos().stream()
                .filter(info -> Boolean.TRUE.equals(info.getIsVisible()))
                .sorted(Comparator.comparing(
                        info -> info.getDisplayOrder() != null ? info.getDisplayOrder() : 999))
                .map(info -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("moduleId", info.getModuleId());
                    map.put("moduleCode", info.getModuleCode());
                    map.put("moduleName", info.getModuleName());
                    map.put("routePath", info.getPrimaryRoute());
                    map.put("icon", info.getIcon());
                    map.put("requiresPremium", info.getRequiresPremium());
                    return map;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(routes));
    }

    // ==================== HELPERS ====================

    private Map<String, Object> toRouteInfoMap(ModuleRouteConfig.ModuleRouteInfo info) {
        Map<String, Object> map = new HashMap<>();
        map.put("moduleId", info.getModuleId());
        map.put("moduleCode", info.getModuleCode());
        map.put("moduleName", info.getModuleName());
        map.put("primaryRoute", info.getPrimaryRoute());
        map.put("alternativeRoutes", info.getAlternativeRoutes());
        return map;
    }

    private ResponseEntity<ApiResponse<List<ModuleResponse>>> listOrEmpty(
            String errorMessage, java.util.function.Supplier<List<ModuleResponse>> supplier) {
        try {
            return ResponseEntity.ok(ApiResponse.success(supplier.get()));
        } catch (Exception e) {
            log.error("{}: {}", errorMessage, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.success(new ArrayList<>()));
        }
    }
}
