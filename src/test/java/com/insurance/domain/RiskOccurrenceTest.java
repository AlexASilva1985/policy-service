package com.insurance.domain;

import com.insurance.service.RiskOccurrenceValidationService;
import com.insurance.service.impl.RiskOccurrenceValidationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for RiskOccurrence entity after refactoring to follow Single Responsibility Principle.
 * The entity now only handles JPA mapping, while business logic is tested through RiskOccurrenceValidationService.
 */
class RiskOccurrenceTest {

    private RiskOccurrence riskOccurrence;
    private RiskOccurrenceValidationService validationService;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        validationService = new RiskOccurrenceValidationServiceImpl();
        riskOccurrence = new RiskOccurrence();
        now = LocalDateTime.now();
    }

    // ========== ENTITY MAPPING TESTS ==========

    @Test
    void testCreateValidRiskOccurrence() {
        riskOccurrence.setType("FRAUD");
        riskOccurrence.setDescription("Suspicious activity detected");
        riskOccurrence.setCreatedAt(now.minusHours(1));
        riskOccurrence.setUpdatedAt(now.minusHours(1));

        assertEquals("FRAUD", riskOccurrence.getType());
        assertEquals("Suspicious activity detected", riskOccurrence.getDescription());
        assertEquals(now.minusHours(1), riskOccurrence.getCreatedAt());
        assertEquals(now.minusHours(1), riskOccurrence.getUpdatedAt());
    }

    @Test
    void testEntitySettersAndGetters() {
        RiskOccurrence testOccurrence = new RiskOccurrence();
        
        // Test all setters and getters work without business validation
        testOccurrence.setType(""); // Empty type - no validation in entity
        assertEquals("", testOccurrence.getType());
        
        testOccurrence.setDescription("   "); // Empty description - no validation in entity
        assertEquals("   ", testOccurrence.getDescription());
        
        LocalDateTime futureTime = LocalDateTime.now().plusDays(1); // Future time - no validation in entity
        testOccurrence.setCreatedAt(futureTime);
        testOccurrence.setUpdatedAt(futureTime);
        assertEquals(futureTime, testOccurrence.getCreatedAt());
        assertEquals(futureTime, testOccurrence.getUpdatedAt());
    }

    @Test
    void testPrePersistHook() {
        RiskOccurrence newOccurrence = new RiskOccurrence();
        assertNull(newOccurrence.getCreatedAt());
        assertNull(newOccurrence.getUpdatedAt());
        
        newOccurrence.onCreate();
        assertNotNull(newOccurrence.getCreatedAt());
        assertNotNull(newOccurrence.getUpdatedAt());
        assertEquals(newOccurrence.getCreatedAt(), newOccurrence.getUpdatedAt());
    }

    @Test
    void testPreUpdateHook() {
        riskOccurrence.setCreatedAt(now.minusHours(1));
        riskOccurrence.setUpdatedAt(now.minusHours(1));
        
        LocalDateTime originalUpdatedAt = riskOccurrence.getUpdatedAt();
        
        // Simulate a small delay
        try { Thread.sleep(10); } catch (InterruptedException e) { /* ignore */ }
        
        riskOccurrence.onUpdate();
        assertTrue(riskOccurrence.getUpdatedAt().isAfter(originalUpdatedAt));
    }

    // ========== BUSINESS LOGIC TESTS (Via RiskOccurrenceValidationService) ==========

    @Test
    void testTypeValidationViaService() {
        // Valid types
        assertDoesNotThrow(() -> validationService.validateType("FRAUD"));
        assertDoesNotThrow(() -> validationService.validateType("SUSPICIOUS_ACTIVITY"));
        
        // Invalid types
        assertThrows(IllegalArgumentException.class, () -> 
            validationService.validateType(null));
        assertThrows(IllegalArgumentException.class, () -> 
            validationService.validateType(""));
        assertThrows(IllegalArgumentException.class, () -> 
            validationService.validateType("   "));
    }

    @Test
    void testDescriptionValidationViaService() {
        // Valid descriptions
        assertDoesNotThrow(() -> validationService.validateDescription("Valid description"));
        assertDoesNotThrow(() -> validationService.validateDescription("Another valid description"));
        
        // Invalid descriptions
        assertThrows(IllegalArgumentException.class, () -> 
            validationService.validateDescription(null));
        assertThrows(IllegalArgumentException.class, () -> 
            validationService.validateDescription(""));
        assertThrows(IllegalArgumentException.class, () -> 
            validationService.validateDescription("   "));
    }

    @Test
    void testTimestampValidationViaService() {
        // Valid timestamps
        assertDoesNotThrow(() -> validationService.validateCreatedAt(now.minusHours(1)));
        assertDoesNotThrow(() -> validationService.validateUpdatedAt(now.minusHours(1), now.minusHours(2)));
        
        // Invalid timestamps
        assertThrows(IllegalArgumentException.class, () -> 
            validationService.validateCreatedAt(null));
        assertThrows(IllegalArgumentException.class, () -> 
            validationService.validateCreatedAt(now.plusHours(1))); // Future time
            
        assertThrows(IllegalArgumentException.class, () -> 
            validationService.validateUpdatedAt(null, now.minusHours(1)));
        assertThrows(IllegalArgumentException.class, () -> 
            validationService.validateUpdatedAt(now.minusHours(1), now)); // Updated before created
    }

    @Test
    void testCompleteValidationViaService() {
        // Valid risk occurrence
        riskOccurrence.setType("FRAUD");
        riskOccurrence.setDescription("Suspicious activity detected");
        riskOccurrence.setCreatedAt(now.minusHours(1));
        riskOccurrence.setUpdatedAt(now.minusHours(1));
        
        assertDoesNotThrow(() -> validationService.validateRiskOccurrence(riskOccurrence));
        
        // Invalid risk occurrence - missing required fields
        RiskOccurrence invalidOccurrence = new RiskOccurrence();
        assertThrows(IllegalArgumentException.class, () -> 
            validationService.validateRiskOccurrence(invalidOccurrence));
    }

    // ========== INTEGRATION TESTS ==========

    @Test
    void testCompleteWorkflowWithService() {
        // Build valid risk occurrence step by step with validation
        RiskOccurrence workflowOccurrence = new RiskOccurrence();
        workflowOccurrence.setType("IDENTITY_THEFT");
        workflowOccurrence.setDescription("Customer identity documents could not be verified");
        workflowOccurrence.setCreatedAt(now.minusDays(1));
        workflowOccurrence.setUpdatedAt(now.minusHours(1));
        
        // Validate individual components
        assertDoesNotThrow(() -> validationService.validateType(workflowOccurrence.getType()));
        assertDoesNotThrow(() -> validationService.validateDescription(workflowOccurrence.getDescription()));
        assertDoesNotThrow(() -> validationService.validateCreatedAt(workflowOccurrence.getCreatedAt()));
        assertDoesNotThrow(() -> validationService.validateUpdatedAt(workflowOccurrence.getUpdatedAt(), workflowOccurrence.getCreatedAt()));
        
        // Validate complete occurrence
        assertDoesNotThrow(() -> validationService.validateRiskOccurrence(workflowOccurrence));
    }

    @Test
    void testEntityInheritanceFromBaseEntity() {
        // Test inheritance structure
        assertTrue(riskOccurrence instanceof BaseEntity);
        
        // Test that we can set/get BaseEntity fields
        UUID testId = UUID.randomUUID();
        riskOccurrence.setId(testId);
        assertEquals(testId, riskOccurrence.getId());
        
        String testUser = "test-user";
        riskOccurrence.setCreatedBy(testUser);
        assertEquals(testUser, riskOccurrence.getCreatedBy());
        
        riskOccurrence.setUpdatedBy(testUser);
        assertEquals(testUser, riskOccurrence.getUpdatedBy());
    }

    @Test
    void testVariousRiskOccurrenceTypes() {
        String[] riskTypes = {
            "FRAUD", "IDENTITY_THEFT", "MONEY_LAUNDERING", 
            "SUSPICIOUS_ACTIVITY", "DOCUMENT_FORGERY", "CREDIT_ABUSE"
        };
        
        for (String type : riskTypes) {
            RiskOccurrence testOccurrence = new RiskOccurrence();
            testOccurrence.setType(type);
            assertEquals(type, testOccurrence.getType());
            
            // Validate through service
            assertDoesNotThrow(() -> validationService.validateType(type));
        }
    }
} 