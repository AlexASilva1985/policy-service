package com.insurance.event;

import com.insurance.domain.PolicyRequest;

public class PaymentRequestedEvent extends PolicyRequestEvent {
    public PaymentRequestedEvent(PolicyRequest request) {
        super(request.getId(), request.getCustomerId(), request.getStatus());
    }
} 