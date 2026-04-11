package com.adrplatform.auth.controller;

import com.adrplatform.auth.domain.Role;
import com.adrplatform.auth.dto.UserDto;
import com.adrplatform.auth.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    void meEndpointShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "AUTHOR")
    void listUsersShouldDenyNonAdmin() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listUsersShouldAllowAdmin() throws Exception {
        when(userService.listUsersInCurrentWorkspace()).thenReturn(List.of(UserDto.builder()
                .id(UUID.randomUUID())
                .workspaceId(UUID.randomUUID())
                .email("admin@adr.com")
                .fullName("Admin")
                .role(Role.ADMIN)
                .createdAt(Instant.now())
                .build()));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateRoleShouldAllowAdmin() throws Exception {
        when(userService.updateRole(any(), any())).thenReturn(UserDto.builder()
                .id(UUID.randomUUID())
                .workspaceId(UUID.randomUUID())
                .email("user@adr.com")
                .fullName("User")
                .role(Role.REVIEWER)
                .createdAt(Instant.now())
                .build());

        mockMvc.perform(put("/api/users/{id}/role", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"REVIEWER\"}"))
                .andExpect(status().isOk());
    }
}
