package com.world.i2v.i2vauthapi.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "modules")
@Data
public class Module {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(unique = true, length = 50)
    private String code;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_module_id")
    private Module parentModule;

    @OneToMany(mappedBy = "parentModule", cascade = CascadeType.ALL)
    private Set<Module> subModules = new HashSet<>();

    @Column(length = 50)
    private String icon;

    @Column(name = "route_path", length = 255)
    private String routePath;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "is_visible")
    private Boolean isVisible = true;

    @Column(name = "requires_premium")
    private Boolean requiresPremium = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "module", cascade = CascadeType.ALL)
    private Set<DepartmentModule> departmentModules = new HashSet<>();

    @OneToMany(mappedBy = "module", cascade = CascadeType.ALL)
    private Set<DesignationModuleAccess> designationAccesses = new HashSet<>();
}