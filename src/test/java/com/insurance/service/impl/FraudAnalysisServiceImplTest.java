package com.insurance.service.impl;

import com.insurance.domain.PolicyRequest;
import com.insurance.domain.RiskAnalysis;
import com.insurance.domain.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class FraudAnalysisServiceImplTest {

    @InjectMocks
    private FraudAnalysisServiceImpl fraudAnalysisService;

    private PolicyRequest policyRequest;
    private UUID requestId;
    private UUID customerId;

    @BeforeEach
    void setUp() {
        requestId = UUID.randomUUID();
        customerId = UUID.randomUUID();

        policyRequest = new PolicyRequest();
        policyRequest.setId(requestId);
        policyRequest.setCustomerId(customerId);
        policyRequest.setCategory(InsuranceCategory.AUTO);
        policyRequest.setInsuredAmount(new BigDecimal("100000.00"));
        policyRequest.setSalesChannel(SalesChannel.MOBILE);
        policyRequest.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        policyRequest.setTotalMonthlyPremiumAmount(new BigDecimal("500.00"));
        policyRequest.setCoverages(new HashMap<>());
    }

    @Test
    void testAnalyzeFraudRegularCustomer() {

        policyRequest.setInsuredAmount(new BigDecimal("100000.00"));
        policyRequest.setCategory(InsuranceCategory.AUTO);

        RiskAnalysis result = fraudAnalysisService.analyzeFraud(policyRequest);

        assertNotNull(result);
        assertEquals(CustomerRiskType.REGULAR, result.getClassification());
        assertNotNull(result.getAnalyzedAt());
        assertTrue(result.getAnalyzedAt().isBefore(LocalDateTime.now().plusMinutes(1)));
        assertTrue(result.getOccurrences().isEmpty());
    }

    @Test
    void testAnalyzeFraudPreferredCustomer() {

        policyRequest.setInsuredAmount(new BigDecimal("200000.00"));
        policyRequest.setCategory(InsuranceCategory.AUTO);

        RiskAnalysis result = fraudAnalysisService.analyzeFraud(policyRequest);

        assertNotNull(result);
        assertEquals(CustomerRiskType.PREFERRED, result.getClassification());
        assertNotNull(result.getAnalyzedAt());
        assertTrue(result.getOccurrences().isEmpty());
    }

    @Test
    void testAnalyzeFraudHighRiskCustomer() {

        policyRequest.setInsuredAmount(new BigDecimal("400000.00"));
        policyRequest.setCategory(InsuranceCategory.AUTO);

        RiskAnalysis result = fraudAnalysisService.analyzeFraud(policyRequest);

        assertNotNull(result);
        assertEquals(CustomerRiskType.HIGH_RISK, result.getClassification());
        assertNotNull(result.getAnalyzedAt());
        assertEquals(1, result.getOccurrences().size());
        assertEquals("HIGH_VALUE", result.getOccurrences().get(0).getType());
    }

    @Test
    void testAnalyzeFraudExtremeValue() {

        policyRequest.setInsuredAmount(new BigDecimal("1500000.00"));
        policyRequest.setCategory(InsuranceCategory.AUTO);

        RiskAnalysis result = fraudAnalysisService.analyzeFraud(policyRequest);

        assertNotNull(result);
        assertEquals(CustomerRiskType.HIGH_RISK, result.getClassification());
        assertEquals(2, result.getOccurrences().size());
        
        List<String> occurrenceTypes = result.getOccurrences().stream()
            .map(occ -> occ.getType())
            .toList();
        assertTrue(occurrenceTypes.contains("HIGH_VALUE"));
        assertTrue(occurrenceTypes.contains("EXTREME_VALUE"));
    }

    @Test
    void testAnalyzeFraudDifferentCategories() {

        policyRequest.setCategory(InsuranceCategory.LIFE);
        policyRequest.setInsuredAmount(new BigDecimal("300000.00"));
        
        RiskAnalysis result = fraudAnalysisService.analyzeFraud(policyRequest);
        assertEquals(CustomerRiskType.PREFERRED, result.getClassification());

        policyRequest.setCategory(InsuranceCategory.RESIDENTIAL);
        policyRequest.setInsuredAmount(new BigDecimal("500000.00"));
        
        result = fraudAnalysisService.analyzeFraud(policyRequest);
        assertEquals(CustomerRiskType.HIGH_RISK, result.getClassification());

        policyRequest.setCategory(InsuranceCategory.TRAVEL);
        policyRequest.setInsuredAmount(new BigDecimal("150000.00"));
        
        result = fraudAnalysisService.analyzeFraud(policyRequest);
        assertEquals(CustomerRiskType.PREFERRED, result.getClassification());
    }

    @Test
    void testAnalyzeFraudWithNullRequest() {
        assertThrows(IllegalArgumentException.class, () ->
            fraudAnalysisService.analyzeFraud(null)
        );
    }

    @Test
    void testAnalyzeFraudWithNullRequestId() {
        policyRequest.setId(null);

        assertThrows(IllegalArgumentException.class, () ->
            fraudAnalysisService.analyzeFraud(policyRequest)
        );
    }

    @Test
    void testAnalyzeFraudWithNullCustomerId() {
        policyRequest.setCustomerId(null);

        assertThrows(IllegalArgumentException.class, () ->
            fraudAnalysisService.analyzeFraud(policyRequest)
        );
    }

    @Test
    void testAnalyzeFraudNullInsuredAmount() throws Exception {

        Field insuredAmountField = PolicyRequest.class.getDeclaredField("insuredAmount");
        insuredAmountField.setAccessible(true);
        insuredAmountField.set(policyRequest, null);

        assertThrows(IllegalArgumentException.class, () ->
            fraudAnalysisService.analyzeFraud(policyRequest)
        );
    }

    @Test
    void testAnalyzeFraudZeroInsuredAmount() throws Exception {
        // Usando reflection para setar valor zero sem passar pelo setter
        Field insuredAmountField = PolicyRequest.class.getDeclaredField("insuredAmount");
        insuredAmountField.setAccessible(true);
        insuredAmountField.set(policyRequest, BigDecimal.ZERO);
        
        assertThrows(IllegalArgumentException.class, () ->
            fraudAnalysisService.analyzeFraud(policyRequest)
        );
    }

    @Test
    void testAnalyzeFraudNegativeInsuredAmount() throws Exception {

        Field insuredAmountField = PolicyRequest.class.getDeclaredField("insuredAmount");
        insuredAmountField.setAccessible(true);
        insuredAmountField.set(policyRequest, new BigDecimal("-100000.00"));

        assertThrows(IllegalArgumentException.class, () ->
            fraudAnalysisService.analyzeFraud(policyRequest)
        );
    }

    @Test
    void testAnalyzeFraudWithNullCategory() throws Exception {

        Field categoryField = PolicyRequest.class.getDeclaredField("category");
        categoryField.setAccessible(true);
        categoryField.set(policyRequest, null);

        assertThrows(IllegalArgumentException.class, () ->
            fraudAnalysisService.analyzeFraud(policyRequest)
        );
    }

    @Test
    void testAnalyzeFraudAllCategories() {
        BigDecimal regularAmount = new BigDecimal("50000.00");
        
        for (InsuranceCategory category : InsuranceCategory.values()) {
            policyRequest.setCategory(category);
            policyRequest.setInsuredAmount(regularAmount);

            RiskAnalysis result = fraudAnalysisService.analyzeFraud(policyRequest);

            assertNotNull(result);
            assertEquals(CustomerRiskType.REGULAR, result.getClassification());
            assertNotNull(result.getAnalyzedAt());
        }
    }

    @Test
    void testAnalyzeFraudRuntimeError() throws Exception {

        Field customerIdField = PolicyRequest.class.getDeclaredField("customerId");
        customerIdField.setAccessible(true);
        customerIdField.set(policyRequest, null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            fraudAnalysisService.analyzeFraud(policyRequest)
        );
        
        assertTrue(exception.getMessage().contains("customerId cannot be null"));
    }
} 