package com.insurance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insurance.domain.PolicyRequest;
import com.insurance.domain.enums.InsuranceCategory;
import com.insurance.domain.enums.PaymentMethod;
import com.insurance.domain.enums.PolicyStatus;
import com.insurance.domain.enums.SalesChannel;
import com.insurance.dto.FraudAnalysisResponseDTO;
import com.insurance.dto.PolicyCancelResponseDTO;
import com.insurance.dto.PolicyRequestDTO;
import com.insurance.dto.PolicyValidationResponseDTO;
import com.insurance.exception.ExceptionHandler;
import com.insurance.mapper.PolicyRequestMapper;
import com.insurance.service.PolicyRequestService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PolicyRequestControllerTest {

    private MockMvc mockMvc;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private PolicyRequestService service;

    @Mock
    private PolicyRequestMapper mapper;

    @InjectMocks
    private PolicyRequestController controller;

    private PolicyRequestDTO requestDTO;
    private PolicyRequest policyRequest;
    private UUID policyId;
    private UUID customerId;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ExceptionHandler())
                .build();

        policyId = UUID.randomUUID();
        customerId = UUID.randomUUID();

        requestDTO = new PolicyRequestDTO();
        requestDTO.setCustomerId(customerId);
        requestDTO.setProductId(UUID.randomUUID());
        requestDTO.setCategory(InsuranceCategory.AUTO);
        requestDTO.setSalesChannel(SalesChannel.MOBILE);
        requestDTO.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        requestDTO.setTotalMonthlyPremiumAmount(new BigDecimal("150.00"));
        requestDTO.setInsuredAmount(new BigDecimal("50000.00"));
        requestDTO.setCoverages(new HashMap<>() {{
            put("Collision", new BigDecimal("30000.00"));
            put("Theft", new BigDecimal("20000.00"));
        }});
        requestDTO.setAssistances(Arrays.asList("Roadside Assistance", "Glass Protection"));

        policyRequest = new PolicyRequest();
        policyRequest.setId(policyId);
        policyRequest.setCustomerId(customerId);
        policyRequest.setStatus(PolicyStatus.RECEIVED);
        policyRequest.setCategory(InsuranceCategory.AUTO);
        policyRequest.setCreatedAt(LocalDateTime.now());
        policyRequest.setUpdatedAt(LocalDateTime.now());
    }

    // ========== CREATE POLICY TESTS ==========
    
    @Test
    void testCreatePolicyRequestCreatedStatus() throws Exception {
        when(mapper.toEntity(any(PolicyRequestDTO.class))).thenReturn(policyRequest);
        when(service.createPolicyRequest(any(PolicyRequest.class))).thenReturn(policyRequest);
        when(mapper.toDTO(any(PolicyRequest.class))).thenReturn(requestDTO);

        mockMvc.perform(post("/api/v1/policy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value(customerId.toString()))
                .andExpect(jsonPath("$.category").value(InsuranceCategory.AUTO.name()));

        verify(service).createPolicyRequest(any(PolicyRequest.class));
        verify(mapper).toEntity(any(PolicyRequestDTO.class));
        verify(mapper).toDTO(any(PolicyRequest.class));
    }

    @Test
    void testCreatePolicyRequestReturnBadRequest() throws Exception {
        requestDTO.setCustomerId(null); // Tornando o DTO inválido

        mockMvc.perform(post("/api/v1/policy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest());

        verify(service, never()).createPolicyRequest(any(PolicyRequest.class));
    }

    @Test
    void testCreatePolicyRequestWithInvalidJson() throws Exception {
        mockMvc.perform(post("/api/v1/policy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ invalid json }"))
                .andExpect(status().isBadRequest());

        verify(service, never()).createPolicyRequest(any(PolicyRequest.class));
    }

    @Test
    void testCreatePolicyRequestWithMissingRequiredFields() throws Exception {
        PolicyRequestDTO invalidDTO = new PolicyRequestDTO();
        // Todos os campos obrigatórios são null

        mockMvc.perform(post("/api/v1/policy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest());

        verify(service, never()).createPolicyRequest(any(PolicyRequest.class));
    }

    // ========== GET POLICY TESTS ==========

    @Test
    void testGetPolicyRequest() throws Exception {
        when(service.findById(policyId)).thenReturn(policyRequest);
        when(mapper.toDTO(any(PolicyRequest.class))).thenReturn(requestDTO);

        mockMvc.perform(get("/api/v1/policy/{id}", policyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(customerId.toString()));

        verify(service).findById(policyId);
    }

    @Test
    void testGetPolicyRequestNotFound() throws Exception {
        when(service.findById(policyId)).thenThrow(new EntityNotFoundException("Policy not found"));

        mockMvc.perform(get("/api/v1/policy/{id}", policyId))
                .andExpect(status().isNotFound());

        verify(service).findById(policyId);
        verify(mapper, never()).toDTO(any(PolicyRequest.class));
    }

    @Test
    void testGetPolicyRequestInternalError() throws Exception {
        when(service.findById(policyId)).thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/api/v1/policy/{id}", policyId))
                .andExpect(status().isInternalServerError());

        verify(service).findById(policyId);
    }

    @Test
    void testGetPolicyRequestWithInvalidUUID() throws Exception {
        mockMvc.perform(get("/api/v1/policy/{id}", "invalid-uuid"))
                .andExpect(status().isBadRequest());

        verify(service, never()).findById(any(UUID.class));
    }

    // ========== GET BY CUSTOMER TESTS ==========

    @Test
    void testGetPolicyRequestsByCustomerList() throws Exception {
        List<PolicyRequest> policyRequests = Arrays.asList(policyRequest);
        when(service.findByCustomerId(customerId)).thenReturn(policyRequests);
        when(mapper.toDTO(any(PolicyRequest.class))).thenReturn(requestDTO);

        mockMvc.perform(get("/api/v1/policy/customer/{customerId}", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerId").value(customerId.toString()))
                .andExpect(jsonPath("$.length()").value(1));

        verify(service).findByCustomerId(customerId);
    }

    @Test
    void testGetPolicyRequestsByCustomerEmptyList() throws Exception {
        when(service.findByCustomerId(customerId)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/policy/customer/{customerId}", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(service).findByCustomerId(customerId);
    }

    @Test
    void testGetPolicyRequestsByCustomerException() throws Exception {
        when(service.findByCustomerId(customerId)).thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/api/v1/policy/customer/{customerId}", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0)); // Returns empty list on error

        verify(service).findByCustomerId(customerId);
    }

    @Test
    void testGetPolicyRequestsByCustomerMultiplePolicies() throws Exception {
        PolicyRequest policy2 = new PolicyRequest();
        policy2.setId(UUID.randomUUID());
        policy2.setCustomerId(customerId);
        policy2.setStatus(PolicyStatus.VALIDATED);

        List<PolicyRequest> policyRequests = Arrays.asList(policyRequest, policy2);
        when(service.findByCustomerId(customerId)).thenReturn(policyRequests);
        when(mapper.toDTO(any(PolicyRequest.class))).thenReturn(requestDTO);

        mockMvc.perform(get("/api/v1/policy/customer/{customerId}", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        verify(service).findByCustomerId(customerId);
        verify(mapper, times(2)).toDTO(any(PolicyRequest.class));
    }

    // ========== VALIDATION TESTS ==========

    @Test
    void testValidateReturnOk() throws Exception {
        PolicyValidationResponseDTO validationResponse = PolicyValidationResponseDTO.success(policyId, PolicyStatus.VALIDATED);
        when(service.validatePolicyRequest(policyId)).thenReturn(validationResponse);

        mockMvc.perform(post("/api/v1/policy/{id}/validate", policyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validated").value(true))
                .andExpect(jsonPath("$.policyId").value(policyId.toString()))
                .andExpect(jsonPath("$.status").value(PolicyStatus.VALIDATED.name()))
                .andExpect(jsonPath("$.message").value("Apólice validada com sucesso"));

        verify(service).validatePolicyRequest(policyId);
    }

    @Test
    void testValidateReturnBadRequest() throws Exception {
        PolicyValidationResponseDTO validationResponse = PolicyValidationResponseDTO.failure(policyId, "Valor excede limite permitido");
        when(service.validatePolicyRequest(policyId)).thenReturn(validationResponse);

        mockMvc.perform(post("/api/v1/policy/{id}/validate", policyId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validated").value(false))
                .andExpect(jsonPath("$.policyId").value(policyId.toString()))
                .andExpect(jsonPath("$.status").value(PolicyStatus.REJECTED.name()))
                .andExpect(jsonPath("$.reason").value("Valor excede limite permitido"));

        verify(service).validatePolicyRequest(policyId);
    }

    @Test
    void testValidateReturnInternalServerError() throws Exception {
        PolicyValidationResponseDTO errorResponse = PolicyValidationResponseDTO.error(policyId, "Internal validation error");
        when(service.validatePolicyRequest(policyId)).thenReturn(errorResponse);

        mockMvc.perform(post("/api/v1/policy/{id}/validate", policyId))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.validated").value(false))
                .andExpect(jsonPath("$.status").doesNotExist());

        verify(service).validatePolicyRequest(policyId);
    }

    // ========== FRAUD ANALYSIS TESTS ==========

    @Test
    void testProcessFraudAnalysisReturnOk() throws Exception {
        FraudAnalysisResponseDTO fraudResponse = new FraudAnalysisResponseDTO();
        fraudResponse.setOrderId(policyId);
        fraudResponse.setCustomerId(customerId);
        when(service.processFraudAnalysis(policyId)).thenReturn(fraudResponse);

        mockMvc.perform(post("/api/v1/policy/{id}/fraud-analysis", policyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(policyId.toString()))
                .andExpect(jsonPath("$.customerId").value(customerId.toString()));

        verify(service).processFraudAnalysis(policyId);
    }

    @Test
    void testProcessFraudAnalysisNotFound() throws Exception {
        when(service.processFraudAnalysis(policyId)).thenThrow(new EntityNotFoundException("Policy not found"));

        mockMvc.perform(post("/api/v1/policy/{id}/fraud-analysis", policyId))
                .andExpect(status().isNotFound());

        verify(service).processFraudAnalysis(policyId);
    }

    @Test
    void testProcessFraudAnalysisInternalError() throws Exception {
        when(service.processFraudAnalysis(policyId)).thenThrow(new RuntimeException("Fraud service unavailable"));

        mockMvc.perform(post("/api/v1/policy/{id}/fraud-analysis", policyId))
                .andExpect(status().isInternalServerError());

        verify(service).processFraudAnalysis(policyId);
    }

    // ========== PAYMENT TESTS ==========

    @Test
    void testProcessPaymentReturnOk() throws Exception {
        doNothing().when(service).processPayment(policyId);

        mockMvc.perform(post("/api/v1/policy/{id}/payment", policyId))
                .andExpect(status().isNoContent());

        verify(service).processPayment(policyId);
    }

    @Test
    void testProcessPaymentBadRequest() throws Exception {
        doThrow(new IllegalStateException("Policy not validated")).when(service).processPayment(policyId);

        mockMvc.perform(post("/api/v1/policy/{id}/payment", policyId))
                .andExpect(status().isBadRequest());

        verify(service).processPayment(policyId);
    }

    @Test
    void testProcessPaymentNotFound() throws Exception {
        doThrow(new EntityNotFoundException("Policy not found")).when(service).processPayment(policyId);

        mockMvc.perform(post("/api/v1/policy/{id}/payment", policyId))
                .andExpect(status().isNotFound());

        verify(service).processPayment(policyId);
    }

    @Test
    void testProcessPaymentInternalError() throws Exception {
        doThrow(new RuntimeException("Payment gateway error")).when(service).processPayment(policyId);

        mockMvc.perform(post("/api/v1/policy/{id}/payment", policyId))
                .andExpect(status().isInternalServerError());

        verify(service).processPayment(policyId);
    }

    // ========== SUBSCRIPTION TESTS ==========

    @Test
    void testProcessSubscriptionReturnOk() throws Exception {
        doNothing().when(service).processSubscription(policyId);

        mockMvc.perform(post("/api/v1/policy/{id}/subscription", policyId))
                .andExpect(status().isNoContent());

        verify(service).processSubscription(policyId);
    }

    @Test
    void testProcessSubscriptionNotFound() throws Exception {
        doThrow(new EntityNotFoundException("Policy not found")).when(service).processSubscription(policyId);

        mockMvc.perform(post("/api/v1/policy/{id}/subscription", policyId))
                .andExpect(status().isNotFound());

        verify(service).processSubscription(policyId);
    }

    @Test
    void testProcessSubscriptionInternalError() throws Exception {
        doThrow(new RuntimeException("Subscription service error")).when(service).processSubscription(policyId);

        mockMvc.perform(post("/api/v1/policy/{id}/subscription", policyId))
                .andExpect(status().isInternalServerError());

        verify(service).processSubscription(policyId);
    }

    // ========== CANCELLATION TESTS ==========

    @Test
    void testCancelPolicyRequestReturnOk() throws Exception {
        PolicyCancelResponseDTO cancelResponse = PolicyCancelResponseDTO.success(policyId);
        when(service.cancelPolicyRequest(policyId)).thenReturn(cancelResponse);

        mockMvc.perform(post("/api/v1/policy/{id}/cancel", policyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cancelled").value(true))
                .andExpect(jsonPath("$.policyId").value(policyId.toString()))
                .andExpect(jsonPath("$.status").value(PolicyStatus.CANCELLED.name()))
                .andExpect(jsonPath("$.message").value("Apólice cancelada com sucesso"));

        verify(service).cancelPolicyRequest(policyId);
    }

    @Test
    void testCancelPolicyRequestBadRequest() throws Exception {
        PolicyCancelResponseDTO cancelResponse = PolicyCancelResponseDTO.failure(policyId, "Cannot cancel approved policy");
        when(service.cancelPolicyRequest(policyId)).thenReturn(cancelResponse);

        mockMvc.perform(post("/api/v1/policy/{id}/cancel", policyId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.cancelled").value(false))
                .andExpect(jsonPath("$.policyId").value(policyId.toString()))
                .andExpect(jsonPath("$.reason").value("Cannot cancel approved policy"));

        verify(service).cancelPolicyRequest(policyId);
    }

    // ========== EDGE CASE TESTS ==========

    @Test
    void testCreatePolicyWithNegativePremium() throws Exception {
        requestDTO.setTotalMonthlyPremiumAmount(new BigDecimal("-100.00"));

        mockMvc.perform(post("/api/v1/policy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest());

        verify(service, never()).createPolicyRequest(any(PolicyRequest.class));
    }

    @Test
    void testCreatePolicyWithNegativeInsuredAmount() throws Exception {
        requestDTO.setInsuredAmount(new BigDecimal("-50000.00"));

        mockMvc.perform(post("/api/v1/policy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest());

        verify(service, never()).createPolicyRequest(any(PolicyRequest.class));
    }

    @Test
    void testCreatePolicyWithEmptyCoverages() throws Exception {
        requestDTO.setCoverages(null);

        mockMvc.perform(post("/api/v1/policy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest());

        verify(service, never()).createPolicyRequest(any(PolicyRequest.class));
    }

    @Test
    void testAllEndpointsWithNullUUIDs() throws Exception {
        // Test all endpoints with null/invalid UUIDs - ajustando expectativas para comportamento real
        String nullUuid = "00000000-0000-0000-0000-000000000000";
        
        // Mock service calls for null UUID scenarios
        when(service.findById(UUID.fromString(nullUuid))).thenThrow(new EntityNotFoundException("Policy not found"));
        when(service.processFraudAnalysis(UUID.fromString(nullUuid))).thenThrow(new EntityNotFoundException("Policy not found"));
        doThrow(new EntityNotFoundException("Policy not found")).when(service).processPayment(UUID.fromString(nullUuid));
        doThrow(new EntityNotFoundException("Policy not found")).when(service).processSubscription(UUID.fromString(nullUuid));
        
        // Mock validation and cancel methods to return appropriate responses
        when(service.validatePolicyRequest(UUID.fromString(nullUuid)))
            .thenReturn(PolicyValidationResponseDTO.error(UUID.fromString(nullUuid), "Policy not found"));
        when(service.cancelPolicyRequest(UUID.fromString(nullUuid)))
            .thenReturn(PolicyCancelResponseDTO.error(UUID.fromString(nullUuid), "Policy not found"));
        
        mockMvc.perform(get("/api/v1/policy/{id}", nullUuid))
                .andExpect(status().isNotFound());
        
        mockMvc.perform(post("/api/v1/policy/{id}/validate", nullUuid))
                .andExpect(status().isInternalServerError()); // Returns error response
        
        mockMvc.perform(post("/api/v1/policy/{id}/fraud-analysis", nullUuid))
                .andExpect(status().isNotFound());
        
        mockMvc.perform(post("/api/v1/policy/{id}/payment", nullUuid))
                .andExpect(status().isNotFound());
        
        mockMvc.perform(post("/api/v1/policy/{id}/subscription", nullUuid))
                .andExpect(status().isNotFound());
        
        mockMvc.perform(post("/api/v1/policy/{id}/cancel", nullUuid))
                .andExpect(status().isBadRequest()); // Returns failure response
    }

    @Test
    void testCreatePolicyWithDifferentInsuranceCategories() throws Exception {
        for (InsuranceCategory category : InsuranceCategory.values()) {
            requestDTO.setCategory(category);
            
            when(mapper.toEntity(any(PolicyRequestDTO.class))).thenReturn(policyRequest);
            when(service.createPolicyRequest(any(PolicyRequest.class))).thenReturn(policyRequest);
            when(mapper.toDTO(any(PolicyRequest.class))).thenReturn(requestDTO);

            mockMvc.perform(post("/api/v1/policy")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.category").value(category.name()));
        }
    }
}