package com.insurance.domain;

import com.insurance.domain.enums.CustomerRiskType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * RiskAnalysis entity representing a risk analysis record in the database.
 */
@Entity
@Table(name = "risk_analysis")
@Data
@EqualsAndHashCode(callSuper = true)
public class RiskAnalysis extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CustomerRiskType classification;

    @Column(name = "analyzed_at", nullable = false)
    private LocalDateTime analyzedAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "risk_analysis_id")
    private List<RiskOccurrence> occurrences = new ArrayList<>();
} 