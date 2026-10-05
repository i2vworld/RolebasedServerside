package com.world.i2v.i2vauthapi.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "clearance_steps")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClearanceStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String step;               // e.g., "Manager Approval", "HR Clearance"

    @Column(name = "assigned_to", nullable = false, length = 50)
    private String assignedTo;         // "manager", "hr", "it", etc.

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ExitRequest.ExitStatus status = ExitRequest.ExitStatus.PENDING;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exit_request_id", nullable = false)
    private ExitRequest exitRequest;
}