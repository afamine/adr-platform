package com.adrplatform.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UpdateStatusResponse {
    private String message;
    private String userId;
    @Getter(onMethod_ = {@JsonProperty("isActive")})
    private boolean isActive;
}
