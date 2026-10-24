package com.civicpulse;

import com.civicpulse.auth.dto.LoginRequest;
import com.civicpulse.auth.dto.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should successfully register a new citizen user")
    void shouldRegisterNewCitizen() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("new.citizen@example.com")
                .password("Password123!")
                .firstName("Jane")
                .lastName("Doe")
                .phoneNumber("+1-555-4321")
                .build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.email", is("new.citizen@example.com")))
                .andExpect(jsonPath("$.data.firstName", is("Jane")))
                .andExpect(jsonPath("$.data.roles", hasItem("ROLE_CITIZEN")));
    }

    @Test
    @DisplayName("Should reject registration with duplicate email")
    void shouldRejectDuplicateEmailRegistration() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("citizen@civicpulse.city") // already seeded by DataInitializer
                .password("Password123!")
                .firstName("Duplicate")
                .lastName("User")
                .build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("already in use")));
    }

    @Test
    @DisplayName("Should authenticate seeded user and return JWT bearer token")
    void shouldLoginSuccessfully() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email("citizen@civicpulse.city")
                .password("Password123!")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andExpect(jsonPath("$.data.type", is("Bearer")))
                .andExpect(jsonPath("$.data.email", is("citizen@civicpulse.city")))
                .andExpect(jsonPath("$.data.roles", hasItem("ROLE_CITIZEN")));
    }

    @Test
    @DisplayName("Should reject authentication with invalid password")
    void shouldRejectBadCredentials() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email("citizen@civicpulse.city")
                .password("WrongPassword123!")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.message", is("Invalid email or password")));
    }

    @Test
    @DisplayName("Should reject unauthenticated access to /auth/me")
    void shouldRejectUnauthenticatedProfileAccess() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }
}
