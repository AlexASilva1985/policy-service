package com.insurance.service;

import com.insurance.domain.RiskOccurrence;
import java.time.LocalDateTime;

/**
 * Service responsible for handling risk occurrence business logic and validations.
 */
public interface RiskOccurrenceValidationService {
    
    /**
     * Validates risk occurrence type
     */
    void validateType(String type);
    
    /**
     * Validates risk occurrence description
     */
    void validateDescription(String description);
    
    /**
     * Validates creation timestamp
     */
    void validateCreatedAt(LocalDateTime createdAt);
    
    /**
     * Validates update timestamp
     */
    void validateUpdatedAt(LocalDateTime updatedAt, LocalDateTime createdAt);
    
    /**
     * Validates the entire risk occurrence for business rules
     */
    void validateRiskOccurrence(RiskOccurrence occurrence);
    
    /**
     * Validates timestamp constraints (not in future, proper ordering)
     */
    void validateTimestampConstraints(LocalDateTime timestamp);
} 