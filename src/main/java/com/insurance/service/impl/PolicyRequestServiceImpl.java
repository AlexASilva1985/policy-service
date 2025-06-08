package com.insurance.service.impl;

import com.insurance.domain.PolicyRequest;
import com.insurance.domain.RiskAnalysis;
import com.insurance.domain.RiskOccurrence;
import com.insurance.domain.enums.*;
import com.insurance.dto.FraudAnalysisResponseDTO;
import com.insurance.dto.PolicyCancelResponseDTO;
import com.insurance.dto.PolicyValidationResponseDTO;
import com.insurance.event.*;
import com.insurance.infrastructure.messaging.config.RabbitMQConfig;
import com.insurance.infrastructure.messaging.service.EventPublisher;
import com.insurance.repository.PolicyRequestRepository;
import com.insurance.service.FraudAnalysisService;
import com.insurance.service.PaymentService;
import com.insurance.service.PolicyRequestService;
import com.insurance.service.SubscriptionService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PolicyRequestServiceImpl implements PolicyRequestService {

    private final PolicyRequestRepository repository;
    private final FraudAnalysisService fraudAnalysisService;
    private final PaymentService paymentService;
    private final SubscriptionService subscriptionService;
    private final EventPublisher eventPublisher;

    @Override
    @Transactional
    public PolicyRequest createPolicyRequest(PolicyRequest request) {
        log.info("Creating policy request for customer: {}, category: {}, amount: {}", 
                request.getCustomerId(), request.getCategory(), request.getInsuredAmount());
        
        try {
            request.setStatus(PolicyStatus.RECEIVED);
            request.setCreatedAt(LocalDateTime.now());
            request.setUpdatedAt(LocalDateTime.now());
            
            PolicyRequest savedRequest = repository.save(request);
            
            eventPublisher.publish(
                RabbitMQConfig.POLICY_EVENTS_EXCHANGE,
                RabbitMQConfig.POLICY_CREATED_KEY,
                new PolicyRequestCreatedEvent(savedRequest)
            );
            
            log.info("Policy request created successfully with ID: {}", savedRequest.getId());
            return savedRequest;
            
        } catch (Exception e) {
            log.error("Error creating policy request for customer: {}", request.getCustomerId(), e);
            throw e;
        }
    }

    @Override
    public PolicyRequest findById(UUID id) {
        log.debug("Finding policy request by ID: {}", id);
        
        return repository.findById(id)
            .orElseThrow(() -> {
                log.warn("Policy request not found with ID: {}", id);
                return new EntityNotFoundException("Policy request not found with id: " + id);
            });
    }

    @Override
    public List<PolicyRequest> findByCustomerId(UUID customerId) {
        log.debug("Finding policy requests for customer: {}", customerId);
        return repository.findByCustomerId(customerId);
    }

    @Override
    @Transactional
    public PolicyRequest updateStatus(UUID id, PolicyStatus newStatus) {
        log.info("Updating status for policy request: {} to: {}", id, newStatus);
        
        PolicyRequest request = findById(id);
        PolicyStatus currentStatus = request.getStatus();
        
        if (!isValidStatusTransition(currentStatus, newStatus)) {
            String errorMsg = String.format("Cannot transition from %s to %s", currentStatus, newStatus);
            log.warn("Invalid status transition for policy {}: {}", id, errorMsg);
            throw new IllegalStateException(errorMsg);
        }
        
        request.setStatus(newStatus);
        request.setUpdatedAt(LocalDateTime.now());
        
        PolicyRequest updatedRequest = repository.save(request);
        
        // Publicar evento baseado no novo status
        String routingKey = switch (newStatus) {
            case VALIDATED -> RabbitMQConfig.POLICY_VALIDATED_KEY;
            case PENDING -> RabbitMQConfig.PAYMENT_PROCESSED_KEY;
            case APPROVED -> RabbitMQConfig.POLICY_APPROVED_KEY;
            case REJECTED -> RabbitMQConfig.POLICY_REJECTED_KEY;
            case CANCELLED -> "policy.cancelled";
            default -> RabbitMQConfig.POLICY_STATUS_UPDATED_KEY;
        };
        
        eventPublisher.publish(
            RabbitMQConfig.POLICY_EVENTS_EXCHANGE,
            routingKey,
            new PolicyRequestEvent(updatedRequest)
        );
        
        log.info("Status updated successfully for policy: {} from {} to {}", 
                id, currentStatus, newStatus);
        
        return updatedRequest;
    }

    @Override
    @Transactional
    public PolicyValidationResponseDTO validatePolicyRequest(UUID id) {
        log.info("Starting validation for policy request: {}", id);
        
        try {
            PolicyRequest request = findById(id);
            
            if (request.getRiskAnalysis() == null) {
                log.warn("Cannot validate policy {} without risk analysis", id);
                return PolicyValidationResponseDTO.failure(id, 
                    "Não é possível validar apólice sem análise de risco");
            }
            
            boolean isValid = validateInsuranceAmount(
                request.getCategory(), 
                request.getInsuredAmount(), 
                request.getRiskAnalysis().getClassification()
            );
            
            if (isValid) {
                request.setStatus(PolicyStatus.VALIDATED);
                repository.save(request);
                
                log.info("Policy {} validated successfully", id);
                return PolicyValidationResponseDTO.success(id);
            } else {
                request.setStatus(PolicyStatus.REJECTED);
                repository.save(request);
                
                String reason = buildValidationFailureReason(
                    request.getCategory(), 
                    request.getInsuredAmount(), 
                    request.getRiskAnalysis().getClassification()
                );
                
                log.warn("Policy {} validation failed: {}", id, reason);
                return PolicyValidationResponseDTO.failure(id, reason);
            }
            
        } catch (Exception e) {
            log.error("Error validating policy request: {}", id, e);
            return PolicyValidationResponseDTO.error(id, "Erro interno durante validação: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public FraudAnalysisResponseDTO processFraudAnalysis(UUID id) {
        log.info("Starting fraud analysis for policy request: {}", id);
        
        PolicyRequest request = findById(id);
        
        try {
            RiskAnalysis riskAnalysis = fraudAnalysisService.analyzeFraud(request);
            request.setRiskAnalysis(riskAnalysis);
            repository.save(request);

            FraudAnalysisResponseDTO response = new FraudAnalysisResponseDTO();
            response.setOrderId(request.getId());
            response.setCustomerId(request.getCustomerId());
            response.setAnalyzedAt(riskAnalysis.getAnalyzedAt());
            response.setClassification(riskAnalysis.getClassification());
            
            List<FraudAnalysisResponseDTO.RiskOccurrenceDTO> occurrenceDTOs =
                riskAnalysis.getOccurrences().stream()
                    .map(this::mapToOccurrenceDTO)
                    .collect(Collectors.toList());
            response.setOccurrences(occurrenceDTOs);
            
            log.info("Fraud analysis completed for policy {}: classification={}, occurrences={}", 
                    id, riskAnalysis.getClassification(), riskAnalysis.getOccurrences().size());
            
            return response;
            
        } catch (Exception e) {
            log.error("Error analyzing fraud for policy request: {}", id, e);
            request.setStatus(PolicyStatus.REJECTED);
            repository.save(request);
            eventPublisher.publish(
                RabbitMQConfig.POLICY_EVENTS_EXCHANGE,
                RabbitMQConfig.POLICY_REJECTED_KEY,
                new PolicyRejectedEvent(request)
            );
            throw e;
        }
    }
    
    private FraudAnalysisResponseDTO.RiskOccurrenceDTO mapToOccurrenceDTO(RiskOccurrence occurrence) {
        FraudAnalysisResponseDTO.RiskOccurrenceDTO dto = new FraudAnalysisResponseDTO.RiskOccurrenceDTO();
        dto.setId(UUID.randomUUID());
        dto.setProductId(12345L);
        dto.setType(occurrence.getType());
        dto.setDescription(occurrence.getDescription());
        dto.setCreatedAt(occurrence.getCreatedAt());
        dto.setUpdatedAt(occurrence.getUpdatedAt());
        return dto;
    }

    @Override
    @Transactional
    public void processPayment(UUID id) {
        log.info("Processing payment for policy request: {}", id);
        
        PolicyRequest request = findById(id);
        
        if (request.getStatus() != PolicyStatus.VALIDATED) {
            String errorMsg = "Cannot process payment for non-validated policy request";
            log.warn("Payment processing failed for policy {}: {}", id, errorMsg);
            throw new IllegalStateException(errorMsg);
        }
        
        try {
            boolean success = paymentService.processPayment(request);
            
            if (success) {
                request.setStatus(PolicyStatus.PENDING);
                repository.save(request);
                eventPublisher.publish(
                    RabbitMQConfig.POLICY_EVENTS_EXCHANGE,
                    RabbitMQConfig.PAYMENT_PROCESSED_KEY,
                    new PaymentProcessedEvent(request)
                );
                
                log.info("Payment processed successfully for policy: {}", id);
            } else {
                request.setStatus(PolicyStatus.REJECTED);
                repository.save(request);
                eventPublisher.publish(
                    RabbitMQConfig.POLICY_EVENTS_EXCHANGE,
                    RabbitMQConfig.PAYMENT_REJECTED_KEY,
                    new PaymentRejectedEvent(request)
                );
                
                log.warn("Payment processing failed for policy: {}", id);
            }
            
        } catch (Exception e) {
            log.error("Error processing payment for policy: {}", id, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public void processSubscription(UUID id) {
        log.info("Processing subscription for policy request: {}", id);
        
        PolicyRequest request = findById(id);
        
        try {
            subscriptionService.processSubscription(request);
            request.setStatus(PolicyStatus.APPROVED);
            repository.save(request);
            eventPublisher.publish(
                RabbitMQConfig.POLICY_EVENTS_EXCHANGE,
                RabbitMQConfig.POLICY_APPROVED_KEY,
                new SubscriptionApprovedEvent(request)
            );
            
            log.info("Subscription processed successfully for policy: {}", id);
            
        } catch (Exception e) {
            log.error("Error processing subscription for policy request: {}", id, e);
            request.setStatus(PolicyStatus.REJECTED);
            repository.save(request);
            eventPublisher.publish(
                RabbitMQConfig.POLICY_EVENTS_EXCHANGE,
                RabbitMQConfig.POLICY_REJECTED_KEY,
                new PolicyRejectedEvent(request)
            );
        }
    }

    @Override
    @Transactional
    public PolicyCancelResponseDTO cancelPolicyRequest(UUID id) {
        log.info("Attempting to cancel policy request: {}", id);
        
        try {
            PolicyRequest request = findById(id);
            
            if (request.getStatus() == PolicyStatus.CANCELLED) {
                log.debug("Policy {} is already cancelled", id);
                return PolicyCancelResponseDTO.success(id);
            }
            
            if (request.getStatus() == PolicyStatus.APPROVED) {
                log.warn("Cannot cancel approved policy: {}", id);
                return PolicyCancelResponseDTO.failure(id,
                    "Não é possível cancelar uma apólice já aprovada");
            }
            
            request.setStatus(PolicyStatus.CANCELLED);
            repository.save(request);
            
            eventPublisher.publish(
                RabbitMQConfig.POLICY_EVENTS_EXCHANGE,
                "policy.cancelled",
                new PolicyCancelledEvent(request)
            );
            
            log.info("Policy {} cancelled successfully", id);
            return PolicyCancelResponseDTO.success(id);
            
        } catch (EntityNotFoundException e) {
            log.warn("Policy request not found with id: {}", id);
            return PolicyCancelResponseDTO.error(id, "Apólice não encontrada");
        } catch (Exception e) {
            log.error("Error cancelling policy request with id: {}", id, e);
            return PolicyCancelResponseDTO.error(id, "Erro interno durante cancelamento: " + e.getMessage());
        }
    }

    private boolean validateInsuranceAmount(InsuranceCategory category, BigDecimal amount, CustomerRiskType riskType) {
        return switch (riskType) {
            case REGULAR -> validateRegularCustomer(category, amount);
            case HIGH_RISK -> validateHighRiskCustomer(category, amount);
            case PREFERRED -> validatePreferredCustomer(category, amount);
            case NO_INFORMATION -> validateNoInformationCustomer(category, amount);
        };
    }
    
    private boolean validateRegularCustomer(InsuranceCategory category, BigDecimal amount) {
        return switch (category) {
            case LIFE, RESIDENTIAL -> amount.compareTo(new BigDecimal("500000.00")) <= 0;
            case AUTO -> amount.compareTo(new BigDecimal("350000.00")) <= 0;
            case TRAVEL, HEALTH -> amount.compareTo(new BigDecimal("255000.00")) <= 0;
        };
    }
    
    private boolean validateHighRiskCustomer(InsuranceCategory category, BigDecimal amount) {
        return switch (category) {
            case AUTO -> amount.compareTo(new BigDecimal("250000.00")) <= 0;
            case RESIDENTIAL -> amount.compareTo(new BigDecimal("150000.00")) <= 0;
            case LIFE, TRAVEL, HEALTH -> amount.compareTo(new BigDecimal("125000.00")) <= 0;
        };
    }
    
    private boolean validatePreferredCustomer(InsuranceCategory category, BigDecimal amount) {
        return switch (category) {
            case LIFE -> amount.compareTo(new BigDecimal("800000.00")) <= 0;
            case AUTO, RESIDENTIAL -> amount.compareTo(new BigDecimal("450000.00")) <= 0;
            case TRAVEL, HEALTH -> amount.compareTo(new BigDecimal("375000.00")) <= 0;
        };
    }
    
    private boolean validateNoInformationCustomer(InsuranceCategory category, BigDecimal amount) {
        return switch (category) {
            case LIFE, RESIDENTIAL -> amount.compareTo(new BigDecimal("200000.00")) <= 0;
            case AUTO -> amount.compareTo(new BigDecimal("75000.00")) <= 0;
            case TRAVEL, HEALTH -> amount.compareTo(new BigDecimal("55000.00")) <= 0;
        };
    }

    private String buildValidationFailureReason(InsuranceCategory category, BigDecimal amount, CustomerRiskType riskType) {
        BigDecimal maxAmount = getMaxAmountForCategoryAndRiskType(category, riskType);
        return String.format("Valor do seguro R$ %s excede o limite de R$ %s para categoria %s e tipo de cliente %s", 
                           amount, maxAmount, category.name(), riskType.name());
    }
    
    private BigDecimal getMaxAmountForCategoryAndRiskType(InsuranceCategory category, CustomerRiskType riskType) {
        return switch (riskType) {
            case REGULAR -> switch (category) {
                case LIFE, RESIDENTIAL -> new BigDecimal("500000.00");
                case AUTO -> new BigDecimal("350000.00");
                case TRAVEL, HEALTH -> new BigDecimal("255000.00");
            };
            case HIGH_RISK -> switch (category) {
                case AUTO -> new BigDecimal("250000.00");
                case RESIDENTIAL -> new BigDecimal("150000.00");
                case LIFE, TRAVEL, HEALTH -> new BigDecimal("125000.00");
            };
            case PREFERRED -> switch (category) {
                case LIFE -> new BigDecimal("800000.00");
                case AUTO, RESIDENTIAL -> new BigDecimal("450000.00");
                case TRAVEL, HEALTH -> new BigDecimal("375000.00");
            };
            case NO_INFORMATION -> switch (category) {
                case LIFE, RESIDENTIAL -> new BigDecimal("200000.00");
                case AUTO -> new BigDecimal("75000.00");
                case TRAVEL, HEALTH -> new BigDecimal("55000.00");
            };
        };
    }

    private boolean isValidStatusTransition(PolicyStatus currentStatus, PolicyStatus newStatus) {
        return switch (currentStatus) {
            case RECEIVED -> newStatus == PolicyStatus.VALIDATED || newStatus == PolicyStatus.REJECTED || newStatus == PolicyStatus.CANCELLED;
            case VALIDATED -> newStatus == PolicyStatus.PENDING || newStatus == PolicyStatus.REJECTED || newStatus == PolicyStatus.CANCELLED;
            case PENDING -> newStatus == PolicyStatus.APPROVED || newStatus == PolicyStatus.REJECTED || newStatus == PolicyStatus.CANCELLED;
            case APPROVED, REJECTED, CANCELLED -> false; // Estados finais
        };
    }
} 