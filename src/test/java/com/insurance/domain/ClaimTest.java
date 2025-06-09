package com.insurance.domain;

import com.insurance.domain.enums.ClaimStatus;
import com.insurance.domain.enums.InsuranceCategory;
import com.insurance.domain.enums.PolicyStatus;
import com.insurance.service.ClaimStatusService;
import com.insurance.service.impl.ClaimStatusServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for Claim entity after refactoring to follow Single Responsibility Principle.
 * The entity now only handles JPA mapping, while business logic is tested through ClaimStatusService.
 */
class ClaimTest {

    private Claim claim;
    private InsurancePolicy policy;
    private ClaimStatusService claimStatusService;
    private LocalDate incidentDate;
    private String claimNumber;

    @BeforeEach
    void setUp() {
        claimStatusService = new ClaimStatusServiceImpl();

        policy = new InsurancePolicy();
        policy.setPolicyNumber("POL-2024-001");
        policy.setStartDate(LocalDate.now().minusMonths(1));
        policy.setEndDate(LocalDate.now().plusMonths(11));
        policy.setPremium(new BigDecimal("1000.00"));
        policy.setCoverageAmount(new BigDecimal("50000.00"));
        policy.setStatus(PolicyStatus.RECEIVED);
        policy.setType(InsuranceCategory.AUTO);

        incidentDate = LocalDate.now().minusDays(5);
        claimNumber = "CLM" + UUID.randomUUID().toString().substring(0, 8);

        claim = new Claim();
        claim.setClaimNumber(claimNumber);
        claim.setIncidentDate(incidentDate);
        claim.setDescription("Car accident on highway");
        claim.setClaimAmount(new BigDecimal("5000.00"));
        claim.setPolicy(policy);
        claim.setSupportingDocuments("police_report.pdf, photos.zip");
        claim.setAdjustorNotes("Initial assessment completed");
    }

    // ========== ENTITY MAPPING TESTS ==========

    @Test
    void testCreateClaimWithCorrectData() {
        assertNotNull(claim);
        assertEquals(claimNumber, claim.getClaimNumber());
        assertEquals(incidentDate, claim.getIncidentDate());
        assertEquals("Car accident on highway", claim.getDescription());
        assertEquals(new BigDecimal("5000.00"), claim.getClaimAmount());
        assertEquals(ClaimStatus.SUBMITTED, claim.getStatus());
        assertEquals(policy, claim.getPolicy());
        assertTrue(claim.getSupportingDocuments().contains("police_report.pdf"));
        assertEquals("Initial assessment completed", claim.getAdjustorNotes());
    }

    @Test
    void testEntitySettersAndGetters() {
        Claim testClaim = new Claim();
        
        // Test all setters and getters work without business validation
        testClaim.setClaimNumber("TEST123");
        assertEquals("TEST123", testClaim.getClaimNumber());
        
        LocalDate testDate = LocalDate.of(2030, 12, 31); // Future date - no validation in entity
        testClaim.setIncidentDate(testDate);
        assertEquals(testDate, testClaim.getIncidentDate());
        
        testClaim.setDescription("Test description");
        assertEquals("Test description", testClaim.getDescription());
        
        BigDecimal testAmount = new BigDecimal("-100.00"); // Negative amount - no validation in entity
        testClaim.setClaimAmount(testAmount);
        assertEquals(testAmount, testClaim.getClaimAmount());
        
        testClaim.setStatus(ClaimStatus.PAID);
        assertEquals(ClaimStatus.PAID, testClaim.getStatus());
        
        testClaim.setPolicy(policy);
        assertEquals(policy, testClaim.getPolicy());
        
        testClaim.setSupportingDocuments("test.pdf");
        assertEquals("test.pdf", testClaim.getSupportingDocuments());
        
        testClaim.setAdjustorNotes("Test notes");
        assertEquals("Test notes", testClaim.getAdjustorNotes());
    }

    @Test
    void testPrePersistHook() {
        Claim newClaim = new Claim();
        // Status is already SUBMITTED by default due to field initialization
        assertEquals(ClaimStatus.SUBMITTED, newClaim.getStatus());
        
        newClaim.onCreate();
        assertEquals(ClaimStatus.SUBMITTED, newClaim.getStatus());
    }

    @Test
    void testPrePersistDoesNotOverrideExistingStatus() {
        Claim newClaim = new Claim();
        newClaim.setStatus(ClaimStatus.UNDER_REVIEW);
        
        newClaim.onCreate();
        assertEquals(ClaimStatus.UNDER_REVIEW, newClaim.getStatus());
    }

