package com.insurance.domain;

import com.insurance.domain.enums.CustomerRiskType;
import com.insurance.service.RiskAnalysisValidationService;
import com.insurance.service.impl.RiskAnalysisValidationServiceImpl;
import com.insurance.service.impl.RiskOccurrenceValidationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for RiskAnalysis entity after refactoring to follow Single Responsibility Principle.
 * The entity now only handles JPA mapping, while business logic is tested through RiskAnalysisValidationService.
 */
class RiskAnalysisTest {

    private RiskAnalysis riskAnalysis;
    private RiskAnalysisValidationService validationService;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        // Create validation service with its dependency
        RiskOccurrenceValidationServiceImpl occurrenceValidationService = new RiskOccurrenceValidationServiceImpl();
        validationService = new RiskAnalysisValidationServiceImpl(occurrenceValidationService);
        
        riskAnalysis = new RiskAnalysis();
        now = LocalDateTime.now();
    }

    // ========== ENTITY MAPPING TESTS ==========

    @Test
    void testCreateValidRiskAnalysis() {
        riskAnalysis.setClassification(CustomerRiskType.REGULAR);
        riskAnalysis.setAnalyzedAt(now.minusHours(1));

        assertEquals(CustomerRiskType.REGULAR, riskAnalysis.getClassification());
        assertEquals(now.minusHours(1), riskAnalysis.getAnalyzedAt());
        assertNotNull(riskAnalysis.getOccurrences());
        assertTrue(riskAnalysis.getOccurrences().isEmpty());
    }

    @Test
    void testEntitySettersAndGetters() {
        RiskAnalysis testAnalysis = new RiskAnalysis();
        
        // Test all setters and getters work without business validation
        testAnalysis.setClassification(CustomerRiskType.HIGH_RISK);
        assertEquals(CustomerRiskType.HIGH_RISK, testAnalysis.getClassification());
        
        LocalDateTime futureTime = LocalDateTime.now().plusDays(1); // Future time - no validation in entity
        testAnalysis.setAnalyzedAt(futureTime);
        assertEquals(futureTime, testAnalysis.getAnalyzedAt());
        
        RiskOccurrence occurrence = new RiskOccurrence();
        testAnalysis.getOccurrences().add(occurrence);
        assertEquals(1, testAnalysis.getOccurrences().size());
        assertTrue(testAnalysis.getOccurrences().contains(occurrence));
    }

    // ========== BUSINESS LOGIC TESTS (Via RiskAnalysisValidationService) ==========

    @Test
    void testClassificationValidationViaService() {
        // Valid classifications
        assertDoesNotThrow(() -> validationService.validateClassification(CustomerRiskType.REGULAR));
        assertDoesNotThrow(() -> validationService.validateClassification(CustomerRiskType.HIGH_RISK));
        assertDoesNotThrow(() -> validationService.validateClassification(CustomerRiskType.PREFERRED));
        
        // Invalid classification
        assertThrows(IllegalArgumentException.class, () -> 
            validationService.validateClassification(null));
    }

    @Test
    void testAnalyzedAtValidationViaService() {
        // Valid timestamps
        assertDoesNotThrow(() -> validationService.validateAnalyzedAt(now.minusHours(1)));
        assertDoesNotThrow(() -> validationService.validateAnalyzedAt(now.minusDays(1)));
        
        // Invalid timestamps
        assertThrows(IllegalArgumentException.class, () -> 
            validationService.validateAnalyzedAt(null));
        assertThrows(IllegalArgumentException.class, () -> 
            validationService.validateAnalyzedAt(now.plusHours(1))); // Future time
    }

    @Test
    void testOccurrenceManagementViaService() {
        riskAnalysis.setClassification(CustomerRiskType.REGULAR);
        riskAnalysis.setAnalyzedAt(now.minusHours(1));
        
        RiskOccurrence occurrence = new RiskOccurrence();
        occurrence.setType("FRAUD");
        occurrence.setDescription("Suspicious activity detected");
        occurrence.setCreatedAt(now.minusHours(2));
        occurrence.setUpdatedAt(now.minusHours(2));
        
        // Test adding occurrence
        assertDoesNotThrow(() -> validationService.addOccurrence(riskAnalysis, occurrence));
        assertEquals(1, riskAnalysis.getOccurrences().size());
        assertTrue(riskAnalysis.getOccurrences().contains(occurrence));
        
        // Test removing occurrence
        assertDoesNotThrow(() -> validationService.removeOccurrence(riskAnalysis, occurrence));
        assertEquals(0, riskAnalysis.getOccurrences().size());
        assertFalse(riskAnalysis.getOccurrences().contains(occurrence));
    }

    @Test
    void testOccurrenceValidationViaService() {
        riskAnalysis.setClassification(CustomerRiskType.REGULAR);
        riskAnalysis.setAnalyzedAt(now.minusHours(1));
        
        // Invalid occurrences
        assertThrows(IllegalArgumentException.class, () -> 
            validationService.addOccurrence(riskAnalysis, null));
        
        RiskOccurrence invalidOccurrence = new RiskOccurrence();
        // Missing required fields - should fail validation
        assertThrows(IllegalArgumentException.class, () -> 
            validationService.addOccurrence(riskAnalysis, invalidOccurrence));
    }

    @Test
    void testCompleteValidationViaService() {
        // Valid risk analysis
        riskAnalysis.setClassification(CustomerRiskType.REGULAR);
        riskAnalysis.setAnalyzedAt(now.minusHours(1));
        
        assertDoesNotThrow(() -> validationService.validateRiskAnalysis(riskAnalysis));
        
        // Invalid risk analysis - missing required fields
        RiskAnalysis invalidAnalysis = new RiskAnalysis();
        assertThrows(IllegalArgumentException.class, () -> 
            validationService.validateRiskAnalysis(invalidAnalysis));
    }

    // ========== INTEGRATION TESTS ==========

    @Test
    void testCompleteWorkflowWithService() {
        // Build valid risk analysis step by step with validation
        RiskAnalysis workflowAnalysis = new RiskAnalysis();
        workflowAnalysis.setClassification(CustomerRiskType.HIGH_RISK);
        workflowAnalysis.setAnalyzedAt(now.minusHours(1));
        
        // Validate individual components
        assertDoesNotThrow(() -> validationService.validateClassification(workflowAnalysis.getClassification()));
        assertDoesNotThrow(() -> validationService.validateAnalyzedAt(workflowAnalysis.getAnalyzedAt()));
        
        // Add occurrences with validation
        RiskOccurrence occurrence1 = new RiskOccurrence();
        occurrence1.setType("FRAUD");
        occurrence1.setDescription("Previous fraud claim");
        occurrence1.setCreatedAt(now.minusDays(1));
        occurrence1.setUpdatedAt(now.minusDays(1));
        
        RiskOccurrence occurrence2 = new RiskOccurrence();
        occurrence2.setType("SUSPICIOUS_ACTIVITY");
        occurrence2.setDescription("Multiple claims in short period");
        occurrence2.setCreatedAt(now.minusHours(2));
        occurrence2.setUpdatedAt(now.minusHours(2));
        
        assertDoesNotThrow(() -> validationService.addOccurrence(workflowAnalysis, occurrence1));
        assertDoesNotThrow(() -> validationService.addOccurrence(workflowAnalysis, occurrence2));
        
        assertEquals(2, workflowAnalysis.getOccurrences().size());
        
        // Validate complete analysis
        assertDoesNotThrow(() -> validationService.validateRiskAnalysis(workflowAnalysis));
    }

    @Test
    void testEntityInheritanceFromBaseEntity() {
        // Test inheritance structure
        assertTrue(riskAnalysis instanceof BaseEntity);
        
        // Test that we can set/get BaseEntity fields
        UUID testId = UUID.randomUUID();
        riskAnalysis.setId(testId);
        assertEquals(testId, riskAnalysis.getId());
        
        String testUser = "test-user";
        riskAnalysis.setCreatedBy(testUser);
        assertEquals(testUser, riskAnalysis.getCreatedBy());
        
        riskAnalysis.setUpdatedBy(testUser);
        assertEquals(testUser, riskAnalysis.getUpdatedBy());
    }

    @Test
    void testAllCustomerRiskTypes() {
        for (CustomerRiskType riskType : CustomerRiskType.values()) {
            RiskAnalysis testAnalysis = new RiskAnalysis();
            testAnalysis.setClassification(riskType);
            assertEquals(riskType, testAnalysis.getClassification());
            
            // Validate through service
            assertDoesNotThrow(() -> validationService.validateClassification(riskType));
        }
    }

    @Test
    void testRiskAnalysisWithMultipleOccurrences() {
        riskAnalysis.setClassification(CustomerRiskType.HIGH_RISK);
        riskAnalysis.setAnalyzedAt(now.minusHours(1));
        
        // Create multiple occurrences
        RiskOccurrence occurrence1 = new RiskOccurrence();
        occurrence1.setType("FRAUD");
        occurrence1.setDescription("Previous fraud claim detected");
        occurrence1.setCreatedAt(now.minusDays(1));
        occurrence1.setUpdatedAt(now.minusDays(1));
        
        RiskOccurrence occurrence2 = new RiskOccurrence();
        occurrence2.setType("SUSPICIOUS_ACTIVITY");
        occurrence2.setDescription("Multiple claims in short period");
        occurrence2.setCreatedAt(now.minusHours(2));
        occurrence2.setUpdatedAt(now.minusHours(2));
        
        RiskOccurrence occurrence3 = new RiskOccurrence();
        occurrence3.setType("IDENTITY_THEFT");
        occurrence3.setDescription("Document verification failed");
        occurrence3.setCreatedAt(now.minusHours(3));
        occurrence3.setUpdatedAt(now.minusHours(3));
        
        assertDoesNotThrow(() -> validationService.addOccurrence(riskAnalysis, occurrence1));
        assertDoesNotThrow(() -> validationService.addOccurrence(riskAnalysis, occurrence2));
        assertDoesNotThrow(() -> validationService.addOccurrence(riskAnalysis, occurrence3));
        
        assertEquals(3, riskAnalysis.getOccurrences().size());
        assertTrue(riskAnalysis.getOccurrences().contains(occurrence1));
        assertTrue(riskAnalysis.getOccurrences().contains(occurrence2));
        assertTrue(riskAnalysis.getOccurrences().contains(occurrence3));
    }

    @Test
    void testRiskAnalysisWithEmptyOccurrencesList() {
        riskAnalysis.setClassification(CustomerRiskType.REGULAR);
        riskAnalysis.setAnalyzedAt(now.minusHours(1));
        
        // Clear occurrences list
        riskAnalysis.getOccurrences().clear();
        
        assertEquals(0, riskAnalysis.getOccurrences().size());
        assertTrue(riskAnalysis.getOccurrences().isEmpty());
        
        // Should still be valid without occurrences
        assertDoesNotThrow(() -> validationService.validateRiskAnalysis(riskAnalysis));
    }

    @Test
    void testRiskAnalysisOccurrenceManipulation() {
        riskAnalysis.setClassification(CustomerRiskType.PREFERRED);
        riskAnalysis.setAnalyzedAt(now.minusHours(1));
        
        RiskOccurrence occurrence = new RiskOccurrence();
        occurrence.setType("MINOR_INCIDENT");
        occurrence.setDescription("Small previous claim");
        occurrence.setCreatedAt(now.minusHours(2));
        occurrence.setUpdatedAt(now.minusHours(2));
        
        // Add occurrence
        assertDoesNotThrow(() -> validationService.addOccurrence(riskAnalysis, occurrence));
        assertEquals(1, riskAnalysis.getOccurrences().size());
        
        // Remove occurrence
        assertDoesNotThrow(() -> validationService.removeOccurrence(riskAnalysis, occurrence));
        assertEquals(0, riskAnalysis.getOccurrences().size());
        
        // Try to remove non-existent occurrence (should not fail)
        assertDoesNotThrow(() -> validationService.removeOccurrence(riskAnalysis, occurrence));
        assertEquals(0, riskAnalysis.getOccurrences().size());
    }

    @Test
    void testRiskAnalysisWithDifferentTimeZones() {
        LocalDateTime[] testTimes = {
            LocalDateTime.of(2023, 1, 1, 0, 0, 0),
            LocalDateTime.of(2023, 6, 15, 12, 30, 45),
            LocalDateTime.of(2023, 12, 31, 23, 59, 59),
            now.minusYears(1),
            now.minusMonths(6),
            now.minusDays(1),
            now.minusHours(1),
            now.minusMinutes(30)
        };
        
        for (LocalDateTime testTime : testTimes) {
            RiskAnalysis timeAnalysis = new RiskAnalysis();
            timeAnalysis.setClassification(CustomerRiskType.REGULAR);
            timeAnalysis.setAnalyzedAt(testTime);
            
            assertEquals(testTime, timeAnalysis.getAnalyzedAt());
            
            if (!testTime.isAfter(now)) {
                assertDoesNotThrow(() -> validationService.validateRiskAnalysis(timeAnalysis));
            }
        }
    }

    @Test
    void testRiskAnalysisEqualsAndHashCode() {
        RiskAnalysis analysis1 = new RiskAnalysis();
        analysis1.setId(UUID.randomUUID());
        analysis1.setClassification(CustomerRiskType.HIGH_RISK);
        analysis1.setAnalyzedAt(now);

        RiskAnalysis analysis2 = new RiskAnalysis();
        analysis2.setId(analysis1.getId());
        analysis2.setClassification(CustomerRiskType.HIGH_RISK);
        analysis2.setAnalyzedAt(now);

        assertEquals(analysis1, analysis2);
        assertEquals(analysis1.hashCode(), analysis2.hashCode());
    }

    @Test
    void testRiskAnalysisToString() {
        riskAnalysis.setClassification(CustomerRiskType.PREFERRED);
        riskAnalysis.setAnalyzedAt(now);
        
        String toString = riskAnalysis.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("RiskAnalysis"));
        assertTrue(toString.contains("PREFERRED"));
    }

    @Test
    void testRiskAnalysisWithNullOccurrencesList() {
        // Test that entity can handle null occurrences list (should initialize to empty)
        RiskAnalysis nullListAnalysis = new RiskAnalysis();
        assertNotNull(nullListAnalysis.getOccurrences());
        assertTrue(nullListAnalysis.getOccurrences().isEmpty());
    }

    @Test
    void testRiskAnalysisClassificationChanges() {
        riskAnalysis.setAnalyzedAt(now.minusHours(1));
        
        // Test changing classification multiple times
        CustomerRiskType[] classifications = CustomerRiskType.values();
        for (CustomerRiskType classification : classifications) {
            riskAnalysis.setClassification(classification);
            assertEquals(classification, riskAnalysis.getClassification());
            
            // Validate each classification change
            assertDoesNotThrow(() -> validationService.validateClassification(classification));
        }
    }

    @Test
    void testRiskAnalysisOccurrenceTypes() {
        riskAnalysis.setClassification(CustomerRiskType.HIGH_RISK);
        riskAnalysis.setAnalyzedAt(now.minusHours(1));
        
        String[] occurrenceTypes = {
            "FRAUD", "IDENTITY_THEFT", "MONEY_LAUNDERING", 
            "SUSPICIOUS_ACTIVITY", "DOCUMENT_FORGERY", "CREDIT_ABUSE",
            "MULTIPLE_CLAIMS", "HIGH_VALUE_CLAIM", "FREQUENT_CLAIMS"
        };
        
        for (String type : occurrenceTypes) {
            RiskOccurrence occurrence = new RiskOccurrence();
            occurrence.setType(type);
            occurrence.setDescription("Test occurrence for " + type);
            occurrence.setCreatedAt(now.minusHours(2));
            occurrence.setUpdatedAt(now.minusHours(2));
            
            assertDoesNotThrow(() -> validationService.addOccurrence(riskAnalysis, occurrence));
        }
        
        assertEquals(occurrenceTypes.length, riskAnalysis.getOccurrences().size());
    }

    @Test
    void testRiskAnalysisComplexScenarios() {
        // Scenario 1: Regular customer with no issues
        RiskAnalysis regularAnalysis = new RiskAnalysis();
        regularAnalysis.setClassification(CustomerRiskType.REGULAR);
        regularAnalysis.setAnalyzedAt(now.minusHours(1));
        assertDoesNotThrow(() -> validationService.validateRiskAnalysis(regularAnalysis));
        
        // Scenario 2: High risk customer with multiple occurrences
        RiskAnalysis highRiskAnalysis = new RiskAnalysis();
        highRiskAnalysis.setClassification(CustomerRiskType.HIGH_RISK);
        highRiskAnalysis.setAnalyzedAt(now.minusHours(2));
        
        for (int i = 0; i < 5; i++) {
            RiskOccurrence occurrence = new RiskOccurrence();
            occurrence.setType("RISK_" + i);
            occurrence.setDescription("Risk occurrence " + i);
            occurrence.setCreatedAt(now.minusHours(3 + i));
            occurrence.setUpdatedAt(now.minusHours(3 + i));
            
            assertDoesNotThrow(() -> validationService.addOccurrence(highRiskAnalysis, occurrence));
        }
        
        assertEquals(5, highRiskAnalysis.getOccurrences().size());
        assertDoesNotThrow(() -> validationService.validateRiskAnalysis(highRiskAnalysis));
        
        // Scenario 3: Preferred customer - minimal risk
        RiskAnalysis preferredAnalysis = new RiskAnalysis();
        preferredAnalysis.setClassification(CustomerRiskType.PREFERRED);
        preferredAnalysis.setAnalyzedAt(now.minusMinutes(30));
        assertDoesNotThrow(() -> validationService.validateRiskAnalysis(preferredAnalysis));
    }

    @Test
    void testRiskAnalysisFieldValidationEdgeCases() {
        // Test null classification
        RiskAnalysis nullClassification = new RiskAnalysis();
        nullClassification.setAnalyzedAt(now);
        assertThrows(IllegalArgumentException.class, () -> 
            validationService.validateRiskAnalysis(nullClassification));
        
        // Test null analyzed at
        RiskAnalysis nullAnalyzedAt = new RiskAnalysis();
        nullAnalyzedAt.setClassification(CustomerRiskType.REGULAR);
        assertThrows(IllegalArgumentException.class, () -> 
            validationService.validateRiskAnalysis(nullAnalyzedAt));
        
        // Test null analysis object
        assertThrows(IllegalArgumentException.class, () -> 
            validationService.validateRiskAnalysis(null));
    }

    @Test
    void testRiskAnalysisOccurrenceCollectionBehavior() {
        riskAnalysis.setClassification(CustomerRiskType.REGULAR);
        riskAnalysis.setAnalyzedAt(now.minusHours(1));
        
        // Test that occurrences list is modifiable
        List<RiskOccurrence> occurrences = riskAnalysis.getOccurrences();
        assertNotNull(occurrences);
        assertTrue(occurrences instanceof List);
        
        // Test direct list manipulation
        RiskOccurrence directOccurrence = new RiskOccurrence();
        directOccurrence.setType("DIRECT_ADD");
        directOccurrence.setDescription("Added directly to list");
        directOccurrence.setCreatedAt(now.minusHours(2));
        directOccurrence.setUpdatedAt(now.minusHours(2));
        
        occurrences.add(directOccurrence);
        assertEquals(1, riskAnalysis.getOccurrences().size());
        assertTrue(riskAnalysis.getOccurrences().contains(directOccurrence));
        
        // Test list clearing
        occurrences.clear();
        assertEquals(0, riskAnalysis.getOccurrences().size());
    }

    @Test
    void testRiskAnalysisPreciseTimestamps() {
        LocalDateTime preciseTime = LocalDateTime.of(2023, 12, 25, 14, 30, 45, 123456789);
        
        riskAnalysis.setClassification(CustomerRiskType.NO_INFORMATION);
        riskAnalysis.setAnalyzedAt(preciseTime);
        
        assertEquals(preciseTime, riskAnalysis.getAnalyzedAt());
        assertEquals(2023, riskAnalysis.getAnalyzedAt().getYear());
        assertEquals(12, riskAnalysis.getAnalyzedAt().getMonthValue());
        assertEquals(25, riskAnalysis.getAnalyzedAt().getDayOfMonth());
        assertEquals(14, riskAnalysis.getAnalyzedAt().getHour());
        assertEquals(30, riskAnalysis.getAnalyzedAt().getMinute());
        assertEquals(45, riskAnalysis.getAnalyzedAt().getSecond());
        assertEquals(123456789, riskAnalysis.getAnalyzedAt().getNano());
    }

    @Test
    void testRiskAnalysisWithBaseEntityFields() {
        riskAnalysis.setClassification(CustomerRiskType.PREFERRED);
        riskAnalysis.setAnalyzedAt(now);
        
        // Test BaseEntity field inheritance
        UUID testId = UUID.randomUUID();
        riskAnalysis.setId(testId);
        assertEquals(testId, riskAnalysis.getId());
        
        String creator = "system-analyzer";
        riskAnalysis.setCreatedBy(creator);
        assertEquals(creator, riskAnalysis.getCreatedBy());
        
        String updater = "admin-user";
        riskAnalysis.setUpdatedBy(updater);
        assertEquals(updater, riskAnalysis.getUpdatedBy());
        
        LocalDateTime createdAt = now.minusHours(2);
        riskAnalysis.setCreatedAt(createdAt);
        assertEquals(createdAt, riskAnalysis.getCreatedAt());
        
        LocalDateTime updatedAt = now.minusHours(1);
        riskAnalysis.setUpdatedAt(updatedAt);
        assertEquals(updatedAt, riskAnalysis.getUpdatedAt());
    }
} 