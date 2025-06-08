package com.insurance.domain;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

import com.insurance.domain.enums.PolicyStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StatusHistoryTest {

    private StatusHistory statusHistory;
    private UUID policyRequestId;
    private PolicyRequest policyRequest;

    @BeforeEach
    void setUp() {
        policyRequestId = UUID.randomUUID();
        statusHistory = new StatusHistory();
        statusHistory.setPolicyRequestId(policyRequestId);
        statusHistory.setPreviousStatus(PolicyStatus.RECEIVED);
        statusHistory.setNewStatus(PolicyStatus.VALIDATED);
        statusHistory.setChangedAt(LocalDateTime.now());
        policyRequest = new PolicyRequest();
        policyRequest.setId(UUID.randomUUID());
    }

    @Test
    void testCreateStatusHistoryWithCorrectData() {
        assertNotNull(statusHistory);
        assertEquals(policyRequestId, statusHistory.getPolicyRequestId());
        assertEquals(PolicyStatus.RECEIVED, statusHistory.getPreviousStatus());
        assertEquals(PolicyStatus.VALIDATED, statusHistory.getNewStatus());
        assertNotNull(statusHistory.getChangedAt());
    }

    @Test
    void testValidateRequiredFields() {
        StatusHistory invalidHistory = new StatusHistory();
        
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            invalidHistory.validate();
        });
        assertTrue(exception.getMessage().contains("required"));

        invalidHistory.setPolicyRequestId(policyRequestId);
        invalidHistory.setNewStatus(PolicyStatus.VALIDATED);
        
        exception = assertThrows(IllegalArgumentException.class, () -> {
            invalidHistory.validate();
        });
        assertTrue(exception.getMessage().contains("previousStatus"));
    }

    @Test
    void testNotAllowSameStatusValues() {
        assertThrows(IllegalArgumentException.class, () -> {
            statusHistory.setNewStatus(statusHistory.getPreviousStatus());
        });
    }

    @Test
    void testValidateStatusTransitions() {

        assertDoesNotThrow(() -> {
            statusHistory.setPreviousStatus(PolicyStatus.VALIDATED);
            statusHistory.setNewStatus(PolicyStatus.PENDING);
        });

        Exception exception = assertThrows(IllegalStateException.class, () -> {
            statusHistory.setPreviousStatus(PolicyStatus.REJECTED);
            statusHistory.setNewStatus(PolicyStatus.PENDING);
        });
        assertTrue(exception.getMessage().contains("Invalid status transition"));
    }

    @Test
    void testAutomaticallySetCreatedAtOnNew() {
        StatusHistory newHistory = new StatusHistory();
        newHistory.setPolicyRequestId(UUID.randomUUID());
        newHistory.setPreviousStatus(PolicyStatus.RECEIVED);
        newHistory.setNewStatus(PolicyStatus.VALIDATED);
        
        newHistory.onCreate();
        
        assertNotNull(newHistory.getChangedAt());
        assertTrue(newHistory.getChangedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
        assertTrue(newHistory.getChangedAt().isAfter(LocalDateTime.now().minusSeconds(1)));
    }

    @Test
    void testStatusHistoryCreationWithAllFields() {
        StatusHistory history = new StatusHistory();
        history.setPolicyRequestId(policyRequest.getId());
        history.setPreviousStatus(PolicyStatus.RECEIVED);
        history.setNewStatus(PolicyStatus.VALIDATED);
        history.setChangedAt(LocalDateTime.now());
        history.setReason("Validation completed successfully");

        assertEquals(policyRequest.getId(), history.getPolicyRequestId());
        assertEquals(PolicyStatus.RECEIVED, history.getPreviousStatus());
        assertEquals(PolicyStatus.VALIDATED, history.getNewStatus());
        assertNotNull(history.getChangedAt());
        assertEquals("Validation completed successfully", history.getReason());
    }

    @Test
    void testStatusHistoryWithNullReason() {
        StatusHistory history = new StatusHistory();
        history.setPolicyRequestId(policyRequest.getId());
        history.setPreviousStatus(PolicyStatus.VALIDATED);
        history.setNewStatus(PolicyStatus.PENDING);
        history.setChangedAt(LocalDateTime.now());
        history.setReason(null);

        assertNull(history.getReason());
    }

    @Test
    void testStatusHistoryWithEmptyReason() {
        StatusHistory history = new StatusHistory();
        history.setPolicyRequestId(policyRequest.getId());
        history.setPreviousStatus(PolicyStatus.PENDING);
        history.setNewStatus(PolicyStatus.APPROVED);
        history.setChangedAt(LocalDateTime.now());
        history.setReason("");

        assertEquals("", history.getReason());
    }

    @Test
    void testStatusHistoryWithLongReason() {
        StatusHistory history = new StatusHistory();
        history.setPolicyRequestId(policyRequest.getId());
        history.setPreviousStatus(PolicyStatus.RECEIVED);
        history.setNewStatus(PolicyStatus.REJECTED);
        history.setChangedAt(LocalDateTime.now());
        
        String longReason = "This is a very long reason that explains in detail why the status was changed. " +
                           "It includes multiple sentences and provides comprehensive information about the decision process.";
        history.setReason(longReason);

        assertEquals(longReason, history.getReason());
    }

    @Test
    void testAllStatusTransitions() {
        LocalDateTime changeTime = LocalDateTime.now();

        StatusHistory history1 = new StatusHistory();
        history1.setPolicyRequestId(policyRequest.getId());
        history1.setPreviousStatus(PolicyStatus.RECEIVED);
        history1.setNewStatus(PolicyStatus.VALIDATED);
        history1.setChangedAt(changeTime);
        history1.setReason("Document validation completed");

        StatusHistory history2 = new StatusHistory();
        history2.setPolicyRequestId(policyRequest.getId());
        history2.setPreviousStatus(PolicyStatus.VALIDATED);
        history2.setNewStatus(PolicyStatus.PENDING);
        history2.setChangedAt(changeTime.plusMinutes(10));
        history2.setReason("Moved to pending for further review");

        StatusHistory history3 = new StatusHistory();
        history3.setPolicyRequestId(policyRequest.getId());
        history3.setPreviousStatus(PolicyStatus.PENDING);
        history3.setNewStatus(PolicyStatus.APPROVED);
        history3.setChangedAt(changeTime.plusMinutes(20));
        history3.setReason("All requirements met, approved");

        assertEquals(PolicyStatus.VALIDATED, history1.getNewStatus());
        assertEquals(PolicyStatus.PENDING, history2.getNewStatus());
        assertEquals(PolicyStatus.APPROVED, history3.getNewStatus());
    }

    @Test
    void testRejectionTransitions() {
        LocalDateTime changeTime = LocalDateTime.now();

        StatusHistory rejectionFromReceived = new StatusHistory();
        rejectionFromReceived.setPolicyRequestId(policyRequest.getId());
        rejectionFromReceived.setPreviousStatus(PolicyStatus.RECEIVED);
        rejectionFromReceived.setNewStatus(PolicyStatus.REJECTED);
        rejectionFromReceived.setChangedAt(changeTime);
        rejectionFromReceived.setReason("Invalid documentation provided");

        StatusHistory rejectionFromValidated = new StatusHistory();
        rejectionFromValidated.setPolicyRequestId(policyRequest.getId());
        rejectionFromValidated.setPreviousStatus(PolicyStatus.VALIDATED);
        rejectionFromValidated.setNewStatus(PolicyStatus.REJECTED);
        rejectionFromValidated.setChangedAt(changeTime.plusMinutes(15));
        rejectionFromValidated.setReason("Risk assessment failed");

        StatusHistory rejectionFromPending = new StatusHistory();
        rejectionFromPending.setPolicyRequestId(policyRequest.getId());
        rejectionFromPending.setPreviousStatus(PolicyStatus.PENDING);
        rejectionFromPending.setNewStatus(PolicyStatus.REJECTED);
        rejectionFromPending.setChangedAt(changeTime.plusMinutes(30));
        rejectionFromPending.setReason("Credit check failed");

        assertEquals(PolicyStatus.REJECTED, rejectionFromReceived.getNewStatus());
        assertEquals(PolicyStatus.REJECTED, rejectionFromValidated.getNewStatus());
        assertEquals(PolicyStatus.REJECTED, rejectionFromPending.getNewStatus());
    }

    @Test
    void testCancellationTransitions() {
        LocalDateTime changeTime = LocalDateTime.now();

        StatusHistory cancellationFromReceived = new StatusHistory();
        cancellationFromReceived.setPolicyRequestId(policyRequest.getId());
        cancellationFromReceived.setPreviousStatus(PolicyStatus.RECEIVED);
        cancellationFromReceived.setNewStatus(PolicyStatus.CANCELLED);
        cancellationFromReceived.setChangedAt(changeTime);
        cancellationFromReceived.setReason("Customer requested cancellation");

        StatusHistory cancellationFromValidated = new StatusHistory();
        cancellationFromValidated.setPolicyRequestId(policyRequest.getId());
        cancellationFromValidated.setPreviousStatus(PolicyStatus.VALIDATED);
        cancellationFromValidated.setNewStatus(PolicyStatus.CANCELLED);
        cancellationFromValidated.setChangedAt(changeTime.plusMinutes(10));
        cancellationFromValidated.setReason("System error, cancelled by admin");

        assertEquals(PolicyStatus.CANCELLED, cancellationFromReceived.getNewStatus());
        assertEquals(PolicyStatus.CANCELLED, cancellationFromValidated.getNewStatus());
    }

    @Test
    void testStatusHistoryWithDifferentPolicyRequests() {
        PolicyRequest anotherPolicyRequest = new PolicyRequest();
        anotherPolicyRequest.setId(UUID.randomUUID());

        StatusHistory history1 = new StatusHistory();
        history1.setPolicyRequestId(policyRequest.getId());
        history1.setPreviousStatus(PolicyStatus.RECEIVED);
        history1.setNewStatus(PolicyStatus.VALIDATED);
        history1.setChangedAt(LocalDateTime.now());

        StatusHistory history2 = new StatusHistory();
        history2.setPolicyRequestId(anotherPolicyRequest.getId());
        history2.setPreviousStatus(PolicyStatus.RECEIVED);
        history2.setNewStatus(PolicyStatus.VALIDATED);
        history2.setChangedAt(LocalDateTime.now());

        assertNotEquals(history1.getPolicyRequestId(), history2.getPolicyRequestId());
        assertEquals(history1.getPreviousStatus(), history2.getPreviousStatus());
        assertEquals(history1.getNewStatus(), history2.getNewStatus());
    }

    @Test
    void testStatusHistoryTimeOrder() {
        LocalDateTime baseTime = LocalDateTime.now();

        StatusHistory firstChange = new StatusHistory();
        firstChange.setChangedAt(baseTime);
        firstChange.setPreviousStatus(PolicyStatus.RECEIVED);
        firstChange.setNewStatus(PolicyStatus.VALIDATED);

        StatusHistory secondChange = new StatusHistory();
        secondChange.setChangedAt(baseTime.plusMinutes(5));
        secondChange.setPreviousStatus(PolicyStatus.VALIDATED);
        secondChange.setNewStatus(PolicyStatus.PENDING);

        StatusHistory thirdChange = new StatusHistory();
        thirdChange.setChangedAt(baseTime.plusMinutes(10));
        thirdChange.setPreviousStatus(PolicyStatus.PENDING);
        thirdChange.setNewStatus(PolicyStatus.APPROVED);

        assertTrue(firstChange.getChangedAt().isBefore(secondChange.getChangedAt()));
        assertTrue(secondChange.getChangedAt().isBefore(thirdChange.getChangedAt()));
    }

    @Test
    void testStatusHistoryWithSpecialCharactersInReason() {
        StatusHistory history = new StatusHistory();
        history.setPolicyRequestId(policyRequest.getId());
        history.setPreviousStatus(PolicyStatus.RECEIVED);
        history.setNewStatus(PolicyStatus.VALIDATED);
        history.setChangedAt(LocalDateTime.now());
        
        String specialReason = "Razão com acentos e çaracteres especiais: !@#$%^&*()";
        history.setReason(specialReason);

        assertEquals(specialReason, history.getReason());
    }

    @Test
    void testStatusHistoryEquality() {
        StatusHistory history1 = createStatusHistory();
        StatusHistory history2 = createStatusHistory();

        StatusHistory differentHistory = new StatusHistory();
        differentHistory.setPolicyRequestId(policyRequest.getId());
        differentHistory.setPreviousStatus(PolicyStatus.VALIDATED);
        differentHistory.setNewStatus(PolicyStatus.PENDING);
        differentHistory.setChangedAt(LocalDateTime.now());
        differentHistory.setReason("Different reason");

        assertNotEquals(history1, differentHistory);
        
        assertEquals(history1, history2);
        assertEquals(history1.getPreviousStatus(), history2.getPreviousStatus());
        assertEquals(history1.getNewStatus(), history2.getNewStatus());
        assertEquals(history1.getReason(), history2.getReason());
    }

    @Test
    void testStatusHistoryToString() {
        StatusHistory history = createStatusHistory();
        String toString = history.toString();
        
        assertNotNull(toString);
        assertTrue(toString.length() > 0);
    }

    @Test
    void testStatusHistoryHashCode() {
        StatusHistory history = createStatusHistory();
        int hashCode = history.hashCode();
        
        assertEquals(hashCode, history.hashCode());
    }

    @Test
    void testInheritanceFromBaseEntity() {
        StatusHistory history = createStatusHistory();
        assertTrue(history instanceof BaseEntity);
        
        history.setCreatedBy("system");
        history.setUpdatedBy("admin");
        
        assertEquals("system", history.getCreatedBy());
        assertEquals("admin", history.getUpdatedBy());
    }

    @Test
    void testStatusHistoryWithPreciseTimestamp() {
        LocalDateTime preciseTime = LocalDateTime.of(2023, 12, 25, 14, 30, 45, 123456789);
        
        StatusHistory history = new StatusHistory();
        history.setPolicyRequestId(policyRequest.getId());
        history.setPreviousStatus(PolicyStatus.VALIDATED);
        history.setNewStatus(PolicyStatus.PENDING);
        history.setChangedAt(preciseTime);
        history.setReason("Precise timing test");

        assertEquals(preciseTime, history.getChangedAt());
        assertEquals(2023, history.getChangedAt().getYear());
        assertEquals(12, history.getChangedAt().getMonthValue());
        assertEquals(25, history.getChangedAt().getDayOfMonth());
        assertEquals(14, history.getChangedAt().getHour());
        assertEquals(30, history.getChangedAt().getMinute());
        assertEquals(45, history.getChangedAt().getSecond());
    }

    @Test
    void testStatusHistoryBatch() {

        List<StatusHistory> histories = new ArrayList<>();
        PolicyStatus[] statuses = {
                PolicyStatus.RECEIVED,
                PolicyStatus.VALIDATED,
                PolicyStatus.PENDING,
                PolicyStatus.APPROVED
        };

        for (int i = 0; i < statuses.length - 1; i++) {
            StatusHistory history = new StatusHistory();
            history.setPolicyRequestId(policyRequest.getId());
            history.setPreviousStatus(statuses[i]);
            history.setNewStatus(statuses[i + 1]);
            history.setChangedAt(LocalDateTime.now().plusMinutes(i * 5));
            history.setReason("Batch transition " + (i + 1));
            histories.add(history);
        }

        assertEquals(3, histories.size());
        
        assertEquals(PolicyStatus.RECEIVED, histories.get(0).getPreviousStatus());
        assertEquals(PolicyStatus.VALIDATED, histories.get(0).getNewStatus());
        
        assertEquals(PolicyStatus.VALIDATED, histories.get(1).getPreviousStatus());
        assertEquals(PolicyStatus.PENDING, histories.get(1).getNewStatus());
        
        assertEquals(PolicyStatus.PENDING, histories.get(2).getPreviousStatus());
        assertEquals(PolicyStatus.APPROVED, histories.get(2).getNewStatus());
    }

    @Test
    void testValidationWithMissingFields() {
        StatusHistory emptyHistory = new StatusHistory();

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            emptyHistory.validate();
        });
        assertEquals("policyRequestId is required", exception.getMessage());

        emptyHistory.setPolicyRequestId(UUID.randomUUID());
        exception = assertThrows(IllegalArgumentException.class, () -> {
            emptyHistory.validate();
        });
        assertEquals("previousStatus is required", exception.getMessage());

        emptyHistory.setPreviousStatus(PolicyStatus.RECEIVED);
        exception = assertThrows(IllegalArgumentException.class, () -> {
            emptyHistory.validate();
        });
        assertEquals("newStatus is required", exception.getMessage());

        emptyHistory.setNewStatus(PolicyStatus.VALIDATED);
        exception = assertThrows(IllegalArgumentException.class, () -> {
            emptyHistory.validate();
        });
        assertEquals("changedAt is required", exception.getMessage());
    }

    @Test
    void testValidationWithAllRequiredFields() {
        StatusHistory validHistory = new StatusHistory();
        validHistory.setPolicyRequestId(UUID.randomUUID());
        validHistory.setPreviousStatus(PolicyStatus.RECEIVED);
        validHistory.setNewStatus(PolicyStatus.VALIDATED);
        validHistory.setChangedAt(LocalDateTime.now());

        assertDoesNotThrow(() -> validHistory.validate());
    }

    @Test
    void testSetPolicyRequestIdValidation() {
        StatusHistory history = new StatusHistory();
        
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            history.setPolicyRequestId(null);
        });
        assertEquals("policyRequestId cannot be null", exception.getMessage());

        UUID validId = UUID.randomUUID();
        history.setPolicyRequestId(validId);
        assertEquals(validId, history.getPolicyRequestId());
    }

    @Test
    void testSetPreviousStatusValidation() {
        StatusHistory history = new StatusHistory();
        
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            history.setPreviousStatus(null);
        });
        assertEquals("previousStatus cannot be null", exception.getMessage());

        history.setPreviousStatus(PolicyStatus.RECEIVED);
        assertEquals(PolicyStatus.RECEIVED, history.getPreviousStatus());
    }

    @Test
    void testSetNewStatusValidation() {
        StatusHistory history = new StatusHistory();
        
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            history.setNewStatus(null);
        });
        assertEquals("newStatus cannot be null", exception.getMessage());

        history.setPreviousStatus(PolicyStatus.RECEIVED);
        exception = assertThrows(IllegalArgumentException.class, () -> {
            history.setNewStatus(PolicyStatus.RECEIVED);
        });
        assertEquals("newStatus cannot be the same as previousStatus", exception.getMessage());
    }

    @Test
    void testInvalidStatusTransitions() {
        StatusHistory history = new StatusHistory();
        
        history.setPreviousStatus(PolicyStatus.RECEIVED);
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            history.setNewStatus(PolicyStatus.PENDING);
        });
        assertTrue(exception.getMessage().contains("Invalid status transition"));

        history.setPreviousStatus(PolicyStatus.APPROVED);
        exception = assertThrows(IllegalStateException.class, () -> {
            history.setNewStatus(PolicyStatus.PENDING);
        });
        assertTrue(exception.getMessage().contains("Invalid status transition"));

        history.setPreviousStatus(PolicyStatus.REJECTED);
        exception = assertThrows(IllegalStateException.class, () -> {
            history.setNewStatus(PolicyStatus.VALIDATED);
        });
        assertTrue(exception.getMessage().contains("Invalid status transition"));

        history.setPreviousStatus(PolicyStatus.CANCELLED);
        exception = assertThrows(IllegalStateException.class, () -> {
            history.setNewStatus(PolicyStatus.VALIDATED);
        });
        assertTrue(exception.getMessage().contains("Invalid status transition"));
    }

    @Test
    void testValidStatusTransitions() {
        StatusHistory history = new StatusHistory();
        
        history.setPreviousStatus(PolicyStatus.RECEIVED);
        
        history.setNewStatus(PolicyStatus.VALIDATED);
        assertEquals(PolicyStatus.VALIDATED, history.getNewStatus());
        
        history.setPreviousStatus(PolicyStatus.RECEIVED);
        history.setNewStatus(PolicyStatus.REJECTED);
        assertEquals(PolicyStatus.REJECTED, history.getNewStatus());
        
        history.setPreviousStatus(PolicyStatus.RECEIVED);
        history.setNewStatus(PolicyStatus.CANCELLED);
        assertEquals(PolicyStatus.CANCELLED, history.getNewStatus());
    }

    @Test
    void testOnCreateSetsDefaultChangedAt() {
        StatusHistory newHistory = new StatusHistory();
        newHistory.onCreate();
        assertNotNull(newHistory.getChangedAt());
        assertTrue(newHistory.getChangedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    void testOnCreateDoesNotOverrideExistingChangedAt() {
        StatusHistory newHistory = new StatusHistory();
        LocalDateTime specificTime = LocalDateTime.of(2023, 1, 1, 12, 0, 0);
        newHistory.setChangedAt(specificTime);
        newHistory.onCreate();
        assertEquals(specificTime, newHistory.getChangedAt());
    }

    private StatusHistory createStatusHistory() {
        StatusHistory history = new StatusHistory();
        history.setPolicyRequestId(policyRequest.getId());
        history.setPreviousStatus(PolicyStatus.RECEIVED);
        history.setNewStatus(PolicyStatus.VALIDATED);
        history.setChangedAt(LocalDateTime.now());
        history.setReason("Test reason");
        return history;
    }
} 