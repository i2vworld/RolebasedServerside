package com.world.i2v.i2vauthapi.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class EmployeeJoinDto {
    private Long userId;
    private String employeeCode;
    private Long departmentId;
    private Long designationId;

    // Accept ISO date strings as LocalDate
    private LocalDate joiningDate;

    // Basic contact / identity fields
    private String phoneNumber;
    private String alternatePhone;
    private String address;
    private String city;
    private String state;
    private String country;
    private String postalCode;
    private String employmentType;
    private String employmentStatus;
    private LocalDate confirmationDate;
    private LocalDate resignationDate;
    private LocalDate dateOfBirth;
    private String gender;
    private String bloodGroup;
    private String panNumber;
    private String aadharNumber;

    // Emergency contact
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelation;

    // Bank information
    private String bankAccountNumber;
    private String bankName;
    private String bankIfscCode;

    // Work contact
    private String workEmail;
    private String workPhone;

    // Reporting manager
    private Long reportingToId;

    // Hierarchy assignment
    private Long unitId;
    private Long squadId;
    private Long territoryId;
    private Long fotId;

    // Flexible KYC JSON (mapped from input key 'kyc')
    @JsonProperty("kyc")
    private JsonNode kycFields;

    // Optional KYC completion / documents
    @JsonProperty("kycCompletion")
    private JsonNode kycCompletion;

    @JsonProperty("kycDocuments")
    private List<String> kycDocuments;

    // Common doc URL fields
    private String panFrontUrl;
    private String panBackUrl;
    private String aadhaarFrontUrl;
    private String aadhaarBackUrl;
    private String employeePhotoUrl;
    private String signatureUrl;
    private String bankDocumentUrl;

    // Raw KYC payload (optional)
    @JsonProperty("kycRaw")
    private JsonNode kycRaw;

    // Helper: optional text/url field used by existing service
    private String kycTextUrl;
}
