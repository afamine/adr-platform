package com.adrplatform.adr.dto;

import java.util.List;

/**
 * AI-generated ADR content. It is intentionally not persisted; the author
 * reviews and saves it through the existing ADR creation/update workflow.
 */
public record GenerateAdrDraftResponse(
        String title,
        String context,
        String decision,
        String consequences,
        String alternatives,
        List<String> suggestedTags
) {
}
