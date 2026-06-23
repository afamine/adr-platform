package com.adrplatform.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

/**
 * Structured validation error response with field-level details and error codes.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ValidationErrorResponse {
    private Instant timestamp;
    private int status;
    private String message;
    private String path;
    private List<FieldError> errors;
}

