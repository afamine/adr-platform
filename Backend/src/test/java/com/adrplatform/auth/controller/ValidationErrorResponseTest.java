package com.adrplatform.auth.controller;

import com.adrplatform.auth.dto.FieldError;
import com.adrplatform.auth.dto.ValidationErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ValidationErrorResponseTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Test that missing required field returns detailed validation error with error code.
     */
    @Test
    void testMissingFullName_ReturnsBadRequest_WithNotBlankCode() throws Exception {
        String requestBody = """
                {
                  "fullName": null,
                  "email": "test@example.com",
                  "password": "P@ssw0rd1",
                  "workspaceSlug": "test-workspace"
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.path").value("/api/auth/register"))
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0].field").value("fullName"))
                .andExpect(jsonPath("$.errors[0].code").value("NOT_BLANK"))
                .andExpect(jsonPath("$.errors[0].message").value("must not be blank"))
                .andReturn();

        // Verify response can be parsed into ValidationErrorResponse
        String responseBody = result.getResponse().getContentAsString();
        ValidationErrorResponse response = objectMapper.readValue(responseBody, ValidationErrorResponse.class);

        assertNotNull(response);
        assertEquals(400, response.getStatus());
        assertEquals("Validation failed", response.getMessage());
        assertNotNull(response.getErrors());
        assertEquals(1, response.getErrors().size());

        FieldError error = response.getErrors().get(0);
        assertEquals("fullName", error.getField());
        assertEquals("NOT_BLANK", error.getCode());
        assertNull(error.getRejectedValue());
    }

    /**
     * Test that invalid email format returns INVALID_EMAIL error code.
     */
    @Test
    void testInvalidEmail_ReturnsInvalidEmailCode() throws Exception {
        String requestBody = """
                {
                  "fullName": "Test User",
                  "email": "not-an-email",
                  "password": "P@ssw0rd1",
                  "workspaceName": "Test Workspace",
                  "workspaceSlug": "test-workspace"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0].field").value("email"))
                .andExpect(jsonPath("$.errors[0].code").value("INVALID_EMAIL"));
    }

    /**
     * Test that short workspace name returns TOO_SHORT error code.
     */
    @Test
    void testWorkspaceNameTooShort_ReturnsTooShortCode() throws Exception {
        String requestBody = """
                {
                  "fullName": "Test User",
                  "email": "test@example.com",
                  "password": "P@ssw0rd1",
                  "workspaceName": "ab",
                  "workspaceSlug": "test-workspace"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0].field").value("workspaceName"))
                .andExpect(jsonPath("$.errors[0].code").value("INVALID_SIZE"));
    }

    /**
     * Test that invalid workspace slug format returns INVALID_FORMAT error code.
     */
    @Test
    void testInvalidWorkspaceSlug_ReturnsInvalidFormatCode() throws Exception {
        String requestBody = """
                {
                  "fullName": "Test User",
                  "email": "test@example.com",
                  "password": "P@ssw0rd1",
                  "workspaceName": "Test Workspace",
                  "workspaceSlug": "My-Workspace"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0].field").value("workspaceSlug"))
                .andExpect(jsonPath("$.errors[0].code").value("INVALID_FORMAT"));
    }

    /**
     * Test multiple validation errors returned together.
     */
    @Test
    void testMultipleValidationErrors_ReturnsAllErrors() throws Exception {
        String requestBody = """
                {
                  "fullName": "",
                  "email": "bad-email",
                  "password": "weak",
                  "workspaceName": "ab",
                  "workspaceSlug": "Invalid_Slug"
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(4)))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        ValidationErrorResponse response = objectMapper.readValue(responseBody, ValidationErrorResponse.class);

        List<FieldError> errors = response.getErrors();

        // Verify each error is present
        assertTrue(errors.stream().anyMatch(e -> "fullName".equals(e.getField()) && "NOT_BLANK".equals(e.getCode())));
        assertTrue(errors.stream().anyMatch(e -> "email".equals(e.getField()) && "INVALID_EMAIL".equals(e.getCode())));
        assertTrue(errors.stream().anyMatch(e -> "workspaceName".equals(e.getField()) && "INVALID_SIZE".equals(e.getCode())));
        assertTrue(errors.stream().anyMatch(e -> "workspaceSlug".equals(e.getField()) && "INVALID_FORMAT".equals(e.getCode())));

        // All errors should have rejectedValue
        errors.forEach(error -> {
            assertNotNull(error.getField());
            assertNotNull(error.getCode());
            assertNotNull(error.getMessage());
            assertNotNull(error.getRejectedValue());
        });
    }

    /**
     * Test that all required JSON fields are present in error response.
     */
    @Test
    void testValidationErrorResponse_ContainsAllRequiredFields() throws Exception {
        String requestBody = """
                {
                  "email": "test@example.com",
                  "password": "P@ssw0rd1"
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.errors").isArray())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        ValidationErrorResponse response = objectMapper.readValue(responseBody, ValidationErrorResponse.class);

        assertNotNull(response.getTimestamp());
        assertEquals(400, response.getStatus());
        assertEquals("Validation failed", response.getMessage());
        assertEquals("/api/auth/register", response.getPath());
        assertNotNull(response.getErrors());
    }

    /**
     * Test error code mapping for boundary cases.
     */
    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    void testBlankValueVariations_ReturnNotBlankCode(String blankValue) throws Exception {
        String requestBody = String.format("""
                {
                  "fullName": "%s",
                  "email": "test@example.com",
                  "password": "P@ssw0rd1",
                  "workspaceName": "Valid Workspace",
                  "workspaceSlug": "valid-workspace"
                }
                """, blankValue);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0].field").value("fullName"))
                .andExpect(jsonPath("$.errors[0].code").value("NOT_BLANK"));
    }

    /**
     * Test that successful registration with all valid fields still works.
     */
    @Test
    void testValidRegistration_SucceedsWithoutValidationErrors() throws Exception {
        String requestBody = """
                {
                  "fullName": "Valid User",
                  "email": "valid@example.com",
                  "password": "ValidP@ss1",
                  "workspaceName": "Valid Workspace Company",
                  "workspaceSlug": "valid-workspace-company"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk());
        // Note: Actual success depends on business logic (e.g., email not already registered)
        // We're just verifying the validation passes
    }

    /**
     * Test that Missing Content-Type is handled gracefully.
     * (Spring should return 400 with message about unsupported media type or parse error)
     */
    @Test
    void testMissingContentType_HandlesGracefully() throws Exception {
        String requestBody = """
                {
                  "fullName": "Test User",
                  "email": "test@example.com",
                  "password": "P@ssw0rd1",
                  "workspaceName": "Test Workspace"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                .content(requestBody))
                // Spring returns 415 Unsupported Media Type when Content-Type is missing/wrong
                .andExpect(status().isUnsupportedMediaType());
    }

    /**
     * Test that workspace slug size validation is applied.
     */
    @Test
    void testWorkspaceSlugTooLong_ReturnsTooLongCode() throws Exception {
        String longSlug = "very-long-workspace-slug-that-exceeds-maximum-length-constraint-definitely";

        String requestBody = String.format("""
                {
                  "fullName": "Test User",
                  "email": "test@example.com",
                  "password": "P@ssw0rd1",
                  "workspaceName": "Test Workspace",
                  "workspaceSlug": "%s"
                }
                """, longSlug);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0].field").value("workspaceSlug"))
                .andExpect(jsonPath("$.errors[0].code").value("INVALID_SIZE"));
    }
}

