package com.insurance.event;

import com.insurance.domain.PolicyRequest;
import com.insurance.domain.enums.PolicyStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PolicyRequestCreatedEventTest {

    private PolicyRequest mockPolicyRequest;
    private UUID policyRequestId;
    private UUID customerId;
    private PolicyRequestCreatedEvent event;

    @BeforeEach
    void setUp() {
        policyRequestId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        
        mockPolicyRequest = mock(PolicyRequest.class);
        when(mockPolicyRequest.getId()).thenReturn(policyRequestId);
        when(mockPolicyRequest.getCustomerId()).thenReturn(customerId);
        
        event = new PolicyRequestCreatedEvent(mockPolicyRequest);
    }

    @Test
    void testEventCreation() {
        assertEquals(policyRequestId, event.getPolicyRequestId());
        assertEquals(customerId, event.getCustomerId());
        assertEquals(PolicyStatus.RECEIVED, event.getStatus());
        assertNotNull(event.getTimestamp());
        assertTrue(event.getTimestamp().isBefore(LocalDateTime.now().plusSeconds(1)));
        assertTrue(event.getTimestamp().isAfter(LocalDateTime.now().minusSeconds(1)));
    }

    @Test
    void testEventType() {
        assertEquals("PolicyRequestCreatedEvent", event.getEventType());
    }

    @Test
    void testInheritedFields() {

        assertEquals(policyRequestId, event.getPolicyRequestId());
        assertEquals(customerId, event.getCustomerId());
        assertEquals(PolicyStatus.RECEIVED, event.getStatus());
        assertNotNull(event.getTimestamp());
    }
} 