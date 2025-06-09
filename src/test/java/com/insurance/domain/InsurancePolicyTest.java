package com.insurance.domain;

import com.insurance.domain.enums.InsuranceCategory;
import com.insurance.domain.enums.PolicyStatus;
import com.insurance.service.InsurancePolicyValidationService;
import com.insurance.service.impl.InsurancePolicyValidationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for InsurancePolicy entity after refactoring to follow Single Responsibility Principle.
 * The entity now only handles JPA mapping, while business logic is tested through InsurancePolicyValidationService.
 */
class InsurancePolicyTest {

    private InsurancePolicy policy;
    private InsurancePolicyValidationService validationService;
    private LocalDate today;
    private LocalDate tomorrow;
    private LocalDate yesterday;

    @BeforeEach
    void setUp() {
        validationService = new InsurancePolicyValidationServiceImpl();
        policy = new InsurancePolicy();
        today = LocalDate.now();
        tomorrow = today.plusDays(1);
        yesterday = today.minusDays(1);
    }

    // ========== ENTITY MAPPING TESTS ==========

    @Test
    void testCreateValidPolicy() {
        policy.setPolicyNumber("POL-12345");
        policy.setStartDate(today);
        policy.setEndDate(tomorrow);
        policy.setPremium(BigDecimal.valueOf(1000));
        policy.setCoverageAmount(BigDecimal.valueOf(50000));
        policy.setStatus(PolicyStatus.RECEIVED);
        policy.setType(InsuranceCategory.AUTO);

        assertEquals("POL-12345", policy.getPolicyNumber());
        assertEquals(today, policy.getStartDate());
        assertEquals(tomorrow, policy.getEndDate());
        assertEquals(BigDecimal.valueOf(1000), policy.getPremium());
        assertEquals(BigDecimal.valueOf(50000), policy.getCoverageAmount());
        assertEquals(PolicyStatus.RECEIVED, policy.getStatus());
        assertEquals(InsuranceCategory.AUTO, policy.getType());
    }

    @Test
    void testEntitySettersAndGetters() {
        InsurancePolicy testPolicy = new InsurancePolicy();
        
        // Test all setters and getters work without business validation
        testPolicy.setPolicyNumber("INVALID"); // Invalid format - no validation in entity
        assertEquals("INVALID", testPolicy.getPolicyNumber());
        
        LocalDate futureDate = LocalDate.of(2030, 12, 31);
        LocalDate pastDate = LocalDate.of(2020, 1, 1);
        
        // Set start date after end date - no validation in entity
        testPolicy.setStartDate(futureDate);
        testPolicy.setEndDate(pastDate);
        assertEquals(futureDate, testPolicy.getStartDate());
        assertEquals(pastDate, testPolicy.getEndDate());
        
        BigDecimal negativePremium = new BigDecimal("-100.00"); // Negative - no validation in entity
        testPolicy.setPremium(negativePremium);
        assertEquals(negativePremium, testPolicy.getPremium());
        
        BigDecimal zeroCoverage = BigDecimal.ZERO; // Zero - no validation in entity
        testPolicy.setCoverageAmount(zeroCoverage);
        assertEquals(zeroCoverage, testPolicy.getCoverageAmount());
        
        testPolicy.setStatus(PolicyStatus.APPROVED);
        assertEquals(PolicyStatus.APPROVED, testPolicy.getStatus());
        
        testPolicy.setType(InsuranceCategory.LIFE);
        assertEquals(InsuranceCategory.LIFE, testPolicy.getType());
        
        Customer customer = new Customer();
        testPolicy.setCustomer(customer);
        assertEquals(customer, testPolicy.getCustomer());
    }

    // ========== BUSINESS LOGIC TESTS (Via InsurancePolicyValidationService) ==========

    @Test
    void testPolicyNumberValidationViaService() {
        // Valid policy number
        assertDoesNotThrow(() -> validationService.validatePolicyNumber("POL-12345"));
        
        // Invalid policy numbers
        assertThrows(IllegalArgumentException.class, () -> 
            validationService.validatePolicyNumber(null));
        assertThrows(IllegalArgumentException.class, () -> 
            validationService.validatePolicyNumber(""));
        assertThrows(IllegalArgumentException.class, () -> 
            validationService.validatePolicyNumber("   "));
        assertThrows(IllegalArgumentException.class, () -> 
            validationService.validatePolicyNumber("INVALID-12345"));
    }

    @Test
    void testDateValidationViaService() {
        // Valid date combinations
        assertDoesNotThrow(() -> 
            validationService.validateStartDate(today, tomorrow));
        assertDoesNotThrow(() -> 
            validationService.validateEndDate(tomorrow, today));
        assertDoesNotThrow(() -> 
            validationService.validateDateConsistency(today, tomorrow));
        
        // Invalid dates
        assertThrows(IllegalArgumentException.class, () -> 
            validationService.validateStartDate(null, tomorrow));
        assertThrows(IllegalArgumentException.class, () -> 
            validationService.validateEndDate(null, today));
        assertThrows(IllegalArgumentException.class, () -> 
            validationService.validateStartDate(tomorrow, today)); // start after end
        assertThrows(IllegalArgumentException.class, () -> 
            validationService.validateEndDate(yesterday, today)); // end before start
    }

