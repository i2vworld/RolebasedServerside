package com.world.i2v.i2vauthapi.service;

import com.world.i2v.i2vauthapi.dto.request.FotAccessLevelRequest;
import com.world.i2v.i2vauthapi.dto.request.SquadRequest;
import com.world.i2v.i2vauthapi.dto.request.TerritoryRequest;
import com.world.i2v.i2vauthapi.dto.request.UnitRequest;
import com.world.i2v.i2vauthapi.dto.response.FotAccessLevelResponse;
import com.world.i2v.i2vauthapi.dto.response.SquadResponse;
import com.world.i2v.i2vauthapi.dto.response.TerritoryResponse;
import com.world.i2v.i2vauthapi.dto.response.UnitResponse;
import com.world.i2v.i2vauthapi.entity.FotAccessLevel;
import com.world.i2v.i2vauthapi.entity.Squad;
import com.world.i2v.i2vauthapi.entity.Territory;
import com.world.i2v.i2vauthapi.entity.Unit;
import com.world.i2v.i2vauthapi.exception.BadRequestException;
import com.world.i2v.i2vauthapi.exception.ResourceNotFoundException;
import com.world.i2v.i2vauthapi.repository.FotAccessLevelRepository;
import com.world.i2v.i2vauthapi.repository.SquadRepository;
import com.world.i2v.i2vauthapi.repository.TerritoryRepository;
import com.world.i2v.i2vauthapi.repository.UnitRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class HierarchyService {

    private final UnitRepository unitRepository;
    private final SquadRepository squadRepository;
    private final TerritoryRepository territoryRepository;
    private final FotAccessLevelRepository fotAccessLevelRepository;

    public HierarchyService(UnitRepository unitRepository,
                            SquadRepository squadRepository,
                            TerritoryRepository territoryRepository,
                            FotAccessLevelRepository fotAccessLevelRepository) {
        this.unitRepository = unitRepository;
        this.squadRepository = squadRepository;
        this.territoryRepository = territoryRepository;
        this.fotAccessLevelRepository = fotAccessLevelRepository;
    }

    // ===================== UNIT =====================

    @Transactional(readOnly = true)
    public List<UnitResponse> getAllUnits() {
        return unitRepository.findAll().stream()
                .map(this::mapUnitToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UnitResponse getUnitById(Long id) {
        Unit unit = unitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found with id: " + id));
        return mapUnitWithChildrenToResponse(unit);
    }

    public UnitResponse createUnit(UnitRequest request) {
        if (unitRepository.existsByName(request.getName())) {
            throw new BadRequestException("Unit with this name already exists");
        }
        if (request.getCode() != null && !request.getCode().isBlank()
                && unitRepository.existsByCode(request.getCode())) {
            throw new BadRequestException("Unit with this code already exists");
        }

        Unit unit = new Unit();
        unit.setName(request.getName());
        unit.setCode(request.getCode() != null ? request.getCode().toUpperCase() : generateCode(request.getName()));
        unit.setDescription(request.getDescription());
        unit.setCountry(request.getCountry());
        unit.setAccessLevel(request.getAccessLevel() != null ? request.getAccessLevel() : 1);
        unit.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        Unit saved = unitRepository.save(unit);
        log.info("Created unit: {}", saved.getName());
        return mapUnitToResponse(saved);
    }

    public UnitResponse updateUnit(Long id, UnitRequest request) {
        Unit unit = unitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found with id: " + id));

        if (!unit.getName().equals(request.getName()) && unitRepository.existsByName(request.getName())) {
            throw new BadRequestException("Unit with this name already exists");
        }

        unit.setName(request.getName());
        if (request.getCode() != null && !request.getCode().isBlank()) {
            unit.setCode(request.getCode().toUpperCase());
        }
        unit.setDescription(request.getDescription());
        unit.setCountry(request.getCountry());
        if (request.getAccessLevel() != null) unit.setAccessLevel(request.getAccessLevel());
        if (request.getIsActive() != null) unit.setIsActive(request.getIsActive());

        Unit saved = unitRepository.save(unit);
        return mapUnitToResponse(saved);
    }

    public void deleteUnit(Long id) {
        Unit unit = unitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found with id: " + id));
        if (!unit.getSquads().isEmpty()) {
            throw new BadRequestException("Cannot delete unit with existing squads. Remove squads first.");
        }
        unitRepository.delete(unit);
        log.info("Deleted unit id: {}", id);
    }

    // ===================== SQUAD =====================

    @Transactional(readOnly = true)
    public List<SquadResponse> getAllSquads() {
        return squadRepository.findAll().stream()
                .map(this::mapSquadToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SquadResponse> getSquadsByUnit(Long unitId) {
        return squadRepository.findByUnitId(unitId).stream()
                .map(this::mapSquadToResponse)
                .collect(Collectors.toList());
    }

    public SquadResponse createSquad(SquadRequest request) {
        Unit unit = unitRepository.findById(request.getUnitId())
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found with id: " + request.getUnitId()));

        if (squadRepository.existsByNameAndUnitId(request.getName(), request.getUnitId())) {
            throw new BadRequestException("Squad with this name already exists in the unit");
        }

        Squad squad = new Squad();
        squad.setName(request.getName());
        squad.setCode(request.getCode() != null ? request.getCode().toUpperCase() : generateCode(request.getName()));
        squad.setDescription(request.getDescription());
        squad.setUnit(unit);
        squad.setAccessLevel(request.getAccessLevel() != null ? request.getAccessLevel() : 1);
        squad.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);

        Squad saved = squadRepository.save(squad);
        log.info("Created squad: {} under unit: {}", saved.getName(), unit.getName());
        return mapSquadToResponse(saved);
    }

    public SquadResponse updateSquad(Long id, SquadRequest request) {
        Squad squad = squadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Squad not found with id: " + id));

        Unit unit = unitRepository.findById(request.getUnitId())
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found with id: " + request.getUnitId()));

        squad.setName(request.getName());
        if (request.getCode() != null && !request.getCode().isBlank()) {
            squad.setCode(request.getCode().toUpperCase());
        }
        squad.setDescription(request.getDescription());
        squad.setUnit(unit);
        if (request.getAccessLevel() != null) squad.setAccessLevel(request.getAccessLevel());
        if (request.getIsActive() != null) squad.setIsActive(request.getIsActive());

        Squad saved = squadRepository.save(squad);
        return mapSquadToResponse(saved);
    }

    public void deleteSquad(Long id) {
        Squad squad = squadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Squad not found with id: " + id));
        if (!squad.getTerritories().isEmpty()) {
            throw new BadRequestException("Cannot delete squad with existing territories. Remove territories first.");
        }
        squadRepository.delete(squad);
        log.info("Deleted squad id: {}", id);
    }

    // ===================== TERRITORY =====================

    @Transactional(readOnly = true)
    public List<TerritoryResponse> getAllTerritories() {
        return territoryRepository.findAll().stream()
                .map(this::mapTerritoryToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TerritoryResponse> getTerritoriesBySquad(Long squadId) {
        return territoryRepository.findBySquadId(squadId).stream()
                .map(this::mapTerritoryToResponse)
                .collect(Collectors.toList());
    }

    public TerritoryResponse createTerritory(TerritoryRequest request) {
        Squad squad = squadRepository.findById(request.getSquadId())
                .orElseThrow(() -> new ResourceNotFoundException("Squad not found with id: " + request.getSquadId()));

        if (territoryRepository.existsByNameAndSquadId(request.getName(), request.getSquadId())) {
            throw new BadRequestException("Territory with this name already exists in the squad");
        }

        Territory territory = new Territory();
        territory.setName(request.getName());
        territory.setCode(request.getCode() != null ? request.getCode().toUpperCase() : generateCode(request.getName()));
        territory.setDescription(request.getDescription());
        territory.setSquad(squad);
        territory.setAccessLevel(request.getAccessLevel() != null ? request.getAccessLevel() : 1);
        territory.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);

        Territory saved = territoryRepository.save(territory);
        log.info("Created territory: {} under squad: {}", saved.getName(), squad.getName());
        return mapTerritoryToResponse(saved);
    }

    public TerritoryResponse updateTerritory(Long id, TerritoryRequest request) {
        Territory territory = territoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Territory not found with id: " + id));

        Squad squad = squadRepository.findById(request.getSquadId())
                .orElseThrow(() -> new ResourceNotFoundException("Squad not found with id: " + request.getSquadId()));

        territory.setName(request.getName());
        if (request.getCode() != null && !request.getCode().isBlank()) {
            territory.setCode(request.getCode().toUpperCase());
        }
        territory.setDescription(request.getDescription());
        territory.setSquad(squad);
        if (request.getAccessLevel() != null) territory.setAccessLevel(request.getAccessLevel());
        if (request.getIsActive() != null) territory.setIsActive(request.getIsActive());

        Territory saved = territoryRepository.save(territory);
        return mapTerritoryToResponse(saved);
    }

    public void deleteTerritory(Long id) {
        Territory territory = territoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Territory not found with id: " + id));
        if (!territory.getFotAccessLevels().isEmpty()) {
            throw new BadRequestException("Cannot delete territory with existing FOT access levels. Remove them first.");
        }
        territoryRepository.delete(territory);
        log.info("Deleted territory id: {}", id);
    }

    // ===================== FOT ACCESS LEVEL =====================

    @Transactional(readOnly = true)
    public List<FotAccessLevelResponse> getAllFotAccessLevels() {
        return fotAccessLevelRepository.findAll().stream()
                .map(this::mapFotToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FotAccessLevelResponse> getFotByTerritory(Long territoryId) {
        return fotAccessLevelRepository.findByTerritoryId(territoryId).stream()
                .map(this::mapFotToResponse)
                .collect(Collectors.toList());
    }

    public FotAccessLevelResponse createFotAccessLevel(FotAccessLevelRequest request) {
        Territory territory = territoryRepository.findById(request.getTerritoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Territory not found with id: " + request.getTerritoryId()));

        FotAccessLevel fot = new FotAccessLevel();
        fot.setName(request.getName());
        fot.setCode(request.getCode() != null ? request.getCode().toUpperCase() : generateCode(request.getName()));
        fot.setDescription(request.getDescription());
        fot.setTerritory(territory);
        fot.setAccessLevel(request.getAccessLevel() != null ? request.getAccessLevel() : 1);
        fot.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);

        FotAccessLevel saved = fotAccessLevelRepository.save(fot);
        log.info("Created FOT access level: {} under territory: {}", saved.getName(), territory.getName());
        return mapFotToResponse(saved);
    }

    public FotAccessLevelResponse updateFotAccessLevel(Long id, FotAccessLevelRequest request) {
        FotAccessLevel fot = fotAccessLevelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FOT Access Level not found with id: " + id));

        Territory territory = territoryRepository.findById(request.getTerritoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Territory not found with id: " + request.getTerritoryId()));

        fot.setName(request.getName());
        if (request.getCode() != null && !request.getCode().isBlank()) {
            fot.setCode(request.getCode().toUpperCase());
        }
        fot.setDescription(request.getDescription());
        fot.setTerritory(territory);
        if (request.getAccessLevel() != null) fot.setAccessLevel(request.getAccessLevel());
        if (request.getIsActive() != null) fot.setIsActive(request.getIsActive());

        FotAccessLevel saved = fotAccessLevelRepository.save(fot);
        return mapFotToResponse(saved);
    }

    public void deleteFotAccessLevel(Long id) {
        FotAccessLevel fot = fotAccessLevelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FOT Access Level not found with id: " + id));
        fotAccessLevelRepository.delete(fot);
        log.info("Deleted FOT access level id: {}", id);
    }

    // ===================== FULL TREE =====================

    @Transactional(readOnly = true)
    public List<UnitResponse> getFullHierarchy() {
        return unitRepository.findAll().stream()
                .map(this::mapUnitWithChildrenToResponse)
                .collect(Collectors.toList());
    }

    // ===================== MAPPERS =====================

    private UnitResponse mapUnitToResponse(Unit unit) {
        return UnitResponse.builder()
                .id(unit.getId())
                .name(unit.getName())
                .code(unit.getCode())
                .description(unit.getDescription())
                .country(unit.getCountry())
                .accessLevel(unit.getAccessLevel())
                .isActive(unit.getIsActive())
                .createdAt(unit.getCreatedAt())
                .updatedAt(unit.getUpdatedAt())
                .squadCount(unit.getSquads().size())
                .build();
    }

    private UnitResponse mapUnitWithChildrenToResponse(Unit unit) {
        List<SquadResponse> squads = unit.getSquads().stream()
                .map(this::mapSquadWithChildrenToResponse)
                .collect(Collectors.toList());
        return UnitResponse.builder()
                .id(unit.getId())
                .name(unit.getName())
                .code(unit.getCode())
                .description(unit.getDescription())
                .country(unit.getCountry())
                .accessLevel(unit.getAccessLevel())
                .isActive(unit.getIsActive())
                .createdAt(unit.getCreatedAt())
                .updatedAt(unit.getUpdatedAt())
                .squads(squads)
                .squadCount(squads.size())
                .build();
    }

    private SquadResponse mapSquadToResponse(Squad squad) {
        return SquadResponse.builder()
                .id(squad.getId())
                .name(squad.getName())
                .code(squad.getCode())
                .description(squad.getDescription())
                .unitId(squad.getUnit().getId())
                .unitName(squad.getUnit().getName())
                .country(squad.getUnit().getCountry())
                .accessLevel(squad.getAccessLevel())
                .isActive(squad.getIsActive())
                .createdAt(squad.getCreatedAt())
                .updatedAt(squad.getUpdatedAt())
                .territoryCount(squad.getTerritories().size())
                .build();
    }

    private SquadResponse mapSquadWithChildrenToResponse(Squad squad) {
        List<TerritoryResponse> territories = squad.getTerritories().stream()
                .map(this::mapTerritoryWithChildrenToResponse)
                .collect(Collectors.toList());
        return SquadResponse.builder()
                .id(squad.getId())
                .name(squad.getName())
                .code(squad.getCode())
                .description(squad.getDescription())
                .unitId(squad.getUnit().getId())
                .unitName(squad.getUnit().getName())
                .country(squad.getUnit().getCountry())
                .accessLevel(squad.getAccessLevel())
                .isActive(squad.getIsActive())
                .createdAt(squad.getCreatedAt())
                .updatedAt(squad.getUpdatedAt())
                .territories(territories)
                .territoryCount(territories.size())
                .build();
    }

    private TerritoryResponse mapTerritoryToResponse(Territory territory) {
        return TerritoryResponse.builder()
                .id(territory.getId())
                .name(territory.getName())
                .code(territory.getCode())
                .description(territory.getDescription())
                .squadId(territory.getSquad().getId())
                .squadName(territory.getSquad().getName())
                .unitId(territory.getSquad().getUnit().getId())
                .unitName(territory.getSquad().getUnit().getName())
                .country(territory.getSquad().getUnit().getCountry())
                .accessLevel(territory.getAccessLevel())
                .isActive(territory.getIsActive())
                .createdAt(territory.getCreatedAt())
                .updatedAt(territory.getUpdatedAt())
                .fotCount(territory.getFotAccessLevels().size())
                .build();
    }

    private TerritoryResponse mapTerritoryWithChildrenToResponse(Territory territory) {
        List<FotAccessLevelResponse> fots = territory.getFotAccessLevels().stream()
                .map(this::mapFotToResponse)
                .collect(Collectors.toList());
        return TerritoryResponse.builder()
                .id(territory.getId())
                .name(territory.getName())
                .code(territory.getCode())
                .description(territory.getDescription())
                .squadId(territory.getSquad().getId())
                .squadName(territory.getSquad().getName())
                .unitId(territory.getSquad().getUnit().getId())
                .unitName(territory.getSquad().getUnit().getName())
                .country(territory.getSquad().getUnit().getCountry())
                .accessLevel(territory.getAccessLevel())
                .isActive(territory.getIsActive())
                .createdAt(territory.getCreatedAt())
                .updatedAt(territory.getUpdatedAt())
                .fotAccessLevels(fots)
                .fotCount(fots.size())
                .build();
    }

    private FotAccessLevelResponse mapFotToResponse(FotAccessLevel fot) {
        Territory territory = fot.getTerritory();
        Squad squad = territory.getSquad();
        return FotAccessLevelResponse.builder()
                .id(fot.getId())
                .name(fot.getName())
                .code(fot.getCode())
                .description(fot.getDescription())
                .accessLevel(fot.getAccessLevel())
                .territoryId(territory.getId())
                .territoryName(territory.getName())
                .squadId(squad.getId())
                .squadName(squad.getName())
                .unitId(squad.getUnit().getId())
                .unitName(squad.getUnit().getName())
                .country(squad.getUnit().getCountry())
                .isActive(fot.getIsActive())
                .createdAt(fot.getCreatedAt())
                .updatedAt(fot.getUpdatedAt())
                .build();
    }

    private String generateCode(String name) {
        if (name == null || name.isBlank()) return "GEN";
        String[] words = name.trim().split("\\s+");
        StringBuilder code = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) code.append(Character.toUpperCase(w.charAt(0)));
        }
        return code.length() > 0 ? code.toString().toUpperCase() : name.substring(0, Math.min(3, name.length())).toUpperCase();
    }
}
