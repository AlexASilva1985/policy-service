package com.insurance.service.impl;

import com.insurance.domain.PolicyRequest;
import com.insurance.domain.StatusHistory;
import com.insurance.domain.enums.PolicyStatus;
import com.insurance.exception.BusinessException;
import com.insurance.service.PolicyStatusService;
import com.insurance.service.StatusHistoryValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PolicyStatusServiceImpl implements PolicyStatusService {

    private final StatusHistoryValidationService statusHistoryValidationService;

    @Override
    public boolean canTransitionTo(PolicyStatus currentStatus, PolicyStatus newStatus) {
        if (currentStatus == null) {
            return newStatus == PolicyStatus.RECEIVED;
        }

        return switch (currentStatus) {
            case RECEIVED -> newStatus == PolicyStatus.VALIDATED ||
                           newStatus == PolicyStatus.REJECTED ||
                           newStatus == PolicyStatus.CANCELLED;
            case VALIDATED -> newStatus == PolicyStatus.PENDING ||
                            newStatus == PolicyStatus.REJECTED ||
                            newStatus == PolicyStatus.CANCELLED;
            case PENDING -> newStatus == PolicyStatus.APPROVED ||
                          newStatus == PolicyStatus.REJECTED ||
                          newStatus == PolicyStatus.CANCELLED;
            case APPROVED -> false;
            case REJECTED, CANCELLED -> false;
        };
    }

    @Override
    public void updatePolicyStatus(PolicyRequest policyRequest, PolicyStatus newStatus) {
        if (policyRequest == null) {
            throw new IllegalArgumentException("Policy request cannot be null");
        }
        
        if (newStatus == null) {
            throw new IllegalArgumentException("New status cannot be null");
        }

        PolicyStatus currentStatus = policyRequest.getStatus();
        
        if (!canTransitionTo(currentStatus, newStatus)) {
            throw new BusinessException(
                    currentStatus != null ? currentStatus.toString() : "null", 
                    newStatus.toString());
        }

        StatusHistory history = createStatusHistory(policyRequest.getId(), currentStatus, newStatus);
        policyRequest.getStatusHistory().add(history);
        
        policyRequest.setStatus(newStatus);

        if (isFinalStatus(newStatus)) {
            policyRequest.setFinishedAt(LocalDateTime.now());
        }
    }

    @Override
    public StatusHistory createStatusHistory(UUID policyRequestId, PolicyStatus previousStatus, PolicyStatus newStatus) {
        return statusHistoryValidationService.createStatusHistory(
                policyRequestId, previousStatus, newStatus, null);
    }

    private boolean isFinalStatus(PolicyStatus status) {
        return status == PolicyStatus.APPROVED ||
               status == PolicyStatus.REJECTED ||
               status == PolicyStatus.CANCELLED;
    }
} 