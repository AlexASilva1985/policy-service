package com.insurance.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.mock;

import com.insurance.domain.PolicyRequest;
import com.insurance.domain.enums.InsuranceCategory;
import com.insurance.domain.enums.PaymentMethod;
import com.insurance.domain.enums.PolicyStatus;
import com.insurance.domain.enums.SalesChannel;
import com.insurance.event.PolicyRequestEvent;
import com.insurance.infrastructure.messaging.config.RabbitMQConfig;
import com.insurance.infrastructure.messaging.service.EventPublisher;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Captor
    private ArgumentCaptor<PolicyRequestEvent> eventCaptor;

    private PolicyRequest policyRequest;
    private UUID requestId;
    private UUID customerId;

    @BeforeEach
    void setUp() {
        requestId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        policyRequest = new PolicyRequest();
        policyRequest.setId(requestId);
        policyRequest.setCustomerId(customerId);
        policyRequest.setProductId(UUID.randomUUID());
        policyRequest.setCategory(InsuranceCategory.AUTO);
        policyRequest.setSalesChannel(SalesChannel.MOBILE);
        policyRequest.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        policyRequest.setStatus(PolicyStatus.VALIDATED);
        policyRequest.setCreatedAt(now);
        policyRequest.setUpdatedAt(now);
        policyRequest.setCreatedBy("system");
        policyRequest.setUpdatedBy("system");
        policyRequest.setTotalMonthlyPremiumAmount(BigDecimal.valueOf(150.00));
        policyRequest.setInsuredAmount(BigDecimal.valueOf(50000.00));
    }

    @Test
    void testProcessPayment() {
        paymentService.processPayment(policyRequest);

        verify(eventPublisher).publish(
            eq(RabbitMQConfig.POLICY_EVENTS_EXCHANGE),
            eq(RabbitMQConfig.PAYMENT_REQUESTED_KEY),
            eventCaptor.capture()
        );

        PolicyRequestEvent capturedEvent = eventCaptor.getValue();
        assertEquals(requestId, capturedEvent.getPolicyRequestId());
        assertEquals(customerId, capturedEvent.getCustomerId());
        assertEquals(PolicyStatus.VALIDATED, capturedEvent.getStatus());
    }

    @Test
    void testProcessPaymentWithDifferentStatus() {
        policyRequest.setStatus(PolicyStatus.RECEIVED);
        
        paymentService.processPayment(policyRequest);

        verify(eventPublisher).publish(
            eq(RabbitMQConfig.POLICY_EVENTS_EXCHANGE),
            eq(RabbitMQConfig.PAYMENT_REQUESTED_KEY),
            eventCaptor.capture()
        );

        PolicyRequestEvent capturedEvent = eventCaptor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(requestId, capturedEvent.getPolicyRequestId());
        org.junit.jupiter.api.Assertions.assertEquals(customerId, capturedEvent.getCustomerId());
        org.junit.jupiter.api.Assertions.assertEquals(PolicyStatus.RECEIVED, capturedEvent.getStatus());
    }

    @Test
    void testProcessPaymentWithDifferentPaymentMethod() {
        policyRequest.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        
        paymentService.processPayment(policyRequest);

        verify(eventPublisher).publish(
            eq(RabbitMQConfig.POLICY_EVENTS_EXCHANGE),
            eq(RabbitMQConfig.PAYMENT_REQUESTED_KEY),
            eventCaptor.capture()
        );

        PolicyRequestEvent capturedEvent = eventCaptor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(requestId, capturedEvent.getPolicyRequestId());
        org.junit.jupiter.api.Assertions.assertEquals(customerId, capturedEvent.getCustomerId());
        org.junit.jupiter.api.Assertions.assertEquals(PolicyStatus.VALIDATED, capturedEvent.getStatus());
    }

    @Test
    void testProcessPaymentWithEventPublisherError() {
        doThrow(new RuntimeException("Failed to publish event"))
            .when(eventPublisher)
            .publish(any(), any(), any());

        assertThrows(RuntimeException.class, () ->
            paymentService.processPayment(policyRequest)
        );

        verify(eventPublisher).publish(
            eq(RabbitMQConfig.POLICY_EVENTS_EXCHANGE),
            eq(RabbitMQConfig.PAYMENT_REQUESTED_KEY),
            eventCaptor.capture()
        );
    }

    @Test
    void testProcessPaymentWithNullRequest() {
        assertThrows(IllegalArgumentException.class, () -> {
            paymentService.processPayment(null);
        });

        verify(eventPublisher, never()).publish(any(), any(), any());
    }

    @Test
    void testProcessPaymentWithNullRequestId() {
        policyRequest.setId(null);
        
        assertThrows(IllegalArgumentException.class, () -> {
            paymentService.processPayment(policyRequest);
        });

        verify(eventPublisher, never()).publish(any(), any(), any());
    }

    @Test
    void testProcessPaymentWithNullCustomerId() {
        policyRequest.setCustomerId(null);
        
        assertThrows(IllegalArgumentException.class, () -> {
            paymentService.processPayment(policyRequest);
        });

        verify(eventPublisher, never()).publish(any(), any(), any());
    }

    @Test
    void testProcessPaymentWithNullPaymentMethod() {
        policyRequest.setPaymentMethod(null);
        
        assertThrows(IllegalArgumentException.class, () -> {
            paymentService.processPayment(policyRequest);
        });

        verify(eventPublisher, never()).publish(any(), any(), any());
    }

    @Test
    void testProcessPaymentWithZeroAmount() {
        PolicyRequest mockRequest = mock(PolicyRequest.class);
        when(mockRequest.getId()).thenReturn(requestId);
        when(mockRequest.getCustomerId()).thenReturn(customerId);
        when(mockRequest.getPaymentMethod()).thenReturn(PaymentMethod.CREDIT_CARD);
        when(mockRequest.getTotalMonthlyPremiumAmount()).thenReturn(BigDecimal.ZERO);

        assertThrows(IllegalArgumentException.class, () ->
            paymentService.processPayment(mockRequest)
        );

        verify(eventPublisher, never()).publish(any(), any(), any());
    }

    @Test
    void testProcessPaymentWithNegativeAmount() {
        PolicyRequest mockRequest = mock(PolicyRequest.class);
        when(mockRequest.getId()).thenReturn(requestId);
        when(mockRequest.getCustomerId()).thenReturn(customerId);
        when(mockRequest.getPaymentMethod()).thenReturn(PaymentMethod.CREDIT_CARD);
        when(mockRequest.getTotalMonthlyPremiumAmount()).thenReturn(new BigDecimal("-100.00"));

        assertThrows(IllegalArgumentException.class, () ->
            paymentService.processPayment(mockRequest)
        );

        verify(eventPublisher, never()).publish(any(), any(), any());
    }

    @Test
    void testProcessPaymentWithAllPaymentMethods() {
        for (PaymentMethod method : PaymentMethod.values()) {
            policyRequest.setPaymentMethod(method);
            paymentService.processPayment(policyRequest);
        }

        verify(eventPublisher, times(PaymentMethod.values().length))
            .publish(eq(RabbitMQConfig.POLICY_EVENTS_EXCHANGE), 
                    eq(RabbitMQConfig.PAYMENT_REQUESTED_KEY), 
                    eventCaptor.capture());

        List<PolicyRequestEvent> events = eventCaptor.getAllValues();
        assertEquals(PaymentMethod.values().length, events.size());
        
        for (int i = 0; i < events.size(); i++) {
            assertEquals(requestId, events.get(i).getPolicyRequestId());
            assertEquals(customerId, events.get(i).getCustomerId());
        }
    }
} 