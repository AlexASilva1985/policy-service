package com.insurance.service.impl;

import com.insurance.domain.Claim;
import com.insurance.domain.InsurancePolicy;
import com.insurance.domain.enums.ClaimStatus;
import com.insurance.domain.enums.InsuranceCategory;
import com.insurance.domain.enums.PolicyStatus;
import com.insurance.service.ClaimStatusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test class for ClaimStatusServiceImpl
 */
class ClaimStatusServiceImplTest {

    private ClaimStatusService claimStatusService;
    private Claim claim;
    private InsurancePolicy policy;

    @BeforeEach
    void setUp() {
        claimStatusService = new ClaimStatusServiceImpl();
        
        policy = new InsurancePolicy();
        policy.setPolicyNumber("POL-2024-001");
        policy.setStartDate(LocalDate.now().minusMonths(6));
        policy.setEndDate(LocalDate.now().plusMonths(6));
        policy.setPremium(new BigDecimal("100.00"));
        policy.setCoverageAmount(new BigDecimal("50000.00"));
        policy.setStatus(PolicyStatus.APPROVED);
        policy.setType(InsuranceCategory.AUTO);
        
        claim = new Claim();
        claim.setClaimNumber("CLM12345678");
        claim.setIncidentDate(LocalDate.now().minusDays(30));
        claim.setDescription("Test claim");
        claim.setClaimAmount(new BigDecimal("5000.00"));
        claim.setPolicy(policy);
        claim.setStatus(ClaimStatus.SUBMITTED);
    }

    @Test
    void testValidateClaimNumber_Valid() {

        assertDoesNotThrow(() -> claimStatusService.validateClaimNumber("CLM12345678"));
        assertDoesNotThrow(() -> claimStatusService.validateClaimNumber("CLM87654321"));
        assertDoesNotThrow(() -> claimStatusService.validateClaimNumber("CLM00000001"));
        assertDoesNotThrow(() -> claimStatusService.validateClaimNumber("CLM99999999"));
    }

    @Test
    void testValidateClaimNumber_Invalid() {

        assertThrows(IllegalArgumentException.class, () -> 
            claimStatusService.validateClaimNumber(null));
        assertThrows(IllegalArgumentException.class, () -> 
            claimStatusService.validateClaimNumber(""));
        assertThrows(IllegalArgumentException.class, () -> 
            claimStatusService.validateClaimNumber("   "));
        assertThrows(IllegalArgumentException.class, () -> 
            claimStatusService.validateClaimNumber("ABC12345678"));
        assertThrows(IllegalArgumentException.class, () -> 
            claimStatusService.validateClaimNumber("CLM123"));
    }

    @Test
    void testValidateIncidentDate_Valid() {
        assertDoesNotThrow(() -> 
            claimStatusService.validateIncidentDate(LocalDate.now().minusDays(1), claim));
        assertDoesNotThrow(() -> 
            claimStatusService.validateIncidentDate(LocalDate.now().minusMonths(1), claim));
        assertDoesNotThrow(() -> 
            claimStatusService.validateIncidentDate(policy.getStartDate(), claim));
        assertDoesNotThrow(() -> 
            claimStatusService.validateIncidentDate(policy.getStartDate().plusDays(1), claim));
    }

    @Test
    void testValidateIncidentDate_Invalid() {
        assertThrows(IllegalArgumentException.class, () -> 
            claimStatusService.validateIncidentDate(null, claim));
        assertThrows(IllegalArgumentException.class, () -> 
            claimStatusService.validateIncidentDate(LocalDate.now().plusDays(1), claim));
        assertThrows(IllegalArgumentException.class, () -> 
            claimStatusService.validateIncidentDate(policy.getStartDate().minusDays(1), claim));
    }

    @Test
    void testValidateClaimAmount_Valid() {

        assertDoesNotThrow(() -> 
            claimStatusService.validateClaimAmount(new BigDecimal("0.01"), claim));
        assertDoesNotThrow(() -> 
            claimStatusService.validateClaimAmount(new BigDecimal("1000.00"), claim));
        assertDoesNotThrow(() -> 
            claimStatusService.validateClaimAmount(policy.getCoverageAmount(), claim));
        assertDoesNotThrow(() -> 
            claimStatusService.validateClaimAmount(policy.getCoverageAmount().subtract(new BigDecimal("0.01")), claim));
    }

