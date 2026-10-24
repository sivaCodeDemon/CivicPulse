package com.civicpulse;

import com.civicpulse.ai.RuleBasedNlpAiService;
import com.civicpulse.ai.dto.AiAnalysisResult;
import com.civicpulse.complaint.ComplaintCategory;
import com.civicpulse.complaint.ComplaintPriority;
import com.civicpulse.complaint.ComplaintSeverity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AiInferenceServiceTest {

    private RuleBasedNlpAiService aiService;

    @BeforeEach
    void setUp() {
        aiService = new RuleBasedNlpAiService();
    }

    @Test
    @DisplayName("Should accurately classify water leakage issues and calculate confidence")
    void shouldClassifyWaterLeakage() {
        String title = "Massive pipe burst on Main Street";
        String description = "Underground water supply pipeline burst, flooding the entire road and losing clean drinking water.";

        AiAnalysisResult result = aiService.analyzeComplaint(title, description, null);

        assertEquals(ComplaintCategory.WATER_LEAKAGE, result.getSuggestedCategory());
        assertTrue(result.getConfidenceScore() >= 0.80);
        assertTrue(result.getDetectedKeywords().contains("pipeline"));
        assertTrue(result.getDetectedKeywords().contains("water supply"));
    }

    @Test
    @DisplayName("Should identify emergency hazards and elevate severity to EMERGENCY / CRITICAL")
    void shouldElevateEmergencySeverity() {
        String title = "Exposed live wire sparking near school gate";
        String description = "An electric pole wire broke and is sparking on the wet pavement. Imminent hazard to children walking by.";

        AiAnalysisResult result = aiService.analyzeComplaint(title, description, ComplaintCategory.BROKEN_STREETLIGHT);

        assertEquals(ComplaintCategory.BROKEN_STREETLIGHT, result.getSuggestedCategory());
        assertEquals(ComplaintSeverity.EMERGENCY, result.getSuggestedSeverity());
        assertEquals(ComplaintPriority.CRITICAL, result.getSuggestedPriority());
        assertTrue(result.getReasoning().contains("Emergency keywords"));
    }

    @Test
    @DisplayName("Should detect high severity for severe road accidents")
    void shouldDetectSevereRoadAccidents() {
        String title = "Severe crater pothole causing vehicle accidents";
        String description = "Deep asphalt crater on high speed expressway causing tires to puncture.";

        AiAnalysisResult result = aiService.analyzeComplaint(title, description, ComplaintCategory.POTHOLE);

        assertEquals(ComplaintCategory.POTHOLE, result.getSuggestedCategory());
        assertEquals(ComplaintSeverity.SEVERE, result.getSuggestedSeverity());
        assertEquals(ComplaintPriority.HIGH, result.getSuggestedPriority());
    }
}
