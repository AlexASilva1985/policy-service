package com.insurance.service.impl;

import com.insurance.domain.PolicyRequest;
import com.insurance.event.PaymentRequestedEvent;
import com.insurance.infrastructure.messaging.config.RabbitMQConfig;
import com.insurance.infrastructure.messaging.service.EventPublisher;
import com.insurance.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final EventPublisher eventPublisher;

    @Override
    @Transactional
    public boolean processPayment(PolicyRequest request) {
        validateRequest(request);

        eventPublisher.publish(
            RabbitMQConfig.POLICY_EVENTS_EXCHANGE,
            RabbitMQConfig.PAYMENT_REQUESTED_KEY,
            new PaymentRequestedEvent(request)
        );
        return true;
    }

    private void validateRequest(PolicyRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Policy request cannot be null");
        }
        if (request.getId() == null) {
            throw new IllegalArgumentException("Policy request ID cannot be null");
        }
        if (request.getCustomerId() == null) {
            throw new IllegalArgumentException("Customer ID cannot be null");
        }
        if (request.getPaymentMethod() == null) {
            throw new IllegalArgumentException("Payment method cannot be null");
        }
        if (request.getTotalMonthlyPremiumAmount() == null) {
            throw new IllegalArgumentException("Total monthly premium amount cannot be null");
        }
        if (request.getTotalMonthlyPremiumAmount().signum() <= 0) {
            throw new IllegalArgumentException("Total monthly premium amount must be greater than zero");
        }
    }
}