package com.insurance.service.impl;

import com.insurance.domain.RiskAnalysis;
import com.insurance.domain.RiskOccurrence;
import com.insurance.domain.enums.CustomerRiskType;
import com.insurance.service.RiskAnalysisValidationService;
import com.insurance.service.RiskOccurrenceValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class RiskAnalysisValidationServiceImpl implements RiskAnalysisValidationService {

    private final RiskOccurrenceValidationService riskOccurrenceValidationService;

    @Override
    public void validateClassification(CustomerRiskType classification) {
        if (classification == null) {
            throw new IllegalArgumentException("Classification cannot be null");
        }
    }

    @Override
    public void validateAnalyzedAt(LocalDateTime analyzedAt) {
        if (analyzedAt == null) {
            throw new IllegalArgumentException("Analyzed at timestamp cannot be null");
        }
        validateAnalysisTimeConstraints(analyzedAt);
    }

    @Override
    public void validateAnalysisTimeConstraints(LocalDateTime analyzedAt) {
        if (analyzedAt != null && analyzedAt.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Analysis timestamp cannot be in the future");
        }
    }

    @Override
    public void validateOccurrence(RiskOccurrence occurrence) {
        if (occurrence == null) {
            throw new IllegalArgumentException("Risk occurrence cannot be null");
        }
        riskOccurrenceValidationService.validateRiskOccurrence(occurrence);
    }

    @Override
    public void addOccurrence(RiskAnalysis riskAnalysis, RiskOccurrence occurrence) {
        if (riskAnalysis == null) {
            throw new IllegalArgumentException("Risk analysis cannot be null");
        }
        
        validateOccurrence(occurrence);
        
        riskAnalysis.getOccurrences().add(occurrence);
    }

    @Override
    public void removeOccurrence(RiskAnalysis riskAnalysis, RiskOccurrence occurrence) {
        if (riskAnalysis == null) {
            throw new IllegalArgumentException("Risk analysis cannot be null");
        }
        if (occurrence == null) {
            throw new IllegalArgumentException("Risk occurrence cannot be null");
        }
        
        boolean removed = riskAnalysis.getOccurrences().remove(occurrence);
        if (removed) {
            log.debug("Removed risk occurrence {} from analysis {}",
                    occurrence.getType(), riskAnalysis.getId());
        }
    }

    @Override
    public void validateRiskAnalysis(RiskAnalysis riskAnalysis) {
        if (riskAnalysis == null) {
            throw new IllegalArgumentException("Risk analysis cannot be null");
        }
        
        if (riskAnalysis.getClassification() == null) {
            throw new IllegalArgumentException("Classification is required");
        }
        validateClassification(riskAnalysis.getClassification());
        
        if (riskAnalysis.getAnalyzedAt() == null) {
            throw new IllegalArgumentException("Analyzed at timestamp is required");
        }
        validateAnalyzedAt(riskAnalysis.getAnalyzedAt());
        
        if (riskAnalysis.getOccurrences() != null) {
            for (RiskOccurrence occurrence : riskAnalysis.getOccurrences()) {
                validateOccurrence(occurrence);
            }
        }
    }
} 