package com.insurance.service.impl;

import com.insurance.domain.PolicyRequest;
import com.insurance.domain.RiskAnalysis;
import com.insurance.domain.RiskOccurrence;
import com.insurance.domain.enums.CustomerRiskType;
import com.insurance.domain.enums.InsuranceCategory;
import com.insurance.service.FraudAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FraudAnalysisServiceImpl implements FraudAnalysisService {

    @Override
    @Transactional
    public RiskAnalysis analyzeFraud(PolicyRequest request) {
        validateRequest(request);

        try {
            RiskAnalysis riskAnalysis = new RiskAnalysis();
            riskAnalysis.setAnalyzedAt(LocalDateTime.now());
            
            CustomerRiskType classification = determineRiskClassification(request);
            riskAnalysis.setClassification(classification);
            
            List<RiskOccurrence> occurrences = generateOccurrences(request, classification);
            riskAnalysis.setOccurrences(occurrences);

            return riskAnalysis;
            
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to analyze fraud for policy request: " + request.getId(), e);
        }
    }

    private void validateRequest(PolicyRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Policy request cannot be null");
        }
        if (request.getId() == null) {
            throw new IllegalArgumentException("Policy request ID cannot be null");
        }
        if (request.getCustomerId() == null) {
            throw new IllegalArgumentException("customerId cannot be null");
        }
        if (request.getInsuredAmount() == null || request.getInsuredAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("insuredAmount must be greater than zero");
        }
        if (request.getCategory() == null) {
            throw new IllegalArgumentException("category cannot be null");
        }
    }

    private CustomerRiskType determineRiskClassification(PolicyRequest request) {
        BigDecimal amount = request.getInsuredAmount();
        InsuranceCategory category = request.getCategory();
        
        if (isHighRiskAmount(amount, category)) {
            return CustomerRiskType.HIGH_RISK;
        } else if (isPreferredAmount(amount, category)) {
            return CustomerRiskType.PREFERRED;
        } else {
            return CustomerRiskType.REGULAR;
        }
    }

    private boolean isHighRiskAmount(BigDecimal amount, InsuranceCategory category) {
        return switch (category) {
            case LIFE -> amount.compareTo(new BigDecimal("500000.00")) > 0;
            case AUTO -> amount.compareTo(new BigDecimal("300000.00")) > 0;
            case RESIDENTIAL -> amount.compareTo(new BigDecimal("400000.00")) > 0;
            case TRAVEL, HEALTH -> amount.compareTo(new BigDecimal("200000.00")) > 0;
        };
    }

    private boolean isPreferredAmount(BigDecimal amount, InsuranceCategory category) {
        return switch (category) {
            case LIFE -> amount.compareTo(new BigDecimal("200000.00")) >= 0 && amount.compareTo(new BigDecimal("500000.00")) <= 0;
            case AUTO -> amount.compareTo(new BigDecimal("150000.00")) >= 0 && amount.compareTo(new BigDecimal("300000.00")) <= 0;
            case RESIDENTIAL -> amount.compareTo(new BigDecimal("200000.00")) >= 0 && amount.compareTo(new BigDecimal("400000.00")) <= 0;
            case TRAVEL, HEALTH -> amount.compareTo(new BigDecimal("100000.00")) >= 0 && amount.compareTo(new BigDecimal("200000.00")) <= 0;
        };
    }
    
    private List<RiskOccurrence> generateOccurrences(PolicyRequest request, CustomerRiskType classification) {
        List<RiskOccurrence> occurrences = new ArrayList<>();
        
        if (classification == CustomerRiskType.HIGH_RISK) {
            RiskOccurrence occurrence = new RiskOccurrence();
            occurrence.setType("HIGH_VALUE");
            occurrence.setDescription("High insured amount detected for category " + request.getCategory());
            occurrence.setCreatedAt(LocalDateTime.now());
            occurrence.setUpdatedAt(LocalDateTime.now());
            occurrences.add(occurrence);
        }
        
        if (request.getInsuredAmount().compareTo(new BigDecimal("1000000.00")) > 0) {
            RiskOccurrence occurrence = new RiskOccurrence();
            occurrence.setType("EXTREME_VALUE");
            occurrence.setDescription("Extremely high insured amount detected");
            occurrence.setCreatedAt(LocalDateTime.now());
            occurrence.setUpdatedAt(LocalDateTime.now());
            occurrences.add(occurrence);
        }
        
        return occurrences;
    }
}