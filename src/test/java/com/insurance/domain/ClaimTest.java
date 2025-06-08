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
        // Configurando a apólice
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
        assertTrue(claim instanceof BaseEntity);
        
        claim.setCreatedBy("claims_processor");
        claim.setUpdatedBy("adjustor");
        
        assertEquals("claims_processor", claim.getCreatedBy());
        assertEquals("adjustor", claim.getUpdatedBy());
    }
}