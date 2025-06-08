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
import com.insurance.mapper.PolicyRequestMapper;
import com.insurance.service.PolicyRequestService;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
                .setControllerAdvice()
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
    }

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
    }

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
    void testGetPolicyRequestsByCustomerList() throws Exception {
        List<PolicyRequest> policyRequests = Arrays.asList(policyRequest);
        when(service.findByCustomerId(customerId)).thenReturn(policyRequests);
        when(mapper.toDTO(any(PolicyRequest.class))).thenReturn(requestDTO);

        mockMvc.perform(get("/api/v1/policy/customer/{customerId}", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerId").value(customerId.toString()));

        verify(service).findByCustomerId(customerId);
    }

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
    void testProcessPaymentReturnOk() throws Exception {
        doNothing().when(service).processPayment(policyId);

        mockMvc.perform(post("/api/v1/policy/{id}/payment", policyId))
                .andExpect(status().isOk());

        verify(service).processPayment(policyId);
    }

    @Test
    void testProcessSubscriptionReturnOk() throws Exception {
        doNothing().when(service).processSubscription(policyId);

        mockMvc.perform(post("/api/v1/policy/{id}/subscription", policyId))
                .andExpect(status().isOk());

        verify(service).processSubscription(policyId);
    }

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
    void testCreatePolicyRequestReturnBadRequest() throws Exception {
        requestDTO.setCustomerId(null); // Tornando o DTO inválido

        mockMvc.perform(post("/api/v1/policy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest());

        verify(service, never()).createPolicyRequest(any(PolicyRequest.class));
    }
}