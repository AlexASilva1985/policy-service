package com.insurance.service;

import com.insurance.domain.Claim;
import com.insurance.domain.enums.ClaimStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Service responsible for handling claim business logic and status transitions.
 */
public interface ClaimStatusService {
    
    /**
     * Validates if a status transition is allowed
     */
    boolean canTransitionTo(ClaimStatus fromStatus, ClaimStatus toStatus);
    
    /**
     * Updates the claim status with proper validation
     */
    void updateClaimStatus(Claim claim, ClaimStatus newStatus);
    
    /**
     * Validates claim number format and business rules
     */
    void validateClaimNumber(String claimNumber);
    
    /**
     * Validates incident date against business rules
     */
    void validateIncidentDate(LocalDate incidentDate, Claim claim);
    
    /**
     * Validates claim amount against policy limits
     */
    void validateClaimAmount(BigDecimal claimAmount, Claim claim);
    
    /**
     * Validates the entire claim for business rules
     */
    void validateClaim(Claim claim);
} 