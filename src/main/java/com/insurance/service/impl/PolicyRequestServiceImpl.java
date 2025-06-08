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
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Policy request not found with id: " + id));
    }

    @Override
    public List<PolicyRequest> findByCustomerId(UUID customerId) {
        return repository.findByCustomerId(customerId);
    }

    @Override
    @Transactional
    public PolicyRequest updateStatus(UUID id, PolicyStatus newStatus) {
        PolicyRequest request = findById(id);

        if (!request.canTransitionTo(newStatus)) {
            throw new IllegalStateException("Cannot transition from " + request.getStatus() + " to " + newStatus);
        }

        request.updateStatus(newStatus);
        request = repository.save(request);

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

        return request;
    }

    @Override
    @Transactional
    public PolicyValidationResponseDTO validatePolicyRequest(UUID id) {
        try {
            PolicyRequest request = findById(id);

            if (request.getRiskAnalysis() == null) {
                return PolicyValidationResponseDTO.error(id,
                    "Não é possível validar apólice sem análise de risco");
            }

            if (request.getStatus() == PolicyStatus.VALIDATED) {
                return PolicyValidationResponseDTO.success(id, PolicyStatus.VALIDATED);
            }

            if (!request.canTransitionTo(PolicyStatus.VALIDATED)) {
                return PolicyValidationResponseDTO.error(id,
                    "Não é possível validar apólice no status atual: " + request.getStatus());
            }

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

        } catch (EntityNotFoundException e) {
            log.warn("Policy request not found with id: {}", id);
            return PolicyValidationResponseDTO.error(id, "Apólice não encontrada");
        } catch (Exception e) {
            log.error("Error validating policy request with id: {}", id, e);
            return PolicyValidationResponseDTO.error(id, "Erro interno durante validação: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public FraudAnalysisResponseDTO processFraudAnalysis(UUID id) {
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
        PolicyRequest request = findById(id);

        if (request.getStatus() != PolicyStatus.VALIDATED) {
            throw new IllegalStateException("Cannot process payment for non-validated policy request");
        }

        boolean success = paymentService.processPayment(request);

        if (success) {
            request.setStatus(PolicyStatus.PENDING);
            repository.save(request);
            eventPublisher.publish(
                RabbitMQConfig.POLICY_EVENTS_EXCHANGE,
                RabbitMQConfig.PAYMENT_PROCESSED_KEY,
                new PaymentProcessedEvent(request)
            );
        } else {
            request.setStatus(PolicyStatus.REJECTED);
            repository.save(request);
            eventPublisher.publish(
                RabbitMQConfig.POLICY_EVENTS_EXCHANGE,
                RabbitMQConfig.PAYMENT_REJECTED_KEY,
                new PaymentRejectedEvent(request)
            );
        }
    }

    @Override
    @Transactional
    public void processSubscription(UUID id) {
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
        try {
            PolicyRequest request = findById(id);

            if (request.getStatus() == PolicyStatus.CANCELLED) {
                return PolicyCancelResponseDTO.success(id);
            }

            if (request.getStatus() == PolicyStatus.APPROVED) {
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
} 