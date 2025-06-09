package com.insurance.domain;

import com.insurance.domain.enums.InsuranceCategory;
import com.insurance.domain.enums.PaymentMethod;
import com.insurance.domain.enums.PolicyStatus;
import com.insurance.domain.enums.SalesChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("PolicyRequest Entity Tests")
class PolicyRequestTest {

    private PolicyRequest policyRequest;
    private UUID customerId;
    private UUID productId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        productId = UUID.randomUUID();
        
        policyRequest = new PolicyRequest();
        policyRequest.setCustomerId(customerId);
        policyRequest.setProductId(productId);
        policyRequest.setCategory(InsuranceCategory.AUTO);
        policyRequest.setSalesChannel(SalesChannel.WEBSITE);
        policyRequest.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        policyRequest.setTotalMonthlyPremiumAmount(new BigDecimal("150.00"));
        policyRequest.setInsuredAmount(new BigDecimal("50000.00"));
    }

    @Nested
    @DisplayName("Basic Property Tests")
    class BasicPropertyTests {

        @Test
        @DisplayName("Should create PolicyRequest with all required fields")
        void shouldCreatePolicyRequestWithAllRequiredFields() {
            assertThat(policyRequest).isNotNull();
            assertThat(policyRequest.getCustomerId()).isEqualTo(customerId);
            assertThat(policyRequest.getProductId()).isEqualTo(productId);
            assertThat(policyRequest.getCategory()).isEqualTo(InsuranceCategory.AUTO);
            assertThat(policyRequest.getSalesChannel()).isEqualTo(SalesChannel.WEBSITE);
            assertThat(policyRequest.getPaymentMethod()).isEqualTo(PaymentMethod.CREDIT_CARD);
            assertThat(policyRequest.getTotalMonthlyPremiumAmount()).isEqualTo(new BigDecimal("150.00"));
            assertThat(policyRequest.getInsuredAmount()).isEqualTo(new BigDecimal("50000.00"));
        }

        @Test
        @DisplayName("Should have default status as RECEIVED")
        void shouldHaveDefaultStatusAsReceived() {
            PolicyRequest newRequest = new PolicyRequest();
            
            assertThat(newRequest.getStatus()).isEqualTo(PolicyStatus.RECEIVED);
        }

        @Test
        @DisplayName("Should allow updating status")
        void shouldAllowUpdatingStatus() {
            policyRequest.setStatus(PolicyStatus.APPROVED);
            
            assertThat(policyRequest.getStatus()).isEqualTo(PolicyStatus.APPROVED);
        }

        @Test
        @DisplayName("Should set and get finishedAt timestamp")
        void shouldSetAndGetFinishedAtTimestamp() {
            LocalDateTime finishedTime = LocalDateTime.now();
            
            policyRequest.setFinishedAt(finishedTime);
            
            assertThat(policyRequest.getFinishedAt()).isEqualTo(finishedTime);
        }
    }

    @Nested
    @DisplayName("Coverage Calculation Tests")
    class CoverageCalculationTests {

        @Test
        @DisplayName("Should calculate total coverage amount with multiple coverages")
        void shouldCalculateTotalCoverageAmountWithMultipleCoverages() {
            Map<String, BigDecimal> coverages = new HashMap<>();
            coverages.put("Collision", new BigDecimal("10000.00"));
            coverages.put("Comprehensive", new BigDecimal("15000.00"));
            coverages.put("Liability", new BigDecimal("25000.00"));
            
            policyRequest.setCoverages(coverages);
            
            BigDecimal totalCoverage = policyRequest.calculateTotalCoverageAmount();
            
            assertThat(totalCoverage).isEqualTo(new BigDecimal("50000.00"));
        }

        @Test
        @DisplayName("Should return zero when no coverages are present")
        void shouldReturnZeroWhenNoCoveragesArePresent() {
            policyRequest.setCoverages(new HashMap<>());
            
            BigDecimal totalCoverage = policyRequest.calculateTotalCoverageAmount();
            
            assertThat(totalCoverage).isEqualTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should handle single coverage correctly")
        void shouldHandleSingleCoverageCorrectly() {
            Map<String, BigDecimal> coverages = new HashMap<>();
            coverages.put("Collision", new BigDecimal("12000.00"));
            
            policyRequest.setCoverages(coverages);
            
            BigDecimal totalCoverage = policyRequest.calculateTotalCoverageAmount();
            
            assertThat(totalCoverage).isEqualTo(new BigDecimal("12000.00"));
        }

        @Test
        @DisplayName("Should handle decimal coverage amounts correctly")
        void shouldHandleDecimalCoverageAmountsCorrectly() {
            Map<String, BigDecimal> coverages = new HashMap<>();
            coverages.put("Coverage1", new BigDecimal("1000.50"));
            coverages.put("Coverage2", new BigDecimal("2000.75"));
            coverages.put("Coverage3", new BigDecimal("500.25"));
            
            policyRequest.setCoverages(coverages);
            
            BigDecimal totalCoverage = policyRequest.calculateTotalCoverageAmount();
            
            assertThat(totalCoverage).isEqualTo(new BigDecimal("3501.50"));
        }
    }

    @Nested
    @DisplayName("Collections Tests")
    class CollectionsTests {

        @Test
        @DisplayName("Should initialize coverages as empty HashMap")
        void shouldInitializeCoveragesAsEmptyHashMap() {
            PolicyRequest newRequest = new PolicyRequest();
            
            assertThat(newRequest.getCoverages()).isNotNull();
            assertThat(newRequest.getCoverages()).isEmpty();
            assertThat(newRequest.getCoverages()).isInstanceOf(HashMap.class);
        }

        @Test
        @DisplayName("Should initialize assistances as empty ArrayList")
        void shouldInitializeAssistancesAsEmptyArrayList() {
            PolicyRequest newRequest = new PolicyRequest();
            
            assertThat(newRequest.getAssistances()).isNotNull();
            assertThat(newRequest.getAssistances()).isEmpty();
            assertThat(newRequest.getAssistances()).isInstanceOf(List.class);
        }

        @Test
        @DisplayName("Should initialize statusHistory as empty ArrayList")
        void shouldInitializeStatusHistoryAsEmptyArrayList() {
            PolicyRequest newRequest = new PolicyRequest();
            
            assertThat(newRequest.getStatusHistory()).isNotNull();
            assertThat(newRequest.getStatusHistory()).isEmpty();
            assertThat(newRequest.getStatusHistory()).isInstanceOf(List.class);
        }

        @Test
        @DisplayName("Should add and retrieve assistances")
        void shouldAddAndRetrieveAssistances() {
            policyRequest.getAssistances().add("24h Towing");
            policyRequest.getAssistances().add("Roadside Assistance");
            policyRequest.getAssistances().add("Car Rental");
            
            assertThat(policyRequest.getAssistances()).hasSize(3);
            assertThat(policyRequest.getAssistances()).containsExactly(
                "24h Towing", 
                "Roadside Assistance", 
                "Car Rental"
            );
        }

        @Test
        @DisplayName("Should add and retrieve coverages")
        void shouldAddAndRetrieveCoverages() {
            policyRequest.getCoverages().put("Collision", new BigDecimal("15000"));
            policyRequest.getCoverages().put("Comprehensive", new BigDecimal("20000"));
            
            assertThat(policyRequest.getCoverages()).hasSize(2);
            assertThat(policyRequest.getCoverages().get("Collision")).isEqualTo(new BigDecimal("15000"));
            assertThat(policyRequest.getCoverages().get("Comprehensive")).isEqualTo(new BigDecimal("20000"));
        }
    }

    @Nested
    @DisplayName("Enum Tests")
    class EnumTests {

        @Test
        @DisplayName("Should set and get all InsuranceCategory values")
        void shouldSetAndGetAllInsuranceCategoryValues() {
            policyRequest.setCategory(InsuranceCategory.AUTO);
            assertThat(policyRequest.getCategory()).isEqualTo(InsuranceCategory.AUTO);
            
            policyRequest.setCategory(InsuranceCategory.RESIDENTIAL);
            assertThat(policyRequest.getCategory()).isEqualTo(InsuranceCategory.RESIDENTIAL);
            
            policyRequest.setCategory(InsuranceCategory.LIFE);
            assertThat(policyRequest.getCategory()).isEqualTo(InsuranceCategory.LIFE);
            
            policyRequest.setCategory(InsuranceCategory.HEALTH);
            assertThat(policyRequest.getCategory()).isEqualTo(InsuranceCategory.HEALTH);
            
            policyRequest.setCategory(InsuranceCategory.TRAVEL);
            assertThat(policyRequest.getCategory()).isEqualTo(InsuranceCategory.TRAVEL);
        }

        @Test
        @DisplayName("Should set and get all SalesChannel values")
        void shouldSetAndGetAllSalesChannelValues() {
            policyRequest.setSalesChannel(SalesChannel.WEBSITE);
            assertThat(policyRequest.getSalesChannel()).isEqualTo(SalesChannel.WEBSITE);
            
            policyRequest.setSalesChannel(SalesChannel.MOBILE);
            assertThat(policyRequest.getSalesChannel()).isEqualTo(SalesChannel.MOBILE);
            
            policyRequest.setSalesChannel(SalesChannel.BROKER);
            assertThat(policyRequest.getSalesChannel()).isEqualTo(SalesChannel.BROKER);
            
            policyRequest.setSalesChannel(SalesChannel.CALL_CENTER);
            assertThat(policyRequest.getSalesChannel()).isEqualTo(SalesChannel.CALL_CENTER);
            
            policyRequest.setSalesChannel(SalesChannel.BANK);
            assertThat(policyRequest.getSalesChannel()).isEqualTo(SalesChannel.BANK);
        }

        @Test
        @DisplayName("Should set and get all PaymentMethod values")
        void shouldSetAndGetAllPaymentMethodValues() {
            policyRequest.setPaymentMethod(PaymentMethod.CREDIT_CARD);
            assertThat(policyRequest.getPaymentMethod()).isEqualTo(PaymentMethod.CREDIT_CARD);
            
            policyRequest.setPaymentMethod(PaymentMethod.DEBIT_CARD);
            assertThat(policyRequest.getPaymentMethod()).isEqualTo(PaymentMethod.DEBIT_CARD);
            
            policyRequest.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
            assertThat(policyRequest.getPaymentMethod()).isEqualTo(PaymentMethod.BANK_TRANSFER);
            
            policyRequest.setPaymentMethod(PaymentMethod.PIX);
            assertThat(policyRequest.getPaymentMethod()).isEqualTo(PaymentMethod.PIX);
        }

        @Test
        @DisplayName("Should set and get all PolicyStatus values")
        void shouldSetAndGetAllPolicyStatusValues() {
            policyRequest.setStatus(PolicyStatus.RECEIVED);
            assertThat(policyRequest.getStatus()).isEqualTo(PolicyStatus.RECEIVED);
            
            policyRequest.setStatus(PolicyStatus.PENDING);
            assertThat(policyRequest.getStatus()).isEqualTo(PolicyStatus.PENDING);
            
            policyRequest.setStatus(PolicyStatus.APPROVED);
            assertThat(policyRequest.getStatus()).isEqualTo(PolicyStatus.APPROVED);
            
            policyRequest.setStatus(PolicyStatus.REJECTED);
            assertThat(policyRequest.getStatus()).isEqualTo(PolicyStatus.REJECTED);
            
            policyRequest.setStatus(PolicyStatus.CANCELLED);
            assertThat(policyRequest.getStatus()).isEqualTo(PolicyStatus.CANCELLED);
        }
    }

    @Nested
    @DisplayName("Relationship Tests")
    class RelationshipTests {

        @Test
        @DisplayName("Should set and get RiskAnalysis")
        void shouldSetAndGetRiskAnalysis() {
            RiskAnalysis riskAnalysis = new RiskAnalysis();
            
            policyRequest.setRiskAnalysis(riskAnalysis);
            
            assertThat(policyRequest.getRiskAnalysis()).isSameAs(riskAnalysis);
        }

        @Test
        @DisplayName("Should allow null RiskAnalysis")
        void shouldAllowNullRiskAnalysis() {
            policyRequest.setRiskAnalysis(null);
            
            assertThat(policyRequest.getRiskAnalysis()).isNull();
        }

        @Test
        @DisplayName("Should add StatusHistory entries")
        void shouldAddStatusHistoryEntries() {
            StatusHistory history1 = new StatusHistory();
            StatusHistory history2 = new StatusHistory();
            
            policyRequest.getStatusHistory().add(history1);
            policyRequest.getStatusHistory().add(history2);
            
            assertThat(policyRequest.getStatusHistory()).hasSize(2);
            assertThat(policyRequest.getStatusHistory()).containsExactly(history1, history2);
        }
    }

    @Nested
    @DisplayName("BigDecimal Amount Tests")
    class BigDecimalAmountTests {

        @Test
        @DisplayName("Should handle zero amounts")
        void shouldHandleZeroAmounts() {
            policyRequest.setInsuredAmount(BigDecimal.ZERO);
            policyRequest.setTotalMonthlyPremiumAmount(BigDecimal.ZERO);
            
            assertThat(policyRequest.getInsuredAmount()).isEqualTo(BigDecimal.ZERO);
            assertThat(policyRequest.getTotalMonthlyPremiumAmount()).isEqualTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should handle large amounts")
        void shouldHandleLargeAmounts() {
            BigDecimal largeAmount = new BigDecimal("999999999.99");
            
            policyRequest.setInsuredAmount(largeAmount);
            policyRequest.setTotalMonthlyPremiumAmount(largeAmount);
            
            assertThat(policyRequest.getInsuredAmount()).isEqualTo(largeAmount);
            assertThat(policyRequest.getTotalMonthlyPremiumAmount()).isEqualTo(largeAmount);
        }

        @Test
        @DisplayName("Should preserve decimal precision")
        void shouldPreserveDecimalPrecision() {
            BigDecimal preciseAmount = new BigDecimal("123.456789");
            
            policyRequest.setInsuredAmount(preciseAmount);
            
            assertThat(policyRequest.getInsuredAmount()).isEqualTo(preciseAmount);
            assertThat(policyRequest.getInsuredAmount().scale()).isEqualTo(6);
        }
    }

    @Nested
    @DisplayName("Lombok Generated Methods Tests")
    class LombokGeneratedMethodsTests {

        @Test
        @DisplayName("Should implement equals and hashCode correctly")
        void shouldImplementEqualsAndHashCodeCorrectly() {
            PolicyRequest request1 = new PolicyRequest();
            request1.setId(UUID.randomUUID());
            request1.setCustomerId(customerId);
            request1.setCategory(InsuranceCategory.AUTO);
            
            PolicyRequest request2 = new PolicyRequest();
            request2.setId(request1.getId());
            request2.setCustomerId(customerId);
            request2.setCategory(InsuranceCategory.AUTO);
            
            PolicyRequest request3 = new PolicyRequest();
            request3.setId(UUID.randomUUID()); // Different ID
            request3.setCustomerId(customerId);
            request3.setCategory(InsuranceCategory.AUTO);
            
            assertThat(request1).isEqualTo(request2);
            assertThat(request1).isNotEqualTo(request3);
            assertThat(request1.hashCode()).isEqualTo(request2.hashCode());
        }

        @Test
        @DisplayName("Should implement toString correctly")
        void shouldImplementToStringCorrectly() {
            String toString = policyRequest.toString();
            
            assertThat(toString).contains("PolicyRequest");
            assertThat(toString).contains(customerId.toString());
            assertThat(toString).contains(productId.toString());
            assertThat(toString).contains("AUTO");
        }
    }

    @Nested
    @DisplayName("Edge Cases and Null Handling")
    class EdgeCasesAndNullHandlingTests {

        @Test
        @DisplayName("Should handle null UUIDs")
        void shouldHandleNullUUIDs() {
            policyRequest.setCustomerId(null);
            policyRequest.setProductId(null);
            
            assertThat(policyRequest.getCustomerId()).isNull();
            assertThat(policyRequest.getProductId()).isNull();
        }

        @Test
        @DisplayName("Should handle null BigDecimal amounts")
        void shouldHandleNullBigDecimalAmounts() {
            policyRequest.setInsuredAmount(null);
            policyRequest.setTotalMonthlyPremiumAmount(null);
            
            assertThat(policyRequest.getInsuredAmount()).isNull();
            assertThat(policyRequest.getTotalMonthlyPremiumAmount()).isNull();
        }

        @Test
        @DisplayName("Should handle calculateTotalCoverageAmount with null map")
        void shouldHandleCalculateTotalCoverageAmountWithNullMap() {
            policyRequest.setCoverages(null);
            
            assertThatThrownBy(() -> policyRequest.calculateTotalCoverageAmount())
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should handle empty coverages map in calculation")
        void shouldHandleEmptyCoveragesMapInCalculation() {
            policyRequest.setCoverages(new HashMap<>());
            
            BigDecimal result = policyRequest.calculateTotalCoverageAmount();
            
            assertThat(result).isEqualTo(BigDecimal.ZERO);
        }
    }
} 