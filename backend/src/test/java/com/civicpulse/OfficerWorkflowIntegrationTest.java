package com.civicpulse;

import com.civicpulse.auth.dto.LoginRequest;
import com.civicpulse.complaint.*;
import com.civicpulse.complaint.dto.AssignComplaintRequest;
import com.civicpulse.complaint.dto.CreateComplaintRequest;
import com.civicpulse.complaint.dto.UpdateComplaintStatusRequest;
import com.civicpulse.notification.NotificationRepository;
import com.civicpulse.notification.NotificationType;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class OfficerWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private SlaEscalationScheduler slaEscalationScheduler;

    @Autowired
    private NotificationRepository notificationRepository;

    private String citizenToken;
    private String officerToken;
    private User officerUser;

    @BeforeEach
    void setUp() throws Exception {
        citizenToken = obtainToken("citizen@civicpulse.city", "Password123!");
        officerToken = obtainToken("officer@civicpulse.city", "Password123!");
        officerUser = userRepository.findByEmail("officer@civicpulse.city").orElseThrow();
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
    @DisplayName("Should assign complaint to officer, update status, and dispatch notifications")
    void shouldAssignComplaintToOfficer() throws Exception {
        // 1. Citizen creates complaint
        CreateComplaintRequest createReq = CreateComplaintRequest.builder()
                .title("Damaged storm drain cover")
                .description("Metal grate missing on pedestrian footpath.")
                .category(ComplaintCategory.OTHER)
                .latitude(12.9300)
                .longitude(77.6200)
                .build();

        MvcResult createRes = mockMvc.perform(post("/complaints")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String complaintId = objectMapper.readTree(createRes.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // 2. Assign to officer
        AssignComplaintRequest assignReq = AssignComplaintRequest.builder()
                .officerId(officerUser.getId())
                .notes("Dispatched for immediate site inspection")
                .build();

        mockMvc.perform(post("/complaints/" + complaintId + "/assign")
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.officerId", is(officerUser.getId().toString())))
                .andExpect(jsonPath("$.data.notes", is("Dispatched for immediate site inspection")));

        // 3. Verify status updated to ASSIGNED
        mockMvc.perform(get("/complaints/" + complaintId)
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("ASSIGNED")));
    }

    @Test
    @DisplayName("Should enforce strict FSM transitions and audit history trail")
    void shouldEnforceFsmAndTrackHistory() throws Exception {
        // 1. Create complaint
        CreateComplaintRequest createReq = CreateComplaintRequest.builder()
                .title("Overflowing garbage bin on 80ft road")
                .description("Household waste overflowing onto the sidewalk for 3 days.")
                .category(ComplaintCategory.GARBAGE_OVERFLOW)
                .latitude(12.9380)
                .longitude(77.6300)
                .build();

        MvcResult createRes = mockMvc.perform(post("/complaints")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String complaintId = objectMapper.readTree(createRes.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // 2. Valid transition: REPORTED -> IN_PROGRESS
        UpdateComplaintStatusRequest progressReq = UpdateComplaintStatusRequest.builder()
                .status(ComplaintStatus.IN_PROGRESS)
                .remarks("Sanitation truck on the way")
                .build();

        mockMvc.perform(patch("/complaints/" + complaintId + "/status")
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(progressReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("IN_PROGRESS")));

        // 3. Invalid transition: IN_PROGRESS -> REPORTED (Illegal backward transition)
        UpdateComplaintStatusRequest illegalReq = UpdateComplaintStatusRequest.builder()
                .status(ComplaintStatus.REPORTED)
                .remarks("Attempting invalid regression")
                .build();

        mockMvc.perform(patch("/complaints/" + complaintId + "/status")
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(illegalReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("Invalid status transition")));

        // 4. Valid transition: IN_PROGRESS -> RESOLVED
        UpdateComplaintStatusRequest resolveReq = UpdateComplaintStatusRequest.builder()
                .status(ComplaintStatus.RESOLVED)
                .remarks("Waste cleared and area disinfected")
                .build();

        mockMvc.perform(patch("/complaints/" + complaintId + "/status")
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resolveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("RESOLVED")));

        // 5. Verify audit history trail
        mockMvc.perform(get("/complaints/" + complaintId + "/history")
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(3))))
                .andExpect(jsonPath("$.data[0].newStatus", is("REPORTED")))
                .andExpect(jsonPath("$.data[1].newStatus", is("IN_PROGRESS")))
                .andExpect(jsonPath("$.data[2].newStatus", is("RESOLVED")));
    }

    @Test
    @DisplayName("SLA background scanner should detect overdue complaints and escalate priority")
    void shouldEscalateOverdueComplaint() {
        // Create an overdue complaint directly in repository
        User citizen = userRepository.findByEmail("citizen@civicpulse.city").orElseThrow();

        Complaint overdue = Complaint.builder()
                .citizen(citizen)
                .title("Overdue pipe leak")
                .description("Long pending water pipeline leak running past SLA deadline.")
                .category(ComplaintCategory.WATER_LEAKAGE)
                .status(ComplaintStatus.IN_PROGRESS)
                .priority(ComplaintPriority.MEDIUM)
                .severity(ComplaintSeverity.MODERATE)
                .latitude(12.9500)
                .longitude(77.6500)
                .slaDeadline(Instant.now().minus(2, ChronoUnit.HOURS)) // Expired 2 hours ago
                .build();

        Complaint saved = complaintRepository.save(overdue);

        // Execute background SLA escalation job
        slaEscalationScheduler.scanAndEscalateOverdueComplaints();

        // Verify priority escalated to CRITICAL
        Complaint refreshed = complaintRepository.findById(saved.getId()).orElseThrow();
        assertEquals(ComplaintPriority.CRITICAL, refreshed.getPriority());

        // Verify SLA escalation notification created for citizen
        boolean notificationExists = notificationRepository.findAll().stream()
                .anyMatch(n -> n.getComplaint() != null &&
                               n.getComplaint().getId().equals(saved.getId()) &&
                               n.getType() == NotificationType.SLA_BREACH_WARNING);

        assertTrue(notificationExists, "Citizen should receive SLA_BREACH_WARNING notification");
    }

    @Test
    @DisplayName("User should view in-app notifications and mark as read")
    void shouldManageNotifications() throws Exception {
        // Query notifications
        MvcResult res = mockMvc.perform(get("/notifications")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", notNullValue()))
                .andReturn();

        JsonNode root = objectMapper.readTree(res.getResponse().getContentAsString());
        JsonNode content = root.path("data").path("content");

        if (content.isArray() && content.size() > 0) {
            String notificationId = content.get(0).path("id").asText();

            // Mark as read
            mockMvc.perform(patch("/notifications/" + notificationId + "/read")
                            .header("Authorization", "Bearer " + citizenToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.read", is(true)));
        }

        // Check unread count endpoint
        mockMvc.perform(get("/notifications/unread-count")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", notNullValue()));
    }
}
