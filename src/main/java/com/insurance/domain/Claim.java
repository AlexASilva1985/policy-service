package com.insurance.domain;

import com.insurance.domain.enums.ClaimStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "claims")
@Data
@EqualsAndHashCode(callSuper = true)
public class Claim extends BaseEntity {

    @Column(name = "claim_number", nullable = false, unique = true)
    private String claimNumber;

    @Column(name = "incident_date", nullable = false)
    private LocalDate incidentDate;

    @Column(nullable = false)
    private String description;

    @Column(name = "claim_amount", nullable = false)
    private BigDecimal claimAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClaimStatus status = ClaimStatus.SUBMITTED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    private InsurancePolicy policy;

    @Column(name = "supporting_documents")
    private String supportingDocuments;

    @Column(name = "adjustor_notes")
    private String adjustorNotes;

    @PrePersist
    public void onCreate() {
        if (status == null) {
            status = ClaimStatus.SUBMITTED;
        }
    }
} 