    @Test
    void testValidateClaimAmount_Invalid() {
        assertThrows(IllegalArgumentException.class, () -> 
            claimStatusService.validateClaimAmount(null, claim));
        assertThrows(IllegalArgumentException.class, () -> 
            claimStatusService.validateClaimAmount(BigDecimal.ZERO, claim));
        assertThrows(IllegalArgumentException.class, () -> 
            claimStatusService.validateClaimAmount(new BigDecimal("-0.01"), claim));
        assertThrows(IllegalArgumentException.class, () -> 
            claimStatusService.validateClaimAmount(policy.getCoverageAmount().add(new BigDecimal("0.01")), claim));
    }

    @Test
    void testCanTransitionTo_ValidTransitions() {

        assertTrue(claimStatusService.canTransitionTo(ClaimStatus.SUBMITTED, ClaimStatus.UNDER_REVIEW));
        assertTrue(claimStatusService.canTransitionTo(ClaimStatus.SUBMITTED, ClaimStatus.REJECTED));
        
        assertTrue(claimStatusService.canTransitionTo(ClaimStatus.UNDER_REVIEW, ClaimStatus.APPROVED));
        assertTrue(claimStatusService.canTransitionTo(ClaimStatus.UNDER_REVIEW, ClaimStatus.REJECTED));
        
        assertTrue(claimStatusService.canTransitionTo(ClaimStatus.APPROVED, ClaimStatus.PAID));
    }

    @Test
    void testCanTransitionTo_InvalidTransitions() {

        assertFalse(claimStatusService.canTransitionTo(ClaimStatus.REJECTED, ClaimStatus.SUBMITTED));
        assertFalse(claimStatusService.canTransitionTo(ClaimStatus.REJECTED, ClaimStatus.UNDER_REVIEW));
        assertFalse(claimStatusService.canTransitionTo(ClaimStatus.REJECTED, ClaimStatus.APPROVED));
        assertFalse(claimStatusService.canTransitionTo(ClaimStatus.REJECTED, ClaimStatus.PAID));
        
        assertFalse(claimStatusService.canTransitionTo(ClaimStatus.PAID, ClaimStatus.SUBMITTED));
        assertFalse(claimStatusService.canTransitionTo(ClaimStatus.PAID, ClaimStatus.UNDER_REVIEW));
        assertFalse(claimStatusService.canTransitionTo(ClaimStatus.PAID, ClaimStatus.APPROVED));
        assertFalse(claimStatusService.canTransitionTo(ClaimStatus.PAID, ClaimStatus.REJECTED));
        
        assertFalse(claimStatusService.canTransitionTo(ClaimStatus.SUBMITTED, ClaimStatus.APPROVED));
        assertFalse(claimStatusService.canTransitionTo(ClaimStatus.SUBMITTED, ClaimStatus.PAID));
        assertFalse(claimStatusService.canTransitionTo(ClaimStatus.UNDER_REVIEW, ClaimStatus.SUBMITTED));
        assertFalse(claimStatusService.canTransitionTo(ClaimStatus.APPROVED, ClaimStatus.SUBMITTED));
        assertFalse(claimStatusService.canTransitionTo(ClaimStatus.APPROVED, ClaimStatus.UNDER_REVIEW));
        assertFalse(claimStatusService.canTransitionTo(ClaimStatus.APPROVED, ClaimStatus.REJECTED));
    }

    @Test
    void testCanTransitionTo_SameStatus() {
        for (ClaimStatus status : ClaimStatus.values()) {
            assertFalse(claimStatusService.canTransitionTo(status, status));
        }
    }

    @Test
    void testUpdateClaimStatus_ValidTransitions() {

        claim.setStatus(ClaimStatus.SUBMITTED);
        assertDoesNotThrow(() -> claimStatusService.updateClaimStatus(claim, ClaimStatus.UNDER_REVIEW));
        assertEquals(ClaimStatus.UNDER_REVIEW, claim.getStatus());
        
        assertDoesNotThrow(() -> claimStatusService.updateClaimStatus(claim, ClaimStatus.APPROVED));
        assertEquals(ClaimStatus.APPROVED, claim.getStatus());
        
        assertDoesNotThrow(() -> claimStatusService.updateClaimStatus(claim, ClaimStatus.PAID));
        assertEquals(ClaimStatus.PAID, claim.getStatus());
    }

