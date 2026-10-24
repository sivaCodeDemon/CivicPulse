package com.civicpulse.ai.dto;

import com.civicpulse.complaint.ComplaintCategory;
import com.civicpulse.complaint.ComplaintPriority;
import com.civicpulse.complaint.ComplaintSeverity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiAnalysisResult {
    private ComplaintCategory suggestedCategory;
    private double confidenceScore;
    private ComplaintSeverity suggestedSeverity;
    private ComplaintPriority suggestedPriority;
    private List<String> detectedKeywords;
    private String reasoning;
}
