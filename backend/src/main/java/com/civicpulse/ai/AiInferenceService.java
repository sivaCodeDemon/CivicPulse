package com.civicpulse.ai;

import com.civicpulse.ai.dto.AiAnalysisResult;
import com.civicpulse.complaint.ComplaintCategory;

public interface AiInferenceService {
    AiAnalysisResult analyzeComplaint(String title, String description, ComplaintCategory citizenSelectedCategory);
}