    @Test
    void testUpdateClaimStatus_InvalidTransitions() {

        claim.setStatus(ClaimStatus.SUBMITTED);
        assertThrows(IllegalStateException.class, () -> 
            claimStatusService.updateClaimStatus(claim, ClaimStatus.PAID));
        assertEquals(ClaimStatus.SUBMITTED, claim.getStatus()); // Status should not change
        
        claim.setStatus(ClaimStatus.PAID);
        assertThrows(IllegalStateException.class, () -> 
            claimStatusService.updateClaimStatus(claim, ClaimStatus.APPROVED));
        assertEquals(ClaimStatus.PAID, claim.getStatus());
        
        claim.setStatus(ClaimStatus.REJECTED);
        assertThrows(IllegalStateException.class, () -> 
            claimStatusService.updateClaimStatus(claim, ClaimStatus.APPROVED));
        assertEquals(ClaimStatus.REJECTED, claim.getStatus());
    }

    @Test
    void testUpdateClaimStatus_NullParameters() {
        assertThrows(IllegalArgumentException.class, () -> 
            claimStatusService.updateClaimStatus(null, ClaimStatus.UNDER_REVIEW));
        
        assertThrows(IllegalArgumentException.class, () -> 
            claimStatusService.updateClaimStatus(claim, null));
    }

    @Test
    void testValidateClaim_Valid() {
        assertDoesNotThrow(() -> claimStatusService.validateClaim(claim));
    }

    @Test
    void testValidateClaim_NullClaim() {
        assertThrows(IllegalArgumentException.class, () -> 
            claimStatusService.validateClaim(null));
    }

    @Test
    void testValidateClaim_InvalidFields() {

        claim.setClaimNumber(null);
        assertThrows(IllegalArgumentException.class, () -> 
            claimStatusService.validateClaim(claim));
        
        claim.setClaimNumber("CLM12345678");
        claim.setIncidentDate(null);
        assertThrows(IllegalArgumentException.class, () -> 
            claimStatusService.validateClaim(claim));
        
        claim.setIncidentDate(LocalDate.now().minusDays(30));
        claim.setClaimAmount(null);
        assertThrows(IllegalArgumentException.class, () -> 
            claimStatusService.validateClaim(claim));
        
        claim.setClaimAmount(new BigDecimal("5000.00"));
        claim.setPolicy(null);
        assertThrows(IllegalArgumentException.class, () -> 
            claimStatusService.validateClaim(claim));
    }

    @Test
    void testValidateClaim_EdgeCases() {

        claim.setClaimAmount(new BigDecimal("0.01"));
        assertDoesNotThrow(() -> claimStatusService.validateClaim(claim));
        
        claim.setClaimAmount(policy.getCoverageAmount());
        assertDoesNotThrow(() -> claimStatusService.validateClaim(claim));
        
        claim.setIncidentDate(policy.getStartDate());
        assertDoesNotThrow(() -> claimStatusService.validateClaim(claim));
        
        claim.setIncidentDate(policy.getStartDate().plusDays(30));
        assertDoesNotThrow(() -> claimStatusService.validateClaim(claim));
    }

    @Test
    void testCompleteClaimWorkflow() {

        claim.setStatus(ClaimStatus.SUBMITTED);
        assertEquals(ClaimStatus.SUBMITTED, claim.getStatus());
        
        assertTrue(claimStatusService.canTransitionTo(claim.getStatus(), ClaimStatus.UNDER_REVIEW));
        claimStatusService.updateClaimStatus(claim, ClaimStatus.UNDER_REVIEW);
        assertEquals(ClaimStatus.UNDER_REVIEW, claim.getStatus());
        
        assertTrue(claimStatusService.canTransitionTo(claim.getStatus(), ClaimStatus.APPROVED));
        claimStatusService.updateClaimStatus(claim, ClaimStatus.APPROVED);
        assertEquals(ClaimStatus.APPROVED, claim.getStatus());
        
        assertTrue(claimStatusService.canTransitionTo(claim.getStatus(), ClaimStatus.PAID));
        claimStatusService.updateClaimStatus(claim, ClaimStatus.PAID);
        assertEquals(ClaimStatus.PAID, claim.getStatus());
        
        for (ClaimStatus status : ClaimStatus.values()) {
            if (status != ClaimStatus.PAID) {
                assertFalse(claimStatusService.canTransitionTo(ClaimStatus.PAID, status));
            }
        }
    }

