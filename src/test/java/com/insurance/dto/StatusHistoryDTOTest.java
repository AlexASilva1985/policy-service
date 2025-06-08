package com.insurance.dto;

import com.insurance.domain.enums.PolicyStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class StatusHistoryDTOTest {

    private StatusHistoryDTO statusHistory;

    @BeforeEach
    void setUp() {
        statusHistory = new StatusHistoryDTO();
    }

    @Test
    void testGettersAndSetters() {
        PolicyStatus status = PolicyStatus.VALIDATED;
        LocalDateTime timestamp = LocalDateTime.now();
        
        statusHistory.setStatus(status);
        statusHistory.setTimestamp(timestamp);
        
        assertEquals(status, statusHistory.getStatus());
        assertEquals(timestamp, statusHistory.getTimestamp());
    }

    @Test
    void testNoArgsConstructor() {
        StatusHistoryDTO history = new StatusHistoryDTO();
        
        assertNull(history.getStatus());
        assertNull(history.getTimestamp());
    }

    @Test
    void testEqualsAndHashCode() {
        LocalDateTime timestamp = LocalDateTime.now();
        
        StatusHistoryDTO history1 = new StatusHistoryDTO();
        history1.setStatus(PolicyStatus.RECEIVED);
        history1.setTimestamp(timestamp);
        
        StatusHistoryDTO history2 = new StatusHistoryDTO();
        history2.setStatus(PolicyStatus.RECEIVED);
        history2.setTimestamp(timestamp);
        
        assertEquals(history1, history2);
        assertEquals(history1.hashCode(), history2.hashCode());
        
        history2.setStatus(PolicyStatus.REJECTED);
        assertNotEquals(history1, history2);
    }

    @Test
    void testToString() {
        statusHistory.setStatus(PolicyStatus.VALIDATED);
        statusHistory.setTimestamp(LocalDateTime.now());
        
        String toString = statusHistory.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("StatusHistoryDTO"));
        assertTrue(toString.contains("VALIDATED"));
    }

    @Test
    void testAllStatuses() {

        for (PolicyStatus status : PolicyStatus.values()) {
            statusHistory.setStatus(status);
            assertEquals(status, statusHistory.getStatus());
        }
    }

    @Test
    void testTimestampHandling() {
        LocalDateTime past = LocalDateTime.now().minusDays(1);
        LocalDateTime future = LocalDateTime.now().plusDays(1);
        LocalDateTime now = LocalDateTime.now();
        
        statusHistory.setTimestamp(past);
        assertEquals(past, statusHistory.getTimestamp());
        
        statusHistory.setTimestamp(future);
        assertEquals(future, statusHistory.getTimestamp());
        
        statusHistory.setTimestamp(now);
        assertEquals(now, statusHistory.getTimestamp());
        
        statusHistory.setTimestamp(null);
        assertNull(statusHistory.getTimestamp());
    }

    @Test
    void testNullValues() {
        StatusHistoryDTO history = new StatusHistoryDTO();
        
        history.setStatus(null);
        history.setTimestamp(null);
        
        assertNull(history.getStatus());
        assertNull(history.getTimestamp());
    }

    @Test
    void testStatusTransitionScenarios() {
        LocalDateTime baseTime = LocalDateTime.now().minusMinutes(60);
        
        StatusHistoryDTO received = new StatusHistoryDTO();
        received.setStatus(PolicyStatus.RECEIVED);
        received.setTimestamp(baseTime);
        
        assertEquals(PolicyStatus.RECEIVED, received.getStatus());
        assertEquals(baseTime, received.getTimestamp());
        
        StatusHistoryDTO validated = new StatusHistoryDTO();
        validated.setStatus(PolicyStatus.VALIDATED);
        validated.setTimestamp(baseTime.plusMinutes(10));
        
        assertEquals(PolicyStatus.VALIDATED, validated.getStatus());
        assertTrue(validated.getTimestamp().isAfter(received.getTimestamp()));
        
        StatusHistoryDTO approved = new StatusHistoryDTO();
        approved.setStatus(PolicyStatus.APPROVED);
        approved.setTimestamp(baseTime.plusMinutes(20));
        
        assertEquals(PolicyStatus.APPROVED, approved.getStatus());
        assertTrue(approved.getTimestamp().isAfter(validated.getTimestamp()));
        
        StatusHistoryDTO rejected = new StatusHistoryDTO();
        rejected.setStatus(PolicyStatus.REJECTED);
        rejected.setTimestamp(baseTime.plusMinutes(15));
        
        assertEquals(PolicyStatus.REJECTED, rejected.getStatus());
        assertTrue(rejected.getTimestamp().isAfter(validated.getTimestamp()));
        
        StatusHistoryDTO cancelled = new StatusHistoryDTO();
        cancelled.setStatus(PolicyStatus.CANCELLED);
        cancelled.setTimestamp(baseTime.plusMinutes(30));
        
        assertEquals(PolicyStatus.CANCELLED, cancelled.getStatus());
        assertTrue(cancelled.getTimestamp().isAfter(approved.getTimestamp()));
    }

    @Test
    void testTimestampPrecision() {
        LocalDateTime preciseTime = LocalDateTime.of(2024, 1, 15, 14, 30, 45, 123456789);
        
        statusHistory.setStatus(PolicyStatus.PENDING);
        statusHistory.setTimestamp(preciseTime);
        
        assertEquals(PolicyStatus.PENDING, statusHistory.getStatus());
        assertEquals(preciseTime, statusHistory.getTimestamp());
        assertEquals(2024, statusHistory.getTimestamp().getYear());
        assertEquals(1, statusHistory.getTimestamp().getMonthValue());
        assertEquals(15, statusHistory.getTimestamp().getDayOfMonth());
        assertEquals(14, statusHistory.getTimestamp().getHour());
        assertEquals(30, statusHistory.getTimestamp().getMinute());
        assertEquals(45, statusHistory.getTimestamp().getSecond());
        assertEquals(123456789, statusHistory.getTimestamp().getNano());
    }

    @Test
    void testMultipleStatusChanges() {

        statusHistory.setStatus(PolicyStatus.RECEIVED);
        statusHistory.setTimestamp(LocalDateTime.now().minusHours(2));
        assertEquals(PolicyStatus.RECEIVED, statusHistory.getStatus());
        
        statusHistory.setStatus(PolicyStatus.VALIDATED);
        statusHistory.setTimestamp(LocalDateTime.now().minusHours(1));
        assertEquals(PolicyStatus.VALIDATED, statusHistory.getStatus());
        
        statusHistory.setStatus(PolicyStatus.PENDING);
        statusHistory.setTimestamp(LocalDateTime.now().minusMinutes(30));
        assertEquals(PolicyStatus.PENDING, statusHistory.getStatus());
        
        statusHistory.setStatus(PolicyStatus.APPROVED);
        statusHistory.setTimestamp(LocalDateTime.now());
        assertEquals(PolicyStatus.APPROVED, statusHistory.getStatus());
    }
} 