package com.insurance.service;

import com.insurance.domain.PolicyRequest;
import com.insurance.domain.enums.PolicyStatus;
import com.insurance.dto.FraudAnalysisResponseDTO;
import com.insurance.dto.PolicyValidationResponseDTO;
import com.insurance.dto.PolicyCancelResponseDTO;

import java.util.List;
import java.util.UUID;

public interface PolicyRequestService {
    /**
     * Creates a new policy request
     */
    PolicyRequest createPolicyRequest(PolicyRequest request);

    /**
     * Finds a policy request by its ID
     */
    PolicyRequest findById(UUID id);

    /**
     * Finds all policy requests for a given customer
     */
    List<PolicyRequest> findByCustomerId(UUID customerId);

    /**
     * Updates the status of a policy request
     */
    PolicyRequest updateStatus(UUID id, PolicyStatus newStatus);

    /**
     * Validates a policy request based on risk analysis
     */
    PolicyValidationResponseDTO validatePolicyRequest(UUID id);

    /**
     * Processes fraud analysis for a policy request
     */
    FraudAnalysisResponseDTO processFraudAnalysis(UUID id);

    /**
     * Processes payment for a policy request
     */
    void processPayment(UUID id);

    /**
     * Processes subscription for a policy request
     */
    void processSubscription(UUID id);

    /**
     * Cancels a policy request if it's not already approved
     */
    PolicyCancelResponseDTO cancelPolicyRequest(UUID id);
} 