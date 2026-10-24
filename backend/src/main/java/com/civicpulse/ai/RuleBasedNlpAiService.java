package com.civicpulse.ai;

import com.civicpulse.ai.dto.AiAnalysisResult;
import com.civicpulse.complaint.ComplaintCategory;
import com.civicpulse.complaint.ComplaintPriority;
import com.civicpulse.complaint.ComplaintSeverity;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RuleBasedNlpAiService implements AiInferenceService {

    private static final Map<ComplaintCategory, List<String>> CATEGORY_KEYWORDS = Map.of(
            ComplaintCategory.POTHOLE, List.of("pothole", "crater", "road damage", "asphalt", "sinkhole", "pavement", "divider", "tar", "road hole", "speed breaker"),
            ComplaintCategory.GARBAGE_OVERFLOW, List.of("garbage", "trash", "waste", "debris", "dump", "bin", "stench", "rotting", "overflow", "litter", "rubbish"),
            ComplaintCategory.WATER_LEAKAGE, List.of("water leak", "pipeline", "pipe burst", "sewage", "drain", "water supply", "drinking water", "flooding", "manhole", "water logging"),
            ComplaintCategory.BROKEN_STREETLIGHT, List.of("streetlight", "lamp", "pole", "bulb", "dark", "flickering", "darkness", "live wire", "wire", "blackout", "fuse"),
            ComplaintCategory.FALLEN_TREE, List.of("tree", "branch", "trunk", "roots", "fallen tree", "horticulture", "foliage", "overgrown")
    );

    private static final List<String> EMERGENCY_KEYWORDS = List.of(
            "emergency", "live wire", "electric shock", "sparking", "sinkhole", "fatal",
            "critical", "danger", "hazard", "gas leak", "collapse", "child"
    );

    private static final List<String> SEVERE_KEYWORDS = List.of(
            "severe", "major", "deep", "accident", "hospital", "school", "flooding",
            "burst", "complete darkness", "urgent", "huge"
    );

    @Override
    public AiAnalysisResult analyzeComplaint(String title, String description, ComplaintCategory citizenSelectedCategory) {
        String combinedText = ((title != null ? title : "") + " " + (description != null ? description : "")).toLowerCase();

        Map<ComplaintCategory, Integer> categoryScores = new EnumMap<>(ComplaintCategory.class);
        List<String> matchedKeywords = new ArrayList<>();

        for (Map.Entry<ComplaintCategory, List<String>> entry : CATEGORY_KEYWORDS.entrySet()) {
            int score = 0;
            for (String kw : entry.getValue()) {
                if (combinedText.contains(kw)) {
                    score += 10;
                    matchedKeywords.add(kw);
                }
            }
            categoryScores.put(entry.getKey(), score);
        }

        ComplaintCategory bestCategory = citizenSelectedCategory != null ? citizenSelectedCategory : ComplaintCategory.OTHER;
        int maxScore = 0;

        for (Map.Entry<ComplaintCategory, Integer> entry : categoryScores.entrySet()) {
            if (entry.getValue() > maxScore) {
                maxScore = entry.getValue();
                bestCategory = entry.getKey();
            }
        }

        double confidenceScore;
        if (maxScore >= 20) {
            confidenceScore = 0.92;
        } else if (maxScore >= 10) {
            confidenceScore = 0.82;
        } else if (citizenSelectedCategory != null) {
            confidenceScore = 0.70;
            bestCategory = citizenSelectedCategory;
        } else {
            confidenceScore = 0.55;
            bestCategory = ComplaintCategory.OTHER;
        }

        // Determine severity & priority
        ComplaintSeverity suggestedSeverity = ComplaintSeverity.MODERATE;
        ComplaintPriority suggestedPriority = ComplaintPriority.MEDIUM;
        String reasoning = "Standard issue severity applied";

        boolean hasEmergency = EMERGENCY_KEYWORDS.stream().anyMatch(combinedText::contains);
        boolean hasSevere = SEVERE_KEYWORDS.stream().anyMatch(combinedText::contains);

        if (hasEmergency) {
            suggestedSeverity = ComplaintSeverity.EMERGENCY;
            suggestedPriority = ComplaintPriority.CRITICAL;
            reasoning = "Emergency keywords detected in description indicating imminent safety hazard";
        } else if (hasSevere) {
            suggestedSeverity = ComplaintSeverity.SEVERE;
            suggestedPriority = ComplaintPriority.HIGH;
            reasoning = "High-impact civic issue identified with elevated community risk";
        } else if (bestCategory == ComplaintCategory.BROKEN_STREETLIGHT || bestCategory == ComplaintCategory.WATER_LEAKAGE) {
            suggestedSeverity = ComplaintSeverity.MODERATE;
            suggestedPriority = ComplaintPriority.MEDIUM;
            reasoning = "Standard municipal utility triage level";
        }

        return AiAnalysisResult.builder()
                .suggestedCategory(bestCategory)
                .confidenceScore(confidenceScore)
                .suggestedSeverity(suggestedSeverity)
                .suggestedPriority(suggestedPriority)
                .detectedKeywords(matchedKeywords)
                .reasoning(reasoning)
                .build();
    }
}
