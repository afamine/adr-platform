package com.adrplatform.auth.dto;

import java.util.UUID;

public record UserSummaryDto(UUID id, String fullName, String email) {}
