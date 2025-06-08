package com.insurance.dto;

import com.insurance.domain.enums.CustomerRiskType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class FraudAnalysisResponseDTO {
    private UUID orderId;
    private UUID customerId;
    private LocalDateTime analyzedAt;
    private CustomerRiskType classification;
    private List<RiskOccurrenceDTO> occurrences;

    @Data
    public static class RiskOccurrenceDTO {
        private UUID id;
        private Long productId;
        private String type;
        private String description;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
} 