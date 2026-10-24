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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ComplaintImageIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String citizenToken;
    private String officerToken;

    @BeforeEach
    void setUp() throws Exception {
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
    @DisplayName("Citizen should upload and stream photo attachment for their complaint")
    void shouldUploadAndDownloadImage() throws Exception {
        // 1. Submit complaint
        CreateComplaintRequest complaintReq = CreateComplaintRequest.builder()
                .title("Broken sidewalk with exposed rebar")
                .description("Concrete damaged near bus shelter causing tripping hazard.")
                .category(ComplaintCategory.OTHER)
                .latitude(12.9100)
                .longitude(77.6100)
                .build();

        MvcResult compRes = mockMvc.perform(post("/complaints")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(complaintReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String complaintId = objectMapper.readTree(compRes.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // 2. Upload valid image
        byte[] fakeImageContent = new byte[]{ (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0 }; // JPEG magic bytes
        MockMultipartFile file = new MockMultipartFile(
                "file", "sidewalk_evidence.jpg", "image/jpeg", fakeImageContent);

        MvcResult uploadRes = mockMvc.perform(multipart("/complaints/" + complaintId + "/images")
                        .file(file)
                        .param("imageType", "COMPLAINT_ATTACHMENT")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.fileName", is("sidewalk_evidence.jpg")))
                .andExpect(jsonPath("$.data.fileType", is("image/jpeg")))
                .andReturn();

        String imageId = objectMapper.readTree(uploadRes.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // 3. Retrieve list of images for complaint
        mockMvc.perform(get("/complaints/" + complaintId + "/images")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id", is(imageId)));

        // 4. Download and stream image binary
        mockMvc.perform(get("/complaints/" + complaintId + "/images/" + imageId)
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", is("image/jpeg")))
                .andExpect(content().bytes(fakeImageContent));

        // 5. Officer can also download and review image
        mockMvc.perform(get("/complaints/" + complaintId + "/images/" + imageId)
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should reject upload with invalid mime type")
    void shouldRejectInvalidMimeType() throws Exception {
        CreateComplaintRequest complaintReq = CreateComplaintRequest.builder()
                .title("Graffiti on public monument")
                .description("Vandalism on park fountain stone.")
                .category(ComplaintCategory.OTHER)
                .latitude(12.9200)
                .longitude(77.6200)
                .build();

        MvcResult compRes = mockMvc.perform(post("/complaints")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(complaintReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String complaintId = objectMapper.readTree(compRes.getResponse().getContentAsString())
                .path("data").path("id").asText();

        MockMultipartFile illegalPdfFile = new MockMultipartFile(
                "file", "script.pdf", "application/pdf", "malicious payload".getBytes());

        mockMvc.perform(multipart("/complaints/" + complaintId + "/images")
                        .file(illegalPdfFile)
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("Only JPEG, PNG, and WebP")));
    }
}
