package com.world.i2v.i2vauthapi.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class EmployeeUserRequest {

    private Long userId;

    @NotBlank(message = "Employee code is required")
    private String employeeCode;

    @NotNull(message = "Department ID is required")
    private Long departmentId;

    @NotNull(message = "Designation ID is required")
    private Long designationId;

    private Long reportingToId;

    private Long unitId;
    private Long squadId;
    private Long territoryId;
    private Long fotId;

    @NotNull(message = "Joining date is required")
    private LocalDate joiningDate;

    private LocalDate confirmationDate;
    
    private LocalDate resignationDate;

    private String employmentType = "PERMANENT";

    private String employmentStatus = "ACTIVE";

    private String phoneNumber;

    private String alternatePhone;

    private String address;

    private String city;

    private String state;

    private String country = "India";

    private String postalCode;

    private LocalDate dateOfBirth;

    private String gender;

    private String bloodGroup;

    private String emergencyContactName;

    private String emergencyContactPhone;

    private String emergencyContactRelation;

    private String bankAccountNumber;

    private String bankName;

    private String bankIfscCode;

    private String panNumber;

    private String aadharNumber;

    private String workEmail;

    private String workPhone;

    private String loginEmail;
}