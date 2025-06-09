package com.insurance.service;

import com.insurance.domain.PolicyRequest;
import com.insurance.domain.StatusHistory;
import com.insurance.domain.enums.PolicyStatus;

import java.util.UUID;

public interface PolicyStatusService {
    
    /**
     * Verifica se uma transição de status é válida
     */
    boolean canTransitionTo(PolicyStatus currentStatus, PolicyStatus newStatus);
    
    /**
     * Atualiza o status de uma política seguindo as regras de negócio
     */
    void updatePolicyStatus(PolicyRequest policyRequest, PolicyStatus newStatus);
    
    /**
     * Cria um histórico de mudança de status
     */
    StatusHistory createStatusHistory(UUID policyRequestId, PolicyStatus previousStatus, PolicyStatus newStatus);
} 