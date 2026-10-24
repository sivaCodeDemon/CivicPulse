package com.civicpulse.ai;

import com.civicpulse.ai.dto.DuplicateMatchResult;
import com.civicpulse.complaint.Complaint;
import com.civicpulse.complaint.ComplaintCategory;
import com.civicpulse.complaint.ComplaintRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class DuplicateDetectionService {

    private static final Logger log = LoggerFactory.getLogger(DuplicateDetectionService.class);
    private static final double EARTH_RADIUS_METERS = 6371000.0;

    @Value("${app.ai.duplicate-distance-meters-threshold:75.0}")
    private double distanceThresholdMeters;

    @Value("${app.ai.duplicate-similarity-threshold:0.55}")
    private double similarityThreshold;

    private final ComplaintRepository complaintRepository;

    public DuplicateMatchResult findPotentialDuplicate(
            Double latitude, Double longitude, String title, String description, ComplaintCategory category) {

        if (latitude == null || longitude == null) {
            return DuplicateMatchResult.none();
        }

        // Bounding box: ~100m in degrees (1 degree lat ~= 111,000m)
        double latDelta = 0.0015;
        double lngDelta = 0.0015;

        List<Complaint> nearbyComplaints = complaintRepository.findNearbyActiveComplaints(
                latitude - latDelta,
                latitude + latDelta,
                longitude - lngDelta,
                longitude + lngDelta
        );

        String newContent = ((title != null ? title : "") + " " + (description != null ? description : "")).toLowerCase();

        DuplicateMatchResult bestMatch = null;
        double highestCombinedScore = 0.0;

        for (Complaint existing : nearbyComplaints) {
            double distance = calculateHaversineDistance(
                    latitude, longitude,
                    existing.getLatitude(), existing.getLongitude()
            );

            if (distance <= distanceThresholdMeters) {
                String existingContent = ((existing.getTitle() != null ? existing.getTitle() : "") + " " +
                        (existing.getDescription() != null ? existing.getDescription() : "")).toLowerCase();

                double textSim = calculateJaccardTokenSimilarity(newContent, existingContent);
                boolean categoryMatches = existing.getCategory() == category;

                // Category match boosts confidence
                double combinedScore = (textSim * 0.7) + (categoryMatches ? 0.3 : 0.0);

                if (combinedScore >= similarityThreshold && combinedScore > highestCombinedScore) {
                    highestCombinedScore = combinedScore;
                    bestMatch = DuplicateMatchResult.builder()
                            .isDuplicate(true)
                            .matchedComplaintId(existing.getId())
                            .distanceMeters(Math.round(distance * 100.0) / 100.0)
                            .textSimilarityScore(Math.round(textSim * 100.0) / 100.0)
                            .combinedConfidence(Math.round(combinedScore * 100.0) / 100.0)
                            .matchReason(String.format("Found existing complaint within %.1fm with %.0f%% semantic similarity",
                                    distance, combinedScore * 100.0))
                            .build();
                }
            }
        }

        if (bestMatch != null) {
            log.info("Duplicate detected for complaint at ({}, {}): matches existing complaint {} (distance: {}m, sim: {})",
                    latitude, longitude, bestMatch.getMatchedComplaintId(), bestMatch.getDistanceMeters(), bestMatch.getCombinedConfidence());
            return bestMatch;
        }

        return DuplicateMatchResult.none();
    }

    public double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_METERS * c;
    }

    public double calculateJaccardTokenSimilarity(String text1, String text2) {
        Set<String> tokens1 = tokenize(text1);
        Set<String> tokens2 = tokenize(text2);

        if (tokens1.isEmpty() || tokens2.isEmpty()) {
            return 0.0;
        }

        Set<String> union = new HashSet<>(tokens1);
        union.addAll(tokens2);

        Set<String> intersection = new HashSet<>(tokens1);
        intersection.retainAll(tokens2);

        return (double) intersection.size() / union.size();
    }

    private Set<String> tokenize(String text) {
        if (text == null) return Collections.emptySet();

        String[] rawWords = text.replaceAll("[^a-zA-Z0-9\\s]", "").toLowerCase().split("\\s+");
        Set<String> stopWords = Set.of("the", "a", "an", "is", "in", "at", "of", "on", "and", "to", "for", "it", "with", "as");

        Set<String> tokens = new HashSet<>();
        for (String w : rawWords) {
            if (w.length() > 2 && !stopWords.contains(w)) {
                tokens.add(w);
            }
        }
        return tokens;
    }
}
