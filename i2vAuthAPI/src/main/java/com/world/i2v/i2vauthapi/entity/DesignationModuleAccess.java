package com.world.i2v.i2vauthapi.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "designation_module_access", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"designation_id", "module_id"})
})
@Data
public class DesignationModuleAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "designation_id", nullable = false)
    private Designation designation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = false)
    private Module module;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_level", nullable = false)
    private AccessLevel accessLevel = AccessLevel.READ;

    @Column(name = "can_create")
    private Boolean canCreate = false;

    @Column(name = "can_read")
    private Boolean canRead = true;

    @Column(name = "can_update")
    private Boolean canUpdate = false;

    @Column(name = "can_delete")
    private Boolean canDelete = false;

    @Column(name = "can_approve")
    private Boolean canApprove = false;

    @Column(name = "can_export")
    private Boolean canExport = false;

    @Column(name = "can_import")
    private Boolean canImport = false;

    // Temporarily disabled - database schema mismatch
    // @JdbcTypeCode(SqlTypes.JSON)
    // @Column(name = "restrictions", columnDefinition = "JSON")
    // private Map<String, Object> restrictions;

    @CreationTimestamp
    @Column(name = "granted_at", updatable = false)
    private LocalDateTime grantedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "granted_by")
    private User grantedBy;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    public boolean hasAccess(AccessLevel requiredLevel) {
        if (!isActive) return false;
        if (expiresAt != null && expiresAt.isBefore(LocalDateTime.now())) return false;

        return switch (requiredLevel) {
            case NONE -> true;
            case READ -> accessLevel == AccessLevel.READ ||
                    accessLevel == AccessLevel.WRITE ||
                    accessLevel == AccessLevel.ADMIN;
            case WRITE -> accessLevel == AccessLevel.WRITE ||
                    accessLevel == AccessLevel.ADMIN;
            case ADMIN -> accessLevel == AccessLevel.ADMIN;
        };
    }
}