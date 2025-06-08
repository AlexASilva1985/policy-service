package com.insurance.dto;

import com.insurance.domain.enums.PolicyStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PolicyCancelResponseDTOTest {

    @Test
    void testSuccessFactoryMethod() {
        UUID policyId = UUID.randomUUID();
        
        PolicyCancelResponseDTO response = PolicyCancelResponseDTO.success(policyId);
        
        assertTrue(response.isCancelled());
        assertEquals(policyId, response.getPolicyId());
        assertEquals(PolicyStatus.CANCELLED, response.getStatus());
        assertEquals("Apólice cancelada com sucesso", response.getMessage());
        assertNotNull(response.getCancelledAt());
        assertEquals("Cancelamento realizado conforme solicitado", response.getReason());
    }

    @Test
    void testFailureFactoryMethod() {
        UUID policyId = UUID.randomUUID();
        String reason = "Não é possível cancelar uma apólice já aprovada";
        
        PolicyCancelResponseDTO response = PolicyCancelResponseDTO.failure(policyId, reason);
        
        assertFalse(response.isCancelled());
        assertEquals(policyId, response.getPolicyId());
        assertNull(response.getStatus());
        assertEquals("Não foi possível cancelar a apólice", response.getMessage());
        assertEquals(reason, response.getReason());
        assertNotNull(response.getCancelledAt());
    }

    @Test
    void testErrorFactoryMethod() {
        UUID policyId = UUID.randomUUID();
        String reason = "Erro interno durante cancelamento";
        
        PolicyCancelResponseDTO response = PolicyCancelResponseDTO.error(policyId, reason);
        
        assertFalse(response.isCancelled());
        assertEquals(policyId, response.getPolicyId());
        assertNull(response.getStatus());
        assertEquals("Erro durante cancelamento da apólice", response.getMessage());
        assertEquals(reason, response.getReason());
        assertNotNull(response.getCancelledAt());
    }

    @Test
    void testGettersAndSetters() {
        PolicyCancelResponseDTO response = new PolicyCancelResponseDTO();
        UUID policyId = UUID.randomUUID();
        LocalDateTime cancelledAt = LocalDateTime.now();
        
        response.setPolicyId(policyId);
        response.setCancelled(true);
        response.setStatus(PolicyStatus.CANCELLED);
        response.setMessage("Success message");
        response.setReason("Test reason");
        response.setCancelledAt(cancelledAt);
        
        assertEquals(policyId, response.getPolicyId());
        assertTrue(response.isCancelled());
        assertEquals(PolicyStatus.CANCELLED, response.getStatus());
        assertEquals("Success message", response.getMessage());
        assertEquals("Test reason", response.getReason());
        assertEquals(cancelledAt, response.getCancelledAt());
    }

    @Test
    void testEqualsAndHashCode() {
        UUID policyId = UUID.randomUUID();
        LocalDateTime cancelledAt = LocalDateTime.now();
        
        PolicyCancelResponseDTO response1 = new PolicyCancelResponseDTO();
        response1.setPolicyId(policyId);
        response1.setCancelled(true);
        response1.setStatus(PolicyStatus.CANCELLED);
        response1.setCancelledAt(cancelledAt);
        
        PolicyCancelResponseDTO response2 = new PolicyCancelResponseDTO();
        response2.setPolicyId(policyId);
        response2.setCancelled(true);
        response2.setStatus(PolicyStatus.CANCELLED);
        response2.setCancelledAt(cancelledAt);
        
        assertEquals(response1, response2);
        assertEquals(response1.hashCode(), response2.hashCode());
        
        response2.setCancelled(false);
        assertNotEquals(response1, response2);
    }

    @Test
    void testToString() {
        PolicyCancelResponseDTO response = PolicyCancelResponseDTO.success(UUID.randomUUID());
        
        String toString = response.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("PolicyCancelResponseDTO"));
        assertTrue(toString.contains("cancelled=true"));
    }

    @Test
    void testNullValues() {
        PolicyCancelResponseDTO response = new PolicyCancelResponseDTO();
        
        assertNull(response.getPolicyId());
        assertFalse(response.isCancelled()); // should default to false
        assertNull(response.getStatus());
        assertNull(response.getMessage());
        assertNull(response.getReason());
        assertNull(response.getCancelledAt());
    }

    @Test
    void testCancellationScenarios() {
        UUID policyId = UUID.randomUUID();
        
        PolicyCancelResponseDTO success = PolicyCancelResponseDTO.success(policyId);
        assertTrue(success.isCancelled());
        assertEquals("Apólice cancelada com sucesso", success.getMessage());
        assertEquals(PolicyStatus.CANCELLED, success.getStatus());
        
        PolicyCancelResponseDTO failure = PolicyCancelResponseDTO.failure(policyId, "Não é possível cancelar uma apólice já aprovada");
        assertFalse(failure.isCancelled());
        assertNull(failure.getStatus());
        assertEquals("Não foi possível cancelar a apólice", failure.getMessage());
        assertTrue(failure.getReason().contains("já aprovada"));
        
        PolicyCancelResponseDTO notFound = PolicyCancelResponseDTO.error(policyId, "Apólice não encontrada");
        assertFalse(notFound.isCancelled());
        assertNull(notFound.getStatus());
        assertEquals("Erro durante cancelamento da apólice", notFound.getMessage());
        assertEquals("Apólice não encontrada", notFound.getReason());
        
        PolicyCancelResponseDTO error = PolicyCancelResponseDTO.error(policyId, "Database connection failed");
        assertFalse(error.isCancelled());
        assertNull(error.getStatus());
        assertEquals("Erro durante cancelamento da apólice", error.getMessage());
        assertEquals("Database connection failed", error.getReason());
    }

    @Test
    void testTimestampConsistency() {
        LocalDateTime before = LocalDateTime.now();
        PolicyCancelResponseDTO response = PolicyCancelResponseDTO.success(UUID.randomUUID());
        LocalDateTime after = LocalDateTime.now();
        
        assertNotNull(response.getCancelledAt());
        assertTrue(response.getCancelledAt().isAfter(before.minusSeconds(1)));
        assertTrue(response.getCancelledAt().isBefore(after.plusSeconds(1)));
    }

    @Test
    void testAllFieldsCombinations() {
        UUID policyId = UUID.randomUUID();
        
        PolicyCancelResponseDTO response = new PolicyCancelResponseDTO();
        response.setPolicyId(policyId);
        response.setCancelled(true);
        response.setStatus(PolicyStatus.CANCELLED);
        response.setMessage("Custom message");
        response.setReason("Custom reason");
        response.setCancelledAt(LocalDateTime.now());
        
        assertEquals(policyId, response.getPolicyId());
        assertTrue(response.isCancelled());
        assertEquals(PolicyStatus.CANCELLED, response.getStatus());
        assertEquals("Custom message", response.getMessage());
        assertEquals("Custom reason", response.getReason());
        assertNotNull(response.getCancelledAt());
    }

    @Test
    void testAllArgsConstructor() {
        UUID policyId = UUID.randomUUID();
        LocalDateTime cancelledAt = LocalDateTime.now();
        
        PolicyCancelResponseDTO response = new PolicyCancelResponseDTO(
            policyId,
            true,
            PolicyStatus.CANCELLED,
            "Constructor message",
            "Constructor reason",
            cancelledAt
        );
        
        assertEquals(policyId, response.getPolicyId());
        assertTrue(response.isCancelled());
        assertEquals(PolicyStatus.CANCELLED, response.getStatus());
        assertEquals("Constructor message", response.getMessage());
        assertEquals("Constructor reason", response.getReason());
        assertEquals(cancelledAt, response.getCancelledAt());
    }
} 