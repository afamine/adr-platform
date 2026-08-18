package com.adrplatform.auth.controller;

import com.adrplatform.auth.dto.AuthResponse;
import com.adrplatform.auth.service.AuthService;
import com.adrplatform.auth.service.PasswordResetService;
import com.adrplatform.adr.dto.GenerateAdrDraftResponse;
import com.adrplatform.adr.service.AiDraftGeneratorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private PasswordResetService passwordResetService;

    @MockBean
    private AiDraftGeneratorService aiDraftGeneratorService;

    @Test
    void loginShouldBePublic() throws Exception {
        when(authService.login(any())).thenReturn(AuthResponse.builder().token("x").refreshToken("y").build());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@b.com\",\"password\":\"Pass1234\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void logoutShouldRequireAuthentication() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer fake"))
                .andExpect(status().isForbidden());
    }

    @Test
    void aiDraftGenerationShouldRequireAuthentication() throws Exception {
        mockMvc.perform(post("/api/adrs/generate-draft")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"problemDescription\":\"We need a reliable transactional database for orders.\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "REVIEWER")
    void aiDraftGenerationShouldDenyReviewers() throws Exception {
        mockMvc.perform(post("/api/adrs/generate-draft")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"problemDescription\":\"We need a reliable transactional database for orders.\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "AUTHOR")
    void aiDraftGenerationShouldAllowAuthors() throws Exception {
        when(aiDraftGeneratorService.generateDraft(any())).thenReturn(new GenerateAdrDraftResponse(
                "Use PostgreSQL for orders", "Order data needs reliable transactions.",
                "Use PostgreSQL.", "Operations need schema migrations.",
                "MongoDB was considered and rejected.", List.of("database", "postgresql")
        ));

        mockMvc.perform(post("/api/adrs/generate-draft")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"problemDescription\":\"We need a reliable transactional database for orders.\"}"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.title")
                        .value("Use PostgreSQL for orders"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.suggestedTags[0]")
                        .value("database"));
    }

    @Test
    @WithMockUser(roles = "AUTHOR")
    void aiDraftGenerationShouldValidateTheProblemDescription() throws Exception {
        mockMvc.perform(post("/api/adrs/generate-draft")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"problemDescription\":\"too short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.errors[0].field")
                        .value("problemDescription"));

        org.mockito.Mockito.verifyNoInteractions(aiDraftGeneratorService);
    }
}