    // ========== BUSINESS LOGIC TESTS (Via ClaimStatusService) ==========

    @Test
    void testClaimNumberValidationViaService() {
        // Valid claim number
        assertDoesNotThrow(() -> claimStatusService.validateClaimNumber("CLM12345678"));
        
        // Invalid claim numbers
        assertThrows(IllegalArgumentException.class, () -> 
            claimStatusService.validateClaimNumber(null));
        assertThrows(IllegalArgumentException.class, () -> 
            claimStatusService.validateClaimNumber(""));
        assertThrows(IllegalArgumentException.class, () -> 
            claimStatusService.validateClaimNumber("ABC12345678"));
        assertThrows(IllegalArgumentException.class, () -> 
            claimStatusService.validateClaimNumber("CLM123"));
    }

    @Test
    void testIncidentDateValidationViaService() {
        // Valid incident date
        assertDoesNotThrow(() -> 
            claimStatusService.validateIncidentDate(LocalDate.now().minusDays(1), claim));
        
        // Invalid incident dates
        assertThrows(IllegalArgumentException.class, () -> 
            claimStatusService.validateIncidentDate(null, claim));
        assertThrows(IllegalArgumentException.class, () -> 
            claimStatusService.validateIncidentDate(LocalDate.now().plusDays(1), claim));
        assertThrows(IllegalArgumentException.class, () -> 
            claimStatusService.validateIncidentDate(policy.getStartDate().minusDays(1), claim));
    }

    @Test
    void testClaimAmountValidationViaService() {
        // Valid claim amount
        assertDoesNotThrow(() -> 
            claimStatusService.validateClaimAmount(new BigDecimal("1000.00"), claim));
        
        // Invalid claim amounts
        assertThrows(IllegalArgumentException.class, () -> 
            claimStatusService.validateClaimAmount(null, claim));
        assertThrows(IllegalArgumentException.class, () -> 
            claimStatusService.validateClaimAmount(new BigDecimal("-1.00"), claim));
        assertThrows(IllegalArgumentException.class, () -> 
            claimStatusService.validateClaimAmount(BigDecimal.ZERO, claim));
        assertThrows(IllegalArgumentException.class, () -> 
            claimStatusService.validateClaimAmount(policy.getCoverageAmount().add(BigDecimal.ONE), claim));
    }

    @Test
    void testStatusTransitionsViaService() {
        // Valid transitions
        assertTrue(claimStatusService.canTransitionTo(ClaimStatus.SUBMITTED, ClaimStatus.UNDER_REVIEW));
        assertTrue(claimStatusService.canTransitionTo(ClaimStatus.SUBMITTED, ClaimStatus.REJECTED));
        assertTrue(claimStatusService.canTransitionTo(ClaimStatus.UNDER_REVIEW, ClaimStatus.APPROVED));
        assertTrue(claimStatusService.canTransitionTo(ClaimStatus.UNDER_REVIEW, ClaimStatus.REJECTED));
        assertTrue(claimStatusService.canTransitionTo(ClaimStatus.APPROVED, ClaimStatus.PAID));
        
        // Invalid transitions
        assertFalse(claimStatusService.canTransitionTo(ClaimStatus.PAID, ClaimStatus.APPROVED));
        assertFalse(claimStatusService.canTransitionTo(ClaimStatus.REJECTED, ClaimStatus.APPROVED));
        assertFalse(claimStatusService.canTransitionTo(ClaimStatus.SUBMITTED, ClaimStatus.PAID));
    }

    @Test
    void testUpdateStatusViaService() {
        claim.setStatus(ClaimStatus.SUBMITTED);
        
        // Valid status update
        assertDoesNotThrow(() -> 
            claimStatusService.updateClaimStatus(claim, ClaimStatus.UNDER_REVIEW));
        assertEquals(ClaimStatus.UNDER_REVIEW, claim.getStatus());
        
        // Invalid status update
        assertThrows(IllegalStateException.class, () -> 
            claimStatusService.updateClaimStatus(claim, ClaimStatus.PAID));
    }

    @Test
    void testCompleteClaimValidationViaService() {
        // Valid claim
        assertDoesNotThrow(() -> claimStatusService.validateClaim(claim));
        
        // Invalid claim - missing required fields
        Claim invalidClaim = new Claim();
        assertThrows(IllegalArgumentException.class, () -> 
            claimStatusService.validateClaim(invalidClaim));
    }

