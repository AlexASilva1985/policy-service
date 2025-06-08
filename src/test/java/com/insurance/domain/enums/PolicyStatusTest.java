package com.insurance.domain.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PolicyStatusTest {

    @Test
    void testAllStatusValuesExist() {
        PolicyStatus[] statuses = PolicyStatus.values();
        assertEquals(6, statuses.length);
    }

    @Test
    void testSpecificStatusValues() {
        assertEquals("RECEIVED", PolicyStatus.RECEIVED.name());
        assertEquals("VALIDATED", PolicyStatus.VALIDATED.name());
        assertEquals("PENDING", PolicyStatus.PENDING.name());
        assertEquals("APPROVED", PolicyStatus.APPROVED.name());
        assertEquals("REJECTED", PolicyStatus.REJECTED.name());
        assertEquals("CANCELLED", PolicyStatus.CANCELLED.name());
    }

    @Test
    void testValueOf() {
        assertEquals(PolicyStatus.RECEIVED, PolicyStatus.valueOf("RECEIVED"));
        assertEquals(PolicyStatus.VALIDATED, PolicyStatus.valueOf("VALIDATED"));
        assertEquals(PolicyStatus.PENDING, PolicyStatus.valueOf("PENDING"));
        assertEquals(PolicyStatus.APPROVED, PolicyStatus.valueOf("APPROVED"));
        assertEquals(PolicyStatus.REJECTED, PolicyStatus.valueOf("REJECTED"));
        assertEquals(PolicyStatus.CANCELLED, PolicyStatus.valueOf("CANCELLED"));
    }

    @Test
    void testEnumOrder() {
        PolicyStatus[] statuses = PolicyStatus.values();
        assertEquals(PolicyStatus.RECEIVED, statuses[0]);
        assertEquals(PolicyStatus.VALIDATED, statuses[1]);
        assertEquals(PolicyStatus.PENDING, statuses[2]);
        assertEquals(PolicyStatus.APPROVED, statuses[3]);
        assertEquals(PolicyStatus.REJECTED, statuses[4]);
        assertEquals(PolicyStatus.CANCELLED, statuses[5]);
    }

    @Test
    void testToString() {
        for (PolicyStatus status : PolicyStatus.values()) {
            assertNotNull(status.toString());
            assertTrue(status.toString().length() > 0);
        }
    }

    @Test
    void testEquality() {
        assertEquals(PolicyStatus.RECEIVED, PolicyStatus.RECEIVED);
        assertEquals(PolicyStatus.APPROVED, PolicyStatus.APPROVED);
    }

    @Test
    void testHashCode() {
        for (PolicyStatus status : PolicyStatus.values()) {
            assertNotNull(status.hashCode());
        }
    }

    @Test
    void testOrdinalValues() {
        assertEquals(0, PolicyStatus.RECEIVED.ordinal());
        assertEquals(1, PolicyStatus.VALIDATED.ordinal());
        assertEquals(2, PolicyStatus.PENDING.ordinal());
        assertEquals(3, PolicyStatus.APPROVED.ordinal());
        assertEquals(4, PolicyStatus.REJECTED.ordinal());
        assertEquals(5, PolicyStatus.CANCELLED.ordinal());
    }
} 