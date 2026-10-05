package com.world.i2v.i2vauthapi.service;

import com.world.i2v.i2vauthapi.dto.request.EmployeeUserRequest;
import com.world.i2v.i2vauthapi.dto.response.EmployeeUserResponse;
import com.world.i2v.i2vauthapi.entity.*;
import com.world.i2v.i2vauthapi.exception.BadRequestException;
import com.world.i2v.i2vauthapi.exception.ResourceNotFoundException;
import com.world.i2v.i2vauthapi.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.world.i2v.i2vauthapi.security.UserDetailsImpl;
import com.world.i2v.i2vauthapi.dto.request.ProfileUpdateRequest;
import com.world.i2v.i2vauthapi.dto.request.EmployeeJoinDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class EmployeeUserService {

    private final EmployeeUserRepository employeeRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final DesignationRepository designationRepository;
    private final UserSessionRepository sessionRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final ExitRequestRepository exitRequestRepository;
    private final UnitRepository unitRepository;
    private final SquadRepository squadRepository;
    private final TerritoryRepository territoryRepository;
    private final FotAccessLevelRepository fotAccessLevelRepository;
    private final AuditService auditService;
    private final EmailService emailService;
    private final PermissionService permissionService;
    private final ObjectMapper objectMapper;


    public EmployeeUserService(EmployeeUserRepository employeeRepository,
                               UserRepository userRepository,
                               DepartmentRepository departmentRepository,
                               DesignationRepository designationRepository,
                               UserSessionRepository sessionRepository,
                               AttendanceRepository attendanceRepository,
                               LeaveRequestRepository leaveRequestRepository,
                               ExitRequestRepository exitRequestRepository,
                               UnitRepository unitRepository,
                               SquadRepository squadRepository,
                               TerritoryRepository territoryRepository,
                               FotAccessLevelRepository fotAccessLevelRepository,
                               AuditService auditService,
                               EmailService emailService,
                               PermissionService permissionService) {
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.designationRepository = designationRepository;
        this.sessionRepository = sessionRepository;
        this.attendanceRepository = attendanceRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.exitRequestRepository = exitRequestRepository;
        this.unitRepository = unitRepository;
        this.squadRepository = squadRepository;
        this.territoryRepository = territoryRepository;
        this.fotAccessLevelRepository = fotAccessLevelRepository;
        this.auditService = auditService;
        this.emailService = emailService;
        this.permissionService = permissionService;
        this.objectMapper = new ObjectMapper();
    }

    public EmployeeUserResponse createEmployeeFromJoin(EmployeeJoinDto dto) {
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() != UserRole.EMPLOYEE) {
            user.setRole(UserRole.EMPLOYEE);
            userRepository.save(user);
        }

        if (employeeRepository.existsById(user.getId())) {
            throw new BadRequestException("Employee record already exists for this user");
        }

        Department department = departmentRepository.findById(dto.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));

        Designation designation = designationRepository.findById(dto.getDesignationId())
                .orElseThrow(() -> new ResourceNotFoundException("Designation not found"));

        EmployeeUser employee = new EmployeeUser();
        employee.setUser(user);
        employee.setEmployeeCode(dto.getEmployeeCode());
        employee.setDepartment(department);
        employee.setDesignation(designation);
        employee.setJoiningDate(dto.getJoiningDate());

        // serialize JsonNode to string for storage
        JsonNode kyc = dto.getKycFields();
        if (kyc != null) {
            try {
                String json = objectMapper.writeValueAsString(kyc);
                employee.setKycData(json);
            } catch (Exception e) {
                throw new BadRequestException("Invalid KYC JSON payload");
            }
        }

        employee.setKycTextUrl(dto.getKycTextUrl());
        // Map common document URLs from DTO into entity columns (if present)
        if (dto.getPanFrontUrl() != null) employee.setPanFrontUrl(dto.getPanFrontUrl());
        if (dto.getPanBackUrl() != null) employee.setPanBackUrl(dto.getPanBackUrl());
        if (dto.getAadhaarFrontUrl() != null) employee.setAadhaarFrontUrl(dto.getAadhaarFrontUrl());
        if (dto.getAadhaarBackUrl() != null) employee.setAadhaarBackUrl(dto.getAadhaarBackUrl());
        if (dto.getEmployeePhotoUrl() != null) employee.setEmployeePhotoUrl(dto.getEmployeePhotoUrl());
        if (dto.getSignatureUrl() != null) employee.setSignatureUrl(dto.getSignatureUrl());
        if (dto.getBankDocumentUrl() != null) employee.setBankDocumentUrl(dto.getBankDocumentUrl());

        // ==================== MAP PERSONAL FIELDS FROM JOIN DTO ====================
        if (dto.getEmploymentType() != null) {
            employee.setEmploymentType(EmployeeUser.EmploymentType.valueOf(dto.getEmploymentType()));
        }
        if (dto.getEmploymentStatus() != null) {
            employee.setEmploymentStatus(EmployeeUser.EmploymentStatus.valueOf(dto.getEmploymentStatus()));
        }
        employee.setConfirmationDate(dto.getConfirmationDate());
        employee.setResignationDate(dto.getResignationDate());

        // Contact info
        employee.setPhoneNumber(dto.getPhoneNumber());
        employee.setAlternatePhone(dto.getAlternatePhone());
        employee.setAddress(dto.getAddress());
        employee.setCity(dto.getCity());
        employee.setState(dto.getState());
        if (dto.getCountry() != null) employee.setCountry(dto.getCountry());
        employee.setPostalCode(dto.getPostalCode());
        employee.setDateOfBirth(dto.getDateOfBirth());
        if (dto.getGender() != null && !dto.getGender().isBlank()) {
            employee.setGender(EmployeeUser.Gender.valueOf(dto.getGender()));
        }
        employee.setBloodGroup(dto.getBloodGroup());

        // Emergency contact
        employee.setEmergencyContactName(dto.getEmergencyContactName());
        employee.setEmergencyContactPhone(dto.getEmergencyContactPhone());
        employee.setEmergencyContactRelation(dto.getEmergencyContactRelation());

        // Bank & ID
        employee.setBankAccountNumber(dto.getBankAccountNumber());
        employee.setBankName(dto.getBankName());
        employee.setBankIfscCode(dto.getBankIfscCode());
        employee.setPanNumber(dto.getPanNumber());
        employee.setAadharNumber(dto.getAadharNumber());

        // Work contact
        employee.setWorkEmail(dto.getWorkEmail());
        employee.setWorkPhone(dto.getWorkPhone());

        // Reporting manager
        if (dto.getReportingToId() != null) {
            EmployeeUser reportingTo = employeeRepository.findById(dto.getReportingToId())
                    .orElseThrow(() -> new ResourceNotFoundException("Reporting manager not found"));
            employee.setReportingTo(reportingTo);
        }

        // Hierarchy assignment
        if (dto.getUnitId() != null) { unitRepository.findById(dto.getUnitId()).ifPresent(employee::setUnit); }
        if (dto.getSquadId() != null) { squadRepository.findById(dto.getSquadId()).ifPresent(employee::setSquad); }
        if (dto.getTerritoryId() != null) { territoryRepository.findById(dto.getTerritoryId()).ifPresent(employee::setTerritory); }
        if (dto.getFotId() != null) { fotAccessLevelRepository.findById(dto.getFotId()).ifPresent(employee::setFotAccessLevel); }

        log.info("✅ [JOIN] Mapping personal fields: phone={}, dob={}, gender={}, bloodGroup={}, bank={}",
                dto.getPhoneNumber(), dto.getDateOfBirth(), dto.getGender(),
                dto.getBloodGroup(), dto.getBankAccountNumber());

        EmployeeUser saved = employeeRepository.save(employee);
        auditService.logAction(null, "CREATE", "EMPLOYEE_JOIN", saved.getId(), saved.getEmployeeCode());
        permissionService.clearCacheForUser(user.getId());

        return mapToResponse(saved);
    }

    public EmployeeUserResponse createEmployee(EmployeeUserRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() != UserRole.EMPLOYEE) {
            user.setRole(UserRole.EMPLOYEE);
            userRepository.save(user);
            log.info("User role updated to EMPLOYEE for user ID: {}", user.getId());
        }

        if (employeeRepository.existsById(user.getId())) {
            throw new BadRequestException("Employee record already exists for this user");
        }

        if (employeeRepository.existsByEmployeeCode(request.getEmployeeCode())) {
            throw new BadRequestException("Employee code already exists");
        }

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));

        Designation designation = designationRepository.findById(request.getDesignationId())
                .orElseThrow(() -> new ResourceNotFoundException("Designation not found"));

        EmployeeUser employee = new EmployeeUser();
        employee.setUser(user);
        employee.setEmployeeCode(request.getEmployeeCode());
        employee.setDepartment(department);
        employee.setDesignation(designation);
        employee.setJoiningDate(request.getJoiningDate());
        employee.setConfirmationDate(request.getConfirmationDate());
        employee.setResignationDate(request.getResignationDate());

        if (request.getEmploymentType() != null) {
            employee.setEmploymentType(EmployeeUser.EmploymentType.valueOf(request.getEmploymentType()));
        }
        if (request.getEmploymentStatus() != null) {
            employee.setEmploymentStatus(EmployeeUser.EmploymentStatus.valueOf(request.getEmploymentStatus()));
        }

        // 📞 Contact Information (from KYC)
        employee.setPhoneNumber(request.getPhoneNumber());
        employee.setAlternatePhone(request.getAlternatePhone());
        employee.setAddress(request.getAddress());
        employee.setCity(request.getCity());
        employee.setState(request.getState());
        employee.setCountry(request.getCountry());
        employee.setPostalCode(request.getPostalCode());
        employee.setDateOfBirth(request.getDateOfBirth());

        if (request.getGender() != null) {
            employee.setGender(EmployeeUser.Gender.valueOf(request.getGender()));
        }

        employee.setBloodGroup(request.getBloodGroup());
        
        // 🚨 Emergency Contact (from KYC)
        employee.setEmergencyContactName(request.getEmergencyContactName());
        employee.setEmergencyContactPhone(request.getEmergencyContactPhone());
        employee.setEmergencyContactRelation(request.getEmergencyContactRelation());
        
        // 🏦 Bank Information (from KYC)
        employee.setBankAccountNumber(request.getBankAccountNumber());
        employee.setBankName(request.getBankName());
        employee.setBankIfscCode(request.getBankIfscCode());
        
        // 📄 Identification Documents (from KYC)
        employee.setPanNumber(request.getPanNumber());
        employee.setAadharNumber(request.getAadharNumber());
        
        // 💼 Work Contact
        employee.setWorkEmail(request.getWorkEmail());
        employee.setWorkPhone(request.getWorkPhone());
        
        log.info("✅ [CREATE] Mapping KYC Data: phone={}, dob={}, address={}, pan={}, aadhaar={}", 
                 request.getPhoneNumber(), request.getDateOfBirth(), request.getAddress(),
                 request.getPanNumber(), request.getAadharNumber());

        if (request.getReportingToId() != null) {
            EmployeeUser reportingTo = employeeRepository.findById(request.getReportingToId())
                    .orElseThrow(() -> new ResourceNotFoundException("Reporting manager not found"));
            employee.setReportingTo(reportingTo);
        }

        // Hierarchy assignment
        if (request.getUnitId() != null) { unitRepository.findById(request.getUnitId()).ifPresent(employee::setUnit); }
        else { employee.setUnit(null); }
        if (request.getSquadId() != null) { squadRepository.findById(request.getSquadId()).ifPresent(employee::setSquad); }
        else { employee.setSquad(null); }
        if (request.getTerritoryId() != null) { territoryRepository.findById(request.getTerritoryId()).ifPresent(employee::setTerritory); }
        else { employee.setTerritory(null); }
        if (request.getFotId() != null) { fotAccessLevelRepository.findById(request.getFotId()).ifPresent(employee::setFotAccessLevel); }
        else { employee.setFotAccessLevel(null); }

        EmployeeUser saved = employeeRepository.save(employee);
        auditService.logAction(null, "CREATE", "EMPLOYEE", saved.getId(), saved.getEmployeeCode());
        permissionService.clearCacheForUser(user.getId());

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<EmployeeUserResponse> getAllEmployees() {
        log.info("════════════════════════════════════════");
        log.info("👨‍💼 START: getAllEmployees()");
        List<EmployeeUser> allEmployees = employeeRepository.findAllWithAssociations();
        log.info("📊 Total employees: {}", allEmployees.size());
        
        allEmployees.forEach(emp -> {
            Long uid = emp.getUser() != null ? emp.getUser().getId() : null;
            log.info("  Employee ID: {} | Code: {} | UserID: {} | Name: {}", 
                emp.getId(), emp.getEmployeeCode(), uid, 
                emp.getUser() != null ? emp.getUser().getFullName() : "NO USER");
        });
        
        List<EmployeeUserResponse> responses = allEmployees.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        
        log.info("👨‍💼 Returning {} employees", responses.size());
        log.info("════════════════════════════════════════");
        return responses;
    }

    @Transactional(readOnly = true)
    public EmployeeUserResponse getEmployeeById(Long id) {
        EmployeeUser employee = employeeRepository.findByIdWithUser(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));
        return mapToResponse(employee);
    }
