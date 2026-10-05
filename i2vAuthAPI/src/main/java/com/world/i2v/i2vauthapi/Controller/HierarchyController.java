package com.world.i2v.i2vauthapi.Controller;

import com.world.i2v.i2vauthapi.dto.request.FotAccessLevelRequest;
import com.world.i2v.i2vauthapi.dto.request.SquadRequest;
import com.world.i2v.i2vauthapi.dto.request.TerritoryRequest;
import com.world.i2v.i2vauthapi.dto.request.UnitRequest;
import com.world.i2v.i2vauthapi.dto.response.ApiResponse;
import com.world.i2v.i2vauthapi.dto.response.FotAccessLevelResponse;
import com.world.i2v.i2vauthapi.dto.response.SquadResponse;
import com.world.i2v.i2vauthapi.dto.response.TerritoryResponse;
import com.world.i2v.i2vauthapi.dto.response.UnitResponse;
import com.world.i2v.i2vauthapi.service.HierarchyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.function.Supplier;

@Slf4j
@RestController
@RequestMapping("/api/hierarchy")
@RequiredArgsConstructor
public class HierarchyController {

    private final HierarchyService hierarchyService;

    // ===================== FULL TREE =====================

    @GetMapping("/full")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<UnitResponse>>> getFullHierarchy() {
        return listOrEmpty("Error getting full hierarchy", hierarchyService::getFullHierarchy);
    }

    // ===================== UNITS =====================

    @GetMapping("/units")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<UnitResponse>>> getAllUnits() {
        return listOrEmpty("Error getting units", hierarchyService::getAllUnits);
    }

