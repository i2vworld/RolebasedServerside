package com.world.i2v.i2vauthapi.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "employee_users")
@Data
@EqualsAndHashCode(exclude = {"user", "reportingTo", "subordinates", "createdBy", "updatedBy"})
@ToString(exclude = {"user", "reportingTo", "subordinates", "createdBy", "updatedBy"})
public class EmployeeUser {

    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @MapsId
    @JoinColumn(name = "id")
    private User user;

    @Column(name = "employee_code", nullable = false, unique = true, length = 50)
    private String employeeCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "designation_id", nullable = false)
    private Designation designation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporting_to")
    private EmployeeUser reportingTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id")
    private Unit unit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "squad_id")
    private Squad squad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "territory_id")
    private Territory territory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fot_access_level_id")
    private FotAccessLevel fotAccessLevel;

    @OneToMany(mappedBy = "reportingTo")
    private Set<EmployeeUser> subordinates = new HashSet<>();

    @Column(name = "joining_date", nullable = false)
    private LocalDate joiningDate;

    @Column(name = "confirmation_date")
    private LocalDate confirmationDate;

    @Column(name = "resignation_date")
    private LocalDate resignationDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type")
    private EmploymentType employmentType = EmploymentType.PERMANENT;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_status")
    private EmploymentStatus employmentStatus = EmploymentStatus.ACTIVE;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "alternate_phone", length = 20)
    private String alternatePhone;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String address;

    @Column(length = 50)
    private String city;

    @Column(length = 50)
    private String state;

    @Column(length = 50)
    private String country = "India";

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(name = "blood_group", length = 5)
    private String bloodGroup;

    @Column(name = "emergency_contact_name", length = 100)
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone", length = 20)
    private String emergencyContactPhone;

    @Column(name = "emergency_contact_relation", length = 50)
    private String emergencyContactRelation;

    @Column(name = "bank_account_number", length = 50)
    private String bankAccountNumber;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "bank_ifsc_code", length = 20)
    private String bankIfscCode;

    @Column(name = "pan_number", length = 10)
    private String panNumber;

    @Column(name = "aadhar_number", length = 20)
    private String aadharNumber;

    @Column(name = "pf_number", length = 50)
    private String pfNumber;

    @Column(name = "esi_number", length = 50)
    private String esiNumber;

    @Column(name = "uan_number", length = 20)
    private String uanNumber;

    @Column(name = "work_email", length = 100)
    private String workEmail;

    @Column(name = "work_phone", length = 20)
    private String workPhone;

    @Column(length = 10)
    private String extension;

    @Column(name = "seating_location", length = 100)
    private String seatingLocation;

    @Column(name = "shift_timing", length = 50)
    private String shiftTiming;

    @Column(name = "weekly_off", length = 20)
    private String weeklyOff = "SUNDAY";

    @Column(name = "probation_period_months")
    private Integer probationPeriodMonths = 6;

    @Column(name = "notice_period_days")
    private Integer noticePeriodDays = 30;

    // 🔥 LEAVE ENTITLEMENTS - ONLY ONCE!
    @Column(name = "annual_leaves_entitled")
    private Integer annualLeavesEntitled = 24;

    @Column(name = "sick_leaves_entitled")
    private Integer sickLeavesEntitled = 12;

    @Column(name = "casual_leaves_entitled")
    private Integer casualLeavesEntitled = 12;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL)
    private Set<Attendance> attendances = new HashSet<>();

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL)
    private Set<LeaveRequest> leaveRequests = new HashSet<>();

    // Flexible KYC JSON stored as string (NVARCHAR(MAX))
    @Column(name = "kyc_data", columnDefinition = "NVARCHAR(MAX)")
    private String kycData;

    // Optional text/document URL for KYC or additional info
    @Column(name = "kyc_text_url", length = 512)
    private String kycTextUrl;

    // Document URLs extracted from KYC (optional)
    @Column(name = "pan_front_url", length = 512)
    private String panFrontUrl;

    @Column(name = "pan_back_url", length = 512)
    private String panBackUrl;

    @Column(name = "aadhaar_front_url", length = 512)
    private String aadhaarFrontUrl;

    @Column(name = "aadhaar_back_url", length = 512)
    private String aadhaarBackUrl;

    @Column(name = "employee_photo_url", length = 512)
    private String employeePhotoUrl;

    @Column(name = "signature_url", length = 512)
    private String signatureUrl;

    @Column(name = "bank_document_url", length = 512)
    private String bankDocumentUrl;

    // ==================== ENUMS ====================

    public enum EmploymentType {
        PERMANENT, CONTRACT, INTERN, CONSULTANT, PROBATION
    }

    public enum EmploymentStatus {
        ACTIVE, ON_LEAVE, SUSPENDED, TERMINATED, RESIGNED, INACTIVE
    }

    public enum Gender {
        MALE, FEMALE, OTHER
    }
}