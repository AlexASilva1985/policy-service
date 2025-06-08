package com.insurance.dto;

import com.insurance.domain.enums.InsuranceCategory;
import com.insurance.domain.enums.PaymentMethod;
import com.insurance.domain.enums.PolicyStatus;
import com.insurance.domain.enums.SalesChannel;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class PolicyRequestDTOTest {

    private Validator validator;
    private PolicyRequestDTO policyRequestDTO;
    private UUID customerId;
    private UUID productId;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        
        policyRequestDTO = new PolicyRequestDTO();
        customerId = UUID.randomUUID();
        productId = UUID.randomUUID();
        policyRequestDTO.setCustomerId(customerId);
        policyRequestDTO.setProductId(productId);
        policyRequestDTO.setCategory(InsuranceCategory.AUTO);
        policyRequestDTO.setSalesChannel(SalesChannel.MOBILE);
        policyRequestDTO.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        policyRequestDTO.setStatus(PolicyStatus.RECEIVED);
        policyRequestDTO.setTotalMonthlyPremiumAmount(BigDecimal.valueOf(150.00));
        policyRequestDTO.setInsuredAmount(BigDecimal.valueOf(50000.00));
        
        Map<String, BigDecimal> coverages = new HashMap<>();
        coverages.put("RCF", BigDecimal.valueOf(100000.00));
        policyRequestDTO.setCoverages(coverages);
        
        List<String> assistances = new ArrayList<>();
        assistances.add("24h Assistance");
        policyRequestDTO.setAssistances(assistances);
        
        List<StatusHistoryDTO> history = new ArrayList<>();
        StatusHistoryDTO historyItem = new StatusHistoryDTO();
        historyItem.setStatus(PolicyStatus.RECEIVED);
        historyItem.setTimestamp(LocalDateTime.now());
        history.add(historyItem);
        policyRequestDTO.setHistory(history);
    }

    @Test
    void testGettersAndSetters() {
        UUID id = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime finishedAt = LocalDateTime.now().plusDays(1);
        BigDecimal premium = BigDecimal.valueOf(200.00);
        BigDecimal insuredAmount = BigDecimal.valueOf(75000.00);

        policyRequestDTO.setId(id);
        policyRequestDTO.setCustomerId(customerId);
        policyRequestDTO.setProductId(productId);
        policyRequestDTO.setCategory(InsuranceCategory.LIFE);
        policyRequestDTO.setSalesChannel(SalesChannel.MOBILE);
        policyRequestDTO.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        policyRequestDTO.setStatus(PolicyStatus.VALIDATED);
        policyRequestDTO.setCreatedAt(createdAt);
        policyRequestDTO.setFinishedAt(finishedAt);
        policyRequestDTO.setTotalMonthlyPremiumAmount(premium);
        policyRequestDTO.setInsuredAmount(insuredAmount);

        assertEquals(id, policyRequestDTO.getId());
        assertEquals(customerId, policyRequestDTO.getCustomerId());
        assertEquals(productId, policyRequestDTO.getProductId());
        assertEquals(InsuranceCategory.LIFE, policyRequestDTO.getCategory());
        assertEquals(SalesChannel.MOBILE, policyRequestDTO.getSalesChannel());
        assertEquals(PaymentMethod.BANK_TRANSFER, policyRequestDTO.getPaymentMethod());
        assertEquals(PolicyStatus.VALIDATED, policyRequestDTO.getStatus());
        assertEquals(createdAt, policyRequestDTO.getCreatedAt());
        assertEquals(finishedAt, policyRequestDTO.getFinishedAt());
        assertEquals(premium, policyRequestDTO.getTotalMonthlyPremiumAmount());
        assertEquals(insuredAmount, policyRequestDTO.getInsuredAmount());
    }

    @Test
    void testValidDTO() {
        Set<ConstraintViolation<PolicyRequestDTO>> violations = validator.validate(policyRequestDTO);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testNullCustomerId() {
        policyRequestDTO.setCustomerId(null);
        
        Set<ConstraintViolation<PolicyRequestDTO>> violations = validator.validate(policyRequestDTO);
        
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().equals("Customer ID is required")));
    }

    @Test
    void testNullProductId() {
        policyRequestDTO.setProductId(null);
        
        Set<ConstraintViolation<PolicyRequestDTO>> violations = validator.validate(policyRequestDTO);
        
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().equals("Product ID is required")));
    }

    @Test
    void testNullCategory() {
        policyRequestDTO.setCategory(null);
        
        Set<ConstraintViolation<PolicyRequestDTO>> violations = validator.validate(policyRequestDTO);
        
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().equals("Category is required")));
    }

    @Test
    void testNullSalesChannel() {
        policyRequestDTO.setSalesChannel(null);
        
        Set<ConstraintViolation<PolicyRequestDTO>> violations = validator.validate(policyRequestDTO);
        
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().equals("Sales channel is required")));
    }

    @Test
    void testNullPaymentMethod() {
        policyRequestDTO.setPaymentMethod(null);
        
        Set<ConstraintViolation<PolicyRequestDTO>> violations = validator.validate(policyRequestDTO);
        
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().equals("Payment method is required")));
    }

    @Test
    void testNullTotalMonthlyPremiumAmount() {
        policyRequestDTO.setTotalMonthlyPremiumAmount(null);
        
        Set<ConstraintViolation<PolicyRequestDTO>> violations = validator.validate(policyRequestDTO);
        
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().equals("Total monthly premium amount is required")));
    }

    @Test
    void testNegativeTotalMonthlyPremiumAmount() {
        policyRequestDTO.setTotalMonthlyPremiumAmount(BigDecimal.valueOf(-100.00));
        
        Set<ConstraintViolation<PolicyRequestDTO>> violations = validator.validate(policyRequestDTO);
        
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().equals("Total monthly premium amount must be positive")));
    }

    @Test
    void testNullInsuredAmount() {
        policyRequestDTO.setInsuredAmount(null);
        
        Set<ConstraintViolation<PolicyRequestDTO>> violations = validator.validate(policyRequestDTO);
        
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().equals("Insured amount is required")));
    }

    @Test
    void testNegativeInsuredAmount() {
        policyRequestDTO.setInsuredAmount(BigDecimal.valueOf(-50000.00));
        
        Set<ConstraintViolation<PolicyRequestDTO>> violations = validator.validate(policyRequestDTO);
        
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().equals("Insured amount must be positive")));
    }

    @Test
    void testNullCoverages() {
        policyRequestDTO.setCoverages(null);
        
        Set<ConstraintViolation<PolicyRequestDTO>> violations = validator.validate(policyRequestDTO);
        
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().equals("Coverages are required")));
    }

    @Test
    void testCoveragesAndAssistances() {
        Map<String, BigDecimal> coverages = new HashMap<>();
        coverages.put("RCF", BigDecimal.valueOf(100000.00));
        coverages.put("APP", BigDecimal.valueOf(50000.00));
        
        List<String> assistances = new ArrayList<>();
        assistances.add("24h Assistance");
        assistances.add("Glass Coverage");
        
        policyRequestDTO.setCoverages(coverages);
        policyRequestDTO.setAssistances(assistances);
        
        assertEquals(2, policyRequestDTO.getCoverages().size());
        assertEquals(2, policyRequestDTO.getAssistances().size());
        assertTrue(policyRequestDTO.getCoverages().containsKey("RCF"));
        assertTrue(policyRequestDTO.getAssistances().contains("24h Assistance"));
    }

    @Test
    void testEqualsAndHashCode() {
        PolicyRequestDTO dto1 = new PolicyRequestDTO();
        PolicyRequestDTO dto2 = new PolicyRequestDTO();
        
        UUID id = UUID.randomUUID();
        dto1.setId(id);
        dto2.setId(id);
        dto1.setCustomerId(UUID.randomUUID());
        dto2.setCustomerId(dto1.getCustomerId());
        
        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
        
        dto2.setId(UUID.randomUUID());
        assertNotEquals(dto1, dto2);
    }

    @Test
    void testToString() {
        String toString = policyRequestDTO.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("PolicyRequestDTO"));
    }

    @Test
    void testDefaultCollections() {
        PolicyRequestDTO dto = new PolicyRequestDTO();
        assertNotNull(dto.getCoverages());
        assertNotNull(dto.getAssistances());
        assertTrue(dto.getCoverages().isEmpty());
        assertTrue(dto.getAssistances().isEmpty());
    }

    @Test
    void testAllInsuranceCategories() {
        for (InsuranceCategory category : InsuranceCategory.values()) {
            policyRequestDTO.setCategory(category);
            assertEquals(category, policyRequestDTO.getCategory());
        }
    }

    @Test
    void testAllSalesChannels() {
        for (SalesChannel channel : SalesChannel.values()) {
            policyRequestDTO.setSalesChannel(channel);
            assertEquals(channel, policyRequestDTO.getSalesChannel());
        }
    }

    @Test
    void testAllPaymentMethods() {
        for (PaymentMethod method : PaymentMethod.values()) {
            policyRequestDTO.setPaymentMethod(method);
            assertEquals(method, policyRequestDTO.getPaymentMethod());
        }
    }

    @Test
    void testCoveragesMap() {
        Map<String, BigDecimal> coverages = new HashMap<>();
        coverages.put("Comprehensive", new BigDecimal("75000.00"));
        coverages.put("Collision", new BigDecimal("25000.00"));
        coverages.put("Medical", new BigDecimal("10000.00"));
        
        policyRequestDTO.setCoverages(coverages);
        
        assertEquals(3, policyRequestDTO.getCoverages().size());
        assertEquals(new BigDecimal("75000.00"), policyRequestDTO.getCoverages().get("Comprehensive"));
        assertEquals(new BigDecimal("25000.00"), policyRequestDTO.getCoverages().get("Collision"));
        assertEquals(new BigDecimal("10000.00"), policyRequestDTO.getCoverages().get("Medical"));
    }

    @Test
    void testEmptyCoveragesMap() {
        policyRequestDTO.setCoverages(new HashMap<>());
        
        assertNotNull(policyRequestDTO.getCoverages());
        assertTrue(policyRequestDTO.getCoverages().isEmpty());
    }

    @Test
    void testAssistancesList() {
        List<String> assistances = Arrays.asList(
            "24/7 Roadside Assistance",
            "Towing Service",
            "Emergency Fuel Delivery",
            "Tire Change Service",
            "Battery Jump Start"
        );
        
        policyRequestDTO.setAssistances(assistances);
        
        assertEquals(5, policyRequestDTO.getAssistances().size());
        assertTrue(policyRequestDTO.getAssistances().contains("24/7 Roadside Assistance"));
        assertTrue(policyRequestDTO.getAssistances().contains("Towing Service"));
        assertTrue(policyRequestDTO.getAssistances().contains("Emergency Fuel Delivery"));
    }

    @Test
    void testEmptyAssistancesList() {
        policyRequestDTO.setAssistances(Arrays.asList());
        
        assertNotNull(policyRequestDTO.getAssistances());
        assertTrue(policyRequestDTO.getAssistances().isEmpty());
    }

    @Test
    void testBigDecimalValues() {
        // Test various BigDecimal scenarios
        policyRequestDTO.setTotalMonthlyPremiumAmount(new BigDecimal("99.99"));
        policyRequestDTO.setInsuredAmount(new BigDecimal("1000000.00"));
        
        assertEquals(new BigDecimal("99.99"), policyRequestDTO.getTotalMonthlyPremiumAmount());
        assertEquals(new BigDecimal("1000000.00"), policyRequestDTO.getInsuredAmount());
        
        // Test zero values
        policyRequestDTO.setTotalMonthlyPremiumAmount(BigDecimal.ZERO);
        assertEquals(BigDecimal.ZERO, policyRequestDTO.getTotalMonthlyPremiumAmount());
        
        // Test null values
        policyRequestDTO.setTotalMonthlyPremiumAmount(null);
        assertNull(policyRequestDTO.getTotalMonthlyPremiumAmount());
    }

    @Test
    void testCompleteAutoInsuranceScenario() {
        Map<String, BigDecimal> autoCoverages = new HashMap<>();
        autoCoverages.put("Liability", new BigDecimal("100000.00"));
        autoCoverages.put("Collision", new BigDecimal("50000.00"));
        autoCoverages.put("Comprehensive", new BigDecimal("50000.00"));
        autoCoverages.put("Personal Injury", new BigDecimal("20000.00"));
        
        List<String> autoAssistances = Arrays.asList(
            "24/7 Roadside Assistance",
            "Towing Service",
            "Emergency Locksmith",
            "Flat Tire Service"
        );
        
        PolicyRequestDTO autoPolicy = new PolicyRequestDTO();
        autoPolicy.setCustomerId(customerId);
        autoPolicy.setProductId(productId);
        autoPolicy.setCategory(InsuranceCategory.AUTO);
        autoPolicy.setSalesChannel(SalesChannel.MOBILE);
        autoPolicy.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        autoPolicy.setTotalMonthlyPremiumAmount(new BigDecimal("180.00"));
        autoPolicy.setInsuredAmount(new BigDecimal("220000.00"));
        autoPolicy.setCoverages(autoCoverages);
        autoPolicy.setAssistances(autoAssistances);
        
        assertEquals(InsuranceCategory.AUTO, autoPolicy.getCategory());
        assertEquals(SalesChannel.MOBILE, autoPolicy.getSalesChannel());
        assertEquals(PaymentMethod.CREDIT_CARD, autoPolicy.getPaymentMethod());
        assertEquals(4, autoPolicy.getCoverages().size());
        assertEquals(4, autoPolicy.getAssistances().size());
        assertTrue(autoPolicy.getCoverages().containsKey("Liability"));
        assertTrue(autoPolicy.getAssistances().contains("24/7 Roadside Assistance"));
    }

    @Test
    void testCompleteHomeInsuranceScenario() {
        Map<String, BigDecimal> homeCoverages = new HashMap<>();
        homeCoverages.put("Dwelling", new BigDecimal("500000.00"));
        homeCoverages.put("Personal Property", new BigDecimal("250000.00"));
        homeCoverages.put("Liability", new BigDecimal("300000.00"));
        
        List<String> homeAssistances = Arrays.asList(
            "Emergency Home Repair",
            "Temporary Housing",
            "Security Monitoring"
        );
        
        PolicyRequestDTO homePolicy = new PolicyRequestDTO();
        homePolicy.setCustomerId(customerId);
        homePolicy.setProductId(productId);
        homePolicy.setCategory(InsuranceCategory.AUTO);
        homePolicy.setSalesChannel(SalesChannel.WEBSITE);
        homePolicy.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        homePolicy.setTotalMonthlyPremiumAmount(new BigDecimal("250.00"));
        homePolicy.setInsuredAmount(new BigDecimal("1050000.00"));
        homePolicy.setCoverages(homeCoverages);
        homePolicy.setAssistances(homeAssistances);
        
        assertEquals(InsuranceCategory.AUTO, homePolicy.getCategory());
        assertEquals(SalesChannel.WEBSITE, homePolicy.getSalesChannel());
        assertEquals(PaymentMethod.BANK_TRANSFER, homePolicy.getPaymentMethod());
        assertEquals(3, homePolicy.getCoverages().size());
        assertEquals(3, homePolicy.getAssistances().size());
    }
} 