    @GetMapping("/units/{id}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<UnitResponse>> getUnitById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.success(hierarchyService.getUnitById(id)));
        } catch (Exception e) {
            log.error("Error getting unit {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Unit not found"));
        }
    }

    @PostMapping("/units")
    @PreAuthorize("hasRole('EMPLOYEE') and @securityUtils.canManageDepartments()")
    public ResponseEntity<ApiResponse<UnitResponse>> createUnit(@Valid @RequestBody UnitRequest request) {
        UnitResponse response = hierarchyService.createUnit(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Unit created successfully", response));
    }

    @PutMapping("/units/{id}")
    @PreAuthorize("hasRole('EMPLOYEE') and @securityUtils.canManageDepartments()")
    public ResponseEntity<ApiResponse<UnitResponse>> updateUnit(
            @PathVariable Long id, @Valid @RequestBody UnitRequest request) {
        UnitResponse response = hierarchyService.updateUnit(id, request);
        return ResponseEntity.ok(ApiResponse.success("Unit updated successfully", response));
    }

    @DeleteMapping("/units/{id}")
    @PreAuthorize("hasRole('EMPLOYEE') and @securityUtils.canManageDepartments()")
    public ResponseEntity<ApiResponse<Void>> deleteUnit(@PathVariable Long id) {
        hierarchyService.deleteUnit(id);
        return ResponseEntity.ok(ApiResponse.success("Unit deleted successfully", null));
    }

    // ===================== SQUADS =====================

    @GetMapping("/squads")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<SquadResponse>>> getAllSquads() {
        return listOrEmpty("Error getting squads", hierarchyService::getAllSquads);
    }

    @GetMapping("/squads/by-unit/{unitId}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<SquadResponse>>> getSquadsByUnit(@PathVariable Long unitId) {
        return listOrEmpty("Error getting squads for unit " + unitId,
                () -> hierarchyService.getSquadsByUnit(unitId));
    }

    @PostMapping("/squads")
    @PreAuthorize("hasRole('EMPLOYEE') and @securityUtils.canManageDepartments()")
    public ResponseEntity<ApiResponse<SquadResponse>> createSquad(@Valid @RequestBody SquadRequest request) {
        SquadResponse response = hierarchyService.createSquad(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Squad created successfully", response));
    }

    @PutMapping("/squads/{id}")
    @PreAuthorize("hasRole('EMPLOYEE') and @securityUtils.canManageDepartments()")
    public ResponseEntity<ApiResponse<SquadResponse>> updateSquad(
            @PathVariable Long id, @Valid @RequestBody SquadRequest request) {
        SquadResponse response = hierarchyService.updateSquad(id, request);
        return ResponseEntity.ok(ApiResponse.success("Squad updated successfully", response));
    }

    @DeleteMapping("/squads/{id}")
    @PreAuthorize("hasRole('EMPLOYEE') and @securityUtils.canManageDepartments()")
    public ResponseEntity<ApiResponse<Void>> deleteSquad(@PathVariable Long id) {
        hierarchyService.deleteSquad(id);
        return ResponseEntity.ok(ApiResponse.success("Squad deleted successfully", null));
    }

    // ===================== TERRITORIES =====================

    @GetMapping("/territories")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<TerritoryResponse>>> getAllTerritories() {
        return listOrEmpty("Error getting territories", hierarchyService::getAllTerritories);
    }

    @GetMapping("/territories/by-squad/{squadId}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<TerritoryResponse>>> getTerritoriesBySquad(@PathVariable Long squadId) {
        return listOrEmpty("Error getting territories for squad " + squadId,
                () -> hierarchyService.getTerritoriesBySquad(squadId));
    }

    @PostMapping("/territories")
    @PreAuthorize("hasRole('EMPLOYEE') and @securityUtils.canManageDepartments()")
    public ResponseEntity<ApiResponse<TerritoryResponse>> createTerritory(@Valid @RequestBody TerritoryRequest request) {
        TerritoryResponse response = hierarchyService.createTerritory(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Territory created successfully", response));
    }

    @PutMapping("/territories/{id}")
    @PreAuthorize("hasRole('EMPLOYEE') and @securityUtils.canManageDepartments()")
    public ResponseEntity<ApiResponse<TerritoryResponse>> updateTerritory(
            @PathVariable Long id, @Valid @RequestBody TerritoryRequest request) {
        TerritoryResponse response = hierarchyService.updateTerritory(id, request);
        return ResponseEntity.ok(ApiResponse.success("Territory updated successfully", response));
    }

    @DeleteMapping("/territories/{id}")
    @PreAuthorize("hasRole('EMPLOYEE') and @securityUtils.canManageDepartments()")
    public ResponseEntity<ApiResponse<Void>> deleteTerritory(@PathVariable Long id) {
        hierarchyService.deleteTerritory(id);
        return ResponseEntity.ok(ApiResponse.success("Territory deleted successfully", null));
    }

    // ===================== FOT ACCESS LEVELS =====================

    @GetMapping("/fot-access-levels")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<FotAccessLevelResponse>>> getAllFotAccessLevels() {
        return listOrEmpty("Error getting FOT access levels", hierarchyService::getAllFotAccessLevels);
    }

    @GetMapping("/fot-access-levels/by-territory/{territoryId}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<FotAccessLevelResponse>>> getFotByTerritory(@PathVariable Long territoryId) {
        return listOrEmpty("Error getting FOT levels for territory " + territoryId,
                () -> hierarchyService.getFotByTerritory(territoryId));
    }

    @PostMapping("/fot-access-levels")
    @PreAuthorize("hasRole('EMPLOYEE') and @securityUtils.canManageDepartments()")
    public ResponseEntity<ApiResponse<FotAccessLevelResponse>> createFotAccessLevel(
            @Valid @RequestBody FotAccessLevelRequest request) {
        FotAccessLevelResponse response = hierarchyService.createFotAccessLevel(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("FOT Access Level created successfully", response));
    }

    @PutMapping("/fot-access-levels/{id}")
    @PreAuthorize("hasRole('EMPLOYEE') and @securityUtils.canManageDepartments()")
    public ResponseEntity<ApiResponse<FotAccessLevelResponse>> updateFotAccessLevel(
            @PathVariable Long id, @Valid @RequestBody FotAccessLevelRequest request) {
        FotAccessLevelResponse response = hierarchyService.updateFotAccessLevel(id, request);
        return ResponseEntity.ok(ApiResponse.success("FOT Access Level updated successfully", response));
    }

    @DeleteMapping("/fot-access-levels/{id}")
    @PreAuthorize("hasRole('EMPLOYEE') and @securityUtils.canManageDepartments()")
    public ResponseEntity<ApiResponse<Void>> deleteFotAccessLevel(@PathVariable Long id) {
        hierarchyService.deleteFotAccessLevel(id);
        return ResponseEntity.ok(ApiResponse.success("FOT Access Level deleted successfully", null));
    }

    // ===================== HELPERS =====================

    private <T> ResponseEntity<ApiResponse<List<T>>> listOrEmpty(String errorMessage, Supplier<List<T>> supplier) {
        try {
            return ResponseEntity.ok(ApiResponse.success(supplier.get()));
        } catch (Exception e) {
            log.error("{}: {}", errorMessage, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.success(List.of()));
        }
    }
}
