package com.insurance.service.impl;

import com.insurance.domain.RiskOccurrence;
import com.insurance.service.RiskOccurrenceValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RiskOccurrenceValidationServiceImpl implements RiskOccurrenceValidationService {

    @Override
    public void validateType(String type) {
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Risk occurrence type cannot be empty");
        }
    }

    @Override
    public void validateDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Risk occurrence description cannot be empty");
        }
    }

    @Override
    public void validateCreatedAt(LocalDateTime createdAt) {
        if (createdAt == null) {
            throw new IllegalArgumentException("Created at timestamp cannot be null");
        }
        validateTimestampConstraints(createdAt);
    }

    @Override
    public void validateUpdatedAt(LocalDateTime updatedAt, LocalDateTime createdAt) {
        if (updatedAt == null) {
            throw new IllegalArgumentException("Updated at timestamp cannot be null");
        }
        validateTimestampConstraints(updatedAt);
        
        if (createdAt != null && updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("Updated at timestamp cannot be before created at timestamp");
        }
    }

    @Override
    public void validateTimestampConstraints(LocalDateTime timestamp) {
        if (timestamp != null && timestamp.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Timestamp cannot be in the future");
        }
    }

    @Override
    public void validateRiskOccurrence(RiskOccurrence occurrence) {
        if (occurrence == null) {
            throw new IllegalArgumentException("Risk occurrence cannot be null");
        }
        
        if (occurrence.getType() == null || occurrence.getType().trim().isEmpty()) {
            throw new IllegalArgumentException("Risk occurrence type is required");
        }
        validateType(occurrence.getType());
        
        if (occurrence.getDescription() == null || occurrence.getDescription().trim().isEmpty()) {
            throw new IllegalArgumentException("Risk occurrence description is required");
        }
        validateDescription(occurrence.getDescription());
        
        if (occurrence.getCreatedAt() == null) {
            throw new IllegalArgumentException("Created at timestamp is required");
        }
        validateCreatedAt(occurrence.getCreatedAt());
        
        if (occurrence.getUpdatedAt() == null) {
            throw new IllegalArgumentException("Updated at timestamp is required");
        }
        validateUpdatedAt(occurrence.getUpdatedAt(), occurrence.getCreatedAt());
    }
}