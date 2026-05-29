package com.adrplatform.adr.dto;

import java.util.UUID;

public record AdrSummaryDto(UUID id, Integer adrNumber, String title, String status) {}
