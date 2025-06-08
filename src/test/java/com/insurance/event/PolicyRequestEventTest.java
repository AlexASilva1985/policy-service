package com.insurance.event;

import com.insurance.domain.enums.PolicyStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PolicyRequestEventTest {

    private UUID policyRequestId;
    private UUID customerId;
    private PolicyStatus status;
    private PolicyRequestEvent event;

    private static class TestPolicyRequestEvent extends PolicyRequestEvent {
        public TestPolicyRequestEvent(UUID policyRequestId, UUID customerId, PolicyStatus status) {
            super(policyRequestId, customerId, status);
        }
    }

    @BeforeEach
    void setUp() {
        policyRequestId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        status = PolicyStatus.RECEIVED;
        event = new TestPolicyRequestEvent(policyRequestId, customerId, status);
    }

    @Test
    void testEventCreation() {
        assertEquals(policyRequestId, event.getPolicyRequestId());
        assertEquals(customerId, event.getCustomerId());
        assertEquals(status, event.getStatus());
    }

    @Test
    void testTimestampGeneration() {
        LocalDateTime timestamp = event.getTimestamp();
        assertNotNull(timestamp);
        assertTrue(timestamp.isBefore(LocalDateTime.now().plusSeconds(1)));
        assertTrue(timestamp.isAfter(LocalDateTime.now().minusSeconds(1)));
    }

    @Test
    void testEventType() {
        assertEquals("TestPolicyRequestEvent", event.getEventType());
    }

    @Test
    void testEventWithDifferentStatus() {
        PolicyRequestEvent differentEvent = new TestPolicyRequestEvent(
            policyRequestId, 
            customerId,
                PolicyStatus.VALIDATED
        );

        assertEquals(PolicyStatus.VALIDATED, differentEvent.getStatus());
        assertEquals(policyRequestId, differentEvent.getPolicyRequestId());
        assertEquals(customerId, differentEvent.getCustomerId());
    }

    @Test
    void testEventWithDifferentIds() {
        UUID differentPolicyId = UUID.randomUUID();
        UUID differentCustomerId = UUID.randomUUID();

        PolicyRequestEvent differentEvent = new TestPolicyRequestEvent(
            differentPolicyId,
            differentCustomerId,
            status
        );

        assertEquals(differentPolicyId, differentEvent.getPolicyRequestId());
        assertEquals(differentCustomerId, differentEvent.getCustomerId());
        assertEquals(status, differentEvent.getStatus());
    }

    @Test
    void testRequiredFields() {
        assertNotNull(event.getPolicyRequestId(), "PolicyRequestId should not be null");
        assertNotNull(event.getCustomerId(), "CustomerId should not be null");
        assertNotNull(event.getStatus(), "Status should not be null");
        assertNotNull(event.getTimestamp(), "Timestamp should not be null");
        assertNotNull(event.getEventType(), "EventType should not be null");
    }
} 