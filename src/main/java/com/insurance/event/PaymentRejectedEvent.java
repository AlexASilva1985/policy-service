package com.insurance.event;

import com.insurance.domain.PolicyRequest;

public class PaymentRejectedEvent extends PolicyRequestEvent {
    public PaymentRejectedEvent(PolicyRequest request) {
        super(request.getId(), request.getCustomerId(), request.getStatus());
    }
} 