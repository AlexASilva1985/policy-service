package com.insurance.domain;

import com.insurance.domain.enums.InsuranceCategory;
import com.insurance.domain.enums.PolicyStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "insurance_policies")
@Data
@EqualsAndHashCode(callSuper = true)
public class InsurancePolicy extends BaseEntity {

    @Column(name = "policy_number", nullable = false, unique = true)
    private String policyNumber;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private BigDecimal premium;

    @Column(name = "coverage_amount", nullable = false)
    private BigDecimal coverageAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PolicyStatus status;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InsuranceCategory type;
}