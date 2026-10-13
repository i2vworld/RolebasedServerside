package com.world.i2v.i2vauthapi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeUserResponse {

    private Long id;
    private String employeeCode;
    private Long userId;
    private String userName;
    private String userEmail;
    private String generatedPassword;
    private Boolean userActive;  // ✅ Add this field
    private Long departmentId;
    private String departmentName;
    private Long designationId;
    private String designationTitle;
    private Integer designationLevel;
    private Boolean isManagerial;
    private Long reportingToId;
    private String reportingToName;

    private Long unitId;
    private String unitName;
    private Long squadId;
    private String squadName;
    private Long territoryId;
    private String territoryName;
    private Long fotId;
    private String fotName;
    private LocalDate joiningDate;
    private LocalDate confirmationDate;
    private LocalDate resignationDate;
    private String employmentType;
    private String employmentStatus;
    private String phoneNumber;
    private String alternatePhone;
    private String address;
    private String city;
    private String state;
    private String country;
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
    // KYC document URLs
    private String panFrontUrl;
    private String panBackUrl;
    private String aadhaarFrontUrl;
    private String aadhaarBackUrl;
    private String employeePhotoUrl;
    private String signatureUrl;
    private String bankDocumentUrl;
    private String workEmail;
    private String workPhone;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer annualLeavesEntitled;
    private Integer sickLeavesEntitled;
    private Integer casualLeavesEntitled;
    // Raw KYC JSON stored as string
    private String kycData;
    private String kycTextUrl;
}
