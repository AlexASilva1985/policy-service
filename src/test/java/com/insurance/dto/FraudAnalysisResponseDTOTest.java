package com.insurance.dto;

import com.insurance.domain.enums.CustomerRiskType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FraudAnalysisResponseDTOTest {

    private FraudAnalysisResponseDTO response;
    private UUID orderId;
    private UUID customerId;

    @BeforeEach
    void setUp() {
        response = new FraudAnalysisResponseDTO();
        orderId = UUID.randomUUID();
        customerId = UUID.randomUUID();
    }

    @Test
    void testGettersAndSetters() {
        LocalDateTime analyzedAt = LocalDateTime.now();
        List<FraudAnalysisResponseDTO.RiskOccurrenceDTO> occurrences = new ArrayList<>();
        
        response.setOrderId(orderId);
        response.setCustomerId(customerId);
        response.setAnalyzedAt(analyzedAt);
        response.setClassification(CustomerRiskType.HIGH_RISK);
        response.setOccurrences(occurrences);
        
        assertEquals(orderId, response.getOrderId());
        assertEquals(customerId, response.getCustomerId());
        assertEquals(analyzedAt, response.getAnalyzedAt());
        assertEquals(CustomerRiskType.HIGH_RISK, response.getClassification());
        assertEquals(occurrences, response.getOccurrences());
    }

    @Test
    void testEqualsAndHashCode() {
        LocalDateTime analyzedAt = LocalDateTime.now();
        
        FraudAnalysisResponseDTO response1 = new FraudAnalysisResponseDTO();
        response1.setOrderId(orderId);
        response1.setCustomerId(customerId);
        response1.setAnalyzedAt(analyzedAt);
        response1.setClassification(CustomerRiskType.REGULAR);
        
        FraudAnalysisResponseDTO response2 = new FraudAnalysisResponseDTO();
        response2.setOrderId(orderId);
        response2.setCustomerId(customerId);
        response2.setAnalyzedAt(analyzedAt);
        response2.setClassification(CustomerRiskType.REGULAR);
        
        assertEquals(response1, response2);
        assertEquals(response1.hashCode(), response2.hashCode());
        
        response2.setClassification(CustomerRiskType.HIGH_RISK);
        assertNotEquals(response1, response2);
    }

    @Test
    void testToString() {
        response.setOrderId(orderId);
        response.setCustomerId(customerId);
        response.setClassification(CustomerRiskType.PREFERRED);
        
        String toString = response.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("FraudAnalysisResponseDTO"));
        assertTrue(toString.contains(orderId.toString()));
        assertTrue(toString.contains("PREFERRED"));
    }

    @Test
    void testNullValues() {
        FraudAnalysisResponseDTO emptyResponse = new FraudAnalysisResponseDTO();
        
        assertNull(emptyResponse.getOrderId());
        assertNull(emptyResponse.getCustomerId());
        assertNull(emptyResponse.getAnalyzedAt());
        assertNull(emptyResponse.getClassification());
        assertNull(emptyResponse.getOccurrences());
    }

    @Test
    void testWithOccurrences() {
        FraudAnalysisResponseDTO.RiskOccurrenceDTO occurrence1 = createOccurrence("HIGH_VALUE", "High value transaction detected");
        FraudAnalysisResponseDTO.RiskOccurrenceDTO occurrence2 = createOccurrence("EXTREME_VALUE", "Extremely high value detected");
        
        List<FraudAnalysisResponseDTO.RiskOccurrenceDTO> occurrences = Arrays.asList(occurrence1, occurrence2);
        
        response.setOrderId(orderId);
        response.setCustomerId(customerId);
        response.setClassification(CustomerRiskType.HIGH_RISK);
        response.setOccurrences(occurrences);
        
        assertEquals(2, response.getOccurrences().size());
        assertEquals("HIGH_VALUE", response.getOccurrences().get(0).getType());
        assertEquals("EXTREME_VALUE", response.getOccurrences().get(1).getType());
    }

    @Test
    void testEmptyOccurrences() {
        response.setOrderId(orderId);
        response.setCustomerId(customerId);
        response.setClassification(CustomerRiskType.REGULAR);
        response.setOccurrences(new ArrayList<>());
        
        assertNotNull(response.getOccurrences());
        assertTrue(response.getOccurrences().isEmpty());
    }

    @Test
    void testAllCustomerRiskTypes() {
        for (CustomerRiskType riskType : CustomerRiskType.values()) {
            response.setClassification(riskType);
            assertEquals(riskType, response.getClassification());
        }
    }

    @Test
    void testRiskOccurrenceDTO() {
        FraudAnalysisResponseDTO.RiskOccurrenceDTO occurrence = new FraudAnalysisResponseDTO.RiskOccurrenceDTO();
        
        UUID occurrenceId = UUID.randomUUID();
        Long productId = 12345L;
        String type = "FRAUD_DETECTED";
        String description = "Suspicious activity detected";
        LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
        LocalDateTime updatedAt = LocalDateTime.now();
        
        occurrence.setId(occurrenceId);
        occurrence.setProductId(productId);
        occurrence.setType(type);
        occurrence.setDescription(description);
        occurrence.setCreatedAt(createdAt);
        occurrence.setUpdatedAt(updatedAt);
        
        assertEquals(occurrenceId, occurrence.getId());
        assertEquals(productId, occurrence.getProductId());
        assertEquals(type, occurrence.getType());
        assertEquals(description, occurrence.getDescription());
        assertEquals(createdAt, occurrence.getCreatedAt());
        assertEquals(updatedAt, occurrence.getUpdatedAt());
    }

    @Test
    void testRiskOccurrenceDTOEqualsAndHashCode() {
        UUID occurrenceId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        
        FraudAnalysisResponseDTO.RiskOccurrenceDTO occurrence1 = new FraudAnalysisResponseDTO.RiskOccurrenceDTO();
        occurrence1.setId(occurrenceId);
        occurrence1.setType("TEST");
        occurrence1.setCreatedAt(now);
        
        FraudAnalysisResponseDTO.RiskOccurrenceDTO occurrence2 = new FraudAnalysisResponseDTO.RiskOccurrenceDTO();
        occurrence2.setId(occurrenceId);
        occurrence2.setType("TEST");
        occurrence2.setCreatedAt(now);
        
        assertEquals(occurrence1, occurrence2);
        assertEquals(occurrence1.hashCode(), occurrence2.hashCode());
        
        occurrence2.setType("DIFFERENT");
        assertNotEquals(occurrence1, occurrence2);
    }

    @Test
    void testRiskOccurrenceDTOToString() {
        FraudAnalysisResponseDTO.RiskOccurrenceDTO occurrence = createOccurrence("TEST_TYPE", "Test description");
        
        String toString = occurrence.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("RiskOccurrenceDTO"));
        assertTrue(toString.contains("TEST_TYPE"));
        assertTrue(toString.contains("Test description"));
    }

    @Test
    void testComplexScenario() {

        LocalDateTime analyzedAt = LocalDateTime.now();
        
        FraudAnalysisResponseDTO.RiskOccurrenceDTO highValueOccurrence = createOccurrence(
            "HIGH_VALUE", 
            "Insurance amount R$ 400000.00 exceeds normal threshold for AUTO category"
        );
        
        FraudAnalysisResponseDTO.RiskOccurrenceDTO extremeValueOccurrence = createOccurrence(
            "EXTREME_VALUE", 
            "Insurance amount R$ 1500000.00 is extremely high and requires additional verification"
        );
        
        response.setOrderId(orderId);
        response.setCustomerId(customerId);
        response.setAnalyzedAt(analyzedAt);
        response.setClassification(CustomerRiskType.HIGH_RISK);
        response.setOccurrences(Arrays.asList(highValueOccurrence, extremeValueOccurrence));
        
        assertEquals(orderId, response.getOrderId());
        assertEquals(customerId, response.getCustomerId());
        assertEquals(CustomerRiskType.HIGH_RISK, response.getClassification());
        assertEquals(2, response.getOccurrences().size());
        
        assertTrue(response.getOccurrences().stream()
            .anyMatch(occ -> "HIGH_VALUE".equals(occ.getType())));
        assertTrue(response.getOccurrences().stream()
            .anyMatch(occ -> "EXTREME_VALUE".equals(occ.getType())));
    }

    private FraudAnalysisResponseDTO.RiskOccurrenceDTO createOccurrence(String type, String description) {
        FraudAnalysisResponseDTO.RiskOccurrenceDTO occurrence = new FraudAnalysisResponseDTO.RiskOccurrenceDTO();
        occurrence.setId(UUID.randomUUID());
        occurrence.setProductId(12345L);
        occurrence.setType(type);
        occurrence.setDescription(description);
        occurrence.setCreatedAt(LocalDateTime.now().minusMinutes(30));
        occurrence.setUpdatedAt(LocalDateTime.now());
        return occurrence;
    }
} 