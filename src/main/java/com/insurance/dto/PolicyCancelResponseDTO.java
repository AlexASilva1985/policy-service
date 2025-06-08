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
public class PolicyCancelResponseDTO {
    
    private UUID policyId;
    private boolean cancelled;
    private PolicyStatus status;
    private String message;
    private String reason;
    private LocalDateTime cancelledAt;
    
    public static PolicyCancelResponseDTO success(UUID policyId) {
        return new PolicyCancelResponseDTO(
            policyId,
            true,
                PolicyStatus.CANCELLED,
            "Apólice cancelada com sucesso",
            "Cancelamento realizado conforme solicitado",
            LocalDateTime.now()
        );
    }
    
    public static PolicyCancelResponseDTO failure(UUID policyId, String reason) {
        return new PolicyCancelResponseDTO(
            policyId,
            false,
            null,
            "Não foi possível cancelar a apólice",
            reason,
            LocalDateTime.now()
        );
    }
    
    public static PolicyCancelResponseDTO error(UUID policyId, String reason) {
        return new PolicyCancelResponseDTO(
            policyId,
            false,
            null,
            "Erro durante cancelamento da apólice",
            reason,
            LocalDateTime.now()
        );
    }
} 