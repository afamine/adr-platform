package com.adrplatform.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UpdateStatusResponse {
    private String message;
    private boolean isActive;
}
