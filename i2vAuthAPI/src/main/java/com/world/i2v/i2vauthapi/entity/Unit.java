package com.world.i2v.i2vauthapi.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "units")
@Data
@EqualsAndHashCode(exclude = {"squads"})
@ToString(exclude = {"squads"})
public class Unit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(unique = true, length = 20)
    private String code;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(length = 100)
    private String country;

    @Column(name = "access_level")
    private Integer accessLevel = 1;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @OneToMany(mappedBy = "unit", cascade = CascadeType.ALL, orphanRemoval = false)
    private Set<Squad> squads = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
