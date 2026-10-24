package com.civicpulse;

import com.civicpulse.ai.DuplicateDetectionService;
import com.civicpulse.complaint.ComplaintRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DuplicateDetectionServiceTest {

    private DuplicateDetectionService duplicateDetectionService;

    @BeforeEach
    void setUp() {
        ComplaintRepository complaintRepository = Mockito.mock(ComplaintRepository.class);
        duplicateDetectionService = new DuplicateDetectionService(complaintRepository);
    }

    @Test
    @DisplayName("Should accurately calculate Haversine distance between coordinates")
    void shouldCalculateAccurateDistance() {
        // Point A: Indiranagar 100ft Rd (12.9716, 77.6412)
        // Point B: ~55 meters away (12.9720, 77.6415)
        double distance = duplicateDetectionService.calculateHaversineDistance(
                12.9716, 77.6412,
                12.9720, 77.6415
        );

        assertTrue(distance > 40 && distance < 70, "Calculated distance should be approximately 55 meters, got: " + distance);
    }

    @Test
    @DisplayName("Should calculate Jaccard token similarity for overlapping complaint descriptions")
    void shouldCalculateJaccardSimilarity() {
        String text1 = "Huge pothole on 4th cross road near bakery";
        String text2 = "Dangerous deep pothole on 4th cross road near the corner bakery";

        double similarity = duplicateDetectionService.calculateJaccardTokenSimilarity(text1, text2);

        assertTrue(similarity > 0.50, "Similarity should exceed 50% for overlapping issue reports, got: " + similarity);
    }

    @Test
    @DisplayName("Should return zero similarity for completely unrelated civic issues")
    void shouldReturnZeroForUnrelatedIssues() {
        String text1 = "Overflowing garbage dump in central market";
        String text2 = "Flickering street light pole number 42";

        double similarity = duplicateDetectionService.calculateJaccardTokenSimilarity(text1, text2);

        assertEquals(0.0, similarity, 0.001);
    }
}
