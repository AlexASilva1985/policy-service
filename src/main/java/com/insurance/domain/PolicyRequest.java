package com.insurance.domain;

import com.insurance.domain.enums.InsuranceCategory;
import com.insurance.domain.enums.PaymentMethod;
import com.insurance.domain.enums.PolicyStatus;
import com.insurance.domain.enums.SalesChannel;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "policy_requests")
@Data
@EqualsAndHashCode(callSuper = true)
public class PolicyRequest extends BaseEntity {

    @Column(nullable = false)
    private UUID customerId;

    @Column(nullable = false)
    private UUID productId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InsuranceCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SalesChannel salesChannel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PolicyStatus status = PolicyStatus.RECEIVED;

    @Column(name = "total_monthly_premium_amount", nullable = false)
    private BigDecimal totalMonthlyPremiumAmount;

    @Column(name = "insured_amount", nullable = false)
    private BigDecimal insuredAmount;

    @ElementCollection
    @CollectionTable(name = "policy_request_coverages", 
                    joinColumns = @JoinColumn(name = "policy_request_id"))
    @MapKeyColumn(name = "coverage_name")
    @Column(name = "coverage_amount")
    private Map<String, BigDecimal> coverages = new HashMap<>();

    @ElementCollection
    @CollectionTable(name = "policy_request_assistances", 
                    joinColumns = @JoinColumn(name = "policy_request_id"))
    @Column(name = "assistance_name")
    private List<String> assistances = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "policy_request_id")
    @OrderBy("changedAt DESC")
    private List<StatusHistory> statusHistory = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "risk_analysis_id")
    private RiskAnalysis riskAnalysis;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    public BigDecimal calculateTotalCoverageAmount() {
        return coverages.values()
                       .stream()
                       .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
} 