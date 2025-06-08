package com.insurance.event;

import com.insurance.domain.PolicyRequest;

public class PolicyValidatedEvent extends PolicyRequestEvent {
    public PolicyValidatedEvent(PolicyRequest request) {
        super(request.getId(), request.getCustomerId(), request.getStatus());
    }
} 