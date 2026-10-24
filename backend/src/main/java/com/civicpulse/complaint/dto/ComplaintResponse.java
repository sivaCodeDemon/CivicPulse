package com.civicpulse.complaint.dto;

import com.civicpulse.complaint.ComplaintCategory;
import com.civicpulse.complaint.ComplaintPriority;
import com.civicpulse.complaint.ComplaintSeverity;
import com.civicpulse.complaint.ComplaintStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintResponse {

    private UUID id;
    private UUID citizenId;
    private String citizenName;
    private String citizenEmail;

    private Long departmentId;
    private String departmentName;
    private String departmentCode;

    private String title;
    private String description;
    private ComplaintCategory category;
    private ComplaintStatus status;
    private ComplaintPriority priority;
    private ComplaintSeverity severity;

    private Double latitude;
    private Double longitude;
    private String addressText;

    @com.fasterxml.jackson.annotation.JsonProperty("isPotentialDuplicate")
    private boolean isPotentialDuplicate;
    private UUID duplicateOfId;

    private Double aiConfidenceScore;
    private String aiSuggestedCategory;

    private Instant slaDeadline;
    @com.fasterxml.jackson.annotation.JsonProperty("isOverdue")
    private boolean isOverdue;
    private Instant resolvedAt;

    private Instant createdAt;
    private Instant updatedAt;
}
