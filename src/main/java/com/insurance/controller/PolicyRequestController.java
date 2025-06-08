package com.insurance.controller;

import com.insurance.dto.FraudAnalysisResponseDTO;
import com.insurance.dto.PolicyRequestDTO;
import com.insurance.dto.PolicyValidationResponseDTO;
import com.insurance.dto.PolicyCancelResponseDTO;
import com.insurance.mapper.PolicyRequestMapper;
import com.insurance.domain.PolicyRequest;
import com.insurance.service.PolicyRequestService;
import io.micrometer.core.annotation.Timed;
import jakarta.persistence.EntityNotFoundException;
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
    public PolicyRequestDTO createPolicyRequest(
            @Valid @RequestBody PolicyRequestDTO request) {

        PolicyRequest entity = mapper.toEntity(request);
        PolicyRequest created = service.createPolicyRequest(entity);
        return mapper.toDTO(created);
    }

    @GetMapping("/{id}")
    @Timed(value = "policy.request.get", description = "Time taken to get a policy request")
    public ResponseEntity<PolicyRequestDTO> getPolicyRequest(@PathVariable UUID id) {
        try {
            PolicyRequest entity = service.findById(id);
            PolicyRequestDTO dto = mapper.toDTO(entity);
            return ResponseEntity.ok(dto);
        } catch (EntityNotFoundException e) {
            log.warn("Policy request not found with id: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error getting policy request with id: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/customer/{customerId}")
    @Timed(value = "policy.request.get.by.customer", description = "Time taken to get policy requests by customer")
    public ResponseEntity<List<PolicyRequestDTO>> getPolicyRequestsByCustomer(@PathVariable UUID customerId) {

        try {
            List<PolicyRequest> policyRequests = service.findByCustomerId(customerId);
            List<PolicyRequestDTO> result = policyRequests.stream()
                    .map(mapper::toDTO)
                    .collect(Collectors.toList());
            
            log.info("Found {} policy requests for customer: {}", result.size(), customerId);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error getting policy requests for customer: {}", customerId, e);
            return ResponseEntity.ok(List.of());
        }
    }

    @PostMapping("/{id}/validate")
    @Timed(value = "policy.request.validate", description = "Time taken to validate a policy request")
    public ResponseEntity<PolicyValidationResponseDTO> validate(@PathVariable UUID id) {
        PolicyValidationResponseDTO result = service.validatePolicyRequest(id);
        
        if (result.isValidated()) {
            return ResponseEntity.ok(result);
        } else if (result.getStatus() == null) {
            return ResponseEntity.internalServerError().body(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    @PostMapping("/{id}/fraud-analysis")
    @Timed(value = "policy.request.fraud.analysis", description = "Time taken to process fraud analysis")
    public ResponseEntity<FraudAnalysisResponseDTO> processFraudAnalysis(@PathVariable UUID id) {
        try {

            FraudAnalysisResponseDTO response = service.processFraudAnalysis(id);
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            log.warn("Policy request not found with id: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error processing fraud analysis for policy request with id: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{id}/payment")
    @Timed(value = "policy.request.payment", description = "Time taken to process payment")
    public ResponseEntity<Void> processPayment(@PathVariable UUID id) {
        try {
            service.processPayment(id);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            log.warn("Cannot process payment for policy request {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (EntityNotFoundException e) {
            log.warn("Policy request not found with id: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error processing payment for policy request with id: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{id}/subscription")
    @Timed(value = "policy.request.subscription", description = "Time taken to process subscription")
    public ResponseEntity<Void> processSubscription(@PathVariable UUID id) {
        try {
            service.processSubscription(id);
            return ResponseEntity.ok().build();
        } catch (EntityNotFoundException e) {
            log.warn("Policy request not found with id: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error processing subscription for policy request with id: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{id}/cancel")
    @Timed(value = "policy.request.cancel", description = "Time taken to cancel a policy request")
    public ResponseEntity<PolicyCancelResponseDTO> cancelPolicyRequest(@PathVariable UUID id) {
        PolicyCancelResponseDTO result = service.cancelPolicyRequest(id);
        
        if (result.isCancelled()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }
}