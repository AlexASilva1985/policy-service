package com.insurance.dto;

import com.insurance.domain.enums.PolicyStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StatusHistoryDTOTest {

    private StatusHistoryDTO statusHistoryDTO;
    private LocalDateTime timestamp;

    @BeforeEach
    void setUp() {
        statusHistoryDTO = new StatusHistoryDTO();
        timestamp = LocalDateTime.now();
    }

    @Test
    void testGettersAndSetters() {
        statusHistoryDTO.setStatus(PolicyStatus.RECEIVED);
        statusHistoryDTO.setTimestamp(timestamp);

        assertEquals(PolicyStatus.RECEIVED, statusHistoryDTO.getStatus());
        assertEquals(timestamp, statusHistoryDTO.getTimestamp());
    }

    @Test
    void testAllStatuses() {
        PolicyStatus[] statuses = {
            PolicyStatus.RECEIVED,
            PolicyStatus.VALIDATED,
            PolicyStatus.PENDING,
            PolicyStatus.APPROVED,
            PolicyStatus.REJECTED,
            PolicyStatus.CANCELLED
        };

        for (PolicyStatus status : statuses) {
            statusHistoryDTO.setStatus(status);
            assertEquals(status, statusHistoryDTO.getStatus());
        }
    }

    @Test
    void testTimestampHandling() {
        LocalDateTime past = LocalDateTime.now().minusDays(1);
        LocalDateTime future = LocalDateTime.now().plusDays(1);

        statusHistoryDTO.setTimestamp(past);
        assertEquals(past, statusHistoryDTO.getTimestamp());

        statusHistoryDTO.setTimestamp(future);
        assertEquals(future, statusHistoryDTO.getTimestamp());

        statusHistoryDTO.setTimestamp(null);
        assertEquals(null, statusHistoryDTO.getTimestamp());
    }

    @Test
    void testEqualsAndHashCode() {
        StatusHistoryDTO dto1 = new StatusHistoryDTO();
        StatusHistoryDTO dto2 = new StatusHistoryDTO();

        dto1.setStatus(PolicyStatus.VALIDATED);
        dto1.setTimestamp(timestamp);

        dto2.setStatus(PolicyStatus.VALIDATED);
        dto2.setTimestamp(timestamp);

        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());

        dto2.setStatus(PolicyStatus.APPROVED);
        assertNotEquals(dto1, dto2);
    }

    @Test
    void testToString() {
        statusHistoryDTO.setStatus(PolicyStatus.RECEIVED);
        statusHistoryDTO.setTimestamp(timestamp);

        String toString = statusHistoryDTO.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("StatusHistoryDTO"));
    }

    @Test
    void testNullValues() {
        StatusHistoryDTO dto = new StatusHistoryDTO();
        dto.setStatus(null);
        dto.setTimestamp(null);

        assertEquals(null, dto.getStatus());
        assertEquals(null, dto.getTimestamp());
    }

    @Test
    void testDefaultConstructor() {
        StatusHistoryDTO dto = new StatusHistoryDTO();
        assertEquals(null, dto.getStatus());
        assertEquals(null, dto.getTimestamp());
    }
} 