    @Test
    void testRejectionWorkflow() {

        claim.setStatus(ClaimStatus.SUBMITTED);
        
        assertTrue(claimStatusService.canTransitionTo(claim.getStatus(), ClaimStatus.REJECTED));
        claimStatusService.updateClaimStatus(claim, ClaimStatus.REJECTED);
        assertEquals(ClaimStatus.REJECTED, claim.getStatus());
        
        for (ClaimStatus status : ClaimStatus.values()) {
            if (status != ClaimStatus.REJECTED) {
                assertFalse(claimStatusService.canTransitionTo(ClaimStatus.REJECTED, status));
            }
        }
    }

    @Test
    void testRejectionFromUnderReview() {

        claim.setStatus(ClaimStatus.SUBMITTED);
        claimStatusService.updateClaimStatus(claim, ClaimStatus.UNDER_REVIEW);
        assertEquals(ClaimStatus.UNDER_REVIEW, claim.getStatus());
        
        assertTrue(claimStatusService.canTransitionTo(claim.getStatus(), ClaimStatus.REJECTED));
        claimStatusService.updateClaimStatus(claim, ClaimStatus.REJECTED);
        assertEquals(ClaimStatus.REJECTED, claim.getStatus());
    }

    @Test
    void testValidateClaimWithDifferentPolicyTypes() {
        InsuranceCategory[] categories = InsuranceCategory.values();
        
        for (InsuranceCategory category : categories) {
            InsurancePolicy testPolicy = new InsurancePolicy();
            testPolicy.setPolicyNumber("POL-" + category.name());
            testPolicy.setStartDate(LocalDate.now().minusMonths(6));
            testPolicy.setEndDate(LocalDate.now().plusMonths(6));
            testPolicy.setPremium(new BigDecimal("100.00"));
            testPolicy.setCoverageAmount(new BigDecimal("50000.00"));
            testPolicy.setStatus(PolicyStatus.APPROVED);
            testPolicy.setType(category);
            
            Claim testClaim = new Claim();
            testClaim.setClaimNumber("CLM" + category.ordinal() + "1234567");
            testClaim.setIncidentDate(LocalDate.now().minusDays(30));
            testClaim.setDescription("Test claim for " + category.name());
            testClaim.setClaimAmount(new BigDecimal("5000.00"));
            testClaim.setPolicy(testPolicy);
            testClaim.setStatus(ClaimStatus.SUBMITTED);
            
            assertDoesNotThrow(() -> claimStatusService.validateClaim(testClaim));
        }
    }

    @Test
    void testValidateClaimWithBoundaryAmounts() {
        BigDecimal[] amounts = {
            new BigDecimal("0.01"),
            new BigDecimal("1.00"),
            new BigDecimal("100.00"),
            new BigDecimal("1000.00"),
            new BigDecimal("10000.00"),
            policy.getCoverageAmount().subtract(new BigDecimal("0.01")),
            policy.getCoverageAmount() // Exact coverage amount
        };
        
        for (BigDecimal amount : amounts) {
            claim.setClaimAmount(amount);
            assertDoesNotThrow(() -> claimStatusService.validateClaim(claim));
        }
    }

    @Test
    void testValidateClaimWithDifferentIncidentDates() {
        LocalDate[] dates = {
            policy.getStartDate(), // Policy start date
            policy.getStartDate().plusDays(1),
            policy.getStartDate().plusMonths(1),
            LocalDate.now().minusMonths(3),
            LocalDate.now().minusMonths(1),
            LocalDate.now().minusDays(1)
        };
        
        for (LocalDate date : dates) {
            claim.setIncidentDate(date);
            assertDoesNotThrow(() -> claimStatusService.validateClaim(claim));
        }
    }
} 