    // ========== INTEGRATION TESTS ==========

    @Test
    void testCompleteClaimWorkflowWithService() {
        // Create claim with service validation
        Claim workflowClaim = new Claim();
        workflowClaim.setClaimNumber("CLM-2024-WORKFLOW");
        workflowClaim.setIncidentDate(LocalDate.now().minusDays(7));
        workflowClaim.setDescription("Complete workflow test claim");
        workflowClaim.setClaimAmount(new BigDecimal("3500.00"));
        workflowClaim.setPolicy(policy);
        
        // Validate the complete claim
        assertDoesNotThrow(() -> claimStatusService.validateClaim(workflowClaim));
        
        // Test status transitions
        assertEquals(ClaimStatus.SUBMITTED, workflowClaim.getStatus());
        
        claimStatusService.updateClaimStatus(workflowClaim, ClaimStatus.UNDER_REVIEW);
        assertEquals(ClaimStatus.UNDER_REVIEW, workflowClaim.getStatus());
        
        claimStatusService.updateClaimStatus(workflowClaim, ClaimStatus.APPROVED);
        assertEquals(ClaimStatus.APPROVED, workflowClaim.getStatus());
        
        claimStatusService.updateClaimStatus(workflowClaim, ClaimStatus.PAID);
        assertEquals(ClaimStatus.PAID, workflowClaim.getStatus());
        
        // Verify final state cannot be changed
        assertThrows(IllegalStateException.class, () -> 
            claimStatusService.updateClaimStatus(workflowClaim, ClaimStatus.SUBMITTED));
    }

    @Test
    void testEntityInheritanceFromBaseEntity() {
        // Test inheritance structure
        assertTrue(claim instanceof BaseEntity);
        
        // Test that we can set/get BaseEntity fields
        UUID testId = UUID.randomUUID();
        claim.setId(testId);
        assertEquals(testId, claim.getId());
        
        String testUser = "test-user";
        claim.setCreatedBy(testUser);
        assertEquals(testUser, claim.getCreatedBy());
        
        claim.setUpdatedBy(testUser);
        assertEquals(testUser, claim.getUpdatedBy());
    }

    @Test
    void testAllClaimStatusTypes() {
        for (ClaimStatus status : ClaimStatus.values()) {
            Claim testClaim = new Claim();
            testClaim.setStatus(status);
            assertEquals(status, testClaim.getStatus());
        }
    }

    @Test
    void testClaimWithNullOptionalFields() {
        Claim minimalClaim = new Claim();
        minimalClaim.setClaimNumber("CLM12345678");
        minimalClaim.setIncidentDate(LocalDate.now().minusDays(1));
        minimalClaim.setDescription("Minimal claim");
        minimalClaim.setClaimAmount(new BigDecimal("100.00"));
        minimalClaim.setPolicy(policy);
        
        // Optional fields can be null
        minimalClaim.setSupportingDocuments(null);
        minimalClaim.setAdjustorNotes(null);
        
        assertNull(minimalClaim.getSupportingDocuments());
        assertNull(minimalClaim.getAdjustorNotes());
        assertEquals(ClaimStatus.SUBMITTED, minimalClaim.getStatus());
    }

    @Test
    void testClaimWithEmptyOptionalFields() {
        Claim testClaim = new Claim();
        testClaim.setClaimNumber("CLM87654321");
        testClaim.setIncidentDate(LocalDate.now().minusDays(2));
        testClaim.setDescription("Test claim");
        testClaim.setClaimAmount(new BigDecimal("250.00"));
        testClaim.setPolicy(policy);
        
        // Empty strings for optional fields
        testClaim.setSupportingDocuments("");
        testClaim.setAdjustorNotes("");
        
        assertEquals("", testClaim.getSupportingDocuments());
        assertEquals("", testClaim.getAdjustorNotes());
    }

    @Test
    void testClaimWithLargeAmounts() {
        Claim largeClaim = new Claim();
        largeClaim.setClaimNumber("CLM99999999");
        largeClaim.setIncidentDate(LocalDate.now().minusDays(1));
        largeClaim.setDescription("Large claim test");
        largeClaim.setClaimAmount(new BigDecimal("999999999.99"));
        largeClaim.setPolicy(policy);
        
        assertEquals(new BigDecimal("999999999.99"), largeClaim.getClaimAmount());
    }

