package com.insurance.domain;

import com.insurance.domain.enums.ClaimStatus;
import com.insurance.domain.enums.InsuranceCategory;
import com.insurance.domain.enums.PolicyStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ClaimTest {

    private Claim claim;
    private InsurancePolicy policy;
    private LocalDate incidentDate;
    private String claimNumber;

    @BeforeEach
    void setUp() {

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
    void testValidateRequiredFields() {
        Claim invalidClaim = new Claim();
        
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            invalidClaim.setClaimNumber(null);
        });
        assertTrue(exception.getMessage().contains("claimNumber"));

        exception = assertThrows(IllegalArgumentException.class, () -> {
            invalidClaim.setIncidentDate(null);
        });
        assertTrue(exception.getMessage().contains("incidentDate"));

        exception = assertThrows(IllegalArgumentException.class, () -> {
            invalidClaim.setDescription(null);
        });
        assertTrue(exception.getMessage().contains("description"));

        exception = assertThrows(IllegalArgumentException.class, () -> {
            invalidClaim.setClaimAmount(null);
        });
        assertTrue(exception.getMessage().contains("claimAmount"));
    }

    @Test
    void testValidateIncidentDate() {

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            claim.setIncidentDate(LocalDate.now().plusDays(1));
        });
        assertTrue(exception.getMessage().contains("future date"));

        exception = assertThrows(IllegalArgumentException.class, () -> {
            claim.setIncidentDate(policy.getStartDate().minusDays(1));
        });
        assertTrue(exception.getMessage().contains("policy start date"));

        assertDoesNotThrow(() -> {
            claim.setIncidentDate(LocalDate.now().minusDays(1));
        });
    }

    @Test
    void testValidateClaimAmount() {

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            claim.setClaimAmount(new BigDecimal("-1.00"));
        });
        assertTrue(exception.getMessage().contains("negative"));

        exception = assertThrows(IllegalArgumentException.class, () -> {
            claim.setClaimAmount(BigDecimal.ZERO);
        });
        assertTrue(exception.getMessage().contains("zero"));

        exception = assertThrows(IllegalArgumentException.class, () -> {
            claim.setClaimAmount(policy.getCoverageAmount().add(BigDecimal.ONE));
        });
        assertTrue(exception.getMessage().contains("coverage amount"));

        assertDoesNotThrow(() -> {
            claim.setClaimAmount(new BigDecimal("1000.00"));
        });
    }

    @Test
    void testHandleStatusTransitions() {

        assertEquals(ClaimStatus.SUBMITTED, claim.getStatus());

        assertDoesNotThrow(() -> {
            claim.setStatus(ClaimStatus.UNDER_REVIEW);
        });
        assertEquals(ClaimStatus.UNDER_REVIEW, claim.getStatus());

        assertDoesNotThrow(() -> {
            claim.setStatus(ClaimStatus.APPROVED);
        });
        assertEquals(ClaimStatus.APPROVED, claim.getStatus());

        assertDoesNotThrow(() -> {
            claim.setStatus(ClaimStatus.PAID);
        });
        assertEquals(ClaimStatus.PAID, claim.getStatus());

        Exception exception = assertThrows(IllegalStateException.class, () -> {
            claim.setStatus(ClaimStatus.SUBMITTED);
        });
        assertTrue(exception.getMessage().contains("Invalid status transition"));
    }

    @Test
    void testValidateClaimNumber() {

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            claim.setClaimNumber("");
        });
        assertTrue(exception.getMessage().contains("empty"));

        exception = assertThrows(IllegalArgumentException.class, () -> {
            claim.setClaimNumber("CLM");
        });
        assertTrue(exception.getMessage().contains("length"));

        exception = assertThrows(IllegalArgumentException.class, () -> {
            claim.setClaimNumber("ABC12345678");
        });
        assertTrue(exception.getMessage().contains("CLM"));

        assertDoesNotThrow(() -> {
            claim.setClaimNumber("CLM12345678");
        });
    }

    @Test
    void testClaimNumberValidation() {

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            claim.setClaimNumber(null);
        });
        assertEquals("claimNumber cannot be null", exception.getMessage());

        exception = assertThrows(IllegalArgumentException.class, () -> {
            claim.setClaimNumber("");
        });
        assertTrue(exception.getMessage().contains("empty"));

        exception = assertThrows(IllegalArgumentException.class, () -> {
            claim.setClaimNumber("   ");
        });
        assertTrue(exception.getMessage().contains("empty"));

        exception = assertThrows(IllegalArgumentException.class, () -> {
            claim.setClaimNumber("ABC-2023-001");
        });
        assertTrue(exception.getMessage().contains("CLM"));

        exception = assertThrows(IllegalArgumentException.class, () -> {
            claim.setClaimNumber("CLM-123");
        });
        assertTrue(exception.getMessage().contains("length"));

        claim.setClaimNumber("CLM-2023-001");
        assertEquals("CLM-2023-001", claim.getClaimNumber());
    }

    @Test
    void testIncidentDateValidation() {

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            claim.setIncidentDate(null);
        });
        assertEquals("incidentDate cannot be null", exception.getMessage());

        exception = assertThrows(IllegalArgumentException.class, () -> {
            claim.setIncidentDate(LocalDate.now().plusDays(1));
        });
        assertTrue(exception.getMessage().contains("future date"));

        LocalDate pastDate = LocalDate.now().minusDays(10);
        claim.setIncidentDate(pastDate);
        assertEquals(pastDate, claim.getIncidentDate());

        LocalDate today = LocalDate.now();
        claim.setIncidentDate(today);
        assertEquals(today, claim.getIncidentDate());
    }

    @Test
    void testIncidentDateWithPolicyValidation() {

        InsurancePolicy policy = new InsurancePolicy();
        policy.setStartDate(LocalDate.now().minusDays(30));
        policy.setCoverageAmount(new BigDecimal("50000.00"));
        claim.setPolicy(policy);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            claim.setIncidentDate(LocalDate.now().minusDays(40));
        });
        assertTrue(exception.getMessage().contains("policy start date"));

        LocalDate validDate = LocalDate.now().minusDays(10);
        claim.setIncidentDate(validDate);
        assertEquals(validDate, claim.getIncidentDate());
    }

    @Test
    void testDescriptionValidation() {

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            claim.setDescription(null);
        });
        assertEquals("description cannot be empty", exception.getMessage());

        exception = assertThrows(IllegalArgumentException.class, () -> {
            claim.setDescription("");
        });
        assertEquals("description cannot be empty", exception.getMessage());

        exception = assertThrows(IllegalArgumentException.class, () -> {
            claim.setDescription("   ");
        });
        assertEquals("description cannot be empty", exception.getMessage());

        String validDescription = "Car accident on highway";
        claim.setDescription(validDescription);
        assertEquals(validDescription, claim.getDescription());
    }

    @Test
    void testClaimAmountValidation() {

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            claim.setClaimAmount(null);
        });
        assertEquals("claimAmount cannot be null", exception.getMessage());

        exception = assertThrows(IllegalArgumentException.class, () -> {
            claim.setClaimAmount(new BigDecimal("-100.00"));
        });
        assertTrue(exception.getMessage().contains("negative"));

        exception = assertThrows(IllegalArgumentException.class, () -> {
            claim.setClaimAmount(BigDecimal.ZERO);
        });
        assertTrue(exception.getMessage().contains("zero"));

        BigDecimal validAmount = new BigDecimal("5000.00");
        claim.setClaimAmount(validAmount);
        assertEquals(validAmount, claim.getClaimAmount());
    }

    @Test
    void testClaimAmountWithPolicyValidation() {

        InsurancePolicy policy = new InsurancePolicy();
        policy.setCoverageAmount(new BigDecimal("10000.00"));
        policy.setStartDate(LocalDate.now().minusDays(30));
        claim.setPolicy(policy);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            claim.setClaimAmount(new BigDecimal("15000.00"));
        });
        assertTrue(exception.getMessage().contains("coverage amount"));

        BigDecimal validAmount = new BigDecimal("8000.00");
        claim.setClaimAmount(validAmount);
        assertEquals(validAmount, claim.getClaimAmount());

        BigDecimal maxAmount = new BigDecimal("10000.00");
        claim.setClaimAmount(maxAmount);
        assertEquals(maxAmount, claim.getClaimAmount());
    }

    @Test
    void testStatusTransitions() {

        claim.setStatus(ClaimStatus.UNDER_REVIEW);
        assertEquals(ClaimStatus.UNDER_REVIEW, claim.getStatus());

        Claim rejectedClaim = new Claim();
        rejectedClaim.setClaimNumber("CLM-REJECTED");
        rejectedClaim.setIncidentDate(LocalDate.now().minusDays(5));
        rejectedClaim.setDescription("Test rejected claim");
        rejectedClaim.setClaimAmount(new BigDecimal("1000.00"));
        
        InsurancePolicy policy = new InsurancePolicy();
        policy.setStartDate(LocalDate.now().minusDays(30));
        policy.setCoverageAmount(new BigDecimal("50000.00"));
        rejectedClaim.setPolicy(policy);
        
        rejectedClaim.setStatus(ClaimStatus.REJECTED);
        assertEquals(ClaimStatus.REJECTED, rejectedClaim.getStatus());

        claim.setStatus(ClaimStatus.APPROVED);
        assertEquals(ClaimStatus.APPROVED, claim.getStatus());

        claim.setStatus(ClaimStatus.PAID);
        assertEquals(ClaimStatus.PAID, claim.getStatus());
    }

    @Test
    void testInvalidStatusTransitions() {

        Exception exception = assertThrows(IllegalStateException.class, () -> {
            claim.setStatus(ClaimStatus.APPROVED);
        });
        assertTrue(exception.getMessage().contains("Invalid status transition"));

        claim.setStatus(ClaimStatus.UNDER_REVIEW);
        claim.setStatus(ClaimStatus.APPROVED);
        exception = assertThrows(IllegalStateException.class, () -> {
            claim.setStatus(ClaimStatus.UNDER_REVIEW);
        });
        assertTrue(exception.getMessage().contains("Invalid status transition"));

        claim.setStatus(ClaimStatus.PAID);
        exception = assertThrows(IllegalStateException.class, () -> {
            claim.setStatus(ClaimStatus.UNDER_REVIEW);
        });
        assertTrue(exception.getMessage().contains("Invalid status transition"));

        Claim rejectedClaim = new Claim();
        rejectedClaim.setClaimNumber("CLM-REJECTED-TEST");
        rejectedClaim.setIncidentDate(LocalDate.now().minusDays(5));
        rejectedClaim.setDescription("Test rejected claim transitions");
        rejectedClaim.setClaimAmount(new BigDecimal("1000.00"));
        
        InsurancePolicy policy = new InsurancePolicy();
        policy.setStartDate(LocalDate.now().minusDays(30));
        policy.setCoverageAmount(new BigDecimal("50000.00"));
        rejectedClaim.setPolicy(policy);
        
        rejectedClaim.setStatus(ClaimStatus.REJECTED);
        exception = assertThrows(IllegalStateException.class, () -> {
            rejectedClaim.setStatus(ClaimStatus.APPROVED);
        });
        assertTrue(exception.getMessage().contains("Invalid status transition"));
    }

    @Test
    void testSetNullStatus() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            claim.setStatus(null);
        });
        assertEquals("status cannot be null", exception.getMessage());
    }

    @Test
    void testValidateWithMissingFields() {
        Claim emptyClaim = new Claim();

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            emptyClaim.validate();
        });
        assertEquals("claimNumber is required", exception.getMessage());

        emptyClaim.setClaimNumber("CLM-2023-002");
        exception = assertThrows(IllegalArgumentException.class, () -> {
            emptyClaim.validate();
        });
        assertEquals("incidentDate is required", exception.getMessage());

        emptyClaim.setIncidentDate(LocalDate.now().minusDays(5));
        exception = assertThrows(IllegalArgumentException.class, () -> {
            emptyClaim.validate();
        });
        assertEquals("description is required", exception.getMessage());

        emptyClaim.setDescription("Test incident");
        exception = assertThrows(IllegalArgumentException.class, () -> {
            emptyClaim.validate();
        });
        assertEquals("claimAmount is required", exception.getMessage());

        emptyClaim.setClaimAmount(new BigDecimal("1000.00"));
        exception = assertThrows(IllegalArgumentException.class, () -> {
            emptyClaim.validate();
        });
        assertEquals("policy is required", exception.getMessage());
    }

    @Test
    void testValidateWithAllRequiredFields() {
        Claim validClaim = new Claim();
        validClaim.setClaimNumber("CLM-2023-003");
        validClaim.setIncidentDate(LocalDate.now().minusDays(5));
        validClaim.setDescription("Valid claim description");
        validClaim.setClaimAmount(new BigDecimal("2000.00"));
        
        InsurancePolicy policy = new InsurancePolicy();
        policy.setStartDate(LocalDate.now().minusDays(30));
        policy.setCoverageAmount(new BigDecimal("50000.00"));
        validClaim.setPolicy(policy);

        assertDoesNotThrow(() -> validClaim.validate());
    }

    @Test
    void testOnCreateSetsDefaultStatus() {
        Claim newClaim = new Claim();
        newClaim.onCreate();
        assertEquals(ClaimStatus.SUBMITTED, newClaim.getStatus());
    }

    @Test
    void testOnCreateDoesNotOverrideExistingStatus() {
        Claim newClaim = new Claim();
        newClaim.setStatus(ClaimStatus.UNDER_REVIEW);
        newClaim.onCreate();
        assertEquals(ClaimStatus.UNDER_REVIEW, newClaim.getStatus());
    }

    @Test
    void testSupportingDocuments() {
        String documents = "photo1.jpg, photo2.jpg, police_report.pdf";
        claim.setSupportingDocuments(documents);
        assertEquals(documents, claim.getSupportingDocuments());

        claim.setSupportingDocuments(null);
        assertNull(claim.getSupportingDocuments());

        claim.setSupportingDocuments("");
        assertEquals("", claim.getSupportingDocuments());
    }

    @Test
    void testAdjustorNotes() {
        String notes = "Claim reviewed and approved based on provided evidence";
        claim.setAdjustorNotes(notes);
        assertEquals(notes, claim.getAdjustorNotes());

        claim.setAdjustorNotes(null);
        assertNull(claim.getAdjustorNotes());

        claim.setAdjustorNotes("");
        assertEquals("", claim.getAdjustorNotes());
    }

    @Test
    void testClaimWithCompleteWorkflow() {

        Claim workflowClaim = new Claim();
        
        workflowClaim.setClaimNumber("CLM-2023-WORKFLOW");
        workflowClaim.setIncidentDate(LocalDate.now().minusDays(7));
        workflowClaim.setDescription("Complete workflow test claim");
        workflowClaim.setClaimAmount(new BigDecimal("3500.00"));
        
        InsurancePolicy policy = new InsurancePolicy();
        policy.setStartDate(LocalDate.now().minusDays(30));
        policy.setCoverageAmount(new BigDecimal("50000.00"));
        workflowClaim.setPolicy(policy);
        
        assertEquals(ClaimStatus.SUBMITTED, workflowClaim.getStatus());
        
        workflowClaim.setStatus(ClaimStatus.UNDER_REVIEW);
        workflowClaim.setAdjustorNotes("Claim under review - evidence being analyzed");
        
        workflowClaim.setStatus(ClaimStatus.APPROVED);
        workflowClaim.setAdjustorNotes("Claim approved - payment authorized");
        
        workflowClaim.setStatus(ClaimStatus.PAID);
        workflowClaim.setAdjustorNotes("Payment processed successfully");
        
        assertEquals(ClaimStatus.PAID, workflowClaim.getStatus());
        assertEquals("Payment processed successfully", workflowClaim.getAdjustorNotes());
        
        assertDoesNotThrow(() -> workflowClaim.validate());
    }

    @Test
    void testInheritanceFromBaseEntity() {

        assertNotNull(claim);
        
        UUID testId = UUID.randomUUID();
        claim.setId(testId);
        assertEquals(testId, claim.getId());
    }

    @Test
    void testAllStatusTransitionPaths() {

        assertEquals(ClaimStatus.SUBMITTED, claim.getStatus());
        
        assertDoesNotThrow(() -> {
            claim.setStatus(ClaimStatus.UNDER_REVIEW);
        });
        assertEquals(ClaimStatus.UNDER_REVIEW, claim.getStatus());
        
        assertDoesNotThrow(() -> {
            claim.setStatus(ClaimStatus.APPROVED);
        });
        assertEquals(ClaimStatus.APPROVED, claim.getStatus());
        
        assertDoesNotThrow(() -> {
            claim.setStatus(ClaimStatus.PAID);
        });
        assertEquals(ClaimStatus.PAID, claim.getStatus());
        
        Claim claim2 = new Claim();
        claim2.setClaimNumber("CLM98765432");
        claim2.setIncidentDate(LocalDate.now().minusDays(3));
        claim2.setDescription("Test claim");
        claim2.setClaimAmount(new BigDecimal("1000.00"));
        claim2.setPolicy(policy);
        
        assertEquals(ClaimStatus.SUBMITTED, claim2.getStatus());
        claim2.setStatus(ClaimStatus.UNDER_REVIEW);
        claim2.setStatus(ClaimStatus.REJECTED);
        
        assertEquals(ClaimStatus.REJECTED, claim2.getStatus());
    }

    @Test
    void testClaimAmountEdgeCases() {

        assertDoesNotThrow(() -> {
            claim.setClaimAmount(policy.getCoverageAmount());
        });
        assertEquals(policy.getCoverageAmount(), claim.getClaimAmount());
        
        assertDoesNotThrow(() -> {
            claim.setClaimAmount(new BigDecimal("0.01"));
        });
        assertEquals(new BigDecimal("0.01"), claim.getClaimAmount());
        
        assertDoesNotThrow(() -> {
            claim.setClaimAmount(policy.getCoverageAmount().subtract(new BigDecimal("0.01")));
        });
    }

    @Test
    void testIncidentDateEdgeCases() {

        assertDoesNotThrow(() -> {
            claim.setIncidentDate(policy.getStartDate());
        });
        assertEquals(policy.getStartDate(), claim.getIncidentDate());
        
        assertDoesNotThrow(() -> {
            claim.setIncidentDate(LocalDate.now());
        });
        assertEquals(LocalDate.now(), claim.getIncidentDate());
        
        InsurancePolicy policyWithValidStart = new InsurancePolicy();
        policyWithValidStart.setPolicyNumber("POL-TEST");
        policyWithValidStart.setCoverageAmount(new BigDecimal("10000.00"));
        policyWithValidStart.setStartDate(LocalDate.now().minusMonths(1)); // Data válida
        
        claim.setPolicy(policyWithValidStart);
        
        assertDoesNotThrow(() -> {
            claim.setIncidentDate(LocalDate.now().minusDays(1));
        });
    }

    @Test
    void testClaimNumberFormats() {

        assertDoesNotThrow(() -> {
            claim.setClaimNumber("CLM12345678");
        });
        
        assertDoesNotThrow(() -> {
            claim.setClaimNumber("CLM2024000001");
        });
        
        assertDoesNotThrow(() -> {
            claim.setClaimNumber("CLMABC123456");
        });
    }

    @Test
    void testDescriptionEdgeCases() {

        assertDoesNotThrow(() -> {
            claim.setDescription("Acidente com danos: R$ 5.000,00 - Veículo Placa ABC-1234");
        });
        
        String longDescription = "A".repeat(1000);
        assertDoesNotThrow(() -> {
            claim.setDescription(longDescription);
        });
        assertEquals(longDescription, claim.getDescription());
        
        assertDoesNotThrow(() -> {
            claim.setDescription("Linha 1\nLinha 2\nLinha 3");
        });
    }

    @Test
    void testInvalidStatusTransitionsDetailed() {

        assertEquals(ClaimStatus.SUBMITTED, claim.getStatus());
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            claim.setStatus(ClaimStatus.APPROVED);
        });
        assertTrue(exception.getMessage().contains("Invalid status transition"));
        
        exception = assertThrows(IllegalStateException.class, () -> {
            claim.setStatus(ClaimStatus.PAID);
        });
        assertTrue(exception.getMessage().contains("Invalid status transition"));
        
        Claim newClaim = new Claim();
        newClaim.setClaimNumber("CLM11223344");
        newClaim.setIncidentDate(LocalDate.now().minusDays(1));
        newClaim.setDescription("Test");
        newClaim.setClaimAmount(new BigDecimal("1000.00"));
        newClaim.setPolicy(policy);
        
        newClaim.setStatus(ClaimStatus.UNDER_REVIEW);
        newClaim.setStatus(ClaimStatus.APPROVED);
        
        exception = assertThrows(IllegalStateException.class, () -> {
            newClaim.setStatus(ClaimStatus.SUBMITTED);
        });
        assertTrue(exception.getMessage().contains("Invalid status transition"));
    }

    @Test
    void testClaimWithNullPolicy() {
        Claim claimWithoutPolicy = new Claim();
        claimWithoutPolicy.setClaimNumber("CLM12345678");
        claimWithoutPolicy.setDescription("Test claim");
        claimWithoutPolicy.setClaimAmount(new BigDecimal("1000.00"));
        claimWithoutPolicy.setIncidentDate(LocalDate.now().minusDays(1));

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            claimWithoutPolicy.validate();
        });
        assertTrue(exception.getMessage().contains("policy is required"));
    }

    @Test
    void testSetIncidentDateWithNullPolicy() {
        claim.setPolicy(null);
        
        assertDoesNotThrow(() -> {
            claim.setIncidentDate(LocalDate.now().minusDays(1));
        });
    }

    @Test
    void testSetClaimAmountWithNullPolicy() {
        claim.setPolicy(null);
        
        assertDoesNotThrow(() -> {
            claim.setClaimAmount(new BigDecimal("1000.00"));
        });
        
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            claim.setClaimAmount(BigDecimal.ZERO);
        });
        assertTrue(exception.getMessage().contains("zero"));
    }

    @Test
    void testPrePersistHook() {
        Claim newClaim = new Claim();
        assertEquals(ClaimStatus.SUBMITTED, newClaim.getStatus());
        
        newClaim.onCreate();
        assertEquals(ClaimStatus.SUBMITTED, newClaim.getStatus());
        
        Claim claimWithDifferentStatus = new Claim();
        claimWithDifferentStatus.setStatus(ClaimStatus.UNDER_REVIEW);
        claimWithDifferentStatus.onCreate();
        
        assertEquals(ClaimStatus.UNDER_REVIEW, claimWithDifferentStatus.getStatus());
    }

    @Test
    void testCompleteClaimLifecycle() {

        Claim lifecycleClaim = new Claim();
        lifecycleClaim.setClaimNumber("CLM99887766");
        lifecycleClaim.setIncidentDate(LocalDate.now().minusDays(2));
        lifecycleClaim.setDescription("Complete lifecycle test");
        lifecycleClaim.setClaimAmount(new BigDecimal("3000.00"));
        lifecycleClaim.setPolicy(policy);
        
        assertEquals(ClaimStatus.SUBMITTED, lifecycleClaim.getStatus());
        
        lifecycleClaim.setStatus(ClaimStatus.UNDER_REVIEW);
        assertEquals(ClaimStatus.UNDER_REVIEW, lifecycleClaim.getStatus());
        
        lifecycleClaim.setStatus(ClaimStatus.APPROVED);
        assertEquals(ClaimStatus.APPROVED, lifecycleClaim.getStatus());
        
        lifecycleClaim.setStatus(ClaimStatus.PAID);
        assertEquals(ClaimStatus.PAID, lifecycleClaim.getStatus());
        
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            lifecycleClaim.setStatus(ClaimStatus.UNDER_REVIEW);
        });
        assertTrue(exception.getMessage().contains("Invalid status transition"));
    }

    @Test
    void testSupportingDocumentsAndNotes() {

        claim.setSupportingDocuments(null);
        assertNull(claim.getSupportingDocuments());
        
        claim.setAdjustorNotes(null);
        assertNull(claim.getAdjustorNotes());
        
        claim.setSupportingDocuments("");
        assertEquals("", claim.getSupportingDocuments());
        
        claim.setAdjustorNotes("");
        assertEquals("", claim.getAdjustorNotes());
        
        String docs = "doc1.pdf,doc2.jpg,witness_statement.txt";
        String notes = "Customer provided all required documentation. Approved for payment.";
        
        claim.setSupportingDocuments(docs);
        claim.setAdjustorNotes(notes);
        
        assertEquals(docs, claim.getSupportingDocuments());
        assertEquals(notes, claim.getAdjustorNotes());
    }

    @Test
    void testEqualsAndHashCodeFromBaseEntity() {
        Claim claim1 = new Claim();
        UUID testId = UUID.randomUUID();
        claim1.setId(testId);
        
        Claim claim2 = new Claim();
        claim2.setId(testId); // Mesmo ID
        
        assertEquals(claim1, claim2);
        assertEquals(claim1.hashCode(), claim2.hashCode());
    }

    @Test
    void testToStringMethod() {
        String toString = claim.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("Claim"));
    }

    @Test
    void testValidateCompleteMethodCoverage() {
        Claim validClaim = new Claim();
        validClaim.setClaimNumber("CLM55443322");
        validClaim.setIncidentDate(LocalDate.now().minusDays(1));
        validClaim.setDescription("Valid claim for testing");
        validClaim.setClaimAmount(new BigDecimal("2500.00"));
        validClaim.setPolicy(policy);
        
        assertDoesNotThrow(() -> {
            validClaim.validate();
        });
    }

    @Test
    void testSetClaimNumberWithWhitespace() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            claim.setClaimNumber("   ");
        });
        assertTrue(exception.getMessage().contains("empty"));
    }

    @Test
    void testSetDescriptionWithWhitespace() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            claim.setDescription("   ");
        });
        assertTrue(exception.getMessage().contains("description cannot be empty"));
    }

    @Test
    void testAllStatusEnumValues() {

        Claim testClaim = new Claim();
        testClaim.setClaimNumber("CLM99887766");
        testClaim.setIncidentDate(LocalDate.now().minusDays(1));
        testClaim.setDescription("Test");
        testClaim.setClaimAmount(new BigDecimal("1000.00"));
        testClaim.setPolicy(policy);
        
        assertEquals(ClaimStatus.SUBMITTED, testClaim.getStatus()); // Default
        
        testClaim.setStatus(ClaimStatus.UNDER_REVIEW);
        assertEquals(ClaimStatus.UNDER_REVIEW, testClaim.getStatus());
        
        testClaim.setStatus(ClaimStatus.APPROVED);
        assertEquals(ClaimStatus.APPROVED, testClaim.getStatus());
        
        testClaim.setStatus(ClaimStatus.PAID);
        assertEquals(ClaimStatus.PAID, testClaim.getStatus());
        
        Claim rejectedClaim = new Claim();
        rejectedClaim.setClaimNumber("CLM55443322");
        rejectedClaim.setIncidentDate(LocalDate.now().minusDays(1));
        rejectedClaim.setDescription("Test");
        rejectedClaim.setClaimAmount(new BigDecimal("1000.00"));
        rejectedClaim.setPolicy(policy);
        
        rejectedClaim.setStatus(ClaimStatus.UNDER_REVIEW);
        rejectedClaim.setStatus(ClaimStatus.REJECTED);
        assertEquals(ClaimStatus.REJECTED, rejectedClaim.getStatus());
    }

    @Test
    void testSetStatusWithNull() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            claim.setStatus(null);
        });
        assertTrue(exception.getMessage().contains("status cannot be null"));
    }
}