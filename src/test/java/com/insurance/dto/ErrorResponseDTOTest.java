package com.insurance.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test class for ErrorResponseDTO
 */
class ErrorResponseDTOTest {

    private ObjectMapper objectMapper;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        now = LocalDateTime.now();
    }

    @Test
    void testErrorResponseDTOBuilder() {
        ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
                .timestamp(now)
                .status(400)
                .error("Bad Request")
                .message("Invalid input")
                .path("/api/test")
                .errorCode("INVALID_INPUT")
                .validationErrors(Arrays.asList("Field is required", "Invalid format"))
                .build();

        assertEquals(now, errorResponse.getTimestamp());
        assertEquals(400, errorResponse.getStatus());
        assertEquals("Bad Request", errorResponse.getError());
        assertEquals("Invalid input", errorResponse.getMessage());
        assertEquals("/api/test", errorResponse.getPath());
        assertEquals("INVALID_INPUT", errorResponse.getErrorCode());
        assertEquals(2, errorResponse.getValidationErrors().size());
        assertTrue(errorResponse.getValidationErrors().contains("Field is required"));
        assertTrue(errorResponse.getValidationErrors().contains("Invalid format"));
    }

    @Test
    void testBusinessErrorBuilder() {
        ErrorResponseDTO.ErrorResponseDTOBuilder builder = ErrorResponseDTO.businessError(
                "Policy has expired", "POLICY_EXPIRED");
        ErrorResponseDTO errorResponse = builder.build();

        assertNotNull(errorResponse.getTimestamp());
        assertTrue(errorResponse.getTimestamp().isBefore(LocalDateTime.now().plusSeconds(1)));
        assertTrue(errorResponse.getTimestamp().isAfter(LocalDateTime.now().minusSeconds(1)));
        assertEquals(422, errorResponse.getStatus());
        assertEquals("Business Rule Violation", errorResponse.getError());
        assertEquals("Policy has expired", errorResponse.getMessage());
        assertEquals("POLICY_EXPIRED", errorResponse.getErrorCode());
    }

    @Test
    void testBusinessErrorBuilderWithPath() {
        ErrorResponseDTO errorResponse = ErrorResponseDTO.businessError(
                "Customer not found", "CUSTOMER_NOT_FOUND")
                .path("/api/customers/123")
                .build();

        assertEquals(422, errorResponse.getStatus());
        assertEquals("Business Rule Violation", errorResponse.getError());
        assertEquals("Customer not found", errorResponse.getMessage());
        assertEquals("CUSTOMER_NOT_FOUND", errorResponse.getErrorCode());
        assertEquals("/api/customers/123", errorResponse.getPath());
        assertNotNull(errorResponse.getTimestamp());
    }

    @Test
    void testValidationErrorBuilder() {
        ErrorResponseDTO.ErrorResponseDTOBuilder builder = ErrorResponseDTO.validationError(
                "Field validation failed");
        ErrorResponseDTO errorResponse = builder.build();

        assertNotNull(errorResponse.getTimestamp());
        assertTrue(errorResponse.getTimestamp().isBefore(LocalDateTime.now().plusSeconds(1)));
        assertTrue(errorResponse.getTimestamp().isAfter(LocalDateTime.now().minusSeconds(1)));
        assertEquals(400, errorResponse.getStatus());
        assertEquals("Validation Failed", errorResponse.getError());
        assertEquals("Field validation failed", errorResponse.getMessage());
    }

    @Test
    void testValidationErrorBuilderWithErrors() {
        List<String> validationErrors = Arrays.asList(
                "Name is required",
                "Email format is invalid",
                "Age must be positive"
        );

        ErrorResponseDTO errorResponse = ErrorResponseDTO.validationError(
                "Multiple validation errors")
                .validationErrors(validationErrors)
                .path("/api/users")
                .build();

        assertEquals(400, errorResponse.getStatus());
        assertEquals("Validation Failed", errorResponse.getError());
        assertEquals("Multiple validation errors", errorResponse.getMessage());
        assertEquals("/api/users", errorResponse.getPath());
        assertEquals(3, errorResponse.getValidationErrors().size());
        assertTrue(errorResponse.getValidationErrors().contains("Name is required"));
        assertTrue(errorResponse.getValidationErrors().contains("Email format is invalid"));
        assertTrue(errorResponse.getValidationErrors().contains("Age must be positive"));
    }

    @Test
    void testNotFoundBuilder() {
        ErrorResponseDTO.ErrorResponseDTOBuilder builder = ErrorResponseDTO.notFound(
                "Policy not found");
        ErrorResponseDTO errorResponse = builder.build();

        assertNotNull(errorResponse.getTimestamp());
        assertTrue(errorResponse.getTimestamp().isBefore(LocalDateTime.now().plusSeconds(1)));
        assertTrue(errorResponse.getTimestamp().isAfter(LocalDateTime.now().minusSeconds(1)));
        assertEquals(404, errorResponse.getStatus());
        assertEquals("Resource Not Found", errorResponse.getError());
        assertEquals("Policy not found", errorResponse.getMessage());
    }

    @Test
    void testNotFoundBuilderWithPath() {
        ErrorResponseDTO errorResponse = ErrorResponseDTO.notFound(
                "Resource not available")
                .path("/api/policies/nonexistent")
                .errorCode("RESOURCE_NOT_FOUND")
                .build();

        assertEquals(404, errorResponse.getStatus());
        assertEquals("Resource Not Found", errorResponse.getError());
        assertEquals("Resource not available", errorResponse.getMessage());
        assertEquals("/api/policies/nonexistent", errorResponse.getPath());
        assertEquals("RESOURCE_NOT_FOUND", errorResponse.getErrorCode());
    }

    @Test
    void testErrorResponseWithNullFields() {
        ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
                .timestamp(now)
                .status(500)
                .error("Internal Server Error")
                .message("Unexpected error")
                .path(null)
                .errorCode(null)
                .validationErrors(null)
                .build();

        assertEquals(now, errorResponse.getTimestamp());
        assertEquals(500, errorResponse.getStatus());
        assertEquals("Internal Server Error", errorResponse.getError());
        assertEquals("Unexpected error", errorResponse.getMessage());
        assertNull(errorResponse.getPath());
        assertNull(errorResponse.getErrorCode());
        assertNull(errorResponse.getValidationErrors());
    }

    @Test
    void testErrorResponseWithEmptyValidationErrors() {
        ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
                .timestamp(now)
                .status(400)
                .error("Validation Error")
                .message("No specific errors")
                .validationErrors(Arrays.asList())
                .build();

        assertEquals(400, errorResponse.getStatus());
        assertNotNull(errorResponse.getValidationErrors());
        assertTrue(errorResponse.getValidationErrors().isEmpty());
    }

    @Test
    void testErrorResponseDifferentStatusCodes() {
        int[] statusCodes = {200, 201, 400, 401, 403, 404, 422, 500, 502, 503};

        for (int status : statusCodes) {
            ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
                    .timestamp(now)
                    .status(status)
                    .error("Test Error")
                    .message("Test message")
                    .build();

            assertEquals(status, errorResponse.getStatus());
            assertEquals("Test Error", errorResponse.getError());
            assertEquals("Test message", errorResponse.getMessage());
        }
    }

    @Test
    void testErrorResponseComplexValidationErrors() {
        List<String> complexErrors = Arrays.asList(
                "Field 'email' is required",
                "Field 'password' must be at least 8 characters",
                "Field 'age' must be between 18 and 100",
                "Field 'phone' format is invalid",
                "Field 'address.zipCode' is required"
        );

        ErrorResponseDTO errorResponse = ErrorResponseDTO.validationError(
                "Multiple field validation errors")
                .validationErrors(complexErrors)
                .path("/api/registration")
                .errorCode("VALIDATION_FAILED")
                .build();

        assertEquals(5, errorResponse.getValidationErrors().size());
        assertTrue(errorResponse.getValidationErrors().contains("Field 'email' is required"));
        assertTrue(errorResponse.getValidationErrors().contains("Field 'address.zipCode' is required"));
        assertEquals("VALIDATION_FAILED", errorResponse.getErrorCode());
    }

    @Test
    void testErrorResponseWithSpecialCharacters() {
        ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
                .timestamp(now)
                .status(400)
                .error("Error with special chars: !@#$%^&*()")
                .message("Message with UTF-8: áéíóú ñç 中文 العربية")
                .path("/api/test?param=value&other=123")
                .errorCode("SPECIAL_CHARS_ERROR")
                .validationErrors(Arrays.asList(
                        "Field contains invalid chars: <>?/",
                        "Unicode error: ñáéíóú"
                ))
                .build();

        assertTrue(errorResponse.getError().contains("!@#$%^&*()"));
        assertTrue(errorResponse.getMessage().contains("áéíóú"));
        assertTrue(errorResponse.getMessage().contains("中文"));
        assertTrue(errorResponse.getPath().contains("?param=value"));
        assertEquals("SPECIAL_CHARS_ERROR", errorResponse.getErrorCode());
    }

    @Test
    void testErrorResponseEqualsAndHashCode() {
        ErrorResponseDTO error1 = ErrorResponseDTO.builder()
                .timestamp(now)
                .status(404)
                .error("Not Found")
                .message("Resource not found")
                .path("/api/test")
                .errorCode("NOT_FOUND")
                .build();

        ErrorResponseDTO error2 = ErrorResponseDTO.builder()
                .timestamp(now)
                .status(404)
                .error("Not Found")
                .message("Resource not found")
                .path("/api/test")
                .errorCode("NOT_FOUND")
                .build();

        assertEquals(error1, error2);
        assertEquals(error1.hashCode(), error2.hashCode());
    }

    @Test
    void testErrorResponseToString() {
        ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
                .timestamp(now)
                .status(422)
                .error("Business Error")
                .message("Test message")
                .path("/api/test")
                .errorCode("TEST_ERROR")
                .build();

        String toString = errorResponse.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("ErrorResponseDTO"));
        assertTrue(toString.contains("422"));
        assertTrue(toString.contains("Business Error"));
        assertTrue(toString.contains("TEST_ERROR"));
    }

    @Test
    void testJsonIncludeNonNull() throws Exception {
        ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
                .timestamp(now)
                .status(400)
                .error("Test Error")
                .message("Test message")
                .path(null) // Should be excluded from JSON
                .errorCode(null) // Should be excluded from JSON
                .validationErrors(null) // Should be excluded from JSON
                .build();

        String json = objectMapper.writeValueAsString(errorResponse);
        
        assertFalse(json.contains("\"path\""));
        assertFalse(json.contains("\"errorCode\""));
        assertFalse(json.contains("\"validationErrors\""));
        assertTrue(json.contains("\"status\":400"));
        assertTrue(json.contains("\"error\":\"Test Error\""));
        assertTrue(json.contains("\"message\":\"Test message\""));
    }

    @Test
    void testJsonSerializationDeserialization() throws Exception {
        List<String> validationErrors = Arrays.asList("Error 1", "Error 2");
        
        ErrorResponseDTO originalError = ErrorResponseDTO.builder()
                .timestamp(LocalDateTime.of(2023, 12, 25, 14, 30, 45)) // Fixed timestamp for test
                .status(422)
                .error("Business Rule Violation")
                .message("Test business error")
                .path("/api/business")
                .errorCode("BUSINESS_ERROR")
                .validationErrors(validationErrors)
                .build();

        String json = objectMapper.writeValueAsString(originalError);
        ErrorResponseDTO deserializedError = objectMapper.readValue(json, ErrorResponseDTO.class);

        assertEquals(originalError.getStatus(), deserializedError.getStatus());
        assertEquals(originalError.getError(), deserializedError.getError());
        assertEquals(originalError.getMessage(), deserializedError.getMessage());
        assertEquals(originalError.getPath(), deserializedError.getPath());
        assertEquals(originalError.getErrorCode(), deserializedError.getErrorCode());
        assertEquals(originalError.getValidationErrors().size(), deserializedError.getValidationErrors().size());
        assertEquals("Error 1", deserializedError.getValidationErrors().get(0));
        assertEquals("Error 2", deserializedError.getValidationErrors().get(1));
    }

    @Test
    void testBuilderChaining() {
        ErrorResponseDTO errorResponse = ErrorResponseDTO.businessError("Test", "CODE")
                .path("/test")
                .validationErrors(Arrays.asList("Error"))
                .build();

        assertEquals(422, errorResponse.getStatus());
        assertEquals("Business Rule Violation", errorResponse.getError());
        assertEquals("Test", errorResponse.getMessage());
        assertEquals("CODE", errorResponse.getErrorCode());
        assertEquals("/test", errorResponse.getPath());
        assertEquals(1, errorResponse.getValidationErrors().size());
    }

    @Test
    void testAllBuilderFactoryMethods() {
        // Test businessError factory
        ErrorResponseDTO businessError = ErrorResponseDTO.businessError("Business message", "BIZ_CODE").build();
        assertEquals(422, businessError.getStatus());
        assertEquals("Business Rule Violation", businessError.getError());
        
        // Test validationError factory
        ErrorResponseDTO validationError = ErrorResponseDTO.validationError("Validation message").build();
        assertEquals(400, validationError.getStatus());
        assertEquals("Validation Failed", validationError.getError());
        
        // Test notFound factory
        ErrorResponseDTO notFoundError = ErrorResponseDTO.notFound("Not found message").build();
        assertEquals(404, notFoundError.getStatus());
        assertEquals("Resource Not Found", notFoundError.getError());
    }
} 