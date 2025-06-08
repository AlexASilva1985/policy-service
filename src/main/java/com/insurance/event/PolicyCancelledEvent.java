package com.insurance.event;

import com.insurance.domain.PolicyRequest;

public class PolicyCancelledEvent extends PolicyRequestEvent {
    public PolicyCancelledEvent(PolicyRequest request) {
        super(request.getId(), request.getCustomerId(), request.getStatus());
    }
} 