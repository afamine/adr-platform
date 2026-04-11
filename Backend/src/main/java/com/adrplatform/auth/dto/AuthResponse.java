package com.adrplatform.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {
    private String token;
    private String refreshToken;
    private UserDto user;
}
