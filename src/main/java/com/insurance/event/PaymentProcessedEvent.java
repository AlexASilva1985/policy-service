package com.insurance.event;

import com.insurance.domain.PolicyRequest;

public class PaymentProcessedEvent extends PolicyRequestEvent {
    public PaymentProcessedEvent(PolicyRequest request) {
        super(request.getId(), request.getCustomerId(), request.getStatus());
    }
} 