//    public EmployeeUser getEmployeeById(Long id) {
//        return employeeRepository.findById(id)
//                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));
//    }

    @Transactional(readOnly = true)
    public EmployeeUserResponse getEmployeeByCode(String employeeCode) {
        EmployeeUser employee = employeeRepository.findByEmployeeCode(employeeCode)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "code", employeeCode));
        return mapToResponse(employee);
    }

    @Transactional(readOnly = true)
    public List<EmployeeUserResponse> getEmployeesByDepartment(Long departmentId) {
        return employeeRepository.findByDepartmentId(departmentId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EmployeeUserResponse> getEmployeesByDesignation(Long designationId) {
        return employeeRepository.findByDesignationId(designationId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    public EmployeeUserResponse updateEmployee(Long id, EmployeeUserRequest request) {
        EmployeeUser employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));

        // ==================== CAPTURE OLD VALUES ====================
        String oldEmploymentStatus = employee.getEmploymentStatus() != null
                ? employee.getEmploymentStatus().name()
                : "ACTIVE";

        String oldEmploymentType = employee.getEmploymentType() != null
                ? employee.getEmploymentType().name()
                : "PERMANENT";

        String oldDepartment = employee.getDepartment() != null
                ? employee.getDepartment().getName()
                : "None";

        String oldDesignation = employee.getDesignation() != null
                ? employee.getDesignation().getTitle()
                : "None";

        // ==================== ROLE CORRECTION ====================
        if (employee.getUser() != null && employee.getUser().getRole() != UserRole.EMPLOYEE) {
            employee.getUser().setRole(UserRole.EMPLOYEE);
            userRepository.save(employee.getUser());
            log.info("User role corrected to EMPLOYEE for user ID: {}", employee.getUser().getId());
        }

        // ==================== VALIDATION ====================
        if (!employee.getEmployeeCode().equals(request.getEmployeeCode()) &&
                employeeRepository.existsByEmployeeCode(request.getEmployeeCode())) {
            throw new BadRequestException("Employee code already exists");
        }

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));

        Designation designation = designationRepository.findById(request.getDesignationId())
                .orElseThrow(() -> new ResourceNotFoundException("Designation not found"));

        // ==================== UPDATE BASIC INFO ====================
        employee.setEmployeeCode(request.getEmployeeCode());
        employee.setDepartment(department);
        employee.setDesignation(designation);
        employee.setJoiningDate(request.getJoiningDate());
        employee.setConfirmationDate(request.getConfirmationDate());
        employee.setResignationDate(request.getResignationDate());

        // ==================== UPDATE EMPLOYMENT DETAILS ====================
        if (request.getEmploymentType() != null) {
            employee.setEmploymentType(EmployeeUser.EmploymentType.valueOf(request.getEmploymentType()));
        }
        if (request.getEmploymentStatus() != null) {
            employee.setEmploymentStatus(EmployeeUser.EmploymentStatus.valueOf(request.getEmploymentStatus()));
        }

        // ==================== UPDATE CONTACT INFO ====================
        employee.setPhoneNumber(request.getPhoneNumber());
        employee.setAlternatePhone(request.getAlternatePhone());
        employee.setAddress(request.getAddress());
        employee.setCity(request.getCity());
        employee.setState(request.getState());
        employee.setCountry(request.getCountry());
        employee.setPostalCode(request.getPostalCode());
        employee.setDateOfBirth(request.getDateOfBirth());

        if (request.getGender() != null) {
            employee.setGender(EmployeeUser.Gender.valueOf(request.getGender()));
        }

        employee.setBloodGroup(request.getBloodGroup());

        // ==================== UPDATE EMERGENCY CONTACT ====================
        employee.setEmergencyContactName(request.getEmergencyContactName());
        employee.setEmergencyContactPhone(request.getEmergencyContactPhone());
        employee.setEmergencyContactRelation(request.getEmergencyContactRelation());

        // ==================== UPDATE BANK & ID DETAILS ====================
        employee.setBankAccountNumber(request.getBankAccountNumber());
        employee.setBankName(request.getBankName());
        employee.setBankIfscCode(request.getBankIfscCode());
        employee.setPanNumber(request.getPanNumber());
        employee.setAadharNumber(request.getAadharNumber());

        // ==================== UPDATE WORK CONTACT ====================
        employee.setWorkEmail(request.getWorkEmail());
        employee.setWorkPhone(request.getWorkPhone());

        // ==================== UPDATE LOGIN EMAIL ====================
        if (request.getLoginEmail() != null && !request.getLoginEmail().isBlank() && employee.getUser() != null) {
            String newLoginEmail = request.getLoginEmail().trim().toLowerCase();
            if (!newLoginEmail.equals(employee.getUser().getEmail())) {
                if (userRepository.existsByEmail(newLoginEmail)) {
                    throw new BadRequestException("Email '" + newLoginEmail + "' is already in use by another account");
                }
                employee.getUser().setEmail(newLoginEmail);
                userRepository.save(employee.getUser());
                log.info("Login email updated for user ID: {}", employee.getUser().getId());
            }
        }

        log.info("✅ [UPDATE] Mapping KYC Data for Employee {}: phone={}, dob={}, address={}, pan={}, aadhaar={}", 
                 id, request.getPhoneNumber(), request.getDateOfBirth(), request.getAddress(),
                 request.getPanNumber(), request.getAadharNumber());

        // ==================== UPDATE REPORTING MANAGER ====================
        if (request.getReportingToId() != null) {
            EmployeeUser reportingTo = employeeRepository.findById(request.getReportingToId())
                    .orElseThrow(() -> new ResourceNotFoundException("Reporting manager not found"));
            employee.setReportingTo(reportingTo);
        } else {
            employee.setReportingTo(null);
        }

        // ==================== UPDATE HIERARCHY ASSIGNMENT ====================
        if (request.getUnitId() != null) { unitRepository.findById(request.getUnitId()).ifPresent(employee::setUnit); }
        else { employee.setUnit(null); }
        if (request.getSquadId() != null) { squadRepository.findById(request.getSquadId()).ifPresent(employee::setSquad); }
        else { employee.setSquad(null); }
        if (request.getTerritoryId() != null) { territoryRepository.findById(request.getTerritoryId()).ifPresent(employee::setTerritory); }
        else { employee.setTerritory(null); }
        if (request.getFotId() != null) { fotAccessLevelRepository.findById(request.getFotId()).ifPresent(employee::setFotAccessLevel); }
        else { employee.setFotAccessLevel(null); }

        // ==================== SAVE EMPLOYEE ====================
        EmployeeUser updated = employeeRepository.save(employee);

        // ==================== GET CURRENT USER INFO ====================
        String currentUserName = getCurrentUserName();
        String currentDateTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        // ==================== NEW VALUES AFTER UPDATE ====================
        String newEmploymentStatus = updated.getEmploymentStatus() != null
                ? updated.getEmploymentStatus().name()
                : "ACTIVE";

        String newEmploymentType = updated.getEmploymentType() != null
                ? updated.getEmploymentType().name()
                : "PERMANENT";

        String newDepartment = updated.getDepartment() != null
                ? updated.getDepartment().getName()
                : "None";

        String newDesignation = updated.getDesignation() != null
                ? updated.getDesignation().getTitle()
                : "None";

        // ==================== BUILD CHANGE SUMMARY ====================
        StringBuilder changeSummary = new StringBuilder();
        changeSummary.append("Changes made by ").append(currentUserName).append(":\n");

        if (!oldEmploymentStatus.equals(newEmploymentStatus)) {
            changeSummary.append("- Employment Status: ").append(oldEmploymentStatus)
                    .append(" → ").append(newEmploymentStatus).append("\n");
        }
        if (!oldEmploymentType.equals(newEmploymentType)) {
            changeSummary.append("- Employment Type: ").append(oldEmploymentType)
                    .append(" → ").append(newEmploymentType).append("\n");
        }
        if (!oldDepartment.equals(newDepartment)) {
            changeSummary.append("- Department: ").append(oldDepartment)
                    .append(" → ").append(newDepartment).append("\n");
        }
        if (!oldDesignation.equals(newDesignation)) {
            changeSummary.append("- Designation: ").append(oldDesignation)
                    .append(" → ").append(newDesignation).append("\n");
        }

        // ==================== SEND STATUS CHANGE EMAIL ====================
        if (!oldEmploymentStatus.equals(newEmploymentStatus)) {
            String statusChangeReason = String.format(
                    "Status changed from %s to %s by %s on %s",
                    oldEmploymentStatus, newEmploymentStatus, currentUserName, currentDateTime
            );

            emailService.sendEmployeeStatusChangeEmail(
                    updated.getUser(),
                    oldEmploymentStatus,
                    newEmploymentStatus,
                    statusChangeReason
            );

            log.info("Employment status change email sent to: {}", updated.getUser().getEmail());
        }

        // ==================== AUDIT LOGGING ====================
        auditService.logAction(
                null,
                "UPDATE_EMPLOYEE",
                "EMPLOYEE",
                updated.getId(),
                String.format("Employee %s updated by %s. %s",
                        updated.getEmployeeCode(),
                        currentUserName,
                        changeSummary.toString())
        );

        log.info("Employee updated: {} by {}", updated.getEmployeeCode(), currentUserName);
        permissionService.clearCacheForUser(updated.getUser() != null ? updated.getUser().getId() : updated.getId());

        // ==================== RETURN RESPONSE ====================
        return mapToResponse(updated);
    }

    public void deleteEmployee(Long id) {
        log.info("═══════════════════════════════════════════════════════════");
        log.info("🗑️ START: deleteEmployee({})", id);
        
        EmployeeUser employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));
        
        User user = employee.getUser();
        String employeeCode = employee.getEmployeeCode();
        Long userId = user != null ? user.getId() : null;
        
        log.info("📋 Employee found: {} | ID: {} | UserID: {}", employeeCode, id, userId);
        
        // Step 1: Delete all attendance records for this employee
        log.info("🗑️ Step 1: Deleting attendance records for employee {}...", id);
        List<Attendance> attendanceRecords = attendanceRepository.findByEmployeeId(id);
        log.info("   Found {} attendance records", attendanceRecords.size());
        if (!attendanceRecords.isEmpty()) {
            attendanceRepository.deleteAll(attendanceRecords);
            log.info("   ✅ Deleted {} attendance records", attendanceRecords.size());
        }
        
        // Step 2: Delete all leave request records for this employee
        log.info("🗑️ Step 2: Deleting leave request records for employee {}...", id);
        List<LeaveRequest> leaveRecords = leaveRequestRepository.findByEmployeeId(id);
        log.info("   Found {} leave request records", leaveRecords.size());
        if (!leaveRecords.isEmpty()) {
            leaveRequestRepository.deleteAll(leaveRecords);
            log.info("   ✅ Deleted {} leave request records", leaveRecords.size());
        }
        
        // Step 3: Delete all exit request records for this employee
        log.info("🗑️ Step 3: Deleting exit request records for employee {}...", id);
        List<ExitRequest> exitRecords = exitRequestRepository.findByEmployeeId(id);
        log.info("   Found {} exit request records", exitRecords.size());
        if (!exitRecords.isEmpty()) {
            exitRequestRepository.deleteAll(exitRecords);
            log.info("   ✅ Deleted {} exit request records", exitRecords.size());
        }
        
        // Step 4: Detach user from employee BEFORE deleting employee
        log.info("🗑️ Step 4: Detaching User from EmployeeUser...");
        if (user != null) {
            user.setEmployee(null);
            userRepository.save(user);
            log.info("   ✅ User detached");
        }
        
        // Step 5: Delete employee record
        log.info("🗑️ Step 5: Deleting EmployeeUser from database...");
        employeeRepository.delete(employee);
        log.info("   ✅ EmployeeUser deleted");
        
        // Step 6: Verify user still exists
        if (userId != null) {
            log.info("🔍 Step 6: Verifying User (ID: {}) still exists...", userId);
            User userAfterDelete = userRepository.findById(userId).orElse(null);
            
            if (userAfterDelete != null) {
                log.info("✅✅✅ SUCCESS! User preserved: {} | ID: {} | Email: {}", 
                    userAfterDelete.getFullName(), userAfterDelete.getId(), userAfterDelete.getEmail());
            } else {
                log.error("❌❌❌ FAILED! User was deleted: ID {}", userId);
            }
        }
        
        auditService.logAction(null, "DELETE", "EMPLOYEE", id, employeeCode);
        permissionService.clearCacheForUser(userId != null ? userId : id);
        log.info("🗑️ END: deleteEmployee({}) complete", id);
        log.info("═══════════════════════════════════════════════════════════");
    }

    /**
     * Deactivate user account (User.active = false)
     * This prevents login but keeps employment status unchanged
     */
    public void deactivateUserAccount(Long employeeId, String reason) {
        EmployeeUser employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));

        User user = employee.getUser();
        if (user == null) {
            throw new BadRequestException("No user associated with this employee");
        }

        if (!user.isActive()) {
            throw new BadRequestException("User account is already deactivated");
        }

        // Deactivate user account
        user.setActive(false);
        userRepository.save(user);

        // Terminate all active sessions
        sessionRepository.deactivateUserSessions(user.getId());

        // Log the action
        auditService.logAction(null, "DEACTIVATE_USER", "USER", user.getId(),
                String.format("User account deactivated. Reason: %s", reason));

        // Send email notification
        emailService.sendAccountDeactivationEmail(user, reason);

        log.info("User account deactivated for employee: {} - Reason: {}", employee.getEmployeeCode(), reason);
    }

    /**
     * Reactivate user account (User.active = true)
     * This allows login again
     */
    public void reactivateUserAccount(Long employeeId, String reason) {
        EmployeeUser employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));

        User user = employee.getUser();
        if (user == null) {
            throw new BadRequestException("No user associated with this employee");
        }

        if (user.isActive()) {
            throw new BadRequestException("User account is already active");
        }

        // Reactivate user account
        user.setActive(true);
        userRepository.save(user);

        // Log the action
        auditService.logAction(null, "REACTIVATE_USER", "USER", user.getId(),
                String.format("User account reactivated. Reason: %s", reason));

        // Send email notification
        emailService.sendAccountReactivationEmail(user, reason);

        log.info("User account reactivated for employee: {} - Reason: {}", employee.getEmployeeCode(), reason);
    }

    /**
     * Deactivate user account by user ID (for users without employee record)
     */
    public void deactivateUserByUserId(Long userId, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (!user.isActive()) {
            throw new BadRequestException("User account is already deactivated");
        }

        // Deactivate user account
        user.setActive(false);
        userRepository.save(user);

        // Terminate all active sessions
        sessionRepository.deactivateUserSessions(userId);

        // Log the action
        auditService.logAction(null, "DEACTIVATE_USER", "USER", userId,
                String.format("User account deactivated by ID. Reason: %s", reason));

        // Send email notification
        emailService.sendAccountDeactivationEmail(user, reason);

        log.info("🔴 User account deactivated by user ID: {} - Reason: {}", userId, reason);
    }

    /**
     * Reactivate user account by user ID (for users without employee record)
     */
    public void reactivateUserByUserId(Long userId, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (user.isActive()) {
            throw new BadRequestException("User account is already active");
        }

        // Reactivate user account
        user.setActive(true);
        userRepository.save(user);

        // Log the action
        auditService.logAction(null, "REACTIVATE_USER", "USER", userId,
                String.format("User account reactivated by ID. Reason: %s", reason));

        // Send email notification
        emailService.sendAccountReactivationEmail(user, reason);

        log.info("🟢 User account reactivated by user ID: {} - Reason: {}", userId, reason);
    }

    /**
     * Check if user account is active
     */
    public boolean isUserActive(Long employeeId) {
        EmployeeUser employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));

        User user = employee.getUser();
        return user != null && user.isActive();
    }



    @Transactional(readOnly = true)
    public EmployeeUserResponse getEmployeeByUserId(Long userId) {
        return employeeRepository.findByUserIdWithUser(userId)
                .map(this::mapToResponse)
                .orElse(null);
    }

    @Transactional
    public EmployeeUserResponse updateLeaveEntitlements(Long employeeId, Map<String, Integer> entitlements) {
        EmployeeUser employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        if (entitlements.containsKey("annualLeavesEntitled")) {
            employee.setAnnualLeavesEntitled(entitlements.get("annualLeavesEntitled"));
        }
        if (entitlements.containsKey("sickLeavesEntitled")) {
            employee.setSickLeavesEntitled(entitlements.get("sickLeavesEntitled"));
        }
        if (entitlements.containsKey("casualLeavesEntitled")) {
            employee.setCasualLeavesEntitled(entitlements.get("casualLeavesEntitled"));
        }

        EmployeeUser saved = employeeRepository.save(employee);
        auditService.logAction(null, "UPDATE_LEAVE_ENTITLEMENTS", "EMPLOYEE", saved.getId(),
                "Updated leave entitlements for " + saved.getEmployeeCode());

        return mapToResponse(saved);
    }

    @Transactional
    public Map<String, Object> bulkUpdateLeaveEntitlements(Long departmentId, String leaveType, Integer entitledDays) {
        if (leaveType == null || leaveType.isBlank() || entitledDays == null) {
            throw new BadRequestException("leaveType and entitledDays are required");
        }

        List<EmployeeUser> employees = employeeRepository.findByDepartmentId(departmentId);
        for (EmployeeUser emp : employees) {
            switch (leaveType.toUpperCase()) {
                case "ANNUAL" -> emp.setAnnualLeavesEntitled(entitledDays);
                case "SICK" -> emp.setSickLeavesEntitled(entitledDays);
                case "CASUAL" -> emp.setCasualLeavesEntitled(entitledDays);
                default -> throw new BadRequestException("Invalid leave type: " + leaveType);
            }
            employeeRepository.save(emp);
        }

        auditService.logAction(null, "BULK_UPDATE_LEAVE_ENTITLEMENTS", "EMPLOYEE", departmentId,
                String.format("Bulk updated %s leave to %d days for %d employees",
                        leaveType, entitledDays, employees.size()));

        Map<String, Object> result = new HashMap<>();
        result.put("updatedCount", employees.size());
        result.put("departmentId", departmentId);
        result.put("leaveType", leaveType);
        result.put("entitledDays", entitledDays);
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getLeaveBalance(Long employeeId, Integer year) {
        EmployeeUser employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        int currentYear = year != null ? year : LocalDate.now().getYear();

        long annualUsed = leaveRequestRepository.countApprovedLeavesByType(
                employee.getId(), LeaveRequest.LeaveType.ANNUAL, currentYear);
        long sickUsed = leaveRequestRepository.countApprovedLeavesByType(
                employee.getId(), LeaveRequest.LeaveType.SICK, currentYear);
        long casualUsed = leaveRequestRepository.countApprovedLeavesByType(
                employee.getId(), LeaveRequest.LeaveType.CASUAL, currentYear);

        Map<String, Object> balance = new HashMap<>();
        balance.put("annual", buildLeaveBalance(annualUsed, employee.getAnnualLeavesEntitled()));
        balance.put("sick", buildLeaveBalance(sickUsed, employee.getSickLeavesEntitled()));
        balance.put("casual", buildLeaveBalance(casualUsed, employee.getCasualLeavesEntitled()));
        return balance;
    }

    private Map<String, Object> buildLeaveBalance(long used, Integer entitled) {
        int total = entitled != null ? entitled : 0;
        return Map.of(
                "used", used,
                "total", total,
                "remaining", total - used
        );
    }

    private EmployeeUserResponse mapToResponse(EmployeeUser employee) {
        EmployeeUserResponse.EmployeeUserResponseBuilder builder = EmployeeUserResponse.builder()
                .id(employee.getId())
                .employeeCode(employee.getEmployeeCode())
                .joiningDate(employee.getJoiningDate())
                .confirmationDate(employee.getConfirmationDate())
                .resignationDate(employee.getResignationDate())
                .employmentType(employee.getEmploymentType() != null ? employee.getEmploymentType().name() : null)
                .employmentStatus(employee.getEmploymentStatus() != null ? employee.getEmploymentStatus().name() : null)
                
                // 📞 Contact Information (from KYC)
                .phoneNumber(employee.getPhoneNumber())
                .alternatePhone(employee.getAlternatePhone())
                .address(employee.getAddress())
                .city(employee.getCity())
                .state(employee.getState())
                .country(employee.getCountry())
                .postalCode(employee.getPostalCode())
                .dateOfBirth(employee.getDateOfBirth())
                .gender(employee.getGender() != null ? employee.getGender().name() : null)
                .bloodGroup(employee.getBloodGroup())
                
                // 🚨 Emergency Contact (from KYC)
                .emergencyContactName(employee.getEmergencyContactName())
                .emergencyContactPhone(employee.getEmergencyContactPhone())
                .emergencyContactRelation(employee.getEmergencyContactRelation())
                
                // 🏦 Bank Information (from KYC)
                .bankAccountNumber(employee.getBankAccountNumber())
                .bankName(employee.getBankName())
                .bankIfscCode(employee.getBankIfscCode())
                
                // 📄 Identification Documents (from KYC)
                .panNumber(employee.getPanNumber())
                .panFrontUrl(employee.getPanFrontUrl())
                .panBackUrl(employee.getPanBackUrl())
                .aadhaarFrontUrl(employee.getAadhaarFrontUrl())
                .aadhaarBackUrl(employee.getAadhaarBackUrl())
                .employeePhotoUrl(employee.getEmployeePhotoUrl())
                .signatureUrl(employee.getSignatureUrl())
                .bankDocumentUrl(employee.getBankDocumentUrl())
                .aadharNumber(employee.getAadharNumber())
                
                // 💼 Work Contact
                .workEmail(employee.getWorkEmail())
                .workPhone(employee.getWorkPhone())
                .createdAt(employee.getCreatedAt())
                .updatedAt(employee.getUpdatedAt())
                .annualLeavesEntitled(employee.getAnnualLeavesEntitled())
                .sickLeavesEntitled(employee.getSickLeavesEntitled())
                .casualLeavesEntitled(employee.getCasualLeavesEntitled());

        if (employee.getUser() != null) {
            builder.userId(employee.getUser().getId())
                    .userName(employee.getUser().getFullName())
                    .userEmail(employee.getUser().getEmail())
                    .userActive(employee.getUser().isActive());
        }

        if (employee.getDepartment() != null) {
            builder.departmentId(employee.getDepartment().getId())
                    .departmentName(employee.getDepartment().getName());
        }

        if (employee.getDesignation() != null) {
            builder.designationId(employee.getDesignation().getId())
                    .designationTitle(employee.getDesignation().getTitle())
                    .designationLevel(employee.getDesignation().getLevel())
                    .isManagerial(employee.getDesignation().getIsManagerial());
        }

        if (employee.getReportingTo() != null && employee.getReportingTo().getUser() != null) {
            builder.reportingToId(employee.getReportingTo().getId())
                    .reportingToName(employee.getReportingTo().getUser().getFullName());
        }

        if (employee.getUnit() != null) {
            builder.unitId(employee.getUnit().getId())
                   .unitName(employee.getUnit().getName());
        }
        if (employee.getSquad() != null) {
            builder.squadId(employee.getSquad().getId())
                   .squadName(employee.getSquad().getName());
        }
        if (employee.getTerritory() != null) {
            builder.territoryId(employee.getTerritory().getId())
                   .territoryName(employee.getTerritory().getName());
        }
        if (employee.getFotAccessLevel() != null) {
            builder.fotId(employee.getFotAccessLevel().getId())
                   .fotName(employee.getFotAccessLevel().getName());
        }

        // include raw KYC JSON and text url so frontend can prefill edit forms
        builder.kycData(employee.getKycData())
               .kycTextUrl(employee.getKycTextUrl());

        return builder.build();
    }
    private String getCurrentUserName() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserDetailsImpl) {
                UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
                return userDetails.getFullName(); // or userDetails.getEmail()
            }
        } catch (Exception e) {
            log.warn("Could not get current user: {}", e.getMessage());
        }
        return "System";
    }
    /**
     * Update only the personal profile fields (non‑sensitive).
     * Used by employees themselves.
     */
    public EmployeeUserResponse updateProfile(Long id, ProfileUpdateRequest request) {
        EmployeeUser employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        // Apply only non‑null values
        if (request.getPhoneNumber() != null) employee.setPhoneNumber(request.getPhoneNumber());
        if (request.getAlternatePhone() != null) employee.setAlternatePhone(request.getAlternatePhone());
        if (request.getWorkEmail() != null) employee.setWorkEmail(request.getWorkEmail());
        if (request.getAddress() != null) employee.setAddress(request.getAddress());
        if (request.getCity() != null) employee.setCity(request.getCity());
        if (request.getState() != null) employee.setState(request.getState());
        if (request.getCountry() != null) employee.setCountry(request.getCountry());
        if (request.getPostalCode() != null) employee.setPostalCode(request.getPostalCode());
        if (request.getEmergencyContactName() != null) employee.setEmergencyContactName(request.getEmergencyContactName());
        if (request.getEmergencyContactPhone() != null) employee.setEmergencyContactPhone(request.getEmergencyContactPhone());
        if (request.getEmergencyContactRelation() != null) employee.setEmergencyContactRelation(request.getEmergencyContactRelation());

        EmployeeUser saved = employeeRepository.save(employee);
        auditService.logAction(null, "UPDATE_PROFILE", "EMPLOYEE", saved.getId(),
                "Profile updated by employee");
        return mapToResponse(saved);
    }

}
