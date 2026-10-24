package com.civicpulse.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DuplicateMatchResult {
    private boolean isDuplicate;
    private UUID matchedComplaintId;
    private double distanceMeters;
    private double textSimilarityScore;
    private double combinedConfidence;
    private String matchReason;

    public static DuplicateMatchResult none() {
        return DuplicateMatchResult.builder()
                .isDuplicate(false)
                .matchedComplaintId(null)
                .distanceMeters(0)
                .textSimilarityScore(0)
                .combinedConfidence(0)
                .matchReason("No duplicate complaints detected")
                .build();
    }
}
