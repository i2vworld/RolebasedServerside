package com.world.i2v.i2vauthapi.service;

import com.world.i2v.i2vauthapi.dto.request.DesignationRequest;
import com.world.i2v.i2vauthapi.dto.response.DepartmentResponse;
import com.world.i2v.i2vauthapi.dto.response.DesignationResponse;
import com.world.i2v.i2vauthapi.entity.Department;
import com.world.i2v.i2vauthapi.entity.Designation;
import com.world.i2v.i2vauthapi.exception.BadRequestException;
import com.world.i2v.i2vauthapi.exception.ResourceNotFoundException;
import com.world.i2v.i2vauthapi.repository.DepartmentRepository;
import com.world.i2v.i2vauthapi.repository.DesignationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class DesignationService {

    private final DesignationRepository designationRepository;
    private final DepartmentRepository departmentRepository;
    private final AuditService auditService;

    public DesignationService(DesignationRepository designationRepository,
                              DepartmentRepository departmentRepository,
                              AuditService auditService) {
        this.designationRepository = designationRepository;
        this.departmentRepository = departmentRepository;
        this.auditService = auditService;
    }

    public DesignationResponse createDesignation(DesignationRequest request) {
        if (designationRepository.existsByTitle(request.getTitle())) {
            throw new BadRequestException("Designation with this title already exists");
        }

        if (request.getCode() != null && designationRepository.existsByCode(request.getCode())) {
            throw new BadRequestException("Designation with this code already exists");
        }

        Designation designation = new Designation();
        designation.setTitle(request.getTitle());
        designation.setCode(request.getCode() != null ? request.getCode() : generateCode(request.getTitle()));
        designation.setDescription(request.getDescription());
        designation.setLevel(request.getLevel() != null ? request.getLevel() : 1);
        designation.setMinSalary(request.getMinSalary());
        designation.setMaxSalary(request.getMaxSalary());
        designation.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        designation.setIsManagerial(request.getIsManagerial() != null ? request.getIsManagerial() : false);

        if (request.getDepartmentId() != null) {
            Department department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
            designation.setDepartment(department);
        }

        Designation saved = designationRepository.save(designation);
        auditService.logAction(null, "CREATE", "DESIGNATION", saved.getId(), saved.getTitle());

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<DesignationResponse> getAllDesignations() {
        List<Designation> designations = designationRepository.findAll();
        return designations.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DesignationResponse getDesignationById(Long id) {
        Designation designation = designationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Designation", "id", id));
        return mapToResponse(designation);
    }

    @Transactional(readOnly = true)
    public List<DesignationResponse> getDesignationsByDepartment(Long departmentId) {
        List<Designation> designations = designationRepository.findByDepartmentId(departmentId);
        return designations.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public DesignationResponse updateDesignation(Long id, DesignationRequest request) {
        Designation designation = designationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Designation", "id", id));

        if (!designation.getTitle().equals(request.getTitle()) &&
                designationRepository.existsByTitle(request.getTitle())) {
            throw new BadRequestException("Designation with this title already exists");
        }

        designation.setTitle(request.getTitle());
        if (request.getCode() != null) {
            designation.setCode(request.getCode());
        }
        designation.setDescription(request.getDescription());
        designation.setLevel(request.getLevel());
        designation.setMinSalary(request.getMinSalary());
        designation.setMaxSalary(request.getMaxSalary());
        designation.setIsActive(request.getIsActive());
        designation.setIsManagerial(request.getIsManagerial());

        if (request.getDepartmentId() != null) {
            Department department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
            designation.setDepartment(department);
        } else {
            designation.setDepartment(null);
        }

        Designation updated = designationRepository.save(designation);
        auditService.logAction(null, "UPDATE", "DESIGNATION", updated.getId(), updated.getTitle());

        return mapToResponse(updated);
    }

    public void deleteDesignation(Long id) {
        Designation designation = designationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Designation", "id", id));

        designation.setIsActive(false);
        designationRepository.save(designation);
        auditService.logAction(null, "DELETE", "DESIGNATION", id, designation.getTitle());
    }

    private String generateCode(String title) {
        return title.toUpperCase()
                .replaceAll("[^A-Z0-9]", "_")
                .replaceAll("_+", "_");
    }

    private DesignationResponse mapToResponse(Designation designation) {
        if (designation == null) return null;

        DesignationResponse.DesignationResponseBuilder builder = DesignationResponse.builder()
                .id(designation.getId())
                .title(designation.getTitle())
                .code(designation.getCode())
                .description(designation.getDescription())
                .level(designation.getLevel())
                .minSalary(designation.getMinSalary())
                .maxSalary(designation.getMaxSalary())
                .isActive(designation.getIsActive())
                .isManagerial(designation.getIsManagerial())
                .createdAt(designation.getCreatedAt())
                .updatedAt(designation.getUpdatedAt())
                .employeeCount(designation.getEmployees() != null ? (long) designation.getEmployees().size() : 0L);

        if (designation.getDepartment() != null) {
            builder.department(DepartmentResponse.builder()
                    .id(designation.getDepartment().getId())
                    .name(designation.getDepartment().getName())
                    .code(designation.getDepartment().getCode())
                    .build());
        }

        return builder.build();
    }
}