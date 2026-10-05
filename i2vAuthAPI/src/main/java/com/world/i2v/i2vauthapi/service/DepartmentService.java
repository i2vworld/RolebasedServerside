package com.world.i2v.i2vauthapi.service;

import com.world.i2v.i2vauthapi.dto.request.DepartmentRequest;
import com.world.i2v.i2vauthapi.dto.response.DepartmentResponse;
import com.world.i2v.i2vauthapi.dto.response.UserResponse;
import com.world.i2v.i2vauthapi.entity.Department;
import com.world.i2v.i2vauthapi.entity.Unit;
import com.world.i2v.i2vauthapi.entity.User;
import com.world.i2v.i2vauthapi.exception.BadRequestException;
import com.world.i2v.i2vauthapi.exception.ResourceNotFoundException;
import com.world.i2v.i2vauthapi.repository.DepartmentRepository;
import com.world.i2v.i2vauthapi.repository.UnitRepository;
import com.world.i2v.i2vauthapi.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final UnitRepository unitRepository;
    private final AuditService auditService;

    public DepartmentService(DepartmentRepository departmentRepository,
                             UserRepository userRepository,
                             UnitRepository unitRepository,
                             AuditService auditService) {
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.unitRepository = unitRepository;
        this.auditService = auditService;
    }

    public DepartmentResponse createDepartment(DepartmentRequest request) {
        if (departmentRepository.existsByName(request.getName())) {
            throw new BadRequestException("Department with this name already exists");
        }

        if (request.getCode() != null && departmentRepository.existsByCode(request.getCode())) {
            throw new BadRequestException("Department with this code already exists");
        }

        Department department = new Department();
        department.setName(request.getName());
        department.setCode(request.getCode() != null ? request.getCode() : generateCode(request.getName()));
        department.setDescription(request.getDescription());
        department.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        department.setBudget(request.getBudget());
        department.setLocation(request.getLocation());
        department.setEmail(request.getEmail());
        department.setPhone(request.getPhone());

        if (request.getParentDepartmentId() != null) {
            Department parent = departmentRepository.findById(request.getParentDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent department not found"));
            department.setParentDepartment(parent);
        }

        if (request.getUnitId() != null) {
            Unit unit = unitRepository.findById(request.getUnitId())
                    .orElseThrow(() -> new ResourceNotFoundException("Unit not found"));
            department.setUnit(unit);
        }

        if (request.getIsHierarchyUnit() != null) {
            department.setIsHierarchyUnit(request.getIsHierarchyUnit());
        }

        if (request.getManagerId() != null) {
            User manager = userRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager not found"));
            department.setManager(manager);
        }

        Department saved = departmentRepository.save(department);
        auditService.logAction(null, "CREATE", "DEPARTMENT", saved.getId(), saved.getName());

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> getAllDepartments() {
        List<Department> departments = departmentRepository.findAll();
        return departments.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DepartmentResponse getDepartmentById(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));
        return mapToResponse(department);
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> getActiveDepartments() {
        List<Department> departments = departmentRepository.findByIsActive(true);
        return departments.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public DepartmentResponse updateDepartment(Long id, DepartmentRequest request) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));

        if (!department.getName().equals(request.getName()) &&
                departmentRepository.existsByName(request.getName())) {
            throw new BadRequestException("Department with this name already exists");
        }

        department.setName(request.getName());
        if (request.getCode() != null) {
            if (!request.getCode().equals(department.getCode()) &&
                    departmentRepository.existsByCode(request.getCode())) {
                throw new BadRequestException("Department with this code already exists");
            }
            department.setCode(request.getCode());
        }
        department.setDescription(request.getDescription());
        department.setIsActive(request.getIsActive());
        department.setBudget(request.getBudget());
        department.setLocation(request.getLocation());
        department.setEmail(request.getEmail());
        department.setPhone(request.getPhone());

        if (request.getParentDepartmentId() != null) {
            if (request.getParentDepartmentId().equals(id)) {
                throw new BadRequestException("Department cannot be its own parent");
            }
            Department parent = departmentRepository.findById(request.getParentDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent department not found"));
            department.setParentDepartment(parent);
        } else {
            department.setParentDepartment(null);
        }

        if (request.getUnitId() != null) {
            Unit unit = unitRepository.findById(request.getUnitId())
                    .orElseThrow(() -> new ResourceNotFoundException("Unit not found"));
            department.setUnit(unit);
        } else {
            department.setUnit(null);
        }

        department.setIsHierarchyUnit(request.getIsHierarchyUnit() != null ? request.getIsHierarchyUnit() : false);

        if (request.getManagerId() != null) {
            User manager = userRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager not found"));
            department.setManager(manager);
        } else {
            department.setManager(null);
        }

        Department updated = departmentRepository.save(department);
        auditService.logAction(null, "UPDATE", "DEPARTMENT", updated.getId(), updated.getName());

        return mapToResponse(updated);
    }

    public void deleteDepartment(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));

        department.setIsActive(false);
        departmentRepository.save(department);
        auditService.logAction(null, "DELETE", "DEPARTMENT", id, department.getName());
    }

    private String generateCode(String name) {
        return name.toUpperCase()
                .replaceAll("[^A-Z0-9]", "_")
                .replaceAll("_+", "_");
    }

    private DepartmentResponse mapToResponse(Department department) {
        if (department == null) return null;

        DepartmentResponse.DepartmentResponseBuilder builder = DepartmentResponse.builder()
                .id(department.getId())
                .name(department.getName())
                .code(department.getCode())
                .description(department.getDescription())
                .isActive(department.getIsActive())
                .budget(department.getBudget())
                .location(department.getLocation())
                .email(department.getEmail())
                .phone(department.getPhone())
                .createdAt(department.getCreatedAt())
                .updatedAt(department.getUpdatedAt())
                .employeeCount(department.getEmployees() != null ? (long) department.getEmployees().size() : 0L);

        if (department.getUnit() != null) {
            builder.unitId(department.getUnit().getId())
                   .unitName(department.getUnit().getName())
                   .unitCode(department.getUnit().getCode());
        }

        builder.isHierarchyUnit(department.getIsHierarchyUnit() != null ? department.getIsHierarchyUnit() : false);

        if (department.getParentDepartment() != null) {
            builder.parentDepartment(DepartmentResponse.builder()
                    .id(department.getParentDepartment().getId())
                    .name(department.getParentDepartment().getName())
                    .code(department.getParentDepartment().getCode())
                    .build());
        }

        if (department.getManager() != null) {
            builder.manager(UserResponse.builder()
                    .id(department.getManager().getId())
                    .fullName(department.getManager().getFullName())
                    .email(department.getManager().getEmail())
                    .build());
        }

        return builder.build();
    }
}