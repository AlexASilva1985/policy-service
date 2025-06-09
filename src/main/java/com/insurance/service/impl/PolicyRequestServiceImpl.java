package com.insurance.service.impl;

import com.insurance.domain.PolicyRequest;
import com.insurance.domain.RiskAnalysis;
import com.insurance.domain.RiskOccurrence;
import com.insurance.domain.enums.CustomerRiskType;
import com.insurance.domain.enums.InsuranceCategory;
import com.insurance.domain.enums.PolicyStatus;
import com.insurance.dto.FraudAnalysisResponseDTO;
import com.insurance.dto.PolicyCancelResponseDTO;
import com.insurance.dto.PolicyValidationResponseDTO;
import com.insurance.event.*;
import com.insurance.exception.BusinessException;
import com.insurance.infrastructure.messaging.config.RabbitMQConfig;
import com.insurance.infrastructure.messaging.service.EventPublisher;
import com.insurance.repository.PolicyRequestRepository;
import com.insurance.service.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PolicyRequestServiceImpl implements PolicyRequestService {

    private final PolicyRequestRepository repository;
    private final FraudAnalysisService fraudAnalysisService;
    private final PaymentService paymentService;
    private final SubscriptionService subscriptionService;
    private final EventPublisher eventPublisher;
    private final PolicyStatusService policyStatusService;

    @Override
    @Transactional
    public PolicyRequest createPolicyRequest(PolicyRequest request) {

        // Validações de negócio
        validatePolicyRequestInput(request);
        
        request.setStatus(PolicyStatus.RECEIVED);
        request = repository.save(request);

        eventPublisher.publish(
            RabbitMQConfig.POLICY_EVENTS_EXCHANGE,
            RabbitMQConfig.POLICY_CREATED_KEY,
            new PolicyRequestCreatedEvent(request)
        );

        return request;
    }

    @Override
    public PolicyRequest findById(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("Policy ID cannot be null");
        }
        
        return repository.findById(id)
                .orElseThrow(() -> {
                    return new EntityNotFoundException("Policy request not found with id: " + id);
                });
    }

    @Override
    public List<PolicyRequest> findByCustomerId(UUID customerId) {
        if (customerId == null) {
            throw new IllegalArgumentException("Customer ID cannot be null");
        }
        
        List<PolicyRequest> requests = repository.findByCustomerId(customerId);

        return requests;
    }

    @Override
    @Transactional
    public PolicyRequest updateStatus(UUID id, PolicyStatus newStatus) {
        if (id == null) {
            throw new IllegalArgumentException("Policy ID cannot be null");
        }
        
        if (newStatus == null) {
            throw new IllegalArgumentException("New status cannot be null");
        }
        
        PolicyRequest request = findById(id);

        // Usar o PolicyStatusService para validar e atualizar
        PolicyStatus previousStatus = request.getStatus();
        policyStatusService.updatePolicyStatus(request, newStatus);
        request = repository.save(request);

        // Publicar evento apropriado
        publishStatusChangeEvent(request, previousStatus, newStatus);

        return request;
    }

    @Override
    @Transactional
    public PolicyValidationResponseDTO validatePolicyRequest(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("Policy ID cannot be null");
        }
        
        try {
            PolicyRequest request = findById(id);

            // Verificar se análise de risco foi realizada
            if (request.getRiskAnalysis() == null) {
                String message = "Cannot validate policy without risk analysis";
                throw new BusinessException(message, "MISSING_RISK_ANALYSIS");
            }

            // Verificar se já está validada
            if (request.getStatus() == PolicyStatus.VALIDATED) {
                return PolicyValidationResponseDTO.success(id, PolicyStatus.VALIDATED);
            }

            // Verificar se pode ser validada usando PolicyStatusService
            if (!policyStatusService.canTransitionTo(request.getStatus(), PolicyStatus.VALIDATED)) {
                String message = String.format("Cannot validate policy in current status: %s", request.getStatus());
                throw new BusinessException(message, "INVALID_STATUS_FOR_VALIDATION");
            }

            // Realizar validação de negócio
            CustomerRiskType riskType = request.getRiskAnalysis().getClassification();
            boolean isValid = validateInsuranceAmount(request.getCategory(),
                                                    request.getInsuredAmount(),
                                                    riskType);

            if (isValid) {
                PolicyStatus newStatus = PolicyStatus.VALIDATED;
                updateStatus(id, newStatus);
                return PolicyValidationResponseDTO.success(id, newStatus);
            } else {
                PolicyStatus newStatus = PolicyStatus.REJECTED;
                updateStatus(id, newStatus);

                String reason = buildValidationFailureReason(request.getCategory(),
                                                           request.getInsuredAmount(),
                                                           riskType);
                return PolicyValidationResponseDTO.failure(id, reason);
            }

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("Unexpected error during validation", "VALIDATION_ERROR", e);
        }
    }

    @Override
    @Transactional
    public FraudAnalysisResponseDTO processFraudAnalysis(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("Policy ID cannot be null");
        }
        
        PolicyRequest request = findById(id);
        
        // Verificar se já possui análise de risco
        if (request.getRiskAnalysis() != null) {
            throw new BusinessException("Policy already has risk analysis", "DUPLICATE_RISK_ANALYSIS");
        }
        
        // Verificar status da política
        if (request.getStatus() != PolicyStatus.RECEIVED) {
            throw new BusinessException("Cannot process fraud analysis in current status", "INVALID_STATUS_FOR_ANALYSIS");
        }

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

            return response;

        } catch (Exception e) {
            // Rejeitar a política em caso de erro na análise
                request.setStatus(PolicyStatus.REJECTED);
                repository.save(request);
                eventPublisher.publish(
                    RabbitMQConfig.POLICY_EVENTS_EXCHANGE,
                    RabbitMQConfig.POLICY_REJECTED_KEY,
                    new PolicyRejectedEvent(request)
                );
            throw new BusinessException("Error during fraud analysis", "FRAUD_ANALYSIS_ERROR", e);
        }
    }

    @Override
    @Transactional
    public void processPayment(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("Policy ID cannot be null");
        }
        
        PolicyRequest request = findById(id);
        
        // Verificar se a política está no status correto para pagamento
        if (request.getStatus() != PolicyStatus.VALIDATED) {
            String message = String.format("Cannot process payment for policy in status: %s", request.getStatus());
            throw new BusinessException(message, "INVALID_STATUS_FOR_PAYMENT");
        }

        try {
            boolean paymentSuccessful = paymentService.processPayment(request);
            
            if (paymentSuccessful) {
                updateStatus(id, PolicyStatus.PENDING);
            } else {
                updateStatus(id, PolicyStatus.REJECTED);
                throw new BusinessException("Payment processing failed", "PAYMENT_FAILED");
            }
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            updateStatus(id, PolicyStatus.REJECTED);
            throw new BusinessException("Error during payment processing", "PAYMENT_ERROR", e);
        }
    }

    @Override
    @Transactional
    public void processSubscription(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("Policy ID cannot be null");
        }
        
        PolicyRequest request = findById(id);
        
        if (request.getStatus() != PolicyStatus.PENDING) {
            String message = String.format("Cannot process subscription for policy in status: %s", request.getStatus());
            throw new BusinessException(message, "INVALID_STATUS_FOR_SUBSCRIPTION");
        }

        try {
            subscriptionService.processSubscription(request);
            updateStatus(id, PolicyStatus.APPROVED);

        } catch (Exception e) {
            updateStatus(id, PolicyStatus.REJECTED);
            throw new BusinessException("Error during subscription processing", "SUBSCRIPTION_ERROR", e);
        }
    }

    @Override
    @Transactional
    public PolicyCancelResponseDTO cancelPolicyRequest(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("Policy ID cannot be null");
        }
        
        PolicyRequest request = findById(id);
        
        if (request.getStatus() == PolicyStatus.CANCELLED) {
            throw new BusinessException("Policy is already cancelled", "ALREADY_CANCELLED");
        }
        
        if (request.getStatus() == PolicyStatus.APPROVED) {
            throw new BusinessException("Cannot cancel approved policy", "CANNOT_CANCEL_APPROVED");
        }

        try {
            PolicyStatus previousStatus = request.getStatus();
            updateStatus(id, PolicyStatus.CANCELLED);
            
            return PolicyCancelResponseDTO.success(id);
            
        } catch (Exception e) {
            throw new BusinessException("Error during policy cancellation", "CANCELLATION_ERROR", e);
        }
    }

    private void validatePolicyRequestInput(PolicyRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Policy request cannot be null");
        }
        
        if (request.getCustomerId() == null) {
            throw new IllegalArgumentException("Customer ID is required");
        }
        
        if (request.getProductId() == null) {
            throw new IllegalArgumentException("Product ID is required");
        }
        
        if (request.getCategory() == null) {
            throw new IllegalArgumentException("Insurance category is required");
        }
        
        if (request.getSalesChannel() == null) {
            throw new IllegalArgumentException("Sales channel is required");
        }
        
        if (request.getPaymentMethod() == null) {
            throw new IllegalArgumentException("Payment method is required");
        }
        
        if (request.getTotalMonthlyPremiumAmount() == null || 
            request.getTotalMonthlyPremiumAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Total monthly premium amount must be greater than zero");
        }
        
        if (request.getInsuredAmount() == null || 
            request.getInsuredAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Insured amount must be greater than zero");
        }
        
        if (request.getCoverages() == null || request.getCoverages().isEmpty()) {
            throw new IllegalArgumentException("At least one coverage is required");
        }
    }
    
    private void publishStatusChangeEvent(PolicyRequest request, PolicyStatus previousStatus, PolicyStatus newStatus) {
        PolicyRequestEvent event = switch (newStatus) {
            case VALIDATED -> new PolicyValidatedEvent(request);
            case REJECTED -> new PolicyRejectedEvent(request);
            case APPROVED -> new SubscriptionApprovedEvent(request);
            case CANCELLED -> new PolicyCancelledEvent(request);
            case PENDING -> new PaymentProcessedEvent(request);
            default -> new PolicyStatusChangedEvent(request);
        };

        String routingKey = switch (newStatus) {
            case VALIDATED -> RabbitMQConfig.POLICY_VALIDATED_KEY;
            case REJECTED -> RabbitMQConfig.POLICY_REJECTED_KEY;
            case APPROVED -> RabbitMQConfig.POLICY_APPROVED_KEY;
            case CANCELLED -> "policy.cancelled";
            case PENDING -> RabbitMQConfig.PAYMENT_PROCESSED_KEY;
            default -> "policy.status.changed";
        };

        eventPublisher.publish(
            RabbitMQConfig.POLICY_EVENTS_EXCHANGE,
            routingKey,
            event
        );
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
} 