package com.insurance.event;

import com.insurance.domain.PolicyRequest;

public class SubscriptionApprovedEvent extends PolicyRequestEvent {
    public SubscriptionApprovedEvent(PolicyRequest request) {
        super(request.getId(), request.getCustomerId(), request.getStatus());
    }
} 