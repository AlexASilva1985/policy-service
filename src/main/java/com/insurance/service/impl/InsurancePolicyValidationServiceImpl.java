package com.insurance.service.impl;

import com.insurance.domain.InsurancePolicy;
import com.insurance.domain.enums.InsuranceCategory;
import com.insurance.domain.enums.PolicyStatus;
import com.insurance.service.InsurancePolicyValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class InsurancePolicyValidationServiceImpl implements InsurancePolicyValidationService {

    @Override
    public void validatePolicyNumber(String policyNumber) {
        if (policyNumber == null || policyNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Policy number cannot be empty");
        }
        if (!policyNumber.startsWith("POL-")) {
            throw new IllegalArgumentException("Policy number must start with 'POL-'");
        }
    }

    @Override
    public void validateStartDate(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            throw new IllegalArgumentException("Start date cannot be null");
        }
        if (endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }
    }

    @Override
    public void validateEndDate(LocalDate endDate, LocalDate startDate) {
        if (endDate == null) {
            throw new IllegalArgumentException("End date cannot be null");
        }
        if (startDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }
    }

    @Override
    public void validatePremium(BigDecimal premium) {
        if (premium == null) {
            throw new IllegalArgumentException("Premium cannot be null");
        }
        if (premium.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Premium must be greater than zero");
        }
    }

    @Override
    public void validateCoverageAmount(BigDecimal coverageAmount) {
        if (coverageAmount == null) {
            throw new IllegalArgumentException("Coverage amount cannot be null");
        }
        if (coverageAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Coverage amount must be greater than zero");
        }
    }

    @Override
    public void validateStatus(PolicyStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
    }

    @Override
    public void validateType(InsuranceCategory type) {
        if (type == null) {
            throw new IllegalArgumentException("Insurance type cannot be null");
        }
    }

    @Override
    public void validateDateConsistency(LocalDate startDate, LocalDate endDate) {
        validateStartDate(startDate, endDate);
        validateEndDate(endDate, startDate);
    }

    @Override
    public void validateInsurancePolicy(InsurancePolicy policy) {
        if (policy == null) {
            throw new IllegalArgumentException("Insurance policy cannot be null");
        }
        
        if (policy.getPolicyNumber() == null || policy.getPolicyNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Policy number is required");
        }
        validatePolicyNumber(policy.getPolicyNumber());
        
        if (policy.getStartDate() == null) {
            throw new IllegalArgumentException("Start date is required");
        }
        
        if (policy.getEndDate() == null) {
            throw new IllegalArgumentException("End date is required");
        }
        
        validateDateConsistency(policy.getStartDate(), policy.getEndDate());
        
        if (policy.getPremium() == null) {
            throw new IllegalArgumentException("Premium is required");
        }
        validatePremium(policy.getPremium());
        
        if (policy.getCoverageAmount() == null) {
            throw new IllegalArgumentException("Coverage amount is required");
        }
        validateCoverageAmount(policy.getCoverageAmount());
        
        if (policy.getStatus() == null) {
            throw new IllegalArgumentException("Status is required");
        }
        validateStatus(policy.getStatus());
        
        if (policy.getType() == null) {
            throw new IllegalArgumentException("Insurance type is required");
        }
        validateType(policy.getType());
        
    }
} 