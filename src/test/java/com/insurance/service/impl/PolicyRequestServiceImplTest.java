package com.insurance.service.impl;

import com.insurance.domain.PolicyRequest;
import com.insurance.domain.RiskAnalysis;
import com.insurance.domain.RiskOccurrence;
import com.insurance.domain.enums.*;
import com.insurance.dto.FraudAnalysisResponseDTO;
import com.insurance.dto.PolicyCancelResponseDTO;
import com.insurance.dto.PolicyValidationResponseDTO;
import com.insurance.event.PolicyRequestCreatedEvent;
import com.insurance.event.PolicyRequestEvent;
import com.insurance.exception.BusinessException;
import com.insurance.infrastructure.messaging.config.RabbitMQConfig;
import com.insurance.infrastructure.messaging.service.EventPublisher;
import com.insurance.repository.PolicyRequestRepository;
import com.insurance.service.FraudAnalysisService;
import com.insurance.service.PaymentService;
import com.insurance.service.SubscriptionService;
import com.insurance.service.PolicyStatusService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PolicyRequestServiceImplTest {

    @Mock
    private PolicyRequestRepository repository;

    @Mock
    private FraudAnalysisService fraudAnalysisService;

    @Mock
    private PaymentService paymentService;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private PolicyStatusService policyStatusService;

    @Spy
    @InjectMocks
    private PolicyRequestServiceImpl policyRequestService;

    @Captor
    private ArgumentCaptor<PolicyRequestEvent> eventCaptor;

    private PolicyRequest policyRequest;
    private UUID requestId;
    private UUID customerId;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        requestId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        now = LocalDateTime.now();

        policyRequest = new PolicyRequest();
        policyRequest.setId(requestId);
        policyRequest.setCustomerId(customerId);
        policyRequest.setProductId(UUID.randomUUID());
        policyRequest.setCategory(InsuranceCategory.AUTO);
        policyRequest.setSalesChannel(SalesChannel.MOBILE);
        policyRequest.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        policyRequest.setStatus(PolicyStatus.RECEIVED);
        policyRequest.setCreatedAt(now);
        policyRequest.setUpdatedAt(now);
        policyRequest.setCreatedBy("system");
        policyRequest.setUpdatedBy("system");
        policyRequest.setTotalMonthlyPremiumAmount(BigDecimal.valueOf(150.00));
        policyRequest.setInsuredAmount(BigDecimal.valueOf(50000.00));
        
        policyRequest.getCoverages().put("Collision", new BigDecimal("30000.00"));
        policyRequest.getCoverages().put("Theft", new BigDecimal("20000.00"));
        
        lenient().when(policyStatusService.canTransitionTo(any(PolicyStatus.class), any(PolicyStatus.class)))
                .thenReturn(true);
        lenient().doAnswer(invocation -> {
            PolicyRequest request = invocation.getArgument(0);
            PolicyStatus newStatus = invocation.getArgument(1);
            request.setStatus(newStatus);
            return null;
        }).when(policyStatusService).updatePolicyStatus(any(PolicyRequest.class), any(PolicyStatus.class));
    }

    @Test
    void testCreatePolicyRequest() {

        when(repository.save(any(PolicyRequest.class))).thenReturn(policyRequest);

        PolicyRequest result = policyRequestService.createPolicyRequest(policyRequest);

        assertNotNull(result);
        assertEquals(PolicyStatus.RECEIVED, result.getStatus());
        verify(repository).save(policyRequest);
        verify(eventPublisher).publish(
            eq(RabbitMQConfig.POLICY_EVENTS_EXCHANGE),
            eq(RabbitMQConfig.POLICY_CREATED_KEY),
            any(PolicyRequestCreatedEvent.class)
        );
    }

    @Test
    void testFindById() {
        when(repository.findById(requestId)).thenReturn(Optional.of(policyRequest));

        PolicyRequest result = policyRequestService.findById(requestId);

        assertNotNull(result);
        assertEquals(requestId, result.getId());
    }

    @Test
    void testFindByIdNotFound() {
        when(repository.findById(requestId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> 
            policyRequestService.findById(requestId)
        );
    }

    @Test
    void testFindByCustomerId() {
        List<PolicyRequest> requests = Arrays.asList(policyRequest);
        when(repository.findByCustomerId(customerId)).thenReturn(requests);

        List<PolicyRequest> result = policyRequestService.findByCustomerId(customerId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(customerId, result.get(0).getCustomerId());
    }

    @Test
    void testUpdateStatusValidTransition() {
        policyRequest.setStatus(PolicyStatus.RECEIVED);
        when(repository.findById(requestId)).thenReturn(Optional.of(policyRequest));
        when(repository.save(any(PolicyRequest.class))).thenReturn(policyRequest);

        PolicyRequest result = policyRequestService.updateStatus(requestId, PolicyStatus.VALIDATED);

        assertEquals(PolicyStatus.VALIDATED, result.getStatus());
        verify(eventPublisher).publish(
            eq(RabbitMQConfig.POLICY_EVENTS_EXCHANGE),
            eq(RabbitMQConfig.POLICY_VALIDATED_KEY),
            eventCaptor.capture()
        );
    }

    @Test
    void testUpdateStatusInvalidTransition() {
        policyRequest.setStatus(PolicyStatus.APPROVED);
        when(repository.findById(requestId)).thenReturn(Optional.of(policyRequest));
        
        doThrow(new IllegalStateException("Cannot transition from APPROVED to VALIDATED"))
                .when(policyStatusService).updatePolicyStatus(any(PolicyRequest.class), eq(PolicyStatus.VALIDATED));

        assertThrows(IllegalStateException.class, () ->
            policyRequestService.updateStatus(requestId, PolicyStatus.VALIDATED)
        );
    }

    @Test
    void testProcessFraudAnalysisSuccess() {
        RiskAnalysis riskAnalysis = new RiskAnalysis();
        riskAnalysis.setClassification(CustomerRiskType.REGULAR);
        riskAnalysis.setAnalyzedAt(LocalDateTime.now());
        riskAnalysis.setOccurrences(new ArrayList<>());
        
        when(repository.findById(requestId)).thenReturn(Optional.of(policyRequest));
        when(fraudAnalysisService.analyzeFraud(policyRequest)).thenReturn(riskAnalysis);
        when(repository.save(any(PolicyRequest.class))).thenReturn(policyRequest);

        FraudAnalysisResponseDTO result = policyRequestService.processFraudAnalysis(requestId);

        assertNotNull(result);
        assertEquals(requestId, result.getOrderId());
        assertEquals(customerId, result.getCustomerId());
        assertEquals(CustomerRiskType.REGULAR, result.getClassification());
        assertNotNull(result.getAnalyzedAt());
        assertNotNull(result.getOccurrences());
        assertTrue(result.getOccurrences().isEmpty());
        
        verify(fraudAnalysisService).analyzeFraud(policyRequest);
        verify(repository, times(1)).save(any(PolicyRequest.class));
    }

    @Test
    void testProcessFraudAnalysisFailure() {
        when(repository.findById(requestId)).thenReturn(Optional.of(policyRequest));
        when(fraudAnalysisService.analyzeFraud(policyRequest))
            .thenThrow(new RuntimeException("Analysis failed"));
        when(repository.save(any(PolicyRequest.class))).thenReturn(policyRequest);

        assertThrows(RuntimeException.class, () -> 
            policyRequestService.processFraudAnalysis(requestId)
        );

        verify(fraudAnalysisService).analyzeFraud(policyRequest);
        assertEquals(PolicyStatus.REJECTED, policyRequest.getStatus());
        verify(eventPublisher).publish(
            eq(RabbitMQConfig.POLICY_EVENTS_EXCHANGE),
            eq(RabbitMQConfig.POLICY_REJECTED_KEY),
            any()
        );
    }

    @Test
    void testProcessPaymentSuccess() {
        policyRequest.setStatus(PolicyStatus.VALIDATED);
        policyRequest.setPaymentMethod(PaymentMethod.CREDIT_CARD);

        when(repository.findById(requestId)).thenReturn(Optional.of(policyRequest));
        when(repository.save(any(PolicyRequest.class))).thenAnswer(invocation -> {
            PolicyRequest savedRequest = invocation.getArgument(0);
            return savedRequest;
        });
        when(paymentService.processPayment(policyRequest)).thenReturn(true);

        policyRequestService.processPayment(requestId);

        verify(paymentService).processPayment(policyRequest);
        verify(repository).save(any(PolicyRequest.class));
        verify(eventPublisher).publish(
            eq(RabbitMQConfig.POLICY_EVENTS_EXCHANGE),
            eq(RabbitMQConfig.PAYMENT_PROCESSED_KEY),
            eventCaptor.capture()
        );

        PolicyRequestEvent event = eventCaptor.getValue();
        assertEquals(requestId, event.getPolicyRequestId());
        assertEquals(customerId, event.getCustomerId());
        assertEquals(PolicyStatus.PENDING, event.getStatus());
        assertEquals(PolicyStatus.PENDING, policyRequest.getStatus());
    }

    @Test
    void testProcessPaymentFailure() {
        policyRequest.setStatus(PolicyStatus.VALIDATED);
        policyRequest.setPaymentMethod(PaymentMethod.CREDIT_CARD);

        when(repository.findById(requestId)).thenReturn(Optional.of(policyRequest));
        when(repository.save(any(PolicyRequest.class))).thenAnswer(invocation -> {
            PolicyRequest savedRequest = invocation.getArgument(0);
            return savedRequest;
        });
        when(paymentService.processPayment(policyRequest)).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class, () ->
            policyRequestService.processPayment(requestId)
        );

        assertEquals("Payment processing failed", exception.getMessage());
        assertEquals("PAYMENT_FAILED", exception.getErrorCode());
        verify(paymentService).processPayment(policyRequest);
        verify(repository).save(any(PolicyRequest.class));
        assertEquals(PolicyStatus.REJECTED, policyRequest.getStatus());
    }

    @Test
    void testProcessSubscriptionSuccess() {
        policyRequest.setStatus(PolicyStatus.PENDING);
        when(repository.findById(requestId)).thenReturn(Optional.of(policyRequest));
        when(repository.save(any(PolicyRequest.class))).thenReturn(policyRequest);

        policyRequestService.processSubscription(requestId);

        verify(subscriptionService).processSubscription(policyRequest);
        verify(repository).save(any(PolicyRequest.class));
    }

    @Test
    void testProcessSubscriptionFailure() {
        policyRequest.setStatus(PolicyStatus.PENDING);
        when(repository.findById(requestId)).thenReturn(Optional.of(policyRequest));
        when(repository.save(any(PolicyRequest.class))).thenReturn(policyRequest);
        doThrow(new RuntimeException("Subscription failed"))
            .when(subscriptionService).processSubscription(policyRequest);

        BusinessException exception = assertThrows(BusinessException.class, () ->
            policyRequestService.processSubscription(requestId)
        );

        assertEquals("Error during subscription processing", exception.getMessage());
        assertEquals("SUBSCRIPTION_ERROR", exception.getErrorCode());
        verify(subscriptionService).processSubscription(policyRequest);
        verify(repository).save(any(PolicyRequest.class));
        assertEquals(PolicyStatus.REJECTED, policyRequest.getStatus());
    }

    @Test
    void testCancelPolicyRequest() {
        policyRequest.setStatus(PolicyStatus.VALIDATED);
        when(repository.findById(requestId)).thenReturn(Optional.of(policyRequest));
        when(repository.save(any(PolicyRequest.class))).thenReturn(policyRequest);

        PolicyCancelResponseDTO result = policyRequestService.cancelPolicyRequest(requestId);

        assertTrue(result.isCancelled());
        assertEquals(PolicyStatus.CANCELLED, result.getStatus());
        assertEquals("Apólice cancelada com sucesso", result.getMessage());
        assertEquals(PolicyStatus.CANCELLED, policyRequest.getStatus());
        verify(eventPublisher).publish(
            eq(RabbitMQConfig.POLICY_EVENTS_EXCHANGE),
            eq("policy.cancelled"),
            any(PolicyRequestEvent.class)
        );
    }

    @Test
    void testCancelApprovedPolicyRequest() {
        policyRequest.setStatus(PolicyStatus.APPROVED);
        when(repository.findById(requestId)).thenReturn(Optional.of(policyRequest));

        BusinessException exception = assertThrows(BusinessException.class, () ->
            policyRequestService.cancelPolicyRequest(requestId)
        );

        assertEquals("Cannot cancel approved policy", exception.getMessage());
        assertEquals("CANNOT_CANCEL_APPROVED", exception.getErrorCode());
        verify(repository, never()).save(any(PolicyRequest.class));
        verify(eventPublisher, never()).publish(any(), any(), any());
    }

    @Test
    void testValidatePolicyRequestWithoutRiskAnalysis() {

        policyRequest.setStatus(PolicyStatus.RECEIVED);
        when(repository.findById(requestId)).thenReturn(Optional.of(policyRequest));

        BusinessException exception = assertThrows(BusinessException.class, () ->
            policyRequestService.validatePolicyRequest(requestId)
        );
        
        assertEquals("Cannot validate policy without risk analysis", exception.getMessage());
        assertEquals("MISSING_RISK_ANALYSIS", exception.getErrorCode());
    }

    @Test
    void testValidatePolicyRequestWithValidRiskAnalysis() {

        RiskAnalysis riskAnalysis = new RiskAnalysis();
        riskAnalysis.setClassification(CustomerRiskType.REGULAR);
        policyRequest.setRiskAnalysis(riskAnalysis);
        policyRequest.setStatus(PolicyStatus.RECEIVED);
        
        when(repository.findById(requestId)).thenReturn(Optional.of(policyRequest));
        when(repository.save(any(PolicyRequest.class))).thenReturn(policyRequest);
        
        when(policyStatusService.canTransitionTo(PolicyStatus.RECEIVED, PolicyStatus.VALIDATED))
                .thenReturn(true);

        PolicyValidationResponseDTO result = policyRequestService.validatePolicyRequest(requestId);

        assertTrue(result.isValidated());
        assertEquals(PolicyStatus.VALIDATED, result.getStatus());
        assertEquals("Apólice validada com sucesso", result.getMessage());
        verify(repository).save(any(PolicyRequest.class));
    }

    @Test
    void testValidatePolicyRequestWithHighRiskAnalysis() {

        RiskAnalysis riskAnalysis = new RiskAnalysis();
        riskAnalysis.setClassification(CustomerRiskType.HIGH_RISK);
        policyRequest.setRiskAnalysis(riskAnalysis);
        policyRequest.setInsuredAmount(BigDecimal.valueOf(500000.00));
        policyRequest.setStatus(PolicyStatus.RECEIVED);
        
        when(repository.findById(requestId)).thenReturn(Optional.of(policyRequest));
        when(repository.save(any(PolicyRequest.class))).thenReturn(policyRequest);

        PolicyValidationResponseDTO result = policyRequestService.validatePolicyRequest(requestId);

        assertFalse(result.isValidated());
        assertEquals(PolicyStatus.REJECTED, result.getStatus());
        assertTrue(result.getReason().contains("excede o limite"));
        verify(repository).save(any(PolicyRequest.class));
    }

    @Test
    void testValidateRegularCustomerWithDifferentCategories() {

        RiskAnalysis riskAnalysis = new RiskAnalysis();
        riskAnalysis.setClassification(CustomerRiskType.REGULAR);
        policyRequest.setRiskAnalysis(riskAnalysis);
        policyRequest.setStatus(PolicyStatus.RECEIVED);
        
        when(repository.findById(requestId)).thenReturn(Optional.of(policyRequest));
        when(repository.save(any(PolicyRequest.class))).thenReturn(policyRequest);
        
        when(policyStatusService.canTransitionTo(eq(PolicyStatus.RECEIVED), any(PolicyStatus.class)))
                .thenReturn(true);

        int validatedCount = 0;
        for (InsuranceCategory category : InsuranceCategory.values()) {
            policyRequest.setCategory(category);
            policyRequest.setInsuredAmount(new BigDecimal("200000.00"));
            policyRequest.setStatus(PolicyStatus.RECEIVED);

            PolicyValidationResponseDTO result = policyRequestService.validatePolicyRequest(requestId);
            if (result.isValidated()) {
                validatedCount++;
            }
        }

        verify(repository, times(InsuranceCategory.values().length)).save(policyRequest);
        assertTrue(validatedCount > 0, "At least some categories should be validated for REGULAR customer");
    }

    @Test
    void testValidateHighRiskCustomerWithDifferentCategories() {
        RiskAnalysis riskAnalysis = new RiskAnalysis();
        riskAnalysis.setClassification(CustomerRiskType.HIGH_RISK);
        policyRequest.setRiskAnalysis(riskAnalysis);
        policyRequest.setStatus(PolicyStatus.RECEIVED);
        
        when(repository.findById(requestId)).thenReturn(Optional.of(policyRequest));
        when(repository.save(any(PolicyRequest.class))).thenReturn(policyRequest);
        
        when(policyStatusService.canTransitionTo(eq(PolicyStatus.RECEIVED), any(PolicyStatus.class)))
                .thenReturn(true);

        int rejectedCount = 0;
        for (InsuranceCategory category : InsuranceCategory.values()) {
            policyRequest.setCategory(category);
            policyRequest.setInsuredAmount(new BigDecimal("1000000.00"));
            policyRequest.setStatus(PolicyStatus.RECEIVED);

            PolicyValidationResponseDTO result = policyRequestService.validatePolicyRequest(requestId);
            if (!result.isValidated()) {
                rejectedCount++;
            }
        }

        verify(repository, times(InsuranceCategory.values().length)).save(policyRequest);
        assertEquals(InsuranceCategory.values().length, rejectedCount, "All high amounts should be rejected for HIGH_RISK customer");
    }

    @Test
    void testValidatePreferredCustomerWithDifferentCategories() {
        RiskAnalysis riskAnalysis = new RiskAnalysis();
        riskAnalysis.setClassification(CustomerRiskType.PREFERRED);
        policyRequest.setRiskAnalysis(riskAnalysis);
        policyRequest.setStatus(PolicyStatus.RECEIVED);
        
        when(repository.findById(requestId)).thenReturn(Optional.of(policyRequest));
        when(repository.save(any(PolicyRequest.class))).thenReturn(policyRequest);
        
        when(policyStatusService.canTransitionTo(eq(PolicyStatus.RECEIVED), any(PolicyStatus.class)))
                .thenReturn(true);

        int validatedCount = 0;
        for (InsuranceCategory category : InsuranceCategory.values()) {
            policyRequest.setCategory(category);
            policyRequest.setInsuredAmount(new BigDecimal("300000.00"));
            policyRequest.setStatus(PolicyStatus.RECEIVED);

            PolicyValidationResponseDTO result = policyRequestService.validatePolicyRequest(requestId);
            if (result.isValidated()) {
                validatedCount++;
            }
        }

        verify(repository, times(InsuranceCategory.values().length)).save(policyRequest);
        assertTrue(validatedCount > 0, "Moderate amounts should be validated for PREFERRED customer");
    }

    @Test
    void testValidateNoInformationCustomerWithDifferentCategories() {
        RiskAnalysis riskAnalysis = new RiskAnalysis();
        riskAnalysis.setClassification(CustomerRiskType.NO_INFORMATION);
        
        policyRequest.setCategory(InsuranceCategory.LIFE);
        policyRequest.setInsuredAmount(new BigDecimal("100000.00"));
        policyRequest.setRiskAnalysis(riskAnalysis);
        policyRequest.setStatus(PolicyStatus.RECEIVED);
        
        when(repository.findById(requestId)).thenReturn(Optional.of(policyRequest));
        when(repository.save(any(PolicyRequest.class))).thenReturn(policyRequest);
        
        when(policyStatusService.canTransitionTo(eq(PolicyStatus.RECEIVED), any(PolicyStatus.class)))
                .thenReturn(true);

        PolicyValidationResponseDTO result1 = policyRequestService.validatePolicyRequest(requestId);
        assertTrue(result1.isValidated(), "100K should be valid for LIFE with NO_INFORMATION");

        policyRequest.setCategory(InsuranceCategory.TRAVEL);
        policyRequest.setInsuredAmount(new BigDecimal("75000.00"));
        policyRequest.setStatus(PolicyStatus.RECEIVED);
        
        PolicyValidationResponseDTO result2 = policyRequestService.validatePolicyRequest(requestId);
        assertFalse(result2.isValidated(), "75K should exceed limit for TRAVEL with NO_INFORMATION");
    }

    @Test
    void testStatusTransitionValidations() {
        when(repository.findById(requestId)).thenReturn(Optional.of(policyRequest));
        when(repository.save(any(PolicyRequest.class))).thenReturn(policyRequest);

        policyRequest.setStatus(PolicyStatus.RECEIVED);
        policyRequestService.updateStatus(requestId, PolicyStatus.VALIDATED);
        assertEquals(PolicyStatus.VALIDATED, policyRequest.getStatus());

        policyRequest.setStatus(PolicyStatus.VALIDATED);
        policyRequestService.updateStatus(requestId, PolicyStatus.PENDING);
        assertEquals(PolicyStatus.PENDING, policyRequest.getStatus());

        policyRequest.setStatus(PolicyStatus.PENDING);
        policyRequestService.updateStatus(requestId, PolicyStatus.APPROVED);
        assertEquals(PolicyStatus.APPROVED, policyRequest.getStatus());

        reset(policyStatusService);
        lenient().when(policyStatusService.canTransitionTo(any(PolicyStatus.class), any(PolicyStatus.class)))
                .thenReturn(false);
        lenient().doThrow(new IllegalStateException("Invalid transition"))
                .when(policyStatusService).updatePolicyStatus(any(PolicyRequest.class), any(PolicyStatus.class));

        policyRequest.setStatus(PolicyStatus.REJECTED);
        assertThrows(IllegalStateException.class, () ->
            policyRequestService.updateStatus(requestId, PolicyStatus.VALIDATED)
        );

        policyRequest.setStatus(PolicyStatus.APPROVED);
        assertThrows(IllegalStateException.class, () ->
            policyRequestService.updateStatus(requestId, PolicyStatus.PENDING)
        );

        policyRequest.setStatus(PolicyStatus.CANCELLED);
        assertThrows(IllegalStateException.class, () ->
            policyRequestService.updateStatus(requestId, PolicyStatus.VALIDATED)
        );
    }

    @Test
    void testProcessFraudAnalysisWithOccurrences() {

        RiskAnalysis riskAnalysis = new RiskAnalysis();
        riskAnalysis.setClassification(CustomerRiskType.HIGH_RISK);
        riskAnalysis.setAnalyzedAt(LocalDateTime.now());
        
        RiskOccurrence occurrence = new RiskOccurrence();
        occurrence.setType("FRAUD");
        occurrence.setDescription("Atividade suspeita detectada");
        occurrence.setCreatedAt(LocalDateTime.now().minusDays(1));
        occurrence.setUpdatedAt(LocalDateTime.now());
        
        riskAnalysis.setOccurrences(Arrays.asList(occurrence));
        
        when(repository.findById(requestId)).thenReturn(Optional.of(policyRequest));
        when(fraudAnalysisService.analyzeFraud(policyRequest)).thenReturn(riskAnalysis);
        when(repository.save(any(PolicyRequest.class))).thenReturn(policyRequest);

        FraudAnalysisResponseDTO result = policyRequestService.processFraudAnalysis(requestId);

        assertNotNull(result);
        assertEquals(requestId, result.getOrderId());
        assertEquals(customerId, result.getCustomerId());
        assertEquals(CustomerRiskType.HIGH_RISK, result.getClassification());
        assertNotNull(result.getOccurrences());
        assertEquals(1, result.getOccurrences().size());
        
        FraudAnalysisResponseDTO.RiskOccurrenceDTO occurrenceDTO = result.getOccurrences().get(0);
        assertEquals("FRAUD", occurrenceDTO.getType());
        assertEquals("Atividade suspeita detectada", occurrenceDTO.getDescription());
        assertEquals(12345L, occurrenceDTO.getProductId());
        assertNotNull(occurrenceDTO.getId());
        
        verify(fraudAnalysisService).analyzeFraud(policyRequest);
    }
} 