    @Test
    void testAmountValidationViaService() {
        // Valid amounts
        assertDoesNotThrow(() -> 
            validationService.validatePremium(new BigDecimal("100.00")));
        assertDoesNotThrow(() -> 
            validationService.validateCoverageAmount(new BigDecimal("50000.00")));
        
        // Invalid amounts
        assertThrows(IllegalArgumentException.class, () -> 
            validationService.validatePremium(null));
        assertThrows(IllegalArgumentException.class, () -> 
            validationService.validatePremium(BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> 
            validationService.validatePremium(new BigDecimal("-100.00")));
        
        assertThrows(IllegalArgumentException.class, () -> 
            validationService.validateCoverageAmount(null));
        assertThrows(IllegalArgumentException.class, () -> 
            validationService.validateCoverageAmount(BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> 
            validationService.validateCoverageAmount(new BigDecimal("-50000.00")));
    }

    @Test
    void testStatusAndTypeValidationViaService() {
        // Valid values
        assertDoesNotThrow(() -> 
            validationService.validateStatus(PolicyStatus.RECEIVED));
        assertDoesNotThrow(() -> 
            validationService.validateType(InsuranceCategory.AUTO));
        
        // Invalid values
        assertThrows(IllegalArgumentException.class, () -> 
            validationService.validateStatus(null));
        assertThrows(IllegalArgumentException.class, () -> 
            validationService.validateType(null));
    }

    @Test
    void testCompleteValidationViaService() {
        // Valid policy
        policy.setPolicyNumber("POL-12345");
        policy.setStartDate(today);
        policy.setEndDate(tomorrow);
        policy.setPremium(BigDecimal.valueOf(1000));
        policy.setCoverageAmount(BigDecimal.valueOf(50000));
        policy.setStatus(PolicyStatus.RECEIVED);
        policy.setType(InsuranceCategory.AUTO);
        
        assertDoesNotThrow(() -> validationService.validateInsurancePolicy(policy));
        
        // Invalid policy - missing required fields
        InsurancePolicy invalidPolicy = new InsurancePolicy();
        assertThrows(IllegalArgumentException.class, () -> 
            validationService.validateInsurancePolicy(invalidPolicy));
    }

    // ========== INTEGRATION TESTS ==========

    @Test
    void testCompleteValidationWorkflowWithService() {
        // Build valid policy step by step with validation
        InsurancePolicy workflowPolicy = new InsurancePolicy();
        workflowPolicy.setPolicyNumber("POL-WORKFLOW-2024");
        workflowPolicy.setStartDate(today);
        workflowPolicy.setEndDate(today.plusYears(1));
        workflowPolicy.setPremium(new BigDecimal("1500.00"));
        workflowPolicy.setCoverageAmount(new BigDecimal("75000.00"));
        workflowPolicy.setStatus(PolicyStatus.VALIDATED);
        workflowPolicy.setType(InsuranceCategory.RESIDENTIAL);
        
        // Validate individual components
        assertDoesNotThrow(() -> validationService.validatePolicyNumber(workflowPolicy.getPolicyNumber()));
        assertDoesNotThrow(() -> validationService.validateDateConsistency(workflowPolicy.getStartDate(), workflowPolicy.getEndDate()));
        assertDoesNotThrow(() -> validationService.validatePremium(workflowPolicy.getPremium()));
        assertDoesNotThrow(() -> validationService.validateCoverageAmount(workflowPolicy.getCoverageAmount()));
        assertDoesNotThrow(() -> validationService.validateStatus(workflowPolicy.getStatus()));
        assertDoesNotThrow(() -> validationService.validateType(workflowPolicy.getType()));
        
        // Validate complete policy
        assertDoesNotThrow(() -> validationService.validateInsurancePolicy(workflowPolicy));
    }

    @Test
    void testEntityInheritanceFromBaseEntity() {
        // Test inheritance structure
        assertTrue(policy instanceof BaseEntity);
        
        // Test that we can set/get BaseEntity fields
        UUID testId = UUID.randomUUID();
        policy.setId(testId);
        assertEquals(testId, policy.getId());
        
        String testUser = "test-user";
        policy.setCreatedBy(testUser);
        assertEquals(testUser, policy.getCreatedBy());
        
        policy.setUpdatedBy(testUser);
        assertEquals(testUser, policy.getUpdatedBy());
    }

    @Test
    void testAllInsuranceCategoryTypes() {
        for (InsuranceCategory category : InsuranceCategory.values()) {
            InsurancePolicy testPolicy = new InsurancePolicy();
            testPolicy.setType(category);
            assertEquals(category, testPolicy.getType());
            
            // Validate through service
            assertDoesNotThrow(() -> validationService.validateType(category));
        }
    }

    @Test
    void testAllPolicyStatusTypes() {
        for (PolicyStatus status : PolicyStatus.values()) {
            InsurancePolicy testPolicy = new InsurancePolicy();
            testPolicy.setStatus(status);
            assertEquals(status, testPolicy.getStatus());
            
            // Validate through service
            assertDoesNotThrow(() -> validationService.validateStatus(status));
        }
    }
} 