    @Test
    void testClaimWithSpecialCharactersInDescription() {
        Claim specialClaim = new Claim();
        specialClaim.setClaimNumber("CLM11111111");
        specialClaim.setIncidentDate(LocalDate.now().minusDays(1));
        specialClaim.setDescription("Special chars: !@#$%^&*()[]{}|;':\",./<>?~`");
        specialClaim.setClaimAmount(new BigDecimal("123.45"));
        specialClaim.setPolicy(policy);
        
        assertEquals("Special chars: !@#$%^&*()[]{}|;':\",./<>?~`", specialClaim.getDescription());
    }

    @Test
    void testClaimWithUTF8CharactersInDescription() {
        Claim utf8Claim = new Claim();
        utf8Claim.setClaimNumber("CLM22222222");
        utf8Claim.setIncidentDate(LocalDate.now().minusDays(1));
        utf8Claim.setDescription("UTF-8: åäö ñáéíóú ßüàèéç 中文 한글 العربية");
        utf8Claim.setClaimAmount(new BigDecimal("456.78"));
        utf8Claim.setPolicy(policy);
        
        assertEquals("UTF-8: åäö ñáéíóú ßüàèéç 中文 한글 العربية", utf8Claim.getDescription());
    }

    @Test
    void testClaimEqualsAndHashCode() {
        Claim claim1 = new Claim();
        claim1.setId(UUID.randomUUID());
        claim1.setClaimNumber("CLM33333333");
        claim1.setIncidentDate(LocalDate.now().minusDays(1));
        claim1.setDescription("Test equals");
        claim1.setClaimAmount(new BigDecimal("100.00"));
        claim1.setPolicy(policy);

        Claim claim2 = new Claim();
        claim2.setId(claim1.getId());
        claim2.setClaimNumber("CLM33333333");
        claim2.setIncidentDate(LocalDate.now().minusDays(1));
        claim2.setDescription("Test equals");
        claim2.setClaimAmount(new BigDecimal("100.00"));
        claim2.setPolicy(policy);

        assertEquals(claim1, claim2);
        assertEquals(claim1.hashCode(), claim2.hashCode());
    }

