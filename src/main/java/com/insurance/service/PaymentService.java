package com.insurance.service;

import com.insurance.domain.PolicyRequest;

public interface PaymentService {
    /**
     * Processa o pagamento de uma solicitação
     */
    boolean processPayment(PolicyRequest request);
} 