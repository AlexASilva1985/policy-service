package com.insurance.service.impl;

import com.insurance.domain.Claim;
import com.insurance.domain.enums.ClaimStatus;
import com.insurance.service.ClaimStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ClaimStatusServiceImpl implements ClaimStatusService {

    @Override
    public boolean canTransitionTo(ClaimStatus fromStatus, ClaimStatus toStatus) {
        if (fromStatus == null || toStatus == null) {
            return false;
        }
        
        return switch (fromStatus) {
            case SUBMITTED -> toStatus == ClaimStatus.UNDER_REVIEW || toStatus == ClaimStatus.REJECTED;
            case UNDER_REVIEW -> toStatus == ClaimStatus.APPROVED || toStatus == ClaimStatus.REJECTED;
            case APPROVED -> toStatus == ClaimStatus.PAID;
            case PAID, REJECTED -> false;
        };
    }

    @Override
    public void updateClaimStatus(Claim claim, ClaimStatus newStatus) {
        if (claim == null) {
            throw new IllegalArgumentException("Claim cannot be null");
        }
        if (newStatus == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        
        ClaimStatus currentStatus = claim.getStatus();
        if (currentStatus != null && !canTransitionTo(currentStatus, newStatus)) {
            throw new IllegalStateException(
                String.format("Invalid status transition from %s to %s", currentStatus, newStatus));
        }
        claim.setStatus(newStatus);
    }

    @Override
    public void validateClaimNumber(String claimNumber) {
        if (claimNumber == null) {
            throw new IllegalArgumentException("Claim number cannot be null");
        }
        if (claimNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Claim number cannot be empty");
        }
        if (!claimNumber.startsWith("CLM")) {
            throw new IllegalArgumentException("Claim number must start with 'CLM'");
        }
        if (claimNumber.length() < 11) {
            throw new IllegalArgumentException("Claim number must have at least 11 characters");
        }
    }

    @Override
    public void validateIncidentDate(LocalDate incidentDate, Claim claim) {
        if (incidentDate == null) {
            throw new IllegalArgumentException("Incident date cannot be null");
        }
        if (incidentDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Incident date cannot be in the future");
        }
        if (claim != null && claim.getPolicy() != null && 
            incidentDate.isBefore(claim.getPolicy().getStartDate())) {
            throw new IllegalArgumentException("Incident date cannot be before policy start date");
        }
    }

    @Override
    public void validateClaimAmount(BigDecimal claimAmount, Claim claim) {
        if (claimAmount == null) {
            throw new IllegalArgumentException("Claim amount cannot be null");
        }
        if (claimAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Claim amount cannot be negative");
        }
        if (claimAmount.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Claim amount must be greater than zero");
        }
        if (claim != null && claim.getPolicy() != null && 
            claimAmount.compareTo(claim.getPolicy().getCoverageAmount()) > 0) {
            throw new IllegalArgumentException("Claim amount cannot exceed policy coverage amount");
        }
    }

    @Override
    public void validateClaim(Claim claim) {
        if (claim == null) {
            throw new IllegalArgumentException("Claim cannot be null");
        }
        
        if (claim.getClaimNumber() == null || claim.getClaimNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Claim number is required");
        }
        
        validateClaimNumber(claim.getClaimNumber());
        
        if (claim.getIncidentDate() == null) {
            throw new IllegalArgumentException("Incident date is required");
        }
        
        validateIncidentDate(claim.getIncidentDate(), claim);
        
        if (claim.getDescription() == null || claim.getDescription().trim().isEmpty()) {
            throw new IllegalArgumentException("Description is required");
        }
        
        if (claim.getClaimAmount() == null) {
            throw new IllegalArgumentException("Claim amount is required");
        }
        
        validateClaimAmount(claim.getClaimAmount(), claim);
        
        if (claim.getPolicy() == null) {
            throw new IllegalArgumentException("Policy is required");
        }
    }
} 