package com.insurance.event;

import com.insurance.domain.PolicyRequest;

public class PolicyStatusChangedEvent extends PolicyRequestEvent {
    public PolicyStatusChangedEvent(PolicyRequest request) {
        super(request.getId(), request.getCustomerId(), request.getStatus());
    }
} 