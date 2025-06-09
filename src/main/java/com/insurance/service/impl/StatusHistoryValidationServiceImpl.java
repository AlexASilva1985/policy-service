package com.insurance.service.impl;

import com.insurance.domain.StatusHistory;
import com.insurance.domain.enums.PolicyStatus;
import com.insurance.service.StatusHistoryValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StatusHistoryValidationServiceImpl implements StatusHistoryValidationService {

    @Override
    public void validatePolicyRequestId(UUID policyRequestId) {
        if (policyRequestId == null) {
            throw new IllegalArgumentException("policyRequestId cannot be null");
        }
    }

    @Override
    public void validatePreviousStatus(PolicyStatus previousStatus) {
        if (previousStatus == null) {
            throw new IllegalArgumentException("previousStatus cannot be null");
        }
    }

    @Override
    public void validateNewStatus(PolicyStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("newStatus cannot be null");
        }
    }

    @Override
    public void validateStatusDifference(PolicyStatus previousStatus, PolicyStatus newStatus) {
        if (previousStatus != null && newStatus == previousStatus) {
            throw new IllegalArgumentException("newStatus cannot be the same as previousStatus");
        }
    }

    @Override
    public void validateStatusTransition(PolicyStatus fromStatus, PolicyStatus toStatus) {
        if (fromStatus != null && !isValidStatusTransition(fromStatus, toStatus)) {
            throw new IllegalStateException("Invalid status transition from " + fromStatus + " to " + toStatus);
        }
    }

    @Override
    public void validateChangedAt(LocalDateTime changedAt) {
        if (changedAt == null) {
            throw new IllegalArgumentException("changedAt cannot be null");
        }
    }

    @Override
    public void validateStatusHistory(StatusHistory statusHistory) {
        if (statusHistory == null) {
            throw new IllegalArgumentException("Status history cannot be null");
        }
        
        if (statusHistory.getPolicyRequestId() == null) {
            throw new IllegalArgumentException("policyRequestId is required");
        }
        validatePolicyRequestId(statusHistory.getPolicyRequestId());
        
        if (statusHistory.getPreviousStatus() == null) {
            throw new IllegalArgumentException("previousStatus is required");
        }
        validatePreviousStatus(statusHistory.getPreviousStatus());
        
        if (statusHistory.getNewStatus() == null) {
            throw new IllegalArgumentException("newStatus is required");
        }
        validateNewStatus(statusHistory.getNewStatus());
        
        validateStatusDifference(statusHistory.getPreviousStatus(), statusHistory.getNewStatus());
        validateStatusTransition(statusHistory.getPreviousStatus(), statusHistory.getNewStatus());
        
        if (statusHistory.getChangedAt() == null) {
            throw new IllegalArgumentException("changedAt is required");
        }
        validateChangedAt(statusHistory.getChangedAt());
    }

    @Override
    public StatusHistory createStatusHistory(UUID policyRequestId, PolicyStatus previousStatus, 
                                           PolicyStatus newStatus, String reason) {
        validatePolicyRequestId(policyRequestId);
        validatePreviousStatus(previousStatus);
        validateNewStatus(newStatus);
        validateStatusDifference(previousStatus, newStatus);
        validateStatusTransition(previousStatus, newStatus);
        
        StatusHistory statusHistory = new StatusHistory();
        statusHistory.setPolicyRequestId(policyRequestId);
        statusHistory.setPreviousStatus(previousStatus);
        statusHistory.setNewStatus(newStatus);
        statusHistory.setChangedAt(LocalDateTime.now());
        statusHistory.setReason(reason);

        return statusHistory;
    }

    @Override
    public void updateStatusHistory(StatusHistory statusHistory, UUID policyRequestId, 
                                  PolicyStatus previousStatus, PolicyStatus newStatus, 
                                  LocalDateTime changedAt, String reason) {
        if (statusHistory == null) {
            throw new IllegalArgumentException("Status history cannot be null");
        }
        
        if (policyRequestId != null) {
            validatePolicyRequestId(policyRequestId);
            statusHistory.setPolicyRequestId(policyRequestId);
        }
        
        if (previousStatus != null) {
            validatePreviousStatus(previousStatus);
            statusHistory.setPreviousStatus(previousStatus);
        }
        
        if (newStatus != null) {
            validateNewStatus(newStatus);
            validateStatusDifference(statusHistory.getPreviousStatus(), newStatus);
            validateStatusTransition(statusHistory.getPreviousStatus(), newStatus);
            statusHistory.setNewStatus(newStatus);
        }
        
        if (changedAt != null) {
            validateChangedAt(changedAt);
            statusHistory.setChangedAt(changedAt);
        }
        
        if (reason != null) {
            statusHistory.setReason(reason);
        }
    }

    private boolean isValidStatusTransition(PolicyStatus from, PolicyStatus to) {
        return switch (from) {
            case RECEIVED -> to == PolicyStatus.VALIDATED ||
                           to == PolicyStatus.REJECTED ||
                           to == PolicyStatus.CANCELLED;
            case VALIDATED -> to == PolicyStatus.PENDING ||
                            to == PolicyStatus.REJECTED ||
                            to == PolicyStatus.CANCELLED;
            case PENDING -> to == PolicyStatus.APPROVED ||
                          to == PolicyStatus.REJECTED ||
                          to == PolicyStatus.CANCELLED;
            case APPROVED -> false;
            case REJECTED, CANCELLED -> false;
        };
    }
} 