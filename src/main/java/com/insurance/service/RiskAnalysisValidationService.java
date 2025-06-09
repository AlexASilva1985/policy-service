package com.insurance.service;

import com.insurance.domain.RiskAnalysis;
import com.insurance.domain.RiskOccurrence;
import com.insurance.domain.enums.CustomerRiskType;
import java.time.LocalDateTime;

/**
 * Service responsible for handling risk analysis business logic and validations.
 */
public interface RiskAnalysisValidationService {
    
    /**
     * Validates customer risk type classification
     */
    void validateClassification(CustomerRiskType classification);
    
    /**
     * Validates analyzed at timestamp
     */
    void validateAnalyzedAt(LocalDateTime analyzedAt);
    
    /**
     * Validates a risk occurrence before adding to analysis
     */
    void validateOccurrence(RiskOccurrence occurrence);
    
    /**
     * Validates the entire risk analysis for business rules
     */
    void validateRiskAnalysis(RiskAnalysis riskAnalysis);
    
    /**
     * Safely adds an occurrence to the risk analysis with validation
     */
    void addOccurrence(RiskAnalysis riskAnalysis, RiskOccurrence occurrence);
    
    /**
     * Safely removes an occurrence from the risk analysis
     */
    void removeOccurrence(RiskAnalysis riskAnalysis, RiskOccurrence occurrence);
    
    /**
     * Validates if the analysis timestamp is not in the future
     */
    void validateAnalysisTimeConstraints(LocalDateTime analyzedAt);
} 