    @Test
    void testClaimToString() {
        String toString = claim.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("Claim"));
        assertTrue(toString.contains(claimNumber));
    }

    @Test
    void testClaimWithDifferentDateFormats() {
        Claim dateClaim = new Claim();
        dateClaim.setClaimNumber("CLM44444444");
        dateClaim.setClaimAmount(new BigDecimal("789.00"));
        dateClaim.setDescription("Date test");
        dateClaim.setPolicy(policy);
        
        // Test different date scenarios
        LocalDate[] testDates = {
            LocalDate.of(2020, 1, 1),
            LocalDate.of(2023, 12, 31),
            LocalDate.now(),
            LocalDate.now().minusYears(1),
            LocalDate.now().minusMonths(6),
            LocalDate.now().minusDays(30)
        };
        
        for (LocalDate date : testDates) {
            dateClaim.setIncidentDate(date);
            assertEquals(date, dateClaim.getIncidentDate());
        }
    }

    @Test
    void testClaimWithDifferentPolicyTypes() {
        InsuranceCategory[] categories = InsuranceCategory.values();
        
        for (InsuranceCategory category : categories) {
            InsurancePolicy testPolicy = new InsurancePolicy();
            testPolicy.setPolicyNumber("POL-TEST-" + category.name());
            testPolicy.setType(category);
            testPolicy.setStartDate(LocalDate.now().minusMonths(1));
            testPolicy.setEndDate(LocalDate.now().plusMonths(11));
            testPolicy.setPremium(new BigDecimal("500.00"));
            testPolicy.setCoverageAmount(new BigDecimal("25000.00"));
            testPolicy.setStatus(PolicyStatus.RECEIVED);
            
            Claim testClaim = new Claim();
            testClaim.setClaimNumber("CLM" + category.ordinal() + "5555555");
            testClaim.setIncidentDate(LocalDate.now().minusDays(1));
            testClaim.setDescription("Test for " + category.name());
            testClaim.setClaimAmount(new BigDecimal("1000.00"));
            testClaim.setPolicy(testPolicy);
            
            assertEquals(testPolicy, testClaim.getPolicy());
            assertEquals(category, testClaim.getPolicy().getType());
        }
    }

    @Test
    void testClaimAmountPrecision() {
        Claim precisionClaim = new Claim();
        precisionClaim.setClaimNumber("CLM66666666");
        precisionClaim.setIncidentDate(LocalDate.now().minusDays(1));
        precisionClaim.setDescription("Precision test");
        precisionClaim.setPolicy(policy);
        
        // Test different decimal precisions
        BigDecimal[] amounts = {
            new BigDecimal("123.45"),
            new BigDecimal("0.01"),
            new BigDecimal("999999.99"),
            new BigDecimal("123.456789"), // High precision
            new BigDecimal("100"),
            new BigDecimal("100.00")
        };
        
        for (BigDecimal amount : amounts) {
            precisionClaim.setClaimAmount(amount);
            assertEquals(amount, precisionClaim.getClaimAmount());
        }
    }

    @Test
    void testClaimMultipleDocuments() {
        Claim docClaim = new Claim();
        docClaim.setClaimNumber("CLM77777777");
        docClaim.setIncidentDate(LocalDate.now().minusDays(1));
        docClaim.setDescription("Document test");
        docClaim.setClaimAmount(new BigDecimal("500.00"));
        docClaim.setPolicy(policy);
        
        String multipleDocuments = "report.pdf;photos.zip;witness_statement.doc;police_report.pdf;medical_records.pdf";
        docClaim.setSupportingDocuments(multipleDocuments);
        
        assertEquals(multipleDocuments, docClaim.getSupportingDocuments());
        assertTrue(docClaim.getSupportingDocuments().contains("report.pdf"));
        assertTrue(docClaim.getSupportingDocuments().contains("photos.zip"));
        assertTrue(docClaim.getSupportingDocuments().contains("medical_records.pdf"));
    }

    @Test
    void testClaimLongAdjustorNotes() {
        Claim notesClaim = new Claim();
        notesClaim.setClaimNumber("CLM88888888");
        notesClaim.setIncidentDate(LocalDate.now().minusDays(1));
        notesClaim.setDescription("Notes test");
        notesClaim.setClaimAmount(new BigDecimal("750.00"));
        notesClaim.setPolicy(policy);
        
        String longNotes = "This is a very detailed adjustor note that contains multiple paragraphs. " +
                          "The first paragraph explains the initial assessment. " +
                          "The second paragraph details the investigation findings. " +
                          "The third paragraph outlines the recommended course of action. " +
                          "Additional notes may include witness statements, expert opinions, " +
                          "and any other relevant information that could affect the claim processing.";
        
        notesClaim.setAdjustorNotes(longNotes);
        assertEquals(longNotes, notesClaim.getAdjustorNotes());
        assertTrue(notesClaim.getAdjustorNotes().length() > 100);
    }

    @Test
    void testClaimWithAllStatusTransitions() {
        Claim workflowClaim = new Claim();
        workflowClaim.setClaimNumber("CLM99999999");
        workflowClaim.setIncidentDate(LocalDate.now().minusDays(1));
        workflowClaim.setDescription("Workflow test");
        workflowClaim.setClaimAmount(new BigDecimal("300.00"));
        workflowClaim.setPolicy(policy);
        
        // Test setting all possible statuses
        ClaimStatus[] statuses = ClaimStatus.values();
        for (ClaimStatus status : statuses) {
            workflowClaim.setStatus(status);
            assertEquals(status, workflowClaim.getStatus());
        }
    }

    @Test
    void testClaimFieldCombinations() {
        // Test various field combinations to increase coverage
        Claim combinationClaim = new Claim();
        
        // Test with minimal required fields
        combinationClaim.setClaimNumber("CLM10101010");
        combinationClaim.setIncidentDate(LocalDate.now().minusDays(1));
        combinationClaim.setDescription("Minimal");
        combinationClaim.setClaimAmount(new BigDecimal("50.00"));
        combinationClaim.setPolicy(policy);
        
        assertNotNull(combinationClaim);
        assertEquals("CLM10101010", combinationClaim.getClaimNumber());
        
        // Add optional fields
        combinationClaim.setSupportingDocuments("single_doc.pdf");
        combinationClaim.setAdjustorNotes("Brief note");
        combinationClaim.setStatus(ClaimStatus.UNDER_REVIEW);
        
        assertEquals("single_doc.pdf", combinationClaim.getSupportingDocuments());
        assertEquals("Brief note", combinationClaim.getAdjustorNotes());
        assertEquals(ClaimStatus.UNDER_REVIEW, combinationClaim.getStatus());
    }
}