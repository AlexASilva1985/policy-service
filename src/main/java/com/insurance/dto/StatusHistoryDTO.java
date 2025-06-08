package com.insurance.dto;

import com.insurance.domain.enums.PolicyStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StatusHistoryDTO {
    private PolicyStatus status;
    private LocalDateTime timestamp;
} 