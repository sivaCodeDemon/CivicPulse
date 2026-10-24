package com.civicpulse;

import com.civicpulse.auth.dto.LoginRequest;
import com.civicpulse.complaint.ComplaintCategory;
import com.civicpulse.complaint.dto.CreateComplaintRequest;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AiComplaintProcessingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String citizenToken;

    @BeforeEach
    void setUp() throws Exception {
        LoginRequest login = LoginRequest.builder()
                .email("citizen@civicpulse.city")
                .password("Password123!")
                .build();

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        citizenToken = root.path("data").path("token").asText();
    }

    @Test
    @DisplayName("AI should infer emergency severity, category and detect nearby duplicate complaints")
    void shouldProcessComplaintWithAiAndDetectDuplicates() throws Exception {
        // 1. Submit initial complaint with electric emergency hazard
        CreateComplaintRequest complaint1 = CreateComplaintRequest.builder()
                .title("Exposed live wire sparking on road")
                .description("Damaged electric pole with live wire sparking and creating critical danger for pedestrians.")
                .category(ComplaintCategory.BROKEN_STREETLIGHT)
                .latitude(12.9750)
                .longitude(77.6050)
                .addressText("Brigade Road junction, Bengaluru")
                .build();

        MvcResult res1 = mockMvc.perform(post("/complaints")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(complaint1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.aiSuggestedCategory", is("BROKEN_STREETLIGHT")))
                .andExpect(jsonPath("$.data.aiConfidenceScore", greaterThanOrEqualTo(0.8)))
                .andExpect(jsonPath("$.data.severity", is("EMERGENCY")))
                .andExpect(jsonPath("$.data.priority", is("CRITICAL")))
                .andExpect(jsonPath("$.data.isPotentialDuplicate", is(false)))
                .andReturn();

        JsonNode root1 = objectMapper.readTree(res1.getResponse().getContentAsString());
        String complaint1Id = root1.path("data").path("id").asText();

        // 2. Submit second complaint at nearly identical location (~20m away) with matching keywords
        CreateComplaintRequest complaint2 = CreateComplaintRequest.builder()
                .title("Electric pole wire sparking")
                .description("Live wire hanging from pole sparking near pavement road.")
                .category(ComplaintCategory.BROKEN_STREETLIGHT)
                .latitude(12.9751) // ~11 meters away
                .longitude(77.6051)
                .addressText("Brigade Road, opposite shop")
                .build();

        mockMvc.perform(post("/complaints")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(complaint2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.isPotentialDuplicate", is(true)))
                .andExpect(jsonPath("$.data.duplicateOfId", is(complaint1Id)));
    }
}
