package com.insurance.event;

import com.insurance.domain.PolicyRequest;

public class PolicyRejectedEvent extends PolicyRequestEvent {
    public PolicyRejectedEvent(PolicyRequest request) {
        super(request.getId(), request.getCustomerId(), request.getStatus());
    }
} 