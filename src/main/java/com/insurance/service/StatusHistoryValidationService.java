package com.insurance.service;

import com.insurance.domain.StatusHistory;
import com.insurance.domain.enums.PolicyStatus;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service responsible for handling status history business logic and validations.
 * This service contains all the business rules that were previously in the StatusHistory entity,
 * following the Single Responsibility Principle.
 */
public interface StatusHistoryValidationService {
    
    /**
     * Validates a policy request ID
     */
    void validatePolicyRequestId(UUID policyRequestId);
    
    /**
     * Validates a previous status
     */
    void validatePreviousStatus(PolicyStatus previousStatus);
    
    /**
     * Validates a new status
     */
    void validateNewStatus(PolicyStatus newStatus);
    
    /**
     * Validates that new status is different from previous status
     */
    void validateStatusDifference(PolicyStatus previousStatus, PolicyStatus newStatus);
    
    /**
     * Validates if a status transition is valid
     */
    void validateStatusTransition(PolicyStatus fromStatus, PolicyStatus toStatus);
    
    /**
     * Validates a changed at timestamp
     */
    void validateChangedAt(LocalDateTime changedAt);
    
    /**
     * Validates the entire status history for business rules
     */
    void validateStatusHistory(StatusHistory statusHistory);
    
    /**
     * Creates a validated status history entry
     */
    StatusHistory createStatusHistory(UUID policyRequestId, PolicyStatus previousStatus, 
                                     PolicyStatus newStatus, String reason);
    
    /**
     * Safely updates status history fields with validation
     */
    void updateStatusHistory(StatusHistory statusHistory, UUID policyRequestId, 
                           PolicyStatus previousStatus, PolicyStatus newStatus, 
                           LocalDateTime changedAt, String reason);
} 