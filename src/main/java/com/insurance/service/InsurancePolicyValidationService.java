package com.insurance.service;

import com.insurance.domain.InsurancePolicy;
import com.insurance.domain.enums.InsuranceCategory;
import com.insurance.domain.enums.PolicyStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Service responsible for handling insurance policy business logic and validations.
 */
public interface InsurancePolicyValidationService {
    
    /**
     * Validates policy number format and business rules
     */
    void validatePolicyNumber(String policyNumber);
    
    /**
     * Validates start date against business rules
     */
    void validateStartDate(LocalDate startDate, LocalDate endDate);
    
    /**
     * Validates end date against business rules
     */
    void validateEndDate(LocalDate endDate, LocalDate startDate);
    
    /**
     * Validates premium amount
     */
    void validatePremium(BigDecimal premium);
    
    /**
     * Validates coverage amount
     */
    void validateCoverageAmount(BigDecimal coverageAmount);
    
    /**
     * Validates policy status
     */
    void validateStatus(PolicyStatus status);
    
    /**
     * Validates insurance category/type
     */
    void validateType(InsuranceCategory type);
    
    /**
     * Validates the entire insurance policy for business rules
     */
    void validateInsurancePolicy(InsurancePolicy policy);
    
    /**
     * Validates if dates are consistent with each other
     */
    void validateDateConsistency(LocalDate startDate, LocalDate endDate);
} 