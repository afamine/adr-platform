package com.adrplatform.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Detailed field validation error with code and rejected value.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FieldError {
    private String field;
    private String message;
    private String code; // e.g. INVALID_EMAIL, WEAK_PASSWORD, TOO_SHORT, TOO_LONG, NOT_BLANK
    private Object rejectedValue;
}

