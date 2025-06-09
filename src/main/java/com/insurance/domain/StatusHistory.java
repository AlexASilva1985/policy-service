package com.insurance.domain;

import com.insurance.domain.enums.PolicyStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * StatusHistory entity representing a status change record in the database.
 */
@Entity
@Table(name = "status_history")
@Data
@EqualsAndHashCode(callSuper = true)
public class StatusHistory extends BaseEntity {

    @Column(name = "policy_request_id", nullable = false)
    private UUID policyRequestId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PolicyStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PolicyStatus newStatus;

    @Column(nullable = false)
    private LocalDateTime changedAt;

    private String reason;

    @PrePersist
    public void onCreate() {
        if (changedAt == null) {
            changedAt = LocalDateTime.now();
        }
    }
} 