package com.insurance.controller;

import com.insurance.dto.FraudAnalysisResponseDTO;
import com.insurance.dto.PolicyRequestDTO;
import com.insurance.dto.PolicyValidationResponseDTO;
import com.insurance.dto.PolicyCancelResponseDTO;
import com.insurance.mapper.PolicyRequestMapper;
import com.insurance.domain.PolicyRequest;
import com.insurance.service.PolicyRequestService;
import io.micrometer.core.annotation.Timed;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/policy")
@RequiredArgsConstructor
public class PolicyRequestController {

    private static final Logger log = LoggerFactory.getLogger(PolicyRequestController.class);
    private final PolicyRequestService service;
    private final PolicyRequestMapper mapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Timed(value = "policy.request.create", description = "Time taken to create a policy request")
    public PolicyRequestDTO createPolicyRequest(@Valid @RequestBody PolicyRequestDTO request) {
        log.info("Creating policy request for customer: {}", request.getCustomerId());
        
        PolicyRequest entity = mapper.toEntity(request);
        PolicyRequest created = service.createPolicyRequest(entity);
        
        return mapper.toDTO(created);
    }

    @GetMapping("/{id}")
    @Timed(value = "policy.request.get", description = "Time taken to get a policy request")
    public PolicyRequestDTO getPolicyRequest(@PathVariable UUID id) {
        log.debug("Getting policy request with id: {}", id);
        
        PolicyRequest entity = service.findById(id);
        return mapper.toDTO(entity);
    }

    @GetMapping("/customer/{customerId}")
    @Timed(value = "policy.request.get.by.customer", description = "Time taken to get policy requests by customer")
    public List<PolicyRequestDTO> getPolicyRequestsByCustomer(@PathVariable UUID customerId) {
        log.debug("Getting policy requests for customer: {}", customerId);
        
        try {
            List<PolicyRequest> policyRequests = service.findByCustomerId(customerId);
            
            List<PolicyRequestDTO> result = policyRequests.stream()
                    .map(mapper::toDTO)
                    .collect(Collectors.toList());
            
            log.info("Found {} policy requests for customer: {}", result.size(), customerId);
            return result;
        } catch (Exception e) {
            log.error("Error retrieving policy requests for customer: {}", customerId, e);
            return List.of(); // Return empty list on error
        }
    }

    @PostMapping("/{id}/validate")
    @Timed(value = "policy.request.validate", description = "Time taken to validate a policy request")
    public ResponseEntity<PolicyValidationResponseDTO> validate(@PathVariable UUID id) {
        log.info("Validating policy request: {}", id);
        
        PolicyValidationResponseDTO response = service.validatePolicyRequest(id);
        
        if (response.isValidated()) {
            return ResponseEntity.ok(response);
        } else if (response.getStatus() != null) {
            // Business validation failure
            return ResponseEntity.badRequest().body(response);
        } else {
            // System error
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/{id}/fraud-analysis")
    @Timed(value = "policy.request.fraud.analysis", description = "Time taken to process fraud analysis")
    public FraudAnalysisResponseDTO processFraudAnalysis(@PathVariable UUID id) {
        log.info("Processing fraud analysis for policy: {}", id);
        
        return service.processFraudAnalysis(id);
    }

    @PostMapping("/{id}/payment")
    @Timed(value = "policy.request.payment", description = "Time taken to process payment")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void processPayment(@PathVariable UUID id) {
        log.info("Processing payment for policy: {}", id);
        
        service.processPayment(id);
    }

    @PostMapping("/{id}/subscription")
    @Timed(value = "policy.request.subscription", description = "Time taken to process subscription")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void processSubscription(@PathVariable UUID id) {
        log.info("Processing subscription for policy: {}", id);
        
        service.processSubscription(id);
    }

    @PostMapping("/{id}/cancel")
    @Timed(value = "policy.request.cancel", description = "Time taken to cancel a policy request")
    public ResponseEntity<PolicyCancelResponseDTO> cancelPolicyRequest(@PathVariable UUID id) {
        log.info("Cancelling policy request: {}", id);
        
        PolicyCancelResponseDTO response = service.cancelPolicyRequest(id);
        
        if (response.isCancelled()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
}