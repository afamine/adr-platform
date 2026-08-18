package com.adrplatform.adr.service;

import com.adrplatform.adr.config.AiAssistProperties;
import com.adrplatform.adr.dto.GenerateAdrDraftRequest;
import com.adrplatform.adr.exception.AiDraftGenerationException;
import com.adrplatform.auth.domain.Role;
import com.adrplatform.auth.domain.User;
import com.adrplatform.auth.domain.Workspace;
import com.adrplatform.auth.service.AuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiDraftGeneratorServiceTest {

    private ChatClient.Builder chatClientBuilder;
    private AuditService auditService;
    private AiDraftRateLimiter rateLimiter;
    private AiDraftGeneratorService service;
    private User author;

    @BeforeEach
    void setUp() {
        chatClientBuilder = mock(ChatClient.Builder.class, Answers.RETURNS_DEEP_STUBS);
        auditService = mock(AuditService.class);
        rateLimiter = mock(AiDraftRateLimiter.class);
        service = new AiDraftGeneratorService(chatClientBuilder, new ObjectMapper(), auditService,
                new AiAssistProperties("Test provider", "privacy"), rateLimiter);

        Workspace workspace = Workspace.builder().id(UUID.randomUUID()).name("Test Workspace").slug("test").build();
        author = User.builder().id(UUID.randomUUID()).workspace(workspace).email("author@example.com")
                .fullName("Author").role(Role.AUTHOR).isActive(true).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(author, null, author.getAuthorities()));
        when(rateLimiter.tryConsume(author.getId())).thenReturn(true);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void generatesAndNormalizesAnUnsavedDraft() {
        when(chatClientBuilder.build().prompt().system(anyString()).user(anyString()).call().content()).thenReturn("""
                ```json
                {
                  "title":"Choose PostgreSQL for Orders",
                  "context":"The order service needs durable transactional storage.",
                  "decision":"Use PostgreSQL as the primary database.",
                  "consequences":"Transactions improve consistency but require relational modelling.",
                  "alternatives":"MongoDB was considered but rejected for weaker relational guarantees.",
                  "suggestedTags":["Database", "postgresql", "database", "invalid/tag"]
                }
                ```
                """);

        var response = service.generateDraft(new GenerateAdrDraftRequest(
                "The order service needs a reliable transactional database."));

        assertThat(response.title()).isEqualTo("Choose PostgreSQL for Orders");
        assertThat(response.suggestedTags()).containsExactly("database", "postgresql");
        verify(auditService).record(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void turnsAnInvalidProviderResponseIntoAControlledFailure() {
        when(chatClientBuilder.build().prompt().system(anyString()).user(anyString()).call().content())
                .thenReturn("This is not JSON");

        assertThatThrownBy(() -> service.generateDraft(new GenerateAdrDraftRequest(
                "The order service needs a reliable transactional database.")))
                .isInstanceOf(AiDraftGenerationException.class)
                .hasMessage("AI service temporarily unavailable. Please try again.");
    }
}
