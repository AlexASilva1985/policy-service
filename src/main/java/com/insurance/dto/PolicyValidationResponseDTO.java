package com.insurance.dto;

import com.insurance.domain.enums.PolicyStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PolicyValidationResponseDTO {
    
    private UUID policyId;
    private boolean validated;
    private PolicyStatus status;
    private String message;
    private String reason;
    private LocalDateTime validatedAt;
    
    public static PolicyValidationResponseDTO success(UUID policyId, PolicyStatus status) {
        return new PolicyValidationResponseDTO(
            policyId,
            true,
            status,
            "Apólice validada com sucesso",
            "Valor do seguro dentro dos limites permitidos para o tipo de cliente",
            LocalDateTime.now()
        );
    }
    
    public static PolicyValidationResponseDTO failure(UUID policyId, String reason) {
        return new PolicyValidationResponseDTO(
            policyId,
            false,
                PolicyStatus.REJECTED,
            "Apólice rejeitada durante validação",
            reason,
            LocalDateTime.now()
        );
    }
    
    public static PolicyValidationResponseDTO error(UUID policyId, String reason) {
        return new PolicyValidationResponseDTO(
            policyId,
            false,
            null,
            "Erro durante validação da apólice",
            reason,
            LocalDateTime.now()
        );
    }
} 