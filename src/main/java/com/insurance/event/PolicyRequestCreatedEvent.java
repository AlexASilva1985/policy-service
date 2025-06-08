package com.insurance.event;

import com.insurance.domain.PolicyRequest;
import com.insurance.domain.enums.PolicyStatus;

public class PolicyRequestCreatedEvent extends PolicyRequestEvent {
    public PolicyRequestCreatedEvent(PolicyRequest request) {
        super(request.getId(), request.getCustomerId(), PolicyStatus.RECEIVED);
    }
} 