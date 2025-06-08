package com.insurance.dto;

import com.insurance.domain.enums.PolicyStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PolicyValidationResponseDTOTest {

    @Test
    void testSuccessFactoryMethod() {
        UUID policyId = UUID.randomUUID();
        PolicyStatus status = PolicyStatus.VALIDATED;
        
        PolicyValidationResponseDTO response = PolicyValidationResponseDTO.success(policyId, status);
        
        assertTrue(response.isValidated());
        assertEquals(policyId, response.getPolicyId());
        assertEquals(status, response.getStatus());
        assertEquals("Apólice validada com sucesso", response.getMessage());
        assertNotNull(response.getValidatedAt());
        assertNotNull(response.getReason());
    }

    @Test
    void testSuccessWithDifferentStatus() {
        UUID policyId = UUID.randomUUID();
        
        PolicyValidationResponseDTO response = PolicyValidationResponseDTO.success(policyId, PolicyStatus.APPROVED);
        
        assertTrue(response.isValidated());
        assertEquals(policyId, response.getPolicyId());
        assertEquals(PolicyStatus.APPROVED, response.getStatus());
        assertEquals("Apólice validada com sucesso", response.getMessage());
        assertNotNull(response.getValidatedAt());
        assertNotNull(response.getReason());
    }

    @Test
    void testFailureFactoryMethod() {
        UUID policyId = UUID.randomUUID();
        String reason = "Valor excede limite permitido";
        
        PolicyValidationResponseDTO response = PolicyValidationResponseDTO.failure(policyId, reason);
        
        assertFalse(response.isValidated());
        assertEquals(policyId, response.getPolicyId());
        assertEquals(PolicyStatus.REJECTED, response.getStatus());
        assertEquals("Apólice rejeitada durante validação", response.getMessage());
        assertEquals(reason, response.getReason());
        assertNotNull(response.getValidatedAt());
    }

    @Test
    void testErrorFactoryMethod() {
        UUID policyId = UUID.randomUUID();
        String reason = "Erro interno durante validação";
        
        PolicyValidationResponseDTO response = PolicyValidationResponseDTO.error(policyId, reason);
        
        assertFalse(response.isValidated());
        assertEquals(policyId, response.getPolicyId());
        assertNull(response.getStatus());
        assertEquals("Erro durante validação da apólice", response.getMessage());
        assertEquals(reason, response.getReason());
        assertNotNull(response.getValidatedAt());
    }

    @Test
    void testGettersAndSetters() {
        PolicyValidationResponseDTO response = new PolicyValidationResponseDTO();
        UUID policyId = UUID.randomUUID();
        LocalDateTime validatedAt = LocalDateTime.now();
        
        response.setPolicyId(policyId);
        response.setValidated(true);
        response.setStatus(PolicyStatus.VALIDATED);
        response.setMessage("Success message");
        response.setReason("Test reason");
        response.setValidatedAt(validatedAt);
        
        assertEquals(policyId, response.getPolicyId());
        assertTrue(response.isValidated());
        assertEquals(PolicyStatus.VALIDATED, response.getStatus());
        assertEquals("Success message", response.getMessage());
        assertEquals("Test reason", response.getReason());
        assertEquals(validatedAt, response.getValidatedAt());
    }

    @Test
    void testEqualsAndHashCode() {
        UUID policyId = UUID.randomUUID();
        LocalDateTime validatedAt = LocalDateTime.now();
        
        PolicyValidationResponseDTO response1 = new PolicyValidationResponseDTO();
        response1.setPolicyId(policyId);
        response1.setValidated(true);
        response1.setStatus(PolicyStatus.VALIDATED);
        response1.setValidatedAt(validatedAt);
        
        PolicyValidationResponseDTO response2 = new PolicyValidationResponseDTO();
        response2.setPolicyId(policyId);
        response2.setValidated(true);
        response2.setStatus(PolicyStatus.VALIDATED);
        response2.setValidatedAt(validatedAt);
        
        assertEquals(response1, response2);
        assertEquals(response1.hashCode(), response2.hashCode());
        
        // Test inequality
        response2.setValidated(false);
        assertNotEquals(response1, response2);
    }

    @Test
    void testToString() {
        PolicyValidationResponseDTO response = PolicyValidationResponseDTO.success(UUID.randomUUID(), PolicyStatus.VALIDATED);
        
        String toString = response.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("PolicyValidationResponseDTO"));
        assertTrue(toString.contains("validated=true"));
    }

    @Test
    void testNullValues() {
        PolicyValidationResponseDTO response = new PolicyValidationResponseDTO();
        
        assertNull(response.getPolicyId());
        assertFalse(response.isValidated()); // should default to false
        assertNull(response.getStatus());
        assertNull(response.getMessage());
        assertNull(response.getReason());
        assertNull(response.getValidatedAt());
    }

    @Test
    void testDifferentStatuses() {
        UUID policyId = UUID.randomUUID();
        
        // Test with different statuses
        PolicyValidationResponseDTO approvedResponse = PolicyValidationResponseDTO.success(policyId, PolicyStatus.APPROVED);
        assertEquals(PolicyStatus.APPROVED, approvedResponse.getStatus());
        
        PolicyValidationResponseDTO pendingResponse = PolicyValidationResponseDTO.success(policyId, PolicyStatus.PENDING);
        assertEquals(PolicyStatus.PENDING, pendingResponse.getStatus());
    }

    @Test
    void testValidationScenarios() {
        UUID policyId = UUID.randomUUID();
        
        // Scenario 1: Successful validation
        PolicyValidationResponseDTO success = PolicyValidationResponseDTO.success(policyId, PolicyStatus.VALIDATED);
        assertTrue(success.isValidated());
        assertEquals("Apólice validada com sucesso", success.getMessage());
        
        // Scenario 2: Failed validation due to business rules
        PolicyValidationResponseDTO failure = PolicyValidationResponseDTO.failure(policyId, "Valor R$ 500000.00 excede o limite de R$ 250000.00 para categoria AUTO e tipo de cliente HIGH_RISK");
        assertFalse(failure.isValidated());
        assertEquals(PolicyStatus.REJECTED, failure.getStatus());
        assertTrue(failure.getReason().contains("excede o limite"));
        
        // Scenario 3: Error during validation
        PolicyValidationResponseDTO error = PolicyValidationResponseDTO.error(policyId, "Connection timeout");
        assertFalse(error.isValidated());
        assertNull(error.getStatus());
        assertEquals("Erro durante validação da apólice", error.getMessage());
    }

    @Test
    void testAllArgsConstructor() {
        UUID policyId = UUID.randomUUID();
        LocalDateTime validatedAt = LocalDateTime.now();
        
        PolicyValidationResponseDTO response = new PolicyValidationResponseDTO(
            policyId,
            true,
            PolicyStatus.VALIDATED,
            "Custom message",
            "Custom reason",
            validatedAt
        );
        
        assertEquals(policyId, response.getPolicyId());
        assertTrue(response.isValidated());
        assertEquals(PolicyStatus.VALIDATED, response.getStatus());
        assertEquals("Custom message", response.getMessage());
        assertEquals("Custom reason", response.getReason());
        assertEquals(validatedAt, response.getValidatedAt());
    }
} 