package com.civicpulse;

import com.civicpulse.auth.dto.LoginRequest;
import com.civicpulse.complaint.ComplaintCategory;
import com.civicpulse.complaint.ComplaintPriority;
import com.civicpulse.complaint.ComplaintSeverity;
import com.civicpulse.complaint.ComplaintStatus;
import com.civicpulse.complaint.dto.CreateComplaintRequest;
import com.civicpulse.complaint.dto.UpdateComplaintStatusRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ComplaintControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String citizenToken;
    private String officerToken;

    @BeforeEach
    void setUp() throws Exception {
        citizenToken = obtainJwtToken("citizen@civicpulse.city", "Password123!");
        officerToken = obtainJwtToken("officer@civicpulse.city", "Password123!");
    }

    private String obtainJwtToken(String email, String password) throws Exception {
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

    @Test
    @DisplayName("Citizen should create complaint with geolocation successfully")
    void shouldCreateComplaint() throws Exception {
        CreateComplaintRequest request = CreateComplaintRequest.builder()
                .title("Severe pothole on 5th Main Avenue")
                .description("Deep crater causing vehicle traffic bottlenecks and bicycle hazard.")
                .category(ComplaintCategory.POTHOLE)
                .latitude(12.9716)
                .longitude(77.5946)
                .addressText("5th Main Rd, Indiranagar, Bengaluru")
                .priority(ComplaintPriority.HIGH)
                .severity(ComplaintSeverity.SEVERE)
                .build();

        mockMvc.perform(post("/complaints")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.title", is("Severe pothole on 5th Main Avenue")))
                .andExpect(jsonPath("$.data.category", is("POTHOLE")))
                .andExpect(jsonPath("$.data.status", is("REPORTED")))
                .andExpect(jsonPath("$.data.departmentCode", is("ROADS_PWD")))
                .andExpect(jsonPath("$.data.slaDeadline", notNullValue()));
    }

    @Test
    @DisplayName("Should reject complaint creation with invalid latitude/longitude")
    void shouldRejectInvalidCoordinates() throws Exception {
        CreateComplaintRequest request = CreateComplaintRequest.builder()
                .title("Invalid location pothole")
                .description("Test description with enough length for validation.")
                .category(ComplaintCategory.POTHOLE)
                .latitude(195.0) // Invalid: exceeds 90.0
                .longitude(77.5946)
                .build();

        mockMvc.perform(post("/complaints")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.validationErrors.latitude", notNullValue()));
    }

    @Test
    @DisplayName("Citizen should fetch their own complaints")
    void shouldFetchMyComplaints() throws Exception {
        // First submit a complaint
        CreateComplaintRequest request = CreateComplaintRequest.builder()
                .title("Streetlight outage")
                .description("Lamp post #42 has been flickering and completely off for 3 nights.")
                .category(ComplaintCategory.BROKEN_STREETLIGHT)
                .latitude(12.9352)
                .longitude(77.6245)
                .build();

        mockMvc.perform(post("/complaints")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Then query /complaints/my
        mockMvc.perform(get("/complaints/my")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", not(empty())))
                .andExpect(jsonPath("$.data.totalElements", greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("Officer should update complaint status and resolve issue")
    void shouldAllowOfficerToUpdateStatus() throws Exception {
        // Create complaint as citizen
        CreateComplaintRequest request = CreateComplaintRequest.builder()
                .title("Garbage overflow near market")
                .description("Community bin has not been cleared for 4 days, spilling on road.")
                .category(ComplaintCategory.GARBAGE_OVERFLOW)
                .latitude(12.9250)
                .longitude(77.5890)
                .build();

        MvcResult createResult = mockMvc.perform(post("/complaints")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode root = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String complaintId = root.path("data").path("id").asText();

        // Update status as officer to IN_PROGRESS
        UpdateComplaintStatusRequest updateRequest = UpdateComplaintStatusRequest.builder()
                .status(ComplaintStatus.IN_PROGRESS)
                .remarks("Sanitation vehicle dispatched to clearance point")
                .build();

        mockMvc.perform(patch("/complaints/" + complaintId + "/status")
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("IN_PROGRESS")));

        // Update status as officer to RESOLVED
        UpdateComplaintStatusRequest resolveRequest = UpdateComplaintStatusRequest.builder()
                .status(ComplaintStatus.RESOLVED)
                .remarks("Waste cleared and sanitized successfully")
                .build();

        mockMvc.perform(patch("/complaints/" + complaintId + "/status")
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resolveRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("RESOLVED")))
                .andExpect(jsonPath("$.data.resolvedAt", notNullValue()));
    }

    @Test
    @DisplayName("Citizen should be forbidden from arbitrarily modifying complaint status")
    void shouldForbidCitizenFromUpdatingStatus() throws Exception {
        CreateComplaintRequest request = CreateComplaintRequest.builder()
                .title("Water leakage in sector 3")
                .description("Fresh drinking water pipe leaking continuously into storm drain.")
                .category(ComplaintCategory.WATER_LEAKAGE)
                .latitude(12.9810)
                .longitude(77.6010)
                .build();

        MvcResult createResult = mockMvc.perform(post("/complaints")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode root = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String complaintId = root.path("data").path("id").asText();

        UpdateComplaintStatusRequest resolveRequest = UpdateComplaintStatusRequest.builder()
                .status(ComplaintStatus.RESOLVED)
                .remarks("Citizen trying to close own complaint")
                .build();

        mockMvc.perform(patch("/complaints/" + complaintId + "/status")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resolveRequest)))
                .andExpect(status().isForbidden());
    }
}
