package com.civicpulse;

import com.civicpulse.auth.dto.LoginRequest;
import com.civicpulse.user.User;
import com.civicpulse.user.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class UserAdminIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private String adminToken;
    private String citizenToken;
    private String officerToken;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = obtainToken("admin@civicpulse.city", "Password123!");
        citizenToken = obtainToken("citizen@civicpulse.city", "Password123!");
        officerToken = obtainToken("officer@civicpulse.city", "Password123!");
    }

    private String obtainToken(String email, String password) throws Exception {
        LoginRequest login = LoginRequest.builder().email(email).password(password).build();
        MvcResult res = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(res.getResponse().getContentAsString());
        return root.path("data").path("token").asText();
    }

    @Test
    @DisplayName("Admin should list users successfully with pagination metadata")
    void shouldAllowAdminToListUsers() throws Exception {
        mockMvc.perform(get("/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", not(empty())))
                .andExpect(jsonPath("$.data.totalElements", greaterThanOrEqualTo(3)));
    }

    @Test
    @DisplayName("Admin should filter users by ROLE_OFFICER")
    void shouldAllowAdminToFilterUsersByRole() throws Exception {
        mockMvc.perform(get("/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("role", "ROLE_OFFICER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content[*].roles", everyItem(hasItem("ROLE_OFFICER"))));
    }

    @Test
    @DisplayName("Admin should fetch user profile by ID")
    void shouldAllowAdminToGetUserById() throws Exception {
        User citizen = userRepository.findByEmail("citizen@civicpulse.city").orElseThrow();

        mockMvc.perform(get("/admin/users/" + citizen.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.email", is("citizen@civicpulse.city")));
    }

    @Test
    @DisplayName("Admin should deactivate and reactivate a user account")
    void shouldAllowAdminToDeactivateAndReactivateUser() throws Exception {
        User citizen = userRepository.findByEmail("citizen@civicpulse.city").orElseThrow();
        UUID userId = citizen.getId();

        // Deactivate
        mockMvc.perform(patch("/admin/users/" + userId + "/deactivate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.active", is(false)));

        User deactivated = userRepository.findById(userId).orElseThrow();
        assertFalse(deactivated.isActive());

        // Reactivate
        mockMvc.perform(patch("/admin/users/" + userId + "/activate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.active", is(true)));

        User reactivated = userRepository.findById(userId).orElseThrow();
        assertTrue(reactivated.isActive());
    }

    @Test
    @DisplayName("Non-admin users (Citizen & Officer) should be forbidden from accessing admin endpoints")
    void shouldDenyAccessToNonAdminUsers() throws Exception {
        mockMvc.perform(get("/admin/users")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/admin/users")
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated requests to admin endpoints should be unauthorized")
    void shouldDenyAccessToUnauthenticatedUsers() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isUnauthorized());
    }
}
