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
import com.insurance.service.StatusHistoryValidationService;
import com.insurance.service.impl.StatusHistoryValidationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test class for StatusHistory entity after refactoring to follow Single Responsibility Principle.
 * The entity now only handles JPA mapping, while business logic is tested through StatusHistoryValidationService.
 */
class StatusHistoryTest {

    private StatusHistory statusHistory;
    private StatusHistoryValidationService validationService;
    private UUID policyRequestId;
    private PolicyRequest policyRequest;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        validationService = new StatusHistoryValidationServiceImpl();
        policyRequestId = UUID.randomUUID();
        statusHistory = new StatusHistory();
        policyRequest = new PolicyRequest();
        policyRequest.setId(UUID.randomUUID());
        now = LocalDateTime.now();
    }

    // ========== ENTITY MAPPING TESTS ==========

    @Test
    void testCreateStatusHistoryWithCorrectData() {
        statusHistory.setPolicyRequestId(policyRequestId);
        statusHistory.setPreviousStatus(PolicyStatus.RECEIVED);
        statusHistory.setNewStatus(PolicyStatus.VALIDATED);
        statusHistory.setChangedAt(now);

        assertNotNull(statusHistory);
        assertEquals(policyRequestId, statusHistory.getPolicyRequestId());
        assertEquals(PolicyStatus.RECEIVED, statusHistory.getPreviousStatus());
        assertEquals(PolicyStatus.VALIDATED, statusHistory.getNewStatus());
        assertEquals(now, statusHistory.getChangedAt());
    }

    @Test
    void testEntitySettersAndGetters() {
        StatusHistory testHistory = new StatusHistory();
        
        // Test all setters and getters work without business validation
        testHistory.setPolicyRequestId(null); // Null values - no validation in entity
        assertNull(testHistory.getPolicyRequestId());
        
        testHistory.setPreviousStatus(PolicyStatus.REJECTED);
        assertEquals(PolicyStatus.REJECTED, testHistory.getPreviousStatus());
        
        testHistory.setNewStatus(PolicyStatus.REJECTED); // Same status - no validation in entity
        assertEquals(PolicyStatus.REJECTED, testHistory.getNewStatus());
        
        LocalDateTime futureTime = LocalDateTime.now().plusDays(1);
        testHistory.setChangedAt(futureTime);
        assertEquals(futureTime, testHistory.getChangedAt());
        
        testHistory.setReason("Test reason");
        assertEquals("Test reason", testHistory.getReason());
    }

    @Test
    void testPrePersistHook() {
        StatusHistory newHistory = new StatusHistory();
        assertNull(newHistory.getChangedAt());
        
        newHistory.onCreate();
        assertNotNull(newHistory.getChangedAt());
        assertTrue(newHistory.getChangedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
        assertTrue(newHistory.getChangedAt().isAfter(LocalDateTime.now().minusSeconds(1)));
    }

    @Test
    void testStatusHistoryWithAllFields() {
        StatusHistory history = new StatusHistory();
        history.setPolicyRequestId(policyRequest.getId());
        history.setPreviousStatus(PolicyStatus.RECEIVED);
        history.setNewStatus(PolicyStatus.VALIDATED);
        history.setChangedAt(now);
        history.setReason("Validation completed successfully");

        assertEquals(policyRequest.getId(), history.getPolicyRequestId());
        assertEquals(PolicyStatus.RECEIVED, history.getPreviousStatus());
        assertEquals(PolicyStatus.VALIDATED, history.getNewStatus());
        assertEquals(now, history.getChangedAt());
        assertEquals("Validation completed successfully", history.getReason());
    }

    @Test
    void testStatusHistoryWithNullReason() {
        StatusHistory history = new StatusHistory();
        history.setPolicyRequestId(policyRequest.getId());
        history.setPreviousStatus(PolicyStatus.VALIDATED);
        history.setNewStatus(PolicyStatus.PENDING);
        history.setChangedAt(now);
        history.setReason(null);

        assertNull(history.getReason());
    }

    @Test
    void testStatusHistoryWithEmptyReason() {
        StatusHistory history = new StatusHistory();
        history.setPolicyRequestId(policyRequest.getId());
        history.setPreviousStatus(PolicyStatus.PENDING);
        history.setNewStatus(PolicyStatus.APPROVED);
        history.setChangedAt(now);
        history.setReason("");

        assertEquals("", history.getReason());
    }

    // ========== BUSINESS LOGIC TESTS (Via StatusHistoryValidationService) ==========

    @Test
    void testValidationViaService() {
        // Valid status history
        statusHistory.setPolicyRequestId(policyRequestId);
        statusHistory.setPreviousStatus(PolicyStatus.RECEIVED);
        statusHistory.setNewStatus(PolicyStatus.VALIDATED);
        statusHistory.setChangedAt(now);

        assertDoesNotThrow(() -> validationService.validateStatusHistory(statusHistory));
        
        // Invalid status history - missing required fields
        StatusHistory invalidHistory = new StatusHistory();
        assertThrows(IllegalArgumentException.class, () -> 
            validationService.validateStatusHistory(invalidHistory));
    }

    @Test
    void testStatusTransitionValidationViaService() {
        // Valid transitions
        assertDoesNotThrow(() -> 
            validationService.validateStatusTransition(PolicyStatus.RECEIVED, PolicyStatus.VALIDATED));
        assertDoesNotThrow(() -> 
            validationService.validateStatusTransition(PolicyStatus.VALIDATED, PolicyStatus.PENDING));
        assertDoesNotThrow(() -> 
            validationService.validateStatusTransition(PolicyStatus.PENDING, PolicyStatus.APPROVED));

        // Invalid transitions
        assertThrows(IllegalStateException.class, () -> 
            validationService.validateStatusTransition(PolicyStatus.REJECTED, PolicyStatus.PENDING));
        assertThrows(IllegalStateException.class, () -> 
            validationService.validateStatusTransition(PolicyStatus.APPROVED, PolicyStatus.VALIDATED));
    }

    @Test
    void testStatusDifferenceValidationViaService() {
        // Valid - different statuses
        assertDoesNotThrow(() -> 
            validationService.validateStatusDifference(PolicyStatus.RECEIVED, PolicyStatus.VALIDATED));
        
        // Invalid - same status
        assertThrows(IllegalArgumentException.class, () -> 
            validationService.validateStatusDifference(PolicyStatus.RECEIVED, PolicyStatus.RECEIVED));
    }

    @Test
    void testCreateStatusHistoryViaService() {
        StatusHistory createdHistory = validationService.createStatusHistory(
                policyRequestId, PolicyStatus.RECEIVED, PolicyStatus.VALIDATED, "Test reason");

        assertEquals(policyRequestId, createdHistory.getPolicyRequestId());
        assertEquals(PolicyStatus.RECEIVED, createdHistory.getPreviousStatus());
        assertEquals(PolicyStatus.VALIDATED, createdHistory.getNewStatus());
        assertEquals("Test reason", createdHistory.getReason());
        assertNotNull(createdHistory.getChangedAt());
    }

    @Test
    void testFieldValidationsViaService() {
        // Policy request ID validation
        assertDoesNotThrow(() -> validationService.validatePolicyRequestId(policyRequestId));
        assertThrows(IllegalArgumentException.class, () -> 
            validationService.validatePolicyRequestId(null));

        // Status validations
        assertDoesNotThrow(() -> validationService.validatePreviousStatus(PolicyStatus.RECEIVED));
        assertThrows(IllegalArgumentException.class, () -> 
            validationService.validatePreviousStatus(null));

        assertDoesNotThrow(() -> validationService.validateNewStatus(PolicyStatus.VALIDATED));
        assertThrows(IllegalArgumentException.class, () -> 
            validationService.validateNewStatus(null));

        // Timestamp validation
        assertDoesNotThrow(() -> validationService.validateChangedAt(now));
        assertThrows(IllegalArgumentException.class, () -> 
            validationService.validateChangedAt(null));
    }

    // ========== INTEGRATION TESTS ==========

    @Test
    void testCompleteWorkflowWithService() {
        // Create and validate a complete status history workflow
        StatusHistory history1 = validationService.createStatusHistory(
                policyRequestId, PolicyStatus.RECEIVED, PolicyStatus.VALIDATED, "Document validation completed");

        StatusHistory history2 = validationService.createStatusHistory(
                policyRequestId, PolicyStatus.VALIDATED, PolicyStatus.PENDING, "Moved to pending for review");

        StatusHistory history3 = validationService.createStatusHistory(
                policyRequestId, PolicyStatus.PENDING, PolicyStatus.APPROVED, "All requirements met");

        // Validate all histories
        assertDoesNotThrow(() -> validationService.validateStatusHistory(history1));
        assertDoesNotThrow(() -> validationService.validateStatusHistory(history2));
        assertDoesNotThrow(() -> validationService.validateStatusHistory(history3));

        assertEquals(PolicyStatus.VALIDATED, history1.getNewStatus());
        assertEquals(PolicyStatus.PENDING, history2.getNewStatus());
        assertEquals(PolicyStatus.APPROVED, history3.getNewStatus());
    }

    @Test
    void testEntityInheritanceFromBaseEntity() {
        // Test inheritance structure
        assertTrue(statusHistory instanceof BaseEntity);
        
        // Test that we can set/get BaseEntity fields
        UUID testId = UUID.randomUUID();
        statusHistory.setId(testId);
        assertEquals(testId, statusHistory.getId());
        
        String testUser = "test-user";
        statusHistory.setCreatedBy(testUser);
        assertEquals(testUser, statusHistory.getCreatedBy());
        
        statusHistory.setUpdatedBy(testUser);
        assertEquals(testUser, statusHistory.getUpdatedBy());
    }

    @Test
    void testAllPolicyStatusTypes() {
        for (PolicyStatus status : PolicyStatus.values()) {
            StatusHistory testHistory = new StatusHistory();
            testHistory.setPreviousStatus(status);
            testHistory.setNewStatus(status); // Entity allows same status
            
            assertEquals(status, testHistory.getPreviousStatus());
            assertEquals(status, testHistory.getNewStatus());
        }
    }

    @Test
    void testUpdateStatusHistoryViaService() {
        statusHistory.setPolicyRequestId(policyRequestId);
        statusHistory.setPreviousStatus(PolicyStatus.RECEIVED);
        statusHistory.setNewStatus(PolicyStatus.VALIDATED);
        statusHistory.setChangedAt(now);

        // Update via service with validation
        assertDoesNotThrow(() -> validationService.updateStatusHistory(
                statusHistory, null, PolicyStatus.VALIDATED, PolicyStatus.PENDING, null, "Updated reason"));

        assertEquals(PolicyStatus.VALIDATED, statusHistory.getPreviousStatus());
        assertEquals(PolicyStatus.PENDING, statusHistory.getNewStatus());
        assertEquals("Updated reason", statusHistory.getReason());
    }

    @Test
    void testStatusHistoryWithVariousReasonLengths() {
        StatusHistory reasonHistory = new StatusHistory();
        reasonHistory.setPolicyRequestId(policyRequestId);
        reasonHistory.setPreviousStatus(PolicyStatus.RECEIVED);
        reasonHistory.setNewStatus(PolicyStatus.VALIDATED);
        reasonHistory.setChangedAt(now);
        
        // Short reason
        reasonHistory.setReason("OK");
        assertEquals("OK", reasonHistory.getReason());
        
        // Medium reason
        String mediumReason = "Document validation completed successfully";
        reasonHistory.setReason(mediumReason);
        assertEquals(mediumReason, reasonHistory.getReason());
        
        // Long reason
        String longReason = "This is a very detailed reason that explains in great detail why the status was changed. " +
                           "It includes multiple aspects of the decision process, references to documentation, " +
                           "compliance requirements, and additional notes from the review process. " +
                           "The reason also contains technical details and references to external systems.";
        reasonHistory.setReason(longReason);
        assertEquals(longReason, reasonHistory.getReason());
        assertTrue(reasonHistory.getReason().length() > 200);
    }

    @Test
    void testStatusHistoryWithSpecialCharactersInReason() {
        StatusHistory specialHistory = new StatusHistory();
        specialHistory.setPolicyRequestId(policyRequestId);
        specialHistory.setPreviousStatus(PolicyStatus.PENDING);
        specialHistory.setNewStatus(PolicyStatus.APPROVED);
        specialHistory.setChangedAt(now);
        
        String specialReason = "Approved with conditions: !@#$%^&*()[]{}|;':\",./<>?~`";
        specialHistory.setReason(specialReason);
        assertEquals(specialReason, specialHistory.getReason());
    }

    @Test
    void testStatusHistoryWithUTF8CharactersInReason() {
        StatusHistory utf8History = new StatusHistory();
        utf8History.setPolicyRequestId(policyRequestId);
        utf8History.setPreviousStatus(PolicyStatus.VALIDATED);
        utf8History.setNewStatus(PolicyStatus.REJECTED);
        utf8History.setChangedAt(now);
        
        String utf8Reason = "Rechazado: documentación inválida. Причина: недостаточно данных. 理由：文档不完整";
        utf8History.setReason(utf8Reason);
        assertEquals(utf8Reason, utf8History.getReason());
    }

    @Test
    void testStatusHistoryPreciseTimestamps() {
        StatusHistory preciseHistory = new StatusHistory();
        preciseHistory.setPolicyRequestId(policyRequestId);
        preciseHistory.setPreviousStatus(PolicyStatus.RECEIVED);
        preciseHistory.setNewStatus(PolicyStatus.VALIDATED);
        
        LocalDateTime preciseTime = LocalDateTime.of(2023, 12, 25, 14, 30, 45, 123456789);
        preciseHistory.setChangedAt(preciseTime);
        
        assertEquals(preciseTime, preciseHistory.getChangedAt());
        assertEquals(2023, preciseHistory.getChangedAt().getYear());
        assertEquals(12, preciseHistory.getChangedAt().getMonthValue());
        assertEquals(25, preciseHistory.getChangedAt().getDayOfMonth());
        assertEquals(14, preciseHistory.getChangedAt().getHour());
        assertEquals(30, preciseHistory.getChangedAt().getMinute());
        assertEquals(45, preciseHistory.getChangedAt().getSecond());
        assertEquals(123456789, preciseHistory.getChangedAt().getNano());
    }

    @Test
    void testStatusHistoryWorkflowSequence() {
        PolicyStatus[] workflowSequence = {
            PolicyStatus.RECEIVED,
            PolicyStatus.VALIDATED,
            PolicyStatus.PENDING,
            PolicyStatus.APPROVED
        };
        
        for (int i = 0; i < workflowSequence.length - 1; i++) {
            StatusHistory workflowHistory = validationService.createStatusHistory(
                policyRequestId, 
                workflowSequence[i], 
                workflowSequence[i + 1], 
                "Workflow step " + (i + 1)
            );
            
            assertEquals(policyRequestId, workflowHistory.getPolicyRequestId());
            assertEquals(workflowSequence[i], workflowHistory.getPreviousStatus());
            assertEquals(workflowSequence[i + 1], workflowHistory.getNewStatus());
            assertEquals("Workflow step " + (i + 1), workflowHistory.getReason());
            assertNotNull(workflowHistory.getChangedAt());
        }
    }

    @Test
    void testStatusHistoryRejectionWorkflows() {
        PolicyStatus[] rejectionPoints = {PolicyStatus.RECEIVED, PolicyStatus.VALIDATED, PolicyStatus.PENDING};
        
        for (PolicyStatus fromStatus : rejectionPoints) {
            StatusHistory rejectionHistory = validationService.createStatusHistory(
                policyRequestId,
                fromStatus,
                PolicyStatus.REJECTED,
                "Rejected from " + fromStatus.name()
            );
            
            assertEquals(fromStatus, rejectionHistory.getPreviousStatus());
            assertEquals(PolicyStatus.REJECTED, rejectionHistory.getNewStatus());
            assertTrue(rejectionHistory.getReason().contains("Rejected"));
        }
    }

    @Test
    void testStatusHistoryCancellationWorkflows() {
        PolicyStatus[] cancellationPoints = {PolicyStatus.RECEIVED, PolicyStatus.VALIDATED, PolicyStatus.PENDING};
        
        for (PolicyStatus fromStatus : cancellationPoints) {
            StatusHistory cancellationHistory = validationService.createStatusHistory(
                policyRequestId,
                fromStatus,
                PolicyStatus.CANCELLED,
                "Cancelled from " + fromStatus.name()
            );
            
            assertEquals(fromStatus, cancellationHistory.getPreviousStatus());
            assertEquals(PolicyStatus.CANCELLED, cancellationHistory.getNewStatus());
            assertTrue(cancellationHistory.getReason().contains("Cancelled"));
        }
    }

    @Test
    void testStatusHistoryEqualsAndHashCode() {
        StatusHistory history1 = new StatusHistory();
        history1.setId(UUID.randomUUID());
        history1.setPolicyRequestId(policyRequestId);
        history1.setPreviousStatus(PolicyStatus.RECEIVED);
        history1.setNewStatus(PolicyStatus.VALIDATED);
        history1.setChangedAt(now);
        history1.setReason("Test");

        StatusHistory history2 = new StatusHistory();
        history2.setId(history1.getId());
        history2.setPolicyRequestId(policyRequestId);
        history2.setPreviousStatus(PolicyStatus.RECEIVED);
        history2.setNewStatus(PolicyStatus.VALIDATED);
        history2.setChangedAt(now);
        history2.setReason("Test");

        assertEquals(history1, history2);
        assertEquals(history1.hashCode(), history2.hashCode());
    }

    @Test
    void testStatusHistoryToString() {
        statusHistory.setPolicyRequestId(policyRequestId);
        statusHistory.setPreviousStatus(PolicyStatus.RECEIVED);
        statusHistory.setNewStatus(PolicyStatus.VALIDATED);
        statusHistory.setChangedAt(now);
        statusHistory.setReason("Test toString");
        
        String toString = statusHistory.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("StatusHistory"));
        assertTrue(toString.contains("RECEIVED"));
        assertTrue(toString.contains("VALIDATED"));
    }

    @Test
    void testStatusHistoryBatchCreation() {
        UUID[] policyIds = {
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID()
        };
        
        for (int i = 0; i < policyIds.length; i++) {
            StatusHistory batchHistory = validationService.createStatusHistory(
                policyIds[i],
                PolicyStatus.RECEIVED,
                PolicyStatus.VALIDATED,
                "Batch validation " + (i + 1)
            );
            
            assertEquals(policyIds[i], batchHistory.getPolicyRequestId());
            assertEquals(PolicyStatus.RECEIVED, batchHistory.getPreviousStatus());
            assertEquals(PolicyStatus.VALIDATED, batchHistory.getNewStatus());
            assertTrue(batchHistory.getReason().contains("Batch"));
        }
    }

    @Test
    void testStatusHistoryWithDifferentTimestamps() {
        LocalDateTime[] testTimes = {
            LocalDateTime.of(2020, 1, 1, 0, 0, 0),
            LocalDateTime.of(2023, 6, 15, 12, 30, 45),
            LocalDateTime.of(2023, 12, 31, 23, 59, 59),
            now.minusYears(1),
            now.minusMonths(6),
            now.minusDays(1),
            now.minusHours(1),
            now.minusMinutes(30)
        };
        
        for (LocalDateTime testTime : testTimes) {
            StatusHistory timeHistory = new StatusHistory();
            timeHistory.setPolicyRequestId(policyRequestId);
            timeHistory.setPreviousStatus(PolicyStatus.RECEIVED);
            timeHistory.setNewStatus(PolicyStatus.VALIDATED);
            timeHistory.setChangedAt(testTime);
            timeHistory.setReason("Time test");
            
            assertEquals(testTime, timeHistory.getChangedAt());
            assertDoesNotThrow(() -> validationService.validateStatusHistory(timeHistory));
        }
    }

    @Test
    void testStatusHistoryValidationEdgeCases() {
        // Test null policy request ID
        StatusHistory nullPolicyId = new StatusHistory();
        nullPolicyId.setPreviousStatus(PolicyStatus.RECEIVED);
        nullPolicyId.setNewStatus(PolicyStatus.VALIDATED);
        nullPolicyId.setChangedAt(now);
        assertThrows(IllegalArgumentException.class, () -> 
            validationService.validateStatusHistory(nullPolicyId));
        
        // Test null previous status
        StatusHistory nullPrevious = new StatusHistory();
        nullPrevious.setPolicyRequestId(policyRequestId);
        nullPrevious.setNewStatus(PolicyStatus.VALIDATED);
        nullPrevious.setChangedAt(now);
        assertThrows(IllegalArgumentException.class, () -> 
            validationService.validateStatusHistory(nullPrevious));
        
        // Test null new status
        StatusHistory nullNew = new StatusHistory();
        nullNew.setPolicyRequestId(policyRequestId);
        nullNew.setPreviousStatus(PolicyStatus.RECEIVED);
        nullNew.setChangedAt(now);
        assertThrows(IllegalArgumentException.class, () -> 
            validationService.validateStatusHistory(nullNew));
        
        // Test null changed at
        StatusHistory nullChanged = new StatusHistory();
        nullChanged.setPolicyRequestId(policyRequestId);
        nullChanged.setPreviousStatus(PolicyStatus.RECEIVED);
        nullChanged.setNewStatus(PolicyStatus.VALIDATED);
        assertThrows(IllegalArgumentException.class, () -> 
            validationService.validateStatusHistory(nullChanged));
    }

    @Test
    void testStatusHistoryComplexUpdateScenarios() {
        statusHistory.setPolicyRequestId(policyRequestId);
        statusHistory.setPreviousStatus(PolicyStatus.RECEIVED);
        statusHistory.setNewStatus(PolicyStatus.VALIDATED);
        statusHistory.setChangedAt(now);
        statusHistory.setReason("Initial");
        
        // Update policy ID
        UUID newPolicyId = UUID.randomUUID();
        assertDoesNotThrow(() -> validationService.updateStatusHistory(
            statusHistory, newPolicyId, null, null, null, null));
        assertEquals(newPolicyId, statusHistory.getPolicyRequestId());
        
        // Update timestamps
        LocalDateTime newTime = now.plusHours(1);
        assertDoesNotThrow(() -> validationService.updateStatusHistory(
            statusHistory, null, null, null, newTime, null));
        assertEquals(newTime, statusHistory.getChangedAt());
        
        // Update reason
        assertDoesNotThrow(() -> validationService.updateStatusHistory(
            statusHistory, null, null, null, null, "Updated reason"));
        assertEquals("Updated reason", statusHistory.getReason());
        
        // Update status transition
        assertDoesNotThrow(() -> validationService.updateStatusHistory(
            statusHistory, null, PolicyStatus.VALIDATED, PolicyStatus.PENDING, null, null));
        assertEquals(PolicyStatus.VALIDATED, statusHistory.getPreviousStatus());
        assertEquals(PolicyStatus.PENDING, statusHistory.getNewStatus());
    }

    @Test
    void testStatusHistoryWithBaseEntityFields() {
        statusHistory.setPolicyRequestId(policyRequestId);
        statusHistory.setPreviousStatus(PolicyStatus.RECEIVED);
        statusHistory.setNewStatus(PolicyStatus.VALIDATED);
        statusHistory.setChangedAt(now);
        
        // Test BaseEntity field inheritance
        UUID testId = UUID.randomUUID();
        statusHistory.setId(testId);
        assertEquals(testId, statusHistory.getId());
        
        String creator = "status-service";
        statusHistory.setCreatedBy(creator);
        assertEquals(creator, statusHistory.getCreatedBy());
        
        String updater = "admin-user";
        statusHistory.setUpdatedBy(updater);
        assertEquals(updater, statusHistory.getUpdatedBy());
        
        LocalDateTime createdAt = now.minusHours(2);
        statusHistory.setCreatedAt(createdAt);
        assertEquals(createdAt, statusHistory.getCreatedAt());
        
        LocalDateTime updatedAt = now.minusHours(1);
        statusHistory.setUpdatedAt(updatedAt);
        assertEquals(updatedAt, statusHistory.getUpdatedAt());
    }
} 