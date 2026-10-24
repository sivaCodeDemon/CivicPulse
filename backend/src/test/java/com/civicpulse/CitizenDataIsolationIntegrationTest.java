package com.civicpulse;

import com.civicpulse.auth.dto.LoginRequest;
import com.civicpulse.auth.dto.RegisterRequest;
import com.civicpulse.complaint.ComplaintCategory;
import com.civicpulse.complaint.dto.CreateComplaintRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CitizenDataIsolationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Verify strict privacy: Citizens cannot view private complaints submitted by other citizens")
    void shouldEnforceCitizenDataIsolation() throws Exception {
        // 1. Register Citizen A and Citizen B
        RegisterRequest citizenA = RegisterRequest.builder()
                .email("citizen.a@example.com")
                .password("Password123!")
                .firstName("Alice")
                .lastName("Walker")
                .build();

        RegisterRequest citizenB = RegisterRequest.builder()
                .email("citizen.b@example.com")
                .password("Password123!")
                .firstName("Bob")
                .lastName("Harris")
                .build();

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(citizenA))).andExpect(status().isCreated());

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(citizenB))).andExpect(status().isCreated());

        String tokenA = loginAndGetToken("citizen.a@example.com", "Password123!");
        String tokenB = loginAndGetToken("citizen.b@example.com", "Password123!");
        String officerToken = loginAndGetToken("officer@civicpulse.city", "Password123!");

        // 2. Citizen A creates a confidential complaint
        CreateComplaintRequest request = CreateComplaintRequest.builder()
                .title("Damaged storm drain behind residential boundary")
                .description("Drain wall collapsed near my private driveway causing foundation risk.")
                .category(ComplaintCategory.OTHER)
                .latitude(12.9200)
                .longitude(77.5800)
                .build();

        MvcResult createResult = mockMvc.perform(post("/complaints")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode root = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String complaintId = root.path("data").path("id").asText();

        // 3. Citizen A can access their own complaint -> 200 OK
        mockMvc.perform(get("/complaints/" + complaintId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is(complaintId)));

        // 4. Citizen B attempts to access Citizen A's complaint -> 403 Forbidden
        mockMvc.perform(get("/complaints/" + complaintId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.message", is("You do not have permission to access or modify this resource")));

        // 5. Municipal Officer can access complaint for review -> 200 OK
        mockMvc.perform(get("/complaints/" + complaintId)
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is(complaintId)));
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("token").asText();
    }
}
