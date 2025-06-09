package com.insurance.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponseDTO {
    
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private String errorCode;
    private List<String> validationErrors;
    
    public static ErrorResponseDTO.ErrorResponseDTOBuilder businessError(String message, String errorCode) {
        return ErrorResponseDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(422)
                .error("Business Rule Violation")
                .message(message)
                .errorCode(errorCode);
    }
    
    public static ErrorResponseDTO.ErrorResponseDTOBuilder validationError(String message) {
        return ErrorResponseDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(400)
                .error("Validation Failed")
                .message(message);
    }
    
    public static ErrorResponseDTO.ErrorResponseDTOBuilder notFound(String message) {
        return ErrorResponseDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(404)
                .error("Resource Not Found")
                .message(message);
    }
} 