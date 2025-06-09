package com.insurance.exception;

import com.insurance.dto.ErrorResponseDTO;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExceptionHandlerTest {

    @InjectMocks
    private ExceptionHandler exceptionHandler;

    @Mock
    private WebRequest webRequest;

    @BeforeEach
    void setUp() {
        when(webRequest.getDescription(false)).thenReturn("uri=/api/v1/policy");
    }

    // ========== BUSINESS EXCEPTIONS ==========

    @Test
    void testHandleBusinessException() {
        // Given
        BusinessException exception = new BusinessException("Policy has expired", "POLICY_EXPIRED");

        // When
        ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleBusinessException(exception, webRequest);

        // Then
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Business Rule Violation", response.getBody().getError());
        assertEquals("Policy has expired", response.getBody().getMessage());
        assertEquals("POLICY_EXPIRED", response.getBody().getErrorCode());
        assertEquals("/api/v1/policy", response.getBody().getPath());
        assertEquals(422, response.getBody().getStatus());
        assertNotNull(response.getBody().getTimestamp());
    }

    // ========== ENTITY EXCEPTIONS ==========

    @Test
    void testHandleEntityNotFound() {
        // Given
        EntityNotFoundException exception = new EntityNotFoundException("Policy not found");

        // When
        ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleEntityNotFound(exception, webRequest);

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Resource Not Found", response.getBody().getError());
        assertEquals("Policy not found", response.getBody().getMessage());
        assertEquals("/api/v1/policy", response.getBody().getPath());
        assertEquals(404, response.getBody().getStatus());
        assertNotNull(response.getBody().getTimestamp());
    }

    // ========== VALIDATION EXCEPTIONS ==========

    @Test
    void testHandleIllegalArgument() {
        // Given
        IllegalArgumentException exception = new IllegalArgumentException("Invalid customer ID");

        // When
        ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleIllegalArgument(exception, webRequest);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid Input", response.getBody().getError());
        assertEquals("Invalid customer ID", response.getBody().getMessage());
        assertEquals("/api/v1/policy", response.getBody().getPath());
        assertEquals(400, response.getBody().getStatus());
    }

    @Test
    void testHandleValidationErrors() {
        // Given
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError1 = new FieldError("policyRequest", "customerId", "must not be null");
        FieldError fieldError2 = new FieldError("policyRequest", "amount", "must be positive");

        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2));

        // When
        ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleValidationErrors(exception, webRequest);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Validation Failed", response.getBody().getError());
        assertEquals("Invalid request data", response.getBody().getMessage());
        assertNotNull(response.getBody().getValidationErrors());
        assertEquals(2, response.getBody().getValidationErrors().size());
        assertTrue(response.getBody().getValidationErrors().contains("customerId: must not be null"));
        assertTrue(response.getBody().getValidationErrors().contains("amount: must be positive"));
    }

    @Test
    void testHandleConstraintViolation() {
        // Given
        ConstraintViolationException exception = mock(ConstraintViolationException.class);
        ConstraintViolation<?> violation1 = mock(ConstraintViolation.class);
        ConstraintViolation<?> violation2 = mock(ConstraintViolation.class);
        Path path1 = mock(Path.class);
        Path path2 = mock(Path.class);

        when(violation1.getPropertyPath()).thenReturn(path1);
        when(violation1.getMessage()).thenReturn("must not be null");
        when(path1.toString()).thenReturn("customerId");

        when(violation2.getPropertyPath()).thenReturn(path2);
        when(violation2.getMessage()).thenReturn("must be positive");
        when(path2.toString()).thenReturn("amount");

        when(exception.getConstraintViolations()).thenReturn(Set.of(violation1, violation2));

        // When
        ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleConstraintViolation(exception, webRequest);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Constraint Violation", response.getBody().getError());
        assertEquals("Data validation failed", response.getBody().getMessage());
        assertNotNull(response.getBody().getValidationErrors());
        assertEquals(2, response.getBody().getValidationErrors().size());
    }

    // ========== HTTP EXCEPTIONS ==========

    @Test
    void testHandleHttpMessageNotReadable() {
        // Given
        HttpMessageNotReadableException exception = mock(HttpMessageNotReadableException.class);
        when(exception.getMessage()).thenReturn("Invalid JSON format");

        // When
        ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleHttpMessageNotReadable(exception, webRequest);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid JSON", response.getBody().getError());
        assertEquals("Request body contains invalid JSON format", response.getBody().getMessage());
        assertEquals("/api/v1/policy", response.getBody().getPath());
        assertEquals(400, response.getBody().getStatus());
    }

    @Test
    void testHandleTypeMismatch() {
        // Given
        MethodArgumentTypeMismatchException exception = mock(MethodArgumentTypeMismatchException.class);
        when(exception.getName()).thenReturn("policyId");
        when(exception.getRequiredType()).thenReturn((Class) Long.class);
        when(exception.getValue()).thenReturn("invalid-uuid");

        // When
        ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleTypeMismatch(exception, webRequest);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid Parameter Type", response.getBody().getError());
        assertEquals("Parameter 'policyId' should be of type Long", response.getBody().getMessage());
        assertEquals("/api/v1/policy", response.getBody().getPath());
        assertEquals(400, response.getBody().getStatus());
    }

    // ========== DATABASE EXCEPTIONS ==========

    @Test
    void testHandleDataIntegrityViolation_UniqueConstraint() {
        // Given
        DataIntegrityViolationException exception = new DataIntegrityViolationException("unique constraint violation");

        // When
        ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleDataIntegrityViolation(exception, webRequest);

        // Then
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Data Integrity Violation", response.getBody().getError());
        assertEquals("A record with this information already exists", response.getBody().getMessage());
        assertEquals("/api/v1/policy", response.getBody().getPath());
        assertEquals(409, response.getBody().getStatus());
    }

    @Test
    void testHandleDataIntegrityViolation_ForeignKey() {
        // Given
        DataIntegrityViolationException exception = new DataIntegrityViolationException("foreign key constraint");

        // When
        ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleDataIntegrityViolation(exception, webRequest);

        // Then
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Data Integrity Violation", response.getBody().getError());
        assertEquals("Referenced data does not exist or cannot be deleted due to dependencies", response.getBody().getMessage());
    }

    @Test
    void testHandleDataIntegrityViolation_NotNull() {
        // Given
        DataIntegrityViolationException exception = new DataIntegrityViolationException("not-null constraint");

        // When
        ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleDataIntegrityViolation(exception, webRequest);

        // Then
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Data Integrity Violation", response.getBody().getError());
        assertEquals("Required field cannot be empty", response.getBody().getMessage());
    }

    // ========== GENERIC EXCEPTION ==========

    @Test
    void testHandleGenericException() {
        // Given
        RuntimeException exception = new RuntimeException("Unexpected error");

        // When
        ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleGenericException(exception, webRequest);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Internal Server Error", response.getBody().getError());
        assertEquals("An unexpected error occurred. Please try again later.", response.getBody().getMessage());
        assertEquals("/api/v1/policy", response.getBody().getPath());
        assertEquals(500, response.getBody().getStatus());
    }
} 